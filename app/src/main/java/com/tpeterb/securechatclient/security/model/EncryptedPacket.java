package com.tpeterb.securechatclient.security.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

@Value
@Builder
@Jacksonized
@JsonIgnoreProperties(ignoreUnknown = true)
public class EncryptedPacket {

    String sessionId;

    byte[] nonce;

    EncryptionResult encryptionResult;

}
