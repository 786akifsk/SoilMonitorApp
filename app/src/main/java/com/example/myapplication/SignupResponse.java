package com.example.myapplication;

import com.google.gson.annotations.SerializedName;

public class SignupResponse {
    @SerializedName("access_token")
    public String access_token;

    @SerializedName("user")
    public User user;

    public static class User {
        @SerializedName("id")
        public String id;

        @SerializedName("email")
        public String email;
    }
}
