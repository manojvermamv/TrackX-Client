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

package com.android.sus_client.control;

import android.content.Context;

import com.android.sus_client.services.SocketHandler;

public abstract class SharingEngine {
    protected int state = Const.STATE_DISCONNECTED;
    protected EventListener eventListener;
    protected StateListener stateListener;

    // Shareable session ID
    protected String sessionId;
    protected String errorReason;

    protected SocketHandler socketHandler;
    protected String username = "Device";
    protected boolean audio;
    protected int screenWidth;
    protected int screenHeight;

    public int getState() {
        return state;
    }

    public void setState(int state) {
        this.state = state;
        if (stateListener != null) {
            stateListener.onSharingApiStateChanged(state);
        }
    }

    public void setEventListener(EventListener eventListener) {
        this.eventListener = eventListener;
    }

    public void setStateListener(StateListener stateListener) {
        this.stateListener = stateListener;
    }

    public void setScreenWidth(int screenWidth) {
        this.screenWidth = screenWidth;
    }

    public void setScreenHeight(int screenHeight) {
        this.screenHeight = screenHeight;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public boolean isAudio() {
        return audio;
    }

    public void setAudio(boolean audio) {
        this.audio = audio;
    }

    public String getErrorReason() {
        return errorReason;
    }

    public abstract void connect(final Context context, final String sessionId, final SocketHandler socketHandler, final CompletionHandler completionHandler);

    public abstract void disconnect(Context context, CompletionHandler completionHandler);

    public abstract int getAudioPort();

    public abstract int getVideoPort();

    public interface CompletionHandler {
        void onComplete(boolean success, String errorReason);
    }

    public interface EventListener {
        void onStartSharing(String username);

        void onStopSharing();

        void onPing();

        void onRemoteControlEvent(String event);
    }

    public interface StateListener {
        void onSharingApiStateChanged(int state);
    }
}