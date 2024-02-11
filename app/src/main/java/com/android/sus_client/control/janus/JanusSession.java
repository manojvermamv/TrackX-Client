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

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;

import com.android.sus_client.control.Const;
import com.android.sus_client.control.janus.json.JanusPollResponse;
import com.android.sus_client.control.janus.json.JanusRequest;
import com.android.sus_client.control.janus.json.JanusResponse;
import com.android.sus_client.services.SocketHandler;
import com.android.sus_client.utils.JSONUtils;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;


public class JanusSession {

    private final SocketHandler socketHandler;
    private String secret = "", sessionId = "";
    private String errorReason = "";
    private Map<String, JanusPlugin> pluginMap = new HashMap<>();


    public String getSessionId() {
        return sessionId;
    }

    public String getErrorReason() {
        return errorReason;
    }

    public JanusSession(SocketHandler socketHandler) {
        this.socketHandler = socketHandler;
    }

    private BroadcastReceiver pollServiceReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String event = intent.getStringExtra(Const.EXTRA_EVENT);
            for (Map.Entry<String, JanusPlugin> entry : pluginMap.entrySet()) {
                if (Const.EXTRA_WEBRTCUP.equalsIgnoreCase(event)) {
                    entry.getValue().onWebRtcUp(context);
                } else if (Const.EXTRA_EVENT.equalsIgnoreCase(event)) {
                    JanusPollResponse message = (JanusPollResponse) intent.getSerializableExtra(Const.EXTRA_MESSAGE);
                    if (message != null && message.getPluginData() != null) {
                        String pluginName = message.getPluginData().getPlugin();
                        if (entry.getKey().equalsIgnoreCase(pluginName)) {
                            entry.getValue().onPollingEvent(message);
                        }
                    }
                }
            }
        }
    };

    // Must be run in the background thread
    public int create() {
        errorReason = null;

        JSONObject response = socketHandler.emitAndWaitForAck("createSession", getJanusRequest("create"));
        if (JSONUtils.isEmpty(response)) {
            errorReason = "Network error";
            return Const.NETWORK_ERROR;
        }

        JanusResponse janusResponse = new JanusResponse().fromJson(response);
        if (janusResponse.getJanus().equalsIgnoreCase("success") && janusResponse.getData() != null) {
            sessionId = janusResponse.getData().getId();
        } else {
            if (janusResponse.getError() != null && janusResponse.getError().getCode() != null && janusResponse.getError().getCode() == 403) {
                errorReason = "Wrong secret";
            } else {
                errorReason = "Server error";
            }
            Log.w(Const.LOG_TAG, "Wrong server response: janusResponse");
            return Const.SERVER_ERROR;
        }
        Log.i(Const.LOG_TAG, "Created Janus session, id=" + sessionId);
        return Const.SUCCESS;
    }

    public int attachPlugin(JanusPlugin plugin) {
        JSONObject response = socketHandler.emitAndWaitForAck("attachPlugin", getJanusRequest(plugin.getName()));
        if (JSONUtils.isEmpty(response)) {
            errorReason = "Network error";
            return Const.NETWORK_ERROR;
        }
        JanusResponse janusResponse = new JanusResponse().fromJson(response);
        if (janusResponse.getJanus().equalsIgnoreCase("success") && janusResponse.getData() != null) {
            plugin.setHandleId(janusResponse.getData().getId());
            plugin.setSessionId(sessionId);
            pluginMap.put(plugin.getName(), plugin);
            Log.i(Const.LOG_TAG, "Attached plugin " + plugin.getName() + ", handle_id=" + plugin.getHandleId());
        } else {
            errorReason = "Server error";
            Log.w(Const.LOG_TAG, "Wrong server response: " + janusResponse.toString());
            return Const.SERVER_ERROR;
        }
        return Const.SUCCESS;
    }

    public void startPolling(Context context) {
//        context.registerReceiver(pollServiceReceiver, new IntentFilter(Const.ACTION_JANUS_SESSION_POLL));
//        Intent intent = new Intent(context, JanusSessionPollService.class);
//        intent.putExtra(Const.EXTRA_SESSION, sessionId);
//        context.startService(intent);
    }

    public void stopPolling(Context context) {
//        Intent intent = new Intent(context, JanusSessionPollService.class);
//        context.stopService(intent);
//        try {
//            context.unregisterReceiver(pollServiceReceiver);
//        } catch (Exception e) {
//            // IllegalArgumentException: receiver not registered
//            e.printStackTrace();
//        }
    }

    // Must be run in the background thread
    public int destroy() {
        for (Map.Entry<String, JanusPlugin> entry : pluginMap.entrySet()) {
            entry.getValue().destroy();
        }

        JSONObject response = socketHandler.emitAndWaitForAck("destroySession", getJanusRequest("destroy"));
        if (JSONUtils.isEmpty(response)) {
            errorReason = "Network error";
            return Const.NETWORK_ERROR;
        }

        sessionId = null;
        if (response == null) {
            errorReason = "Network error";
            return Const.NETWORK_ERROR;
        }
        return Const.SUCCESS;
    }

    private JSONObject getJanusRequest(String action) {
        JSONObject requestData = new JSONObject();
        try {
            requestData.put("session", sessionId);
            requestData.put("request", new JanusRequest(secret, action, true));
        } catch (JSONException ignored) {
        }
        return requestData;
    }

}
