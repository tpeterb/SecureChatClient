package com.tpeterb.securechatclient.users.service;

import static com.tpeterb.securechatclient.constants.Constants.MAX_USERNAME_LENGTH;
import static com.tpeterb.securechatclient.constants.Constants.MIN_PASSWORD_LENGTH;
import static com.tpeterb.securechatclient.constants.Constants.MIN_USERNAME_LENGTH;

import androidx.lifecycle.LiveData;

import com.tpeterb.securechatclient.users.model.ChatPartner;
import com.tpeterb.securechatclient.users.model.LoginUserDTO;
import com.tpeterb.securechatclient.users.model.RegisterUserDTO;
import com.tpeterb.securechatclient.users.model.UserLoginResult;
import com.tpeterb.securechatclient.users.model.UserRegistrationResult;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class UserService {

    private final ChatServerApiServiceProvider chatServerApiServiceProvider;

    @Inject
    public UserService(ChatServerApiServiceProvider chatServerApiServiceProvider) {
        this.chatServerApiServiceProvider = chatServerApiServiceProvider;
    }

    public LiveData<List<String>> getUsernamesForSearchedUsername(String searchedUsername) {
        return chatServerApiServiceProvider.getUsernamesForSearchedUsername(searchedUsername);
    }

    public LiveData<List<ChatPartner>> getChatPartnersForUsername(String username) {
        return chatServerApiServiceProvider.getChatPartnersForUsername(username);
    }

    public CompletableFuture<UserLoginResult> loginUser(LoginUserDTO loginUserDTO) {
        return chatServerApiServiceProvider.loginUser(loginUserDTO);
    }

    public CompletableFuture<UserRegistrationResult> registerUser(RegisterUserDTO registerUserDTO) {
        return chatServerApiServiceProvider.registerUser(registerUserDTO);
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
