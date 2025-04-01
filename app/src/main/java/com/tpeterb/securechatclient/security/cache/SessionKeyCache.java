package com.tpeterb.securechatclient.security.cache;

import androidx.security.crypto.EncryptedSharedPreferences;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Objects;

import javax.inject.Inject;
import javax.inject.Singleton;

import lombok.extern.slf4j.Slf4j;

@Singleton
@Slf4j
public class SessionKeyCache {

    private static final String SESSION_KEY = "session_key";

    private volatile byte[] sessionKey;

    private final EncryptedSharedPreferences encryptedSharedPreferences;

    @Inject
    public SessionKeyCache(EncryptedSharedPreferences encryptedSharedPreferences) {
        this.encryptedSharedPreferences = encryptedSharedPreferences;
        encryptedSharedPreferences.edit().remove(SESSION_KEY).apply();
        String storedSessionKey = encryptedSharedPreferences.getString(SESSION_KEY, null);
        if (Objects.nonNull(storedSessionKey)) {
            log.info("A session key is already stored on device!");
            sessionKey = storedSessionKey.getBytes(StandardCharsets.UTF_8);
        }
    }

    public synchronized byte[] getSessionKey() {
        return Objects.isNull(sessionKey) ? null : Arrays.copyOf(sessionKey, sessionKey.length);
    }

    public synchronized void setSessionKey(byte[] sessionKey) {
        destroyOldSessionKey();
        this.sessionKey = Arrays.copyOf(sessionKey, sessionKey.length);
        encryptedSharedPreferences.edit()
                .putString(SESSION_KEY, new String(sessionKey, StandardCharsets.UTF_8))
                .apply();
    }

    public synchronized void destroyOldSessionKey() {
        if (Objects.nonNull(sessionKey)) {
            Arrays.fill(sessionKey, (byte) 0);
            sessionKey = null;
            encryptedSharedPreferences.edit()
                    .remove(SESSION_KEY)
                    .apply();
        }
    }

}
