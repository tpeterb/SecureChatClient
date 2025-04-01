package com.tpeterb.securechatclient.security.service;

import com.tpeterb.securechatclient.exception.AsymmetricDecryptionException;
import com.tpeterb.securechatclient.exception.AsymmetricEncryptionException;
import com.tpeterb.securechatclient.exception.AsymmetricKeyConversionException;

import org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import org.bouncycastle.asn1.pkcs.RSAPrivateKey;
import org.bouncycastle.asn1.pkcs.RSAPublicKey;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.crypto.AsymmetricCipherKeyPair;
import org.bouncycastle.crypto.InvalidCipherTextException;
import org.bouncycastle.crypto.encodings.OAEPEncoding;
import org.bouncycastle.crypto.engines.RSAEngine;
import org.bouncycastle.crypto.generators.RSAKeyPairGenerator;
import org.bouncycastle.crypto.params.AsymmetricKeyParameter;
import org.bouncycastle.crypto.params.RSAKeyGenerationParameters;
import org.bouncycastle.crypto.params.RSAKeyParameters;
import org.bouncycastle.crypto.util.PrivateKeyInfoFactory;
import org.bouncycastle.crypto.util.SubjectPublicKeyInfoFactory;

import java.io.IOException;
import java.math.BigInteger;
import java.security.SecureRandom;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class RSAService implements AsymmetricCipherService {

    private static final int KEY_LENGTH_IN_BITS = 3072;

    private static final int PUBLIC_EXPONENT = 65537;

    private static final int PRIME_CERTAINTY = 128;

    @Override
    public byte[] encryptData(byte[] dataToEncrypt, AsymmetricKeyParameter encryptionKey) throws AsymmetricEncryptionException {
        OAEPEncoding oaepEncoding = new OAEPEncoding(new RSAEngine());
        oaepEncoding.init(true, encryptionKey);
        try {
            return oaepEncoding.encodeBlock(dataToEncrypt, 0, dataToEncrypt.length);
        } catch (InvalidCipherTextException e) {
            log.error("There was an error while trying to encrypt data with RSA, reason: {}", e.getMessage());
            throw new AsymmetricEncryptionException(e.getMessage());
        }
    }

    @Override
    public byte[] decryptData(byte[] dataToDecrypt, AsymmetricKeyParameter decryptionKey) throws AsymmetricDecryptionException {
        OAEPEncoding oaepEncoding = new OAEPEncoding(new RSAEngine());
        oaepEncoding.init(false, decryptionKey);
        try {
            return oaepEncoding.decodeBlock(dataToDecrypt, 0, dataToDecrypt.length);
        } catch (InvalidCipherTextException e) {
            log.error("Failed to decrypt data with RSA, reason: {}", e.getMessage());
            throw new AsymmetricDecryptionException(e.getMessage());
        }
    }

    @Override
    public AsymmetricCipherKeyPair generateAsymmetricKeyPair() {
        RSAKeyPairGenerator rsaKeyPairGenerator = new RSAKeyPairGenerator();
        RSAKeyGenerationParameters rsaKeyGenerationParameters;
        rsaKeyGenerationParameters = new RSAKeyGenerationParameters(
                new BigInteger(String.valueOf(PUBLIC_EXPONENT)),
                new SecureRandom(),
                KEY_LENGTH_IN_BITS,
                PRIME_CERTAINTY
        );
        rsaKeyPairGenerator.init(rsaKeyGenerationParameters);
        return rsaKeyPairGenerator.generateKeyPair();
    }

    @Override
    public byte[] convertPublicKeyToBytes(AsymmetricKeyParameter publicKey) {
        RSAKeyParameters rsaPublicKey = (RSAKeyParameters) publicKey;
        try {
            SubjectPublicKeyInfo publicKeyInfo = SubjectPublicKeyInfoFactory.createSubjectPublicKeyInfo(rsaPublicKey);
            return publicKeyInfo.getEncoded();
        } catch (IOException e) {
            throw AsymmetricKeyConversionException.toBytesConversionError(e.getMessage());
        }
    }

    @Override
    public byte[] convertPrivateKeyToBytes(AsymmetricKeyParameter privateKey) {
        RSAKeyParameters rsaPrivateKey = (RSAKeyParameters) privateKey;
        try {
            PrivateKeyInfo privateKeyInfo = PrivateKeyInfoFactory.createPrivateKeyInfo(rsaPrivateKey);
            return privateKeyInfo.getEncoded();
        } catch (IOException e) {
            throw AsymmetricKeyConversionException.toBytesConversionError(e.getMessage());
        }
    }

    @Override
    public AsymmetricKeyParameter convertFromBytesToPublicKey(byte[] publicKeyBytes) {
        SubjectPublicKeyInfo subjectPublicKeyInfo = SubjectPublicKeyInfo.getInstance(publicKeyBytes);
        try {
            RSAPublicKey rsaPublicKey = RSAPublicKey.getInstance(subjectPublicKeyInfo.parsePublicKey());
            return new RSAKeyParameters(true, rsaPublicKey.getModulus(), rsaPublicKey.getPublicExponent());
        } catch (IOException e) {
            throw AsymmetricKeyConversionException.fromBytesConversionError(e.getMessage());
        }
    }

    @Override
    public AsymmetricKeyParameter convertFromBytesToPrivateKey(byte[] privateKeyBytes) {
        PrivateKeyInfo privateKeyInfo = PrivateKeyInfo.getInstance(privateKeyBytes);
        try {
            RSAPrivateKey rsaPrivateKey = RSAPrivateKey.getInstance(privateKeyInfo.parsePrivateKey());
            return new RSAKeyParameters(false, rsaPrivateKey.getModulus(), rsaPrivateKey.getPrivateExponent());
        } catch (IOException e) {
            throw AsymmetricKeyConversionException.fromBytesConversionError(e.getMessage());
        }
    }

}

