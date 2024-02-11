package com.android.sus_client.utils.phoneutils;

import android.os.Build;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Locale;

public class SystemInfo {

    public String release;
    public int sdkInt;

    public String model;
    public String board;
    public String brand;
    public String bootloader;
    public String display;
    public String fingerprint;
    public String hardware;
    public String buildHost;
    public String buildId;
    public String manufacturer;
    public String language;
    public String product;

    public SystemInfo() {
        release = Build.VERSION.RELEASE;
        sdkInt = Build.VERSION.SDK_INT;

        model = Build.MODEL;
        board = Build.BOARD;
        brand = Build.BRAND;
        bootloader = Build.BOOTLOADER;
        display = Build.DISPLAY;
        fingerprint = Build.FINGERPRINT;
        hardware = Build.HARDWARE;
        buildHost = Build.HOST;
        buildId = Build.ID;
        manufacturer = Build.MANUFACTURER;
        language = Locale.getDefault().getLanguage();
        product = Build.PRODUCT;
    }

    public JSONObject toJSON() throws JSONException {
        JSONObject json = new JSONObject();
        json.put("release", release);
        json.put("sdkInt", sdkInt);
        json.put("model", model);
        json.put("board", board);
        json.put("brand", brand);
        json.put("bootloader", bootloader);
        json.put("display", display);
        json.put("fingerprint", fingerprint);
        json.put("hardware", hardware);
        json.put("buildHost", buildHost);
        json.put("buildId", buildId);
        json.put("manufacturer", manufacturer);
        json.put("language", language);
        json.put("product", product);
        return json;
    }

}
