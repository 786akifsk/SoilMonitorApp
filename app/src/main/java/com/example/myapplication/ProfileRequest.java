package com.example.myapplication;


public class ProfileRequest {
    private String id;
    private String username;
    private String email;
    private String avatar_url;

    public ProfileRequest(String id, String email, String username) {
        this.id = id;
        this.email = email;
        this.username = username;
    }

    public String getId() { return id; }
    public String getUsername() { return username; }
    public String getEmail() { return email; }

    public String getAvatarUrl() { return avatar_url; }
    public void setAvatarUrl(String avatarUrl) { this.avatar_url = avatarUrl; }
}

