package com.tpeterb.securechatclient.ui.form;

import android.widget.EditText;
import android.widget.TextView;

import com.google.android.material.textfield.TextInputLayout;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class UserFormService {

    @Inject
    public UserFormService() {}

    public static final String USERNAME_FORMAT_ERROR_MESSAGE = "The provided username format is bad! It can only contain alphanumeric characters, underscores and dots and has to be 3-20 characters long!";

    public static final String PASSWORD_FORMAT_ERROR_MESSAGE = "The provided password format is bad! It has to be at least 8 characters long, and must contain at least one uppercase, one lowercase letter, and a digit!";

    public static final String EMAIL_FORMAT_ERROR_MESSAGE = "The email format is not valid!";

    public static final String CONFLICTED_USERNAME_ERROR_MESSAGE = "The provided username already exists, please choose a new one!";

    public static final String CONFLICTED_EMAIL_ERROR_MESSAGE = "The given email is already registered, please provide a different one!";

    public static final String GENERAL_REGISTRATION_FAILURE_ERROR_MESSAGE = "An error occurred while attempting registration! Please try again later!";

    public static final String SUCCESSFUL_REGISTRATION_MESSAGE = "Registration was successful, please log in to continue!";

    public static final String LOGIN_FAILURE_ERROR_MESSAGE = "There was an error while trying to log in! Please try again!";

    public void clearInputBox(EditText editText) {
        editText.setText(null);
    }

    public String extractInputFromEditText(EditText editText) {
        return editText.getText().toString().trim();
    }

    public void showMessage(TextView textView, String message) {
        textView.setText(message);
    }

    public void showErrorMessage(TextInputLayout errorMessageBox, String errorMessage) {
        errorMessageBox.setError(errorMessage);
        errorMessageBox.setAlpha(0);
        errorMessageBox.animate()
                .alpha(1)
                .setDuration(300)
                .start();
    }

    public void clearErrorMessage(TextInputLayout errorMessageBox) {
        errorMessageBox.animate()
                .alpha(0)
                .setDuration(300)
                .withEndAction(() -> {
                    errorMessageBox.setError(null);
                })
                .start();
    }

}
