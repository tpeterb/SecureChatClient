package com.tpeterb.securechatclient.ui.listener;

import com.tpeterb.securechatclient.users.model.ChatPartner;

@FunctionalInterface
public interface ChatListUserItemClickListener {

    void onChatListUserItemClick(ChatPartner chatPartner);

}
