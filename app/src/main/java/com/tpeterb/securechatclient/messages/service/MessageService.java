package com.tpeterb.securechatclient.messages.service;

import static com.tpeterb.securechatclient.constants.Constants.CHAT_SERVER_CONVERSATION_PART_FETCH_REQUEST_DESTINATION;
import static com.tpeterb.securechatclient.constants.Constants.CHAT_SERVER_MESSAGE_SINGLE_SENDING_DESTINATION;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tpeterb.securechatclient.messages.delivery.WebSocketClient;
import com.tpeterb.securechatclient.messages.model.ConversationPartFetchRequestDTO;
import com.tpeterb.securechatclient.messages.model.MessageDTO;
import com.tpeterb.securechatclient.messages.registry.MessageRegistry;
import com.tpeterb.securechatclient.users.session.UserSession;

import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.UUID;

import javax.inject.Inject;
import javax.inject.Singleton;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Singleton
public class MessageService {

    private final ObjectMapper objectMapper;

    private final WebSocketClient webSocketClient;

    private final UserSession userSession;

    private final MessageRegistry messageRegistry;

    @Inject
    public MessageService(ObjectMapper objectMapper, WebSocketClient webSocketClient, UserSession userSession, MessageRegistry messageRegistry) {
        this.objectMapper = objectMapper;
        this.webSocketClient = webSocketClient;
        this.userSession = userSession;
        this.messageRegistry = messageRegistry;
    }

    public void initiateFetchingLastMessagesForConversation(int numberOfAlreadyFetchedMessages, int messageCount, String loggedInUsername, String otherChatParticipantUsername) {

        ConversationPartFetchRequestDTO conversationPartFetchRequest = new ConversationPartFetchRequestDTO(
                loggedInUsername,
                otherChatParticipantUsername,
                numberOfAlreadyFetchedMessages,
                messageCount
        );
        String conversationPartFetchRequestJsonString;
        try {
            conversationPartFetchRequestJsonString = objectMapper.writeValueAsString(conversationPartFetchRequest);
        } catch (JsonProcessingException e) {
            log.error("There was an error while converting a conversation part fetch request to json, reason: {}", e.getMessage());
            return;
        }
        webSocketClient.sendMessage(conversationPartFetchRequestJsonString, CHAT_SERVER_CONVERSATION_PART_FETCH_REQUEST_DESTINATION);

    }

    public void sendMessageToChatPartner(String messageContent, String chatPartnerUsername) {

        MessageDTO message = new MessageDTO(
                UUID.randomUUID().toString(),
                userSession.getUsername(),
                chatPartnerUsername,
                messageContent,
                ZonedDateTime.now(),
                false
        );
        messageRegistry.addMessageToRegistry(chatPartnerUsername, message);
        Collections.sort(messageRegistry.getMessagesForChatPartner(chatPartnerUsername));
        String jsonMessage;
        try {
            jsonMessage = objectMapper.writeValueAsString(message);
        } catch (JsonProcessingException e) {
            log.error("There was an error while trying to serialize the message to send to JSON, reason: {}", e.getMessage());
            return;
        }
        webSocketClient.sendMessage(jsonMessage, CHAT_SERVER_MESSAGE_SINGLE_SENDING_DESTINATION);

    }

}
