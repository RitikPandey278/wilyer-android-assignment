package com.example.wilyerandroidassignment.assets.data;

import android.content.Context;
import android.util.Log;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public class JsonUtils {
    public static String getJsonFromAssets(Context context) {
        String json;
        try {
            InputStream is = context.getAssets().open("data.json");
            int size = is.available();
            byte[] buffer = new byte[size];
            is.read(buffer);
            is.close();
            json = new String(buffer, StandardCharsets.UTF_8);
        } catch (Exception ex) {
            Log.e("JsonUtils", "Error reading asset: " + ex.getMessage());
            return null;
        }
        return json;
    }
}