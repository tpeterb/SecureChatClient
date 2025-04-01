package com.tpeterb.securechatclient.messages.service;

import static com.tpeterb.securechatclient.constants.Constants.MESSAGE_SLICING_SIZE_THRESHOLD_IN_BYTES;

import com.tpeterb.securechatclient.security.model.EncryptedReceivedMessageChunkDTO;
import com.tpeterb.securechatclient.security.model.EncryptedReceivedMessageDTO;
import com.tpeterb.securechatclient.security.model.EncryptedSentMessageChunkDTO;
import com.tpeterb.securechatclient.security.model.EncryptedSentMessageDTO;
import com.tpeterb.securechatclient.security.model.EncryptionResult;
import com.tpeterb.securechatclient.users.session.UserSession;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import javax.inject.Inject;
import javax.inject.Singleton;

import lombok.extern.slf4j.Slf4j;

@Singleton
@Slf4j
public class MessagePartitioningService {

    private final UserSession userSession;

    @Inject
    public MessagePartitioningService(UserSession userSession) {
        this.userSession = userSession;
    }

    public EncryptedReceivedMessageDTO assembleMessageChunks(Set<EncryptedReceivedMessageChunkDTO> messageChunks) {
        EncryptedReceivedMessageChunkDTO firstMessageChunk = messageChunks.stream().findFirst().get();
        byte[] fullMessageBytes = new byte[firstMessageChunk.getSizeOfFullMessageInBytes()];
        for (EncryptedReceivedMessageChunkDTO messageChunkDTO : messageChunks) {
            byte[] messageChunkContentBytes = messageChunkDTO.getContent();
            System.arraycopy(
                    messageChunkContentBytes,
                    0,
                    fullMessageBytes,
                    MESSAGE_SLICING_SIZE_THRESHOLD_IN_BYTES * (messageChunkDTO.getSerialNumberWithinFullMessage() - 1),
                    messageChunkContentBytes.length
            );
        }
        return EncryptedReceivedMessageDTO.builder()
                .id(firstMessageChunk.getFullMessageId())
                .sender(firstMessageChunk.getSender())
                .receiver(firstMessageChunk.getReceiver())
                .content(new EncryptionResult(
                        fullMessageBytes,
                        firstMessageChunk.getFullMessageContentInitializationVector()
                ))
                .messageContentType(firstMessageChunk.getMessageContentType())
                .timestamp(Instant.now())
                .contentEncryptionKey(firstMessageChunk.getFullMessageContentEncryptionKey())
                .build();
    }

    public List<EncryptedSentMessageChunkDTO> partitionMessageIntoChunks(EncryptedSentMessageDTO message) {
        List<EncryptedSentMessageChunkDTO> messageChunks = new ArrayList<>();
        byte[] messageBytes = message.getContent().getEncryptedData();
        log.info("TELJES TITKOSÍTOTT ÜZENET MÉRETE = {} bájt", messageBytes.length);
        int numberOfChunks = (int) Math.ceil((double) messageBytes.length / MESSAGE_SLICING_SIZE_THRESHOLD_IN_BYTES);
        for (int i = 1; i <= numberOfChunks; i++) {
            byte[] messageChunkBytes = new byte[i == numberOfChunks
                    ? messageBytes.length - (i - 1) * MESSAGE_SLICING_SIZE_THRESHOLD_IN_BYTES
                    : MESSAGE_SLICING_SIZE_THRESHOLD_IN_BYTES];
            System.arraycopy(
                    messageBytes,
                    (i - 1) * MESSAGE_SLICING_SIZE_THRESHOLD_IN_BYTES,
                    messageChunkBytes,
                    0,
                    messageChunkBytes.length);
            log.info("i = {}, messageChunkBytes size = {}", i, messageChunkBytes.length);
            EncryptedSentMessageChunkDTO encryptedSentMessageChunkDTO = EncryptedSentMessageChunkDTO.builder()
                    .sessionId(userSession.getSessionId())
                    .fullMessageId(message.getId())
                    .messageChunkId(UUID.randomUUID().toString())
                    .serialNumberWithinFullMessage(i)
                    .numberOfChunksOfFullMessage(numberOfChunks)
                    .sizeOfFullMessageInBytes(messageBytes.length)
                    .sender(message.getSender())
                    .receiver(message.getReceiver())
                    .content(messageChunkBytes)
                    .messageContentType(message.getMessageContentType())
                    .timestamp(message.getTimestamp())
                    .fullMessageContentEncryptionKey(message.getContentEncryptionKey())
                    .fullMessageContentInitializationVector(message.getContent().getInitializationVector())
                    .build();
            log.info("CHUNK: FULL MESSAGE ID = {}", encryptedSentMessageChunkDTO.getFullMessageId());
            log.info("CHUNK: CHUNK ID = {}", encryptedSentMessageChunkDTO.getMessageChunkId());
            log.info("CHUNK: SERIAL NUMBER = {}", encryptedSentMessageChunkDTO.getSerialNumberWithinFullMessage());
            log.info("CHUNK: NOC = {}", encryptedSentMessageChunkDTO.getNumberOfChunksOfFullMessage());
            messageChunks.add(encryptedSentMessageChunkDTO);
        }
        return messageChunks;
    }

}
