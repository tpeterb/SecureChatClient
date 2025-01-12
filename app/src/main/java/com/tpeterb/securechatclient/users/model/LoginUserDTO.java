package com.tpeterb.securechatclient.users.model;

import lombok.Value;

@Value
public class LoginUserDTO extends UserActionDTO {

    String username;

    String password;

    @Override
    public String toString() {
        return "LoginUserDTO{" +
                "username='" + username + '\'' +
                '}';
    }

}