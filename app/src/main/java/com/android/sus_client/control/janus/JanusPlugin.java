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

import android.content.Context;

import com.android.sus_client.control.janus.json.JanusPollResponse;
import com.android.sus_client.services.SocketHandler;

public abstract class JanusPlugin {

    protected Context context;
    protected SocketHandler socketHandler;

    protected String sessionId;
    protected String handleId;
    protected String errorReason;

    protected JanusPollResponse pollingEvent;
    protected Object pollingEventLock = new Object();

    public String getHandleId() {
        return handleId;
    }

    public String getErrorReason() {
        return errorReason;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public void setHandleId(String handleId) {
        this.handleId = handleId;
    }

    public void init(Context context, SocketHandler socketHandler) {
        this.context = context;
        this.socketHandler = socketHandler;
    }

    public void onWebRtcUp(final Context context) {
    }

    public void onPollingEvent(JanusPollResponse event) {
        synchronized (pollingEventLock) {
            pollingEvent = event;
            pollingEventLock.notify();
        }
    }

    public abstract String getName();

    public abstract int destroy();

}