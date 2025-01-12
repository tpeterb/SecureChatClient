package com.tpeterb.securechatclient.ui;

import static com.tpeterb.securechatclient.ui.form.UserFormService.CONFLICTED_EMAIL_ERROR_MESSAGE;
import static com.tpeterb.securechatclient.ui.form.UserFormService.CONFLICTED_USERNAME_ERROR_MESSAGE;
import static com.tpeterb.securechatclient.ui.form.UserFormService.EMAIL_FORMAT_ERROR_MESSAGE;
import static com.tpeterb.securechatclient.ui.form.UserFormService.GENERAL_REGISTRATION_FAILURE_ERROR_MESSAGE;
import static com.tpeterb.securechatclient.ui.form.UserFormService.PASSWORD_FORMAT_ERROR_MESSAGE;
import static com.tpeterb.securechatclient.ui.form.UserFormService.USERNAME_FORMAT_ERROR_MESSAGE;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.Button;
import android.widget.EditText;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.textfield.TextInputLayout;
import com.tpeterb.securechatclient.R;
import com.tpeterb.securechatclient.application.ChatApplication;
import com.tpeterb.securechatclient.ui.form.UserFormService;
import com.tpeterb.securechatclient.users.model.RegisterUserDTO;
import com.tpeterb.securechatclient.users.model.UserRegistrationResult;
import com.tpeterb.securechatclient.users.service.UserService;

import java.util.Optional;

import javax.inject.Inject;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class RegisterActivity extends AppCompatActivity {

    @Inject
    UserService userService;

    @Inject
    UserFormService userFormService;

    private EditText usernameEditText;

    private EditText passwordEditText;

    private EditText emailEditText;

    private Button registerButton;

    private TextInputLayout usernameErrorMessageBox;

    private TextInputLayout passwordErrorMessageBox;

    private TextInputLayout emailErrorMessageBox;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        ChatApplication.getAppComponent().inject(this);
        setContentView(R.layout.activity_register);

        initializeUIElements();

        clearInputBoxes();

        setupRegistrationButtonClickHandling();

    }

    private void clearInputBoxes() {
        userFormService.clearInputBox(usernameEditText);
        userFormService.clearInputBox(passwordEditText);
        userFormService.clearInputBox(emailEditText);
    }

    private void setupRegistrationButtonClickHandling() {

        registerButton.setOnClickListener(view -> {
            clearAllErrorMessages();
            RegisterUserDTO registerUserDTO = new RegisterUserDTO(
                    userFormService.extractInputFromEditText(usernameEditText),
                    userFormService.extractInputFromEditText(passwordEditText),
                    userFormService.extractInputFromEditText(emailEditText));
            Optional<UserRegistrationResult> userRegistrationSyntacticalResult = userService.validateUserRegistrationData(registerUserDTO);
            if (userRegistrationSyntacticalResult.isPresent()) {
                switch (userRegistrationSyntacticalResult.get()) {
                    case BAD_USERNAME_FORMAT:
                        userFormService.showErrorMessage(usernameErrorMessageBox, USERNAME_FORMAT_ERROR_MESSAGE);
                        break;
                    case BAD_PASSWORD_FORMAT:
                        userFormService.showErrorMessage(passwordErrorMessageBox, PASSWORD_FORMAT_ERROR_MESSAGE);
                        break;
                    case BAD_EMAIL_FORMAT:
                        userFormService.showErrorMessage(emailErrorMessageBox, EMAIL_FORMAT_ERROR_MESSAGE);
                        break;
                }
            } else {
                userService.registerUser(registerUserDTO).thenAccept(userRegistrationResult -> {
                    switch (userRegistrationResult) {
                        case SUCCESS:
                            log.info("RegisterActivity got the successful login result!");
                            redirectToLoginActivity();
                            break;
                        case CONFLICTED_USERNAME:
                            log.info("A user already registered with this username!");
                            userFormService.showErrorMessage(usernameErrorMessageBox, CONFLICTED_USERNAME_ERROR_MESSAGE);
                            break;
                        case CONFLICTED_EMAIL:
                            log.info("A user already registered with this email!");
                            userFormService.showErrorMessage(emailErrorMessageBox, CONFLICTED_EMAIL_ERROR_MESSAGE);
                            break;
                        case GENERAL_FAILURE:
                            log.error("RegisterActivity got the failed login result!");
                            userFormService.showErrorMessage(usernameErrorMessageBox, GENERAL_REGISTRATION_FAILURE_ERROR_MESSAGE);
                            break;
                    }
                });
            }
        });

    }

    private void initializeUIElements() {

        registerButton = findViewById(R.id.register_page_register_button);
        usernameEditText = findViewById(R.id.register_username_textbox);
        passwordEditText = findViewById(R.id.register_password_textbox);
        emailEditText = findViewById(R.id.register_email_textbox);
        usernameErrorMessageBox = findViewById(R.id.register_username_error_text);
        passwordErrorMessageBox = findViewById(R.id.register_password_error_text);
        emailErrorMessageBox = findViewById(R.id.register_email_error_text);

        addEditTextChangeListener(usernameEditText, usernameErrorMessageBox);
        addEditTextChangeListener(passwordEditText, passwordErrorMessageBox);
        addEditTextChangeListener(emailEditText, emailErrorMessageBox);

    }

    private void clearAllErrorMessages() {
        userFormService.clearErrorMessage(usernameErrorMessageBox);
        userFormService.clearErrorMessage(passwordErrorMessageBox);
        userFormService.clearErrorMessage(emailErrorMessageBox);
    }

    private void addEditTextChangeListener(EditText editText, TextInputLayout errorMessageBox) {
        editText.addTextChangedListener(new TextWatcher() {

            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {}

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                userFormService.clearErrorMessage(errorMessageBox);
            }

            @Override
            public void afterTextChanged(Editable editable) {}

        });
    }

    private void redirectToLoginActivity() {
        Intent intent = new Intent(RegisterActivity.this, LoginActivity.class);
        intent.putExtra("showSuccessfulRegistrationMessage", true);
        startActivity(intent);
    }

}