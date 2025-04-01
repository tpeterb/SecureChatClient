package com.tpeterb.securechatclient.messages.service;

import static com.tpeterb.securechatclient.constants.Constants.CHAT_SERVER_CONVERSATION_PART_FETCH_REQUEST_DESTINATION;
import static com.tpeterb.securechatclient.constants.Constants.CHAT_SERVER_FULL_MESSAGE_RECEIPT_ACKNOWLEDGEMENT_DESTINATION;
import static com.tpeterb.securechatclient.constants.Constants.CHAT_SERVER_MESSAGE_SINGLE_CHUNK_SENDING_DESTINATION;
import static com.tpeterb.securechatclient.constants.Constants.CHAT_SERVER_MESSAGE_SINGLE_SENDING_DESTINATION;
import static com.tpeterb.securechatclient.constants.Constants.MESSAGE_SLICING_SIZE_THRESHOLD_IN_BYTES;
import static com.tpeterb.securechatclient.constants.Constants.SERVER_PUBLIC_KEY_FOR_MESSAGES_REQUEST_DESTINATION;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tpeterb.securechatclient.exception.AsymmetricEncryptionException;
import com.tpeterb.securechatclient.messages.delivery.WebSocketClient;
import com.tpeterb.securechatclient.messages.model.ConversationPartFetchRequestDTO;
import com.tpeterb.securechatclient.messages.model.FullMessageAcknowledgementDTO;
import com.tpeterb.securechatclient.messages.model.MessageChunkDTO;
import com.tpeterb.securechatclient.messages.model.MessageContentType;
import com.tpeterb.securechatclient.messages.model.MessageDTO;
import com.tpeterb.securechatclient.messages.registry.MessageRegistry;
import com.tpeterb.securechatclient.security.model.EncryptedPacket;
import com.tpeterb.securechatclient.security.model.EncryptedSentMessageChunkDTO;
import com.tpeterb.securechatclient.security.model.EncryptionResult;
import com.tpeterb.securechatclient.security.model.ServerMessagePublicKeyRequestDTO;
import com.tpeterb.securechatclient.security.service.PacketEncryptionService;
import com.tpeterb.securechatclient.users.session.UserSession;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import javax.inject.Inject;
import javax.inject.Singleton;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Singleton
public class MessageService {

    private final ObjectMapper objectMapper;

    private final PacketEncryptionService packetEncryptionService;

    private final WebSocketClient webSocketClient;

    private final MessagePartitioningService messagePartitioningService;

    private final UserSession userSession;

    private final MessageRegistry messageRegistry;

    @Inject
    public MessageService(ObjectMapper objectMapper,
                          PacketEncryptionService packetEncryptionService,
                          WebSocketClient webSocketClient,
                          MessagePartitioningService messagePartitioningService,
                          UserSession userSession,
                          MessageRegistry messageRegistry) {
        this.objectMapper = objectMapper;
        this.packetEncryptionService = packetEncryptionService;
        this.webSocketClient = webSocketClient;
        this.messagePartitioningService = messagePartitioningService;
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
        byte[] conversationPartFetchRequestBytes;
        try {
            conversationPartFetchRequestBytes = objectMapper.writeValueAsBytes(conversationPartFetchRequest);
        } catch (JsonProcessingException e) {
            log.error("There was an error while converting a conversation part fetch request to json, reason: {}", e.getMessage());
            return;
        }
        EncryptedPacket encryptedPacket = packetEncryptionService.encryptEntirePacket(conversationPartFetchRequestBytes);
        log.info("Session id BEFORE SENDING = {}", encryptedPacket.getSessionId());
        log.info("Encrypted packet data BEFORE SENDING = {}", encryptedPacket.getEncryptionResult().getEncryptedData());
        log.info("NONCE BEFORE SENDING = {}", encryptedPacket.getEncryptionResult().getInitializationVector());
        String serializedEncryptedPacket;
        try {
            serializedEncryptedPacket = objectMapper.writeValueAsString(encryptedPacket);
        } catch (JsonProcessingException e) {
            log.error("There was an error while converting an encrypted conversation part fetch request to json, reason: {}", e.getMessage());
            return;
        }
        webSocketClient.sendMessage(serializedEncryptedPacket, CHAT_SERVER_CONVERSATION_PART_FETCH_REQUEST_DESTINATION);

    }

    public void sendServerMessagePublicKeyRequest() {
        log.info("Assembling server message public key request");
        ServerMessagePublicKeyRequestDTO serverMessagePublicKeyRequestDTO = ServerMessagePublicKeyRequestDTO.builder()
                .publicKeyRecipientUsername(userSession.getUsername())
                .build();
        byte[] serializedPublicKeyRequest;
        try {
            serializedPublicKeyRequest = objectMapper.writeValueAsBytes(serverMessagePublicKeyRequestDTO);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize server public key request, reason: {}", e.getMessage());
            return;
        }
        EncryptedPacket encryptedPacket = packetEncryptionService.encryptEntirePacket(serializedPublicKeyRequest);
        String serializedEncryptedPacket;
        try {
            serializedEncryptedPacket = objectMapper.writeValueAsString(encryptedPacket);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize encrypted server public key request, reason: {}", e.getMessage());
            return;
        }
        log.info("Sending server message public key request, request = {}", serverMessagePublicKeyRequestDTO);
        webSocketClient.sendMessage(serializedEncryptedPacket, SERVER_PUBLIC_KEY_FOR_MESSAGES_REQUEST_DESTINATION);
    }

    public void sendMessageToChatPartner(String messageContent, MessageContentType messageContentType, String chatPartnerUsername) {
        MessageDTO message = new MessageDTO(
                UUID.randomUUID().toString(),
                userSession.getUsername(),
                chatPartnerUsername,
                messageContent,
                messageContentType,
                Instant.now()
        );
        messageRegistry.addMessageToRegistry(chatPartnerUsername, message);
        Collections.sort(messageRegistry.getMessagesForChatPartner(chatPartnerUsername));
        byte[] messageBytes = messageContent.getBytes(StandardCharsets.UTF_8);
        if (messageBytes.length <= MESSAGE_SLICING_SIZE_THRESHOLD_IN_BYTES) {
            sendEntireMessageToChatPartner(message);
        } else {
            sendMessageChunksToChatPartner(message);
        }
    }

    public void sendAcknowledgementForFullMessage(String messageId) {

        FullMessageAcknowledgementDTO acknowledgement = new FullMessageAcknowledgementDTO(messageId);
        byte[] acknowledgementBytes;
        try {
            acknowledgementBytes = objectMapper.writeValueAsBytes(acknowledgement);
        } catch (JsonProcessingException e) {
            log.error("There was an error while trying to serialize message acknowledgement! Reason: {}", e.getMessage());
            return;
        }
        EncryptedPacket encryptedPacket = packetEncryptionService.encryptEntirePacket(acknowledgementBytes);
        String serializedEncryptedPacket;
        try {
            serializedEncryptedPacket = objectMapper.writeValueAsString(encryptedPacket);
        } catch (JsonProcessingException e) {
            log.error("There was an error while trying to serialize encrypted message acknowledgement! Reason: {}", e.getMessage());
            return;
        }
        webSocketClient.sendMessage(serializedEncryptedPacket, CHAT_SERVER_FULL_MESSAGE_RECEIPT_ACKNOWLEDGEMENT_DESTINATION);

    }

    private void sendEntireMessageToChatPartner(MessageDTO message) {
        try {
            EncryptedPacket encryptedPacket = packetEncryptionService.wrapFullChatMessageInEncryptedPacket(message);
            String serializedEncryptedPacket = objectMapper.writeValueAsString(encryptedPacket);
            webSocketClient.sendMessage(serializedEncryptedPacket, CHAT_SERVER_MESSAGE_SINGLE_SENDING_DESTINATION);
        } catch (AsymmetricEncryptionException e) {
            log.error("There was an error while encrypting the message to send, reason: {}", e.getMessage());
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize message to send, reason: {}", e.getMessage());
        }
    }

    private void sendMessageChunksToChatPartner(MessageDTO messageDTO) {
        try {
            List<EncryptedPacket> encryptedMessageChunkPackets = packetEncryptionService.wrapTooBigChatMessageInEncryptedChunkPackets(messageDTO);
            for (final EncryptedPacket encryptedPacket : encryptedMessageChunkPackets) {
                String serializedEncryptedPacket = objectMapper.writeValueAsString(encryptedPacket);
                webSocketClient.sendMessage(serializedEncryptedPacket, CHAT_SERVER_MESSAGE_SINGLE_CHUNK_SENDING_DESTINATION);
                log.info("SERIALIZED JSON MESSAGE CHUNK IN sendMessageChunkToChatPartner IN MESSAGE SERVICE CLASS = {}", serializedEncryptedPacket);
            }
        } catch (JsonProcessingException e) {
            log.error("There was an error while trying to serialize the message chunk to send to JSON, reason: {}", e.getMessage());
        } catch (AsymmetricEncryptionException e) {
            log.error("Failed to encrypt message chunk to send, reason: {}", e.getMessage());
        }
    }

}
