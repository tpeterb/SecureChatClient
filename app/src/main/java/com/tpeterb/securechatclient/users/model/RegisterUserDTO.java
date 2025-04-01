package com.tpeterb.securechatclient.users.model;

import lombok.Value;

@Value
public class RegisterUserDTO extends UserActionDTO {

    String username;

    String password;

    String email;

    byte[] digitalSignaturePublicKeyOfUser;

    @Override
    public String toString() {
        return "UserDTO{" +
                "username='" + getUsername() + '\'' +
                ", email='" + getEmail() + '\'' +
                '}';
    }

}
