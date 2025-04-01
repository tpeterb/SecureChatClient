package com.tpeterb.securechatclient.messages.model;

import com.tpeterb.securechatclient.security.model.EncryptedReceivedMessageChunkDTO;

import java.util.TreeSet;

public interface MessageChunkChangeSubject {

    void subscribe(MessageChunkChangeObserver messageChunkChangeObserver);

    void unsubscribe(MessageChunkChangeObserver messageChunkChangeObserver);

    void notifyObservers(TreeSet<EncryptedReceivedMessageChunkDTO> fullyReceivedMessageChunks);

}