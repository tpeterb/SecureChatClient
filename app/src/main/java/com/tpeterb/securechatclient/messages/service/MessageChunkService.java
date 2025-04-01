package com.tpeterb.securechatclient.messages.service;

import android.os.Handler;

import com.tpeterb.securechatclient.messages.listener.PersonalChatMessageSingleUpdateListener;
import com.tpeterb.securechatclient.messages.model.MessageChunkChangeObserver;
import com.tpeterb.securechatclient.messages.model.MessageDTO;
import com.tpeterb.securechatclient.messages.registry.ChatPartnerRegistry;
import com.tpeterb.securechatclient.messages.registry.MessageChunkRegistry;
import com.tpeterb.securechatclient.messages.registry.MessageRegistry;
import com.tpeterb.securechatclient.security.model.EncryptedReceivedMessageChunkDTO;
import com.tpeterb.securechatclient.security.model.EncryptedReceivedMessageDTO;
import com.tpeterb.securechatclient.security.service.PacketEncryptionService;
import com.tpeterb.securechatclient.users.model.ChatPartner;

import java.util.Collections;
import java.util.Objects;
import java.util.TreeSet;

import javax.inject.Inject;
import javax.inject.Singleton;

import lombok.Setter;

@Singleton
public class MessageChunkService implements MessageChunkChangeObserver {

    private final ChatPartnerRegistry chatPartnerRegistry;

    private final PacketEncryptionService packetEncryptionService;

    private final MessageRegistry messageRegistry;

    private final MessagePartitioningService messagePartitioningService;

    private final MessageService messageService;

    private final MessageChunkRegistry messageChunkRegistry;

    @Setter
    private PersonalChatMessageSingleUpdateListener personalChatMessageSingleUpdateListener;

    @Setter
    private Handler handler;

    @Inject
    public MessageChunkService(
            ChatPartnerRegistry chatPartnerRegistry,
            PacketEncryptionService packetEncryptionService,
            MessageRegistry messageRegistry,
            MessagePartitioningService messagePartitioningService,
            MessageService messageService,
            MessageChunkRegistry messageChunkRegistry) {
        this.chatPartnerRegistry = chatPartnerRegistry;
        this.packetEncryptionService = packetEncryptionService;
        this.messageRegistry = messageRegistry;
        this.messagePartitioningService = messagePartitioningService;
        this.messageService = messageService;
        this.messageChunkRegistry = messageChunkRegistry;
        this.messageChunkRegistry.subscribe(this);
    }

    @Override
    public void handleAllMessageChunksReceivedEvent(TreeSet<EncryptedReceivedMessageChunkDTO> fullyReceivedMessageChunks) {
        EncryptedReceivedMessageChunkDTO firstMessageChunk = fullyReceivedMessageChunks.first();
        messageService.sendAcknowledgementForFullMessage(firstMessageChunk.getFullMessageId());
        EncryptedReceivedMessageDTO fullMessage = messagePartitioningService.assembleMessageChunks(fullyReceivedMessageChunks);
        MessageDTO decryptedFullChatMessage = packetEncryptionService.decryptFullChatMessage(fullMessage);
        String messageSenderUsername = decryptedFullChatMessage.getSender();
        messageChunkRegistry.removeAllMessageChunksForMessage(fullMessage.getId());
        chatPartnerRegistry.addChatPartnerToRegistry(new ChatPartner(messageSenderUsername));
        messageRegistry.addMessageToRegistry(messageSenderUsername, decryptedFullChatMessage);
        Collections.sort(messageRegistry.getMessagesForChatPartner(messageSenderUsername));
        if (Objects.nonNull(handler)) {
            handler.post(() -> {
                personalChatMessageSingleUpdateListener.onNewMessage(decryptedFullChatMessage);
            });
        }
    }

}
