package com.tpeterb.securechatclient.users.service.factory;

import com.tpeterb.securechatclient.users.api.LoginResponse;
import com.tpeterb.securechatclient.users.api.RegistrationResponse;
import com.tpeterb.securechatclient.users.api.UserActionResponse;
import com.tpeterb.securechatclient.users.model.LoginUserDTO;
import com.tpeterb.securechatclient.users.model.RegisterUserDTO;
import com.tpeterb.securechatclient.users.model.UserActionDTO;
import com.tpeterb.securechatclient.users.service.ChatServerApiService;

import javax.inject.Inject;

import retrofit2.Call;

public class CallFactory {

    private final ChatServerApiService chatServerApiService;

    @Inject
    public CallFactory(ChatServerApiService chatServerApiService) {
        this.chatServerApiService = chatServerApiService;
    }

    public <T extends UserActionResponse, U extends UserActionDTO> Call<T> createCall(Class<T> userActionResponseClass, U userDTO) {
        if (userActionResponseClass.equals(RegistrationResponse.class)) {
            return (Call<T>) chatServerApiService.registerUser((RegisterUserDTO) userDTO);
        } else if (userActionResponseClass.equals(LoginResponse.class)) {
            return (Call<T>) chatServerApiService.loginUser((LoginUserDTO) userDTO);
        }
        return null;
    }

}
