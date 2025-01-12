package com.tpeterb.securechatclient.ui;

import static com.tpeterb.securechatclient.ui.form.UserFormService.LOGIN_FAILURE_ERROR_MESSAGE;
import static com.tpeterb.securechatclient.ui.form.UserFormService.PASSWORD_FORMAT_ERROR_MESSAGE;
import static com.tpeterb.securechatclient.ui.form.UserFormService.SUCCESSFUL_REGISTRATION_MESSAGE;
import static com.tpeterb.securechatclient.ui.form.UserFormService.USERNAME_FORMAT_ERROR_MESSAGE;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;


import com.google.android.material.textfield.TextInputLayout;
import com.tpeterb.securechatclient.R;
import com.tpeterb.securechatclient.application.ChatApplication;
import com.tpeterb.securechatclient.messages.registry.ChatPartnerRegistry;
import com.tpeterb.securechatclient.messages.registry.MessageRegistry;
import com.tpeterb.securechatclient.messages.registry.StompSubscriptionRegistry;
import com.tpeterb.securechatclient.ui.form.UserFormService;
import com.tpeterb.securechatclient.users.model.LoginUserDTO;
import com.tpeterb.securechatclient.users.model.UserLoginResult;
import com.tpeterb.securechatclient.users.service.UserService;
import com.tpeterb.securechatclient.users.session.UserSession;

import java.util.Objects;
import java.util.Optional;

import javax.inject.Inject;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class LoginActivity extends AppCompatActivity {

    private boolean isLoadedForTheFirstTime = true;

    @Inject
    UserService userService;

    @Inject
    UserFormService userFormService;

    @Inject
    UserSession userSession;

    @Inject
    MessageRegistry messageRegistry;

    @Inject
    StompSubscriptionRegistry stompSubscriptionRegistry;

    @Inject
    ChatPartnerRegistry chatPartnerRegistry;

    private TextView successfulRegistrationText;

    private EditText usernameTextBox;

    private EditText passwordTextBox;

    private Button loginButton;

    private TextInputLayout usernameErrorTextbox;

    private TextInputLayout passwordErrorTextbox;

    @Override
    protected void onResume() {

        super.onResume();

        if (!isLoadedForTheFirstTime) {
            clearInputBoxes();

            clearRegistries();

            userSession.clearUserSession();
        }

        isLoadedForTheFirstTime = false;

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        ChatApplication.getAppComponent().inject(this);
        setContentView(R.layout.activity_login);

        initializeUIElements();

        Intent intent = getIntent();
        if (Objects.nonNull(intent) && intent.getBooleanExtra("showSuccessfulRegistrationMessage", false)) {
            userFormService.showMessage(successfulRegistrationText, SUCCESSFUL_REGISTRATION_MESSAGE);
        }

        clearInputBoxes();

        clearRegistries();

        userSession.clearUserSession();

        setupLoginButtonClickHandling();

    }

    private void initializeUIElements() {

        successfulRegistrationText = findViewById(R.id.login_successful_registration_text);
        usernameTextBox = findViewById(R.id.login_username_textbox);
        passwordTextBox = findViewById(R.id.login_password_textbox);
        loginButton = findViewById(R.id.login_page_login_button);
        usernameErrorTextbox = findViewById(R.id.login_username_error_text);
        passwordErrorTextbox = findViewById(R.id.login_password_error_text);

        addTextChangeListenerToEditText(usernameTextBox, usernameErrorTextbox);
        addTextChangeListenerToEditText(passwordTextBox, passwordErrorTextbox);

    }

    private void setupLoginButtonClickHandling() {

        loginButton.setOnClickListener(view -> {
            clearAllErrorMessages();
            LoginUserDTO loginUserDTO = new LoginUserDTO(
                    userFormService.extractInputFromEditText(usernameTextBox),
                    userFormService.extractInputFromEditText(passwordTextBox)
            );
            Optional<UserLoginResult> userLoginSyntacticalResult = userService.validateUserLoginData(loginUserDTO);
            if (userLoginSyntacticalResult.isPresent()) {
                switch (userLoginSyntacticalResult.get()) {
                    case BAD_USERNAME_FORMAT:
                        userFormService.showErrorMessage(usernameErrorTextbox, USERNAME_FORMAT_ERROR_MESSAGE);
                        break;
                    case BAD_PASSWORD_FORMAT:
                        userFormService.showErrorMessage(passwordErrorTextbox, PASSWORD_FORMAT_ERROR_MESSAGE);
                        break;
                }
            } else {
                userService.loginUser(loginUserDTO).thenAccept(userLoginResult -> {
                    switch (userLoginResult) {
                        case SUCCESS:
                            log.info("LoginActivity received successful login result!");
                            userSession.setUsername(loginUserDTO.getUsername());
                            redirectToChatListActivity();
                            break;
                        case FAILURE:
                            log.info("LoginActivity received failed login result!");
                            userFormService.showErrorMessage(usernameErrorTextbox, LOGIN_FAILURE_ERROR_MESSAGE);
                            break;
                    }
                });
            }
        });

    }

    private void addTextChangeListenerToEditText(EditText editText, TextInputLayout errorMessageBox) {
        editText.addTextChangedListener(new TextWatcher() {

            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                userFormService.clearErrorMessage(errorMessageBox);
            }

            @Override
            public void afterTextChanged(Editable editable) {

            }

        });
    }

    private void clearInputBoxes() {
        userFormService.clearInputBox(usernameTextBox);
        usernameTextBox.clearFocus();
        userFormService.clearInputBox(passwordTextBox);
        passwordTextBox.clearFocus();
    }

    private void redirectToChatListActivity() {
        Intent intent = new Intent(LoginActivity.this, ChatListActivity.class);
        startActivity(intent);
    }

    private void clearAllErrorMessages() {
        userFormService.clearErrorMessage(usernameErrorTextbox);
        userFormService.clearErrorMessage(passwordErrorTextbox);
    }

    private void clearRegistries() {
        messageRegistry.clearMessageRegistry();
        chatPartnerRegistry.clearChatPartnerRegistry();
        stompSubscriptionRegistry.clearStompSubscriptionRegistry();
    }

}