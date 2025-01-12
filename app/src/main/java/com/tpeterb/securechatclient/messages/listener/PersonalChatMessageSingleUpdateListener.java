package com.tpeterb.securechatclient.messages.listener;

import com.tpeterb.securechatclient.messages.model.MessageDTO;

@FunctionalInterface
public interface PersonalChatMessageSingleUpdateListener {

    void onNewMessage(MessageDTO messageDTO);

}
