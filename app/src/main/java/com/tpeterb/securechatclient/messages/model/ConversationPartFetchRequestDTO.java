package com.tpeterb.securechatclient.messages.model;

import lombok.Value;

@Value
public class ConversationPartFetchRequestDTO {

    String loggedInUsername;

    String otherChatParticipantUsername;

    int numberOfAlreadyFetchedMessages;

    int numberOfMessagesToFetch;

}
