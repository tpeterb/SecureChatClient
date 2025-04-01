package com.tpeterb.securechatclient.messages.service;

import static com.tpeterb.securechatclient.constants.Constants.MESSAGE_BULK_FETCHING_GENERAL_DESTINATION;
import static com.tpeterb.securechatclient.constants.Constants.NEW_KEY_EXCHANGE_SIGNALING_GENERAL_ENDPOINT;
import static com.tpeterb.securechatclient.constants.Constants.SERVER_PUBLIC_KEY_FOR_MESSAGES_DESTINATION_GENERAL_DESTINATION;
import static com.tpeterb.securechatclient.constants.Constants.SINGLE_MESSAGE_CHUNK_RECEIVING_GENERAL_DESTINATION;
import static com.tpeterb.securechatclient.constants.Constants.SINGLE_MESSAGE_RECEIVING_GENERAL_DESTINATION;
import static com.tpeterb.securechatclient.security.model.KeyExchangeResult.SUCCESS;

import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.ProcessLifecycleOwner;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tpeterb.securechatclient.messages.listener.PersonalChatMessageBulkUpdateListener;
import com.tpeterb.securechatclient.messages.listener.PersonalChatMessageSingleUpdateListener;
import com.tpeterb.securechatclient.messages.model.ConversationPartFetchResponseDTO;
import com.tpeterb.securechatclient.messages.model.MessageDTO;
import com.tpeterb.securechatclient.messages.registry.ChatPartnerRegistry;
import com.tpeterb.securechatclient.messages.registry.MessageChunkRegistry;
import com.tpeterb.securechatclient.messages.registry.MessageRegistry;
import com.tpeterb.securechatclient.messages.registry.StompSubscriptionRegistry;
import com.tpeterb.securechatclient.security.cache.DigitalSignatureKeyPairCache;
import com.tpeterb.securechatclient.security.cache.ServerPublicKeyCache;
import com.tpeterb.securechatclient.security.config.SecurityConfig;
import com.tpeterb.securechatclient.security.model.EncryptedPacket;
import com.tpeterb.securechatclient.security.model.EncryptedReceivedMessageChunkDTO;
import com.tpeterb.securechatclient.security.model.EncryptedReceivedMessageDTO;
import com.tpeterb.securechatclient.security.model.KeyExchangeResult;
import com.tpeterb.securechatclient.security.model.ServerMessagePublicKeyResponseDTO;
import com.tpeterb.securechatclient.security.service.AsymmetricCipherService;
import com.tpeterb.securechatclient.security.service.EllipticCurveDiffieHellmanKeyExchangeService;
import com.tpeterb.securechatclient.security.service.PacketEncryptionService;
import com.tpeterb.securechatclient.ui.MainActivity;
import com.tpeterb.securechatclient.users.model.ChatPartner;
import com.tpeterb.securechatclient.users.session.UserSession;

import org.bouncycastle.crypto.params.AsymmetricKeyParameter;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

import javax.inject.Inject;
import javax.inject.Singleton;

import io.reactivex.disposables.Disposable;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import ua.naiksoftware.stomp.StompClient;

@Singleton
@Slf4j
public class StompSubscriptionService {

    private final UserSession userSession;

    private final PacketEncryptionService packetEncryptionService;

    private final AsymmetricCipherService asymmetricCipherService;

    private final StompSubscriptionRegistry stompSubscriptionRegistry;

    private final SecurityConfig securityConfig;

    private final ChatPartnerRegistry chatPartnerRegistry;

    private final DigitalSignatureKeyPairCache digitalSignatureKeyPairCache;

    private final MessageRegistry messageRegistry;

    private final MessageChunkRegistry messageChunkRegistry;

    private final MessageService messageService;

    private final ObjectMapper objectMapper;

    private final StompClient stompClient;

    private final ServerPublicKeyCache serverPublicKeyCache;

    private final EllipticCurveDiffieHellmanKeyExchangeService ellipticCurveDiffieHellmanKeyExchangeService;

    private final Context context;

    @Setter
    private PersonalChatMessageBulkUpdateListener personalChatMessageBulkUpdateListener;

    @Setter
    private PersonalChatMessageSingleUpdateListener personalChatMessageSingleUpdateListener;

    @Setter
    private Handler handler;

    private static final String EXISTING_SUBSCRIPTION_LOG_MESSAGE = "Subscription already exists!";

    @Inject
    public StompSubscriptionService (UserSession userSession,
                                     PacketEncryptionService packetEncryptionService,
                                     AsymmetricCipherService asymmetricCipherService,
                                     StompSubscriptionRegistry stompSubscriptionRegistry,
                                     SecurityConfig securityConfig,
                                     ChatPartnerRegistry chatPartnerRegistry,
                                     DigitalSignatureKeyPairCache digitalSignatureKeyPairCache,
                                     MessageRegistry messageRegistry,
                                     MessageChunkRegistry messageChunkRegistry,
                                     MessageService messageService,
                                     ObjectMapper objectMapper,
                                     StompClient stompClient,
                                     ServerPublicKeyCache serverPublicKeyCache,
                                     EllipticCurveDiffieHellmanKeyExchangeService ellipticCurveDiffieHellmanKeyExchangeService,
                                     Context context) {
        this.userSession = userSession;
        this.packetEncryptionService = packetEncryptionService;
        this.asymmetricCipherService = asymmetricCipherService;
        this.stompSubscriptionRegistry = stompSubscriptionRegistry;
        this.securityConfig = securityConfig;
        this.chatPartnerRegistry = chatPartnerRegistry;
        this.digitalSignatureKeyPairCache = digitalSignatureKeyPairCache;
        this.messageRegistry = messageRegistry;
        this.messageChunkRegistry = messageChunkRegistry;
        this.messageService = messageService;
        this.objectMapper = objectMapper;
        this.stompClient = stompClient;
        this.serverPublicKeyCache = serverPublicKeyCache;
        this.ellipticCurveDiffieHellmanKeyExchangeService = ellipticCurveDiffieHellmanKeyExchangeService;
        this.context = context;
        handler = new Handler(Looper.getMainLooper());
    }

    public void subscribeToNewKeyExchangeSignalingDestination() {
        String subscriptionDestination = NEW_KEY_EXCHANGE_SIGNALING_GENERAL_ENDPOINT + userSession.getSessionId();
        if (!stompSubscriptionRegistry.isSubscriptionRegistered(subscriptionDestination)) {
            Disposable disposable = stompClient.topic(subscriptionDestination).subscribe(stompMessage -> {
                log.info("Received signal from server that the session key expired");
                stompSubscriptionRegistry.removeSubscription(subscriptionDestination);
                handler.post(() -> {
                    initiateKeyExchangeWithServerWithRetries(0, this::subscribeToNewKeyExchangeSignalingDestination, () -> {
                        userSession.clearUserSession();
                        digitalSignatureKeyPairCache.clearDigitalSignatureKeyPairCache();
                        Intent intent = new Intent(context, MainActivity.class);
                        context.startActivity(intent);
                    });
                });
            }, throwable -> {
                log.error("There was an error while trying to subscribe to new key exchange signaling destination, reason: {}", throwable.getMessage());
            });
            stompSubscriptionRegistry.addSubscription(subscriptionDestination, disposable);
        } else {
            log.info(EXISTING_SUBSCRIPTION_LOG_MESSAGE);
        }
    }

    public void subscribeToServerMessagePublicKeyDestination() {
        String subscriptionDestination = SERVER_PUBLIC_KEY_FOR_MESSAGES_DESTINATION_GENERAL_DESTINATION + userSession.getUsername();
        if (!stompSubscriptionRegistry.isSubscriptionRegistered(subscriptionDestination)) {
            Disposable disposable = stompClient.topic(subscriptionDestination).subscribe(stompMessage -> {
                log.info("Received server message public key!!");
                String serializedEncryptedPacket = stompMessage.getPayload();
                EncryptedPacket encryptedPacket = objectMapper.readValue(serializedEncryptedPacket, EncryptedPacket.class);
                byte[] decryptedPacket = packetEncryptionService.decryptEntirePacket(encryptedPacket);
                ServerMessagePublicKeyResponseDTO serverMessagePublicKeyResponseDTO = objectMapper.readValue(decryptedPacket, ServerMessagePublicKeyResponseDTO.class);
                AsymmetricKeyParameter publicKey = asymmetricCipherService.convertFromBytesToPublicKey(serverMessagePublicKeyResponseDTO.getServerPublicKey());
                serverPublicKeyCache.setServerPublicKeyForChatMessages(publicKey);
                messageService.sendAcknowledgementForFullMessage(serverMessagePublicKeyResponseDTO.getResponseId());
            }, throwable -> {
                log.error("There was an error while trying to subscribe to server message public key receiving destination, reason: {}", throwable.getMessage());
            });
            stompSubscriptionRegistry.addSubscription(subscriptionDestination, disposable);
        } else {
            log.info(EXISTING_SUBSCRIPTION_LOG_MESSAGE);
        }
    }

    public void subscribeToConversationPartFetchingDestination(PersonalChatMessageBulkUpdateListener personalChatMessageBulkUpdateListener) {
        String subscriptionDestination = MESSAGE_BULK_FETCHING_GENERAL_DESTINATION + userSession.getUsername();
        this.personalChatMessageBulkUpdateListener = personalChatMessageBulkUpdateListener;
        if (!stompSubscriptionRegistry.isSubscriptionRegistered(subscriptionDestination)) {
            Disposable disposable = stompClient.topic(subscriptionDestination)
                    .subscribe(stompMessage -> {
                        log.info("subscribeToConversationPartFetchingDestination callback");
                        String serializedEncryptedPacket = stompMessage.getPayload();
                        EncryptedPacket encryptedPacket = objectMapper.readValue(serializedEncryptedPacket, EncryptedPacket.class);
                        log.info("Session id AFTER RECEIVAL: {}", encryptedPacket.getSessionId());
                        log.info("Encrypted data AFTER RECEIVAL: {}", encryptedPacket.getEncryptionResult().getEncryptedData());
                        log.info("NONCE AFTER RECEIVAL: {}", encryptedPacket.getEncryptionResult().getInitializationVector());
                        byte[] decryptedPacket = packetEncryptionService.decryptEntirePacket(encryptedPacket);
                        ConversationPartFetchResponseDTO conversationPartFetchResponseDTO = objectMapper.readValue(decryptedPacket, new TypeReference<ConversationPartFetchResponseDTO>() {});
                        List<EncryptedReceivedMessageDTO> encryptedMessages = conversationPartFetchResponseDTO.getMessages();
                        List<MessageDTO> decryptedMessages = packetEncryptionService.decryptFullChatMessages(encryptedMessages);
                        messageRegistry.addAllMessagesToRegistry(conversationPartFetchResponseDTO.getChatPartnerUsername(), decryptedMessages);
                        Collections.sort(messageRegistry.getMessagesForChatPartner(conversationPartFetchResponseDTO.getChatPartnerUsername()));
                        log.info("Received conversation part, messages = {}", decryptedMessages);
                        if (Objects.nonNull(handler)) {
                            handler.post(() -> {
                                this.personalChatMessageBulkUpdateListener.onNewMessages(decryptedMessages);
                            });
                        }
                        messageService.sendAcknowledgementForFullMessage(conversationPartFetchResponseDTO.getConversationPartId());
                    }, throwable -> {
                        log.error("There was an error while trying to subscribe to conversation fetching destination, reason: {}", throwable.getMessage());
                    });
            stompSubscriptionRegistry.addSubscription(subscriptionDestination, disposable);
        } else {
            log.info(EXISTING_SUBSCRIPTION_LOG_MESSAGE);
        }
    }

    public void subscribeToSingleMessageReceivingDestination() {

        String loggedInUsername = userSession.getUsername();
        String subscriptionDestination = SINGLE_MESSAGE_RECEIVING_GENERAL_DESTINATION + loggedInUsername;
        if (!stompSubscriptionRegistry.isSubscriptionRegistered(subscriptionDestination)) {
            Disposable disposable = stompClient.topic(subscriptionDestination).subscribe(stompMessage -> {
                String serializedEncryptedPacket = stompMessage.getPayload();
                EncryptedPacket encryptedPacket = objectMapper.readValue(serializedEncryptedPacket, EncryptedPacket.class);
                byte[] decryptedPacket = packetEncryptionService.decryptEntirePacket(encryptedPacket);
                EncryptedReceivedMessageDTO encryptedReceivedMessageDTO = objectMapper.readValue(decryptedPacket, EncryptedReceivedMessageDTO.class);
                MessageDTO message = packetEncryptionService.decryptFullChatMessage(encryptedReceivedMessageDTO);
                String messageSenderUsername = message.getSender();
                chatPartnerRegistry.addChatPartnerToRegistry(new ChatPartner(messageSenderUsername));
                messageRegistry.addMessageToRegistry(messageSenderUsername, message);
                Collections.sort(messageRegistry.getMessagesForChatPartner(messageSenderUsername));
                log.info("Received message, message = {}", message);
                if (Objects.nonNull(handler)) {
                    handler.post(() -> {
                        personalChatMessageSingleUpdateListener.onNewMessage(message);
                    });
                }
                messageService.sendAcknowledgementForFullMessage(message.getId());
            }, throwable -> {
                log.error("There was an error while trying to subscribe to message receiving destination, reason: {}", throwable.getMessage());
            });
            stompSubscriptionRegistry.addSubscription(subscriptionDestination, disposable);
        } else {
            log.info(EXISTING_SUBSCRIPTION_LOG_MESSAGE);
        }
    }

    public void subscribeToSingleMessageChunkReceivingDestination() {

        String loggedInUsername = userSession.getUsername();
        String subscriptionDestination = SINGLE_MESSAGE_CHUNK_RECEIVING_GENERAL_DESTINATION + loggedInUsername;
        if (!stompSubscriptionRegistry.isSubscriptionRegistered(subscriptionDestination)) {
            Disposable disposable = stompClient.topic(subscriptionDestination).subscribe(stompMessage -> {
                String serializedEncryptedPacket = stompMessage.getPayload();
                EncryptedPacket encryptedPacket = objectMapper.readValue(serializedEncryptedPacket, EncryptedPacket.class);
                byte[] decryptedPacket = packetEncryptionService.decryptEntirePacket(encryptedPacket);
                EncryptedReceivedMessageChunkDTO messageChunk = objectMapper.readValue(decryptedPacket, EncryptedReceivedMessageChunkDTO.class);
                messageChunkRegistry.addMessageChunk(messageChunk);
                log.info("Received message chunk for message with id {}, message chunk = {}", messageChunk.getFullMessageId(), messageChunk);
            }, throwable -> {
                log.error("There was an error while trying to subscribe to message chunk receiving destination, reason: {}", throwable.getMessage());
            });
            stompSubscriptionRegistry.addSubscription(subscriptionDestination, disposable);
        } else {
            log.info(EXISTING_SUBSCRIPTION_LOG_MESSAGE);
        }
    }

    private void initiateKeyExchangeWithServerWithRetries(int retryCount, Runnable successfulKeyExchangeTask, Runnable failedRetryAttemptsTask) {
        if (retryCount > securityConfig.getKeyExchangeRetryAttempts()) {
            failedRetryAttemptsTask.run();
            return;
        }
        int nextRetryCount = retryCount + 1;
        LiveData<KeyExchangeResult> keyExchangeLiveData = ellipticCurveDiffieHellmanKeyExchangeService.initiateKeyExchangeWithServerAfterLogin(ProcessLifecycleOwner.get());
        keyExchangeLiveData.observe(ProcessLifecycleOwner.get(), keyExchangeResult -> {
            if (keyExchangeResult == SUCCESS) {
                successfulKeyExchangeTask.run();
            } else {
                initiateKeyExchangeWithServerWithRetries(nextRetryCount, successfulKeyExchangeTask, failedRetryAttemptsTask);
            }
        });
    }

}
