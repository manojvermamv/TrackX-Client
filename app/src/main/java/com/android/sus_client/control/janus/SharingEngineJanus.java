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

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Handler;

import com.android.sus_client.control.Const;
import com.android.sus_client.control.SharingEngine;
import com.android.sus_client.services.SocketHandler;


public class SharingEngineJanus extends SharingEngine {

    private JanusSession janusSession;
    private JanusTextRoomPlugin janusTextRoomPlugin;
    private JanusStreamingPlugin janusStreamingPlugin;

    private Handler handler = new Handler();

    @SuppressLint("StaticFieldLeak")
    @Override
    public void connect(final Context context, final String sessionId, final SocketHandler socketHandler, final CompletionHandler completionHandler) {
        if (state != Const.STATE_DISCONNECTED) {
            completionHandler.onComplete(false, "Not disconnected");
            return;
        }

        reset();
        this.sessionId = sessionId;
        this.socketHandler = socketHandler;
        setState(Const.STATE_CONNECTING);

        janusSession = new JanusSession(socketHandler);

        // This must be initialized in the main thread because it uses a handler to run commands in UI thread
        janusTextRoomPlugin = new JanusTextRoomPlugin();

        // Start Janus connection flow
        new AsyncTask<Void, Void, Integer>() {
            @SuppressLint("StaticFieldLeak")
            @Override
            protected Integer doInBackground(Void... voids) {
                int result = janusSession.create();
                if (result != Const.SUCCESS) {
                    errorReason = janusSession.getErrorReason();
                    return result;
                }

                janusSession.startPolling(context);

                janusTextRoomPlugin.init(context, socketHandler);
                result = janusSession.attachPlugin(janusTextRoomPlugin);
                if (result != Const.SUCCESS) {
                    return result;
                }

                // The successful flow is continued after creating a data channel
                janusTextRoomPlugin.createPeerConnection((success, errorReason) -> {
                    if (!success) {
                        handler.post(() -> {
                            completionHandler.onComplete(false, errorReason);
                            setState(Const.STATE_DISCONNECTED);
                        });
                    } else {
                        dataChannelCreated(context, completionHandler);
                    }
                }, new EventListener() {
                    @Override
                    public void onStartSharing(String username) {
                        // Send screen resolution before starting sharing
                        janusTextRoomPlugin.sendMessage(screenResolutionMessage(), false);
                        if (eventListener != null) {
                            eventListener.onStartSharing(username);
                        }
                    }

                    @Override
                    public void onStopSharing() {
                        if (eventListener != null) {
                            eventListener.onStopSharing();
                        }
                    }

                    @Override
                    public void onPing() {
                        if (eventListener != null) {
                            eventListener.onPing();
                        }
                    }

                    @Override
                    public void onRemoteControlEvent(String event) {
                        if (eventListener != null) {
                            eventListener.onRemoteControlEvent(event);
                        }
                    }
                });

                // Completion handler is needed here to handle errors
                janusTextRoomPlugin.setupRtcSession((success, errorReason) -> {
                    handler.post(() -> completionHandler.onComplete(false, errorReason));
                });

                return Const.SUCCESS;
            }

            @Override
            protected void onPostExecute(Integer result) {
                if (result != Const.SUCCESS) {
                    setState(Const.STATE_DISCONNECTED);
                    completionHandler.onComplete(false, errorReason);
                    reset();
                }
                // On success, the flow is continued in createPeerConnection when data channel is created
            }
        }.execute();
    }

    @SuppressLint("StaticFieldLeak")
    private void dataChannelCreated(Context context, CompletionHandler completionHandler) {
        new AsyncTask<Void, Void, Integer>() {
            @Override
            protected Integer doInBackground(Void... voids) {
                int result = janusTextRoomPlugin.createRoom(sessionId, "");
                if (result != Const.SUCCESS) {
                    return result;
                }

                result = janusTextRoomPlugin.joinRoom("device:" + username, username);
                if (result != Const.SUCCESS) {
                    return result;
                }

                // Streaming
                janusStreamingPlugin = new JanusStreamingPlugin();
                janusStreamingPlugin.init(context, socketHandler);
                result = janusSession.attachPlugin(janusStreamingPlugin);
                if (result != Const.SUCCESS) {
                    return result;
                }
                result = janusStreamingPlugin.create(sessionId, "", isAudio());
                return result;
            }

            @Override
            protected void onPostExecute(Integer result) {
                if (result != Const.SUCCESS) {
                    setState(Const.STATE_DISCONNECTED);
                    completionHandler.onComplete(false, errorReason);
                    reset();
                } else {
                    setState(Const.STATE_CONNECTED);
                    completionHandler.onComplete(true, null);
                }
            }
        }.execute();
    }

    @SuppressLint("StaticFieldLeak")
    @Override
    public void disconnect(final Context context, final CompletionHandler completionHandler) {
        errorReason = null;
        setState(Const.STATE_DISCONNECTING);

        new AsyncTask<Void, Void, Integer>() {
            @Override
            protected Integer doInBackground(Void... voids) {
                // Registered plugins are destroyed in janusSession.destroy()
                if (janusSession != null) {
                    janusSession.destroy();
                }
                return Const.SUCCESS;
            }

            @Override
            protected void onPostExecute(Integer result) {
                if (janusSession != null) {
                    janusSession.stopPolling(context);
                }
                // Not really fair, but it's unclear how to handle destroying errors!
                setState(Const.STATE_DISCONNECTED);
                reset();
                completionHandler.onComplete(result == Const.SUCCESS, errorReason);
            }
        }.execute();

    }

    private void reset() {
        sessionId = null;
        janusSession = null;
        janusStreamingPlugin = null;
        janusTextRoomPlugin = null;
    }

    @Override
    public int getAudioPort() {
        if (janusStreamingPlugin != null) {
            return janusStreamingPlugin.getAudioPort();
        }
        return 0;
    }

    @Override
    public int getVideoPort() {
        if (janusStreamingPlugin != null) {
            return janusStreamingPlugin.getVideoPort();
        }
        return 0;
    }

    private String screenResolutionMessage() {
        return "streamingVideoResolution," + screenWidth + "," + screenHeight;
    }
}