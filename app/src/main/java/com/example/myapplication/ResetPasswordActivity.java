package com.example.myapplication;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.gson.JsonObject;


import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ResetPasswordActivity extends AppCompatActivity {

    private String accessToken;
    private EditText newPasswordEditText,newPasswordConfirmEditText;
    private Button resetBtn;
    private SupabaseAuthService authService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        SharedPreferences prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE);
        boolean isDark = prefs.getBoolean("isDarkTheme", true);
        AppCompatDelegate.setDefaultNightMode(
                isDark ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO
        );
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reset_password);

        newPasswordEditText = findViewById(R.id.newPasswordEditText);
        newPasswordConfirmEditText = findViewById(R.id.newPasswordConfirmEditText); // Matches layout ID
        resetBtn = findViewById(R.id.resetPasswordBtn);
        FloatingActionButton fabBackResetPage = findViewById(R.id.fabBackResetPage);

        Uri uri = getIntent().getData();
        if (uri != null && uri.getFragment() != null) {
            Uri fragmentUri = Uri.parse("http://dummy/?" + uri.getFragment());
            accessToken = fragmentUri.getQueryParameter("access_token");
            Log.d("ResetPasswordActivity", "Extracted Token: " + accessToken);
        } else {
            Log.d("ResetPasswordActivity", "URI or Fragment is null: " + (uri != null ? uri.toString() : "null"));
        }

        if (accessToken == null || accessToken.isEmpty()) {
            Toast.makeText(this, "Invalid or missing token", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        setPasswordToggle(newPasswordEditText);
        setPasswordToggle(newPasswordConfirmEditText);

        authService = RetrofitClient.getAuthService(); // Use default service

        // Set OnClickListener for the FloatingActionButton
        fabBackResetPage.setOnClickListener(v -> {
            Intent intent = new Intent(ResetPasswordActivity.this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
            finish(); // Close the current activity
        });

        resetBtn.setOnClickListener(v -> {
            newPasswordEditText.setEnabled(false);
            newPasswordConfirmEditText.setEnabled(false);
            resetBtn.setText("");
            findViewById(R.id.resetLoadingProgress).setVisibility(View.VISIBLE);
            resetBtn.setEnabled(false);


            String newPassword = newPasswordEditText.getText().toString().trim();
            String confirmPassword = newPasswordConfirmEditText.getText().toString().trim();
            if (newPassword.length() < 6) {
                restoreLoginState("Password must be at least 6 characters");
                return;
            }
            if (!newPassword.equals(confirmPassword)) {
                restoreLoginState("Passwords do not match!");
                return;
            }
            updatePassword(newPassword);
        });
    }

    private void restoreLoginState(String message) {
        newPasswordEditText.setEnabled(true);
        newPasswordConfirmEditText.setEnabled(true);
        findViewById(R.id.resetLoadingProgress).setVisibility(View.GONE); // Hide ProgressBar
        resetBtn.setEnabled(true);
        resetBtn.setText("Reset Password");
        if(message != null) {
            Toast.makeText(ResetPasswordActivity.this, message, Toast.LENGTH_SHORT).show();
        }
    }


    @SuppressLint("ClickableViewAccessibility")
    private void setPasswordToggle(EditText passwordField) {
        passwordField.setOnTouchListener((v, event) -> {
            final int DRAWABLE_RIGHT = 2;
            if (event.getAction() == MotionEvent.ACTION_UP) {
                if (event.getRawX() >= (passwordField.getRight() - passwordField.getCompoundDrawables()[DRAWABLE_RIGHT].getBounds().width())) {
                    int selection = passwordField.getSelectionEnd();
                    Typeface currentTypeface = passwordField.getTypeface();
                    boolean isVisible = (passwordField.getInputType() & InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD) == InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD;

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        passwordField.setImportantForAutofill(View.IMPORTANT_FOR_AUTOFILL_NO);
                    }
                    passwordField.setShowSoftInputOnFocus(false);
                    passwordField.clearFocus();
                    passwordField.requestFocus();
                    passwordField.setShowSoftInputOnFocus(true);

                    if (isVisible) {
                        passwordField.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                        passwordField.setCompoundDrawablesWithIntrinsicBounds(R.drawable.lock_solid, 0, R.drawable.ic_eye_closed, 0);
                    } else {
                        passwordField.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
                        passwordField.setCompoundDrawablesWithIntrinsicBounds(R.drawable.lock_solid, 0, R.drawable.ic_eye_open, 0);
                    }

                    passwordField.setTypeface(currentTypeface);
                    passwordField.setSelection(selection);

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        passwordField.setImportantForAutofill(View.IMPORTANT_FOR_AUTOFILL_AUTO);
                    }
                    return true;
                }
            }
            return false;
        });
    }

    private void updatePassword(String newPassword) {
        JsonObject body = new JsonObject();
        body.addProperty("password", newPassword);

        authService.updatePassword("Bearer " + accessToken, body).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                restoreLoginState(null);

                if (response.isSuccessful()) {
                    // ✅ Show success dialog
                    CustomDialog.showDialog(
                            ResetPasswordActivity.this,
                            "Password updated!",
                            "Please go back to the Sign In page.",
                            "Go Back",
                            null,
                            R.raw.completed,
                            new CustomDialog.DialogCallback() {
                                @Override
                                public void onConfirm() {
                                    startActivity(new Intent(ResetPasswordActivity.this, LoginActivity.class));
                                    finish();
                                }

                                @Override
                                public void onCancel() { }
                            }
                    );
                } else {
                    try {
                        String errorBody = response.errorBody() != null ? response.errorBody().string() : null;
                        if (response.code() == 422 && errorBody != null) {
                            org.json.JSONObject json = new org.json.JSONObject(errorBody);
                            String errorCode = json.optString("error_code");
                            String message = json.optString("msg", "New password must be different.");

                            if ("same_password".equals(errorCode)) {
                                // ⚠️ Show dialog for same password case
                                CustomDialog.showDialog(
                                        ResetPasswordActivity.this,
                                        "Password unchanged!",
                                        message,
                                        "Ok",
                                        null,
                                        R.raw.warning,
                                        new CustomDialog.DialogCallback() {
                                            @Override
                                            public void onConfirm() { }

                                            @Override
                                            public void onCancel() { }
                                        }
                                );
                            } else {
                                Toast.makeText(ResetPasswordActivity.this, message, Toast.LENGTH_LONG).show();
                            }
                        } else {
                            Toast.makeText(ResetPasswordActivity.this, "Failed to update password: " + response.code(), Toast.LENGTH_LONG).show();
                        }
                        Log.e("ResetPasswordActivity", "Error Response Body: " + errorBody);
                    } catch (Exception e) {
                        Log.e("ResetPasswordActivity", "Failed to parse error body", e);
                        Toast.makeText(ResetPasswordActivity.this, "Unexpected error occurred", Toast.LENGTH_SHORT).show();
                    }
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                restoreLoginState(null);
                // ❌ Don't show dialog, only log and toast
                Toast.makeText(ResetPasswordActivity.this, "Network error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                Log.e("ResetPasswordActivity", "Failure: " + t.getMessage(), t);
            }
        });
    }
}