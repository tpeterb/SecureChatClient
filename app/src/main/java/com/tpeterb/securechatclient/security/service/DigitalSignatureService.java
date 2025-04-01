package com.tpeterb.securechatclient.security.service;

import org.bouncycastle.crypto.AsymmetricCipherKeyPair;
import org.bouncycastle.crypto.CipherParameters;
import org.bouncycastle.crypto.params.AsymmetricKeyParameter;

public interface DigitalSignatureService {

    AsymmetricCipherKeyPair generateAsymmetricKeyPair();

    byte[] signData(byte[] dataToSign, CipherParameters signatureKey);

    boolean verifySignature(byte[] data, byte[] signatureToVerify, CipherParameters verificationKey);

    byte[] convertPublicKeyToBytes(AsymmetricKeyParameter publicKey);

    AsymmetricKeyParameter convertBytesToPublicKey(byte[] publicKeyBytes);

    byte[] convertPrivateKeyToBytes(AsymmetricKeyParameter privateKey);

    AsymmetricKeyParameter convertBytesToPrivateKey(byte[] privateKeyBytes);

}
