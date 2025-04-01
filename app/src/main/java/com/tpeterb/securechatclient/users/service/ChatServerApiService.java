package com.tpeterb.securechatclient.users.service;

import com.tpeterb.securechatclient.security.model.EncryptedPacket;
import com.tpeterb.securechatclient.users.api.LoginResponse;
import com.tpeterb.securechatclient.users.api.RegistrationResponse;
import com.tpeterb.securechatclient.users.model.ChatPartner;
import com.tpeterb.securechatclient.users.model.LoginUserDTO;
import com.tpeterb.securechatclient.users.model.RegisterUserDTO;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;

public interface ChatServerApiService {

    @POST("api/register")
    Call<EncryptedPacket> registerUser(@Body EncryptedPacket encryptedPacket);

    @POST("api/login")
    Call<EncryptedPacket> loginUser(@Body EncryptedPacket encryptedPacket);

    @POST("api/search/usernames")
    Call<EncryptedPacket> getUsernamesForSearchedUsername(@Body EncryptedPacket encryptedPacket);

    @POST("api/search/chat-partners")
    Call<EncryptedPacket> getChatPartnersForUsername(@Body EncryptedPacket encryptedPacket);

}
