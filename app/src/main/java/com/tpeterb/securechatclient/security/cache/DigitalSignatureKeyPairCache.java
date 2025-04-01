package com.tpeterb.securechatclient.security.cache;

import androidx.security.crypto.EncryptedSharedPreferences;

import com.tpeterb.securechatclient.security.converter.GeneralAsymmetricKeyConverter;
import com.tpeterb.securechatclient.security.service.DigitalSignatureService;

import org.bouncycastle.crypto.AsymmetricCipherKeyPair;
import org.bouncycastle.crypto.params.AsymmetricKeyParameter;

import java.util.Objects;

import javax.inject.Inject;
import javax.inject.Singleton;

import lombok.extern.slf4j.Slf4j;

@Singleton
@Slf4j
public class DigitalSignatureKeyPairCache {

    private static final String USER_DIGITAL_SIGNATURE_PUBLIC_KEY_POSTFIX = "_DIGITAL_SIGNATURE_PUBLIC_KEY";

    private static final String USER_DIGITAL_SIGNATURE_PRIVATE_KEY_POSTFIX = "_DIGITAL_SIGNATURE_PRIVATE_KEY";

    private volatile AsymmetricKeyParameter digitalSignaturePublicKeyForRegistration;

    private volatile AsymmetricKeyParameter digitalSignaturePrivateKeyForRegistration;

    private volatile AsymmetricKeyParameter digitalSignaturePublicKey;

    private volatile AsymmetricKeyParameter digitalSignaturePrivateKey;

    private final GeneralAsymmetricKeyConverter generalAsymmetricKeyConverter;

    private final DigitalSignatureService digitalSignatureService;

    private final EncryptedSharedPreferences encryptedSharedPreferences;

    @Inject
    public DigitalSignatureKeyPairCache(GeneralAsymmetricKeyConverter generalAsymmetricKeyConverter,
                                        DigitalSignatureService digitalSignatureService,
                                        EncryptedSharedPreferences encryptedSharedPreferences) {
        this.generalAsymmetricKeyConverter = generalAsymmetricKeyConverter;
        this.digitalSignatureService = digitalSignatureService;
        this.encryptedSharedPreferences = encryptedSharedPreferences;
        AsymmetricCipherKeyPair asymmetricCipherKeyPair = digitalSignatureService.generateAsymmetricKeyPair();
        digitalSignaturePublicKey = asymmetricCipherKeyPair.getPublic();
        digitalSignaturePrivateKey = asymmetricCipherKeyPair.getPrivate();
        log.info("digitalSignaturePublicKey AFTER DI = {}", digitalSignatureService.convertPublicKeyToBytes(digitalSignaturePublicKey));
        log.info("digitalSignaturePrivateKey AFTER DI = {}", digitalSignatureService.convertPrivateKeyToBytes(digitalSignaturePrivateKey));
    }

    public void clearDigitalSignatureKeyPairCache() {
        digitalSignaturePrivateKey = null;
        digitalSignaturePublicKey = null;
        digitalSignaturePublicKeyForRegistration = null;
        digitalSignaturePrivateKeyForRegistration = null;
        generateTemporaryDigitalSignatureKeyPair();
    }

    public synchronized AsymmetricKeyParameter getDigitalSignaturePublicKey() {
        return digitalSignaturePublicKey;
    }

    public synchronized AsymmetricKeyParameter getDigitalSignaturePrivateKey() {
        return digitalSignaturePrivateKey;
    }

    public synchronized AsymmetricKeyParameter getDigitalSignaturePublicKeyForRegistration() {
        return digitalSignaturePublicKeyForRegistration;
    }

    public synchronized void persistNewlyRegisteredUserDigitalSignatureKeyPair(String username) {
        encryptedSharedPreferences.edit()
                .putString(username + USER_DIGITAL_SIGNATURE_PUBLIC_KEY_POSTFIX, generalAsymmetricKeyConverter.serializePublicKeyForStorage(digitalSignaturePublicKeyForRegistration))
                .putString(username + USER_DIGITAL_SIGNATURE_PRIVATE_KEY_POSTFIX, generalAsymmetricKeyConverter.serializePrivateKeyForStorage(digitalSignaturePrivateKeyForRegistration))
                .apply();
        log.info("PUBLIC KEY BEFORE SAVING = {}", digitalSignatureService.convertPublicKeyToBytes(digitalSignaturePublicKeyForRegistration));
        log.info("PRIVATE KEY BEFORE SAVING = {}", digitalSignatureService.convertPrivateKeyToBytes(digitalSignaturePrivateKeyForRegistration));
        digitalSignaturePublicKeyForRegistration = null;
        digitalSignaturePrivateKeyForRegistration = null;
    }

    public synchronized void generateDigitalSignatureKeyPairForRegistration() {
        AsymmetricCipherKeyPair asymmetricCipherKeyPair = digitalSignatureService.generateAsymmetricKeyPair();
        log.info("digitalSignaturePublicKey AFTER CLICKING REGISTER BUTTON = {}", digitalSignatureService.convertPublicKeyToBytes(digitalSignaturePublicKey));
        log.info("digitalSignaturePrivateKey AFTER CLICKING REGISTER BUTTON = {}", digitalSignatureService.convertPrivateKeyToBytes(digitalSignaturePrivateKey));
        digitalSignaturePrivateKeyForRegistration = asymmetricCipherKeyPair.getPrivate();
        digitalSignaturePublicKeyForRegistration = asymmetricCipherKeyPair.getPublic();
    }

    public synchronized void loadKeyPairForUser(String username) {
        String privateKey = encryptedSharedPreferences.getString(username + USER_DIGITAL_SIGNATURE_PRIVATE_KEY_POSTFIX, null);
        if (Objects.nonNull(privateKey)) {
            String publicKey = encryptedSharedPreferences.getString(username + USER_DIGITAL_SIGNATURE_PUBLIC_KEY_POSTFIX, null);
            digitalSignaturePrivateKey = generalAsymmetricKeyConverter.deserializePrivateKey(privateKey);
            digitalSignaturePublicKey = generalAsymmetricKeyConverter.deserializePublicKey(publicKey);
            log.info("digitalSignaturePublicKey AFTER SAVING and LOGGING IN = {}", digitalSignatureService.convertPublicKeyToBytes(digitalSignaturePublicKey));
            log.info("digitalSignaturePrivateKey AFTER SAVING and LOGGING IN = {}", digitalSignatureService.convertPrivateKeyToBytes(digitalSignaturePrivateKey));
        } else {
            AsymmetricCipherKeyPair asymmetricCipherKeyPair = digitalSignatureService.generateAsymmetricKeyPair();
            setDigitalSignaturePrivateKeyForUser(asymmetricCipherKeyPair.getPrivate(), username);
            setDigitalSignaturePublicKeyForUser(asymmetricCipherKeyPair.getPublic(), username);
        }
    }

    private void generateTemporaryDigitalSignatureKeyPair() {
        AsymmetricCipherKeyPair asymmetricCipherKeyPair = digitalSignatureService.generateAsymmetricKeyPair();
        digitalSignaturePublicKey = asymmetricCipherKeyPair.getPublic();
        digitalSignaturePrivateKey = asymmetricCipherKeyPair.getPrivate();
    }

    private void setDigitalSignaturePublicKeyForUser(AsymmetricKeyParameter digitalSignaturePublicKey, String username) {
        this.digitalSignaturePublicKey = digitalSignaturePublicKey;
        storeDigitalSignaturePublicKeyPersistentlyForUser(digitalSignaturePublicKey, username);
    }

    private void setDigitalSignaturePrivateKeyForUser(AsymmetricKeyParameter digitalSignaturePrivateKey, String username) {
        this.digitalSignaturePrivateKey = digitalSignaturePrivateKey;
        storeDigitalSignaturePrivateKeyPersistentlyForUser(digitalSignaturePrivateKey, username);
    }

    private void storeDigitalSignaturePublicKeyPersistentlyForUser(AsymmetricKeyParameter digitalSignaturePublicKey, String username) {
        encryptedSharedPreferences.edit()
                .putString(username + USER_DIGITAL_SIGNATURE_PUBLIC_KEY_POSTFIX, generalAsymmetricKeyConverter.serializePublicKeyForStorage(digitalSignaturePublicKey))
                .apply();
    }

    private void storeDigitalSignaturePrivateKeyPersistentlyForUser(AsymmetricKeyParameter digitalSignaturePrivateKey, String username) {
        encryptedSharedPreferences.edit()
                .putString(username + USER_DIGITAL_SIGNATURE_PRIVATE_KEY_POSTFIX, generalAsymmetricKeyConverter.serializePrivateKeyForStorage(digitalSignaturePrivateKey))
                .apply();
    }

}
