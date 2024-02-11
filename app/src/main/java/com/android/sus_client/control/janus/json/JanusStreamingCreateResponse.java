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

public class JanusStreamingCreateResponse {

    public static class Stream {
        private String id = "";
        private String type = "";
        private String description = "";
        private boolean is_private = false;
        private int audio_port = 0;
        private int video_port = 0;

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public boolean isIs_private() {
            return is_private;
        }

        public void setIs_private(boolean is_private) {
            this.is_private = is_private;
        }

        public int getAudio_port() {
            return audio_port;
        }

        public void setAudio_port(int audio_port) {
            this.audio_port = audio_port;
        }

        public int getVideo_port() {
            return video_port;
        }

        public void setVideo_port(int video_port) {
            this.video_port = video_port;
        }

        @Override
        public String toString() {
            return toJson().toString();
        }

        public JSONObject toJson() {
            JSONObject json = new JSONObject();
            try {
                json.put("id", id);
                json.put("type", type);
                json.put("description", description);
                json.put("is_private", is_private);
                json.put("audio_port", audio_port);
                json.put("video_port", video_port);
            } catch (Exception ignored) {
            }
            return json;
        }

        public Stream fromJson(JSONObject json) {
            if (json == null) return null;
            id = json.optString("id");
            type = json.optString("type");
            description = json.optString("description");
            is_private = json.optBoolean("is_private");
            audio_port = json.optInt("audio_port");
            video_port = json.optInt("video_port");
            return this;
        }

    }

    public static class StreamingData {
        private String streaming;
        private String created;
        private String permanent;
        private Stream stream;

        public String getStreaming() {
            return streaming;
        }

        public void setStreaming(String streaming) {
            this.streaming = streaming;
        }

        public String getCreated() {
            return created;
        }

        public void setCreated(String created) {
            this.created = created;
        }

        public String getPermanent() {
            return permanent;
        }

        public void setPermanent(String permanent) {
            this.permanent = permanent;
        }

        public Stream getStream() {
            return stream;
        }

        public void setStream(Stream stream) {
            this.stream = stream;
        }

        @Override
        public String toString() {
            return toJson().toString();
        }

        public JSONObject toJson() {
            JSONObject json = new JSONObject();
            try {
                json.put("streaming", streaming);
                json.put("created", created);
                json.put("permanent", permanent);
                json.put("stream", stream.toJson());
            } catch (Exception ignored) {
            }
            return json;
        }

        public StreamingData fromJson(JSONObject json) {
            if (json == null) return null;
            streaming = json.optString("streaming");
            created = json.optString("created");
            permanent = json.optString("permanent");
            stream = new Stream().fromJson(json.optJSONObject("stream"));
            return this;
        }

    }

    public static class PluginData {
        private String plugin;
        private StreamingData data;

        public PluginData(String plugin, StreamingData data) {
            this.plugin = plugin;
            this.data = data;
        }

        public String getPlugin() {
            return plugin;
        }

        public void setPlugin(String plugin) {
            this.plugin = plugin;
        }

        public StreamingData getData() {
            return data;
        }

        public void setData(StreamingData data) {
            this.data = data;
        }

        @Override
        public String toString() {
            return "{\"plugin\":\"" + plugin + "\",\"data\":" + (data != null ? data.toString() : "null") + "}";
        }

    }

    private PluginData pluginData;

    private JanusPluginResponse janusPluginResponse;

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

    @Override
    public String toString() {
        return "{\"janus\":\"" + janusPluginResponse.getJanus() + "\",\"session_id\":" + janusPluginResponse.getSession_id() + "\",\"transaction\":\"" + janusPluginResponse.getTransaction() + "\",\"sender\":\"" + janusPluginResponse.getSender() + "\","
                + "\"plugindata\":" + (pluginData != null ? pluginData.toString() : "null") + "}";
    }

    public JSONObject toJson() {
        JSONObject json = new JSONObject();
        try {
            json.put("janus", janusPluginResponse.getJanus());
            json.put("session_id", janusPluginResponse.getSession_id());
            json.put("transaction", janusPluginResponse.getTransaction());
            json.put("sender", janusPluginResponse.getSender());
            json.put("plugindata", pluginData != null ? pluginData.toString() : "null");
        } catch (Exception ignored) {
        }
        return json;
    }

    public JanusStreamingCreateResponse fromJson(JSONObject json) {
        janusPluginResponse = JanusPluginResponse.fromJson(json);
        pluginData = new PluginData(json.optString("plugin"), new StreamingData().fromJson(json.optJSONObject("data")));
        return this;
    }

}