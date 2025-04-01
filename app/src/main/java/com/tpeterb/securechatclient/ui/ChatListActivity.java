package com.tpeterb.securechatclient.ui;

import static com.tpeterb.securechatclient.constants.Constants.NEW_KEY_EXCHANGE_SIGNALING_GENERAL_ENDPOINT;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ListView;
import android.widget.SearchView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.LiveData;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.tpeterb.securechatclient.R;
import com.tpeterb.securechatclient.application.ChatApplication;
import com.tpeterb.securechatclient.messages.delivery.WebSocketClient;
import com.tpeterb.securechatclient.messages.registry.ChatPartnerRegistry;
import com.tpeterb.securechatclient.messages.registry.StompSubscriptionRegistry;
import com.tpeterb.securechatclient.messages.service.StompSubscriptionService;
import com.tpeterb.securechatclient.security.cache.DigitalSignatureKeyPairCache;
import com.tpeterb.securechatclient.security.config.SecurityConfig;
import com.tpeterb.securechatclient.security.model.KeyExchangeResult;
import com.tpeterb.securechatclient.security.service.EllipticCurveDiffieHellmanKeyExchangeService;
import com.tpeterb.securechatclient.ui.adapter.ChatListAdapter;
import com.tpeterb.securechatclient.ui.adapter.CustomSearchAdapter;
import com.tpeterb.securechatclient.users.model.ChatPartner;
import com.tpeterb.securechatclient.users.service.UserService;
import com.tpeterb.securechatclient.users.session.UserSession;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import javax.inject.Inject;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ChatListActivity extends AppCompatActivity {

    private boolean isLoadedForTheFirstTime = true;

    @Inject
    UserService userService;

    @Inject
    UserSession userSession;

    @Inject
    WebSocketClient webSocketClient;

    @Inject
    DigitalSignatureKeyPairCache digitalSignatureKeyPairCache;

    @Inject
    SecurityConfig securityConfig;

    @Inject
    EllipticCurveDiffieHellmanKeyExchangeService ellipticCurveDiffieHellmanKeyExchangeService;

    @Inject
    ChatPartnerRegistry chatPartnerRegistry;

    @Inject
    StompSubscriptionRegistry stompSubscriptionRegistry;

    @Inject
    StompSubscriptionService stompSubscriptionService;

    private TextView brandNameTextView;

    private SearchView userSearchBarView;

    private FrameLayout frameLayout;

    private ListView userSearchResultsView;

    private RecyclerView conversationsView;

    private CustomSearchAdapter searchAdapter;

    private ChatListAdapter chatListAdapter;

    @Override
    protected void onResume() {

        super.onResume();

        if (!isLoadedForTheFirstTime) {
            resetSearchBoxAndResults();
        }

        replaceChatList();

        isLoadedForTheFirstTime = false;

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        log.info("onCreate in ChatListActivity started!");
        ChatApplication.getAppComponent().inject(this);
        setContentView(R.layout.activity_chat_list);

        initializeUIElements();

        log.info("After initializeUIElements");

        setupAdapters();

        log.info("After setupAdapters");

        setupClickListenersForHidingSearchResults();

        log.info("After setupClickListenersForHidingSearchResults");

        setupSearchBarTextListener();

        log.info("After setupSearchBarTextListener");

        if (!chatPartnerRegistry.isInitialFetchDone()) {
            loadChatUsers();
        }

        log.info("After loadChatUsers");

        if (!webSocketClient.isConnected()) {
            webSocketClient.connectToServerWithFailureHandling();
        }

        log.info("After connectToServer");

    }

    private void initializeUIElements() {

        brandNameTextView = findViewById(R.id.chat_list_page_app_name);
        frameLayout = findViewById(R.id.chat_list_page_frame_layout);
        userSearchBarView = findViewById(R.id.chat_list_page_user_search_bar);
        userSearchResultsView = findViewById(R.id.chat_list_page_search_results);
        conversationsView = findViewById(R.id.chat_list_page_chat_list);

        conversationsView.setLayoutManager(new LinearLayoutManager(this));

    }

    private void setupSearchBarTextListener() {

        userSearchBarView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String newText) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                if (newText.isEmpty()) {
                    searchAdapter.clear();
                    searchAdapter.notifyDataSetChanged();
                    userSearchResultsView.setVisibility(View.GONE);
                } else {
                    log.info("Starting the search for usernames containing the string {}", newText);
                    searchForUsernames(newText);
                }
                return true;
            }
        });

    }

    private void setupClickListenersForHidingSearchResults() {

        frameLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                resetSearchBoxAndResults();
            }
        });

        brandNameTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                resetSearchBoxAndResults();
            }
        });

        conversationsView.setOnClickListener(view -> {
            resetSearchBoxAndResults();
        });

    }

    private void resetSearchBoxAndResults() {
        userSearchBarView.clearFocus();
        if (userSearchResultsView.getVisibility() == View.VISIBLE) {
            userSearchResultsView.setVisibility(View.GONE);
        }
    }

    private void setupAdapters() {

        searchAdapter = new CustomSearchAdapter(this, searchedUsername -> {
            Intent intent = new Intent(this, PersonalChatActivity.class);
            intent.putExtra("chatPartnerUsername", searchedUsername);
            startActivity(intent);
        });
        userSearchResultsView.setAdapter(searchAdapter);

        chatListAdapter = new ChatListAdapter(this, new ArrayList<>(), chatPartner -> {
            Intent intent = new Intent(this, PersonalChatActivity.class);
            intent.putExtra("chatPartnerUsername", chatPartner.getUsername());
            startActivity(intent);
        });
        conversationsView.setAdapter(chatListAdapter);

    }

    private void searchForUsernames(String searchedUsername) {
        userService.getUsernamesForSearchedUsername(searchedUsername).observe(this, usernamesOptional -> {
            if (Objects.isNull(usernamesOptional) || usernamesOptional.isEmpty()) {
                userSearchBarView.setOnQueryTextListener(null);
                stompSubscriptionRegistry.removeSubscription(NEW_KEY_EXCHANGE_SIGNALING_GENERAL_ENDPOINT + userSession.getSessionId());
                initiateKeyExchangeWithServerWithRetries(0, () -> {
                    stompSubscriptionService.subscribeToNewKeyExchangeSignalingDestination();
                    setupSearchBarTextListener();
                });
            }
            if (Objects.nonNull(usernamesOptional) && usernamesOptional.isPresent()) {
                List<String> usernames = usernamesOptional.get();
                log.info("Fetched usernames = {}", usernames);
                searchAdapter.clear();
                searchAdapter.addAll(usernames);
                searchAdapter.notifyDataSetChanged();
                userSearchResultsView.setVisibility(View.VISIBLE);
            }
        });
    }

    private void initiateKeyExchangeWithServerWithRetries(int retryCount, Runnable successfulKeyExchangeTask) {
        if (retryCount > securityConfig.getKeyExchangeRetryAttempts()) {
            handleFailedKeyExchange();
            return;
        }
        int nextRetryCount = retryCount + 1;
        LiveData<KeyExchangeResult> keyExchangeLiveData = ellipticCurveDiffieHellmanKeyExchangeService.initiateKeyExchangeWithServerAfterLogin(this);
        keyExchangeLiveData.observe(this, keyExchangeResult -> {
            if (keyExchangeResult == KeyExchangeResult.SUCCESS) {
                successfulKeyExchangeTask.run();
            } else {
                initiateKeyExchangeWithServerWithRetries(nextRetryCount, successfulKeyExchangeTask);
            }
        });
    }

    private void handleFailedKeyExchange() {
        userSession.clearUserSession();
        digitalSignatureKeyPairCache.clearDigitalSignatureKeyPairCache();
        redirectToMainActivity();
    }

    private void redirectToMainActivity() {
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
    }

    private void replaceChatList() {
        chatListAdapter.replaceChatPartners(chatPartnerRegistry.getChatPartnerRegistry());
        chatListAdapter.notifyDataSetChanged();
    }

    private void loadChatUsers() {
        userService.getChatPartnersForUsername(userSession.getUsername()).observe(this, chatPartnersOptional -> {
            if (Objects.isNull(chatPartnersOptional) || chatPartnersOptional.isEmpty()) {
                stompSubscriptionRegistry.removeSubscription(NEW_KEY_EXCHANGE_SIGNALING_GENERAL_ENDPOINT + userSession.getSessionId());
                initiateKeyExchangeWithServerWithRetries(0, () -> {
                    stompSubscriptionService.subscribeToNewKeyExchangeSignalingDestination();
                    loadChatUsers();
                });
            }
            if (Objects.nonNull(chatPartnersOptional) && chatPartnersOptional.isPresent()) {
                List<ChatPartner> chatPartners = chatPartnersOptional.get();
                log.info("Fetched chat partners = {}", chatPartners);
                chatPartnerRegistry.addAllChatPartners(chatPartners);
                chatPartnerRegistry.setInitialFetchDone(true);
                chatListAdapter.replaceChatPartners(chatPartnerRegistry.getChatPartnerRegistry());
                chatListAdapter.notifyDataSetChanged();
            }
        });
    }

}