package com.android.sus_client.utils.phoneutils;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;

public class FileDirectoryInfo implements Serializable {

    public String filePath = "";
    public long lastModified = 0;
    public int items = 0;
    public String dirType = "";

    public FileDirectoryInfo() {
    }

    public JSONObject toJSON() throws JSONException {
        JSONObject json = new JSONObject();
        json.put("filePath", filePath);
        json.put("lastModified", lastModified);
        json.put("items", items);
        json.put("dirType", dirType);
        return json;
    }

}