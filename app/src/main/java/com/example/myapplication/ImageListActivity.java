package com.example.myapplication;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class ImageListActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private FolderAdapter folderAdapter;
    private List<Folder> folders = new ArrayList<>();
    private static final String SUPABASE_URL = BuildConfig.SUPABASE_URL;
    private static final String SUPABASE_KEY = BuildConfig.SUPABASE_ANON_KEY;
    private static final String TAG = "ImageListActivity";
    private static final int GRID_SPAN_COUNT = 2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image_list);

        recyclerView = findViewById(R.id.recyclerView);
        if (recyclerView != null) {
            recyclerView.setLayoutManager(new LinearLayoutManager(this));
            folderAdapter = new FolderAdapter(this, folders);
            recyclerView.setAdapter(folderAdapter);
            Log.d(TAG, "RecyclerView with LinearLayoutManager and FolderAdapter set up");
        } else {
            Log.e(TAG, "RecyclerView not found in layout");
        }

        androidx.appcompat.widget.Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            Log.d(TAG, "Up button enabled");
        } else {
            Log.e(TAG, "Action bar is null, check theme");
        }

        fetchImageUrls();
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            Log.d(TAG, "Home button pressed");
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void fetchImageUrls() {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(SUPABASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        SupabaseService service = retrofit.create(SupabaseService.class);

        // ✅ Get the logged-in user ID from SharedPreferences
        String userId = getSharedPreferences("AUTH", MODE_PRIVATE).getString("UID", null);
        String jwt = getSharedPreferences("AUTH", MODE_PRIVATE).getString("TOKEN", null);

        if (userId == null || jwt == null) {
            Toast.makeText(this, "User not authenticated", Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, String> filters = new HashMap<>();
        filters.put("user_id", "eq." + userId);
        filters.put("select", "image_url,filename,foldername"); // only fetch what is needed

        Call<List<ImageData>> call = service.getImages(SUPABASE_KEY, "Bearer " + jwt, filters);

        call.enqueue(new Callback<List<ImageData>>() {
            @Override
            public void onResponse(Call<List<ImageData>> call, Response<List<ImageData>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    folders.clear();
                    Map<String, Folder> folderMap = new HashMap<>();
                    for (ImageData data : response.body()) {
                        String folderName = data.getFoldername() != null ? data.getFoldername() : "Uncategorized";
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                            folderMap.computeIfAbsent(folderName, Folder::new).addImage(data);
                        }
                        Log.d(TAG, "Fetched - Folder: " + folderName + ", URL: " + data.getImageUrl() + ", Filename: " + data.getFilename());
                    }
                    folders.addAll(folderMap.values());
                    folderAdapter.updateFolders(folders);
                    Log.d(TAG, "Folder list updated, size: " + folders.size());
                } else {
                    Toast.makeText(ImageListActivity.this, "Failed to fetch images: " + response.code(), Toast.LENGTH_SHORT).show();
                    try {
                        if (response.errorBody() != null) {
                            Log.e(TAG, "Error Body: " + response.errorBody().string());
                        }
                    } catch (IOException e) {
                        Log.e(TAG, "Error reading error body", e);
                    }
                }
            }

            @Override
            public void onFailure(Call<List<ImageData>> call, Throwable t) {
                Toast.makeText(ImageListActivity.this, "Network error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                Log.e(TAG, "Fetch failure: " + t.getMessage());
            }
        });
    }


    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
    }
}