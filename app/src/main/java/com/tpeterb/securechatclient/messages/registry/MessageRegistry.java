package com.tpeterb.securechatclient.messages.registry;

import static com.tpeterb.securechatclient.constants.Constants.NUMBER_OF_MESSAGES_TO_FETCH_AT_ONCE;

import com.tpeterb.securechatclient.messages.model.MessageDTO;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.inject.Inject;
import javax.inject.Singleton;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Singleton
public class MessageRegistry {

    private final Map<String, List<MessageDTO>> messageRegistry;

    private final Map<String, Boolean> fetchRegistry;

    @Inject
    public MessageRegistry() {
        messageRegistry = new ConcurrentHashMap<>();
        fetchRegistry = new ConcurrentHashMap<>();
    }

    public synchronized void clearMessageRegistry() {
        messageRegistry.clear();
        fetchRegistry.clear();
    }

    public boolean conversationHasNoPreviousMessages(String chatPartnerUsername) {
        List<MessageDTO> messages = messageRegistry.get(chatPartnerUsername);
        log.info("conversationHasNoPreviousMessages, size = {}", Objects.nonNull(messages) ? messages.size() : "nincs ilyen");
        return Objects.nonNull(messages) && messages.size() % NUMBER_OF_MESSAGES_TO_FETCH_AT_ONCE != 0;
    }

    public boolean conversationNeedsInitialMessageFetching(String chatPartnerUsername) {
        log.info("conversationNeedMessageFetching");
        log.info("Message registry = {}", messageRegistry);
        log.info("Fetch registry = {}", fetchRegistry);
        return Objects.isNull(fetchRegistry.get(chatPartnerUsername));
    }

    public int getNumberOfAlreadyFetchedMessagesForChatParner(String chatPartnerUsername) {
        log.info("getNumberOfAlreadyFetchedMessagesForChatParner");
        List<MessageDTO> messages = messageRegistry.get(chatPartnerUsername);
        return Objects.nonNull(messages) ? messages.size() : 0;
    }

    public List<MessageDTO> getMessagesForChatPartner(String chatPartnerUsername) {
        log.info("getMessagesForChatPartner");
        List<MessageDTO> messages = messageRegistry.get(chatPartnerUsername);
        log.info("ChatPartnerUsername = {}", chatPartnerUsername);
        log.info("Messages = {}", messages);
        return Objects.nonNull(messages) ? messages : List.of();
    }

    public synchronized void addMessageToRegistry(String chatPartnerUsername, MessageDTO messageDTO) {
        messageRegistry.putIfAbsent(chatPartnerUsername, new CopyOnWriteArrayList<>());
        messageRegistry.get(chatPartnerUsername).add(messageDTO);
        fetchRegistry.putIfAbsent(chatPartnerUsername, Boolean.TRUE);
        log.info("Message registry = {}", messageRegistry);
        log.info("Fetch registry = {}", fetchRegistry);
    }

    public synchronized void addAllMessagesToRegistry(String chatPartnerUsername, List<MessageDTO> messages) {
        log.info("addAllMessagesToRegistry");
        messageRegistry.putIfAbsent(chatPartnerUsername, new CopyOnWriteArrayList<>());
        messageRegistry.get(chatPartnerUsername).addAll(messages);
        fetchRegistry.putIfAbsent(chatPartnerUsername, Boolean.TRUE);
        log.info("Message registry = {}", messageRegistry);
        log.info("Fetch registry = {}", fetchRegistry);
    }

    @Override
    public String toString() {
        return "MessageRegistry = " + messageRegistry +
                "\nFetch registry = " + fetchRegistry;
    }

}
