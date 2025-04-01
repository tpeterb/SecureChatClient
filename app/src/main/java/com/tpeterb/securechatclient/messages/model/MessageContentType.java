package com.tpeterb.securechatclient.messages.model;

import java.util.Optional;

public enum MessageContentType {

    JPEG("image/jpeg"),
    PNG("image/png"),
    TEXT("text/plain");

    private final String mimeType;

    MessageContentType(String mimeType) {
        this.mimeType = mimeType;
    }

    public static Optional<MessageContentType> of(String mimeType) {
        switch (mimeType) {
            case "text/plain":
                return Optional.of(TEXT);
            case "image/jpeg":
                return Optional.of(JPEG);
            case "image/png":
                return Optional.of(PNG);
            default:
                return Optional.empty();
        }
    }

    public boolean isImage() {
        return this == JPEG ||
               this == PNG;
    }

}
