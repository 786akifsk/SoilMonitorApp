package com.example.myapplication;

import static androidx.core.content.ContentProviderCompat.requireContext;

import android.Manifest;
import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.RenderEffect;
import android.graphics.Shader;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import com.nafis.bottomnavigation.NafisBottomNavigation;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.json.JSONArray;
import org.json.JSONObject;
import android.os.Handler;
import android.os.Looper;
import android.graphics.Color;





public class MainActivity extends BaseActivity {

    private static final String SUPABASE_URL = BuildConfig.SUPABASE_URL;
    private static final String SUPABASE_ROLE_KEY = BuildConfig.SUPABASE_ROLE_KEY;
    private static final String TAG = "MainActivity";
    private static final String PREFS_NAME = "AppPrefs";
    private static final String THEME_KEY = "isDarkTheme";
    private static String currentUserEmail = null;
    private NafisBottomNavigation bottomNavigation;
    private Toolbar toolbar;
    private int currentTabId = 1; // Default: Home, ID 1
    private FragmentManager fragmentManager;
    private static final int PERMISSION_REQUEST_CODE = 100;
    private Map<String, Fragment> fragmentMap;
    private ExecutorService executorService;
    private SharedPreferences sharedPreferences;

    public interface RefreshableFragment {
        void refreshData();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Load theme preference before setting content view, default to dark mode
        sharedPreferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        boolean isDarkTheme = sharedPreferences.getBoolean(THEME_KEY, true); // Default to true (dark mode)
        AppCompatDelegate.setDefaultNightMode(
                isDarkTheme ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO
        );
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        String token = getSharedPreferences("AUTH", MODE_PRIVATE).getString("TOKEN", null);
        if (token == null) {
            Intent intent = new Intent(MainActivity.this, LoginActivity.class);
            startActivity(intent);
            finish(); // Close MainActivity
            return;
        }

        // Set system navigation bar color to match the app's background
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = getWindow();
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            int backgroundColor = isDarkTheme ? Color.parseColor("#121212") : Color.parseColor("#FAFAFA");
            window.setNavigationBarColor(backgroundColor);
        }

        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            Log.d(TAG, "Toolbar set up");
        } else {
            Log.e(TAG, "Action bar is null, check theme");
        }

        bottomNavigation = findViewById(R.id.bottomNavigation);
        View blurView = findViewById(R.id.blurView);


        // Initialize bottom navigation items
        bottomNavigation.add(new NafisBottomNavigation.Model(2, R.drawable.ic_view_image));
        bottomNavigation.add(new NafisBottomNavigation.Model(1, R.drawable.ic_home));
        bottomNavigation.add(new NafisBottomNavigation.Model(3, R.drawable.ic_account));

        // Restore the selected tab if the activity is recreated
        if (savedInstanceState != null) {
            currentTabId = savedInstanceState.getInt("currentTabId", 1);
            Log.d(TAG, "Restored currentTabId: " + currentTabId);
        }


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            applyRenderEffectBlur(blurView);
        }
        else {
            blurView.setBackgroundResource(R.drawable.shadow_gradient);
        }




        // Set the click listener before showing the tab to ensure proper initialization
        bottomNavigation.setOnClickMenuListener(model -> {
            if (model.getId() == currentTabId) {
                Log.d(TAG, "Tab " + model.getId() + " already selected, ignoring click");
                return null;
            }

            String title = "";
            String tag = "";
            Fragment selectedFragment;

            switch (model.getId()) {
                case 1:
                    tag = "HomeFragment";
                    title = "Soil Monitor";
                    selectedFragment = fragmentMap.get(tag);
                    if (selectedFragment == null) {
                        selectedFragment = new HomeFragment();
                        fragmentMap.put(tag, selectedFragment);
                        fragmentManager.beginTransaction()
                                .add(R.id.con, selectedFragment, tag)
                                .commit();
                    }
                    break;
                case 2:
                    tag = "RecycleView";
                    title = "Images";
                    selectedFragment = fragmentMap.get(tag);
                    if (selectedFragment == null) {
                        selectedFragment = new UploadedImageFragment();
                        fragmentMap.put(tag, selectedFragment);
                        fragmentManager.beginTransaction()
                                .add(R.id.con, selectedFragment, tag)
                                .commit();
                    }
                    if (selectedFragment instanceof RefreshableFragment) {
                        ((RefreshableFragment) selectedFragment).refreshData();
                    }
                    break;
                case 3:
                    tag = "ProfileFragment";
                    title = "Profile";
                    selectedFragment = fragmentMap.get(tag);
                    if (selectedFragment == null) {
                        selectedFragment = new ProfileFragment();
                        fragmentMap.put(tag, selectedFragment);
                        fragmentManager.beginTransaction()
                                .add(R.id.con, selectedFragment, tag)
                                .commit();
                    }
                    break;
                default:
                    return null;
            }

            FragmentTransaction transaction = fragmentManager.beginTransaction();
            for (Fragment fragment : fragmentMap.values()) {
                if (fragment != selectedFragment && fragment.isAdded()) {
                    transaction.hide(fragment);
                }
            }
            transaction.show(selectedFragment).commit();

            currentTabId = model.getId();
            if (getSupportActionBar() != null) {
                getSupportActionBar().setTitle(title);
            }

            Log.d(TAG, "Switched to tab " + currentTabId + " with title: " + title);
            return null;
        });

        // Show the current tab after setting the listener
        bottomNavigation.show(currentTabId, true);

        // Set the toolbar title based on the current tab
        if (getSupportActionBar() != null) {
            switch (currentTabId) {
                case 1:
                    getSupportActionBar().setTitle("Soil Monitor");
                    break;
                case 2:
                    getSupportActionBar().setTitle("Images");
                    break;
                case 3:
                    getSupportActionBar().setTitle("Profile");
                    break;
                default:
                    getSupportActionBar().setTitle("Soil Monitor");
            }
        }

        fragmentManager = getSupportFragmentManager();
        fragmentMap = new HashMap<>();
        executorService = Executors.newFixedThreadPool(2);

        if (savedInstanceState == null) {
            HomeFragment homeFragment = new HomeFragment();
            fragmentMap.put("HomeFragment", homeFragment);
            fragmentManager.beginTransaction()
                    .add(R.id.con, homeFragment, "HomeFragment")
                    .commit();
        } else {
            Fragment homeFragment = fragmentManager.findFragmentByTag("HomeFragment");
            Fragment recycleViewFragment = fragmentManager.findFragmentByTag("RecycleView");
            Fragment profileFragment = fragmentManager.findFragmentByTag("ProfileFragment");
            if (homeFragment != null) fragmentMap.put("HomeFragment", homeFragment);
            if (recycleViewFragment != null) fragmentMap.put("RecycleView", recycleViewFragment);
            if (profileFragment != null) fragmentMap.put("ProfileFragment", profileFragment);

            // Restore fragment visibility
            FragmentTransaction transaction = fragmentManager.beginTransaction();
            for (Fragment fragment : fragmentMap.values()) {
                if (fragment != null) {
                    if (fragment.getTag().equals(getFragmentTagForTab(currentTabId))) {
                        transaction.show(fragment);
                    } else {
                        transaction.hide(fragment);
                    }
                }
            }
            transaction.commit();
        }

        checkAndRequestPermissions();
    }



    public static String getCurrentUserEmail() {
        return currentUserEmail;
    }

    public static void setCurrentUserEmail(String email) {
        currentUserEmail = email;
    }


    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        Log.d(TAG, "Saving currentTabId: " + currentTabId);
        outState.putInt("currentTabId", currentTabId);
    }

//    private void applyLegacyBlur(View blurView) {
//        if (blurView != null && blurView.getParent() instanceof ViewGroup) {
//            // Delay until layout pass is complete
//            blurView.post(() -> {
//                Blurry.with(this)
//                        .radius(20) // blur radius
//                        .sampling(2) // downscale factor for performance
//                        .animate(500) // optional
//                        .onto((ViewGroup) blurView.getParent());
//            });
//        } else {
//            Log.e("BlurEffect", "View or parent is invalid");
//        }
//    }

    @TargetApi(Build.VERSION_CODES.S)
    private void applyRenderEffectBlur(View blurView) {
        RenderEffect blurEffect = RenderEffect.createBlurEffect(20f, 20f, Shader.TileMode.CLAMP);
        blurView.setRenderEffect(blurEffect);
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        executorService.shutdown();
        SupabaseUpdater.stopPolling();
    }

    @Override
    public void onBackPressed() {
        if (currentTabId != 1) {
            Fragment homeFragment = fragmentMap.get("HomeFragment");
            if (homeFragment != null) {
                FragmentTransaction transaction = fragmentManager.beginTransaction();
                for (Fragment fragment : fragmentMap.values()) {
                    if (fragment != homeFragment && fragment.isAdded()) {
                        transaction.hide(fragment);
                    }
                }
                transaction.show(homeFragment).commit();
                bottomNavigation.show(1, true);
                currentTabId = 1;
                if (getSupportActionBar() != null) {
                    getSupportActionBar().setTitle("Soil Monitor");
                }
            }
        } else {
            super.onBackPressed();
        }
    }

    private void checkAndRequestPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            String[] permissionsToRequest;

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                permissionsToRequest = new String[]{
                        Manifest.permission.CAMERA,
                        Manifest.permission.READ_MEDIA_IMAGES
                };
            } else {
                permissionsToRequest = new String[]{
                        Manifest.permission.CAMERA,
                        Manifest.permission.READ_EXTERNAL_STORAGE
                };
            }

            boolean permissionsNeeded = false;
            for (String permission : permissionsToRequest) {
                if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                    permissionsNeeded = true;
                    Log.d(TAG, "Permission needed: " + permission);
                    break;
                }
            }

            if (permissionsNeeded) {
                ActivityCompat.requestPermissions(this, permissionsToRequest, PERMISSION_REQUEST_CODE);
                Log.d(TAG, "Requesting permissions: " + String.join(", ", permissionsToRequest));
            } else {
                Log.d(TAG, "All permissions already granted");
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            boolean allGranted = true;
            for (int i = 0; i < grantResults.length; i++) {
                if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false;
                    Log.w(TAG, "Permission denied: " + permissions[i]);
                    break;
                }
            }
            if (allGranted) {
                Toast.makeText(this, "Permissions granted", Toast.LENGTH_SHORT).show();
                Log.d(TAG, "Permissions granted");
            } else {
                Toast.makeText(this, "Permissions denied. Some features may not work.", Toast.LENGTH_LONG).show();
                Log.w(TAG, "Some permissions denied");
            }
        }
    }


    private String getFragmentTagForTab(int tabId) {
        switch (tabId) {
            case 1:
                return "HomeFragment";
            case 2:
                return "RecycleView";
            case 3:
                return "ProfileFragment";
            default:
                return "HomeFragment";
        }
    }


    public static String capitalizeFirstLetter(String input) {
        if (input == null || input.isEmpty()) {
            return input;
        }
        return input.substring(0, 1).toUpperCase() + input.substring(1).toLowerCase();
    }

    public static class SupabaseUpdater {
        private static final String TAG = "SupabaseUpdater";
        private static final String TABLE_URL = SUPABASE_URL + "/rest/v1/current_filename?id=eq.1";
        private static final String SERVICE_API_KEY = SUPABASE_ROLE_KEY;
        private static final int POLL_INTERVAL_MS = 1000;
        private static final Handler mainHandler = new Handler(Looper.getMainLooper());
        private static volatile boolean isPolling = false;
        private static final ExecutorService executorService = Executors.newFixedThreadPool(2);

        public static void updateFilename(String newFilename) {
            executorService.execute(() -> {
                try {
                    URL url = new URL(TABLE_URL);
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("PATCH");
                    conn.setRequestProperty("apikey", SERVICE_API_KEY);
                    conn.setRequestProperty("Authorization", "Bearer " + SERVICE_API_KEY);
                    conn.setRequestProperty("Content-Type", "application/json");
                    conn.setDoOutput(true);

                    JSONObject jsonParam = new JSONObject();
                    jsonParam.put("filename", newFilename);

                    OutputStream os = conn.getOutputStream();
                    os.write(jsonParam.toString().getBytes());
                    os.flush();
                    os.close();

                    int responseCode = conn.getResponseCode();
                    Log.d(TAG, "Response code: " + responseCode);
                    conn.disconnect();
                } catch (Exception e) {
                    Log.e(TAG, "Update failed: ", e);
                }
            });
        }

        public static void sendTriggerToSupabase(Context context, TextView sensorDisplay) {
            // Show confirmation dialog before proceeding
            CustomDialog.showDialog(
                    context,
                    "Sensor Check",
                    "Ensure the sensor is connected before proceeding. Do you want to continue?",
                    "Yes",
                    "No",
                    R.raw.info,
                    new CustomDialog.DialogCallback() {
                        @Override
                        public void onConfirm() {

                            // Now run the trigger logic
                            executorService.execute(() -> {
                                try {
                                    JSONObject currentData = getCurrentFilenameData();
                                    boolean currentValue = currentData.getBoolean("trigger_value");
                                    boolean newValue = !currentValue;

                                    updateTriggerValue(newValue);

                                    if (newValue) {
                                        mainHandler.post(() -> Toast.makeText(context, "Trigger set to TRUE. Reading sensor...", Toast.LENGTH_SHORT).show());
                                        Log.d("SupabaseTrigger", "Trigger set to true, starting success status polling");
                                        startPolling(context, sensorDisplay);
                                    } else {
                                        mainHandler.post(() -> {
                                            Toast.makeText(context, "Trigger set to FALSE", Toast.LENGTH_SHORT).show();
                                            if (sensorDisplay != null) {
                                                sensorDisplay.setText("Sensor Reading: N/A");
                                            }
                                        });
                                        stopPolling();
                                    }
                                } catch (Exception e) {
                                    Log.e("SupabaseTrigger", "Exception occurred: " + e.getMessage());
                                    mainHandler.post(() -> {
                                        Toast.makeText(context, "Error toggling trigger: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                        if (sensorDisplay != null) {
                                            sensorDisplay.setText("Sensor Reading: Error");
                                        }
                                    });
                                    stopPolling();
                                }
                            });
                        }

                        @Override
                        public void onCancel() {
                            Toast.makeText(context, "Sensor reading cancelled", Toast.LENGTH_SHORT).show();
                        }
                    }
            );
        }


        private static JSONObject getCurrentFilenameData() throws Exception {
            URL url = new URL(TABLE_URL + "&select=trigger_value,filename,success");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("apikey", SERVICE_API_KEY);
            conn.setRequestProperty("Authorization", "Bearer " + SERVICE_API_KEY);
            conn.setRequestProperty("Content-Type", "application/json");

            int responseCode = conn.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                StringBuilder response = new StringBuilder();
                try (BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
                    String line;
                    while ((line = br.readLine()) != null) {
                        response.append(line);
                    }
                }
                JSONArray jsonArray = new JSONArray(response.toString());
                if (jsonArray.length() > 0) {
                    JSONObject data = jsonArray.getJSONObject(0);
                    Log.d("SupabaseTrigger", "Current data: " + data.toString() + ", Response: " + response);
                    conn.disconnect();
                    return data;
                }
                conn.disconnect();
                throw new Exception("No data found in current_filename: " + response);
            } else {
                StringBuilder errorResponse = new StringBuilder();
                try (BufferedReader br = new BufferedReader(new InputStreamReader(conn.getErrorStream()))) {
                    String line;
                    while ((line = br.readLine()) != null) {
                        errorResponse.append(line);
                    }
                }
                Log.e("SupabaseTrigger", "Failed to get current_filename. Code: " + responseCode + ", Error: " + errorResponse);
                conn.disconnect();
                throw new Exception("Failed to get current_filename: " + errorResponse);
            }
        }

        private static void updateTriggerValue(boolean value) throws Exception {
            URL url = new URL(TABLE_URL);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("PATCH");
            conn.setRequestProperty("apikey", SERVICE_API_KEY);
            conn.setRequestProperty("Authorization", "Bearer " + SERVICE_API_KEY);
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setDoOutput(true);

            String jsonBody = "{\"trigger_value\": " + value + "}";
            try (OutputStream os = conn.getOutputStream()) {
                byte[] input = jsonBody.getBytes("utf-8");
                os.write(input, 0, input.length);
            }

            int responseCode = conn.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK || responseCode == HttpURLConnection.HTTP_NO_CONTENT) {
                Log.d("SupabaseTrigger", "Trigger set to " + value);
            } else {
                StringBuilder errorResponse = new StringBuilder();
                try (BufferedReader br = new BufferedReader(new InputStreamReader(conn.getErrorStream()))) {
                    String line;
                    while ((line = br.readLine()) != null) {
                        errorResponse.append(line);
                    }
                }
                Log.e("SupabaseTrigger", "Failed to set trigger. Code: " + responseCode + ", Error: " + errorResponse);
                conn.disconnect();
                throw new Exception("Failed to update trigger_value: " + errorResponse);
            }
            conn.disconnect();
        }

        private static void startPolling(Context context, TextView sensorDisplay) {
            if (isPolling) return;
            isPolling = true;

            Runnable poller = new Runnable() {
                @Override
                public void run() {
                    if (!isPolling) return;

                    executorService.execute(() -> {
                        try {
                            JSONObject currentData = getCurrentFilenameData();
                            boolean triggerValue = currentData.getBoolean("trigger_value");
                            boolean success = currentData.getBoolean("success");

                            if (!triggerValue) {
                                Log.d("SupabaseTrigger", "Trigger is false, stopping polling");
                                mainHandler.post(() -> {
                                    Toast.makeText(context, "Trigger set to FALSE", Toast.LENGTH_SHORT).show();
                                    if (sensorDisplay != null) {
                                        sensorDisplay.setText("Sensor Reading: N/A");
                                    }
                                });
                                stopPolling();
                                return;
                            }

                            mainHandler.post(() -> {
                                if (sensorDisplay != null) {
                                    sensorDisplay.setText(success ? "Successful" : "Sensor taking reading");
                                }
                            });

                            if (isPolling) {
                                mainHandler.postDelayed(this, POLL_INTERVAL_MS);
                            }
                        } catch (Exception e) {
                            Log.e("SupabaseTrigger", "Polling error: " + e.getMessage());
                            mainHandler.post(() -> {
                                Toast.makeText(context, "Polling error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                if (sensorDisplay != null) {
                                    sensorDisplay.setText("Sensor Reading: Error");
                                }
                            });
                            stopPolling();
                        }
                    });
                }
            };
            mainHandler.post(poller);
        }

        public static void stopPolling() {
            isPolling = false;
            Log.d("SupabaseTrigger", "Polling stopped");
        }
    }
}