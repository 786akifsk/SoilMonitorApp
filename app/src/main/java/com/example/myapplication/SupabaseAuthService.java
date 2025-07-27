package com.example.myapplication;

import com.google.gson.JsonObject;
import java.util.List;
import java.util.Map;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.PATCH;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.QueryMap;

public interface SupabaseAuthService {

    @POST("/auth/v1/signup")
    Call<SignupResponse> signUp(@Body SignupRequest request);

    @POST("/auth/v1/token?grant_type=password")
    Call<LoginResponse> login(@Body LoginRequest request);

    @POST("/auth/v1/recover")
    Call<Void> resetPassword(@Body PasswordResetRequest request);

    @PUT("/auth/v1/user")
    Call<Void> updatePassword(
            @Header("Authorization") String authorization,
            @Body JsonObject body
    );

    @POST("rest/v1/profiles")
    Call<Void> createProfile(@Body ProfileRequest profile);

    @GET("rest/v1/profiles")
    Call<List<ProfileRequest>> getProfile(@QueryMap Map<String, String> query);

    @PATCH("rest/v1/profiles")
    Call<Void> updateProfile(
            @Body JsonObject body,
            @QueryMap Map<String, String> query
    );

    @POST("/auth/v1/token?grant_type=refresh_token")
    Call<LoginResponse> refreshSession(@Body JsonObject body);

}
