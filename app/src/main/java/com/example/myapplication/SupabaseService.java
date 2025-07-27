package com.example.myapplication;

import java.util.List;
import java.util.Map;

import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Path;
import retrofit2.http.QueryMap;

public interface SupabaseService {

    // Upload an image to Supabase Storage
    @PUT("/storage/v1/object/{bucket}/{path}")
    Call<ResponseBody> uploadImage(
            @Header("apikey") String apiKey,
            @Header("Authorization") String auth,
            @Path("bucket") String bucket,
            @Path("path") String path,
            @Body RequestBody file
    );

    // Upload user avatar to 'public/avatars' bucket
    @PUT("/storage/v1/object/avatars/{filename}")
    Call<ResponseBody> uploadAvatar(
            @Path("filename") String filename,
            @Body RequestBody file
    );


    // Get list of images from the database (updated to include folderName)

    @GET("/rest/v1/images?select=*")
    Call<List<ImageData>> getImages(
            @Header("apikey") String apiKey,
            @Header("Authorization") String authToken,
            @QueryMap Map<String, String> filters
    );



    // List objects in a storage bucket (corrected to use POST with body)
    @POST("/storage/v1/object/list/images")
    Call<List<StorageObject>> listObjects(
            @Header("apikey") String apiKey,
            @Header("Authorization") String auth,
            @Body RequestBody requestBody
    );

    // Insert image URL and filename into the database
    @POST("/rest/v1/images")
    Call<ResponseBody> insertImageUrl(
            @Header("apikey") String apiKey,
            @Header("Authorization") String auth,
            @Body ImageData imageData
    );
}
