package com.example.myapplication;

import static androidx.core.content.ContentProviderCompat.requireContext;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import android.view.MenuItem;
import android.app.AlertDialog;
import android.widget.Toast;


import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class ImageDetailsActivity extends AppCompatActivity {

    private static final String TAG = "ImageDetailActivity";
    public static final String BASE_URL = BuildConfig.SUPABASE_URL;
    public static final String ANON_KEY = BuildConfig.SUPABASE_ANON_KEY;

    private String imageUrl;
    private String filename;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image_details);

        imageUrl = getIntent().getStringExtra("image_url");
        filename = getIntent().getStringExtra("filename");
        if (imageUrl == null || filename == null) {
            Log.e(TAG, "Missing intent extras - URL: " + imageUrl + ", Filename: " + filename);
            finish();
            return;
        }

        fetchSoilDetails(filename);

        Log.d(TAG, "Displayed - URL: " + imageUrl + ", Filename: " + filename);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> onBackPressed());

        ImageView detailImageView = findViewById(R.id.detailImageView);

        Glide.with(this)
                .load(imageUrl)
                .apply(RequestOptions.bitmapTransform(new RoundedCorners(16)))
                .diskCacheStrategy(DiskCacheStrategy.NONE)
                .skipMemoryCache(true)
                .into(detailImageView);

        // New logic: Set initial filename display
        TextView filenameTextView = findViewById(R.id.soilFilenameTextView);
        filenameTextView.setText("Filename: " + filename);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.image_details_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.action_delete) {
            CustomDialog.showDialog(
                    ImageDetailsActivity.this,
                    "Delete Confirmation",
                    "Are you sure you want to delete this?",
                    "Yes, Delete",
                    "Abort",
                    R.raw.error,
                    R.color.error,    // custom confirm button color
                    R.color.cancelBtn,   // custom cancel button color
                    new CustomDialog.DialogCallback() {
                        @Override public void onConfirm() { deleteImageFromSupabase();}
                        @Override public void onCancel() { /* handle cancel */ }
                    }
            );

            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void deleteImageFromSupabase() {
        String folder = getIntent().getStringExtra("folder_name"); // e.g., "Alluvial"
        String filename = getIntent().getStringExtra("filename"); // from earlier
        String userId = getSharedPreferences("AUTH", Context.MODE_PRIVATE).getString("UID", null);
        String jwt = getSharedPreferences("AUTH", MODE_PRIVATE).getString("TOKEN", null); // auth token

        if (folder == null || filename == null || userId == null || jwt == null) {
            Toast.makeText(this, "Missing folder, filename, or auth info", Toast.LENGTH_SHORT).show();
            Log.d(TAG, folder+"  "+ filename+"  "+userId+"  "+jwt);
            return;
        }

        String path = userId + "/" + folder + "/" + filename;
        String storageUrl = BASE_URL + "/storage/v1/object/images/" + path;

        OkHttpClient client = new OkHttpClient();

        Request storageRequest = new Request.Builder()
                .url(storageUrl)
                .delete()
                .addHeader("Authorization", "Bearer " + jwt)
                .addHeader("apikey", ANON_KEY)
                .build();

        client.newCall(storageRequest).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                runOnUiThread(() ->
                        Toast.makeText(ImageDetailsActivity.this, "Image delete failed: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                );
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (response.isSuccessful()) {
                    deleteSoilTableEntry(filename); // delete metadata/
                    deleteImagesTableEntry(filename);
                } else {
                    runOnUiThread(() ->
                            Toast.makeText(ImageDetailsActivity.this, "Image delete failed: " + response.code(), Toast.LENGTH_SHORT).show()
                    );
                }
            }
        });
    }



    private void deleteSoilTableEntry(String filename) {
        String jwt = getSharedPreferences("AUTH", MODE_PRIVATE).getString("TOKEN", null);
        String tableUrl = BASE_URL + "/rest/v1/soil_properties?filename=eq." + filename;

        Log.d("DELETE_SOIL", "Request URL: " + tableUrl);

        Request request = new Request.Builder()
                .url(tableUrl)
                .delete()
                .addHeader("Authorization", "Bearer " + jwt)
                .addHeader("apikey", ANON_KEY)
                .build();

        new OkHttpClient().newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                runOnUiThread(() -> Toast.makeText(ImageDetailsActivity.this,
                        "Delete failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) {
                runOnUiThread(() -> {
                    if (response.isSuccessful()) {
                        Log.d("DELETE_SOIL", "Success: " + response.code());
                        Toast.makeText(ImageDetailsActivity.this, "Deleted successfully", Toast.LENGTH_SHORT).show();
                        finish();
                    } else {
                        Toast.makeText(ImageDetailsActivity.this, "Delete failed: " + response.code(), Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
    }






    private void deleteImagesTableEntry(String filename) {
        String tableUrl = BASE_URL + "/rest/v1/images?filename=eq." + filename;
        String jwt = getSharedPreferences("AUTH", MODE_PRIVATE).getString("TOKEN", null);
        Log.d("DELETE_SOIL", "Request URL: " + tableUrl);

        Request tableRequest = new Request.Builder()
                .url(tableUrl)
                .delete()
                .addHeader("Authorization", "Bearer " + jwt)
                .addHeader("apikey", ANON_KEY)
                .build();

        new OkHttpClient().newCall(tableRequest).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                runOnUiThread(() ->
                        Toast.makeText(ImageDetailsActivity.this, "Image metadata delete failed: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                );
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) {
                runOnUiThread(() -> {
                    if (response.isSuccessful()) {
                        Log.d("Delete", "Image metadata removed");
                    } else {
                        Toast.makeText(ImageDetailsActivity.this, "Image metadata delete failed: " + response.code(), Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
    }




    private void fetchSoilDetails(String imageName) {
        String url = BASE_URL+"/rest/v1/soil_properties?filename=eq." + imageName;
        String jwt = getSharedPreferences("AUTH", MODE_PRIVATE).getString("TOKEN", null);
        Log.d(TAG, "Fetching soil details for URL: " + url);

        Request request = new Request.Builder()
                .url(url)
                .addHeader("apikey", ANON_KEY)
                .addHeader("Authorization", "Bearer "+jwt)
                .build();

        OkHttpClient client = new OkHttpClient();
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "Failed to fetch soil details: " + e.getMessage());
                e.printStackTrace();
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    final String responseData = response.body().string();

                    runOnUiThread(() -> {
                        try {
                            JSONArray array = new JSONArray(responseData);
                            if (array.length() > 0) {
                                JSONObject soilInfo = array.getJSONObject(0);

                                String type = soilInfo.optString("type", "N/A");
                                String color = soilInfo.optString("color", "N/A");
                                String texture = soilInfo.optString("texture", "N/A");
                                String rich_in = soilInfo.optString("rich_in", "N/A");
                                String poor_in = soilInfo.optString("poor_in", "N/A");
                                String water_retention = soilInfo.optString("water_retention", "N/A");
                                String suitable_crops = soilInfo.optString("suitable_crops", "N/A");
                                String region = soilInfo.optString("region", "N/A");
                                String avgTemp = soilInfo.optString("avg_temperature", "N/A");
                                String avgHumidity = soilInfo.optString("avg_humidity", "N/A");
                                String avgMoisture = soilInfo.optString("avg_moisture", "N/A");
                                String avgNitrogen = soilInfo.optString("avg_nitrogen", "N/A");
                                String avgPhosphorus = soilInfo.optString("avg_phosphorus", "N/A");
                                String avgPotassium = soilInfo.optString("avg_potassium", "N/A");



                                // UI components
                                TextView filenameTextView = findViewById(R.id.soilFilenameTextView);
                                TextView soilTypeTextView = findViewById(R.id.soilSoilTypeTextView);
                                TextView soilColorTextView = findViewById(R.id.soilColorTextView);
                                TextView soilTextureTextView = findViewById(R.id.soilTextureTextView);
                                TextView soilRichInTextView = findViewById(R.id.soilRichInTextView);
                                TextView soilPoorInTextView = findViewById(R.id.soilPoorInTextView);
                                TextView soilWaterRetentionTextView = findViewById(R.id.soilWaterRetentionTextView);
                                TextView soilSuitableCropsTextView = findViewById(R.id.soilSuitableCropsTextView);
                                TextView soilRegionTextView = findViewById(R.id.soilRegionTextView);
                                TextView avgTempTextView = findViewById(R.id.avgTemperatureTextView);
                                TextView avgHumidityTextView = findViewById(R.id.avgHumidityTextView);
                                TextView avgMoistureTextView = findViewById(R.id.avgMoistureTextView);
                                TextView avgNitrogenTextView = findViewById(R.id.avgNitrogenTextView);
                                TextView avgPhosphorusTextView = findViewById(R.id.avgPhosphorusTextView);
                                TextView avgPotassiumTextView = findViewById(R.id.avgPotassiumTextView);
                                TextView rensorHeaderTextView = findViewById(R.id.rensorHeader);


                                // New logic: Update filename and other details
                                filenameTextView.setText("Filename:  " + filename); // Ensure filename is set
                                soilTypeTextView.setText("Soil Type:  " + type);
                                soilColorTextView.setText("Color:  " +color);
                                soilTextureTextView.setText("Texture:  " + texture);
                                soilRichInTextView.setText("Rich In:  " + rich_in);
                                soilPoorInTextView.setText("Poor In:  " + poor_in);
                                soilWaterRetentionTextView.setText("Water Retention:  " + water_retention);
                                soilSuitableCropsTextView.setText("Suitable Crops:  " + suitable_crops);
                                soilRegionTextView.setText("Region:  " + region);
                                rensorHeaderTextView.setText("Sensor Properties");
                                avgTempTextView.setText("Temperature: " + avgTemp +" ℃");
                                avgHumidityTextView.setText("Humidity: " + avgHumidity+" %");
                                avgMoistureTextView.setText("Moisture: " + avgMoisture+" %");
                                avgNitrogenTextView.setText("Nitrogen: " + avgNitrogen+" mg/kg");
                                avgPhosphorusTextView.setText("Phosphorus: " + avgPhosphorus+" mg/kg");
                                avgPotassiumTextView.setText("Potassium: " + avgPotassium+" mg/kg");


                            } else {
                                Log.w(TAG, "No soil details found for filename: " + imageName);
                            }
                        } catch (JSONException e) {
                            Log.e(TAG, "JSON parsing error: " + e.getMessage());
                            e.printStackTrace();
                        }
                    });
                } else {
                    Log.e(TAG, "Unsuccessful response: " + response.code() + " - " + response.message());
                }
            }
        });
    }
}