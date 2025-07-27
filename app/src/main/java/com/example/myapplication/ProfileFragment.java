package com.example.myapplication;

import static android.app.Activity.RESULT_OK;
import static android.content.Context.MODE_PRIVATE;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.bumptech.glide.request.RequestOptions;
import com.google.gson.JsonObject;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;


public class ProfileFragment extends Fragment {

    private static final int EDIT_PROFILE_REQUEST_CODE = 1001;

    private ProgressBar profileLoadingSpinner;
    private SwipeRefreshLayout swipeRefreshLayout;


    private TextView profileName, profileEmail;
    private ImageView profileImage;
    private Button logoutBtn;

    @SuppressLint("MissingInflatedId")
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        profileName = view.findViewById(R.id.profileName);
        profileEmail = view.findViewById(R.id.profileEmail);
        profileImage = view.findViewById(R.id.profileImage);

        Glide.with(this)
                .load(R.drawable.default_avatar)
                .apply(RequestOptions.circleCropTransform())
                .into(profileImage);


        TextView openSettings = view.findViewById(R.id.openSettings);
        openSettings.setOnClickListener(v -> {
            Intent intent = new Intent(requireContext(), SettingsActivity.class);
            startActivity(intent);
        });

        swipeRefreshLayout = view.findViewById(R.id.profileSwipeRefresh);
        swipeRefreshLayout.setOnRefreshListener(() -> {
            fetchUserProfile();  // Refresh user info
        });


        Button editProfileBtn = view.findViewById(R.id.editProfileBtn);
        editProfileBtn.setOnClickListener(v -> {
            startActivityForResult(new Intent(requireContext(), EditProfileActivity.class), EDIT_PROFILE_REQUEST_CODE);
        });
        profileLoadingSpinner = view.findViewById(R.id.profileLoadingSpinner);

        fetchUserProfile();

        logoutBtn = view.findViewById(R.id.logoutBtn);
        logoutBtn.setOnClickListener(v -> {
            requireActivity().getSharedPreferences("AUTH", MODE_PRIVATE)
                    .edit()
                    .remove("TOKEN")
                    .remove("UID")
                    .remove("USERNAME")
                    .remove("EMAIL")
                    .apply();
            startActivity(new Intent(requireActivity(), LoginActivity.class));
            requireActivity().finish();
        });

        return view;
    }

    private void fetchUserProfile() {
        SharedPreferences prefs = requireActivity().getSharedPreferences("AUTH", MODE_PRIVATE);
        String token = prefs.getString("TOKEN", null);
        String refreshToken = prefs.getString("REFRESH_TOKEN", null); // <- Make sure this is stored at login
        String userId = prefs.getString("UID", null);
        profileLoadingSpinner.setVisibility(View.VISIBLE);

        if (token != null && userId != null) {
            SupabaseAuthService authService = RetrofitClient.getAuthService(token);
            Map<String, String> queryMap = new HashMap<>();
            queryMap.put("id", "eq." + userId);
            queryMap.put("select", "*");

            authService.getProfile(queryMap).enqueue(new Callback<List<ProfileRequest>>() {
                @Override
                public void onResponse(Call<List<ProfileRequest>> call, Response<List<ProfileRequest>> response) {
                    if (swipeRefreshLayout != null && swipeRefreshLayout.isRefreshing()) {
                        swipeRefreshLayout.setRefreshing(false);
                    }
                    if (response.isSuccessful() && response.body() != null && !response.body().isEmpty()) {
                        ProfileRequest profile = response.body().get(0);
                        profileName.setText(profile.getUsername());
                        profileEmail.setText(profile.getEmail());

                        if (profile.getAvatarUrl() != null && !profile.getAvatarUrl().isEmpty()) {
                            if (!isAdded() || getActivity() == null) return;
                            String bustCacheUrl = profile.getAvatarUrl() + "?t=" + System.currentTimeMillis();
                            Glide.with(ProfileFragment.this)
                                    .load(bustCacheUrl)
                                    .transition(DrawableTransitionOptions.withCrossFade(300))
                                    .apply(RequestOptions.circleCropTransform())
                                    .placeholder(R.drawable.default_avatar)
                                    .error(R.drawable.default_avatar)
                                    .into(profileImage);
                        } else {
                            if (!isAdded() || getActivity() == null) return;
                            Glide.with(ProfileFragment.this)
                                    .load(R.drawable.default_avatar)
                                    .apply(RequestOptions.circleCropTransform())
                                    .into(profileImage);
                        }
                    } else if (response.code() == 401 && refreshToken != null) {
                        Log.w("PROFILE", "Access token expired. Attempting refresh...");
                        refreshTokenAndRetry(refreshToken, userId);
                    } else {
                        setFallbackProfile();
                        logError(response);
                    }
                    profileLoadingSpinner.setVisibility(View.GONE);
                }

                @Override
                public void onFailure(Call<List<ProfileRequest>> call, Throwable t) {
                    profileLoadingSpinner.setVisibility(View.GONE);
                    if (swipeRefreshLayout != null && swipeRefreshLayout.isRefreshing()) {
                        swipeRefreshLayout.setRefreshing(false);
                    }
                    setFallbackProfile();
                    Log.e("PROFILE", "Error fetching profile", t);
                }
            });
        }
    }

    private void refreshTokenAndRetry(String refreshToken, String userId) {
        SupabaseAuthService refreshService = RetrofitClient.getAuthService(null);
        JsonObject body = new JsonObject();
        body.addProperty("refresh_token", refreshToken);

        refreshService.refreshSession(body).enqueue(new Callback<LoginResponse>() {
            @Override
            public void onResponse(Call<LoginResponse> call, Response<LoginResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    String newToken = response.body().access_token;
                    String newRefresh = response.body().refresh_token;

                    SharedPreferences.Editor editor = requireActivity().getSharedPreferences("AUTH", MODE_PRIVATE).edit();
                    editor.putString("TOKEN", newToken);
                    if (newRefresh != null) editor.putString("REFRESH_TOKEN", newRefresh);
                    editor.apply();

                    fetchUserProfile(); // Retry after refreshing
                } else {
                    Log.e("PROFILE", "Failed to refresh token: " + response.code());
                    logoutAndRedirect();
                }
            }

            @Override
            public void onFailure(Call<LoginResponse> call, Throwable t) {
                Log.e("PROFILE", "Refresh token failed: " + t.getMessage());
                logoutAndRedirect();
            }
        });
    }

    private void setFallbackProfile() {
        profileName.setText("User");
        profileEmail.setText("your@email.com");
        Glide.with(ProfileFragment.this)
                .load(R.drawable.default_avatar)
                .apply(RequestOptions.circleCropTransform())
                .into(profileImage);
    }

    private void logError(Response<?> response) {
        try {
            String errorBody = response.errorBody() != null ? response.errorBody().string() : "No error body";
            Log.e("PROFILE", "Profile fetch failed: Code=" + response.code() + ", Body=" + errorBody);
        } catch (IOException e) {
            Log.e("PROFILE", "Error reading errorBody: " + e.getMessage());
        }
    }

    private void logoutAndRedirect() {
        requireActivity().getSharedPreferences("AUTH", MODE_PRIVATE).edit().clear().apply();
        Intent intent = new Intent(requireContext(), LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == EDIT_PROFILE_REQUEST_CODE && resultCode == RESULT_OK && data != null) {
            boolean avatarChanged = data.getBooleanExtra("avatarChanged", false);
            boolean usernameChanged = data.getBooleanExtra("usernameChanged", false);

            if (avatarChanged || usernameChanged) {
                fetchUserProfile(); // Only reload if something changed
            } else {
                Log.d("PROFILE", "No profile data changed, skipping reload");
            }
        }
    }

}
