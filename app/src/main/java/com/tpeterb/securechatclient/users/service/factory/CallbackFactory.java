package com.tpeterb.securechatclient.users.service.factory;

import static com.tpeterb.securechatclient.constants.Constants.HTTP_STATUS_CODE_FOR_EXPIRED_SESSION_KEY;
import static com.tpeterb.securechatclient.constants.Constants.HTTP_STATUS_CODE_FOR_NON_EXISTENT_SESSION_KEY;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tpeterb.securechatclient.constants.Constants;
import com.tpeterb.securechatclient.security.model.EncryptedPacket;
import com.tpeterb.securechatclient.security.service.PacketEncryptionService;
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
import javax.inject.Singleton;

import lombok.extern.slf4j.Slf4j;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

@Slf4j
@Singleton
public class CallbackFactory {

    private final ObjectMapper objectMapper;

    private final ChatServerApiService chatServerApiService;

    private final PacketEncryptionService packetEncryptionService;

    private final UserConfig userConfig;

    private final ScheduledExecutorService scheduledExecutorService = Executors.newScheduledThreadPool(1);

    @Inject
    public CallbackFactory(ObjectMapper objectMapper,
                           ChatServerApiService chatServerApiService,
                           PacketEncryptionService packetEncryptionService,
                           UserConfig userConfig) {
        this.objectMapper = objectMapper;
        this.chatServerApiService = chatServerApiService;
        this.packetEncryptionService = packetEncryptionService;
        this.userConfig = userConfig;
    }

    public <U> Callback<EncryptedPacket> createCallback(UserAction userAction, EncryptedPacket encryptedPacket, CompletableFuture<U> completableFuture, AtomicInteger attemptCount) {
        switch (userAction) {
            case REGISTRATION:
                return createRegistrationCallback(encryptedPacket, (CompletableFuture<UserRegistrationResult>) completableFuture, attemptCount);
            case LOGIN:
                return createLoginCallback(encryptedPacket, (CompletableFuture<UserLoginResult>) completableFuture, attemptCount);
        };
        return null;
    }

    private Callback<EncryptedPacket> createRegistrationCallback(EncryptedPacket encryptedPacket, CompletableFuture<UserRegistrationResult> registrationResult, AtomicInteger executedRegistrationAttempts) {
        return new Callback<EncryptedPacket>() {
            @Override
            public void onResponse(Call<EncryptedPacket> call, Response<EncryptedPacket> response) {
                if (response.isSuccessful() && Objects.nonNull(response.body())) {
                    log.info("Encrypted packet = {}", response.body());
                    byte[] decryptedPacket = packetEncryptionService.decryptEntirePacket(response.body());
                    log.info("Decrypted packet = {}", decryptedPacket);
                    RegistrationResponse registrationResponse;
                    try {
                        registrationResponse = objectMapper.readValue(decryptedPacket, RegistrationResponse.class);
                    } catch (IOException e) {
                        log.error("Could not deserialize successful registration reply, reason: {}", e.getMessage());
                        registrationResult.complete(UserRegistrationResult.GENERAL_FAILURE);
                        return;
                    }
                    log.info("Response body: {}", registrationResponse);
                    registrationResult.complete(UserRegistrationResult.SUCCESS);
                } else if (!response.isSuccessful() && Objects.nonNull(response.errorBody())) {
                    if (response.code() == HTTP_STATUS_CODE_FOR_EXPIRED_SESSION_KEY || response.code() == HTTP_STATUS_CODE_FOR_NON_EXISTENT_SESSION_KEY) {
                        log.error("There was an issue with the session key during registration, generating a new one!");
                        registrationResult.complete(UserRegistrationResult.BAD_SESSION_KEY);
                        return;
                    }
                    EncryptedPacket encryptedPacket;
                    try {
                        String errorBody = response.errorBody().string();
                        encryptedPacket = objectMapper.readValue(errorBody, EncryptedPacket.class);
                    } catch (IOException e) {
                        log.error("Could not deserialize encrypted error body of registration response, reason: {}", e.getMessage());
                        registrationResult.complete(UserRegistrationResult.GENERAL_FAILURE);
                        return;
                    }
                    byte[] decryptedPacket = packetEncryptionService.decryptEntirePacket(encryptedPacket);
                    RegistrationResponse registrationResponse;
                    try {
                        registrationResponse = objectMapper.readValue(decryptedPacket, RegistrationResponse.class);
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
            public void onFailure(Call<EncryptedPacket> call, Throwable t) {
                log.error("Registration request failed", t);
                if (registrationResult.isDone()) {
                    return;
                }
                int attempts = executedRegistrationAttempts.incrementAndGet();
                if (attempts < userConfig.getRegistrationRetryAttempts()) {
                    scheduledExecutorService.schedule(() -> {
                        Call<EncryptedPacket> registrationCall = chatServerApiService.registerUser(encryptedPacket);
                        registrationCall.enqueue(this);
                    }, userConfig.getRegistrationRetryBackoffMs(), TimeUnit.MILLISECONDS);
                } else {
                    log.info("Reached the max {} retry attempts for registration, stopped retying!", userConfig.getRegistrationRetryAttempts());
                    registrationResult.complete(UserRegistrationResult.GENERAL_FAILURE);
                }
            }

        };
    }

    private Callback<EncryptedPacket> createLoginCallback(EncryptedPacket encryptedPacket, CompletableFuture<UserLoginResult> userLoginResult, AtomicInteger executedLoginAttempts) {
        return new Callback<EncryptedPacket>() {
            @Override
            public void onResponse(Call<EncryptedPacket> call, Response<EncryptedPacket> response) {
                if (response.isSuccessful()) {
                    log.info("Login was successful!");
                    userLoginResult.complete(UserLoginResult.SUCCESS);
                    return;
                }
                if (Objects.nonNull(response.errorBody())) {
                    if (response.code() == HTTP_STATUS_CODE_FOR_EXPIRED_SESSION_KEY || response.code() == HTTP_STATUS_CODE_FOR_NON_EXISTENT_SESSION_KEY) {
                        log.error("There was an issue with the session key during login, generating a new one!");
                        userLoginResult.complete(UserLoginResult.BAD_SESSION_KEY);
                        return;
                    }
                    EncryptedPacket encryptedPacket;
                    try {
                        String errorBody = response.errorBody().string();
                        encryptedPacket = objectMapper.readValue(errorBody, EncryptedPacket.class);
                    } catch (IOException e) {
                        log.error("Failed to deserialize the error body of the encrypted login response, reason: {}", e.getMessage());
                        userLoginResult.complete(UserLoginResult.FAILURE);
                        return;
                    }
                    byte[] decryptedPacket = packetEncryptionService.decryptEntirePacket(encryptedPacket);
                    LoginResponse loginResponse;
                    try {
                        loginResponse = objectMapper.readValue(decryptedPacket, LoginResponse.class);
                    } catch (JsonProcessingException e) {
                        log.error("Could not deserialize login response, reason: {}", e.getMessage());
                        userLoginResult.complete(UserLoginResult.FAILURE);
                        return;
                    } catch (IOException e) {
                        log.error("An IO problem occurred while processing the login response, reason: {}", e.getMessage());
                        userLoginResult.complete(UserLoginResult.FAILURE);
                        return;
                    }
                    log.error("Login failed, reason: {}", loginResponse.getResponse());
                    userLoginResult.complete(loginResponse.getUserLoginResult());
                }
            }

            @Override
            public void onFailure(Call<EncryptedPacket> call, Throwable throwable) {
                log.error("Login attempt failed, reason: {}", throwable.getMessage());
                if (userLoginResult.isDone()) {
                    return;
                }
                int loginAttempts = executedLoginAttempts.incrementAndGet();
                if (loginAttempts < userConfig.getLoginRetryAttempts()) {
                    scheduledExecutorService.schedule(() -> {
                        Call<EncryptedPacket> loginResponseCall = chatServerApiService.loginUser(encryptedPacket);
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
