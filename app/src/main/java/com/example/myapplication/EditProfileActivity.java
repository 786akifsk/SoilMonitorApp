package com.example.myapplication;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.bumptech.glide.request.RequestOptions;
import com.google.gson.JsonObject;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import okhttp3.MediaType;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class EditProfileActivity extends AppCompatActivity {

    private static final int PICK_IMAGE_REQUEST = 1;
    private Uri imageUri;
    private ImageView currentProfileImage;
    private EditText editUsername;
    private Button updateBtn;
    ImageButton editIcon;

    private View loadingProgress;
    private String token, userId, currentUsername, currentAvatarUrl;
    private SupabaseAuthService authService;

    // Regular expression for letters and numbers only (no symbols)
    private static final Pattern USERNAME_PATTERN = Pattern.compile("^[a-zA-Z0-9 ]+$");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_profile);

        currentProfileImage = findViewById(R.id.currentProfileImage);
        editUsername = findViewById(R.id.editUsername);
        updateBtn = findViewById(R.id.updateBtn);

        editIcon = findViewById(R.id.imageButton);
        editIcon.setOnClickListener(v -> openGallery());

        loadingProgress = findViewById(R.id.loadingProgress);

        token = getSharedPreferences("AUTH", MODE_PRIVATE).getString("TOKEN", null);
        userId = getSharedPreferences("AUTH", MODE_PRIVATE).getString("UID", null);

        if (token == null || userId == null) {
            Toast.makeText(this, "Authentication error: Please log in again", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        authService = RetrofitClient.getAuthService(token);

        // Fetch current profile data
        fetchCurrentProfile();

        updateBtn.setOnClickListener(v -> updateProfile());

        Toolbar toolbar = findViewById(R.id.editProfile);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> onBackPressed());
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
        if (message != null) {
            Toast.makeText(EditProfileActivity.this, message, Toast.LENGTH_SHORT).show();
        }
    }

    private void fetchCurrentProfile() {
        Map<String, String> queryMap = new HashMap<>();
        queryMap.put("id", "eq." + userId);
        queryMap.put("select", "*");

        authService.getProfile(queryMap).enqueue(new Callback<List<ProfileRequest>>() {
            @Override
            public void onResponse(Call<List<ProfileRequest>> call, Response<List<ProfileRequest>> response) {
                if (response.isSuccessful() && response.body() != null && !response.body().isEmpty()) {
                    ProfileRequest profile = response.body().get(0);
                    currentUsername = profile.getUsername();
                    currentAvatarUrl = profile.getAvatarUrl();

                    // Set current username
                    editUsername.setText(currentUsername);

                    // Load current avatar
                    if (currentAvatarUrl != null && !currentAvatarUrl.isEmpty()) {
                        // Add timestamp to bust the cache
                        String bustCacheUrl = currentAvatarUrl + "?t=" + System.currentTimeMillis();

                        Glide.with(EditProfileActivity.this)
                                .load(bustCacheUrl)
                                .transition(DrawableTransitionOptions.withCrossFade(300))
                                .apply(RequestOptions.circleCropTransform())
                                .placeholder(R.drawable.default_avatar)
                                .error(R.drawable.default_avatar)
                                .into(currentProfileImage);
                    } else {
                        Glide.with(EditProfileActivity.this)
                                .load(R.drawable.default_avatar)
                                .apply(RequestOptions.circleCropTransform())
                                .into(currentProfileImage);
                    }
                } else {
                    try {
                        String errorBody = response.errorBody() != null ? response.errorBody().string() : "No error body";
                        Log.e("PROFILE_FETCH", "Failed: Code=" + response.code() + ", Body=" + errorBody);
                        editUsername.setText("Unknown");
                        Glide.with(EditProfileActivity.this)
                                .load(R.drawable.default_avatar)
                                .apply(RequestOptions.circleCropTransform())
                                .into(currentProfileImage);
                    } catch (IOException e) {
                        Log.e("PROFILE_FETCH", "Error reading errorBody: " + e.getMessage());
                    }
                }
            }

            @Override
            public void onFailure(Call<List<ProfileRequest>> call, Throwable t) {
                Log.e("PROFILE_FETCH", "Error fetching profile", t);
                editUsername.setText("Unknown");
                Glide.with(EditProfileActivity.this)
                        .load(R.drawable.default_avatar)
                        .apply(RequestOptions.circleCropTransform())
                        .into(currentProfileImage);
            }
        });
    }

    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        startActivityForResult(Intent.createChooser(intent, "Select Picture"), PICK_IMAGE_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null && data.getData() != null) {
            imageUri = data.getData();
            try {
                Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), imageUri);
                currentProfileImage.setImageBitmap(bitmap);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

//    private boolean isValidUsername(String username) {
//        // Check length (> 4 characters)
//        if (username.length() <= 4 ) {
//            return false;
//        }
////         Check for letters and numbers only (no symbols)
//        if (!USERNAME_PATTERN.matcher(username).matches()) {
//            return false;
//        }
//        return true;
//    }

    private void updateProfile() {
        String newUsername = editUsername.getText().toString().trim();
        boolean usernameChanged = !newUsername.equals(currentUsername);
        boolean imageChanged = imageUri != null;

        // Validate username if it has changed
        if (usernameChanged) {
            if (newUsername.length() < 4) {
                Toast.makeText(this, "Username must be longer than 4 characters", Toast.LENGTH_SHORT).show();
                return;
            }
            if (newUsername.length() > 8) {
                Toast.makeText(this, "Username must be shorter than 8 characters", Toast.LENGTH_SHORT).show();
                return;
            }
            if (!USERNAME_PATTERN.matcher(newUsername).matches()) {
                Toast.makeText(this, "Username must contain only letters and numbers", Toast.LENGTH_SHORT).show();
                return;
            }
        }

        if (!usernameChanged && !imageChanged) {
            Toast.makeText(this, "No changes to update", Toast.LENGTH_SHORT).show();
            return;
        }
        startLoading(updateBtn, loadingProgress, editUsername);
        if (imageChanged) {
            uploadImageToSupabase();
        } else if (usernameChanged) {
            updateProfileData(null, newUsername);
        }
    }

    private void uploadImageToSupabase() {
        try {
            File file = FileUtil.from(this, imageUri);
            String mimeType = getContentResolver().getType(imageUri);
            Log.d("UPLOAD_DEBUG", "File: " + file.getAbsolutePath() + ", MIME Type: " + mimeType + ", Size: " + file.length());

            RequestBody fileBody = RequestBody.create(file, MediaType.parse(mimeType));
            SupabaseService storageService = RetrofitClient.getStorageService(token);

            storageService.uploadAvatar(userId + ".png", fileBody).enqueue(new Callback<ResponseBody>() {
                @Override
                public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                    Log.d("UPLOAD_DEBUG", "Response Code: " + response.code() + ", Is Successful: " + response.isSuccessful());
                    if (response.isSuccessful()) {
                        String publicUrl = BuildConfig.SUPABASE_URL + "/storage/v1/object/public/avatars/" + userId + ".png";
                        updateProfileData(publicUrl, editUsername.getText().toString().trim());
                    } else {
                        try {
                            String errorBody = response.errorBody() != null ? response.errorBody().string() : "No error details";
                            Log.e("UPLOAD_FAIL", "Code: " + response.code() + ", Body: " + errorBody);
                            restoreState(updateBtn, loadingProgress, new EditText[]{editUsername}, "Update Profile", "Image upload failed: " + errorBody);
                        } catch (IOException e) {
                            Log.e("UPLOAD_FAIL", "Error reading error body: " + e.getMessage());
                            restoreState(updateBtn, loadingProgress, new EditText[]{editUsername}, "Update Profile", "Image upload failed: Error reading response");
                        }
                    }
                }

                @Override
                public void onFailure(Call<ResponseBody> call, Throwable t) {
                    Log.e("UPLOAD_FAIL", "Upload error: " + t.getMessage());
                    restoreState(updateBtn, loadingProgress, new EditText[]{editUsername}, "Update Profile", "Upload failed");
                }
            });

        } catch (Exception e) {
            Log.e("UPLOAD_FAIL", "File conversion error: " + e.getMessage(), e);
            Toast.makeText(this, "File conversion error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void updateProfileData(String avatarUrl, String newUsername) {
        JsonObject updateBody = new JsonObject();
        if (avatarUrl != null) {
            updateBody.addProperty("avatar_url", avatarUrl);
        }
        if (newUsername != null && !newUsername.equals(currentUsername)) {
            updateBody.addProperty("username", newUsername);
        }

        Map<String, String> query = new HashMap<>();
        query.put("id", "eq." + userId);

        authService.updateProfile(updateBody, query).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                restoreState(updateBtn, loadingProgress, new EditText[]{editUsername}, "Update Profile", null);

                if (response.isSuccessful()) {
                    boolean usernameActuallyChanged = !newUsername.equals(currentUsername);
                    boolean avatarActuallyChanged = avatarUrl != null;

                    Intent resultIntent = new Intent();
                    resultIntent.putExtra("avatarChanged", avatarActuallyChanged);
                    resultIntent.putExtra("usernameChanged", usernameActuallyChanged);
                    setResult(RESULT_OK, resultIntent);

                    currentUsername = newUsername;
                    currentAvatarUrl = avatarUrl;

                    Toast.makeText(EditProfileActivity.this, "Profile updated!", Toast.LENGTH_SHORT).show();

                    if (avatarUrl != null && !avatarUrl.isEmpty()) {
                        String bustCacheUrl = avatarUrl + "?t=" + System.currentTimeMillis();
                        Glide.with(EditProfileActivity.this)
                                .load(bustCacheUrl)
                                .transition(DrawableTransitionOptions.withCrossFade(300))
                                .apply(RequestOptions.circleCropTransform())
                                .placeholder(R.drawable.default_avatar)
                                .error(R.drawable.default_avatar)
                                .into(currentProfileImage);
                    }

                    finish(); // ✅ finish last
                } else {
                    try {
                        String errorBody = response.errorBody() != null ? response.errorBody().string() : "";
                        if (errorBody.contains("duplicate key value violates unique constraint")) {
                            // ⚠️ Show dialog for same password case
                            CustomDialog.showDialog(
                                    EditProfileActivity.this,
                                    "Username Unavailable",
                                    "The username you entered is already in use. Please try a different one.",
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
                            Toast.makeText(EditProfileActivity.this, "Update failed: " + errorBody, Toast.LENGTH_SHORT).show();
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                        Toast.makeText(EditProfileActivity.this, "Update failed!", Toast.LENGTH_SHORT).show();
                    }
                }
            }


            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                Toast.makeText(EditProfileActivity.this, "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }


    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
    }
}