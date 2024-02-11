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

public class JanusPluginRequest extends JanusRequest {
    private String handle_id;


    public JanusPluginRequest() {
    }

    public JanusPluginRequest(String secret, String janus, String sessionId, String handleId) {
        super(secret, janus, true);
        setSession_id(sessionId);
        setHandle_id(handleId);
    }

    public String getHandle_id() {
        return handle_id;
    }

    public void setHandle_id(String handle_id) {
        this.handle_id = handle_id;
    }
}
