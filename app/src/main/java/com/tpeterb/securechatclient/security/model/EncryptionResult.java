package com.tpeterb.securechatclient.security.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

@Value
@Builder
@AllArgsConstructor
@Jacksonized
public class EncryptionResult {

    byte[] encryptedData;

    byte[] initializationVector;

}
