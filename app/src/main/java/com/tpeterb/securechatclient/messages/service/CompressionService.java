package com.tpeterb.securechatclient.messages.service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Objects;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import javax.inject.Inject;
import javax.inject.Singleton;

import lombok.extern.slf4j.Slf4j;

@Singleton
@Slf4j
public class CompressionService {

    @Inject
    public CompressionService() {}

    public byte[] compressBytesWithGzip(byte[] byteArray) {
        if (Objects.isNull(byteArray) || byteArray.length == 0) {
            return new byte[] {};
        }
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        try (GZIPOutputStream gzipOutputStream = new GZIPOutputStream(byteArrayOutputStream)) {
            gzipOutputStream.write(byteArray);
        } catch (IOException e) {
            log.error("There was an error while compressing byte array with gzip, reason: {}", e.getMessage());
            return new byte[] {};
        }
        return byteArrayOutputStream.toByteArray();
    }

    public byte[] decompressBytesWithGzip(byte[] compressedByteArray) {
        if (Objects.isNull(compressedByteArray) || compressedByteArray.length == 0) {
            return new byte[] {};
        }
        try (ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(compressedByteArray);
             GZIPInputStream gzipInputStream = new GZIPInputStream(byteArrayInputStream);
             ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = gzipInputStream.read(buffer)) != -1) {
                byteArrayOutputStream.write(buffer, 0, bytesRead);
            }
            return byteArrayOutputStream.toByteArray();
        } catch (IOException e) {
            log.error("There was an error while decompressing byte array with gzip, reason: {}", e.getMessage());
            return new byte[] {};
        }
    }

}
