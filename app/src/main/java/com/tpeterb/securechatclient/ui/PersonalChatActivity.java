package com.tpeterb.securechatclient.ui;

import static com.tpeterb.securechatclient.constants.Constants.NUMBER_OF_MESSAGES_TO_FETCH_AT_ONCE;

import android.content.Context;
import android.content.Intent;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.tpeterb.securechatclient.R;
import com.tpeterb.securechatclient.application.ChatApplication;
import com.tpeterb.securechatclient.messages.delivery.WebSocketClient;
import com.tpeterb.securechatclient.messages.listener.PersonalChatMessageBulkUpdateListener;
import com.tpeterb.securechatclient.messages.listener.PersonalChatMessageSingleUpdateListener;
import com.tpeterb.securechatclient.messages.model.MessageDTO;
import com.tpeterb.securechatclient.messages.registry.ChatPartnerRegistry;
import com.tpeterb.securechatclient.messages.registry.MessageRegistry;
import com.tpeterb.securechatclient.messages.registry.StompSubscriptionRegistry;
import com.tpeterb.securechatclient.messages.service.MessageService;
import com.tpeterb.securechatclient.ui.adapter.PersonalChatAdapter;
import com.tpeterb.securechatclient.users.model.ChatPartner;
import com.tpeterb.securechatclient.users.session.UserSession;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Objects;
import java.util.UUID;

import javax.inject.Inject;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class PersonalChatActivity extends AppCompatActivity {

    private Handler handler = new Handler(Looper.getMainLooper());

    private ImageView backArrowButton;

    private TextView chatPartnerUsernameTextView;

    private PersonalChatAdapter personalChatAdapter;

    private RecyclerView chatMessagesView;

    private EditText messageInputBox;

    private ImageView messageSendingButton;

    private LinearLayoutManager linearLayoutManager;

    private int numberOfMessagesInConversationBeforeFetching = 0;

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

        webSocketClient.setHandler(handler);

        initializeUIElements();

        Intent causeIntent = getIntent();
        String chatPartnerUsername = causeIntent.getStringExtra("chatPartnerUsername");
        chatPartnerUsernameTextView.setText(chatPartnerUsername);

        setupBackArrowButtonClickHandling();

        setupMessageSendingButtonClickHandling(chatPartnerUsername);

        setupMessageViewScrollHandling(chatPartnerUsername);

        setupAdapters();

        log.info("BEFORE CALLING conversationNeedMessageFetching, chatPartnerUsername = {}", chatPartnerUsername);

        if (messageRegistry.conversationNeedsInitialMessageFetching(chatPartnerUsername)) {
            fetchInitialMessages(chatPartnerUsername);
        } else {
            displayMessages(chatPartnerUsername);
            scrollToBottomOfConversation();
        }
        webSocketClient.subscribeToSingleMessageReceivingDestination();

    }

    private void initializeUIElements() {

        backArrowButton = findViewById(R.id.personal_chat_back_button);
        chatPartnerUsernameTextView = findViewById(R.id.personal_chat_chat_partner_username);
        chatMessagesView = findViewById(R.id.personal_chat_messages);
        messageInputBox = findViewById(R.id.personal_chat_message_input_box);
        messageSendingButton = findViewById(R.id.personal_chat_message_sending_button);

        linearLayoutManager = new LinearLayoutManager(this);
        chatMessagesView.setLayoutManager(linearLayoutManager);

    }

    private void setupBackArrowButtonClickHandling() {

        backArrowButton.setOnClickListener(view -> {
            Intent intent = new Intent(this, ChatListActivity.class);
            startActivity(intent);
        });

    }

    private void setupMessageSendingButtonClickHandling(String chatPartnerUsername) {

        messageSendingButton.setOnClickListener(view -> {
            if (!isEditTextEmpty(messageInputBox)) {
                sendMessage(messageInputBox.getText().toString(), chatPartnerUsername);
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

    private void sendMessage(String message, String chatPartnerUsername) {

        log.info("sendMessage");

        PersonalChatMessageSingleUpdateListener messageSingleListener = receivedMessage -> {
            displayMessages(chatPartnerUsername);
            scrollToBottomOfConversation();
            numberOfMessagesInConversationBeforeFetching++;
        };

        webSocketClient.setPersonalChatMessageSingleUpdateListener(messageSingleListener);
        messageService.sendMessageToChatPartner(message, chatPartnerUsername);
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

        webSocketClient.setPersonalChatMessageBulkUpdateListener(messageBulkListener);
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

        webSocketClient.subscribeToConversationPartFetchingDestination(messageBulkListener);
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

}