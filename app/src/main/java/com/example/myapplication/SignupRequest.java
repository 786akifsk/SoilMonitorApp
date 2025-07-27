package com.example.myapplication;

import com.google.gson.annotations.SerializedName;

import java.util.HashMap;
import java.util.Map;

public class SignupRequest {
    @SerializedName("email")
    private String email;

    @SerializedName("password")
    private String password;

    @SerializedName("data")
    private Map<String, Object> data;

    public SignupRequest(String email, String password) {
        this.email = email;
        this.password = password;
        this.data = new HashMap<>();

        // Default username from email before '@'
        String username = email.split("@")[0];
        this.data.put("username", username);
    }
}

