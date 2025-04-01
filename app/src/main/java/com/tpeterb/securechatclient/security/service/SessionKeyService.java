package com.tpeterb.securechatclient.security.service;

import androidx.core.util.Pair;

import com.tpeterb.securechatclient.security.cache.SessionKeyCache;
import com.tpeterb.securechatclient.security.config.SecurityConfig;

import org.bouncycastle.crypto.digests.SHA512Digest;
import org.bouncycastle.crypto.generators.HKDFBytesGenerator;
import org.bouncycastle.crypto.params.HKDFParameters;

import java.security.SecureRandom;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class SessionKeyService {

    private static final String SESSION_KEY_INFO = "chatSessionKey";

    private static final String ONE_TIME_KEY_INFO = "chatOneTimeKey";

    private final SessionKeyCache sessionKeyCache;

    private final SecurityConfig securityConfig;

    private final SecureRandom secureRandom;

    @Inject
    public SessionKeyService(SessionKeyCache sessionKeyCache, SecurityConfig securityConfig) {
        this.sessionKeyCache = sessionKeyCache;
        this.securityConfig = securityConfig;
        secureRandom = new SecureRandom();
    }

    public byte[] generateSessionKeyFromSharedSecret(byte[] sharedSecret) {
        int sessionKeyLength = securityConfig.getSymmetricCipherKeyLengthInBits() / 8;
        byte[] salt = generateSaltForSessionKeyDerivation(sharedSecret);
        return deriveKeyFromSecretValue(sharedSecret, salt, SESSION_KEY_INFO.getBytes(), sessionKeyLength);
    }

    public Pair<byte[], byte[]> generateOneTimeEncryptionKeyFromSessionKey() {
        byte[] sessionKey = sessionKeyCache.getSessionKey();
        byte[] salt = generateSaltForOneTimeKeyDerivation();
        byte[] oneTimeKey = deriveKeyFromSecretValue(sessionKey, salt, ONE_TIME_KEY_INFO.getBytes(), securityConfig.getSymmetricCipherKeyLengthInBits() / 8);
        return Pair.create(oneTimeKey, salt);
    }

    public byte[] generateOneTimeDecryptionKeyFromNonce(byte[] nonce) {
        byte[] sessionKey = sessionKeyCache.getSessionKey();
        int oneTimeKeyLength = securityConfig.getSymmetricCipherKeyLengthInBits() / 8;
        return deriveKeyFromSecretValue(sessionKey, nonce, ONE_TIME_KEY_INFO.getBytes(), oneTimeKeyLength);
    }

    private byte[] generateSaltForOneTimeKeyDerivation() {
        byte[] salt = new byte[securityConfig.getSaltLengthForKeyDerivationInBits() / 8];
        secureRandom.nextBytes(salt);
        return salt;
    }

    private byte[] generateSaltForSessionKeyDerivation(byte[] sharedSecret) {
        SHA512Digest sha512Digest = new SHA512Digest();
        byte[] salt = new byte[sha512Digest.getDigestSize()];
        sha512Digest.update(sharedSecret, 0, sharedSecret.length);
        sha512Digest.doFinal(salt, 0);
        return salt;
    }

    private byte[] deriveKeyFromSecretValue(byte[] secret, byte[] salt, byte[] contextInfo, int keyLength) {
        HKDFBytesGenerator hkdfBytesGenerator = new HKDFBytesGenerator(new SHA512Digest());
        HKDFParameters hkdfParameters = new HKDFParameters(
                secret,
                salt,
                contextInfo);
        hkdfBytesGenerator.init(hkdfParameters);
        byte[] key = new byte[keyLength];
        hkdfBytesGenerator.generateBytes(key, 0, keyLength);
        return key;
    }

}
