package com.tpeterb.securechatclient.users.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;

import lombok.Value;

@Value
public class ChatPartner {

    String username;

    @JsonCreator
    public ChatPartner(@JsonProperty("username") String username) {
        this.username = username;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof ChatPartner)) {
            return false;
        }
        ChatPartner chatPartner = (ChatPartner) other;
        return username.equals(chatPartner.username);
    }

    @Override
    public int hashCode() {
        return Objects.hash(username);
    }

}
