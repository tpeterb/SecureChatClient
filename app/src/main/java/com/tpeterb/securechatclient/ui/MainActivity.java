package com.tpeterb.securechatclient.ui;

import static com.tpeterb.securechatclient.security.model.KeyExchangeResult.*;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.LiveData;

import com.tpeterb.securechatclient.R;
import com.tpeterb.securechatclient.application.ChatApplication;
import com.tpeterb.securechatclient.security.cache.ServerPublicKeyCache;
import com.tpeterb.securechatclient.security.cache.SessionKeyCache;
import com.tpeterb.securechatclient.security.config.SecurityConfig;
import com.tpeterb.securechatclient.security.model.KeyExchangeResult;
import com.tpeterb.securechatclient.security.service.EllipticCurveDiffieHellmanKeyExchangeService;
import com.tpeterb.securechatclient.security.service.ServerPublicKeyApiServiceProvider;

import java.util.Objects;

import javax.inject.Inject;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class MainActivity extends AppCompatActivity {

    @Inject
    EllipticCurveDiffieHellmanKeyExchangeService ellipticCurveDiffieHellmanKeyExchangeService;

    @Inject
    ServerPublicKeyApiServiceProvider serverPublicKeyApiServiceProvider;

    @Inject
    SecurityConfig securityConfig;

    @Inject
    SessionKeyCache sessionKeyCache;

    @Inject
    ServerPublicKeyCache serverPublicKeyCache;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ChatApplication.getAppComponent().inject(this);

        initializeUIElements();

        if (Objects.isNull(serverPublicKeyCache.getServerPublicKeyForDigitalSignatures())) {
            log.info("NINCS MEG A SZERVER ALÁÍRÁSOS KULCSA! LEKÉRJÜK MAJD KULCSCSERE!");
            retrieveServerPublicKeyForDigitalSignatures();
        } else {
            log.info("MEGVAN A SZERVER ALÁÍRÁSOS KULCSA! KULCSCSERE!");
            initiateKeyExchangeWithServerWithRetries(0);
        };

    }

    private void initializeUIElements() {

        Button registerButton = findViewById(R.id.welcome_screen_register_button);
        Button loginButton = findViewById(R.id.welcome_screen_login_button);

        registerButton.setOnClickListener(view -> {
            Intent intent = new Intent(MainActivity.this, RegisterActivity.class);
            startActivity(intent);
        });

        loginButton.setOnClickListener(view -> {
            Intent intent = new Intent(MainActivity.this, LoginActivity.class);
            startActivity(intent);
        });

    }

    private void retrieveServerPublicKeyForDigitalSignatures() {
        serverPublicKeyApiServiceProvider.retrieveServerPublicKeyForDigitalSignatures().observe(this, serverDigitalSignaturePublicKeyResponseDTO -> {
            log.info("In MainActivity, server dig sig pk = {}", serverDigitalSignaturePublicKeyResponseDTO.getServerDigitalSignaturePublicKey());
            serverPublicKeyCache.setServerPublicKeyForDigitalSignatures(serverDigitalSignaturePublicKeyResponseDTO.getServerDigitalSignaturePublicKey());
            initiateKeyExchangeWithServerWithRetries(0);
        });
    }

    private void initiateKeyExchangeWithServerWithRetries(int retryCount) {
        if (retryCount > securityConfig.getKeyExchangeRetryAttempts()) {
            return;
        }
        int nextRetryCount = retryCount + 1;
        if (Objects.isNull(sessionKeyCache.getSessionKey())) {
            LiveData<KeyExchangeResult> keyExchangeResult = ellipticCurveDiffieHellmanKeyExchangeService.initiateKeyExchangeWithServerBeforeLogin(this);
            keyExchangeResult.observe(this, result -> {
                if (result != SUCCESS) {
                    log.info("NEM SIKERÜLT A KULCSCSERE, ÚJRAPRÓBÁL!");
                    initiateKeyExchangeWithServerWithRetries(nextRetryCount);
                }
            });
        }
    }

}