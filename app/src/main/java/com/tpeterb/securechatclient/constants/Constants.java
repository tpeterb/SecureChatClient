package com.tpeterb.securechatclient.constants;

public final class Constants {

    private Constants() {}

    public static final int MIN_USERNAME_LENGTH = 3;

    public static final int MAX_USERNAME_LENGTH = 20;

    public static final int MIN_PASSWORD_LENGTH = 8;

    public static final String CHAT_SERVER_BASE_URL = "http://192.168.100.8:5555/";

    public static final String CHAT_SERVER_WEBSOCKET_BASE_URL = "ws://192.168.100.8:5555/ws";

    public static final String CHAT_SERVER_CONVERSATION_PART_FETCH_REQUEST_DESTINATION = "/app/chat/fetch";

    public static final String CHAT_SERVER_MESSAGE_RECEIPT_ACKNOWLEDGEMENT_DESTINATION = "/app/chat/ack";

    public static final int NUMBER_OF_MESSAGES_TO_FETCH_AT_ONCE = 20;

    public static final String CHAT_SERVER_MESSAGE_SINGLE_SENDING_DESTINATION = "/app/chat/private";

    public static final String SINGLE_MESSAGE_RECEIVING_GENERAL_DESTINATION = "/queue/private/";

    public static final String MESSAGE_BULK_FETCHING_GENERAL_DESTINATION = "/queue/conversationPart/";

}
