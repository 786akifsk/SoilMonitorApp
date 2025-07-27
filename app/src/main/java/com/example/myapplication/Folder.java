package com.example.myapplication;

import com.google.gson.annotations.SerializedName;
import java.util.ArrayList;
import java.util.List;

public class Folder {
    @SerializedName("foldername")
    private String foldername;
    private List<ImageData> images;

    public Folder(String foldername) {
        this.foldername = foldername;
        this.images = new ArrayList<>();
    }

    public String getFoldername() { return foldername; }
    public List<ImageData> getImages() { return images; }
    public void addImage(ImageData image) { images.add(image); }
}