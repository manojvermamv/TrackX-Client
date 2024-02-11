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

public class JanusPollResponse implements Serializable {

    public static class TextRoomData implements Serializable {
        private String textroom;
        private String result;

        public String getTextroom() {
            return textroom;
        }

        public void setTextroom(String textroom) {
            this.textroom = textroom;
        }

        public String getResult() {
            return result;
        }

        public void setResult(String result) {
            this.result = result;
        }

        public JSONObject toJson() {
            JSONObject json = new JSONObject();
            try {
                json.put("textroom", textroom);
                json.put("result", result);
            } catch (Exception ignored) {
            }
            return json;
        }

        public static TextRoomData fromJson(JSONObject json) {
            if (json == null) return null;
            TextRoomData data = new TextRoomData();
            data.textroom = json.optString("textroom");
            data.result = json.optString("result");
            return data;
        }

    }

    public static class PluginData implements Serializable {
        private String plugin;
        private TextRoomData data;

        public String getPlugin() {
            return plugin;
        }

        public void setPlugin(String plugin) {
            this.plugin = plugin;
        }

        // Not really good because different plugins may send different data JSON
        // But since poll responds only to textroom events, that is ok
        public TextRoomData getData() {
            return data;
        }

        public void setData(TextRoomData data) {
            this.data = data;
        }

        public JSONObject toJson() {
            JSONObject json = new JSONObject();
            try {
                json.put("plugin", plugin);
                json.put("data", data.toJson());
            } catch (Exception ignored) {
            }
            return json;
        }

        public static PluginData fromJson(JSONObject json) {
            if (json == null) return null;
            PluginData data = new PluginData();
            data.plugin = json.optString("plugin");
            if (json.has("data")) {
                data.data = TextRoomData.fromJson(json.optJSONObject("data"));
            }
            return data;
        }

    }

    private PluginData pluginData;
    private JanusPluginResponse janusPluginResponse;
    private Jsep jsep;

    public PluginData getPluginData() {
        return pluginData;
    }

    public void setPluginData(PluginData pluginData) {
        this.pluginData = pluginData;
    }

    public JanusPluginResponse getJanusPluginResponse() {
        return janusPluginResponse;
    }

    public void setJanusPluginResponse(JanusPluginResponse janusPluginResponse) {
        this.janusPluginResponse = janusPluginResponse;
    }

    public Jsep getJsep() {
        return jsep;
    }

    public void setJsep(Jsep jsep) {
        this.jsep = jsep;
    }

    public JSONObject toJson() {
        JSONObject json = new JSONObject();
        try {
            json.put("plugindata", pluginData.toJson());
            json.put("janusPluginResponse", janusPluginResponse.toJson());
            json.put("jsep", jsep.toJson());
        } catch (Exception ignored) {
        }
        return json;
    }

    public static JanusPollResponse fromJson(JSONObject json) {
        if (json == null) return null;
        JanusPollResponse data = new JanusPollResponse();
        if (json.has("plugindata")) {
            data.pluginData = PluginData.fromJson(json.optJSONObject("plugindata"));
        }
        if (json.has("janusPluginResponse")) {
            data.janusPluginResponse = JanusPluginResponse.fromJson(json.optJSONObject("janusPluginResponse"));
        }
        if (json.has("jsep")) {
            data.jsep = Jsep.fromJson(json.optJSONObject("jsep"));
        }
        return data;
    }

}
