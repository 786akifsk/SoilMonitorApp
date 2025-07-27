package com.example.myapplication;

import static android.content.Context.MODE_PRIVATE;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

public class SoilInfoManager {

    private Context context;
    public static final String BASE_URL = BuildConfig.SUPABASE_URL;
    public static final String ANON_KEY = BuildConfig.SUPABASE_ANON_KEY;
    public SoilInfoManager(Context context) {
        this.context = context;
    }

    public Map<String, Map<String, String>> getSoilData() {
        Map<String, Map<String, String>> soilData = new HashMap<>();

        Map<String, String> Alluvial = new HashMap<>();
        Alluvial.put("color", "Light grey to sandy brown");
        Alluvial.put("texture", "Silty to loamy");
        Alluvial.put("rich_in", "Potash, phosphoric acid, lime");
        Alluvial.put("poor_in", "Nitrogen");
        Alluvial.put("water_retention", "Moderate to high");
        Alluvial.put("suitable_crops", "Rice, wheat, sugarcane, jute");
        Alluvial.put("region", "Indo-Gangetic plain");


        Map<String, String> Black = new HashMap<>();
        Black.put("color", "Black");
        Black.put("texture", "Clayey");
        Black.put("rich_in", "Calcium carbonate, magnesium, potash");
        Black.put("poor_in", "Nitrogen, phosphorus");
        Black.put("water_retention", "High");
        Black.put("suitable_crops", "Cotton, soybean, pulses, millets");
        Black.put("region", "Maharashtra, MP, Gujarat, Andhra Pradesh");


        Map<String, String> Cinder = new HashMap<>();
        Cinder.put("color", "Dark grey to Black");
        Cinder.put("texture", "Porous and loose");
        Cinder.put("rich_in", "Minerals from volcanic rocks");
        Cinder.put("poor_in", "Organic matter");
        Cinder.put("water_retention", "Low");
        Cinder.put("suitable_crops", "Not suitable without conditioning");
        Cinder.put("region", "Volcanic regions");

        Map<String, String> Laterite = new HashMap<>();
        Laterite.put("color", "Reddish-brown");
        Laterite.put("texture", "Porous and clayey");
        Laterite.put("rich_in", "Iron, aluminum");
        Laterite.put("poor_in", "Nitrogen, phosphate");
        Laterite.put("water_retention", "Medium");
        Laterite.put("suitable_crops", "Tea, coffee, cashew, coconut");
        Laterite.put("region", "Western Ghats, Odisha, West Bengal");


        Map<String, String> Peat = new HashMap<>();
        Peat.put("color", "Dark brown to Black");
        Peat.put("texture", "Spongy, organic-rich");
        Peat.put("rich_in", "Humus and organic matter");
        Peat.put("poor_in", "Mineral content");
        Peat.put("water_retention", "Very high");
        Peat.put("suitable_crops", "Vegetables, rice (after drainage)");
        Peat.put("region", "Kottayam and Alappuzha (Kerala)");

        Map<String, String> Yellow = new HashMap<>();
        Yellow.put("color", "Yellow");
        Yellow.put("texture", "Sandy to loamy");
        Yellow.put("rich_in", "Iron oxide");
        Yellow.put("poor_in", "Nitrogen, phosphorus");
        Yellow.put("water_retention", "Moderate");
        Yellow.put("suitable_crops", "Paddy, pulses, oilseeds");
        Yellow.put("region", "Eastern India, Odisha, Assam");


        Map<String, String> Clay = new HashMap<>();
        Clay.put("color", "Dark brown to reddish brown");
        Clay.put("texture", "Sticky, fine particles");
        Clay.put("rich_in", "Calcium, potassium");
        Clay.put("poor_in", "Drainage");
        Clay.put("water_retention", "Very high");
        Clay.put("suitable_crops", "Rice, broccoli, cabbage");
        Clay.put("region", "River basins, lowlands");


        Map<String, String> Red = new HashMap<>();
        Red.put("color", "Reddish");
        Red.put("texture", "Sandy to loamy");
        Red.put("rich_in", "Iron");
        Red.put("poor_in", "Nitrogen, phosphorus, humus");
        Red.put("water_retention", "Moderate");
        Red.put("suitable_crops", "Millet, tobacco, groundnut");
        Red.put("region", "Tamil Nadu, Karnataka, Chhattisgarh");



        soilData.put("Clay", Clay);
        soilData.put("Alluvial", Alluvial);
        soilData.put("Black", Black);
        soilData.put("Cinder", Cinder);
        soilData.put("Laterite", Laterite);
        soilData.put("Peat", Peat);
        soilData.put("Yellow", Yellow);
        soilData.put("Red", Red);



        return soilData;
    }

    public void storeInSupabase(String type, String color, String texture, String rich_in, String poor_in, String water_retention, String suitable_crops, String region , String filename) {
        String jwt = context.getSharedPreferences("AUTH", MODE_PRIVATE).getString("TOKEN", null);
        String user_id = context.getSharedPreferences("AUTH", MODE_PRIVATE).getString("UID", null);

        if (jwt == null || user_id == null) {
            Log.e("Supabase", "Missing token or UID");
            return;
        }

        new Thread(() -> {
            try {
                URL url = new URL(BASE_URL + "/rest/v1/soil_properties");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("apikey", ANON_KEY);
                conn.setRequestProperty("Authorization", "Bearer " + jwt);
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setDoOutput(true);

                JSONObject jsonParam = new JSONObject();
                jsonParam.put("user_id", user_id);
                jsonParam.put("type", type);
                jsonParam.put("color", color);
                jsonParam.put("texture", texture);
                jsonParam.put("rich_in", rich_in);
                jsonParam.put("poor_in", poor_in);
                jsonParam.put("water_retention", water_retention);
                jsonParam.put("suitable_crops", suitable_crops);
                jsonParam.put("region", region);
                jsonParam.put("filename", filename);

                OutputStream os = conn.getOutputStream();
                os.write(jsonParam.toString().getBytes());
                os.flush();
                os.close();

                int responseCode = conn.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_CREATED || responseCode == HttpURLConnection.HTTP_OK) {
                    Log.d("Supabase", "Inserted successfully");
                } else {
                    Log.e("Supabase", "Insert failed: " + responseCode);
                    InputStream errorStream = conn.getErrorStream();
                    if (errorStream != null) {
                        BufferedReader reader = new BufferedReader(new InputStreamReader(errorStream));
                        String line;
                        StringBuilder errorOutput = new StringBuilder();
                        while ((line = reader.readLine()) != null) {
                            errorOutput.append(line);
                        }
                        Log.e("Supabase", "Error response: " + errorOutput);
                    }
                }

                conn.disconnect();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }


}
