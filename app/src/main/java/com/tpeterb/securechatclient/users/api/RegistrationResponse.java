package com.tpeterb.securechatclient.users.api;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.tpeterb.securechatclient.users.model.UserRegistrationResult;

import lombok.Value;

@Value
public class RegistrationResponse extends UserActionResponse {

    UserRegistrationResult userRegistrationResult;

    @JsonCreator
    public RegistrationResponse(@JsonProperty("userRegistrationResult") UserRegistrationResult userRegistrationResult,
                                @JsonProperty("response") String response) {
        super(response);
        this.userRegistrationResult = userRegistrationResult;
    }

}
