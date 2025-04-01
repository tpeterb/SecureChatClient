package com.tpeterb.securechatclient.utils;

import java.util.Base64;

public final class Base64Utils {

    private static final Base64.Encoder ENCODER;

    private static final Base64.Decoder DECODER;

    static {
        ENCODER = Base64.getEncoder();
        DECODER = Base64.getDecoder();
    }

    private Base64Utils() {}

    public static String encode(byte[] data) {
        return ENCODER.encodeToString(data);
    }

    public static byte[] decode(String encodedData) {
        return DECODER.decode(encodedData);
    }

}