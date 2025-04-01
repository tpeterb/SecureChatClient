package com.tpeterb.securechatclient.security.service;

import com.tpeterb.securechatclient.security.cache.SessionKeyCache;
import com.tpeterb.securechatclient.security.model.EncryptionResult;

import org.bouncycastle.crypto.InvalidCipherTextException;
import org.bouncycastle.crypto.modes.ChaCha20Poly1305;
import org.bouncycastle.crypto.params.AEADParameters;
import org.bouncycastle.crypto.params.KeyParameter;

import javax.inject.Inject;
import javax.inject.Singleton;

import java.security.SecureRandom;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Singleton
public class ChaCha20Service implements SymmetricCipherService {

    private static final int KEY_SIZE = 256;

    private static final int MAC_SIZE = 128;

    private static final int NONCE_SIZE = 96;

    private final SecureRandom secureRandom;

    @Inject
    public ChaCha20Service() {
        secureRandom = new SecureRandom();
    }

    @Override
    public byte[] generateSymmetricKey() {
        byte[] symmetricKey = new byte[KEY_SIZE / 8];
        secureRandom.nextBytes(symmetricKey);
        return symmetricKey;
    }

    @Override
    public EncryptionResult encryptData(byte[] dataToEncrypt, byte[] encryptionKey) {
        byte[] nonce = generateNonce();
        ChaCha20Poly1305 chaCha20Poly1305 = new ChaCha20Poly1305();
        AEADParameters aeadParameters = new AEADParameters(
            new KeyParameter(encryptionKey),
            MAC_SIZE,
            nonce
        );
        chaCha20Poly1305.init(true, aeadParameters);
        int outputLength = chaCha20Poly1305.getOutputSize(dataToEncrypt.length);
        byte[] encryptedData = new byte[outputLength];
        int processedNumberOfBytes = chaCha20Poly1305.processBytes(dataToEncrypt, 0, dataToEncrypt.length, encryptedData, 0);
        try {
            chaCha20Poly1305.doFinal(encryptedData, processedNumberOfBytes);
        } catch (InvalidCipherTextException e) {
            log.error("Failed to encrypt data with ChaCha20-Poly1305, reason: {}", e.getMessage());
            return null;
        }
        return EncryptionResult.builder()
                .encryptedData(encryptedData)
                .initializationVector(nonce)
                .build();
    }

    @Override
    public byte[] decryptData(byte[] dataToDecrypt, byte[] decryptionKey, byte[] initializationVector) {
        ChaCha20Poly1305 chaCha20Poly1305 = new ChaCha20Poly1305();
        AEADParameters aeadParameters = new AEADParameters(
                new KeyParameter(decryptionKey),
                MAC_SIZE,
                initializationVector
        );
        chaCha20Poly1305.init(false, aeadParameters);
        int outputLength = chaCha20Poly1305.getOutputSize(dataToDecrypt.length);
        byte[] decryptedData = new byte[outputLength];
        int processedNumberOfBytes = chaCha20Poly1305.processBytes(dataToDecrypt, 0, dataToDecrypt.length, decryptedData, 0);
        try {
            chaCha20Poly1305.doFinal(decryptedData, processedNumberOfBytes);
        } catch (InvalidCipherTextException e) {
            log.error("Failed to decrypt data with ChaCha20-Poly1305, reason: {}", e.getMessage());
            return null;
        }
        return decryptedData;
    }

    @Override
    public int getKeySize() {
        return KEY_SIZE;
    }

    private byte[] generateNonce() {
        byte[] nonce = new byte[NONCE_SIZE / 8];
        secureRandom.nextBytes(nonce);
        return nonce;
    }

}
