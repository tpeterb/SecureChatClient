package com.tpeterb.securechatclient.users.session;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class UserSession {

    private String username;

    @Inject
    public UserSession() {}

    public synchronized void clearUserSession() {
        username = null;
    }

    public synchronized String getUsername() {
        return username;
    }

    public synchronized void setUsername(String username) {
        this.username = username;
    }

}
