package com.tpeterb.securechatclient.security.model;

import org.bouncycastle.crypto.params.AsymmetricKeyParameter;

import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

@Value
@Builder
@Jacksonized
public class ServerMessagePublicKeyResponseDTO {

    byte[] serverPublicKey;

    String responseId;

}