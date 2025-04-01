package com.tpeterb.securechatclient.security.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import org.bouncycastle.crypto.params.ECDomainParameters;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

@Value
@Builder
@AllArgsConstructor
public class EllipticCurveKeyExchangeDTO {

    String sessionId;

    EllipticCurve ellipticCurve;

    ECDomainParameters ecDomainParameters;

    @JsonProperty("isBeforeLogin")
    boolean isBeforeLogin;

    String username;

    byte[] keyExchangePublicKey;

    byte[] signatureVerificationKey;

    byte[] keyExchangePublicKeySignature;

    public EllipticCurveKeyExchangeDTO(
            @JsonProperty("keyExchangePublicKeySignature") byte[] keyExchangePublicKeySignature,
            @JsonProperty("signatureVerificationKey") byte[] signatureVerificationKey,
            @JsonProperty("keyExchangePublicKey") byte[] keyExchangePublicKey,
            @JsonProperty("username") String username,
            @JsonProperty("isBeforeLogin") boolean isBeforeLogin,
            @JsonProperty("ecDomainParameters") ECDomainParameters ecDomainParameters,
            @JsonProperty("ellipticCurve") EllipticCurve ellipticCurve,
            @JsonProperty("sessionId") String sessionId) {
        this.keyExchangePublicKeySignature = keyExchangePublicKeySignature;
        this.signatureVerificationKey = signatureVerificationKey;
        this.keyExchangePublicKey = keyExchangePublicKey;
        this.username = username;
        this.isBeforeLogin = isBeforeLogin;
        this.ecDomainParameters = ecDomainParameters;
        this.ellipticCurve = ellipticCurve;
        this.sessionId = sessionId;
    }
}
