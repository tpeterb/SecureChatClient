package com.tpeterb.securechatclient.ui.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.tpeterb.securechatclient.R;
import com.tpeterb.securechatclient.ui.listener.ChatListUserItemClickListener;
import com.tpeterb.securechatclient.users.model.ChatPartner;

import java.util.Collection;
import java.util.List;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public class ChatListAdapter extends RecyclerView.Adapter<ChatListAdapter.ChatViewHolder> {

    private Context context;

    private List<ChatPartner> chatPartnerList;

    private ChatListUserItemClickListener chatListUserItemClickListener;

    @NonNull
    @Override
    public ChatViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.chat_list_user_item, parent, false);
        return new ChatViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ChatViewHolder holder, int position) {
        ChatPartner chatPartner = chatPartnerList.get(position);
        holder.usernameTextView.setText(chatPartner.getUsername());
        holder.itemView.setOnClickListener(view -> chatListUserItemClickListener.onChatListUserItemClick(chatPartner));
    }

    @Override
    public int getItemCount() {
        return chatPartnerList.size();
    }

    public synchronized void replaceChatPartners(Collection<ChatPartner> chatPartners) {
        chatPartnerList.clear();
        chatPartnerList.addAll(chatPartners);
    }

    public static class ChatViewHolder extends RecyclerView.ViewHolder {

        private TextView usernameTextView;

        public ChatViewHolder(View itemView) {
            super(itemView);
            usernameTextView = itemView.findViewById(R.id.chat_list_page_user_item_username);
        }

    }

}
