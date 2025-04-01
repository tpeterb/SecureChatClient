package com.tpeterb.securechatclient.security.service;

import static com.tpeterb.securechatclient.security.model.KeyExchangeResult.FAILED_SIGNATURE_VERIFICATION_ON_CLIENT_SIDE;
import static com.tpeterb.securechatclient.security.model.KeyExchangeResult.FAILED_SIGNATURE_VERIFICATION_ON_SERVER_SIDE;
import static com.tpeterb.securechatclient.security.model.KeyExchangeResult.GENERAL_FAILURE;
import static com.tpeterb.securechatclient.security.model.KeyExchangeResult.SUCCESS;

import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.tpeterb.securechatclient.security.cache.DigitalSignatureKeyPairCache;
import com.tpeterb.securechatclient.security.cache.ServerPublicKeyCache;
import com.tpeterb.securechatclient.security.cache.SessionKeyCache;
import com.tpeterb.securechatclient.security.model.EllipticCurve;
import com.tpeterb.securechatclient.security.model.EllipticCurveKeyExchangeDTO;
import com.tpeterb.securechatclient.security.model.KeyExchangeResult;
import com.tpeterb.securechatclient.users.session.UserSession;

import org.bouncycastle.crypto.AsymmetricCipherKeyPair;
import org.bouncycastle.crypto.agreement.X25519Agreement;
import org.bouncycastle.crypto.agreement.X448Agreement;
import org.bouncycastle.crypto.generators.X25519KeyPairGenerator;
import org.bouncycastle.crypto.generators.X448KeyPairGenerator;
import org.bouncycastle.crypto.params.AsymmetricKeyParameter;
import org.bouncycastle.crypto.params.X25519KeyGenerationParameters;
import org.bouncycastle.crypto.params.X25519PrivateKeyParameters;
import org.bouncycastle.crypto.params.X25519PublicKeyParameters;
import org.bouncycastle.crypto.params.X448KeyGenerationParameters;
import org.bouncycastle.crypto.params.X448PrivateKeyParameters;
import org.bouncycastle.crypto.params.X448PublicKeyParameters;

import java.security.SecureRandom;
import java.util.Arrays;
import java.util.UUID;

import javax.inject.Inject;
import javax.inject.Singleton;

import lombok.extern.slf4j.Slf4j;

@Singleton
@Slf4j
public class EllipticCurveDiffieHellmanKeyExchangeService {

    private final ServerKeyExchangeApiServiceProvider serverKeyExchangeApiServiceProvider;

    private final UserSession userSession;

    private final SessionKeyCache sessionKeyCache;

    private final ServerPublicKeyCache serverPublicKeyCache;

    private final DigitalSignatureService digitalSignatureService;

    private final DigitalSignatureKeyPairCache digitalSignatureKeyPairCache;

    private final SessionKeyService sessionKeyService;

    @Inject
    public EllipticCurveDiffieHellmanKeyExchangeService(ServerKeyExchangeApiServiceProvider serverKeyExchangeApiServiceProvider,
                                                        UserSession userSession,
                                                        SessionKeyCache sessionKeyCache,
                                                        ServerPublicKeyCache serverPublicKeyCache,
                                                        DigitalSignatureService digitalSignatureService,
                                                        DigitalSignatureKeyPairCache digitalSignatureKeyPairCache,
                                                        SessionKeyService sessionKeyService) {
        this.serverKeyExchangeApiServiceProvider = serverKeyExchangeApiServiceProvider;
        this.userSession = userSession;
        this.sessionKeyCache = sessionKeyCache;
        this.serverPublicKeyCache = serverPublicKeyCache;
        this.digitalSignatureService = digitalSignatureService;
        this.digitalSignatureKeyPairCache = digitalSignatureKeyPairCache;
        this.sessionKeyService = sessionKeyService;
    }

    public LiveData<KeyExchangeResult> initiateKeyExchangeWithServerBeforeLogin(LifecycleOwner lifecycleOwner) {
        log.info("KULCSCSERE BELÉPÉS ELŐTT!");
        AsymmetricCipherKeyPair ellipticCurveDiffieHellmanKeyPair = generateKeyPairForCurve(EllipticCurve.CURVE25519);
        X25519PublicKeyParameters publicKeyParameter = (X25519PublicKeyParameters) ellipticCurveDiffieHellmanKeyPair.getPublic();
        X25519PrivateKeyParameters privateKeyParameter = (X25519PrivateKeyParameters) ellipticCurveDiffieHellmanKeyPair.getPrivate();
        byte[] publicKeyBytes = convertPublicKeyToByteArray(EllipticCurve.CURVE25519, publicKeyParameter);
        byte[] publicKeySignature = digitalSignatureService.signData(publicKeyBytes, digitalSignatureKeyPairCache.getDigitalSignaturePrivateKey());
        EllipticCurveKeyExchangeDTO ellipticCurveKeyExchangeDTO = EllipticCurveKeyExchangeDTO.builder()
                .sessionId(UUID.randomUUID().toString())
                .ellipticCurve(EllipticCurve.CURVE25519)
                .isBeforeLogin(true)
                .keyExchangePublicKey(publicKeyBytes)
                .keyExchangePublicKeySignature(publicKeySignature)
                .signatureVerificationKey(digitalSignatureService.convertPublicKeyToBytes(digitalSignatureKeyPairCache.getDigitalSignaturePublicKey()))
                .build();
        MutableLiveData<KeyExchangeResult> keyExchangeResult = new MutableLiveData<>();
        serverKeyExchangeApiServiceProvider.initiateEllipticCurveDiffieHellmanKeyExchange(ellipticCurveKeyExchangeDTO).observe(lifecycleOwner, ellipticCurveKeyExchangeResponse -> {
            log.info("KEY EXCHANGE ANSWER OBSERVER!");
            if (ellipticCurveKeyExchangeResponse.second == GENERAL_FAILURE || ellipticCurveKeyExchangeResponse.second == FAILED_SIGNATURE_VERIFICATION_ON_SERVER_SIDE) {
                keyExchangeResult.postValue(ellipticCurveKeyExchangeResponse.second);
                return;
            }
            EllipticCurveKeyExchangeDTO ellipticCurveKeyExchangeData = ellipticCurveKeyExchangeResponse.first;
            if (!digitalSignatureService.verifySignature(
                    ellipticCurveKeyExchangeData.getKeyExchangePublicKey(),
                    ellipticCurveKeyExchangeData.getKeyExchangePublicKeySignature(),
                    serverPublicKeyCache.getServerPublicKeyForDigitalSignatures()
            )) {
                log.error("The verification of the digital signature of the key exchange public key sent by the server failed!");
                keyExchangeResult.postValue(FAILED_SIGNATURE_VERIFICATION_ON_CLIENT_SIDE);
                return;
            }
            byte[] sharedSecret = generateSharedSecretFromCurve(
                    ellipticCurveKeyExchangeData.getEllipticCurve(),
                    convertPublicKeyFromBytes(
                            ellipticCurveKeyExchangeData.getEllipticCurve(),
                            ellipticCurveKeyExchangeData.getKeyExchangePublicKey()
                    ),
                    privateKeyParameter
            );
            log.info("ECDH SHARED SECRET = {}", Arrays.toString(sharedSecret));
            userSession.setSessionId(ellipticCurveKeyExchangeData.getSessionId());
            log.info("USER SESSION SESSION ID IN OBSERVER = {}", userSession.getSessionId());
            byte[] sessionKey = sessionKeyService.generateSessionKeyFromSharedSecret(sharedSecret);
            sessionKeyCache.setSessionKey(sessionKey);
            keyExchangeResult.postValue(SUCCESS);
            log.info("ECDH SESSION KEY = {}", Arrays.toString(sessionKeyCache.getSessionKey()));
        });
        return keyExchangeResult;
    }

    public LiveData<KeyExchangeResult> initiateKeyExchangeWithServerAfterLogin(LifecycleOwner lifecycleOwner) {
        log.info("KULCSCSERE BELÉPÉS UTÁN!");
        AsymmetricCipherKeyPair ellipticCurveDiffieHellmanKeyPair = generateKeyPairForCurve(EllipticCurve.CURVE25519);
        X25519PublicKeyParameters publicKeyParameter = (X25519PublicKeyParameters) ellipticCurveDiffieHellmanKeyPair.getPublic();
        X25519PrivateKeyParameters privateKeyParameter = (X25519PrivateKeyParameters) ellipticCurveDiffieHellmanKeyPair.getPrivate();
        byte[] publicKeyBytes = convertPublicKeyToByteArray(EllipticCurve.CURVE25519, publicKeyParameter);
        log.info("PUBLIC VERIFICATION KEY OF USER = {}", digitalSignatureService.convertPublicKeyToBytes(digitalSignatureKeyPairCache.getDigitalSignaturePublicKey()));
        byte[] publicKeySignature = digitalSignatureService.signData(publicKeyBytes, digitalSignatureKeyPairCache.getDigitalSignaturePrivateKey());
        EllipticCurveKeyExchangeDTO ellipticCurveKeyExchangeDTO = EllipticCurveKeyExchangeDTO.builder()
                .sessionId(UUID.randomUUID().toString())
                .ellipticCurve(EllipticCurve.CURVE25519)
                .isBeforeLogin(false)
                .username(userSession.getUsername())
                .keyExchangePublicKey(publicKeyBytes)
                .keyExchangePublicKeySignature(publicKeySignature)
                .build();
        MutableLiveData<KeyExchangeResult> keyExchangeResult = new MutableLiveData<>();
        serverKeyExchangeApiServiceProvider.initiateEllipticCurveDiffieHellmanKeyExchange(ellipticCurveKeyExchangeDTO).observe(lifecycleOwner, ellipticCurveKeyExchangeResponse -> {
            log.info("KEY EXCHANGE ANSWER OBSERVER!");
            if (ellipticCurveKeyExchangeResponse.second == GENERAL_FAILURE || ellipticCurveKeyExchangeResponse.second == FAILED_SIGNATURE_VERIFICATION_ON_SERVER_SIDE) {
                keyExchangeResult.postValue(ellipticCurveKeyExchangeResponse.second);
                return;
            }
            EllipticCurveKeyExchangeDTO ellipticCurveKeyExchangeData = ellipticCurveKeyExchangeResponse.first;
            if (!digitalSignatureService.verifySignature(
                    ellipticCurveKeyExchangeData.getKeyExchangePublicKey(),
                    ellipticCurveKeyExchangeData.getKeyExchangePublicKeySignature(),
                    serverPublicKeyCache.getServerPublicKeyForDigitalSignatures()
            )) {
                log.error("The verification of the digital signature of the key exchange public key sent by the server failed!");
                keyExchangeResult.postValue(FAILED_SIGNATURE_VERIFICATION_ON_CLIENT_SIDE);
                return;
            }
            byte[] sharedSecret = generateSharedSecretFromCurve(
                    ellipticCurveKeyExchangeData.getEllipticCurve(),
                    convertPublicKeyFromBytes(
                            ellipticCurveKeyExchangeData.getEllipticCurve(),
                            ellipticCurveKeyExchangeData.getKeyExchangePublicKey()
                    ),
                    privateKeyParameter
            );
            log.info("ECDH SHARED SECRET = {}", Arrays.toString(sharedSecret));
            userSession.setSessionId(ellipticCurveKeyExchangeData.getSessionId());
            log.info("USER SESSION SESSION ID IN OBSERVER = {}", userSession.getSessionId());
            byte[] sessionKey = sessionKeyService.generateSessionKeyFromSharedSecret(sharedSecret);
            sessionKeyCache.setSessionKey(sessionKey);
            keyExchangeResult.postValue(SUCCESS);
            log.info("ECDH SESSION KEY = {}", Arrays.toString(sessionKeyCache.getSessionKey()));
        });
        return keyExchangeResult;
    }

    public AsymmetricCipherKeyPair generateKeyPairForCurve(EllipticCurve ellipticCurve) {
        AsymmetricCipherKeyPair asymmetricCipherKeyPair = null;
        switch (ellipticCurve) {
            case CURVE448:
                asymmetricCipherKeyPair = generateKeyPairForCurve448();
                break;
            case CURVE25519:
                asymmetricCipherKeyPair = generateKeyPairForCurve25519();
                break;
        }
        return asymmetricCipherKeyPair;
    }

    public byte[] generateSharedSecretFromCurve(EllipticCurve ellipticCurve, AsymmetricKeyParameter publicKey, AsymmetricKeyParameter privateKey) {
        byte[] sharedSecret = new byte[0];
        switch (ellipticCurve) {
            case CURVE25519:
                sharedSecret = generateSharedSecretFromCurve25519((X25519PublicKeyParameters) publicKey, (X25519PrivateKeyParameters) privateKey);
                break;
            case CURVE448:
                sharedSecret = generateSharedSecretFromCurve448((X448PublicKeyParameters) publicKey, (X448PrivateKeyParameters) privateKey);
                break;
        }
        return sharedSecret;
    }

    public byte[] convertPublicKeyToByteArray(EllipticCurve ellipticCurve, AsymmetricKeyParameter publicKey) {
        byte[] publicKeyBytes = new byte[0];
        switch (ellipticCurve) {
            case CURVE448:
                publicKeyBytes = ((X448PublicKeyParameters) publicKey).getEncoded();
                break;
            case CURVE25519:
                publicKeyBytes = ((X25519PublicKeyParameters) publicKey).getEncoded();
                break;
        }
        return publicKeyBytes;
    }

    public AsymmetricKeyParameter convertPublicKeyFromBytes(EllipticCurve ellipticCurve, byte[] publicKeyBytes) {
        AsymmetricKeyParameter publicKey = null;
        switch (ellipticCurve) {
            case CURVE448:
                publicKey = new X448PublicKeyParameters(publicKeyBytes);
                break;
            case CURVE25519:
                publicKey = new X25519PublicKeyParameters(publicKeyBytes);
                break;
        }
        return publicKey;
    }

    private AsymmetricCipherKeyPair generateKeyPairForCurve25519() {
        X25519KeyPairGenerator keyPairGenerator = new X25519KeyPairGenerator();
        keyPairGenerator.init(new X25519KeyGenerationParameters(new SecureRandom()));
        return keyPairGenerator.generateKeyPair();
    }

    private AsymmetricCipherKeyPair generateKeyPairForCurve448() {
        X448KeyPairGenerator keyPairGenerator = new X448KeyPairGenerator();
        keyPairGenerator.init(new X448KeyGenerationParameters(new SecureRandom()));
        return keyPairGenerator.generateKeyPair();
    }

    private byte[] generateSharedSecretFromCurve25519(X25519PublicKeyParameters publicKeyParameters, X25519PrivateKeyParameters privateKeyParameters) {
        byte[] sharedSecret = new byte[EllipticCurve.CURVE25519.getSharedSecretLengthInBytes()];
        X25519Agreement agreement = new X25519Agreement();
        agreement.init(privateKeyParameters);
        agreement.calculateAgreement(publicKeyParameters, sharedSecret, 0);
        return sharedSecret;
    }

    private byte[] generateSharedSecretFromCurve448(X448PublicKeyParameters publicKeyParameters, X448PrivateKeyParameters privateKeyParameters) {
        byte[] sharedSecret = new byte[EllipticCurve.CURVE448.getSharedSecretLengthInBytes()];
        X448Agreement agreement = new X448Agreement();
        agreement.init(privateKeyParameters);
        agreement.calculateAgreement(publicKeyParameters, sharedSecret, 0);
        return sharedSecret;
    }

}
