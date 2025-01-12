package com.tpeterb.securechatclient.messages.factory;

import com.tpeterb.securechatclient.messages.config.WebSocketConnectionConfig;

import net.jodah.failsafe.CircuitBreaker;

import java.time.Duration;
import java.time.temporal.ChronoUnit;

public final class CircuitBreakerFactory {

    private CircuitBreakerFactory() {}

    public static CircuitBreaker<Object> createWebSocketCircuitBreaker(WebSocketConnectionConfig webSocketConnectionConfig) {
        return new CircuitBreaker<>()
                .withFailureThreshold(webSocketConnectionConfig.getCircuitBreaker().getFailureThreshold())
                .withDelay(Duration.of(webSocketConnectionConfig.getCircuitBreaker().getDelayInSeconds(), ChronoUnit.SECONDS))
                .withSuccessThreshold(webSocketConnectionConfig.getCircuitBreaker().getSuccessThreshold());
    }

}
