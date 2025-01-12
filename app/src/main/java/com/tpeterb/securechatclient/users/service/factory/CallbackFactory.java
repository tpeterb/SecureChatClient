package com.tpeterb.securechatclient.users.service.factory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tpeterb.securechatclient.users.api.LoginResponse;
import com.tpeterb.securechatclient.users.api.RegistrationResponse;
import com.tpeterb.securechatclient.users.api.UserActionResponse;
import com.tpeterb.securechatclient.users.config.UserConfig;
import com.tpeterb.securechatclient.users.model.LoginUserDTO;
import com.tpeterb.securechatclient.users.model.RegisterUserDTO;
import com.tpeterb.securechatclient.users.model.UserAction;
import com.tpeterb.securechatclient.users.model.UserActionDTO;
import com.tpeterb.securechatclient.users.model.UserLoginResult;
import com.tpeterb.securechatclient.users.model.UserRegistrationResult;
import com.tpeterb.securechatclient.users.service.ChatServerApiService;

import java.io.IOException;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import javax.inject.Inject;

import lombok.extern.slf4j.Slf4j;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

@Slf4j
public class CallbackFactory {

    private final ObjectMapper objectMapper;

    private final ChatServerApiService chatServerApiService;

    private final UserConfig userConfig;

    private final ScheduledExecutorService scheduledExecutorService = Executors.newScheduledThreadPool(1);

    @Inject
    public CallbackFactory(ObjectMapper objectMapper, ChatServerApiService chatServerApiService, UserConfig userConfig) {
        this.objectMapper = objectMapper;
        this.chatServerApiService = chatServerApiService;
        this.userConfig = userConfig;
    }

    public <T, U> Callback<? extends UserActionResponse> createCallback(UserAction userAction, T userActionDTO, CompletableFuture<U> completableFuture, AtomicInteger attemptCount) {
        switch (userAction) {
            case REGISTRATION:
                return createRegistrationCallback((RegisterUserDTO) userActionDTO, (CompletableFuture<UserRegistrationResult>) completableFuture, attemptCount);
            case LOGIN:
                return createLoginCallback((LoginUserDTO) userActionDTO, (CompletableFuture<UserLoginResult>) completableFuture, attemptCount);
        };
        return null;
    }

    private Callback<RegistrationResponse> createRegistrationCallback(RegisterUserDTO registerUserDTO, CompletableFuture<UserRegistrationResult> registrationResult, AtomicInteger executedRegistrationAttempts) {
        return new Callback<RegistrationResponse>() {

            @Override
            public void onResponse(Call<RegistrationResponse> call, Response<RegistrationResponse> response) {
                if (response.isSuccessful() && Objects.nonNull(response.body())) {
                    log.info("Response body: {}", response.body());
                    registrationResult.complete(UserRegistrationResult.SUCCESS);
                } else if (!response.isSuccessful() && Objects.nonNull(response.errorBody())){
                    RegistrationResponse registrationResponse = null;
                    try {
                        String errorBody = response.errorBody().string();
                        registrationResponse = objectMapper.readValue(errorBody, RegistrationResponse.class);
                    } catch (JsonProcessingException e) {
                        log.error("Could not deserialize error body of registration response, reason: {}", e.getMessage());
                        registrationResult.complete(UserRegistrationResult.GENERAL_FAILURE);
                        return;
                    } catch (IOException e) {
                        log.error("Some IO Error occurred while trying to deserialize registration response! Reason: {}", e.getMessage());
                        registrationResult.complete(UserRegistrationResult.GENERAL_FAILURE);
                        return;
                    }
                    log.error("Registration failed: {}", registrationResponse.getResponse());
                    registrationResult.complete(registrationResponse.getUserRegistrationResult());
                } else {
                    log.error("Registration failed!");
                    registrationResult.complete(UserRegistrationResult.GENERAL_FAILURE);
                }
            }

            @Override
            public void onFailure(Call<RegistrationResponse> call, Throwable t) {
                log.error("Registration request failed", t);
                if (registrationResult.isDone()) {
                    return;
                }
                int attempts = executedRegistrationAttempts.incrementAndGet();
                if (attempts < userConfig.getRegistrationRetryAttempts()) {
                    scheduledExecutorService.schedule(() -> {
                        Call<RegistrationResponse> registrationCall = chatServerApiService.registerUser(registerUserDTO);
                        registrationCall.enqueue(this);
                    }, userConfig.getRegistrationRetryBackoffMs(), TimeUnit.MILLISECONDS);
                } else {
                    log.info("Reached the max {} retry attempts for registration, stopped retying!", userConfig.getRegistrationRetryAttempts());
                    registrationResult.complete(UserRegistrationResult.GENERAL_FAILURE);
                }
            }

        };
    }

    private Callback<LoginResponse> createLoginCallback(LoginUserDTO loginUserDTO, CompletableFuture<UserLoginResult> userLoginResult, AtomicInteger executedLoginAttempts) {
        return new Callback<LoginResponse>() {

            @Override
            public void onResponse(Call<LoginResponse> call, Response<LoginResponse> response) {
                if (response.isSuccessful()) {
                    log.info("Login was successful!");
                    userLoginResult.complete(UserLoginResult.SUCCESS);
                    return;
                }
                if (Objects.nonNull(response.errorBody())) {
                    LoginResponse loginResponse = null;
                    try {
                        String errorBody = response.errorBody().string();
                        loginResponse = objectMapper.readValue(errorBody, LoginResponse.class);
                    } catch (JsonProcessingException e) {
                        log.error("Could not deserialize login response, reason: {}", e.getMessage());
                        userLoginResult.complete(UserLoginResult.FAILURE);
                    } catch (IOException e) {
                        log.error("An IO problem occurred while processing the login response, reason: {}", e.getMessage());
                        userLoginResult.complete(UserLoginResult.FAILURE);
                    }
                    log.error("Login failed, reason: {}", loginResponse.getResponse());
                    userLoginResult.complete(loginResponse.getUserLoginResult());
                }
            }

            @Override
            public void onFailure(Call<LoginResponse> call, Throwable throwable) {
                log.error("Login attempt failed, reason: {}", throwable.getMessage());
                if (userLoginResult.isDone()) {
                    return;
                }
                int loginAttempts = executedLoginAttempts.incrementAndGet();
                if (loginAttempts < userConfig.getLoginRetryAttempts()) {
                    scheduledExecutorService.schedule(() -> {
                        Call<LoginResponse> loginResponseCall = chatServerApiService.loginUser(loginUserDTO);
                        loginResponseCall.enqueue(this);
                    }, userConfig.getLoginRetryBackoffMs(), TimeUnit.MILLISECONDS);
                } else {
                    log.error("Reached the max {} retry attemps for logging in, stopped retrying!", userConfig.getLoginRetryAttempts());
                    userLoginResult.complete(UserLoginResult.FAILURE);
                }
            }

        };
    }

}
