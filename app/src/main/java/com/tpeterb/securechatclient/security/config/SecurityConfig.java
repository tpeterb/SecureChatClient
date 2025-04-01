package com.tpeterb.securechatclient.security.config;

import javax.inject.Inject;
import javax.inject.Singleton;

import lombok.Value;

@Singleton
@Value
public class SecurityConfig {

    int keyExchangeRetryAttempts = 3;

    int saltLengthForKeyDerivationInBits = 512;

    int symmetricCipherKeyLengthInBits = 256;

    @Inject
    public SecurityConfig() {}

}