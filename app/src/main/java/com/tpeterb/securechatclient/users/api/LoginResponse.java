package com.tpeterb.securechatclient.users.api;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.tpeterb.securechatclient.users.model.UserLoginResult;

import lombok.Value;

@Value
public class LoginResponse extends UserActionResponse {

    UserLoginResult userLoginResult;

    @JsonCreator
    public LoginResponse(@JsonProperty("userLoginResult") UserLoginResult userLoginResult,
                         @JsonProperty("response") String response) {
        super(response);
        this.userLoginResult = userLoginResult;
    }

}
