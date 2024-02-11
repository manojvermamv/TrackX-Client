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
import android.content.Intent;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaCodecList;
import android.media.MediaFormat;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.AsyncTask;
import android.os.Build;
import android.util.Log;
import android.util.Range;
import android.view.Surface;

import net.majorkernelpanic.streaming.rtp.AbstractPacketizer;
import net.majorkernelpanic.streaming.rtp.H264Packetizer;
import net.majorkernelpanic.streaming.rtp.MediaCodecInputStream;

import java.io.IOException;
import java.net.InetAddress;
import java.util.HashMap;
import java.util.Map;

public class ScreenSharingHandler {

    private static final String MIME_TYPE_VIDEO = "video/avc";
    private static final String GOOGLE_AVC_ENCODER_NAME = "OMX.google.h264.encoder";

    private int mScreenDensity;
    private int mScreenWidth;
    private int mScreenHeight;

    private boolean mRecordAudio;
    private String mRtpHost;
    private int mRtpAudioPort;
    private int mRtpVideoPort;
    private int mVideoFrameRate;
    private int mVideoBitrate;

    private MediaProjectionManager mProjectionManager;
    private MediaProjection mMediaProjection;
    private MediaProjection.Callback mMediaProjectionCallback;
    private VirtualDisplay mVirtualDisplay;

    private MediaCodec mMediaCodec;
    private Surface mInputSurface;

    private AbstractPacketizer mPacketizer;

    private ScreenSharingCallback sharingCallback;

    public static final String ACTION_SET_METRICS = "metrics";
    public static final String ACTION_CONFIGURE = "configure";
    public static final String ACTION_REQUEST_SHARING = "request";
    public static final String ACTION_START_SHARING = "start";
    public static final String ACTION_STOP_SHARING = "stop";
    public static final String ATTR_SCREEN_WIDTH = "screenWidth";
    public static final String ATTR_SCREEN_HEIGHT = "screenHeight";
    public static final String ATTR_SCREEN_DENSITY = "screenDensity";
    public static final String ATTR_AUDIO = "audio";
    public static final String ATTR_FRAME_RATE = "frameRate";
    public static final String ATTR_BITRATE = "bitrate";
    public static final String ATTR_HOST = "host";
    public static final String ATTR_AUDIO_PORT = "audioPort";
    public static final String ATTR_VIDEO_PORT = "videoPort";
    public static final String ATTR_RESULT_CODE = "resultCode";
    public static final String ATTR_DATA = "data";
    public static final String ATTR_DESTROY_MEDIA_PROJECTION = "destroyMediaProjection";

    public ScreenSharingHandler(Context context, ScreenSharingCallback screenSharingCallback) {
        Log.d(Const.LOG_TAG, "ScreenSharingService created");
        sharingCallback = screenSharingCallback;
        mPacketizer = new H264Packetizer();
        mProjectionManager = (MediaProjectionManager) context.getSystemService(Context.MEDIA_PROJECTION_SERVICE);
    }

    public boolean onStartCommand(final Intent intent) {
        boolean isActionable = false;
        //Check if there was an action called
        if (intent != null && intent.getAction() != null/* && intent.getAction().startsWith("screen_recorder")*/) {
            String action = intent.getAction();
            Log.d(Const.LOG_TAG, "ScreenSharingService got command: " + action);
            if (action.equals(ACTION_SET_METRICS)) {
                isActionable = true;
                mScreenWidth = intent.getIntExtra(ATTR_SCREEN_WIDTH, 0);
                mScreenHeight = intent.getIntExtra(ATTR_SCREEN_HEIGHT, 0);
                mScreenDensity = intent.getIntExtra(ATTR_SCREEN_DENSITY, 0);
                Log.d(Const.LOG_TAG, "ScreenSharingService: width=" + mScreenWidth + ", height=" + mScreenHeight + ", density=" + mScreenDensity);

            } else if (action.equals(ACTION_CONFIGURE)) {
                isActionable = true;
                configure(intent.getBooleanExtra(ATTR_AUDIO, false),
                        intent.getIntExtra(ATTR_FRAME_RATE, 0),
                        intent.getIntExtra(ATTR_BITRATE, 0),
                        intent.getStringExtra(ATTR_HOST),
                        intent.getIntExtra(ATTR_AUDIO_PORT, 0),
                        intent.getIntExtra(ATTR_VIDEO_PORT, 0));

            } else if (action.equals(ACTION_REQUEST_SHARING)) {
                isActionable = true;
                requestSharing();

            } else if (action.equals(ACTION_START_SHARING)) {
                isActionable = true;
                int resultCode = intent.getIntExtra(ATTR_RESULT_CODE, 0);
                Intent data = intent.getParcelableExtra(ATTR_DATA);
                startSharing(resultCode, data);

            } else if (action.equals(ACTION_STOP_SHARING)) {
                isActionable = true;
                boolean destroyMediaProjection = intent.getBooleanExtra(ATTR_DESTROY_MEDIA_PROJECTION, false);
                stopSharing(destroyMediaProjection);
            }
        }
        return isActionable;
    }

    private void configure(boolean audio, int videoFrameRate, int videoBitRate, String host, int audioPort, int videoPort) {
        mVideoFrameRate = videoFrameRate;
        mVideoBitrate = videoBitRate;
        Log.d(Const.LOG_TAG, "ScreenSharingService: frameRate=" + mVideoFrameRate + ", bitrate=" + mVideoBitrate);

        // This is executed in the background because the operation requires host resolution
        new AsyncTask<Void, Void, Void>() {

            @Override
            protected Void doInBackground(Void... voids) {
                try {
                    // Here I set RTCP port to videoPort+1 (conventional), but RTCP is not used, and 0 or -1 cause errors in libstreaming
                    mPacketizer.setDestination(InetAddress.getByName(host), videoPort, videoPort + 1);
                    mPacketizer.setTimeToLive(64);

                } catch (Exception e) {
                    // We should not be here because configure() is called after successful connection to the host
                    e.printStackTrace();
                }
                return null;
            }

        }.execute();

    }

    private void requestSharing() {
        if (!initRecorder()) {
            // Some initialization error, report to activity
            String sharing_error = "Attempt to share the screen is failed! Looks like the app is not compatible with your device, sorry for that!";
            sharingCallback.onSharingFailed(sharing_error);
            //Intent intent = new Intent(Const.ACTION_SCREEN_SHARING_FAILED);
            //intent.putExtra(Const.EXTRA_MESSAGE, sharing_error);
            //LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
            return;
        }
        tryShareScreen();
    }


    private void tryShareScreen() {
        if (mMediaProjection == null) {
            sharingCallback.onSharingPermissionNeeded();
            //LocalBroadcastManager.getInstance(this).sendBroadcast(new Intent(Const.ACTION_SCREEN_SHARING_PERMISSION_NEEDED));
            return;
        }
        mVirtualDisplay = createVirtualDisplay();
        mMediaCodec.start();
        startSending();
    }

    private void startSharing(int resultCode, Intent data) {
        mMediaProjection = mProjectionManager.getMediaProjection(resultCode, data);
        mMediaProjectionCallback = new MediaProjection.Callback() {
            @Override
            public void onStop() {
                super.onStop();
                stopSharing(false);
                sharingCallback.onSharingStopped();
                //LocalBroadcastManager.getInstance(service).sendBroadcast(new Intent(Const.ACTION_SCREEN_SHARING_STOP));
            }
        };
        mMediaProjection.registerCallback(mMediaProjectionCallback, null);
        mVirtualDisplay = createVirtualDisplay();
        mMediaCodec.start();
        startSending();

    }

    public void stopSharing(boolean destroyMediaProjection) {
        try {
            mPacketizer.stop();
            mMediaCodec.stop();
            Log.v(Const.LOG_TAG, "Stopping Recording");
            stopScreenSharing(destroyMediaProjection);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void stopScreenSharing(boolean destroyMediaProjection) {
        if (mVirtualDisplay == null) {
            return;
        }
        mPacketizer.stop();
        mVirtualDisplay.release();
        mMediaCodec.release();
        if (destroyMediaProjection) {
            destroyMediaProjection();
        }
    }

    private void destroyMediaProjection() {
        if (mMediaProjection != null) {
            mMediaProjection.unregisterCallback(mMediaProjectionCallback);
            mMediaProjection.stop();
            mMediaProjection = null;
        }
        Log.i(Const.LOG_TAG, "MediaProjection Stopped");
    }

    private void startSending() {
        MediaCodecInputStream mcis = new MediaCodecInputStream(mMediaCodec);
        mPacketizer.setInputStream(mcis);
        mcis.setH264Packetizer((H264Packetizer) mPacketizer);
        mPacketizer.start();
        sharingCallback.onSharingStarted();
        //LocalBroadcastManager.getInstance(this).sendBroadcast(new Intent(Const.ACTION_SCREEN_SHARING_START));
    }

    private VirtualDisplay createVirtualDisplay() {
        return mMediaProjection.createVirtualDisplay("MainActivity",
                mScreenWidth, mScreenHeight, mScreenDensity,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                mInputSurface, null /*Callbacks*/, null
                /*Handler*/);
    }

    public static MediaCodec createAvcEncoder() {
        Map<String, MediaCodecInfo> avcEncoders = queryAvcEncoders();
        MediaCodec mediaCodec = null;

        // Prefer Google (software) AVC encoder if available
        final boolean USE_GOOGLE_ENCODER = false;
        if (USE_GOOGLE_ENCODER && avcEncoders.containsKey(GOOGLE_AVC_ENCODER_NAME)) {
            try {
                mediaCodec = MediaCodec.createByCodecName(GOOGLE_AVC_ENCODER_NAME);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        try {
            if (mediaCodec == null) {
                // Use best match for the codec (usually a manufacturer-specific hardware codec)
                mediaCodec = MediaCodec.createEncoderByType(MIME_TYPE_VIDEO);
            }
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }

        return mediaCodec;
    }

    private boolean initRecorder() {
        mMediaCodec = createAvcEncoder();

        try {
            logCodecCapabilities(mMediaCodec);
        } catch (Exception e) {
        }

        alignScreenForCodec(mMediaCodec);

        MediaFormat mediaFormat = MediaFormat.createVideoFormat(MIME_TYPE_VIDEO, mScreenWidth, mScreenHeight);
        mediaFormat.setInteger(MediaFormat.KEY_BIT_RATE, mVideoBitrate);
        mediaFormat.setInteger(MediaFormat.KEY_FRAME_RATE, mVideoFrameRate);
        mediaFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface);
        mediaFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1);
        // This method call may throw CodecException!
        try {
            mMediaCodec.configure(mediaFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
        } catch (Exception e) {
            Log.e(Const.LOG_TAG, "Failed to configure codec with parameters: screenWidth=" + mScreenWidth +
                    ", screenHeight=" + mScreenHeight + ", bitrate=" + mVideoBitrate + ", frameRate=" + mVideoFrameRate +
                    ", colorFormat=" + MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface + ", frameInterval=1");
            e.printStackTrace();
            return false;
        }

        MediaCodecInfo.CodecProfileLevel[] profileLevels = mMediaCodec.getCodecInfo().getCapabilitiesForType(MIME_TYPE_VIDEO).profileLevels;

        mInputSurface = mMediaCodec.createInputSurface();
        return true;
    }

    private static Map<String, MediaCodecInfo> queryAvcEncoders() {
        MediaCodecList codecList = new MediaCodecList(MediaCodecList.ALL_CODECS);
        MediaCodecInfo[] infoList = codecList.getCodecInfos();
        Map<String, MediaCodecInfo> avcEncoders = new HashMap<>();

        for (int i = 0; i < infoList.length; i++) {
            MediaCodecInfo codecInfo = infoList[i];
            if (!codecInfo.isEncoder()) {
                continue;
            }

            String[] types = codecInfo.getSupportedTypes();
            for (int j = 0; j < types.length; j++) {
                if (types[j].equalsIgnoreCase(MIME_TYPE_VIDEO)) {
                    avcEncoders.put(codecInfo.getName(), codecInfo);
                    Log.i(Const.LOG_TAG, "Found AVC encoder: " + codecInfo.getName());
                    break;
                }
            }
        }
        return avcEncoders;
    }

    public static void logCodecCapabilities(MediaCodec mediaCodec) {
        MediaCodecInfo info = mediaCodec.getCodecInfo();

        String infoStr = "name:" + info.getName();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            infoStr += " isVendor:" + info.isVendor() +
                    " isSoftwareOnly:" + info.isSoftwareOnly() +
                    " isHardwareAccelerated:" + info.isHardwareAccelerated();
        }

        MediaCodecInfo.CodecCapabilities c = info.getCapabilitiesForType(MIME_TYPE_VIDEO);

        MediaCodecInfo.VideoCapabilities vc = c.getVideoCapabilities();
        Range<Integer> bitrateRange = vc.getBitrateRange();
        if (bitrateRange != null) {
            infoStr += " bitrateRange:" + bitrateRange.toString();
        }
        Range<Integer> widthRange = vc.getSupportedWidths();
        if (widthRange != null) {
            infoStr += " widthRange:" + widthRange.toString();
        }
        Range<Integer> heightRange = vc.getSupportedHeights();
        if (heightRange != null) {
            infoStr += " heightRange:" + heightRange.toString();
        }
        infoStr += " widthAlignment:" + vc.getWidthAlignment();
        infoStr += " heightAlignment:" + vc.getHeightAlignment();

        int[] fmts = c.colorFormats;
        MediaCodecInfo.CodecProfileLevel[] levels = c.profileLevels;
        infoStr += " colorFormats:{";
        for (int i = 0; i < fmts.length; i++) {
            infoStr += "" + fmts[i];
            if (i < fmts.length - 1) {
                infoStr += ",";
            }
        }
        infoStr += "} profileLevels:{";
        for (int i = 0; i < levels.length; i++) {
            infoStr += "" + levels[i].profile + "/" + levels[i].level;
            if (i < levels.length - 1) {
                infoStr += ",";
            }
        }
        infoStr += "}";

        Log.d(Const.LOG_TAG, infoStr);
    }

    private void alignScreenForCodec(MediaCodec mediaCodec) {
        try {
            MediaCodecInfo.VideoCapabilities vc =
                    mediaCodec.getCodecInfo()
                            .getCapabilitiesForType(MIME_TYPE_VIDEO)
                            .getVideoCapabilities();
            int widthAlignment = vc.getWidthAlignment();
            int heightAlignment = vc.getHeightAlignment();
            mScreenWidth &= -widthAlignment;
            mScreenHeight &= -heightAlignment;
            Log.d(Const.LOG_TAG, "ScreenSharingService: aligned video size: width=" + mScreenWidth + ", height=" + mScreenHeight);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public interface ScreenSharingCallback {
        void onSharingStarted();

        void onSharingStopped();

        void onSharingPermissionNeeded();

        void onSharingFailed(String error);

        void onConnectionFailure();
        //intentFilter.addAction(Const.ACTION_CONNECTION_FAILURE);
    }

}