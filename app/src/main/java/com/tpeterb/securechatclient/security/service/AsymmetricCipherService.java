package com.tpeterb.securechatclient.security.service;

import com.tpeterb.securechatclient.exception.AsymmetricDecryptionException;
import com.tpeterb.securechatclient.exception.AsymmetricEncryptionException;

import org.bouncycastle.crypto.AsymmetricCipherKeyPair;
import org.bouncycastle.crypto.params.AsymmetricKeyParameter;

public interface AsymmetricCipherService {

    byte[] encryptData(byte[] dataToEncrypt, AsymmetricKeyParameter encryptionKey) throws AsymmetricEncryptionException;

    byte[] decryptData(byte[] dataToDecrypt, AsymmetricKeyParameter decryptionKey) throws AsymmetricDecryptionException;

    AsymmetricCipherKeyPair generateAsymmetricKeyPair();

    byte[] convertPublicKeyToBytes(AsymmetricKeyParameter publicKey);

    byte[] convertPrivateKeyToBytes(AsymmetricKeyParameter privateKey);

    AsymmetricKeyParameter convertFromBytesToPublicKey(byte[] publicKeyBytes);

    AsymmetricKeyParameter convertFromBytesToPrivateKey(byte[] privateKeyBytes);

}
