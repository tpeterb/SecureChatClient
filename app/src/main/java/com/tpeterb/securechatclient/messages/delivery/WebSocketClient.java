package com.tpeterb.securechatclient.messages.delivery;

import static com.tpeterb.securechatclient.constants.Constants.CHAT_SERVER_FULL_MESSAGE_RECEIPT_ACKNOWLEDGEMENT_DESTINATION;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tpeterb.securechatclient.messages.config.WebSocketConnectionConfig;
import com.tpeterb.securechatclient.messages.factory.CircuitBreakerFactory;
import com.tpeterb.securechatclient.messages.factory.RetryPolicyFactory;
import com.tpeterb.securechatclient.messages.model.FullMessageAcknowledgementDTO;
import com.tpeterb.securechatclient.users.session.UserSession;

import net.jodah.failsafe.CircuitBreaker;
import net.jodah.failsafe.Failsafe;
import net.jodah.failsafe.RetryPolicy;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

import io.reactivex.disposables.Disposable;
import lombok.extern.slf4j.Slf4j;
import ua.naiksoftware.stomp.StompClient;
import ua.naiksoftware.stomp.dto.StompHeader;

@Slf4j
@Singleton
public class WebSocketClient {

    private final UserSession userSession;

    private final ObjectMapper objectMapper;

    private final RetryPolicy<Object> webSocketRetryPolicy;

    private final CircuitBreaker<Object> webSocketCircuitBreaker;

    private StompClient stompClient;

    private volatile boolean isConnected;

    @Inject
    public WebSocketClient(ObjectMapper objectMapper,
                           UserSession userSession,
                           WebSocketConnectionConfig webSocketConnectionConfig,
                           StompClient stompClient) {
        this.objectMapper = objectMapper;
        this.userSession = userSession;
        this.stompClient = stompClient;
        isConnected = false;
        configureStompClient();
        webSocketRetryPolicy = RetryPolicyFactory.createWebSocketRetryPolicy(webSocketConnectionConfig);
        webSocketCircuitBreaker = CircuitBreakerFactory.createWebSocketCircuitBreaker(webSocketConnectionConfig);
    }

    public void connectToServerWithFailureHandling() {
        log.info("Initiating connection to server with failure handling");

        Failsafe.with(webSocketCircuitBreaker, webSocketRetryPolicy)
                .onSuccess(result -> {
                    log.info("Successfully established WebSocket connection");
                    isConnected = true;
                })
                .onFailure(failure -> {
                    log.error("Failed to establish WebSocket connection after retries, {}", failure);
                    isConnected = false;
                })
                .run(() -> {
                    connectToServer();
                });
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
        Disposable connectionDisposable = stompClient.lifecycle().subscribe(lifecycleEvent -> {
            switch (lifecycleEvent.getType()) {
                case OPENED:
                    log.info("WebSocket connection opened successfully");
                    isConnected = true;
                    break;
                case CLOSED:
                    log.info("WebSocket connection closed!");
                    isConnected = false;
                    connectToServerWithFailureHandling();
                    break;
                case ERROR:
                    log.error("WebSocket connection error: {}",
                            lifecycleEvent.getException().getMessage());
                    isConnected = false;
                    break;
            }
        });
    }

}
