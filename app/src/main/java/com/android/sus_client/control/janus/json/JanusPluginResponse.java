/*
 * Headwind Remote: Open Source Remote Access Software for Android
 * https://headwind-remote.com
 *
 * Copyright (C) 2022 headwind-remote.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.sus_client.control.janus.json;

import org.json.JSONObject;

import java.io.Serializable;

public class JanusPluginResponse implements Serializable {
    private String janus;
    private String session_id;
    private String transaction;
    private String sender;

    public String getJanus() {
        return janus;
    }

    public void setJanus(String janus) {
        this.janus = janus;
    }

    public String getSession_id() {
        return session_id;
    }

    public void setSession_id(String session_id) {
        this.session_id = session_id;
    }

    public String getTransaction() {
        return transaction;
    }

    public void setTransaction(String transaction) {
        this.transaction = transaction;
    }

    public String getSender() {
        return sender;
    }

    public void setSender(String sender) {
        this.sender = sender;
    }

    public JSONObject toJson() {
        JSONObject json = new JSONObject();
        try {
            json.put("janus", janus);
            json.put("session_id", session_id);
            json.put("transaction", transaction);
            json.put("sender", sender);
        } catch (Exception ignored) {
        }
        return json;
    }

    public static JanusPluginResponse fromJson(JSONObject json) {
        if (json == null) return null;
        JanusPluginResponse data = new JanusPluginResponse();
        data.janus = json.optString("janus");
        data.session_id = json.optString("session_id");
        data.transaction = json.optString("transaction");
        data.sender = json.optString("sender");
        return data;
    }

}