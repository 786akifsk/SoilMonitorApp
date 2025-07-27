package com.example.myapplication;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;

@SuppressLint("CustomSplashScreen")
public class SplashActivity extends BaseActivity {
    private static final int SPLASH_TIME_OUT = 2000; // 2 seconds

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);
        ImageView splashLogo = findViewById(R.id.splashLogo);

        int nightModeFlags =
                getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;

        if (nightModeFlags == Configuration.UI_MODE_NIGHT_YES) {
            splashLogo.setImageResource(R.drawable.logo_dark);
        } else {
            splashLogo.setImageResource(R.drawable.logo_light);
        }


        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            // 🔐 Check if the user is logged in
            String token = getSharedPreferences("AUTH", MODE_PRIVATE).getString("TOKEN", null);

            Intent intent;
            if (token != null) {
                // ✅ Already logged in → Go to MainActivity
                intent = new Intent(SplashActivity.this, MainActivity.class);
            } else {
                // ❌ Not logged in → Go to LoginActivity
                intent = new Intent(SplashActivity.this, LoginActivity.class);
            }

            startActivity(intent);
            finish(); // Close Splash screen

        }, SPLASH_TIME_OUT);
    }
}