package com.tpeterb.securechatclient.module;

import static com.tpeterb.securechatclient.constants.Constants.CHAT_SERVER_BASE_URL;
import static com.tpeterb.securechatclient.constants.Constants.CHAT_SERVER_WEBSOCKET_BASE_URL;
import static com.tpeterb.securechatclient.constants.Constants.SHARED_PREFERENCE_NAME;

import android.content.Context;

import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKey;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.tpeterb.securechatclient.exception.SessionKeyStorageInitializationException;
import com.tpeterb.securechatclient.security.service.AsymmetricCipherService;
import com.tpeterb.securechatclient.security.service.ChaCha20Service;
import com.tpeterb.securechatclient.security.service.DigitalSignatureService;
import com.tpeterb.securechatclient.security.service.EdDSAService;
import com.tpeterb.securechatclient.security.service.RSAService;
import com.tpeterb.securechatclient.security.service.ServerKeyExchangeApiService;
import com.tpeterb.securechatclient.security.service.ServerPublicKeyApiService;
import com.tpeterb.securechatclient.security.service.SymmetricCipherService;
import com.tpeterb.securechatclient.users.service.ChatServerApiService;

import java.io.IOException;
import java.security.GeneralSecurityException;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import lombok.extern.slf4j.Slf4j;
import retrofit2.Retrofit;
import retrofit2.converter.jackson.JacksonConverterFactory;
import ua.naiksoftware.stomp.Stomp;
import ua.naiksoftware.stomp.StompClient;

@Module
@Slf4j
public class AppModule {

    private final Context context;

    public AppModule(Context context) {
        this.context = context;
    }

    @Provides
    @Singleton
    public Context provideContext() {
        return context;
    }

    @Provides
    @Singleton
    public StompClient provideStompClient() {
        return Stomp.over(Stomp.ConnectionProvider.OKHTTP, CHAT_SERVER_WEBSOCKET_BASE_URL);
    }

    @Provides
    @Singleton
    public ObjectMapper provideObjectMapper() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        return objectMapper;
    }

    @Provides
    @Singleton
    public Retrofit provideRetrofit() {
        return new Retrofit.Builder()
                .baseUrl(CHAT_SERVER_BASE_URL)
                .addConverterFactory(JacksonConverterFactory.create())
                .build();
    }

    @Provides
    @Singleton
    public ChatServerApiService provideChatServerApiService() {
        return provideRetrofit().create(ChatServerApiService.class);
    }

    @Provides
    @Singleton
    public ServerKeyExchangeApiService provideServerKeyExchangeApiService() {
        return provideRetrofit().create(ServerKeyExchangeApiService.class);
    }

    @Provides
    @Singleton
    public ServerPublicKeyApiService provideServerPublicKeyApiService() {
        return provideRetrofit().create(ServerPublicKeyApiService.class);
    }

    @Provides
    @Singleton
    public AsymmetricCipherService provideAsymmetricCipherService() {
        return new RSAService();
    }

    @Provides
    @Singleton
    public SymmetricCipherService provideSymmetricCipherService() {
        return new ChaCha20Service();
    }

    @Provides
    @Singleton
    public DigitalSignatureService provideDigitalSignatureService() {
        return new EdDSAService();
    }

    @Provides
    @Singleton
    public EncryptedSharedPreferences provideEncryptedSharedPreferences() {
        MasterKey masterKey;
        try {
            masterKey = new MasterKey.Builder(context)
                    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                    .build();
            return (EncryptedSharedPreferences) EncryptedSharedPreferences.create(
                    context,
                    SHARED_PREFERENCE_NAME,
                    masterKey,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            );
        } catch (GeneralSecurityException | IOException e) {
            log.error("Failed to initialize session key cache, reason: {}", e.getMessage());
            throw new SessionKeyStorageInitializationException(e.getMessage());
        }
    }

}
