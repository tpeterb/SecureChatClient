package com.tpeterb.securechatclient.messages.listener;

import com.tpeterb.securechatclient.messages.model.MessageDTO;

import java.util.List;

@FunctionalInterface
public interface PersonalChatMessageBulkUpdateListener {

    void onNewMessages(List<MessageDTO> messages);

}
