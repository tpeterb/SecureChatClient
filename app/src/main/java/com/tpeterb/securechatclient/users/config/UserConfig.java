package com.tpeterb.securechatclient.users.config;

import javax.inject.Inject;
import javax.inject.Singleton;

import lombok.Value;

@Value
@Singleton
public class UserConfig {

    @Inject
    public UserConfig() {}

    int registrationRetryAttempts = 5;

    int registrationRetryBackoffMs = 1000;

    int registrationMaxWaitMs = 10000;

    int loginRetryAttempts = 5;

    int loginRetryBackoffMs = 1000;

    int loginMaxWaitMs = 10000;

}
