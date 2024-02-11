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

public class JanusMessageRequest extends JanusPluginRequest {

    public static class Body {
        private String request;
        private String id;

        // For textroom messages
        private String room;

        public Body() {
        }

        public Body(String request, String id) {
            this.request = request;
            this.id = id;
        }

        public Body(String request, String id, String room) {
            this.request = request;
            this.id = id;
            this.room = room;
        }

        public String getRequest() {
            return request;
        }

        public void setRequest(String request) {
            this.request = request;
        }

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getRoom() {
            return room;
        }

        public void setRoom(String room) {
            this.room = room;
        }
    }

    private Body body;

    public JanusMessageRequest() {
    }

    public JanusMessageRequest(String secret, String janus, String sessionId, String handleId) {
        super(secret, janus, sessionId, handleId);
    }

    public Body getBody() {
        return body;
    }

    public void setBody(Body body) {
        this.body = body;
    }
}
