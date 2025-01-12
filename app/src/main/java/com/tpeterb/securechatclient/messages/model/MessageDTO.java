package com.tpeterb.securechatclient.messages.model;

import android.annotation.SuppressLint;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.ZonedDateTime;

import lombok.Value;

@Value
public class MessageDTO implements Comparable<MessageDTO> {

    String id;

    String sender;

    String receiver;

    String content;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'", timezone = "UTC")
    ZonedDateTime timestamp;

    boolean encrypted;

    @JsonCreator
    public MessageDTO(
            @JsonProperty("id") String id,
            @JsonProperty("sender") String sender,
            @JsonProperty("receiver") String receiver,
            @JsonProperty("content") String content,
            @JsonProperty("timestamp") ZonedDateTime timestamp,
            @JsonProperty("encrypted") boolean encrypted
    ) {
        this.id = id;
        this.sender = sender;
        this.receiver = receiver;
        this.content = content;
        this.timestamp = timestamp;
        this.encrypted = encrypted;
    }

    @Override
    public int compareTo(MessageDTO messageDTO) {
        return timestamp.compareTo(messageDTO.getTimestamp());
    }
}
