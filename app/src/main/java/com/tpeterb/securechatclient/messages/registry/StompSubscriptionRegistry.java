package com.tpeterb.securechatclient.messages.registry;

import static java.util.Map.*;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

import javax.inject.Inject;
import javax.inject.Singleton;

import io.reactivex.disposables.Disposable;

@Singleton
public class StompSubscriptionRegistry {

    private final Map<String, Disposable> subscriptionRegistry;

    @Inject
    public StompSubscriptionRegistry() {
        subscriptionRegistry = new ConcurrentHashMap<>();
    }

    public synchronized void clearStompSubscriptionRegistry() {
        for (Entry<String, Disposable> entry : subscriptionRegistry.entrySet()) {
            entry.getValue().dispose();
        }
        subscriptionRegistry.clear();
    }

    public synchronized boolean isSubscriptionRegistered(String subscriptionDestination) {
        return Objects.nonNull(subscriptionRegistry.get(subscriptionDestination));
    }

    public void addSubscription(String subscriptionDestination, Disposable disposable) {
        subscriptionRegistry.put(subscriptionDestination, disposable);
    }

    @Override
    public String toString() {
        return "SubscriptionRegistry = " + subscriptionRegistry;
    }

}
