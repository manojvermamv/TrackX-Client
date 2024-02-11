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
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Handler;
import android.util.Log;

import com.android.sus_client.control.Const;
import com.android.sus_client.control.SharingEngine;
import com.android.sus_client.control.janus.json.JanusJsepRequest;
import com.android.sus_client.control.janus.json.JanusMessageRequest;
import com.android.sus_client.control.janus.json.JanusResponse;
import com.android.sus_client.control.janus.json.Jsep;
import com.android.sus_client.control.utils.Utils;
import com.android.sus_client.services.SocketHandler;
import com.android.sus_client.utils.JSONUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.webrtc.DataChannel;
import org.webrtc.IceCandidate;
import org.webrtc.MediaConstraints;
import org.webrtc.MediaStream;
import org.webrtc.PeerConnection;
import org.webrtc.PeerConnectionFactory;
import org.webrtc.RtpReceiver;
import org.webrtc.SdpObserver;
import org.webrtc.SessionDescription;

import java.nio.ByteBuffer;
import java.util.LinkedList;
import java.util.List;

public class JanusTextRoomPlugin extends JanusPlugin {

    private String roomId;
    private String password;
    private PeerConnectionFactory peerConnectionFactory;
    private PeerConnection peerConnection;
    private DataChannel dataChannel;
    private boolean joined;

    private boolean dcResult;
    private Object dcResultLock = new Object();

    private Handler handler = new Handler();
    private Runnable hangupProtectionRunnable = null;

    @Override
    public String getName() {
        return Const.JANUS_PLUGIN_TEXTROOM;
    }

    public String getRoomId() {
        return roomId;
    }

    @Override
    public void init(Context context, SocketHandler socketHandler) {
        super.init(context, socketHandler);
        PeerConnectionFactory.InitializationOptions initializationOptions =
                PeerConnectionFactory.InitializationOptions.builder(context)
                        .createInitializationOptions();
        PeerConnectionFactory.initialize(initializationOptions);
        peerConnectionFactory = PeerConnectionFactory.builder().createPeerConnectionFactory();
    }

    public void createPeerConnection(final SharingEngine.CompletionHandler completionHandler, final SharingEngine.EventListener eventListener) {
        // No ICE servers required for textroom: textroom messages are all coming through Janus
        List<PeerConnection.IceServer> iceServers = new LinkedList<>();
        peerConnection = peerConnectionFactory.createPeerConnection(iceServers, new PeerConnection.Observer() {
            @Override
            public void onSignalingChange(PeerConnection.SignalingState signalingState) {
                Log.d(Const.LOG_TAG, "Textroom plugin: signalingState changed to " + signalingState);
            }

            @Override
            public void onIceConnectionChange(PeerConnection.IceConnectionState iceConnectionState) {
                Log.d(Const.LOG_TAG, "Textroom plugin: iceConnectionState changed to " + iceConnectionState);
                if (iceConnectionState.equals(PeerConnection.IceConnectionState.FAILED)) {
                    completionHandler.onComplete(false, Const.ERROR_ICE_FAILED);
                }
            }

            @Override
            public void onIceConnectionReceivingChange(boolean b) {
                Log.d(Const.LOG_TAG, "Textroom plugin: iceConnectionReceivingChange: " + b);
            }

            @Override
            public void onIceGatheringChange(PeerConnection.IceGatheringState iceGatheringState) {
                Log.d(Const.LOG_TAG, "Textroom plugin: iceGatheringState changed to " + iceGatheringState);
            }

            @Override
            public void onIceCandidate(IceCandidate iceCandidate) {
                Log.d(Const.LOG_TAG, "Textroom plugin: iceCandidate: " + iceCandidate.toString());
            }

            @Override
            public void onIceCandidatesRemoved(IceCandidate[] iceCandidates) {
                Log.d(Const.LOG_TAG, "Textroom plugin: iceCandidateRemoved");
            }

            @Override
            public void onAddStream(MediaStream mediaStream) {
                Log.d(Const.LOG_TAG, "Textroom plugin: onAddStream: " + mediaStream.toString());
            }

            @Override
            public void onRemoveStream(MediaStream mediaStream) {
                Log.d(Const.LOG_TAG, "Textroom plugin: onRemoveStream: " + mediaStream.toString());
            }

            @Override
            public void onDataChannel(DataChannel dataChannel) {
                Log.d(Const.LOG_TAG, "Textroom plugin: onDataChannel, id=" + dataChannel.id() + ", label=" + dataChannel.label());
                dataChannel.registerObserver(new DataChannel.Observer() {
                    @Override
                    public void onBufferedAmountChange(long l) {
                        Log.d(Const.LOG_TAG, "Textroom plugin: dataChannel - onBufferedAmountChange=" + l);
                    }

                    @Override
                    public void onStateChange() {
                        Log.d(Const.LOG_TAG, "Textroom plugin: dataChannel - onStateChange");
                    }

                    @Override
                    public void onMessage(DataChannel.Buffer buffer) {
                        String message = Utils.byteBufferToString(buffer.data);
                        Log.d(Const.LOG_TAG, "Textroom plugin: got message from DataChannel: " + message);

                        try {
                            JSONObject jsonObject = new JSONObject(message);
                            String type = jsonObject.optString("textroom");
                            if ("join".equalsIgnoreCase(type)) {
                                if (!checkJoined()) {
                                    return;
                                }
                                Log.d(Const.LOG_TAG, "Remote control agent connected, starting sharing");
                                String username = jsonObject.optString("username");
                                if (eventListener != null) {
                                    handler.post(() -> eventListener.onStartSharing(username));
                                }
                            } else if ("message".equalsIgnoreCase(type)) {
                                if (!checkJoined()) {
                                    return;
                                }
                                String text = jsonObject.optString("text");
                                if (text.startsWith("ping,")) {
                                    String[] parts = text.split(",");
                                    sendMessage("pong," + parts[1], false);
                                    handler.post(() -> eventListener.onPing());
                                } else if (text.startsWith("pong,")) {
                                    // Echo from our response, do nothing
                                } else if (eventListener != null) {
                                    Log.d(Const.LOG_TAG, "Dispatching message: " + text);
                                    handler.post(() -> eventListener.onRemoteControlEvent(text));
                                }
                            } else if ("leave".equalsIgnoreCase(type)) {
                                if (!checkJoined()) {
                                    return;
                                }
                                Log.d(Const.LOG_TAG, "Remote control agent disconnected, stopping sharing");
                                if (eventListener != null) {
                                    handler.post(() -> eventListener.onStopSharing());
                                }
                            } else if ("success".equalsIgnoreCase(type)) {
                                JSONArray list = jsonObject.optJSONArray("list");
                                if (list != null) {
                                    // This is the response to test request, nothing to do
                                    return;
                                }
                                synchronized (dcResultLock) {
                                    dcResult = true;
                                    dcResultLock.notify();
                                }
                            } else if ("error".equalsIgnoreCase(type)) {
                                synchronized (dcResultLock) {
                                    dcResult = false;
                                    dcResultLock.notify();
                                }
                            } else {
                                Log.d(Const.LOG_TAG, "Ignoring this message");
                            }

                        } catch (JSONException e) {
                            Log.w(Const.LOG_TAG, "Failed to parse JSON, ignoring!");
                        }
                    }
                });
                // Here's a final point of data channel creation
                completionHandler.onComplete(true, null);
            }

            @Override
            public void onRenegotiationNeeded() {
                Log.d(Const.LOG_TAG, "Textroom plugin: onRenegotiationNeeded");
            }

            @Override
            public void onAddTrack(RtpReceiver rtpReceiver, MediaStream[] mediaStreams) {
                Log.d(Const.LOG_TAG, "Textroom plugin: onAddTrack");
            }
        });

    }

    private boolean checkJoined() {
        if (!joined) {
            Log.w(Const.LOG_TAG, "Ignoring message because we're not yet joined the textroom");
            return false;
        }
        return true;
    }


    private JanusJsepRequest createJsepRequest(String requestType) {
        JanusJsepRequest request = new JanusJsepRequest("", "message", getSessionId(), getHandleId());
        request.setBody(new JanusMessageRequest.Body(requestType, null));
        return request;
    }

    public void setupRtcSession(final SharingEngine.CompletionHandler completionHandler) {
        errorReason = null;

        JanusJsepRequest offerRequest = createJsepRequest("setup");
        JSONObject response = socketHandler.emitAndWaitForAck("sendJsep", getJanusRequest(offerRequest));
        if (JSONUtils.isEmpty(response)) {
            errorReason = "Network error";
            completionHandler.onComplete(false, errorReason);
            return;
        }

        JanusResponse janusResponse = new JanusResponse().fromJson(response);
        if (janusResponse.getJanus().equalsIgnoreCase("ack")) {
            Log.i(Const.LOG_TAG, "Got response to JSEP offer, waiting for event");
            synchronized (pollingEventLock) {
                try {
                    pollingEventLock.wait();
                } catch (InterruptedException e) {
                    errorReason = "Interrupted";
                    completionHandler.onComplete(false, errorReason);
                    return;
                }
            }
            Jsep jsepData = pollingEvent.getJsep();
            if (jsepData == null) {
                errorReason = "Server error";
                Log.w(Const.LOG_TAG, "Missing JSEP: " + response.toString());
                completionHandler.onComplete(false, errorReason);
                return;
            }
            peerConnection.setRemoteDescription(new SdpObserver() {
                @Override
                public void onCreateSuccess(SessionDescription sessionDescription) {
                    Log.i(Const.LOG_TAG, "RemoteDescription - create success");
                }

                @Override
                public void onSetSuccess() {
                    Log.i(Const.LOG_TAG, "RemoteDescription - success");
                    // Proceed with SDP asynchronously
                    createSessionAnswer(completionHandler);
                }

                @Override
                public void onCreateFailure(String s) {
                    Log.i(Const.LOG_TAG, "RemoteDescription - create failure: " + s);
                }

                @Override
                public void onSetFailure(String s) {
                    errorReason = "RemoteDescription - failure: " + s;
                    Log.w(Const.LOG_TAG, errorReason);
                    completionHandler.onComplete(false, errorReason);
                }
            }, new SessionDescription(SessionDescription.Type.OFFER, jsepData.getSdp()));
        }
    }

    private void createSessionAnswer(final SharingEngine.CompletionHandler completionHandler) {
        MediaConstraints constraints = new MediaConstraints();
        peerConnection.createAnswer(new SdpObserver() {
            @Override
            public void onCreateSuccess(SessionDescription sessionDescription) {
                Log.i(Const.LOG_TAG, "createAnswer - create success");
                // Proceed with setting local session description asynchronously
                setLocalSessionDescription(sessionDescription, completionHandler);
            }

            @Override
            public void onSetSuccess() {
                Log.i(Const.LOG_TAG, "createAnswer - success");
            }

            @Override
            public void onCreateFailure(String s) {
                errorReason = "createAnswer - create failure: " + s;
                Log.w(Const.LOG_TAG, errorReason);
                completionHandler.onComplete(false, errorReason);
            }

            @Override
            public void onSetFailure(String s) {
                Log.i(Const.LOG_TAG, "createAnswer - failure: " + s);
            }
        }, constraints);
    }

    private void setLocalSessionDescription(final SessionDescription sessionDescription, final SharingEngine.CompletionHandler completionHandler) {
        peerConnection.setLocalDescription(new SdpObserver() {
            @Override
            public void onCreateSuccess(SessionDescription sessionDescription) {
                Log.i(Const.LOG_TAG, "LocalDescription - create success");
            }

            @Override
            public void onSetSuccess() {
                Log.i(Const.LOG_TAG, "LocalDescription - success");
                // Proceed with SDP asynchronously
                sendSessionDescriptionAck(completionHandler);
            }

            @Override
            public void onCreateFailure(String s) {
                Log.i(Const.LOG_TAG, "LocalDescription - create failure: " + s);
            }

            @Override
            public void onSetFailure(String s) {
                errorReason = "LocalDescription - failure: " + s;
                Log.w(Const.LOG_TAG, errorReason);
                completionHandler.onComplete(false, errorReason);
            }
        }, sessionDescription);
    }

    private void sendSessionDescriptionAck(final SharingEngine.CompletionHandler completionHandler) {
        JanusJsepRequest offerRequest = createJsepRequest("ack");
        offerRequest.setJsep(new Jsep("answer", peerConnection.getLocalDescription().description));
        JSONObject response = socketHandler.emitAndWaitForAck("sendJsep", getJanusRequest(offerRequest));
        if (JSONUtils.isEmpty(response)) {
            errorReason = "Network error";
            completionHandler.onComplete(false, errorReason);
        }

        JanusResponse janusResponse = new JanusResponse().fromJson(response);
        if (janusResponse.getJanus().equalsIgnoreCase("ack")) {
            Log.i(Const.LOG_TAG, "Got response to JSEP offer, waiting for event");

            synchronized (pollingEventLock) {
                try {
                    pollingEventLock.wait();
                } catch (InterruptedException e) {
                    errorReason = "Interrupted";
                    completionHandler.onComplete(false, errorReason);
                    return;
                }
            }
            if (pollingEvent.getPluginData() == null || pollingEvent.getPluginData().getData() == null ||
                    !"ok".equalsIgnoreCase(pollingEvent.getPluginData().getData().getResult())) {
                // Failure
                errorReason = "Server error";
                Log.w(Const.LOG_TAG, "Wrong server response: " + pollingEvent.toString());
                completionHandler.onComplete(false, errorReason);
            }
            // Success
        }
    }

    @Override
    public void onWebRtcUp(final Context context) {
        Log.i(Const.LOG_TAG, "WebRTC is up!");
        DataChannel.Init init = new DataChannel.Init();

        // This is running in a main thread and may hang up (a WebRTC fault)!!!
        @SuppressLint("StaticFieldLeak") final AsyncTask<Void, Void, Void> asyncTask = new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... voids) {
                dataChannel = peerConnection.createDataChannel("Trick", init);
                return null;
            }

            @Override
            protected void onPostExecute(Void v) {
                if (hangupProtectionRunnable != null) {
                    handler.removeCallbacks(hangupProtectionRunnable);
                }
                // We need to send something into the data channel, otherwise it won't be initialized
                String message = "{\"textroom\":\"list\",\"transaction\":\"" + Utils.generateTransactionId() + "\"}";
                sendToDataChannel(message);
            }
        }.execute();

        // Hangup protection
        hangupProtectionRunnable = () -> {
            asyncTask.cancel(true);
            context.sendBroadcast(new Intent(Const.ACTION_CONNECTION_FAILURE));
            //LocalBroadcastManager.getInstance(context).sendBroadcast(new Intent(Const.ACTION_CONNECTION_FAILURE));
        };
        handler.postDelayed(hangupProtectionRunnable, 5000);
    }

    private void sendToDataChannel(String message) {
        Log.d(Const.LOG_TAG, "Sending message: " + message);
        ByteBuffer data = Utils.stringToByteBuffer(message);
        DataChannel.Buffer buffer = new DataChannel.Buffer(data, false);
        dataChannel.send(buffer);
    }

    public int createRoom(String roomId, String password) {
        this.roomId = roomId;
        this.password = password;

        String createMessage = "{\"textroom\":\"create\",\"is_private\":false,\"permanent\":false,\"transaction\":\"" + Utils.generateTransactionId() +
                "\",\"room\":\"" + roomId + "\",\"pin\":\"" + password + "\"}";

        sendToDataChannel(createMessage);
        synchronized (dcResultLock) {
            try {
                dcResultLock.wait();
            } catch (InterruptedException e) {
                e.printStackTrace();
                errorReason = "Interrupted";
                return Const.INTERNAL_ERROR;
            }
            if (!dcResult) {
                errorReason = "Failed to create a text room";
                return Const.SERVER_ERROR;
            }
        }
        return Const.SUCCESS;
    }

    public int joinRoom(String username, String displayName) {
        String joinMessage = "{\"textroom\":\"join\",\"room\":\"" + roomId + "\",\"username\":\"" + username + "\",\"display\":\"" + displayName +
                "\",\"pin\":\"" + password + "\", \"transaction\":\"" + Utils.generateTransactionId() + "\"}";

        sendToDataChannel(joinMessage);
        synchronized (dcResultLock) {
            try {
                dcResultLock.wait();
            } catch (InterruptedException e) {
                e.printStackTrace();
                errorReason = "Interrupted";
                return Const.INTERNAL_ERROR;
            }
            if (!dcResult) {
                errorReason = "Failed to join a text room";
                return Const.SERVER_ERROR;
            }
        }
        joined = true;
        return Const.SUCCESS;
    }

    public int sendMessage(String message, boolean ack) {
        String sendCommand = "{\"textroom\":\"message\",\"room\":\"" + roomId + "\",\"text\":\"" + message + "\",\"ack\":" + ack + "," +
                "\"transaction\":\"" + Utils.generateTransactionId() + "\"}";

        sendToDataChannel(sendCommand);
        if (ack) {
            synchronized (dcResultLock) {
                try {
                    dcResultLock.wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    errorReason = "Interrupted";
                    return Const.INTERNAL_ERROR;
                }
                if (!dcResult) {
                    errorReason = "Failed to send a message to room";
                    return Const.SERVER_ERROR;
                }
            }
        }
        return Const.SUCCESS;
    }

    @Override
    public int destroy() {
 /*       if (dataChannel != null) {
            String destroyMessage = "{\"textroom\":\"destroy\",\"room\":\"" + roomId + "\",\"permanent\":false,\"transaction\":\"" + Utils.generateTransactionId() + "\"}";
            sendToDataChannel(destroyMessage);
        }*/
        // DataChannel may be broken, so we destroy using HTTP
        destroyViaHttp();
        return Const.SUCCESS;
    }

    private int destroyViaHttp() {
        JanusMessageRequest destroyRequest = new JanusMessageRequest("", "message", sessionId, getHandleId());
        destroyRequest.setBody(new JanusMessageRequest.Body("destroy", null, roomId));
        destroyRequest.generateTransactionId();

        JSONObject response = socketHandler.emitAndWaitForAck("sendMessage", getJanusRequest(destroyRequest));
        if (JSONUtils.isEmpty(response)) {
            errorReason = "Network error";
            return Const.NETWORK_ERROR;
        }

        JanusResponse janusResponse = new JanusResponse().fromJson(response);
        if (!janusResponse.getJanus().equalsIgnoreCase("success")) {
            errorReason = "Server error";
            Log.w(Const.LOG_TAG, "Wrong server response: " + janusResponse.toString());
            return Const.SERVER_ERROR;
        }
        return Const.SUCCESS;
    }

    private JSONObject getJanusRequest(JanusJsepRequest request) {
        JSONObject requestData = new JSONObject();
        try {
            requestData.put("session", sessionId);
            requestData.put("handle", getHandleId());
            requestData.put("request", request);
        } catch (JSONException ignored) {
        }
        return requestData;
    }

    private JSONObject getJanusRequest(JanusMessageRequest request) {
        JSONObject requestData = new JSONObject();
        try {
            requestData.put("session", sessionId);
            requestData.put("handle", getHandleId());
            requestData.put("request", request);
        } catch (JSONException ignored) {
        }
        return requestData;
    }

}
