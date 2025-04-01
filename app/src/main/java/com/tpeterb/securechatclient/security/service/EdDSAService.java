package com.tpeterb.securechatclient.security.service;

import org.bouncycastle.crypto.AsymmetricCipherKeyPair;
import org.bouncycastle.crypto.CipherParameters;
import org.bouncycastle.crypto.generators.Ed25519KeyPairGenerator;
import org.bouncycastle.crypto.params.AsymmetricKeyParameter;
import org.bouncycastle.crypto.params.Ed25519KeyGenerationParameters;
import org.bouncycastle.crypto.params.Ed25519PrivateKeyParameters;
import org.bouncycastle.crypto.params.Ed25519PublicKeyParameters;
import org.bouncycastle.crypto.signers.Ed25519Signer;

import java.security.SecureRandom;

public class EdDSAService implements DigitalSignatureService {

    @Override
    public AsymmetricCipherKeyPair generateAsymmetricKeyPair() {
        Ed25519KeyPairGenerator ed25519KeyPairGenerator = new Ed25519KeyPairGenerator();
        Ed25519KeyGenerationParameters ed25519KeyGenerationParameters = new Ed25519KeyGenerationParameters(new SecureRandom());
        ed25519KeyPairGenerator.init(ed25519KeyGenerationParameters);
        return ed25519KeyPairGenerator.generateKeyPair();
    }

    @Override
    public byte[] signData(byte[] dataToSign, CipherParameters signatureKey) {
        Ed25519Signer ed25519Signer = new Ed25519Signer();
        ed25519Signer.init(true, signatureKey);
        ed25519Signer.update(dataToSign, 0, dataToSign.length);
        return ed25519Signer.generateSignature();
    }

    @Override
    public boolean verifySignature(byte[] data, byte[] signatureToVerify, CipherParameters verificationKey) {
        Ed25519Signer ed25519Signer = new Ed25519Signer();
        ed25519Signer.init(false, verificationKey);
        ed25519Signer.update(data, 0, data.length);
        return ed25519Signer.verifySignature(signatureToVerify);
    }

    @Override
    public byte[] convertPublicKeyToBytes(AsymmetricKeyParameter publicKey) {
        return ((Ed25519PublicKeyParameters) publicKey).getEncoded();
    }

    @Override
    public AsymmetricKeyParameter convertBytesToPublicKey(byte[] publicKeyBytes) {
        return new Ed25519PublicKeyParameters(publicKeyBytes);
    }

    @Override
    public byte[] convertPrivateKeyToBytes(AsymmetricKeyParameter privateKey) {
        return ((Ed25519PrivateKeyParameters) privateKey).getEncoded();
    }

    @Override
    public AsymmetricKeyParameter convertBytesToPrivateKey(byte[] privateKeyBytes) {
        return new Ed25519PrivateKeyParameters(privateKeyBytes);
    }

}
