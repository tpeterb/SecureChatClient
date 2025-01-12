package com.tpeterb.securechatclient.messages.delivery;

import static com.tpeterb.securechatclient.constants.Constants.CHAT_SERVER_MESSAGE_RECEIPT_ACKNOWLEDGEMENT_DESTINATION;
import static com.tpeterb.securechatclient.constants.Constants.CHAT_SERVER_WEBSOCKET_BASE_URL;
import static com.tpeterb.securechatclient.constants.Constants.MESSAGE_BULK_FETCHING_GENERAL_DESTINATION;
import static com.tpeterb.securechatclient.constants.Constants.SINGLE_MESSAGE_RECEIVING_GENERAL_DESTINATION;

import android.os.Handler;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tpeterb.securechatclient.messages.config.WebSocketConnectionConfig;
import com.tpeterb.securechatclient.messages.factory.CircuitBreakerFactory;
import com.tpeterb.securechatclient.messages.factory.RetryPolicyFactory;
import com.tpeterb.securechatclient.messages.listener.PersonalChatMessageBulkUpdateListener;
import com.tpeterb.securechatclient.messages.listener.PersonalChatMessageSingleUpdateListener;
import com.tpeterb.securechatclient.messages.model.AcknowledgementDTO;
import com.tpeterb.securechatclient.messages.model.ConversationPartFetchResponseDTO;
import com.tpeterb.securechatclient.messages.model.MessageDTO;
import com.tpeterb.securechatclient.messages.registry.ChatPartnerRegistry;
import com.tpeterb.securechatclient.messages.registry.MessageRegistry;
import com.tpeterb.securechatclient.messages.registry.StompSubscriptionRegistry;
import com.tpeterb.securechatclient.users.model.ChatPartner;
import com.tpeterb.securechatclient.users.session.UserSession;

import net.jodah.failsafe.CircuitBreaker;
import net.jodah.failsafe.Failsafe;
import net.jodah.failsafe.RetryPolicy;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import javax.inject.Inject;
import javax.inject.Singleton;

import io.reactivex.disposables.Disposable;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import ua.naiksoftware.stomp.Stomp;
import ua.naiksoftware.stomp.StompClient;
import ua.naiksoftware.stomp.dto.StompHeader;

@Slf4j
@Singleton
public class WebSocketClient {

    private final StompSubscriptionRegistry stompSubscriptionRegistry;

    private final UserSession userSession;

    private final MessageRegistry messageRegistry;

    private final ChatPartnerRegistry chatPartnerRegistry;

    private final ObjectMapper objectMapper;

    private final WebSocketConnectionConfig webSocketConnectionConfig;

    private final RetryPolicy<Object> webSocketRetryPolicy;

    private final CircuitBreaker<Object> webSocketCircuitBreaker;

    private StompClient stompClient;

    private boolean isConnected;

    @Setter
    private Handler handler;

    @Setter
    private PersonalChatMessageBulkUpdateListener personalChatMessageBulkUpdateListener;

    @Setter
    private PersonalChatMessageSingleUpdateListener personalChatMessageSingleUpdateListener;

    @Inject
    public WebSocketClient(ObjectMapper objectMapper, UserSession userSession, MessageRegistry messageRegistry, ChatPartnerRegistry chatPartnerRegistry, StompSubscriptionRegistry stompSubscriptionRegistry, WebSocketConnectionConfig webSocketConnectionConfig) {
        this.objectMapper = objectMapper;
        this.userSession = userSession;
        this.messageRegistry = messageRegistry;
        this.chatPartnerRegistry = chatPartnerRegistry;
        this.stompSubscriptionRegistry = stompSubscriptionRegistry;
        this.webSocketConnectionConfig = webSocketConnectionConfig;
        isConnected = false;
        configureStompClient();
        webSocketRetryPolicy = RetryPolicyFactory.createWebSocketRetryPolicy(webSocketConnectionConfig);
        webSocketCircuitBreaker = CircuitBreakerFactory.createWebSocketCircuitBreaker(webSocketConnectionConfig);
    }

    public void connectToServerWithFailureHandling() {
        Failsafe.with(webSocketCircuitBreaker, webSocketRetryPolicy).run(this::connectToServer);
    }

    public void subscribeToSingleMessageReceivingDestination() {

        String loggedInUsername = userSession.getUsername();
        String subscriptionDestination = SINGLE_MESSAGE_RECEIVING_GENERAL_DESTINATION + loggedInUsername;
        if (!stompSubscriptionRegistry.isSubscriptionRegistered(subscriptionDestination)) {
            Disposable disposable = stompClient.topic(subscriptionDestination).subscribe(stompMessage -> {
                String jsonMessage = stompMessage.getPayload();
                MessageDTO message = objectMapper.readValue(jsonMessage, MessageDTO.class);
                String messageSenderUsername = message.getSender();
                chatPartnerRegistry.addChatPartnerToRegistry(new ChatPartner(messageSenderUsername));
                messageRegistry.addMessageToRegistry(messageSenderUsername, message);
                Collections.sort(messageRegistry.getMessagesForChatPartner(messageSenderUsername));
                log.info("Received message, message = {}", message);
                if (Objects.nonNull(handler)) {
                    handler.post(() -> {
                        personalChatMessageSingleUpdateListener.onNewMessage(message);
                    });
                }
                sendAcknowledgementForMessage(message.getId());
            }, throwable -> {
                log.error("There was an error while trying to subscribe to message receiving destination, reason: {}", throwable.getMessage());
            });
            stompSubscriptionRegistry.addSubscription(subscriptionDestination, disposable);
        } else {
            log.info("Subscription has already been registered!");
        }
    }

    public void subscribeToConversationPartFetchingDestination(PersonalChatMessageBulkUpdateListener personalChatMessageBulkUpdateListener) {

        String subscriptionDestination = MESSAGE_BULK_FETCHING_GENERAL_DESTINATION + userSession.getUsername();
        this.personalChatMessageBulkUpdateListener = personalChatMessageBulkUpdateListener;
        if (!stompSubscriptionRegistry.isSubscriptionRegistered(subscriptionDestination)) {
            Disposable disposable = stompClient.topic(subscriptionDestination)
                    .subscribe(stompMessage -> {
                        log.info("subscribeToConversationPartFetchingDestination callback");
                        String jsonMessageList = stompMessage.getPayload();
                        ConversationPartFetchResponseDTO conversationPartFetchResponseDTO = objectMapper.readValue(jsonMessageList, new TypeReference<ConversationPartFetchResponseDTO>() {});
                        List<MessageDTO> messages = conversationPartFetchResponseDTO.getMessages();
                        messageRegistry.addAllMessagesToRegistry(conversationPartFetchResponseDTO.getChatPartnerUsername(), messages);
                        Collections.sort(messageRegistry.getMessagesForChatPartner(conversationPartFetchResponseDTO.getChatPartnerUsername()));
                        log.info("Received conversation part, messages = {}", messages);
                        if (handler != null) {
                            handler.post(() -> {
                                this.personalChatMessageBulkUpdateListener.onNewMessages(messages);
                            });
                        }
                        sendAcknowledgementForMessage(conversationPartFetchResponseDTO.getConversationPartId());
                    }, throwable -> {
                        log.error("There was an error while trying to subscribe to conversation fetching destination, reason: {}", throwable.getMessage());
                    });
            stompSubscriptionRegistry.addSubscription(subscriptionDestination, disposable);
        } else {
            log.info("Subscription already exists!");
        }

    }

    public void sendMessage(String message, String destination) {

        var observable = stompClient.send(destination, message).subscribe(() -> {
            log.info("Message was successfully sent!");
        }, throwable -> {
            log.error("There was an error while sending the message, reason: {}", throwable.getMessage());
        });

    }

    public boolean isConnected() {
        return isConnected;
    }

    private void connectToServer() {

        List<StompHeader> headers = new ArrayList<>();
        headers.add(new StompHeader("username", userSession.getUsername()));

        stompClient.connect(headers);

    }

    private void configureStompClient() {
        stompClient = Stomp.over(Stomp.ConnectionProvider.OKHTTP, CHAT_SERVER_WEBSOCKET_BASE_URL);

        Disposable connectionDisposable = stompClient.lifecycle().subscribe(lifecycleEvent -> {
            switch (lifecycleEvent.getType()) {
                case OPENED:
                    log.info("Successfully connected via websocket to server!");
                    isConnected = true;
                    break;
                case CLOSED:
                    log.info("Websocket connection successfully closed!");
                    isConnected = false;
                    connectToServerWithFailureHandling();
                    break;
                case ERROR:
                    log.error("There was an error while trying to create websocket connection with the server, reason: {}", lifecycleEvent.getException().getMessage());
                    break;
            }
        });
    }

    private void sendAcknowledgementForMessage(String messageId) {

        AcknowledgementDTO acknowledgement = new AcknowledgementDTO(messageId);
        String jsonAcknowledgement;
        try {
            jsonAcknowledgement = objectMapper.writeValueAsString(acknowledgement);
        } catch (JsonProcessingException e) {
            log.error("There was an error while trying to serialize message acknowledgement! Reason: {}", e.getMessage());
            return;
        }
        sendMessage(jsonAcknowledgement, CHAT_SERVER_MESSAGE_RECEIPT_ACKNOWLEDGEMENT_DESTINATION);

    }

}
