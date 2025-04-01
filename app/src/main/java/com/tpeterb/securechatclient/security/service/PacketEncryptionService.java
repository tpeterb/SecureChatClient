package com.tpeterb.securechatclient.security.service;

import androidx.core.util.Pair;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tpeterb.securechatclient.exception.AsymmetricEncryptionException;
import com.tpeterb.securechatclient.messages.model.MessageDTO;
import com.tpeterb.securechatclient.messages.service.MessagePartitioningService;
import com.tpeterb.securechatclient.security.cache.ServerPublicKeyCache;
import com.tpeterb.securechatclient.security.cache.SessionKeyCache;
import com.tpeterb.securechatclient.security.model.EncryptedPacket;
import com.tpeterb.securechatclient.security.model.EncryptedReceivedMessageDTO;
import com.tpeterb.securechatclient.security.model.EncryptedSentMessageChunkDTO;
import com.tpeterb.securechatclient.security.model.EncryptedSentMessageDTO;
import com.tpeterb.securechatclient.security.model.EncryptionResult;
import com.tpeterb.securechatclient.users.session.UserSession;

import org.bouncycastle.crypto.params.AsymmetricKeyParameter;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Singleton
@Slf4j
public class PacketEncryptionService {

    @Getter
    private final SymmetricCipherService symmetricCipherService;

    private final AsymmetricCipherService asymmetricCipherService;

    private final MessagePartitioningService messagePartitioningService;

    private final SessionKeyCache sessionKeyCache;

    private final ServerPublicKeyCache serverPublicKeyCache;

    private final UserSession userSession;

    private final SessionKeyService sessionKeyService;

    private final ObjectMapper objectMapper;

    @Inject
    public PacketEncryptionService(SymmetricCipherService symmetricCipherService,
                                   AsymmetricCipherService asymmetricCipherService,
                                   MessagePartitioningService messagePartitioningService,
                                   SessionKeyCache sessionKeyCache,
                                   ServerPublicKeyCache serverPublicKeyCache,
                                   UserSession userSession,
                                   SessionKeyService sessionKeyService,
                                   ObjectMapper objectMapper) {
        this.symmetricCipherService = symmetricCipherService;
        this.asymmetricCipherService = asymmetricCipherService;
        this.messagePartitioningService = messagePartitioningService;
        this.sessionKeyCache = sessionKeyCache;
        this.serverPublicKeyCache = serverPublicKeyCache;
        this.userSession = userSession;
        this.sessionKeyService = sessionKeyService;
        this.objectMapper = objectMapper;
    }

    public byte[] generateSymmetricKeyForMessageContentEncryption() {
        return symmetricCipherService.generateSymmetricKey();
    }

    public EncryptionResult encryptMessageContent(String messageContent, byte[] messageContentEncryptionKey) {
        return symmetricCipherService.encryptData(messageContent.getBytes(StandardCharsets.UTF_8), messageContentEncryptionKey);
    }

    public byte[] encryptMessageContentEncryptionKey(byte[] messageContentEncryptionKey) throws AsymmetricEncryptionException {
        return asymmetricCipherService.encryptData(messageContentEncryptionKey, serverPublicKeyCache.getServerPublicKeyForChatMessages());
    }

    public List<MessageDTO> decryptFullChatMessages(List<EncryptedReceivedMessageDTO> encryptedReceivedMessages) {
        List<MessageDTO> decryptedMessages = new ArrayList<>();
        for (final EncryptedReceivedMessageDTO encryptedReceivedMessageDTO : encryptedReceivedMessages) {
            byte[] decryptedMessageContent = symmetricCipherService.decryptData(
                    encryptedReceivedMessageDTO.getContent().getEncryptedData(),
                    encryptedReceivedMessageDTO.getContentEncryptionKey(),
                    encryptedReceivedMessageDTO.getContent().getInitializationVector()
            );
            decryptedMessages.add(new MessageDTO(
                    encryptedReceivedMessageDTO.getId(),
                    encryptedReceivedMessageDTO.getSender(),
                    encryptedReceivedMessageDTO.getReceiver(),
                    new String(decryptedMessageContent, StandardCharsets.UTF_8),
                    encryptedReceivedMessageDTO.getMessageContentType(),
                    encryptedReceivedMessageDTO.getTimestamp()
            ));
        }
        return decryptedMessages;
    }

    public MessageDTO decryptFullChatMessage(EncryptedReceivedMessageDTO encryptedReceivedMessageDTO) {
        byte[] decryptedMessageContent = symmetricCipherService.decryptData(
                encryptedReceivedMessageDTO.getContent().getEncryptedData(),
                encryptedReceivedMessageDTO.getContentEncryptionKey(),
                encryptedReceivedMessageDTO.getContent().getInitializationVector()
        );
        return new MessageDTO(
            encryptedReceivedMessageDTO.getId(),
            encryptedReceivedMessageDTO.getSender(),
            encryptedReceivedMessageDTO.getReceiver(),
            new String(decryptedMessageContent, StandardCharsets.UTF_8),
            encryptedReceivedMessageDTO.getMessageContentType(),
            encryptedReceivedMessageDTO.getTimestamp()
        );
    }

    public List<EncryptedPacket> wrapTooBigChatMessageInEncryptedChunkPackets(MessageDTO messageDTO) throws AsymmetricEncryptionException, JsonProcessingException {
        byte[] fullMessageContentEncryptionKey = symmetricCipherService.generateSymmetricKey();
        log.info("TITKOSÍTATLAN CONTENT ENCRYPTION KEY = {}", fullMessageContentEncryptionKey);
        EncryptionResult fullMessageContentEncryptionResult = symmetricCipherService.encryptData(messageDTO.getContent().getBytes(StandardCharsets.UTF_8), fullMessageContentEncryptionKey);
        log.info("TITKOSÍTOTT EGÉSZ ÜZENET: {}", fullMessageContentEncryptionResult.getEncryptedData());
        log.info("INITIALIZATION VECTOR: {}", fullMessageContentEncryptionResult.getInitializationVector());
        AsymmetricKeyParameter serverPublicKey = serverPublicKeyCache.getServerPublicKeyForChatMessages();
        byte[] encryptedSender = asymmetricCipherService.encryptData(messageDTO.getSender().getBytes(StandardCharsets.UTF_8), serverPublicKey);
        byte[] encryptedReceiver = asymmetricCipherService.encryptData(messageDTO.getReceiver().getBytes(StandardCharsets.UTF_8), serverPublicKey);
        byte[] encryptedFullMessageContentEncryptionKey = asymmetricCipherService.encryptData(fullMessageContentEncryptionKey, serverPublicKey);
        log.info("TITKOSÍTOTT CONTENT ENCRYPTION KEY = {}", encryptedFullMessageContentEncryptionKey);
        EncryptedSentMessageDTO encryptedSentMessageDTO = EncryptedSentMessageDTO.builder()
                .id(messageDTO.getId())
                .sender(encryptedSender)
                .receiver(encryptedReceiver)
                .content(fullMessageContentEncryptionResult)
                .messageContentType(messageDTO.getMessageContentType())
                .timestamp(messageDTO.getTimestamp())
                .contentEncryptionKey(encryptedFullMessageContentEncryptionKey)
                .build();
        List<EncryptedSentMessageChunkDTO> messageChunks = messagePartitioningService.partitionMessageIntoChunks(encryptedSentMessageDTO);
        List<EncryptedPacket> encryptedMessageChunkPackets = new ArrayList<>();
        for (final EncryptedSentMessageChunkDTO encryptedSentMessageChunkDTO : messageChunks) {
            log.info("CHUNK NUMBER = {}, ENCRYPTED CHUNK CONTENT = {}", encryptedSentMessageChunkDTO.getSerialNumberWithinFullMessage(), encryptedSentMessageChunkDTO.getContent());
            byte[] serializedEncryptedMessageChunk = objectMapper.writeValueAsBytes(encryptedSentMessageChunkDTO);
            EncryptedPacket encryptedPacket = encryptEntirePacket(serializedEncryptedMessageChunk);
            log.info("ENCRYPTED CHUNK PACKET = {}", encryptedPacket);
            encryptedMessageChunkPackets.add(encryptedPacket);
        }
        return encryptedMessageChunkPackets;
    }

    public EncryptedPacket wrapFullChatMessageInEncryptedPacket(MessageDTO messageDTO) throws AsymmetricEncryptionException, JsonProcessingException {
        byte[] contentEncryptionKey = symmetricCipherService.generateSymmetricKey();
        AsymmetricKeyParameter serverPublicKey = serverPublicKeyCache.getServerPublicKeyForChatMessages();
        EncryptionResult encryptedMessageContent = symmetricCipherService.encryptData(messageDTO.getContent().getBytes(StandardCharsets.UTF_8), contentEncryptionKey);
        byte[] encryptedContentEncryptionKey = asymmetricCipherService.encryptData(contentEncryptionKey, serverPublicKey);
        byte[] encryptedSender = asymmetricCipherService.encryptData(messageDTO.getSender().getBytes(StandardCharsets.UTF_8), serverPublicKey);
        byte[] encryptedReceiver = asymmetricCipherService.encryptData(messageDTO.getReceiver().getBytes(StandardCharsets.UTF_8), serverPublicKey);
        EncryptedSentMessageDTO encryptedMessageDTO = EncryptedSentMessageDTO.builder()
                .id(messageDTO.getId())
                .sender(encryptedSender)
                .receiver(encryptedReceiver)
                .content(encryptedMessageContent)
                .messageContentType(messageDTO.getMessageContentType())
                .timestamp(messageDTO.getTimestamp())
                .contentEncryptionKey(encryptedContentEncryptionKey)
                .build();
        byte[] serializedEncryptedMessage = objectMapper.writeValueAsBytes(encryptedMessageDTO);
        return encryptEntirePacket(serializedEncryptedMessage);
    }

    public byte[] decryptEntirePacket(EncryptedPacket encryptedPacket) {
        log.info("SESSION KEY AFTER RECEIVAL: {}", sessionKeyCache.getSessionKey());
        byte[] oneTimeDecryptionKey = sessionKeyService.generateOneTimeDecryptionKeyFromNonce(encryptedPacket.getNonce());
        return symmetricCipherService.decryptData(
                encryptedPacket.getEncryptionResult().getEncryptedData(),
                oneTimeDecryptionKey,
                encryptedPacket.getEncryptionResult().getInitializationVector()
        );
    }

    public EncryptedPacket encryptEntirePacket(byte[] packet) {
        Pair<byte[], byte[]> oneTimeEncryptionKeyData = sessionKeyService.generateOneTimeEncryptionKeyFromSessionKey();
        byte[] oneTimeEncryptionKey = oneTimeEncryptionKeyData.first;
        byte[] nonce = oneTimeEncryptionKeyData.second;
        EncryptionResult packetEncryptionResult = symmetricCipherService.encryptData(
                packet,
                oneTimeEncryptionKey);
        return EncryptedPacket.builder()
                .sessionId(userSession.getSessionId())
                .nonce(nonce)
                .encryptionResult(packetEncryptionResult)
                .build();
    }

}
