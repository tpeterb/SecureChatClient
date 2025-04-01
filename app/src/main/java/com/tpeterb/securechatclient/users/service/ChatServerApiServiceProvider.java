package com.tpeterb.securechatclient.users.service;

import static com.tpeterb.securechatclient.constants.Constants.HTTP_STATUS_CODE_FOR_EXPIRED_SESSION_KEY;
import static com.tpeterb.securechatclient.constants.Constants.HTTP_STATUS_CODE_FOR_NON_EXISTENT_SESSION_KEY;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tpeterb.securechatclient.security.model.EncryptedPacket;
import com.tpeterb.securechatclient.security.service.EllipticCurveDiffieHellmanKeyExchangeService;
import com.tpeterb.securechatclient.security.service.PacketEncryptionService;
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

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
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
public class ChatServerApiServiceProvider {

    private final ChatServerApiService chatServerApiService;

    private final CallbackFactory callbackFactory;

    private final CallFactory callFactory;

    private final PacketEncryptionService packetEncryptionService;

    private final UserConfig userConfig;

    private final EllipticCurveDiffieHellmanKeyExchangeService ellipticCurveDiffieHellmanKeyExchangeService;

    private final ScheduledExecutorService scheduledExecutorService = Executors.newScheduledThreadPool(1);

    private final ObjectMapper objectMapper;

    @Inject
    public ChatServerApiServiceProvider(ChatServerApiService chatServerApiService,
                                        CallbackFactory callbackFactory,
                                        CallFactory callFactory,
                                        PacketEncryptionService packetEncryptionService,
                                        UserConfig userConfig,
                                        EllipticCurveDiffieHellmanKeyExchangeService ellipticCurveDiffieHellmanKeyExchangeService,
                                        ObjectMapper objectMapper) {
        this.chatServerApiService = chatServerApiService;
        this.callbackFactory = callbackFactory;
        this.callFactory = callFactory;
        this.packetEncryptionService = packetEncryptionService;
        this.userConfig = userConfig;
        this.ellipticCurveDiffieHellmanKeyExchangeService = ellipticCurveDiffieHellmanKeyExchangeService;
        this.objectMapper = objectMapper;
    }

    public LiveData<Optional<List<ChatPartner>>> getChatPartnersForUsername(EncryptedPacket encryptedPacket) {
        MutableLiveData<Optional<List<ChatPartner>>> liveData = new MutableLiveData<>();
        Call<EncryptedPacket> chatPartnersCall = chatServerApiService.getChatPartnersForUsername(encryptedPacket);
        chatPartnersCall.enqueue(new Callback<EncryptedPacket>() {
            @Override
            public void onResponse(Call<EncryptedPacket> call, Response<EncryptedPacket> response) {
                if (response.isSuccessful() && Objects.nonNull(response.body())) {
                    log.info("Chat partners were successfully fetched!");
                    byte[] decryptedPacket = packetEncryptionService.decryptEntirePacket(response.body());
                    List<ChatPartner> chatPartners;
                    try {
                        chatPartners = objectMapper.readValue(decryptedPacket, new TypeReference<List<ChatPartner>>() {});
                    } catch (IOException e) {
                        log.error("Failed to deserialize the list of chat partners, reason: {}", e.getMessage());
                        liveData.postValue(Optional.of(Collections.emptyList()));
                        return;
                    }
                    log.info("DESERIALIZED chat partners = {}", chatPartners);
                    liveData.postValue(Optional.of(chatPartners));
                } else {
                    log.info("Failed to fetch chat partners, response code = {}, message = {}", response.code(), response.message());
                    if (Objects.nonNull(response.body())) {
                        log.info("Chat partners response body = {}", response.body());
                    }
                    if (response.code() == HTTP_STATUS_CODE_FOR_EXPIRED_SESSION_KEY || response.code() == HTTP_STATUS_CODE_FOR_NON_EXISTENT_SESSION_KEY) {
                        log.error("There was an error while searching for chat partners with the session key, generating a new one!");
                        liveData.postValue(Optional.empty());
                        return;
                    }
                    liveData.postValue(Optional.of(Collections.emptyList()));
                }
            }

            @Override
            public void onFailure(Call<EncryptedPacket> call, Throwable throwable) {
                if (Objects.nonNull(throwable)) {
                    log.error("There was an error while trying to fetch the chat partners! Reason: {}", throwable.getMessage());
                } else {
                    log.error("There was an error while trying to fetch the chat partners!");
                }
                liveData.postValue(Optional.of(Collections.emptyList()));
            }
        });
        return liveData;
    }

    public LiveData<Optional<List<String>>> getUsernamesForSearchedUsername(EncryptedPacket encryptedPacket) {
        MutableLiveData<Optional<List<String>>> liveData = new MutableLiveData<>();
        Call<EncryptedPacket> usersCall = chatServerApiService.getUsernamesForSearchedUsername(encryptedPacket);
        usersCall.enqueue(new Callback<EncryptedPacket>() {
            @Override
            public void onResponse(Call<EncryptedPacket> call, Response<EncryptedPacket> response) {
                if (response.isSuccessful() && Objects.nonNull(response.body())) {
                    byte[] decryptedPacket = packetEncryptionService.decryptEntirePacket(response.body());
                    List<String> searchedUsernames;
                    try {
                        searchedUsernames = objectMapper.readValue(decryptedPacket, new TypeReference<List<String>>() {});
                    } catch (IOException e) {
                        log.error("Couldn't deserialize username search results, reason: {}", e.getMessage());
                        liveData.postValue(Optional.of(Collections.emptyList()));
                        return;
                    }
                    liveData.postValue(Optional.of(searchedUsernames));
                } else {
                    if (response.code() == HTTP_STATUS_CODE_FOR_EXPIRED_SESSION_KEY || response.code() == HTTP_STATUS_CODE_FOR_NON_EXISTENT_SESSION_KEY) {
                        log.error("There was an error while searching for usernames with the session key, generating a new one!");
                        liveData.postValue(Optional.empty());
                        return;
                    }
                    liveData.postValue(Optional.of(Collections.emptyList()));
                }
            }

            @Override
            public void onFailure(Call<EncryptedPacket> call, Throwable throwable) {
                if (Objects.nonNull(throwable)) {
                    log.error("Failed to fetch username search result, reason: {}", throwable.getMessage());
                }
                liveData.postValue(Optional.of(Collections.emptyList()));
            }
        });
        return liveData;
    }

    public CompletableFuture<UserRegistrationResult> registerUser(EncryptedPacket encryptedPacket) {
        return executeUserAction(UserAction.REGISTRATION, UserRegistrationResult.GENERAL_FAILURE, RegistrationResponse.class, userConfig.getRegistrationMaxWaitMs(), encryptedPacket);
    }

    public CompletableFuture<UserLoginResult> loginUser(EncryptedPacket encryptedPacket) {
        return executeUserAction(UserAction.LOGIN, UserLoginResult.FAILURE, LoginResponse.class, userConfig.getLoginMaxWaitMs(), encryptedPacket);
    }

    private <T extends Enum<T>, U extends UserActionResponse> CompletableFuture<T> executeUserAction(UserAction userAction, T userActionFailureResult, Class<U> userActionResponseClass, int maxWaitMs, EncryptedPacket encryptedPacket) {

        final AtomicInteger executedUserAttempts = new AtomicInteger(0);

        CompletableFuture<T> userActionResult = new CompletableFuture<>();

        scheduledExecutorService.schedule(() -> {
            if (!userActionResult.isDone()) {
                log.error("User action attempts timed out!");
                userActionResult.complete(userActionFailureResult);
            }
        }, maxWaitMs, TimeUnit.MILLISECONDS);

        Call<EncryptedPacket> userActionCall = callFactory.createCall(userActionResponseClass, encryptedPacket);
        Callback<EncryptedPacket> userActionCallback = callbackFactory.createCallback(userAction, encryptedPacket, userActionResult, executedUserAttempts);

        userActionCall.enqueue(userActionCallback);
        return userActionResult;

    }

}
