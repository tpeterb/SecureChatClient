package com.tpeterb.securechatclient.users.api;

import lombok.Getter;

@Getter
public abstract class UserActionResponse {

    protected final String response;

    protected UserActionResponse(String response) {
        this.response = response;
    }

}
