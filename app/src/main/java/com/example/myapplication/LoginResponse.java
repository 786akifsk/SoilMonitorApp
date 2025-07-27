package com.example.myapplication;

import com.google.gson.annotations.SerializedName;
import java.util.Map;

public class LoginResponse {
    @SerializedName("access_token")
    public String access_token;

    @SerializedName("refresh_token")
    public String refresh_token;

    @SerializedName("user")
    public User user;

    public static class User {
        @SerializedName("id")      // 👈 Needed to fetch profile
        public String id;

        @SerializedName("email")
        public String email;

        @SerializedName("user_metadata")
        public Map<String, Object> user_metadata;
    }
}
