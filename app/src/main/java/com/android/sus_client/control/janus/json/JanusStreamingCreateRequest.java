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

public class JanusStreamingCreateRequest extends JanusPluginRequest {

    public static class Body {
        public String request;
        public String type;
        public String description;
        public boolean permanent;
        public boolean is_private;
        public boolean audio;
        public boolean video;
        public boolean data;
        public String id;
        public String name;
        public String pin;
        public int audioport;
        public int audiopt;
        public String audiortpmap;
        public int videoport;
        public int videopt;
        public String videortpmap;
        public boolean videobufferkf;
        public String videofmtp;

        public String getRequest() {
            return request;
        }

        public void setRequest(String request) {
            this.request = request;
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

        public boolean isPermanent() {
            return permanent;
        }

        public void setPermanent(boolean permanent) {
            this.permanent = permanent;
        }

        public boolean isIs_private() {
            return is_private;
        }

        public void setIs_private(boolean is_private) {
            this.is_private = is_private;
        }

        public boolean isAudio() {
            return audio;
        }

        public void setAudio(boolean audio) {
            this.audio = audio;
        }

        public boolean isVideo() {
            return video;
        }

        public void setVideo(boolean video) {
            this.video = video;
        }

        public boolean isData() {
            return data;
        }

        public void setData(boolean data) {
            this.data = data;
        }

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getPin() {
            return pin;
        }

        public void setPin(String pin) {
            this.pin = pin;
        }

        public int getAudioport() {
            return audioport;
        }

        public void setAudioport(int audioport) {
            this.audioport = audioport;
        }

        public int getAudiopt() {
            return audiopt;
        }

        public void setAudiopt(int audiopt) {
            this.audiopt = audiopt;
        }

        public String getAudiortpmap() {
            return audiortpmap;
        }

        public void setAudiortpmap(String audiortpmap) {
            this.audiortpmap = audiortpmap;
        }

        public int getVideoport() {
            return videoport;
        }

        public void setVideoport(int videoport) {
            this.videoport = videoport;
        }

        public int getVideopt() {
            return videopt;
        }

        public void setVideopt(int videopt) {
            this.videopt = videopt;
        }

        public String getVideortpmap() {
            return videortpmap;
        }

        public void setVideortpmap(String videortpmap) {
            this.videortpmap = videortpmap;
        }

        public boolean isVideobufferkf() {
            return videobufferkf;
        }

        public void setVideobufferkf(boolean videobufferkf) {
            this.videobufferkf = videobufferkf;
        }

        public String getVideofmtp() {
            return videofmtp;
        }

        public void setVideofmtp(String videofmtp) {
            this.videofmtp = videofmtp;
        }
    }

    private Body body;

    public JanusStreamingCreateRequest() {
    }

    public JanusStreamingCreateRequest(String secret, String janus, String sessionId, String handleId) {
        super(secret, janus, sessionId, handleId);
    }

    public Body getBody() {
        return body;
    }

    public void setBody(Body body) {
        this.body = body;
    }
}