package com.tpeterb.securechatclient.security.cache;

import androidx.security.crypto.EncryptedSharedPreferences;

import com.tpeterb.securechatclient.security.converter.GeneralAsymmetricKeyConverter;
import com.tpeterb.securechatclient.security.observer.MessagePublicKeyChangeObserver;
import com.tpeterb.securechatclient.security.observer.MessagePublicKeyChangeSubject;
import com.tpeterb.securechatclient.security.service.DigitalSignatureService;

import org.bouncycastle.crypto.params.AsymmetricKeyParameter;
import org.bouncycastle.crypto.params.RSAKeyParameters;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import javax.inject.Inject;
import javax.inject.Singleton;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Singleton
public class ServerPublicKeyCache implements MessagePublicKeyChangeSubject {

    private final List<MessagePublicKeyChangeObserver> messagePublicKeyChangeObservers;

    private final EncryptedSharedPreferences encryptedSharedPreferences;

    private final GeneralAsymmetricKeyConverter generalAsymmetricKeyConverter;

    private final DigitalSignatureService digitalSignatureService;

    private volatile AsymmetricKeyParameter serverPublicKeyForChatMessages;

    private volatile AsymmetricKeyParameter serverPublicKeyForDigitalSignatures;

    private static final String SERVER_ENCRYPTION_PUBLIC_KEY = "SERVER_PUBLIC_KEY_FOR_ENCRYPTION";

    private static final String SERVER_DIGITAL_SIGNATURE_PUBLIC_KEY = "SERVER_PUBLIC_KEY_FOR_DIGITAL_SIGNATURE";

    @Inject
    public ServerPublicKeyCache(EncryptedSharedPreferences encryptedSharedPreferences,
                                GeneralAsymmetricKeyConverter generalAsymmetricKeyConverter,
                                DigitalSignatureService digitalSignatureService) {
        this.encryptedSharedPreferences = encryptedSharedPreferences;
        this.generalAsymmetricKeyConverter = generalAsymmetricKeyConverter;
        this.digitalSignatureService = digitalSignatureService;
        messagePublicKeyChangeObservers = new ArrayList<>();
        String serverEncryptionPublicKeyString = encryptedSharedPreferences.getString(SERVER_ENCRYPTION_PUBLIC_KEY, null);
        String serverDigitalSignaturePublicKeyString = encryptedSharedPreferences.getString(SERVER_DIGITAL_SIGNATURE_PUBLIC_KEY, null);
        if (Objects.nonNull(serverEncryptionPublicKeyString)) {
            log.info("Server's public encryption key is already stored on the device!");
            serverPublicKeyForChatMessages = generalAsymmetricKeyConverter.deserializePublicKey(serverEncryptionPublicKeyString);
        } else {
            log.info("Server's public encryption key is not stored on device!");
        }
        if (Objects.nonNull(serverDigitalSignaturePublicKeyString)) {
            log.info("Server's public digital signature key is already stored on the device!");
            serverPublicKeyForDigitalSignatures = generalAsymmetricKeyConverter.deserializePublicKey(serverDigitalSignaturePublicKeyString);
        } else {
            log.info("Server's public digital signature key is not stored on device!");
        }
    }

    public synchronized AsymmetricKeyParameter getServerPublicKeyForChatMessages() {
        return serverPublicKeyForChatMessages;
    }

    public synchronized AsymmetricKeyParameter getServerPublicKeyForDigitalSignatures() {
        return serverPublicKeyForDigitalSignatures;
    }

    public synchronized void setServerPublicKeyForChatMessages(AsymmetricKeyParameter serverPublicKeyForChatMessages) {
        this.serverPublicKeyForChatMessages = serverPublicKeyForChatMessages;
        encryptedSharedPreferences.edit()
                        .putString(SERVER_ENCRYPTION_PUBLIC_KEY, generalAsymmetricKeyConverter.serializePublicKeyForStorage(serverPublicKeyForChatMessages))
                        .apply();
        log.info("Server's public key for messages = {}", (RSAKeyParameters) serverPublicKeyForChatMessages);
        notifyObservers();
    }

    public synchronized void setServerPublicKeyForDigitalSignatures(byte[] serverPublicKeyForDigitalSignatures) {
        this.serverPublicKeyForDigitalSignatures = digitalSignatureService.convertBytesToPublicKey(serverPublicKeyForDigitalSignatures);
        encryptedSharedPreferences.edit()
                .putString(SERVER_DIGITAL_SIGNATURE_PUBLIC_KEY, generalAsymmetricKeyConverter.serializePublicKeyForStorage(this.serverPublicKeyForDigitalSignatures))
                .apply();
        log.info("Server's public key for digital signatures = {}", serverPublicKeyForDigitalSignatures);
    }

    @Override
    public synchronized void addSubscriber(MessagePublicKeyChangeObserver publicKeyChangeObserver) {
        messagePublicKeyChangeObservers.add(publicKeyChangeObserver);
    }

    @Override
    public synchronized void removeSubscriber(MessagePublicKeyChangeObserver publicKeyChangeObserver) {
        messagePublicKeyChangeObservers.remove(publicKeyChangeObserver);
    }

    @Override
    public void notifyObservers() {
        for (MessagePublicKeyChangeObserver messagePublicKeyChangeObserver : messagePublicKeyChangeObservers) {
            messagePublicKeyChangeObserver.handleMessagePublicKeyChange();
        }
    }

}
