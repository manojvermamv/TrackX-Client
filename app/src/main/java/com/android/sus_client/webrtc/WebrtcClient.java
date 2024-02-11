package com.android.sus_client.webrtc;

import static com.android.sus_client.webrtc.WebrtcRepository.iceCandidateToString;
import static com.android.sus_client.webrtc.utils.DataModelType.Answer;
import static com.android.sus_client.webrtc.utils.DataModelType.IceCandidates;
import static com.android.sus_client.webrtc.utils.DataModelType.Offer;

import android.content.Context;
import android.content.Intent;
import android.media.projection.MediaProjection;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.WindowManager;

import com.android.sus_client.webrtc.utils.DataModel;

import org.webrtc.DefaultVideoDecoderFactory;
import org.webrtc.DefaultVideoEncoderFactory;
import org.webrtc.EglBase;
import org.webrtc.IceCandidate;
import org.webrtc.MediaConstraints;
import org.webrtc.MediaStream;
import org.webrtc.PeerConnection;
import org.webrtc.PeerConnectionFactory;
import org.webrtc.ScreenCapturerAndroid;
import org.webrtc.SessionDescription;
import org.webrtc.SurfaceTextureHelper;
import org.webrtc.SurfaceViewRenderer;
import org.webrtc.VideoCapturer;
import org.webrtc.VideoSource;
import org.webrtc.VideoTrack;

import java.util.Collections;
import java.util.List;

public class WebrtcClient {

    private static final String localTrackId = "local_track";
    private static final String localStreamId = "local_stream";

    private final Context context;
    private PeerConnection.Observer observer;
    private SurfaceViewRenderer localSurfaceView;
    private Listener listener;
    private Intent permissionIntent;

    private PeerConnection peerConnection;
    private final EglBase.Context eglBaseContext = EglBase.create().getEglBaseContext();
    private PeerConnectionFactory peerConnectionFactory = null;
    private final MediaConstraints mediaConstraint = new MediaConstraints();
    private final List<PeerConnection.IceServer> iceServer = Collections.singletonList(
            new PeerConnection.IceServer("turn:openrelay.metered.ca:443?transport=tcp",
                    "openrelayproject", "openrelayproject")
    );

    private VideoCapturer screenCapturer;

    private MediaStream localStream;

    public WebrtcClient(Context context) {
        this.context = context;
        initPeerConnectionFactory(context);
    }

    public void setListener(Listener listener) {
        this.listener = listener;
    }

    public void initializeWebrtcClient(SurfaceViewRenderer view, PeerConnection.Observer observer) {
        this.observer = observer;
        peerConnection = createPeerConnection(observer);
        initSurfaceView(view);
    }

    public void setPermissionIntent(Intent intent) {
        this.permissionIntent = intent;
    }

    private void initSurfaceView(SurfaceViewRenderer view) {
        this.localSurfaceView = view;
        view.setMirror(false);
        view.setEnableHardwareScaler(true);
        view.init(eglBaseContext, null);
    }

    public void startScreenCapturing(SurfaceViewRenderer view) {
        DisplayMetrics displayMetrics = new DisplayMetrics();
        WindowManager windowsManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        windowsManager.getDefaultDisplay().getMetrics(displayMetrics);

        if (peerConnectionFactory == null)
            peerConnectionFactory = createPeerConnectionFactory();

        SurfaceTextureHelper surfaceTextureHelper = SurfaceTextureHelper.create(
                Thread.currentThread().getName(), eglBaseContext);

        VideoSource localVideoSource = peerConnectionFactory.createVideoSource(false);
        screenCapturer = createScreenCapturer();
        screenCapturer.initialize(surfaceTextureHelper, context, localVideoSource.getCapturerObserver());
        screenCapturer.startCapture(displayMetrics.widthPixels, displayMetrics.heightPixels, 15);

        VideoTrack localVideoTrack = peerConnectionFactory.createVideoTrack(localTrackId + "_video", localVideoSource);
        localVideoTrack.addSink(view);
        localStream = peerConnectionFactory.createLocalMediaStream(localStreamId);
        localStream.addTrack(localVideoTrack);
        peerConnection.addStream(localStream);
    }

    private VideoCapturer createScreenCapturer() {
        return new ScreenCapturerAndroid(permissionIntent, new MediaProjection.Callback() {
            @Override
            public void onStop() {
                super.onStop();
                Log.d("TAG", "onStop: stopped screen casting permission");
            }
        });
    }

    private void initPeerConnectionFactory(Context application) {
        PeerConnectionFactory.InitializationOptions options = PeerConnectionFactory.InitializationOptions.builder(application)
                .setEnableInternalTracer(true)
                .setFieldTrials("WebRTC-H264HighProfile/Enabled/")
                .createInitializationOptions();
        PeerConnectionFactory.initialize(options);
    }

    private PeerConnectionFactory createPeerConnectionFactory() {
        return PeerConnectionFactory.builder()
                .setVideoDecoderFactory(new DefaultVideoDecoderFactory(eglBaseContext))
                .setVideoEncoderFactory(new DefaultVideoEncoderFactory(eglBaseContext, true, true))
                .setOptions(new PeerConnectionFactory.Options() {{
                    disableEncryption = false;
                    disableNetworkMonitor = false;
                }})
                .createPeerConnectionFactory();
    }

    private PeerConnection createPeerConnection(PeerConnection.Observer observer) {
        if (peerConnectionFactory == null)
            peerConnectionFactory = createPeerConnectionFactory();
        return peerConnectionFactory.createPeerConnection(iceServer, observer);
    }

    public void call() {
        peerConnection.createOffer(new MySdpObserver() {
            @Override
            public void onCreateSuccess(SessionDescription desc) {
                super.onCreateSuccess(desc);
                peerConnection.setLocalDescription(new MySdpObserver() {
                    @Override
                    public void onSetSuccess() {
                        super.onSetSuccess();
                        listener.onTransferEventToSocket(new DataModel(Offer, desc.description));
                    }
                }, desc);
            }
        }, mediaConstraint);
    }

    public void answer() {
        peerConnection.createAnswer(new MySdpObserver() {
            @Override
            public void onCreateSuccess(SessionDescription desc) {
                super.onCreateSuccess(desc);
                peerConnection.setLocalDescription(new MySdpObserver() {
                    @Override
                    public void onSetSuccess() {
                        super.onSetSuccess();
                        listener.onTransferEventToSocket(new DataModel(Answer, desc.description));
                    }
                }, desc);
            }
        }, mediaConstraint);
    }

    public void onRemoteSessionReceived(SessionDescription sessionDescription) {
        peerConnection.setRemoteDescription(new MySdpObserver(), sessionDescription);
    }

    public void addIceCandidate(IceCandidate iceCandidate) {
        peerConnection.addIceCandidate(iceCandidate);
    }

    public void sendIceCandidate(IceCandidate candidate) {
        addIceCandidate(candidate);
        listener.onTransferEventToSocket(new DataModel(IceCandidates, iceCandidateToString(candidate)));
    }

    public void closeConnection() {
        try {
            screenCapturer.stopCapture();
            screenCapturer.dispose();
            localStream.dispose();
            peerConnection.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void restart() {
        closeConnection();
        localSurfaceView.clearImage();
        localSurfaceView.release();
        initializeWebrtcClient(localSurfaceView, observer);
    }

    public interface Listener {
        void onTransferEventToSocket(DataModel data);
    }
}