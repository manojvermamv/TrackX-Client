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

public class JanusResponse {

    public class Data {
        private String id;

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public JSONObject toJson() {
            JSONObject json = new JSONObject();
            try {
                json.put("id", id);
            } catch (Exception ignored) {
            }
            return json;
        }

        public Data fromJson(JSONObject json) {
            if (json == null) return null;
            id = json.optString("id");
            return this;
        }
    }

    public class Error {
        private Integer code;
        private String reason;

        public Integer getCode() {
            return code;
        }

        public void setCode(Integer code) {
            this.code = code;
        }

        public String getReason() {
            return reason;
        }

        public void setReason(String reason) {
            this.reason = reason;
        }

        public JSONObject toJson() {
            JSONObject json = new JSONObject();
            try {
                json.put("code", code);
                json.put("reason", reason);
            } catch (Exception ignored) {
            }
            return json;
        }

        public Error fromJson(JSONObject json) {
            if (json == null) return null;
            code = json.optInt("code");
            reason = json.optString("reason");
            return this;
        }

    }

    private String janus;
    private String transaction;

    private Data data;
    private Error error;

    public String getJanus() {
        return janus;
    }

    public void setJanus(String janus) {
        this.janus = janus;
    }

    public String getTransaction() {
        return transaction;
    }

    public void setTransaction(String transaction) {
        this.transaction = transaction;
    }

    public Data getData() {
        return data;
    }

    public void setData(Data data) {
        this.data = data;
    }

    public Error getError() {
        return error;
    }

    public void setError(Error error) {
        this.error = error;
    }

    public JSONObject toJson() {
        JSONObject json = new JSONObject();
        try {
            json.put("janus", janus);
            json.put("transaction", transaction);
            json.put("data", data.toJson());
            json.put("error", error.toJson());
        } catch (Exception ignored) {
        }
        return json;
    }

    public JanusResponse fromJson(JSONObject json) {
        janus = json.optString("janus");
        transaction = json.optString("transaction");
        if (json.has("data")) {
            data = new Data().fromJson(json.optJSONObject("data"));
        }
        if (json.has("error")) {
            error = new Error().fromJson(json.optJSONObject("error"));
        }
        return this;
    }

}