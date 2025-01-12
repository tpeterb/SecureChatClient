package com.tpeterb.securechatclient.messages.registry;

import com.tpeterb.securechatclient.users.model.ChatPartner;

import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import javax.inject.Inject;
import javax.inject.Singleton;

import lombok.extern.slf4j.Slf4j;

@Singleton
@Slf4j
public class ChatPartnerRegistry {

    private final Set<ChatPartner> chatPartnerRegistry;

    private volatile boolean isInitialFetchDone;

    @Inject
    public ChatPartnerRegistry() {
        chatPartnerRegistry = new TreeSet<>(Comparator.comparing(ChatPartner::getUsername));
        isInitialFetchDone = false;
    }

    public synchronized void clearChatPartnerRegistry() {
        chatPartnerRegistry.clear();
        isInitialFetchDone = false;
    }

    public synchronized void addChatPartnerToRegistry(ChatPartner chatPartner) {
        chatPartnerRegistry.add(chatPartner);
    }

    public synchronized void replaceChatPartners(List<ChatPartner> chatPartners) {
        chatPartnerRegistry.clear();
        chatPartnerRegistry.addAll(chatPartners);
    }

    public synchronized void addAllChatPartners(Collection<ChatPartner> chatPartners) {
        chatPartnerRegistry.addAll(chatPartners);
    }

    public Set<ChatPartner> getChatPartnerRegistry() {
        log.info("Chat partner registry = {}", chatPartnerRegistry);
        return Collections.synchronizedSet(chatPartnerRegistry);
    }

    public synchronized void setInitialFetchDone(boolean initialFetchDone) {
        isInitialFetchDone = initialFetchDone;
    }

    public synchronized boolean isInitialFetchDone() {
        return isInitialFetchDone;
    }

    @Override
    public String toString() {
        return "chatPartnerRegistry = " + chatPartnerRegistry +
                "\nisInitialFetchDone = " + isInitialFetchDone;
    }

}
