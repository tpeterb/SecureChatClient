package com.tpeterb.securechatclient.messages.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;

import lombok.Value;

@Value
public class MessageDTO implements Comparable<MessageDTO> {

    String id;

    String sender;

    String receiver;

    String content;

    MessageContentType messageContentType;

    //@JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'", timezone = "UTC")
    Instant timestamp;

    @JsonCreator
    public MessageDTO(
            @JsonProperty("id") String id,
            @JsonProperty("sender") String sender,
            @JsonProperty("receiver") String receiver,
            @JsonProperty("content") String content,
            @JsonProperty("messageContentType") MessageContentType messageContentType,
            @JsonProperty("timestamp") Instant timestamp
    ) {
        this.id = id;
        this.sender = sender;
        this.receiver = receiver;
        this.content = content;
        this.messageContentType = messageContentType;
        this.timestamp = timestamp;
    }

    @Override
    public int compareTo(MessageDTO messageDTO) {
        return timestamp.compareTo(messageDTO.getTimestamp());
    }

}
