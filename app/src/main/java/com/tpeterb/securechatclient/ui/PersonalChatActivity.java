package com.tpeterb.securechatclient.ui;

import static com.tpeterb.securechatclient.constants.Constants.NUMBER_OF_MESSAGES_TO_FETCH_AT_ONCE;

import android.content.Intent;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.tpeterb.securechatclient.R;
import com.tpeterb.securechatclient.application.ChatApplication;
import com.tpeterb.securechatclient.messages.delivery.WebSocketClient;
import com.tpeterb.securechatclient.messages.listener.PersonalChatMessageBulkUpdateListener;
import com.tpeterb.securechatclient.messages.listener.PersonalChatMessageSingleUpdateListener;
import com.tpeterb.securechatclient.messages.model.MessageContentType;
import com.tpeterb.securechatclient.messages.registry.ChatPartnerRegistry;
import com.tpeterb.securechatclient.messages.registry.MessageRegistry;
import com.tpeterb.securechatclient.messages.registry.StompSubscriptionRegistry;
import com.tpeterb.securechatclient.messages.service.CompressionService;
import com.tpeterb.securechatclient.messages.service.MessageChunkService;
import com.tpeterb.securechatclient.messages.service.MessageService;
import com.tpeterb.securechatclient.messages.service.StompSubscriptionService;
import com.tpeterb.securechatclient.security.cache.ServerPublicKeyCache;
import com.tpeterb.securechatclient.security.config.SecurityConfig;
import com.tpeterb.securechatclient.security.observer.MessagePublicKeyChangeObserver;
import com.tpeterb.securechatclient.ui.adapter.PersonalChatAdapter;
import com.tpeterb.securechatclient.users.model.ChatPartner;
import com.tpeterb.securechatclient.users.session.UserSession;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Objects;
import java.util.Optional;

import javax.inject.Inject;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class PersonalChatActivity extends AppCompatActivity implements MessagePublicKeyChangeObserver {

    private final Handler handler = new Handler(Looper.getMainLooper());

    private ImageView backArrowButton;

    private TextView chatPartnerUsernameTextView;

    private PersonalChatAdapter personalChatAdapter;

    private RecyclerView chatMessagesView;

    private ImageView imageSendingButton;

    private EditText messageInputBox;

    private ImageView messageSendingButton;

    private LinearLayoutManager linearLayoutManager;

    private int numberOfMessagesInConversationBeforeFetching = 0;

    private ActivityResultLauncher<String> imagePickingActivityLauncher;

    @Inject
    CompressionService compressionService;

    @Inject
    SecurityConfig securityConfig;

    @Inject
    ServerPublicKeyCache serverPublicKeyCache;

    @Inject
    StompSubscriptionService stompSubscriptionService;

    @Inject
    MessageChunkService messageChunkService;

    @Inject
    MessageRegistry messageRegistry;

    @Inject
    MessageService messageService;

    @Inject
    ChatPartnerRegistry chatPartnerRegistry;

    @Inject
    StompSubscriptionRegistry stompSubscriptionRegistry;

    @Inject
    WebSocketClient webSocketClient;

    @Inject
    UserSession userSession;

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            View view = getCurrentFocus();
            if (view instanceof EditText) {
                Rect outRect = new Rect();
                view.getGlobalVisibleRect(outRect);
                if (!outRect.contains((int) event.getRawX(), (int) event.getRawY())) {
                    view.clearFocus();
                    ((EditText) view).setMaxLines(1);
                    InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
                    if (imm != null) {
                        imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
                    }
                } else {
                    ((EditText) view).setMaxLines(3);
                }
            }
        }
        return super.dispatchTouchEvent(event);
    }

    @Override
    protected void onResume() {
        super.onResume();
        log.info("onResume of Personal, messageRegistry = {}", messageRegistry);
        log.info("onResume of Personal, stompSubscriptionRegistry = {}", stompSubscriptionRegistry);
        log.info("onResume of Personal, chatPartnerRegistry = {}", chatPartnerRegistry);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_personal_chat);
        ChatApplication.getAppComponent().inject(this);

        serverPublicKeyCache.addSubscriber(this);

        stompSubscriptionService.setHandler(handler);

        stompSubscriptionService.subscribeToServerMessagePublicKeyDestination();

        initializeUIElements();

        Intent causeIntent = getIntent();
        String chatPartnerUsername = causeIntent.getStringExtra("chatPartnerUsername");
        chatPartnerUsernameTextView.setText(chatPartnerUsername);

        setupMessageChunkService(chatPartnerUsername);

        initializeImagePickingActivityLauncher(chatPartnerUsername);

        setupBackArrowButtonClickHandling();

        setupImageSendingButtonClickHandling();

        setupMessageViewScrollHandling(chatPartnerUsername);

        setupAdapters();

        stompSubscriptionService.subscribeToSingleMessageReceivingDestination();
        stompSubscriptionService.subscribeToSingleMessageChunkReceivingDestination();

        messageService.sendServerMessagePublicKeyRequest();

        log.info("BEFORE CALLING conversationNeedMessageFetching, chatPartnerUsername = {}", chatPartnerUsername);

        if (messageRegistry.conversationNeedsInitialMessageFetching(chatPartnerUsername)) {
            fetchInitialMessages(chatPartnerUsername);
        } else {
            displayMessages(chatPartnerUsername);
            scrollToBottomOfConversation();
        }

    }

    private void initializeUIElements() {

        backArrowButton = findViewById(R.id.personal_chat_back_button);
        chatPartnerUsernameTextView = findViewById(R.id.personal_chat_chat_partner_username);
        chatMessagesView = findViewById(R.id.personal_chat_messages);
        imageSendingButton = findViewById(R.id.personal_chat_image_sending_button);
        messageInputBox = findViewById(R.id.personal_chat_message_input_box);
        messageSendingButton = findViewById(R.id.personal_chat_message_sending_button);

        linearLayoutManager = new LinearLayoutManager(this);
        chatMessagesView.setLayoutManager(linearLayoutManager);

    }

    private void setupMessageChunkService(String chatPartnerUsername) {
        PersonalChatMessageSingleUpdateListener messageSingleListener = receivedMessage -> {
            displayMessages(chatPartnerUsername);
            scrollToBottomOfConversation();
            numberOfMessagesInConversationBeforeFetching++;
        };
        messageChunkService.setHandler(handler);
        messageChunkService.setPersonalChatMessageSingleUpdateListener(messageSingleListener);
    }

    private void initializeImagePickingActivityLauncher(String chatPartnerUsername) {
        imagePickingActivityLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                uri -> {
                    if (Objects.nonNull(uri)) {
                        byte[] imageBytes = getImageFromURI(uri);
                        log.info("RAW BYTES = {}", imageBytes.length);
                        byte[] compressedBytes = compressionService.compressBytesWithGzip(imageBytes);
                        log.info("COMPRESSED BYTES = {}", compressedBytes.length);
                        String encodedImage = Base64.getEncoder().encodeToString(compressedBytes);
                        log.info("ENCODED IMAGE = {}", encodedImage.length());
                        String mimeType = getContentResolver().getType(uri);
                        log.info("MIME TYPE OF IMAGE TO SEND = {}", mimeType);
                        Optional<MessageContentType> messageContentTypeOptional = MessageContentType.of(mimeType);
                        messageContentTypeOptional.ifPresentOrElse(messageContentType -> {
                            log.info("IMAGE WILL BE SENT, MESSAGE CONTENT TYPE = {}", messageContentType);
                            sendMessage(encodedImage, messageContentType, chatPartnerUsername);
                        }, () -> {
                            log.info("The mime type of the selected image is not supported!");
                        });
                    }
                }
        );
    }

    private void setupBackArrowButtonClickHandling() {

        backArrowButton.setOnClickListener(view -> {
            Intent intent = new Intent(this, ChatListActivity.class);
            startActivity(intent);
        });

    }

    private void setupImageSendingButtonClickHandling() {

        imageSendingButton.setOnClickListener(view -> {
            imagePickingActivityLauncher.launch("image/*");
        });

    }

    private void setupMessageSendingButtonClickHandling(String chatPartnerUsername) {

        messageSendingButton.setOnClickListener(view -> {
            if (!isEditTextEmpty(messageInputBox)) {
                sendMessage(messageInputBox.getText().toString(), MessageContentType.TEXT, chatPartnerUsername);
                messageInputBox.setText(null);
            }
        });

    }

    private void setupMessageViewScrollHandling(String chatPartnerUsername) {

        chatMessagesView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);
                if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                    LinearLayoutManager linearLayoutManager = (LinearLayoutManager) recyclerView.getLayoutManager();
                    if (Objects.nonNull(linearLayoutManager)) {
                        int firstCurrentlyVisibleItemPosition = linearLayoutManager.findFirstVisibleItemPosition();
                        if (firstCurrentlyVisibleItemPosition == 0 && !messageRegistry.conversationHasNoPreviousMessages(chatPartnerUsername)) {
                            fetchFurtherMessagesWhenScrollingUp(chatPartnerUsername);
                        }
                    }
                }
            }
        });

    }

    private void setupAdapters() {

        personalChatAdapter = new PersonalChatAdapter(this, new ArrayList<>(), userSession);
        chatMessagesView.setAdapter(personalChatAdapter);

    }

    private byte[] getImageFromURI(Uri imageURI) {
        try (InputStream inputStream = getContentResolver().openInputStream(imageURI);
             ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                byteArrayOutputStream.write(buffer, 0, bytesRead);
            }
            return byteArrayOutputStream.toByteArray();
        } catch (IOException e) {
            log.error("There was an error while trying to read the into bytes, reason: {}", e.getMessage());
            return new byte[] {};
        }
    }

    private void sendMessage(String message, MessageContentType messageContentType, String chatPartnerUsername) {

        log.info("sendMessage");

        PersonalChatMessageSingleUpdateListener messageSingleListener = receivedMessage -> {
            displayMessages(chatPartnerUsername);
            scrollToBottomOfConversation();
            numberOfMessagesInConversationBeforeFetching++;
        };

        stompSubscriptionService.setPersonalChatMessageSingleUpdateListener(messageSingleListener);
        messageService.sendMessageToChatPartner(message, messageContentType, chatPartnerUsername);
        chatPartnerRegistry.addChatPartnerToRegistry(new ChatPartner(chatPartnerUsername));
        displayMessages(chatPartnerUsername);
        scrollToBottomOfConversation();

    }

    private void fetchFurtherMessagesWhenScrollingUp(String chatPartnerUsername) {

        log.info("fetchFurtherMessagesWhenScrollingUp");
        log.info("chatPartnerUsername = {}", chatPartnerUsername);

        PersonalChatMessageBulkUpdateListener messageBulkListener = messages -> {
            log.info("IN messageBulkListener, chatPartnerUsername = {}", chatPartnerUsername);
            displayMessages(chatPartnerUsername);
            scrollToTopMessageBeforeFurtherFetching();
            numberOfMessagesInConversationBeforeFetching = messageRegistry.getNumberOfAlreadyFetchedMessagesForChatParner(chatPartnerUsername);
        };

        stompSubscriptionService.setPersonalChatMessageBulkUpdateListener(messageBulkListener);
        messageService.initiateFetchingLastMessagesForConversation(
                messageRegistry.getNumberOfAlreadyFetchedMessagesForChatParner(chatPartnerUsername),
                NUMBER_OF_MESSAGES_TO_FETCH_AT_ONCE,
                userSession.getUsername(),
                chatPartnerUsername
        );

    }

    private void fetchInitialMessages(String chatPartnerUsername) {

        log.info("fetchMessages");
        log.info("chatPartnerUsername = {}", chatPartnerUsername);

        PersonalChatMessageBulkUpdateListener messageBulkListener = messages -> {
            log.info("IN messageBulkListener, chatPartnerUsername = {}", chatPartnerUsername);
            displayMessages(chatPartnerUsername);
            scrollToBottomOfConversation();
            numberOfMessagesInConversationBeforeFetching = messageRegistry.getNumberOfAlreadyFetchedMessagesForChatParner(chatPartnerUsername);
        };

        stompSubscriptionService.subscribeToConversationPartFetchingDestination(messageBulkListener);
        messageService.initiateFetchingLastMessagesForConversation(
                messageRegistry.getNumberOfAlreadyFetchedMessagesForChatParner(chatPartnerUsername),
                NUMBER_OF_MESSAGES_TO_FETCH_AT_ONCE,
                userSession.getUsername(),
                chatPartnerUsername
        );

    }

    private void scrollToTopMessageBeforeFurtherFetching() {
        int firstVisiblePosition = linearLayoutManager.findFirstVisibleItemPosition();
        View firstVisibleView = linearLayoutManager.findViewByPosition(firstVisiblePosition);
        int offset = (firstVisibleView != null) ? firstVisibleView.getTop() : 0;
        linearLayoutManager.scrollToPositionWithOffset(firstVisiblePosition + messageRegistry.getNumberOfAlreadyFetchedMessagesForChatParner(chatPartnerUsernameTextView.getText().toString()) - numberOfMessagesInConversationBeforeFetching, offset);
    }

    private void scrollToBottomOfConversation() {
        chatMessagesView.scrollToPosition(personalChatAdapter.getItemCount() - 1);
    }

    private void displayMessages(String chatPartnerUsername) {
        personalChatAdapter.replaceChatMessages(messageRegistry.getMessagesForChatPartner(chatPartnerUsername));
        personalChatAdapter.notifyDataSetChanged();
    }

    private boolean isEditTextEmpty(EditText editText) {
        return editText.getText().toString().trim().isEmpty();
    }

    @Override
    public void handleMessagePublicKeyChange() {
        setupMessageSendingButtonClickHandling(chatPartnerUsernameTextView.getText().toString());
    }

}