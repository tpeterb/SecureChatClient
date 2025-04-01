package com.tpeterb.securechatclient.exception;

public class WebSocketConnectionAttemptFailureException extends RuntimeException {

    public WebSocketConnectionAttemptFailureException(String message) {
        super(message);
    }

}
