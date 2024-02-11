package com.android.sus_client.webrtc;

import android.content.Intent;
import android.util.Log;

import com.android.sus_client.webrtc.utils.DataModel;
import com.android.sus_client.webrtc.utils.DataModelType;

import org.webrtc.IceCandidate;
import org.webrtc.MediaStream;
import org.webrtc.PeerConnection;
import org.webrtc.SessionDescription;
import org.webrtc.SurfaceViewRenderer;

public class WebrtcRepository implements WebrtcClient.Listener {

    private final WebrtcClient webrtcClient;
    private Listener listener;

    public WebrtcRepository(WebrtcClient webrtcClient) {
        this.webrtcClient = webrtcClient;
    }

    public void init(Listener listener, SurfaceViewRenderer surfaceView) {
        this.listener = listener;
        initWebrtcClient(surfaceView);
    }

    public void sendScreenShareConnection(Intent intent, SurfaceViewRenderer surfaceView) {
        webrtcClient.setPermissionIntent(intent);
        webrtcClient.startScreenCapturing(surfaceView);
        listener.onSendMessageToSocket(new DataModel(DataModelType.StartStreaming));
    }

    public void sendCallEndedToOtherPeer() {
        listener.onSendMessageToSocket(new DataModel(DataModelType.EndCall));
    }

    public void startCall() {
        webrtcClient.call();
    }

    public void restartRepository() {
        webrtcClient.restart();
    }

    public void onDestroy() {
        webrtcClient.closeConnection();
    }

    private void initWebrtcClient(SurfaceViewRenderer surfaceView) {
        listener.onSendMessageToSocket(new DataModel(DataModelType.SignIn));

        webrtcClient.setListener(this);
        webrtcClient.initializeWebrtcClient(surfaceView,
                new MyPeerObserver() {
                    @Override
                    public void onIceCandidate(IceCandidate p0) {
                        super.onIceCandidate(p0);
                        if (p0 != null) {
                            webrtcClient.sendIceCandidate(p0);
                        }
                    }

                    @Override
                    public void onConnectionChange(PeerConnection.PeerConnectionState newState) {
                        super.onConnectionChange(newState);
                        Log.d("TAG", "onConnectionChange: " + newState);
                        if (newState == PeerConnection.PeerConnectionState.CONNECTED) {
                            listener.onConnectionConnected();
                        }
                    }

                    @Override
                    public void onAddStream(MediaStream p0) {
                        super.onAddStream(p0);
                        Log.d("TAG", "onAddStream: " + p0);
                        if (p0 != null) {
                            listener.onRemoteStreamAdded(p0);
                        }
                    }
                });
    }

    public void onNewMessageReceived(DataModel model) {
        switch (model.getType()) {
            case StartStreaming:
                listener.onConnectionRequestReceived();
                break;
            case EndCall:
                listener.onCallEndReceived();
                break;
            case Offer:
                SessionDescription sessionDescription = new SessionDescription(SessionDescription.Type.OFFER, model.getData());
                webrtcClient.onRemoteSessionReceived(sessionDescription);
                webrtcClient.answer();
                break;
            case Answer:
                sessionDescription = new SessionDescription(SessionDescription.Type.ANSWER, model.getData());
                webrtcClient.onRemoteSessionReceived(sessionDescription);
                break;
            case IceCandidates:
                try {
                    webrtcClient.addIceCandidate(stringToIceCandidate(model.getData()));
                } catch (Exception e) {
                    e.printStackTrace();
                }
                break;
        }
    }

    @Override
    public void onTransferEventToSocket(DataModel data) {
        listener.onSendMessageToSocket(data);
    }

    public interface Listener {
        void onConnectionRequestReceived();

        void onConnectionConnected();

        void onCallEndReceived();

        void onRemoteStreamAdded(MediaStream stream);

        void onSendMessageToSocket(DataModel data);
    }

    public static IceCandidate stringToIceCandidate(String data) {
        final String delimiter = "@#<>#@";
        String[] extra = data.split(delimiter);
        String id = extra[0];
        int label = Integer.parseInt(extra[1]);
        String candidate = extra[2];
        return new IceCandidate(id, label, candidate);
    }

    public static String iceCandidateToString(IceCandidate iceCandidate) {
        final String delimiter = "@#<>#@";
        String id = iceCandidate.sdpMid;
        int label = iceCandidate.sdpMLineIndex;
        String candidate = iceCandidate.sdp;
        return id + delimiter + label + delimiter + candidate;
    }

}