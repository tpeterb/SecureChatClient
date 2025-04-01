package com.tpeterb.securechatclient.users.session;

import com.tpeterb.securechatclient.messages.service.StompSubscriptionService;

import javax.inject.Inject;
import javax.inject.Singleton;

import lombok.extern.slf4j.Slf4j;

@Singleton
@Slf4j
public class UserSession {

    private volatile String username;

    private volatile String sessionId;

    @Inject
    public UserSession() {

    }

    public synchronized void clearUserSession() {
        username = null;
        sessionId = null;
    }

    public synchronized String getSessionId() {
        return sessionId;
    }

    public synchronized void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public synchronized String getUsername() {
        return username;
    }

    public synchronized void setUsername(String username) {
        this.username = username;
    }

}
