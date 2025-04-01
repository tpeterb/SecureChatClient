package com.tpeterb.securechatclient.exception;

public class AsymmetricKeyConversionException extends RuntimeException {

    private static final String TO_BYTES_CONVERSION_MESSAGE = "There was an error while trying to convert asymmetric key to bytes, reason: %s";

    private static final String FROM_BYTES_CONVERSION_MESSAGE = "There was an error while trying to convert asymmetric key from bytes, reason: %s";

    private static final String TO_STRING_FOR_STORAGE_CONVERSION_MESSAGE = "There was an error while trying to convert asymmetric key to string for storage, reason: %s";

    private static final String FROM_STRING_CONVERSION_MESSAGE = "There was an error while trying to convert asymmetric key from string, reason: %s";

    private AsymmetricKeyConversionException (String message) {
        super(message);
    }

    public static AsymmetricKeyConversionException toBytesConversionError(String causeMessage) {
        return new AsymmetricKeyConversionException(String.format(TO_BYTES_CONVERSION_MESSAGE, causeMessage));
    }

    public static AsymmetricKeyConversionException fromBytesConversionError(String causeMessage) {
        return new AsymmetricKeyConversionException(String.format(FROM_BYTES_CONVERSION_MESSAGE, causeMessage));
    }

    public static AsymmetricKeyConversionException toStringForStorageConversionError(String causeMessage) {
        return new AsymmetricKeyConversionException(String.format(TO_STRING_FOR_STORAGE_CONVERSION_MESSAGE, causeMessage));
    }

    public static AsymmetricKeyConversionException fromStringConversionError(String causeMessage) {
        return new AsymmetricKeyConversionException(String.format(FROM_STRING_CONVERSION_MESSAGE, causeMessage));
    }

}
