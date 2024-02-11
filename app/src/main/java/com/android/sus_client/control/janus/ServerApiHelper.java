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

package com.android.sus_client.control.janus;

public class ServerApiHelper {
    public static String lastError = null;

    /*public static Response execute(Call call, String description) {
        Response response = null;

        try {
            lastError = null;
            response = call.execute();
        } catch (Exception e) {
            Log.w(Const.LOG_TAG, "Failed to " + description + ": " + e.getMessage());
            lastError = e.getMessage();
            return null;
        }
        if (response == null) {
            Log.w(Const.LOG_TAG, "Failed to " + description + ": network error");
            lastError = "Network error";
            return null;
        }

        if (!response.isSuccessful()) {
            if (response.code() == 502) {
                lastError = "Media server is down";
                return null;
            }

            Log.w(Const.LOG_TAG, "Failed to " + description + ": bad server response: " + response.message());
            lastError = response.message();
            return null;
        }

        return response;
    }*/

}