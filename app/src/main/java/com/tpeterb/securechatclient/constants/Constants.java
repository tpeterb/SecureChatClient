package com.tpeterb.securechatclient.constants;

public final class Constants {

    private Constants() {}

    public static final int MIN_USERNAME_LENGTH = 3;

    public static final int MAX_USERNAME_LENGTH = 20;

    public static final int MIN_PASSWORD_LENGTH = 8;

    public static final String CHAT_SERVER_BASE_URL = "http://192.168.100.8:5555/";

    public static final String CHAT_SERVER_WEBSOCKET_BASE_URL = "ws://192.168.100.8:5555/ws";

    public static final String CHAT_SERVER_CONVERSATION_PART_FETCH_REQUEST_DESTINATION = "/app/chat/fetch";

    public static final String CHAT_SERVER_FULL_MESSAGE_RECEIPT_ACKNOWLEDGEMENT_DESTINATION = "/app/chat/ack/full";

    public static final int NUMBER_OF_MESSAGES_TO_FETCH_AT_ONCE = 20;

    public static final String CHAT_SERVER_MESSAGE_SINGLE_SENDING_DESTINATION = "/app/chat/private";

    public static final String CHAT_SERVER_MESSAGE_SINGLE_CHUNK_SENDING_DESTINATION = "/app/chat/private/chunk";

    public static final String SINGLE_MESSAGE_RECEIVING_GENERAL_DESTINATION = "/queue/private/";

    public static final String SINGLE_MESSAGE_CHUNK_RECEIVING_GENERAL_DESTINATION = "/queue/private/chunk/";

    public static final String MESSAGE_BULK_FETCHING_GENERAL_DESTINATION = "/queue/conversationPart/";

    public static final String SERVER_PUBLIC_KEY_FOR_MESSAGES_DESTINATION_GENERAL_DESTINATION = "/queue/key/public/messages/";

    public static final String SERVER_PUBLIC_KEY_FOR_MESSAGES_REQUEST_DESTINATION = "/app/key/public/message";

    public static final String NEW_KEY_EXCHANGE_SIGNALING_GENERAL_ENDPOINT = "/queue/key/exchange/";

    public static final int MESSAGE_SLICING_SIZE_THRESHOLD_IN_BYTES = Integer.MAX_VALUE;

    public static final int HTTP_STATUS_CODE_FOR_NON_EXISTENT_SESSION_KEY = 403;

    public static final int HTTP_STATUS_CODE_FOR_EXPIRED_SESSION_KEY = 401;

    public static final int HTTP_STATUS_CODE_FOR_FAILED_KEY_EXCHANGE = 401;

    public static final String SHARED_PREFERENCE_NAME = "secure_chat_preference";

}
