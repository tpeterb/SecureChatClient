package com.tpeterb.securechatclient.users.service;

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
    Call<RegistrationResponse> registerUser(@Body RegisterUserDTO registerUserDTO);

    @POST("api/login")
    Call<LoginResponse> loginUser(@Body LoginUserDTO loginUserDTO);

    @GET("api/usernames/{username}")
    Call<List<String>> getUsernamesForSearchedUsername(@Path("username") String searchedUsername);

    @GET("api/chat-partners/{username}")
    Call<List<ChatPartner>> getChatPartnersForUsername(@Path("username") String username);

}
