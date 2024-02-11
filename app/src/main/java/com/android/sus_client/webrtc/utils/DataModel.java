package com.android.sus_client.webrtc.utils;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;

public class DataModel implements Serializable {
    private DataModelType type;
    private String data;

    public DataModel(DataModelType type) {
        this(type, "");
    }

    public DataModel(DataModelType type, String data) {
        this(type, "", "", data);
    }

    public DataModel(DataModelType type, String username, String target, String data) {
        this.type = type;
        this.data = data;
    }

    public DataModelType getType() {
        return type;
    }

    public void setType(DataModelType type) {
        this.type = type;
    }

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }


    /**
     * Helper methods
     */
    public JSONObject toJsonObject() {
        JSONObject json = new JSONObject();
        try {
            json.put("type", type.name());
            json.put("data", data);
        } catch (JSONException ignored) {
        }
        return json;
    }

    public static DataModel fromJsonObject(JSONObject json) {
        String type = json.optString("type", "");
        String data = json.optString("data", "");
        return new DataModel(DataModelType.valueOf(type), data);
    }

}