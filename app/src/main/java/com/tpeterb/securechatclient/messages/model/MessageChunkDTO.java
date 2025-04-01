package com.tpeterb.securechatclient.messages.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;

import lombok.Value;

@Value
public class MessageChunkDTO {

    String fullMessageId;

    String messageChunkId;

    int serialNumberWithinFullMessage;

    int numberOfChunksOfFullMessage;

    int sizeOfFullMessageInBytes;

    String sender;

    String receiver;

    String content;

    MessageContentType messageContentType;

    Instant timestamp;

    @JsonCreator
    public MessageChunkDTO(
            @JsonProperty("fullMessageId") String fullMessageId,
            @JsonProperty("messageChunkId") String messageChunkId,
            @JsonProperty("serialNumberWithinFullMessage") int serialNumberWithinFullMessage,
            @JsonProperty("numberOfChunksOfFullMessage") int numberOfChunksOfFullMessage,
            @JsonProperty("sizeOfFullMessageInBytes") int sizeOfFullMessageInBytes,
            @JsonProperty("sender") String sender,
            @JsonProperty("receiver") String receiver,
            @JsonProperty("content") String content,
            @JsonProperty("messageContentType") MessageContentType messageContentType,
            @JsonProperty("timestamp") Instant timestamp
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
    }

}
