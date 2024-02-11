package com.android.sus_client.control;

import android.os.Build;

import org.json.JSONException;
import org.json.JSONObject;

public class ScreenSharingConfig {

    public boolean KEY_USE_DEFAULT = false;
    public String KEY_SERVER_URL = "";
    public String KEY_SECRET = "";
    public String KEY_DEVICE_NAME = Build.MANUFACTURER + " " + Build.MODEL;
    public boolean KEY_TRANSLATE_AUDIO = false;
    public int KEY_BITRATE = Const.DEFAULT_BITRATE;
    public int KEY_FRAME_RATE = Const.DEFAULT_FRAME_RATE;
    public String KEY_TEST_DST_IP = "";
    public float KEY_VIDEO_SCALE = 0;
    public boolean KEY_NOTIFY_SHARING = true;
    public int KEY_IDLE_TIMEOUT = Const.DEFAULT_IDLE_TIMEOUT;
    public int KEY_PING_TIMEOUT = Const.DEFAULT_PING_TIMEOUT;

    public ScreenSharingConfig() {

    }

    public static JSONObject toJSON(ScreenSharingConfig info) {
        JSONObject json = new JSONObject();
        try {
            json.put("KEY_SERVER_URL", info.KEY_SERVER_URL);
        } catch (JSONException ignored) {
        }
        return json;
    }

    private static ScreenSharingConfig fromJSON(JSONObject json) {
        final ScreenSharingConfig config = new ScreenSharingConfig();
        config.KEY_SERVER_URL = json.optString("KEY_SERVER_URL");
        return config;
    }

}