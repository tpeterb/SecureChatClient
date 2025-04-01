package com.tpeterb.securechatclient.security.converter;

import com.tpeterb.securechatclient.exception.AsymmetricKeyConversionException;
import com.tpeterb.securechatclient.utils.Base64Utils;

import org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.crypto.params.AsymmetricKeyParameter;
import org.bouncycastle.crypto.util.PrivateKeyFactory;
import org.bouncycastle.crypto.util.PrivateKeyInfoFactory;
import org.bouncycastle.crypto.util.PublicKeyFactory;
import org.bouncycastle.crypto.util.SubjectPublicKeyInfoFactory;

import java.io.IOException;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class GeneralAsymmetricKeyConverter {

    @Inject
    public GeneralAsymmetricKeyConverter() {

    }

    public String serializePublicKeyForStorage(AsymmetricKeyParameter publicKey) {
        try {
            SubjectPublicKeyInfo subjectPublicKeyInfo = SubjectPublicKeyInfoFactory.createSubjectPublicKeyInfo(publicKey);
            return Base64Utils.encode(subjectPublicKeyInfo.getEncoded());
        } catch (IOException e) {
            throw AsymmetricKeyConversionException.toStringForStorageConversionError(e.getMessage());
        }
    }

    public AsymmetricKeyParameter deserializePublicKey(String publicKey) {
        try {
            byte[] publicKeyBytes = Base64Utils.decode(publicKey);
            return PublicKeyFactory.createKey(publicKeyBytes);
        } catch (IOException e) {
            throw AsymmetricKeyConversionException.fromStringConversionError(e.getMessage());
        }
    }

    public String serializePrivateKeyForStorage(AsymmetricKeyParameter privateKey) {
        try {
            PrivateKeyInfo privateKeyInfo = PrivateKeyInfoFactory.createPrivateKeyInfo(privateKey);
            return Base64Utils.encode(privateKeyInfo.getEncoded());
        } catch (IOException e) {
            throw AsymmetricKeyConversionException.toStringForStorageConversionError(e.getMessage());
        }
    }

    public AsymmetricKeyParameter deserializePrivateKey(String privateKey) {
        try {
            byte[] publicKeyBytes = Base64Utils.decode(privateKey);
            return PrivateKeyFactory.createKey(publicKeyBytes);
        } catch (IOException e) {
            throw AsymmetricKeyConversionException.fromStringConversionError(e.getMessage());
        }
    }

}
