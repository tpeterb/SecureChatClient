package com.tpeterb.securechatclient.messages.config;

import javax.inject.Inject;
import javax.inject.Singleton;

import lombok.Value;

@Singleton
@Value
public class WebSocketConnectionConfig {

    RetryPolicy retryPolicy;

    CircuitBreaker circuitBreaker;

    @Inject
    public WebSocketConnectionConfig() {
        retryPolicy = new RetryPolicy();
        circuitBreaker = new CircuitBreaker();
    }

    @Value
    public static class RetryPolicy {

        int minRetryBackoffInSeconds = 1;

        int maxRetryBackoffInSeconds = 60;

        int maxRetryAttempts = Integer.MAX_VALUE;

        int timeoutInMinutes = 60;

        int delayFactor = 2;

        double jitterFactor = 0.2;

    }

    @Value
    public static class CircuitBreaker {

        int failureThreshold = 10;

        int delayInSeconds = 60;

        int successThreshold = 1;

    }

}
