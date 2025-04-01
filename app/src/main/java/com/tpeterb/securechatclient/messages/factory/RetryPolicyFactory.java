package com.tpeterb.securechatclient.messages.factory;

import com.tpeterb.securechatclient.exception.WebSocketConnectionAttemptFailureException;
import com.tpeterb.securechatclient.messages.config.WebSocketConnectionConfig;

import net.jodah.failsafe.RetryPolicy;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.CompletionException;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public final class RetryPolicyFactory {

    private RetryPolicyFactory() {}

    public static RetryPolicy<Object> createWebSocketRetryPolicy(WebSocketConnectionConfig webSocketConnectionConfig) {
        return new RetryPolicy<>()
                .withBackoff(
                        webSocketConnectionConfig.getRetryPolicy().getMinRetryBackoffInSeconds(),
                        webSocketConnectionConfig.getRetryPolicy().getMaxRetryBackoffInSeconds(),
                        ChronoUnit.SECONDS,
                        webSocketConnectionConfig.getRetryPolicy().getDelayFactor())
                .withMaxAttempts(webSocketConnectionConfig.getRetryPolicy().getMaxRetryAttempts())
                .withMaxDuration(Duration.of(webSocketConnectionConfig.getRetryPolicy().getTimeoutInMinutes(), ChronoUnit.MINUTES))
                .withJitter(webSocketConnectionConfig.getRetryPolicy().getJitterFactor())
                .onRetry(objectExecutionAttemptedEvent ->
                        log.info("Retrying connecting to the server via websocket")
                )
                .onRetriesExceeded(objectExecutionCompletedEvent ->
                        log.error("Exhausted max retry attempts for connecting to server via websocket after {} attempts", objectExecutionCompletedEvent.getAttemptCount())
                )
                .onFailure(objectExecutionCompletedEvent ->
                        log.error("Failed to create websocket connection with server after {} retry attempts, reason: {}", objectExecutionCompletedEvent.getAttemptCount(), objectExecutionCompletedEvent.getFailure().getMessage())
                )
                .onSuccess(objectExecutionCompletedEvent ->
                        log.info("Successfully finished retrying connecting to the server via websocket!")
                )
                .handle(WebSocketConnectionAttemptFailureException.class)
                .handle(CompletionException.class);
    }

}
