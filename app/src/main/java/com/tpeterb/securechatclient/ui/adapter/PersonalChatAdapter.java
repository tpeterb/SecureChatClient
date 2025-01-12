package com.tpeterb.securechatclient.ui.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.tpeterb.securechatclient.R;
import com.tpeterb.securechatclient.messages.model.MessageDTO;
import com.tpeterb.securechatclient.users.session.UserSession;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@AllArgsConstructor
@Slf4j
public class PersonalChatAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private Context context;

    private List<MessageDTO> messages;

    private UserSession userSession;

    private static final int RECEIVED_MESSAGE = 1;

    private static final int SENT_MESSAGE = 2;

    public void replaceChatMessages(List<MessageDTO> messages) {
        log.info("replaceChatMessages");
        log.info("Messages to display = {}", messages);
        this.messages.clear();
        this.messages.addAll(messages);
    }

    @Override
    public int getItemViewType(int position) {
        return getMessageType(position);
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater layoutInflater = LayoutInflater.from(context);
        if (viewType == RECEIVED_MESSAGE) {
            View view = layoutInflater.inflate(R.layout.personal_chat_received_message, parent, false);
            return new ReceivedMessageViewHolder(view);
        } else {
            View view = layoutInflater.inflate(R.layout.personal_chat_sent_message, parent, false);
            return new SentMessageViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        MessageDTO message = messages.get(position);
        int messageType = getMessageType(position);
        if (messageType == RECEIVED_MESSAGE) {
            ((ReceivedMessageViewHolder) holder).bindData(message.getContent());
        } else {
            ((SentMessageViewHolder) holder).bindData(message.getContent());
        }
    }

    @Override
    public int getItemCount() {
        return messages.size();
    }

    public static class ReceivedMessageViewHolder extends RecyclerView.ViewHolder {

        private TextView receivedMessage;

        public ReceivedMessageViewHolder(View view) {
            super(view);
            receivedMessage = view.findViewById(R.id.personal_chat_received_message);
        }

        public void bindData(String message) {
            receivedMessage.setText(message);
        }

    }

    public static class SentMessageViewHolder extends RecyclerView.ViewHolder {

        private TextView sentMessage;

        public SentMessageViewHolder(View view) {
            super(view);
            sentMessage = view.findViewById(R.id.personal_chat_sent_message);
        }

        public void bindData(String message) {
            sentMessage.setText(message);
        }

    }

    private int getMessageType(int position) {
        String loggedInUsername = userSession.getUsername();
        if (messages.get(position).getReceiver().equals(loggedInUsername)) {
            return RECEIVED_MESSAGE;
        }
        return SENT_MESSAGE;
    }

}
