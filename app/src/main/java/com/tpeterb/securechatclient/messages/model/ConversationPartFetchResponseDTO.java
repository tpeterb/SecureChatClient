package com.tpeterb.securechatclient.messages.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.tpeterb.securechatclient.security.model.EncryptedReceivedMessageDTO;

import java.util.List;

import lombok.Value;

@Value
public class ConversationPartFetchResponseDTO {

    String conversationPartId;

    String chatPartnerUsername;

    List<EncryptedReceivedMessageDTO> messages;

    @JsonCreator
    public ConversationPartFetchResponseDTO(
            @JsonProperty("conversationPartId") String conversationPartId,
            @JsonProperty("chatPartnerUsername") String chatPartnerUsername,
            @JsonProperty("messages") List<EncryptedReceivedMessageDTO> messages) {
        this.conversationPartId = conversationPartId;
        this.chatPartnerUsername = chatPartnerUsername;
        this.messages = messages;
    }

}
