package com.tpeterb.securechatclient.messages.registry;

import com.tpeterb.securechatclient.messages.model.MessageChunkChangeObserver;
import com.tpeterb.securechatclient.messages.model.MessageChunkChangeSubject;
import com.tpeterb.securechatclient.messages.model.MessageChunkDTO;
import com.tpeterb.securechatclient.security.model.EncryptedReceivedMessageChunkDTO;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;
import javax.inject.Singleton;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Singleton
public class MessageChunkRegistry implements MessageChunkChangeSubject {

    private final ScheduledExecutorService scheduledExecutorService;

    private final List<MessageChunkChangeObserver> observers;

    private final ConcurrentHashMap<String, TreeSet<EncryptedReceivedMessageChunkDTO>> messageChunkRegistry;

    private final int maxWaitMsForAllChunksToArriveForAMessage;

    @Inject
    public MessageChunkRegistry() {
        scheduledExecutorService = Executors.newScheduledThreadPool(10);
        messageChunkRegistry = new ConcurrentHashMap<>();
        this.observers = new ArrayList<>();
        maxWaitMsForAllChunksToArriveForAMessage = 60000;
    }

    public synchronized int getNumberOfReceivedMessageChunksForMessage(String fullMessageId) {
        return messageChunkRegistry.get(fullMessageId).size();
    }

    public synchronized void addMessageChunk(EncryptedReceivedMessageChunkDTO messageChunkDTO) {
        String fullMessageId = messageChunkDTO.getFullMessageId();
        if (Objects.isNull(messageChunkRegistry.get(fullMessageId))) {
            messageChunkRegistry.put(fullMessageId, new TreeSet<>(Comparator.comparingInt(EncryptedReceivedMessageChunkDTO::getSerialNumberWithinFullMessage)));
            scheduledExecutorService.schedule(() -> {
                if (Objects.nonNull(messageChunkRegistry.get(fullMessageId)) && messageChunkRegistry.get(fullMessageId).size() != messageChunkDTO.getNumberOfChunksOfFullMessage()) {
                    log.error("All message chunks haven't been received for message with id {} in {} milliseconds from the arrival of the first message chunk, deleting the arrived chunks from the registry!",
                            fullMessageId, maxWaitMsForAllChunksToArriveForAMessage);
                    messageChunkRegistry.remove(fullMessageId);
                }
            }, maxWaitMsForAllChunksToArriveForAMessage, TimeUnit.MILLISECONDS);
        }
        TreeSet<EncryptedReceivedMessageChunkDTO> messageChunks = messageChunkRegistry.get(fullMessageId);
        messageChunks.add(messageChunkDTO);
        if (messageChunks.size() == messageChunkDTO.getNumberOfChunksOfFullMessage()) {
            notifyObservers(messageChunks);
        }
    }

    public void removeAllMessageChunksForMessage(String messageId) {
        messageChunkRegistry.remove(messageId);
    }

    @Override
    public void subscribe(MessageChunkChangeObserver messageChunkChangeObserver) {
        observers.add(messageChunkChangeObserver);
    }

    @Override
    public void unsubscribe(MessageChunkChangeObserver messageChunkChangeObserver) {
        observers.remove(messageChunkChangeObserver);
    }

    public void notifyObservers(TreeSet<EncryptedReceivedMessageChunkDTO> fullyReceivedMessageChunks) {
        for (MessageChunkChangeObserver observer : observers) {
            observer.handleAllMessageChunksReceivedEvent(fullyReceivedMessageChunks);
        }
    }

}
