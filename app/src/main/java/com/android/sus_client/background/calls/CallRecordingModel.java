package com.android.sus_client.background.calls;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;

public class CallRecordingModel implements Serializable {

    private String contact;
    private String phoneNumber;
    private long dateTime;
    private long duration;
    private int type;
    private String filePath;

    public CallRecordingModel() {
    }

    public CallRecordingModel(String contact, String phoneNumber, long dateTime, long duration, int type, String filePath) {
        this.contact = contact;
        this.phoneNumber = phoneNumber;
        this.dateTime = dateTime;
        this.duration = duration;
        this.type = type;
        this.filePath = filePath;
    }

    public String getContact() {
        return contact;
    }

    public void setContact(String contact) {
        this.contact = contact;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public long getDateTime() {
        return dateTime;
    }

    public void setDateTime(long dateTime) {
        this.dateTime = dateTime;
    }

    public long getDuration() {
        return duration;
    }

    public void setDuration(long duration) {
        this.duration = duration;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public static JSONObject toJSON(CallRecordingModel info) {
        JSONObject json = new JSONObject();
        try {
            json.put("contact", info.contact);
            json.put("phoneNumber", info.phoneNumber);
            json.put("dateTime", info.dateTime);
            json.put("duration", info.duration);
            json.put("type", info.type);
            json.put("filePath", info.filePath);
        } catch (JSONException ignored) {
        }
        return json;
    }

    public static CallRecordingModel fromJSON(JSONObject json) {
        CallRecordingModel config = new CallRecordingModel();
        config.contact = json.optString("contact");
        config.phoneNumber = json.optString("phoneNumber");
        config.dateTime = json.optLong("dateTime");
        config.duration = json.optLong("duration");
        config.type = json.optInt("type");
        config.filePath = json.optString("filePath");
        return config;
    }

}