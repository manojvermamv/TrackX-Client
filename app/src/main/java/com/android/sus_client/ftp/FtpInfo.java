package com.android.sus_client.ftp;

import android.text.TextUtils;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;

public class FtpInfo implements Serializable {

    private int id;
    private String host;
    private String port;
    private String username;
    private String password;
    private String homeDir;


    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public String getPort() {
        return port;
    }

    public void setPort(String port) {
        this.port = port;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getHomeDir() {
        return homeDir;
    }

    public void setHomeDir(String homeDir) {
        this.homeDir = homeDir;
    }


    /**
     * Helper methods
     **/
    public int getPortInt() {
        return TextUtils.isEmpty(getPort()) ? 21 : Integer.parseInt(getPort());
    }

    public static JSONObject toJSON(FtpInfo info) {
        JSONObject json = new JSONObject();
        try {
            json.put("id", info.id);
            json.put("host", info.host);
            json.put("port", info.port);
            json.put("username", info.username);
            json.put("password", info.password);
            json.put("homeDir", info.homeDir);
        } catch (JSONException ignored) {
        }
        return json;
    }

    public static FtpInfo fromJSON(JSONObject json) {
        FtpInfo config = new FtpInfo();
        config.id = json.optInt("id");
        config.host = json.optString("host");
        config.port = json.optString("port");
        config.username = json.optString("username");
        config.password = json.optString("password");
        config.homeDir = json.optString("homeDir");
        return config;
    }

}