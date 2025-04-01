package com.tpeterb.securechatclient.users.service;

import static com.tpeterb.securechatclient.constants.Constants.MAX_USERNAME_LENGTH;
import static com.tpeterb.securechatclient.constants.Constants.MIN_PASSWORD_LENGTH;
import static com.tpeterb.securechatclient.constants.Constants.MIN_USERNAME_LENGTH;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tpeterb.securechatclient.security.model.EncryptedPacket;
import com.tpeterb.securechatclient.security.service.PacketEncryptionService;
import com.tpeterb.securechatclient.users.model.ChatPartner;
import com.tpeterb.securechatclient.users.model.LoginUserDTO;
import com.tpeterb.securechatclient.users.model.RegisterUserDTO;
import com.tpeterb.securechatclient.users.model.UserLoginResult;
import com.tpeterb.securechatclient.users.model.UserRegistrationResult;
import com.tpeterb.securechatclient.users.session.UserSession;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import javax.inject.Inject;
import javax.inject.Singleton;

import lombok.extern.slf4j.Slf4j;

@Singleton
@Slf4j
public class UserService {

    private final ChatServerApiServiceProvider chatServerApiServiceProvider;

    private final PacketEncryptionService packetEncryptionService;

    private final ObjectMapper objectMapper;

    private final UserSession userSession;

    @Inject
    public UserService(ChatServerApiServiceProvider chatServerApiServiceProvider,
                       PacketEncryptionService packetEncryptionService,
                       ObjectMapper objectMapper,
                       UserSession userSession) {
        this.chatServerApiServiceProvider = chatServerApiServiceProvider;
        this.packetEncryptionService = packetEncryptionService;
        this.objectMapper = objectMapper;
        this.userSession = userSession;
    }

    public LiveData<Optional<List<String>>> getUsernamesForSearchedUsername(String searchedUsername) {
        byte[] serializedSearchedUsernameBytes;
        try {
            serializedSearchedUsernameBytes = objectMapper.writeValueAsBytes(searchedUsername);
        } catch (JsonProcessingException e) {
            log.error("Could not serialize searched username, reason: {}", e.getMessage());
            MutableLiveData<Optional<List<String>>> liveData = new MutableLiveData<>();
            liveData.postValue(Optional.of(Collections.emptyList()));
            return liveData;
        }
        EncryptedPacket encryptedPacket = packetEncryptionService.encryptEntirePacket(serializedSearchedUsernameBytes);
        return chatServerApiServiceProvider.getUsernamesForSearchedUsername(encryptedPacket);
    }

    public LiveData<Optional<List<ChatPartner>>> getChatPartnersForUsername(String username) {
        byte[] serializedUsernameBytes;
        try {
            serializedUsernameBytes = objectMapper.writeValueAsBytes(username);
        } catch (JsonProcessingException e) {
            log.error("Could not serialize username for chat partner searching, reason: {}", e.getMessage());
            MutableLiveData<Optional<List<ChatPartner>>> liveData = new MutableLiveData<>();
            liveData.postValue(Optional.of(Collections.emptyList()));
            return liveData;
        }
        EncryptedPacket encryptedPacket = packetEncryptionService.encryptEntirePacket(serializedUsernameBytes);
        return chatServerApiServiceProvider.getChatPartnersForUsername(encryptedPacket);
    }

    public CompletableFuture<UserLoginResult> loginUser(LoginUserDTO loginUserDTO) {
        byte[] userLoginDataBytes;
        try {
            userLoginDataBytes = objectMapper.writeValueAsBytes(loginUserDTO);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize user login data to bytes! Reason: {}", e.getMessage());
            return CompletableFuture.completedFuture(UserLoginResult.FAILURE);
        }
        log.info("USER SESSION SESSION ID IN loginUser BEFORE ENCRYPTION: {}", userSession.getSessionId());
        EncryptedPacket encryptedPacket = packetEncryptionService.encryptEntirePacket(userLoginDataBytes);
        log.info("ENCRYPTED PACKET SESSION ID = {}", encryptedPacket.getSessionId());
        return chatServerApiServiceProvider.loginUser(encryptedPacket);
    }

    public CompletableFuture<UserRegistrationResult> registerUser(RegisterUserDTO registerUserDTO) {
        byte[] userRegistrationDataBytes;
        try {
            userRegistrationDataBytes = objectMapper.writeValueAsBytes(registerUserDTO);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize user registration data to bytes! Reason: {}", e.getMessage());
            return CompletableFuture.completedFuture(UserRegistrationResult.GENERAL_FAILURE);
        }
        EncryptedPacket encryptedPacket = packetEncryptionService.encryptEntirePacket(userRegistrationDataBytes);
        return chatServerApiServiceProvider.registerUser(encryptedPacket);
    }

    public Optional<UserLoginResult> validateUserLoginData(LoginUserDTO loginUserDTO) {
        if (!isUsernameValid(loginUserDTO.getUsername())) {
            return Optional.of(UserLoginResult.BAD_USERNAME_FORMAT);
        }
        if (!isPasswordValid(loginUserDTO.getPassword())) {
            return Optional.of(UserLoginResult.BAD_PASSWORD_FORMAT);
        }
        return Optional.empty();
    }

    public Optional<UserRegistrationResult> validateUserRegistrationData(RegisterUserDTO registerUserDTO) {
        if (!isUsernameValid(registerUserDTO.getUsername())) {
            return Optional.of(UserRegistrationResult.BAD_USERNAME_FORMAT);
        }
        if (!isPasswordValid(registerUserDTO.getPassword())) {
            return Optional.of(UserRegistrationResult.BAD_PASSWORD_FORMAT);
        }
        if (!isEmailValid(registerUserDTO.getEmail())) {
            return Optional.of(UserRegistrationResult.BAD_EMAIL_FORMAT);
        }
        return Optional.empty();
    }

    private boolean isUsernameValid(String username) {
        final String usernameRegex = "^[a-zA-Z0-9._]+$";
        return Objects.nonNull(username) &&
                username.length() >= MIN_USERNAME_LENGTH &&
                username.length() <= MAX_USERNAME_LENGTH &&
                username.matches(usernameRegex) &&
                !username.contains("..");
    }

    private boolean isPasswordValid(String password) {
        return Objects.nonNull(password) &&
                password.length() >= MIN_PASSWORD_LENGTH &&
                password.matches("^.*[A-Z].*$") &&
                password.matches("^.*[a-z].*$") &&
                password.matches("^.*[0-9].*$");
    }

    private boolean isEmailValid(String email) {
        final String emailRegex = "^[\\w-\\.]+@([\\w-]+\\.)+[\\w-]{2,4}$";
        return Objects.nonNull(email) &&
                email.matches(emailRegex);
    }

}
