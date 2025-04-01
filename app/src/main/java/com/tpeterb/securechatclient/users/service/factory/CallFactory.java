package com.tpeterb.securechatclient.users.service.factory;

import com.tpeterb.securechatclient.security.model.EncryptedPacket;
import com.tpeterb.securechatclient.users.api.LoginResponse;
import com.tpeterb.securechatclient.users.api.RegistrationResponse;
import com.tpeterb.securechatclient.users.api.UserActionResponse;
import com.tpeterb.securechatclient.users.model.LoginUserDTO;
import com.tpeterb.securechatclient.users.model.RegisterUserDTO;
import com.tpeterb.securechatclient.users.model.UserActionDTO;
import com.tpeterb.securechatclient.users.service.ChatServerApiService;

import javax.inject.Inject;
import javax.inject.Singleton;

import retrofit2.Call;

@Singleton
public class CallFactory {

    private final ChatServerApiService chatServerApiService;

    @Inject
    public CallFactory(ChatServerApiService chatServerApiService) {
        this.chatServerApiService = chatServerApiService;
    }

    public <T extends UserActionResponse> Call<EncryptedPacket> createCall(Class<T> userActionResponseClass, EncryptedPacket encryptedPacket) {
        if (userActionResponseClass.equals(RegistrationResponse.class)) {
            return chatServerApiService.registerUser(encryptedPacket);
        } else if (userActionResponseClass.equals(LoginResponse.class)) {
            return chatServerApiService.loginUser(encryptedPacket);
        }
        return null;
    }

}
