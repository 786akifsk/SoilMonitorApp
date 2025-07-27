package com.example.myapplication;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

import java.util.concurrent.TimeUnit;

public class RetrofitClient {
    private static final int CONNECT_TIMEOUT = 30; // seconds
    private static final int READ_TIMEOUT = 60;   // seconds
    private static final int WRITE_TIMEOUT = 60;  // seconds

    private static OkHttpClient createAuthenticatedClient(String token) {
        HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
        logging.setLevel(HttpLoggingInterceptor.Level.BODY); // Log body for debugging

        return new OkHttpClient.Builder()
                .connectTimeout(CONNECT_TIMEOUT, TimeUnit.SECONDS)
                .readTimeout(READ_TIMEOUT, TimeUnit.SECONDS)
                .writeTimeout(WRITE_TIMEOUT, TimeUnit.SECONDS)
                .addInterceptor(logging)
                .addInterceptor(chain -> {
                    okhttp3.Request original = chain.request();
                    okhttp3.Request.Builder builder = original.newBuilder()
                            .addHeader("apikey", BuildConfig.SUPABASE_ANON_KEY)
                            .addHeader("Content-Type", "application/json");

                    if (token != null && !token.isEmpty()) {
                        builder.addHeader("Authorization", "Bearer " + token);
                    }

                    return chain.proceed(builder.build());
                })
                .build();
    }

    public static SupabaseAuthService getAuthService() {
        return getAuthService(null); // Default to no authentication
    }

    public static SupabaseAuthService getAuthService(String token) {
        OkHttpClient client = createAuthenticatedClient(token);
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(BuildConfig.SUPABASE_URL.endsWith("/") ? BuildConfig.SUPABASE_URL : BuildConfig.SUPABASE_URL + "/")
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        return retrofit.create(SupabaseAuthService.class);
    }

    public static SupabaseService getStorageService(String token) {
        OkHttpClient client = createAuthenticatedClient(token);
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(BuildConfig.SUPABASE_URL.endsWith("/") ? BuildConfig.SUPABASE_URL : BuildConfig.SUPABASE_URL + "/")
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        return retrofit.create(SupabaseService.class);
    }
}