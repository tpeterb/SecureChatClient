package com.tpeterb.securechatclient.users.model;

public enum UserRegistrationResult {
    SUCCESS,
    CONFLICTED_USERNAME,
    CONFLICTED_EMAIL,
    BAD_USERNAME_FORMAT,
    BAD_PASSWORD_FORMAT,
    BAD_EMAIL_FORMAT,
    GENERAL_FAILURE
}
