package com.example.myapplication;

import com.google.gson.annotations.SerializedName;

public class ImageData {

    @SerializedName("image_url")
    private String imageUrl;

    @SerializedName("filename")
    private String filename;

    @SerializedName("foldername")
    private String foldername;

    @SerializedName("user_id")
    private String userId;

    // Constructor
    public ImageData(String imageUrl, String filename, String foldername, String userId) {
        this.imageUrl = imageUrl;
        this.filename = filename;
        this.foldername = foldername;
        this.userId = userId;
    }

    // Getters
    public String getImageUrl() { return imageUrl; }
    public String getFilename() { return filename; }
    public String getFoldername() { return foldername; }
    public String getUserId() { return userId; }

    // Setters (optional, depending on use)
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
    public void setFilename(String filename) { this.filename = filename; }
    public void setFoldername(String foldername) { this.foldername = foldername; }
    public void setUserId(String userId) { this.userId = userId; }
}
