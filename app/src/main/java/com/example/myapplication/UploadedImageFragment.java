package com.example.myapplication;

import static android.content.Context.MODE_PRIVATE;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.gson.JsonObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import okhttp3.OkHttpClient;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class UploadedImageFragment extends Fragment implements MainActivity.RefreshableFragment {

    private static final String SUPABASE_URL = BuildConfig.SUPABASE_URL;
    private static final String SUPABASE_KEY = BuildConfig.SUPABASE_ANON_KEY;
    private static final String TAG = "UploadedImageFragment";

    private RecyclerView recyclerView;
    private SwipeRefreshLayout swipeRefreshLayout;
    private ProgressBar progressBar;
    private TextView noImagesTextView;
    private FolderAdapter folderAdapter;
    private List<Folder> folderList;
    private ExecutorService executorService;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        folderList = new ArrayList<>();
        folderAdapter = new FolderAdapter(requireContext(), folderList);
        executorService = Executors.newSingleThreadExecutor();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_recyclerview, container, false);

        recyclerView = view.findViewById(R.id.imageRecyclerView);
        swipeRefreshLayout = view.findViewById(R.id.swipeRefreshLayout);
        progressBar = view.findViewById(R.id.progressBar);
        noImagesTextView = view.findViewById(R.id.noImagesTextView);

        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerView.setAdapter(folderAdapter);

        if (swipeRefreshLayout != null) {
            swipeRefreshLayout.setOnRefreshListener(() -> {
                fetchImagesFromSupabase();
                swipeRefreshLayout.setRefreshing(false);
            });
        }

        fetchImagesFromSupabase();

        return view;
    }

    @Override
    public void refreshData() {
        if (isAdded()) {
            Log.d(TAG, "Refreshing data for UploadedImageFragment");
            fetchImagesFromSupabase();
        }
    }

    private void fetchImagesFromSupabase() {
        if (!isAdded()) return;

        SharedPreferences prefs = requireActivity().getSharedPreferences("AUTH", Context.MODE_PRIVATE);
        String userId = prefs.getString("UID", null);
        String jwt = prefs.getString("TOKEN", null);
        String refreshToken = prefs.getString("REFRESH_TOKEN", null);

        if (userId == null || jwt == null) {
            Log.e(TAG, "UID or TOKEN is null");
            return;
        }

        requireActivity().runOnUiThread(() -> {
            progressBar.setVisibility(View.VISIBLE);
            noImagesTextView.setVisibility(View.GONE);
            folderList.clear();
            folderAdapter.updateFolders(folderList);
        });

        OkHttpClient client = new OkHttpClient.Builder()
                .cache(null)
                .addInterceptor(chain -> chain.proceed(
                        chain.request().newBuilder().header("Cache-Control", "no-cache").build()
                ))
                .build();

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(SUPABASE_URL)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        SupabaseService service = retrofit.create(SupabaseService.class);

        Map<String, String> queryMap = new HashMap<>();
        queryMap.put("user_id", "eq." + userId);
        queryMap.put("select", "image_url,filename,foldername");

        Call<List<ImageData>> call = service.getImages(SUPABASE_KEY, "Bearer " + jwt, queryMap);

        call.enqueue(new Callback<List<ImageData>>() {
            @Override
            public void onResponse(@NonNull Call<List<ImageData>> call, @NonNull Response<List<ImageData>> response) {
                if (!isAdded()) return;

                requireActivity().runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    if (response.isSuccessful() && response.body() != null) {
                        folderList.clear();
                        Map<String, Folder> folderMap = new HashMap<>();
                        for (ImageData image : response.body()) {
                            String foldername = image.getFoldername() != null ? image.getFoldername() : "Uncategorized";
                            Folder folder = folderMap.get(foldername);
                            if (folder == null) {
                                folder = new Folder(foldername);
                                folderMap.put(foldername, folder);
                                folderList.add(folder);
                            }
                            folder.addImage(image);
                        }
                        folderAdapter.updateFolders(folderList);
                        noImagesTextView.setVisibility(folderList.isEmpty() ? View.VISIBLE : View.GONE);
                    } else if (response.code() == 401 && refreshToken != null) {
                        Log.w(TAG, "Token expired, refreshing...");
                        refreshTokenAndRetry(refreshToken, userId);
                    } else {
                        showError(response);
                    }
                });
            }

            @Override
            public void onFailure(@NonNull Call<List<ImageData>> call, @NonNull Throwable t) {
                if (!isAdded()) return;
                requireActivity().runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    noImagesTextView.setVisibility(View.VISIBLE);
                    Log.e(TAG, "Network failure: " + t.getMessage());
                    Toast.makeText(requireContext(), "Network error: " + t.getMessage(), Toast.LENGTH_LONG).show();
                });
            }
        });
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

                    fetchImagesFromSupabase(); // Retry after refreshing
                } else {
                    Log.e(TAG, "Token refresh failed: " + response.code());
                    logoutAndRedirect();
                }
            }

            @Override
            public void onFailure(Call<LoginResponse> call, Throwable t) {
                Log.e(TAG, "Refresh token failed: " + t.getMessage());
                logoutAndRedirect();
            }
        });
    }

    private void logoutAndRedirect() {
        requireActivity().getSharedPreferences("AUTH", MODE_PRIVATE).edit().clear().apply();
        Intent intent = new Intent(requireContext(), LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }


    private void showError(Response<List<ImageData>> response) {
        String errorMsg = "Error " + response.code() + ": " + response.message();
        try {
            if (response.errorBody() != null) {
                errorMsg += " - " + response.errorBody().string();
            }
        } catch (IOException e) {
            errorMsg += " - Error reading body";
        }
        Log.e(TAG, errorMsg);
        Toast.makeText(requireContext(), "Failed to fetch images: " + errorMsg, Toast.LENGTH_LONG).show();
        if (folderList.isEmpty()) noImagesTextView.setVisibility(View.VISIBLE);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        executorService.shutdown();
    }
}
