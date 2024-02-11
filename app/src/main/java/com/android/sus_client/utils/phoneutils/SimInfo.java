package com.android.sus_client.utils.phoneutils;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;

public class SimInfo implements Serializable {

    public String sim = "";
    public String simOperator = "";
    public String simCountryIso = "";
    public String simSerialNumber = "";
    public String simLine1Number = "";
    public String imei = "";

    public SimInfo() {
    }

    public JSONObject toJSON() throws JSONException {
        JSONObject json = new JSONObject();
        json.put("sim", sim);
        json.put("simOperator", simOperator);
        json.put("simCountryIso", simCountryIso);
        json.put("simSerialNumber", simSerialNumber);
        json.put("simLine1Number", simLine1Number);
        json.put("imei", imei);
        return json;
    }

}