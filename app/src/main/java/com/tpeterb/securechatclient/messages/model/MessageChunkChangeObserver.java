package com.tpeterb.securechatclient.messages.model;

import com.tpeterb.securechatclient.security.model.EncryptedReceivedMessageChunkDTO;

import java.util.Set;
import java.util.TreeSet;

public interface MessageChunkChangeObserver {

    void handleAllMessageChunksReceivedEvent(TreeSet<EncryptedReceivedMessageChunkDTO> fullyReceivedMessageChunks);

}
