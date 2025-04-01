package com.tpeterb.securechatclient.security.model;

import lombok.Getter;

public enum EllipticCurve {

    CURVE25519(32),

    CURVE448(56);

    @Getter
    private final int sharedSecretLengthInBytes;

    EllipticCurve(int sharedSecretLengthInBytes) {
        this.sharedSecretLengthInBytes = sharedSecretLengthInBytes;
    }

}
