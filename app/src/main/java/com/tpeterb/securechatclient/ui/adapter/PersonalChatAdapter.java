package com.tpeterb.securechatclient.ui.adapter;

import android.content.Context;
import android.graphics.BitmapFactory;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.tpeterb.securechatclient.R;
import com.tpeterb.securechatclient.messages.model.MessageDTO;
import com.tpeterb.securechatclient.messages.service.CompressionService;
import com.tpeterb.securechatclient.users.session.UserSession;

import java.util.Base64;
import java.util.List;

import javax.inject.Inject;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class PersonalChatAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private final Context context;

    private final CompressionService compressionService;

    private final List<MessageDTO> messages;

    private final UserSession userSession;

    public PersonalChatAdapter(Context context, List<MessageDTO> messages, UserSession userSession) {
        this.context = context;
        compressionService = new CompressionService();
        this.messages = messages;
        this.userSession = userSession;
    }

    private static final int RECEIVED_TEXT_MESSAGE = 1;

    private static final int SENT_TEXT_MESSAGE = 2;

    private static final int RECEIVED_IMAGE = 3;

    private static final int SENT_IMAGE = 4;

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
        if (viewType == RECEIVED_TEXT_MESSAGE) {
            View view = layoutInflater.inflate(R.layout.personal_chat_received_text_message, parent, false);
            return new ReceivedTextMessageViewHolder(view);
        } else if (viewType == SENT_TEXT_MESSAGE) {
            View view = layoutInflater.inflate(R.layout.personal_chat_sent_text_message, parent, false);
            return new SentTextMessageViewHolder(view);
        } else if (viewType == RECEIVED_IMAGE) {
            View view = layoutInflater.inflate(R.layout.personal_chat_received_image, parent, false);
            return new ReceivedImageViewHolder(view);
        } else {
            View view = layoutInflater.inflate(R.layout.personal_chat_sent_image, parent, false);
            return new SentImageViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        MessageDTO message = messages.get(position);
        int messageType = getMessageType(position);
        if (messageType == RECEIVED_TEXT_MESSAGE) {
            ((ReceivedTextMessageViewHolder) holder).bindData(message.getContent());
        } else if (messageType == SENT_TEXT_MESSAGE) {
            ((SentTextMessageViewHolder) holder).bindData(message.getContent());
        } else if (messageType == RECEIVED_IMAGE) {
            ((ReceivedImageViewHolder) holder).bindData(message.getContent());
        } else {
            ((SentImageViewHolder) holder).bindData(message.getContent());
        }
    }

    @Override
    public int getItemCount() {
        return messages.size();
    }

    public class ReceivedTextMessageViewHolder extends RecyclerView.ViewHolder {

        private TextView receivedMessage;

        public ReceivedTextMessageViewHolder(View view) {
            super(view);
            receivedMessage = view.findViewById(R.id.personal_chat_received_text_message);
        }

        public void bindData(String message) {
            receivedMessage.setText(message);
        }

    }

    public class SentTextMessageViewHolder extends RecyclerView.ViewHolder {

        private TextView sentMessage;

        public SentTextMessageViewHolder(View view) {
            super(view);
            sentMessage = view.findViewById(R.id.personal_chat_sent_text_message);
        }

        public void bindData(String message) {
            sentMessage.setText(message);
        }

    }

    public class ReceivedImageViewHolder extends RecyclerView.ViewHolder {

        private ImageView imageView;

        public ReceivedImageViewHolder(View view) {
            super(view);
            imageView = view.findViewById(R.id.personal_chat_received_image);
        }

        public void bindData(String message) {
            byte[] decodedAndCompressedImage = Base64.getDecoder().decode(message);
            byte[] decompressedImage = compressionService.decompressBytesWithGzip(decodedAndCompressedImage);
            imageView.setImageBitmap(BitmapFactory.decodeByteArray(decompressedImage, 0, decompressedImage.length));
        }

    }

    public class SentImageViewHolder extends RecyclerView.ViewHolder {

        private ImageView imageView;

        public SentImageViewHolder(View view) {
            super(view);
            imageView = view.findViewById(R.id.personal_chat_sent_image);
        }

        public void bindData(String message) {
            byte[] decodedAndCompressedImage = Base64.getDecoder().decode(message);
            byte[] decompressedImage = compressionService.decompressBytesWithGzip(decodedAndCompressedImage);
            imageView.setImageBitmap(BitmapFactory.decodeByteArray(decompressedImage, 0, decompressedImage.length));
        }

    }

    private int getMessageType(int position) {
        String loggedInUsername = userSession.getUsername();
        MessageDTO messageDTO = messages.get(position);
        if (messageDTO.getReceiver().equals(loggedInUsername)) {
            if (messageDTO.getMessageContentType().isImage()) {
                return RECEIVED_IMAGE;
            }
            return RECEIVED_TEXT_MESSAGE;
        } else {
            if (messageDTO.getMessageContentType().isImage()) {
                return SENT_IMAGE;
            }
            return SENT_TEXT_MESSAGE;
        }
    }

}
