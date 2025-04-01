package com.tpeterb.securechatclient.security.model;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class ServerMessagePublicKeyRequestDTO {

    String publicKeyRecipientUsername;

}