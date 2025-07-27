package com.example.myapplication;

import static android.content.Context.MODE_PRIVATE;

import static com.example.myapplication.MainActivity.setCurrentUserEmail;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.google.gson.JsonObject;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class HomeFragment extends Fragment {

    private static final String SUPABASE_URL = BuildConfig.SUPABASE_URL;
    private static final String SUPABASE_KEY = BuildConfig.SUPABASE_ANON_KEY;
    private static final String TAG = "BlankFragment2";
    private static final String BUCKET_NAME = "images";
    private static final String DEFAULT_EXTENSION = ".jpg";
    private static final String KEY_IMAGE_URI = "selected_image_uri";
    private static final String KEY_SENSOR_VISIBLE = "sensor_visible";
    private static final String KEY_SENSOR_TEXT = "sensor_text";
    private static final int MAX_IMAGE_DIMENSION = 1920;

    private ImageView imageView;
    private Bitmap selectedImage;
    private Uri selectedImageUri;
    private TextView sensorReading;
    private Button btnTrigger;
    private ConstraintLayout progressContainer;
    private SoilInfoManager soilManager;

    private ActivityResultLauncher<Intent> cameraLauncher;
    private ActivityResultLauncher<Intent> galleryLauncher;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        soilManager = new SoilInfoManager(requireContext());
        String savedEmail = requireActivity().getSharedPreferences("AUTH", Context.MODE_PRIVATE).getString("EMAIL", null);
        if (savedEmail != null) {
            setCurrentUserEmail(savedEmail);
        }

        if (savedInstanceState != null) {
            selectedImageUri = savedInstanceState.getParcelable(KEY_IMAGE_URI);
        }

        cameraLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            if (result.getResultCode() == getActivity().RESULT_OK && result.getData() != null) {
                selectedImage = (Bitmap) result.getData().getExtras().get("data");
                selectedImageUri = null;
                if (selectedImage != null) {
                    selectedImage = resizeBitmap(selectedImage, MAX_IMAGE_DIMENSION);
                    imageView.setImageBitmap(selectedImage);
                    Log.d(TAG, "Camera image selected and resized: " + selectedImage.getWidth() + "x" + selectedImage.getHeight());
                } else {
                    Log.e(TAG, "Failed to get camera image");
                    Toast.makeText(requireContext(), "Failed to load camera image", Toast.LENGTH_SHORT).show();
                }
            }
        });

        galleryLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            if (result.getResultCode() == getActivity().RESULT_OK && result.getData() != null) {
                selectedImageUri = result.getData().getData();
                if (selectedImageUri != null) {
                    try {
                        selectedImage = loadBitmapFromUri(selectedImageUri);
                        imageView.setImageBitmap(selectedImage);
                        Log.d(TAG, "Gallery image selected and resized: " + selectedImageUri + ", " +
                                selectedImage.getWidth() + "x" + selectedImage.getHeight());
                    } catch (IOException e) {
                        Log.e(TAG, "Error loading gallery image: " + e.getMessage());
                        Toast.makeText(requireContext(), "Error loading gallery image", Toast.LENGTH_SHORT).show();
                        selectedImageUri = null;
                        selectedImage = null;
                    }
                } else {
                    Log.e(TAG, "Gallery URI is null");
                    Toast.makeText(requireContext(), "Failed to load gallery image", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        imageView = view.findViewById(R.id.imageView);
        Button btnCamera = view.findViewById(R.id.btnCamera);
        Button btnGallery = view.findViewById(R.id.btnGallery);
        Button btnSave = view.findViewById(R.id.btnSave);
        sensorReading = view.findViewById(R.id.sensorReading);
        btnTrigger = view.findViewById(R.id.btnTrigger);

        progressContainer = view.findViewById(R.id.progressContainer);

        // Restore visibility and sensor text if available
        if (savedInstanceState != null) {
            boolean isSensorVisible = savedInstanceState.getBoolean(KEY_SENSOR_VISIBLE, false);
            String sensorText = savedInstanceState.getString(KEY_SENSOR_TEXT, "Sensor Reading: N/A");
            if (isSensorVisible) {
                sensorReading.setVisibility(View.VISIBLE);
                btnTrigger.setVisibility(View.VISIBLE);
            } else {
                sensorReading.setVisibility(View.GONE);
                btnTrigger.setVisibility(View.GONE);
            }
            sensorReading.setText(sensorText);
        }

        if (selectedImageUri != null && selectedImage == null) {
            try {
                selectedImage = loadBitmapFromUri(selectedImageUri);
                imageView.setImageBitmap(selectedImage);
                Log.d(TAG, "Restored gallery image from URI: " + selectedImageUri);
            } catch (IOException e) {
                Log.e(TAG, "Error restoring gallery image: " + e.getMessage());
                selectedImageUri = null;
                selectedImage = null;
            }
        } else if (selectedImage != null) {
            imageView.setImageBitmap(selectedImage);
            Log.d(TAG, "Restored camera image");
        }

        btnCamera.setOnClickListener(v -> openCamera());
        btnGallery.setOnClickListener(v -> openGallery());
        btnSave.setOnClickListener(v -> saveImage());
        btnTrigger.setOnClickListener(v -> MainActivity.SupabaseUpdater.sendTriggerToSupabase(requireContext(), sensorReading));

        return view;
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelable(KEY_IMAGE_URI, selectedImageUri);
        outState.putBoolean(KEY_SENSOR_VISIBLE, sensorReading.getVisibility() == View.VISIBLE);
        outState.putString(KEY_SENSOR_TEXT, sensorReading.getText().toString());
    }

    private void openCamera() {
        sensorReading.setVisibility(View.GONE);
        btnTrigger.setVisibility(View.GONE);
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            if (intent.resolveActivity(requireContext().getPackageManager()) != null) {
                cameraLauncher.launch(intent);
            } else {
                Log.e(TAG, "Camera app not found");
                Toast.makeText(requireContext(), "Camera app not found", Toast.LENGTH_SHORT).show();
            }
        } else {
            requestPermissions(new String[]{Manifest.permission.CAMERA}, 100);
        }
    }

    private void openGallery() {
        sensorReading.setVisibility(View.GONE);
        btnTrigger.setVisibility(View.GONE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Android 13 and above uses READ_MEDIA_IMAGES
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_MEDIA_IMAGES)
                    != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.READ_MEDIA_IMAGES}, 101);
                return;
            }
        } else {
            // Android 12 and below uses READ_EXTERNAL_STORAGE
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 101);
                return;
            }
        }

        // Launch gallery intent
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        if (intent.resolveActivity(requireContext().getPackageManager()) != null) {
            galleryLauncher.launch(intent);
        } else {
            Log.e(TAG, "Gallery app not found");
            Toast.makeText(requireContext(), "Gallery app not found", Toast.LENGTH_SHORT).show();
        }
    }

    private void saveImage() {

        if (selectedImage != null) {
            // Proceed with upload
            if (progressContainer != null) {
                progressContainer.setVisibility(View.VISIBLE);
            }

            try {
                ImageClassifier classifier = new ImageClassifier(requireContext().getAssets(), "model.tflite", "labels.txt");
                String predictedLabel = classifier.classify(selectedImage);
                String[] parts = predictedLabel.split(" \\(");
                String className = parts[0].trim();
                String folderName = MainActivity.capitalizeFirstLetter(className);
                byte[] imageBytes = getBytes(selectedImage);
                Log.d(TAG, "Folder Name From Model is --> " + folderName + ", Image size: " + (imageBytes.length / 1024) + " KB");

                getNextSequenceNumber(folderName, imageBytes);
            } catch (Exception e) {
                Toast.makeText(requireContext(), "Model load/classify error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                if (progressContainer != null) progressContainer.setVisibility(View.GONE);
            }
        } else {
            Toast.makeText(requireContext(), "Please select an image first!", Toast.LENGTH_SHORT).show();
        }
    }

    private void getNextSequenceNumber(String folderName, byte[] imageBytes) {
        String userId = requireContext().getSharedPreferences("AUTH", Context.MODE_PRIVATE).getString("UID", null);
        String userFolder = userId + "/" + folderName + "/";

        OkHttpClient client = new OkHttpClient.Builder().build();
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(SUPABASE_URL)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        SupabaseService service = retrofit.create(SupabaseService.class);
        JSONObject jsonBody = new JSONObject();
        try {
            jsonBody.put("bucket", BUCKET_NAME);
            jsonBody.put("prefix", userFolder); // ✅ updated
        } catch (Exception e) {
            Log.e(TAG, "Error creating JSON body: " + e.getMessage());
        }

        RequestBody requestBody = RequestBody.create(MediaType.parse("application/json"), jsonBody.toString());

        Call<List<StorageObject>> call = service.listObjects(SUPABASE_KEY, "Bearer " + SUPABASE_KEY, requestBody);

        call.enqueue(new Callback<List<StorageObject>>() {
            @Override
            public void onResponse(@NonNull Call<List<StorageObject>> call, @NonNull Response<List<StorageObject>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    int maxSequence = 0;
                    for (StorageObject obj : response.body()) {
                        String name = obj.getName(); // ex: userId/folderName/folderName_3.jpg
                        String[] parts = name.split("/"); // Split path
                        String filename = parts[parts.length - 1]; // Get filename
                        if (filename.startsWith(folderName + "_") && filename.endsWith(DEFAULT_EXTENSION)) {
                            try {
                                String sequenceStr = filename.replace(folderName + "_", "").replace(DEFAULT_EXTENSION, "");
                                int sequence = Integer.parseInt(sequenceStr);
                                maxSequence = Math.max(maxSequence, sequence);
                            } catch (NumberFormatException e) {
                                Log.w(TAG, "Invalid sequence in filename: " + filename);
                            }
                        }
                    }
                    int nextSequence = maxSequence + 1;
                    String finalFilename = folderName + "_" + nextSequence + DEFAULT_EXTENSION;

                    Map<String, Map<String, String>> soilMap = soilManager.getSoilData();
                    if (soilMap.containsKey(folderName)) {
                        Map<String, String> details = soilMap.get(folderName);
                        soilManager.storeInSupabase(folderName, details.get("color"), details.get("texture"), details.get("rich_in"),
                                details.get("poor_in"), details.get("water_retention"), details.get("suitable_crops"), details.get("region"), finalFilename);
                    } else {
                        Toast.makeText(requireContext(), "Soil type not found", Toast.LENGTH_SHORT).show();
                    }

                    if (!Objects.equals(folderName, "Not soil")) {
                        MainActivity.SupabaseUpdater.updateFilename(finalFilename);
                    }

                    uploadToSupabase(finalFilename, folderName, imageBytes);
                } else {
                    handleError(folderName, imageBytes, response);
                }
            }

            @Override
            public void onFailure(@NonNull Call<List<StorageObject>> call, @NonNull Throwable t) {
                Log.e(TAG, "Failed to list objects: " + t.getMessage());
                Toast.makeText(requireContext(), "Network error: " + t.getMessage(), Toast.LENGTH_LONG).show();
                uploadToSupabase(folderName + "_1" + DEFAULT_EXTENSION, folderName, imageBytes);
            }

            private void handleError(String folderName, byte[] imageBytes, Response<List<StorageObject>> response) {
                if (response.code() == 404) {
                    uploadToSupabase(folderName + "_1" + DEFAULT_EXTENSION, folderName, imageBytes);
                } else {
                    try {
                        Log.w(TAG, "Failed to list objects - Code: " + response.code() + ", Message: " + response.message() +
                                ", Body: " + (response.errorBody() != null ? response.errorBody().string() : "No body"));
                    } catch (IOException e) {
                        Log.e(TAG, "Error reading response body", e);
                    }
                    Toast.makeText(requireContext(), "Failed to check existing files", Toast.LENGTH_LONG).show();
                    uploadToSupabase(folderName + "_1" + DEFAULT_EXTENSION, folderName, imageBytes);
                }
            }
        });
    }


    private void uploadToSupabase(String filename, String folderName, byte[] imageBytes) {
        String userId = requireContext().getSharedPreferences("AUTH", Context.MODE_PRIVATE).getString("UID", null);

        String fullPath = userId + "/" + folderName + "/" + filename;

        String timestamp = String.valueOf(System.currentTimeMillis());
        String imageUrlWithTimestamp = SUPABASE_URL + "/storage/v1/object/public/" + BUCKET_NAME + "/" + fullPath + "?timestamp=" + timestamp;

        RequestBody requestFile = RequestBody.create(MediaType.parse("image/jpeg"), imageBytes);

        OkHttpClient client = new OkHttpClient.Builder().build();
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(SUPABASE_URL)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        SupabaseService service = retrofit.create(SupabaseService.class);
        Call<ResponseBody> call = service.uploadImage(SUPABASE_KEY, "Bearer " + SUPABASE_KEY, BUCKET_NAME, fullPath, requestFile);

        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(@NonNull Call<ResponseBody> call, @NonNull Response<ResponseBody> response) {
                if (progressContainer != null) {
                    progressContainer.setVisibility(View.GONE);
                }
                if (response.isSuccessful()) {
                    Toast.makeText(requireContext(), "Uploaded! URL:\n" + imageUrlWithTimestamp, Toast.LENGTH_LONG).show();

                    if (!Objects.equals(folderName, "Not soil")) {
                        requireActivity().runOnUiThread(() -> {
                            sensorReading.setVisibility(View.VISIBLE);
                            sensorReading.setText("Sensor Reading: N/A");
                            btnTrigger.setVisibility(View.VISIBLE);
                            Toast.makeText(requireContext(), "Image uploaded. Click to read sensor!", Toast.LENGTH_SHORT).show();
                        });
                    }

                    saveUrlToDatabase(imageUrlWithTimestamp, filename, folderName);
                } else {
                    String errorMessage = "Upload failed: " + response.code() + " - " + response.message();
                    try {
                        String errorBody = response.errorBody() != null ? response.errorBody().string() : "";
                        Log.e(TAG, "Upload Error - Code: " + response.code() + ", Message: " + errorMessage + ", Body: " + errorBody);
                    } catch (IOException e) {
                        Log.e(TAG, "Error reading response body: " + e.getMessage());
                    }
                    Toast.makeText(requireContext(), errorMessage, Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<ResponseBody> call, @NonNull Throwable t) {
                if (progressContainer != null) {
                    progressContainer.setVisibility(View.GONE);
                }
                Log.e(TAG, "Upload Failure: " + t.getMessage());
                Toast.makeText(requireContext(), "Error: " + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }



    private void saveUrlToDatabase(String imageUrl, String filename, String folderName) {
        String uid = requireContext().getSharedPreferences("AUTH", Context.MODE_PRIVATE).getString("UID", null);
        ImageData imageData = new ImageData(imageUrl, filename, folderName,uid);
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(SUPABASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        SupabaseService service = retrofit.create(SupabaseService.class);
        String jwt = requireContext().getSharedPreferences("AUTH", Context.MODE_PRIVATE).getString("TOKEN", null);

        Call<ResponseBody> call = service.insertImageUrl(SUPABASE_KEY, "Bearer " + jwt, imageData);


        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(@NonNull Call<ResponseBody> call, @NonNull Response<ResponseBody> response) {
                if (progressContainer != null) {
                    progressContainer.setVisibility(View.GONE);
                }

                if (response.isSuccessful()) {
                    Log.i(TAG, "URL and filename saved: " + imageUrl + ", " + filename + ", " + folderName);
                    Toast.makeText(requireContext(), "Image saved to database", Toast.LENGTH_SHORT).show();
                } else if (response.code() == 401) {
                    Log.e(TAG, "Token expired. Attempting to refresh...");
                    SharedPreferences prefs = requireContext().getSharedPreferences("AUTH", Context.MODE_PRIVATE);
                    String refreshToken = prefs.getString("REFRESH_TOKEN", null);
                    String uid = prefs.getString("UID", null);

                    // 🔁 Retry after token refresh
                    refreshTokenAndRetry(refreshToken, () -> saveUrlToDatabase(imageUrl, filename, folderName));
                } else {
                    try {
                        if (response.errorBody() != null) {
                            Log.e(TAG, "Database Error - Code: " + response.code() + ", Body: " + response.errorBody().string());
                        }
                    } catch (IOException e) {
                        Log.e(TAG, "Error reading response body", e);
                    }
                    Toast.makeText(requireContext(), "Failed to save to database", Toast.LENGTH_SHORT).show();
                }
            }


            @Override
            public void onFailure(@NonNull Call<ResponseBody> call, @NonNull Throwable t) {
                if (progressContainer != null) {
                    progressContainer.setVisibility(View.GONE);
                }
                Log.e(TAG, "Database Failure: " + t.getMessage());
                Toast.makeText(requireContext(), "Database error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private Bitmap loadBitmapFromUri(Uri uri) throws IOException {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        try (InputStream inputStream = requireContext().getContentResolver().openInputStream(uri)) {
            BitmapFactory.decodeStream(inputStream, null, options);
        }

        options.inSampleSize = calculateInSampleSize(options, MAX_IMAGE_DIMENSION, MAX_IMAGE_DIMENSION);
        options.inJustDecodeBounds = false;

        try (InputStream inputStream = requireContext().getContentResolver().openInputStream(uri)) {
            Bitmap bitmap = BitmapFactory.decodeStream(inputStream, null, options);
            if (bitmap == null) {
                throw new IOException("Failed to decode Bitmap from URI");
            }
            return resizeBitmap(bitmap, MAX_IMAGE_DIMENSION);
        }
    }

    private void refreshTokenAndRetry(String refreshToken, Runnable onSuccess)
    {
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

                    onSuccess.run(); // 👈 Retry the original action
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

    private void logoutAndRedirect() {
        SharedPreferences.Editor editor = requireActivity().getSharedPreferences("AUTH", MODE_PRIVATE).edit();
        editor.clear();
        editor.apply();

        startActivity(new Intent(requireActivity(), LoginActivity.class));
        requireActivity().finish();
    }




    private int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {
            final int halfHeight = height / 2;
            final int halfWidth = width / 2;
            while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2;
            }
        }
        return inSampleSize;
    }

    private Bitmap resizeBitmap(Bitmap bitmap, int maxDimension) {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        if (width <= maxDimension && height <= maxDimension) {
            return bitmap;
        }

        float aspectRatio = (float) width / height;
        if (width > height) {
            width = maxDimension;
            height = Math.round(maxDimension / aspectRatio);
        } else {
            height = maxDimension;
            width = Math.round(maxDimension * aspectRatio);
        }
        return Bitmap.createScaledBitmap(bitmap, width, height, true);
    }

    private byte[] getBytes(Bitmap bitmap) {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 80, stream);
        return stream.toByteArray();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 100) { // Camera
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openCamera();
            } else {
                Toast.makeText(requireContext(), "Camera permission denied", Toast.LENGTH_SHORT).show();
            }
        } else if (requestCode == 101) { // Gallery
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openGallery();
            } else {
                Toast.makeText(requireContext(), "Storage permission denied", Toast.LENGTH_SHORT).show();
            }
        }
    }

}