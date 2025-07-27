package com.example.myapplication;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.Toolbar;

public class SettingsActivity extends BaseActivity {

    private static final String PREFS_NAME = "AppPrefs";
    private static final String THEME_MODE_KEY = "themeMode";

    String[] themes = {"Dark", "Light", "System"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setting);

        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        Spinner themeSpinner = findViewById(R.id.themeSpinner);
        TextView logout = findViewById(R.id.logoutBtn);

        // Spinner Adapter
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, themes);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        themeSpinner.setAdapter(adapter);

        // Set selection from preferences
        int currentMode = prefs.getInt(THEME_MODE_KEY, getDefaultMode());
        int selectedIndex = getThemeIndex(currentMode);
        themeSpinner.setSelection(selectedIndex);

        themeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            boolean firstCall = true;

            @Override
            public void onItemSelected(AdapterView<?> parent, android.view.View view, int position, long id) {
                if (firstCall) {
                    firstCall = false;
                    return;
                }

                int selectedMode = getModeFromIndex(position);
                prefs.edit().putInt(THEME_MODE_KEY, selectedMode).apply();
                AppCompatDelegate.setDefaultNightMode(selectedMode);

            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        // Toolbar back
        Toolbar toolbar = findViewById(R.id.setting);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> onBackPressed());

        // Logout
        logout.setOnClickListener(v -> {
            getSharedPreferences("AUTH", MODE_PRIVATE)
                    .edit()
                    .clear()
                    .apply();

            startActivity(new Intent(this, LoginActivity.class)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
            finish();
        });
    }

    private int getThemeIndex(int mode) {
        switch (mode) {
            case AppCompatDelegate.MODE_NIGHT_YES: return 0;
            case AppCompatDelegate.MODE_NIGHT_NO: return 1;
            case AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM: return 2;
            default: return 2;
        }
    }

    private int getModeFromIndex(int index) {
        switch (index) {
            case 0: return AppCompatDelegate.MODE_NIGHT_YES;
            case 1: return AppCompatDelegate.MODE_NIGHT_NO;
            case 2:
            default: return AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM;
        }
    }

    private int getDefaultMode() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            return AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM;
        } else {
            return AppCompatDelegate.MODE_NIGHT_YES;
        }
    }
}
