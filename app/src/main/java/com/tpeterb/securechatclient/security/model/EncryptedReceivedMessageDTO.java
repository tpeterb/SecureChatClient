package com.tpeterb.securechatclient.security.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.tpeterb.securechatclient.messages.model.MessageContentType;

import java.time.Instant;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class EncryptedReceivedMessageDTO {

    String id;

    String sender;

    String receiver;

    EncryptionResult content;

    MessageContentType messageContentType;

    //@JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'", timezone = "UTC")
    Instant timestamp;

    byte[] contentEncryptionKey;

    @JsonCreator
    public EncryptedReceivedMessageDTO(
            @JsonProperty("id") String id,
            @JsonProperty("sender") String sender,
            @JsonProperty("receiver") String receiver,
            @JsonProperty("content") EncryptionResult content,
            @JsonProperty("messageContentType") MessageContentType messageContentType,
            @JsonProperty("timestamp") Instant timestamp,
            @JsonProperty("contentEncryptionKey") byte[] contentEncryptionKey
    ) {
        this.id = id;
        this.sender = sender;
        this.receiver = receiver;
        this.content = content;
        this.messageContentType = messageContentType;
        this.timestamp = timestamp;
        this.contentEncryptionKey = contentEncryptionKey;
    }

}
