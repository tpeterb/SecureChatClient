package com.tpeterb.securechatclient.security.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.tpeterb.securechatclient.messages.model.MessageContentType;

import java.time.Instant;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class EncryptedReceivedMessageChunkDTO {

    String fullMessageId;

    String messageChunkId;

    int serialNumberWithinFullMessage;

    int numberOfChunksOfFullMessage;

    int sizeOfFullMessageInBytes;

    String sender;

    String receiver;

    byte[] content;

    MessageContentType messageContentType;

    Instant timestamp;

    byte[] fullMessageContentEncryptionKey;

    byte[] fullMessageContentInitializationVector;

    @JsonCreator
    public EncryptedReceivedMessageChunkDTO(
            @JsonProperty("fullMessageId") String fullMessageId,
            @JsonProperty("messageChunkId") String messageChunkId,
            @JsonProperty("serialNumberWithinFullMessage") int serialNumberWithinFullMessage,
            @JsonProperty("numberOfChunksOfFullMessage") int numberOfChunksOfFullMessage,
            @JsonProperty("sizeOfFullMessageInBytes") int sizeOfFullMessageInBytes,
            @JsonProperty("sender") String sender,
            @JsonProperty("receiver") String receiver,
            @JsonProperty("content") byte[] content,
            @JsonProperty("messageContentType") MessageContentType messageContentType,
            @JsonProperty("timestamp") Instant timestamp,
            @JsonProperty("fullMessageContentEncryptionKey") byte[] fullMessageContentEncryptionKey,
            @JsonProperty("fullMessageContentInitializationVector") byte[] fullMessageContentInitializationVector
    ) {
        this.fullMessageId = fullMessageId;
        this.messageChunkId = messageChunkId;
        this.serialNumberWithinFullMessage = serialNumberWithinFullMessage;
        this.numberOfChunksOfFullMessage = numberOfChunksOfFullMessage;
        this.sizeOfFullMessageInBytes = sizeOfFullMessageInBytes;
        this.sender = sender;
        this.receiver = receiver;
        this.content = content;
        this.messageContentType = messageContentType;
        this.timestamp = timestamp;
        this.fullMessageContentEncryptionKey = fullMessageContentEncryptionKey;
        this.fullMessageContentInitializationVector = fullMessageContentInitializationVector;
    }

}
