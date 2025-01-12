package com.tpeterb.securechatclient.users.service;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tpeterb.securechatclient.users.api.LoginResponse;
import com.tpeterb.securechatclient.users.api.RegistrationResponse;
import com.tpeterb.securechatclient.users.api.UserActionResponse;
import com.tpeterb.securechatclient.users.config.UserConfig;
import com.tpeterb.securechatclient.users.model.ChatPartner;
import com.tpeterb.securechatclient.users.model.LoginUserDTO;
import com.tpeterb.securechatclient.users.model.RegisterUserDTO;
import com.tpeterb.securechatclient.users.model.UserAction;
import com.tpeterb.securechatclient.users.model.UserActionDTO;
import com.tpeterb.securechatclient.users.model.UserLoginResult;
import com.tpeterb.securechatclient.users.model.UserRegistrationResult;
import com.tpeterb.securechatclient.users.service.factory.CallFactory;
import com.tpeterb.securechatclient.users.service.factory.CallbackFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import javax.inject.Inject;
import javax.inject.Singleton;

import lombok.extern.slf4j.Slf4j;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.http.Body;

@Slf4j
@Singleton
public class ChatServerApiServiceProvider {

    private final ChatServerApiService chatServerApiService;

    private final CallbackFactory callbackFactory;

    private final CallFactory callFactory;

    private final UserConfig userConfig;

    private final ScheduledExecutorService scheduledExecutorService = Executors.newScheduledThreadPool(1);

    @Inject
    public ChatServerApiServiceProvider(ChatServerApiService chatServerApiService, CallbackFactory callbackFactory, CallFactory callFactory, UserConfig userConfig) {
        this.chatServerApiService = chatServerApiService;
        this.callbackFactory = callbackFactory;
        this.callFactory = callFactory;
        this.userConfig = userConfig;
    }

    public LiveData<List<ChatPartner>> getChatPartnersForUsername(String username) {
        MutableLiveData<List<ChatPartner>> liveData = new MutableLiveData<>();
        Call<List<ChatPartner>> chatPartnersCall = chatServerApiService.getChatPartnersForUsername(username);
        chatPartnersCall.enqueue(new Callback<List<ChatPartner>>() {
            @Override
            public void onResponse(Call<List<ChatPartner>> call, Response<List<ChatPartner>> response) {
                if (response.isSuccessful() && Objects.nonNull(response.body())) {
                    log.info("Chat partners were successfully fetched!");
                    liveData.postValue(response.body());
                } else {
                    log.info("Failed to fetch chat partners, response code = {}, message = {}", response.code(), response.message());
                    if (Objects.nonNull(response.body())) {
                        log.info("Chat partners response body = {}", response.body());
                    }
                    liveData.postValue(Collections.emptyList());
                }
            }

            @Override
            public void onFailure(Call<List<ChatPartner>> call, Throwable throwable) {
                if (Objects.nonNull(throwable)) {
                    log.error("There was an error while trying to fetch the chat partners! Reason: {}", throwable.getMessage());
                } else {
                    log.error("There was an error while trying to fetch the chat partners!");
                }
                liveData.postValue(Collections.emptyList());
            }
        });
        return liveData;
    }

    public LiveData<List<String>> getUsernamesForSearchedUsername(String searchedUsername) {
        MutableLiveData<List<String>> liveData = new MutableLiveData<>();
        Call<List<String>> usersCall = chatServerApiService.getUsernamesForSearchedUsername(searchedUsername);
        usersCall.enqueue(new Callback<List<String>>() {
            @Override
            public void onResponse(Call<List<String>> call, Response<List<String>> response) {
                if (response.isSuccessful() && Objects.nonNull(response.body())) {
                    liveData.postValue(response.body());
                } else {
                    liveData.postValue(Collections.emptyList());
                }
            }

            @Override
            public void onFailure(Call<List<String>> call, Throwable throwable) {
                liveData.postValue(Collections.emptyList());
            }
        });
        return liveData;
    }

    public CompletableFuture<UserRegistrationResult> registerUser(RegisterUserDTO registerUserDTO) {

        return executeUserAction(UserAction.REGISTRATION, UserRegistrationResult.GENERAL_FAILURE, RegistrationResponse.class, userConfig.getRegistrationMaxWaitMs(), registerUserDTO);

    }

    public CompletableFuture<UserLoginResult> loginUser(LoginUserDTO loginUserDTO) {

        return executeUserAction(UserAction.LOGIN, UserLoginResult.FAILURE, LoginResponse.class, userConfig.getLoginMaxWaitMs(), loginUserDTO);

    }

    private <T extends Enum<T>, U extends UserActionResponse, V extends UserActionDTO> CompletableFuture<T> executeUserAction(UserAction userAction, T userActionFailureResult, Class<U> userActionResponseClass, int maxWaitMs, V userDTO) {

        final AtomicInteger executedUserAttempts = new AtomicInteger(0);

        CompletableFuture<T> userActionResult = new CompletableFuture<>();

        scheduledExecutorService.schedule(() -> {
            if (!userActionResult.isDone()) {
                log.error("User action attempts timed out!");
                userActionResult.complete(userActionFailureResult);
            }
        }, maxWaitMs, TimeUnit.MILLISECONDS);

        Call<U> userActionCall = callFactory.createCall(userActionResponseClass, userDTO);
        Callback<U> userActionCallback = (Callback<U>) callbackFactory.createCallback(userAction, userDTO, userActionResult, executedUserAttempts);

        userActionCall.enqueue(userActionCallback);
        return userActionResult;

    }

}
