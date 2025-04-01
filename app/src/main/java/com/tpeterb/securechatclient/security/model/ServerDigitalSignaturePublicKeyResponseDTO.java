package com.tpeterb.securechatclient.security.model;

import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

@Value
@Builder
@Jacksonized
public class ServerDigitalSignaturePublicKeyResponseDTO {

    byte[] serverDigitalSignaturePublicKey;

}
