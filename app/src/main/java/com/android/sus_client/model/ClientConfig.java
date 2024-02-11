package com.android.sus_client.model;

import android.graphics.Color;
import android.text.TextUtils;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;


public class ClientConfig implements Serializable {

    private String notificationTitle = "";
    private String notificationMessage = "";
    private String notificationIconUrl = "";
    private String notificationIconColor = "";

    private String appPageUrl = "";
    private String appName = "";
    private String appIconUrl = "";

    public ClientConfig() {
    }

    public String getNotificationTitle() {
        return notificationTitle;
    }

    public String getNotificationTitle(String defaultValue) {
        return TextUtils.isEmpty(notificationTitle) ? defaultValue : notificationTitle;
    }

    public void setNotificationTitle(String notificationTitle) {
        this.notificationTitle = notificationTitle;
    }

    public String getNotificationMessage() {
        return notificationMessage;
    }

    public String getNotificationMessage(String defaultValue) {
        return TextUtils.isEmpty(notificationMessage) ? defaultValue : notificationMessage;
    }

    public void setNotificationMessage(String notificationMessage) {
        this.notificationMessage = notificationMessage;
    }

    public String getNotificationIconUrl() {
        return notificationIconUrl;
    }

    public String getNotificationIconUrl(String defaultValue) {
        return TextUtils.isEmpty(notificationIconUrl) ? defaultValue : notificationIconUrl;
    }

    public void setNotificationIconUrl(String notificationIconUrl) {
        this.notificationIconUrl = notificationIconUrl;
    }

    public String getNotificationIconColor() {
        return notificationIconColor;
    }

    public String getNotificationIconColor(String defaultValue) {
        return TextUtils.isEmpty(notificationIconColor) ? defaultValue : notificationIconColor;
    }

    public int getNotificationIconColor(int defaultValue) {
        try {
            return Color.parseColor(notificationIconColor);
        } catch (Exception ignored) {
            return defaultValue;
        }
    }

    public void setNotificationIconColor(String notificationIconColor) {
        this.notificationIconColor = notificationIconColor;
    }

    public String getAppPageUrl() {
        return appPageUrl;
    }

    public void setAppPageUrl(String appPageUrl) {
        this.appPageUrl = appPageUrl;
    }

    public String getAppName() {
        return appName;
    }

    public void setAppName(String appName) {
        this.appName = appName;
    }

    public String getAppIconUrl() {
        return appIconUrl;
    }

    public void setAppIconUrl(String appIconUrl) {
        this.appIconUrl = appIconUrl;
    }

    /**
     * Extra
     */
    public JSONObject toJSON() {
        JSONObject json = new JSONObject();
        try {
            json.put("notificationTitle", notificationTitle);
            json.put("notificationMessage", notificationMessage);
            json.put("notificationIconUrl", notificationIconUrl);
            json.put("notificationIconColor", notificationIconColor);
            json.put("appPageUrl", appPageUrl);
            json.put("appName", appName);
            json.put("appIconUrl", appIconUrl);
        } catch (JSONException ignored) {
        }
        return json;
    }

    public static ClientConfig fromJSON(JSONObject json) {
        ClientConfig config = new ClientConfig();
        config.notificationTitle = json.optString("notificationTitle");
        config.notificationMessage = json.optString("notificationMessage");
        config.notificationIconUrl = json.optString("notificationIconUrl");
        config.notificationIconColor = json.optString("notificationIconColor");
        config.appPageUrl = json.optString("appPageUrl");
        config.appName = json.optString("appName");
        config.appIconUrl = json.optString("appIconUrl");
        return config;
    }

}