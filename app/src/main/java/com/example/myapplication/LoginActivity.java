package com.example.myapplication;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.constraintlayout.widget.ConstraintLayout;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LoginActivity extends BaseActivity {

    EditText emailSignin, passwordSignin, emailSignup, passwordSignup, confirmPasswordInput, forgotEmail;
    Button signinBtn, resetPasswordBtn, signupBtn;
    ConstraintLayout signInLayout, signUpLayout, forgotPassLayout;
    TextView goToSignup, backToSigninBtn, headerText, goToForgotPass, forgotToSigninBtn;
    SupabaseAuthService authService;

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        SharedPreferences prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE);
        boolean isDark = prefs.getBoolean("isDarkTheme", true);
        AppCompatDelegate.setDefaultNightMode(
                isDark ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO
        );

        super.onCreate(savedInstanceState);

        String token = getSharedPreferences("AUTH", MODE_PRIVATE).getString("TOKEN", null);
        if (token != null) {
            startActivity(new Intent(LoginActivity.this, MainActivity.class));
            finish();
            return;
        }

        setContentView(R.layout.activity_login);

        headerText = findViewById(R.id.headerText);
        emailSignin = findViewById(R.id.email_signin);
        passwordSignin = findViewById(R.id.password_signin);
        signInLayout = findViewById(R.id.signInLayout);
        signinBtn = findViewById(R.id.signinBtn);
        backToSigninBtn = findViewById(R.id.goToSignin);
        forgotToSigninBtn = findViewById(R.id.forgotToSigninBtn);
        emailSignup = findViewById(R.id.email_signup);
        passwordSignup = findViewById(R.id.password_signup);
        signUpLayout = findViewById(R.id.signUpLayout);
        signupBtn = findViewById(R.id.signupBtn);
        goToSignup = findViewById(R.id.goToSignup);
        confirmPasswordInput = findViewById(R.id.confirm_password);
        goToForgotPass = findViewById(R.id.goToForgotPass);
        forgotPassLayout = findViewById(R.id.forgotPassLayout);
        forgotEmail = findViewById(R.id.forgot_email);
        resetPasswordBtn = findViewById(R.id.resetPasswordBtn);

        authService = RetrofitClient.getAuthService();

        setPasswordToggle(passwordSignin);
        setPasswordToggle(passwordSignup);
        setPasswordToggle(confirmPasswordInput);

        signinBtn.setOnClickListener(v -> loginUser());
        signupBtn.setOnClickListener(v -> signupUser());
        resetPasswordBtn.setOnClickListener(v -> resetPassword());

        goToSignup.setOnClickListener(v -> {
            animateSlide(signInLayout, signUpLayout);
            updateHeaderText("Create Account");
        });
        backToSigninBtn.setOnClickListener(v -> {
            animateSlide2(signUpLayout, signInLayout);
            updateHeaderText("Welcome Back");
        });
        goToForgotPass.setOnClickListener(v -> {
            animateSlide2(signInLayout, forgotPassLayout);
            updateHeaderText("Forgot Password");
        });
        forgotToSigninBtn.setOnClickListener(v -> {
            animateSlide(forgotPassLayout, signInLayout);
            updateHeaderText("Welcome Back");
        });
    }

    private void startLoading(Button button, View loadingProgress, EditText... inputs) {
        // Disable all input fields
        for (EditText input : inputs) {
            if (input != null) input.setEnabled(false);
        }
        // Show loading state on button
        button.setText("");
        if (loadingProgress != null) {
            loadingProgress.setVisibility(View.VISIBLE);
        }
        button.setEnabled(false);
    }

    private void restoreState(Button button, View loadingProgress, EditText[] inputs, String defaultText, String message) {
        // Re-enable all input fields
        for (EditText input : inputs) {
            if (input != null) input.setEnabled(true);
        }
        // Restore button state
        button.setText(defaultText);
        if (loadingProgress != null) {
            loadingProgress.setVisibility(View.GONE);
        }
        button.setEnabled(true);
        if(message != null) {
            Toast.makeText(LoginActivity.this, message, Toast.LENGTH_SHORT).show();
        }
    }

    private void loginUser() {
        startLoading(signinBtn, findViewById(R.id.signinLoadingProgress), emailSignin, passwordSignin);

        String email = emailSignin.getText().toString().trim();
        String password = passwordSignin.getText().toString().trim();

        if (email.isEmpty() || password.isEmpty()) {
            restoreState(signinBtn, findViewById(R.id.signinLoadingProgress), new EditText[]{emailSignin, passwordSignin},
                    "Sign In", "Please enter email and password");
            return;
        }

        LoginRequest request = new LoginRequest(email, password);
        authService.login(request).enqueue(new Callback<LoginResponse>() {
            @Override
            public void onResponse(Call<LoginResponse> call, Response<LoginResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    String token = response.body().access_token;
                    String refreshToken = response.body().refresh_token;
                    String userId = response.body().user.id;

                    SharedPreferences.Editor editor = getSharedPreferences("AUTH", MODE_PRIVATE).edit();
                    editor.putString("TOKEN", token);
                    editor.putString("REFRESH_TOKEN", refreshToken);
                    editor.putString("UID", userId);
                    editor.apply();

                    SupabaseAuthService authorizedService = RetrofitClient.getAuthService(token);
                    Map<String, String> query = new HashMap<>();
                    query.put("id", "eq." + userId);
                    query.put("select", "*"); // Add select parameter

                    authorizedService.getProfile(query)
                            .enqueue(new Callback<List<ProfileRequest>>() {
                                @Override
                                public void onResponse(Call<List<ProfileRequest>> call, Response<List<ProfileRequest>> profileResponse) {
                                    if (profileResponse.isSuccessful() && profileResponse.body() != null && !profileResponse.body().isEmpty()) {
                                        ProfileRequest profile = profileResponse.body().get(0);
                                        String email = profile.getEmail();
                                        String username = profile.getUsername();

                                        // Save in SharedPreferences if needed
                                        SharedPreferences.Editor profileEditor = getSharedPreferences("AUTH", MODE_PRIVATE).edit();
                                        profileEditor.putString("EMAIL", email);
                                        profileEditor.putString("USERNAME", username);
                                        profileEditor.apply();

                                        // ✅ Set current user email for access across app
                                        MainActivity.setCurrentUserEmail(email);

                                    } else {
                                        try {
                                            String errorBody = profileResponse.errorBody() != null ? profileResponse.errorBody().string() : "No error body";
                                            Log.e("PROFILE", "Profile fetch failed: Code=" + profileResponse.code() + ", Body=" + errorBody);
                                        } catch (IOException e) {
                                            Log.e("PROFILE", "Error reading errorBody: " + e.getMessage());
                                        }
                                    }
                                    Toast.makeText(LoginActivity.this, "Login Success!", Toast.LENGTH_SHORT).show();
                                    startActivity(new Intent(LoginActivity.this, MainActivity.class));
                                    finish();
                                }

                                @Override
                                public void onFailure(Call<List<ProfileRequest>> call, Throwable t) {
                                    Log.e("PROFILE", "Profile fetch failed: " + t.getMessage());
                                    Toast.makeText(LoginActivity.this, "Login Success, but failed to load profile.", Toast.LENGTH_SHORT).show();
                                    startActivity(new Intent(LoginActivity.this, MainActivity.class));
                                    finish();
                                }
                            });
                } else {
                    CustomDialog.showDialog(
                            LoginActivity.this,
                            "Login Failed !",
                            "Incorrect email or password.\n Please try again.",
                            "Ok",
                            null,
                            R.raw.error,
                            new CustomDialog.DialogCallback() {
                                @Override public void onConfirm() {}
                                @Override public void onCancel() {}
                            }
                    );
                    restoreState(signinBtn, findViewById(R.id.signinLoadingProgress),
                            new EditText[]{emailSignin, passwordSignin},
                            "Sign In", null);
                }
            }

            @Override
            public void onFailure(Call<LoginResponse> call, Throwable t) {
                restoreState(signinBtn, findViewById(R.id.signinLoadingProgress),
                        new EditText[]{emailSignin, passwordSignin},
                        "Sign In", "Error: " + t.getMessage());
            }
        });
    }



    private void signupUser() {
        startLoading(signupBtn, findViewById(R.id.signupLoadingProgress), emailSignup, passwordSignup, confirmPasswordInput);

        String email = emailSignup.getText().toString().trim();
        String password = passwordSignup.getText().toString().trim();
        String confirmPassword = confirmPasswordInput.getText().toString().trim();

        // ✅ Restrict to gmail.com only
        if (!email.toLowerCase().endsWith("@gmail.com")) {
            restoreState(signupBtn, findViewById(R.id.signupLoadingProgress), new EditText[]{emailSignup, passwordSignup, confirmPasswordInput}, "Sign Up", "Only Gmail accounts are allowed.");
            return;
        }

        if (!password.equals(confirmPassword)) {
            restoreState(signupBtn, findViewById(R.id.signupLoadingProgress), new EditText[]{emailSignup, passwordSignup, confirmPasswordInput}, "Sign Up", "Passwords do not match!");
            return;
        }

        if (email.isEmpty() || password.isEmpty()) {
            restoreState(signupBtn, findViewById(R.id.signupLoadingProgress), new EditText[]{emailSignup, passwordSignup, confirmPasswordInput}, "Sign Up", "Please fill in all fields.");
            return;
        }

        SignupRequest request = new SignupRequest(email, password);
        authService.signUp(request).enqueue(new Callback<SignupResponse>() {
            @Override
            public void onResponse(Call<SignupResponse> call, Response<SignupResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    SignupResponse signupResponse = response.body();
                    String uid = signupResponse.user.id;
                    String email = signupResponse.user.email;
                    String username = email.split("@")[0];

                    ProfileRequest profile = new ProfileRequest(uid, email, username);
                    SupabaseAuthService authorizedService = RetrofitClient.getAuthService(signupResponse.access_token);
                    authorizedService.createProfile(profile).enqueue(new Callback<Void>() {
                        @Override
                        public void onResponse(Call<Void> call, Response<Void> response) {
                            if (response.isSuccessful()) {
                                Log.d("Profile", "Profile created successfully");
                            } else {
                                try {
                                    Log.e("Profile", "Error creating profile: " + response.errorBody().string());
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                            }
                        }

                        @Override
                        public void onFailure(Call<Void> call, Throwable t) {
                            Log.e("Profile", "Failed to create profile: " + t.getMessage());
                        }
                    });

                    restoreState(signupBtn, findViewById(R.id.signupLoadingProgress), new EditText[]{emailSignup, passwordSignup, confirmPasswordInput}, "Sign Up", "Signup Success! Please Sign In.");
                } else {
                    try {
                        String errorBody = response.errorBody() != null ? response.errorBody().string() : "";
                        if (errorBody.toLowerCase().contains("already registered")) {
                            restoreState(signupBtn, findViewById(R.id.signupLoadingProgress), new EditText[]{emailSignup, passwordSignup, confirmPasswordInput}, "Sign Up", "This email is already registered.");
                        } else {
                            restoreState(signupBtn, findViewById(R.id.signupLoadingProgress), new EditText[]{emailSignup, passwordSignup, confirmPasswordInput}, "Sign Up", "Signup Failed: " + errorBody);
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                        restoreState(signupBtn, findViewById(R.id.signupLoadingProgress), new EditText[]{emailSignup, passwordSignup, confirmPasswordInput}, "Sign Up", "Signup Failed!");
                    }
                }
            }

            @Override
            public void onFailure(Call<SignupResponse> call, Throwable t) {
                restoreState(signupBtn, findViewById(R.id.signupLoadingProgress), new EditText[]{emailSignup, passwordSignup, confirmPasswordInput}, "Sign Up", "Error: " + t.getMessage());
            }
        });
    }


    private void resetPassword() {
        startLoading(resetPasswordBtn, findViewById(R.id.forgotLoadingProgress), forgotEmail);

        String email = forgotEmail.getText().toString().trim();
        if (email.isEmpty()) {
            restoreState(resetPasswordBtn, findViewById(R.id.forgotLoadingProgress), new EditText[]{forgotEmail}, "Reset Password", "Please enter your email");
            return;
        }
        PasswordResetRequest request = new PasswordResetRequest(email, "myapp://reset-password");
        authService.resetPassword(request).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(@NonNull Call<Void> call, @NonNull Response<Void> response) {
                if (response.isSuccessful()) {
                    restoreState(resetPasswordBtn, findViewById(R.id.forgotLoadingProgress), new EditText[]{forgotEmail}, "Reset Password", "Reset link sent. Check your email.");
                } else {
                    restoreState(resetPasswordBtn, findViewById(R.id.forgotLoadingProgress), new EditText[]{forgotEmail}, "Reset Password", "Failed to send reset link: " + response.code());
                }
            }
            @Override
            public void onFailure(@NonNull Call<Void> call, @NonNull Throwable t) {
                restoreState(resetPasswordBtn, findViewById(R.id.forgotLoadingProgress), new EditText[]{forgotEmail}, "Reset Password", "Error: " + t.getMessage());
            }
        });
    }

    private void updateHeaderText(String newText) {
        headerText.animate().alpha(0f).setDuration(150).withEndAction(() -> {
            headerText.setText(newText);
            headerText.animate().alpha(1f).setDuration(150).start();
        }).start();
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

    private void animateSlide(View outView, View inView) {
        outView.animate()
                .translationX(-outView.getWidth())
                .alpha(0f)
                .setDuration(250)
                .withEndAction(() -> {
                    outView.setVisibility(View.GONE);
                    outView.setTranslationX(0);
                    outView.setAlpha(1f);
                    inView.setTranslationX(inView.getWidth());
                    inView.setAlpha(0f);
                    inView.setVisibility(View.VISIBLE);
                    inView.animate()
                            .translationX(0)
                            .alpha(1f)
                            .setDuration(250)
                            .start();
                }).start();
    }

    private void animateSlide2(View outView, View inView) {
        outView.animate()
                .translationX(outView.getWidth())
                .alpha(0f)
                .setDuration(250)
                .withEndAction(() -> {
                    outView.setVisibility(View.GONE);
                    outView.setTranslationX(0);
                    outView.setAlpha(1f);
                    inView.setTranslationX(-inView.getWidth());
                    inView.setAlpha(0f);
                    inView.setVisibility(View.VISIBLE);
                    inView.animate()
                            .translationX(0)
                            .alpha(1f)
                            .setDuration(250)
                            .start();
                }).start();
    }
}