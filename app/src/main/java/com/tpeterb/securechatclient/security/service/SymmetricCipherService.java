package com.tpeterb.securechatclient.security.service;

import com.tpeterb.securechatclient.security.model.EncryptionResult;

public interface SymmetricCipherService {

    byte[] generateSymmetricKey();

    EncryptionResult encryptData(byte[] dataToEncrypt, byte[] encryptionKey);

    byte[] decryptData(byte[] dataToDecrypt, byte[] decryptionKey, byte[] initializationVector);

    int getKeySize();

}
