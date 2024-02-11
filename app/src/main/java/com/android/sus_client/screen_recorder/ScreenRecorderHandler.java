package com.android.sus_client.screen_recorder;

import static com.android.sus_client.utils.Constants.ERROR_KEY;
import static com.android.sus_client.utils.Constants.ERROR_REASON_KEY;
import static com.android.sus_client.utils.Constants.MAX_FILE_SIZE_KEY;
import static com.android.sus_client.utils.Constants.MAX_FILE_SIZE_REACHED_ERROR;
import static com.android.sus_client.utils.Constants.NO_SPECIFIED_MAX_SIZE;
import static com.android.sus_client.utils.Constants.ON_COMPLETE;
import static com.android.sus_client.utils.Constants.ON_COMPLETE_KEY;
import static com.android.sus_client.utils.Constants.ON_PAUSE;
import static com.android.sus_client.utils.Constants.ON_PAUSE_KEY;
import static com.android.sus_client.utils.Constants.ON_RESUME;
import static com.android.sus_client.utils.Constants.ON_RESUME_KEY;
import static com.android.sus_client.utils.Constants.ON_START;
import static com.android.sus_client.utils.Constants.ON_START_KEY;
import static com.android.sus_client.utils.Constants.SETTINGS_ERROR;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.MediaRecorder;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.ResultReceiver;
import android.text.TextUtils;
import android.util.Log;

import com.android.sus_client.annotation.RequiresApi;

import java.io.File;
import java.io.FileDescriptor;
import java.util.Locale;
import java.util.Objects;

/**
 * Created by HBiSoft on 13 Aug 2019
 * Copyright (c) 2019 . All rights reserved.
 */

public class ScreenRecorderHandler {

    private static final String TAG = "ScreenRecorderHandler";
    private final String maxFileReachedMsg = "File size max has been reached.";

    private long maxFileSize = NO_SPECIFIED_MAX_SIZE;
    private boolean hasMaxFileBeenReached = false;
    private int mScreenWidth;
    private int mScreenHeight;
    private int mScreenDensity;
    private int mResultCode;
    private Intent mResultData;
    private boolean isVideoHD;
    private boolean isAudioEnabled;
    private String path;

    private MediaProjection mMediaProjection;
    private MediaRecorder mMediaRecorder;
    private VirtualDisplay mVirtualDisplay;
    private String name;
    private int audioBitrate;
    private int audioSamplingRate;
    private static String filePath;
    private static String fileName;
    private int audioSourceAsInt;
    private int videoEncoderAsInt;
    private boolean isCustomSettingsEnabled;
    private int videoFrameRate;
    private int videoBitrate;
    private int outputFormatAsInt;
    private int orientationHint;

    public final static String BUNDLED_LISTENER = "listener";
    private Uri returnedUri = null;
    private Intent mIntent;
    private Context context;

    public ScreenRecorderHandler(Context context) {
        this.context = context;
    }

    public boolean onStartCommand(final Intent intent) {
        boolean isActionable = false;
        //Check if there was an action called
        if (intent != null && intent.getAction() != null && intent.getAction().startsWith("screen_recorder")) {
            isActionable = true;
            //If there was an action, check what action it was
            //Called when recording should be paused or resumed or start
            String mAction = intent.getAction();

            //Pause Recording
            if (mAction.equals("screen_recorder.pause")) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    pauseRecording();
                }
            }
            //Resume Recording
            else if (mAction.equals("screen_recorder.resume")) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    resumeRecording();
                }
            }
            //Stop Recording
            else if (mAction.equals("screen_recorder.stop")) {
                onDestroyService();
            }
            //Start Recording
            else {
                //Get intent extras
                hasMaxFileBeenReached = false;
                mIntent = intent;
                maxFileSize = intent.getLongExtra(MAX_FILE_SIZE_KEY, NO_SPECIFIED_MAX_SIZE);
                orientationHint = intent.getIntExtra("orientation", 400);
                mResultCode = intent.getIntExtra("code", -1);
                mResultData = intent.getParcelableExtra("data");
                mScreenWidth = intent.getIntExtra("width", 0);
                mScreenHeight = intent.getIntExtra("height", 0);

                if (intent.getStringExtra("mUri") != null) {
                    returnedUri = Uri.parse(intent.getStringExtra("mUri"));
                }

                if (mScreenHeight == 0 || mScreenWidth == 0) {
                    ScreenRecorderCodecInfo screenRecorderCodecInfo = new ScreenRecorderCodecInfo();
                    screenRecorderCodecInfo.setContext(context);
                    mScreenHeight = screenRecorderCodecInfo.getMaxSupportedHeight();
                    mScreenWidth = screenRecorderCodecInfo.getMaxSupportedWidth();
                }

                mScreenDensity = intent.getIntExtra("density", 1);
                isVideoHD = intent.getBooleanExtra("quality", true);
                isAudioEnabled = intent.getBooleanExtra("audio", true);
                path = intent.getStringExtra("path");
                name = intent.getStringExtra("fileName");
                String audioSource = intent.getStringExtra("audioSource");
                String videoEncoder = intent.getStringExtra("videoEncoder");
                videoFrameRate = intent.getIntExtra("videoFrameRate", 30);
                videoBitrate = intent.getIntExtra("videoBitrate", 40000000);

                if (audioSource != null) {
                    setAudioSourceAsInt(audioSource);
                }
                if (videoEncoder != null) {
                    setVideoEncoderAsInt(videoEncoder);
                }

                filePath = name;
                audioBitrate = intent.getIntExtra("audioBitrate", 128000);
                audioSamplingRate = intent.getIntExtra("audioSamplingRate", 44100);
                String outputFormat = intent.getStringExtra("outputFormat");
                if (outputFormat != null) {
                    setOutputFormatAsInt(outputFormat);
                }

                isCustomSettingsEnabled = intent.getBooleanExtra("enableCustomSettings", false);
                //Set notification bitrate if developer did not
                if (audioBitrate == 0) {
                    audioBitrate = 128000;
                }
                //Set notification sampling rate if developer did not
                if (audioSamplingRate == 0) {
                    audioSamplingRate = 44100;
                }

                if (returnedUri == null) {
                    if (path == null) {
                        path = String.valueOf(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES));
                    }
                }

                //Init MediaRecorder
                try {
                    initRecorder();
                } catch (Exception e) {
                    ResultReceiver receiver = intent.getParcelableExtra(ScreenRecorderHandler.BUNDLED_LISTENER);
                    Bundle bundle = new Bundle();
                    bundle.putString(ERROR_REASON_KEY, Log.getStackTraceString(e));
                    if (receiver != null) {
                        receiver.send(Activity.RESULT_OK, bundle);
                    }
                }

                //Init MediaProjection
                try {
                    initMediaProjection();
                } catch (Exception e) {
                    ResultReceiver receiver = intent.getParcelableExtra(ScreenRecorderHandler.BUNDLED_LISTENER);
                    Bundle bundle = new Bundle();
                    bundle.putString(ERROR_REASON_KEY, Log.getStackTraceString(e));
                    if (receiver != null) {
                        receiver.send(Activity.RESULT_OK, bundle);
                    }
                }

                //Init VirtualDisplay
                try {
                    initVirtualDisplay();
                } catch (Exception e) {
                    ResultReceiver receiver = intent.getParcelableExtra(ScreenRecorderHandler.BUNDLED_LISTENER);
                    Bundle bundle = new Bundle();
                    bundle.putString(ERROR_REASON_KEY, Log.getStackTraceString(e));
                    if (receiver != null) {
                        receiver.send(Activity.RESULT_OK, bundle);
                    }
                }

                // ava.lang.RuntimeException: Unable to start service com.android.sus_client.services.ForegroundService@4bc1ec7 with Intent { act=screen_recorder cmp=com.android.sus_client/.services.ForegroundService (has extras) }
                mMediaRecorder.setOnErrorListener(new MediaRecorder.OnErrorListener() {
                    @Override
                    public void onError(MediaRecorder mediaRecorder, int what, int extra) {
                        if (what == 268435556 && hasMaxFileBeenReached) {
                            // Benign error b/c recording is too short and has no frames. See SO: https://stackoverflow.com/questions/40616466/mediarecorder-stop-failed-1007
                            return;
                        }
                        ResultReceiver receiver = intent.getParcelableExtra(ScreenRecorderHandler.BUNDLED_LISTENER);
                        Bundle bundle = new Bundle();
                        bundle.putInt(ERROR_KEY, SETTINGS_ERROR);
                        bundle.putString(ERROR_REASON_KEY, String.valueOf(what));
                        if (receiver != null) {
                            receiver.send(Activity.RESULT_OK, bundle);
                        }
                    }
                });

                mMediaRecorder.setOnInfoListener(new MediaRecorder.OnInfoListener() {
                    @Override
                    public void onInfo(MediaRecorder mr, int what, int extra) {
                        if (what == MediaRecorder.MEDIA_RECORDER_INFO_MAX_FILESIZE_REACHED) {
                            hasMaxFileBeenReached = true;
                            Log.i(TAG, String.format(Locale.US, "onInfoListen what : %d | extra %d", what, extra));
                            ResultReceiver receiver = intent.getParcelableExtra(ScreenRecorderHandler.BUNDLED_LISTENER);
                            Bundle bundle = new Bundle();
                            bundle.putInt(ERROR_KEY, MAX_FILE_SIZE_REACHED_ERROR);
                            bundle.putString(ERROR_REASON_KEY, maxFileReachedMsg);
                            if (receiver != null) {
                                receiver.send(Activity.RESULT_OK, bundle);
                            }
                        }
                    }
                });

                //Start Recording
                try {
                    mMediaRecorder.start();
                    ResultReceiver receiver = intent.getParcelableExtra(ScreenRecorderHandler.BUNDLED_LISTENER);
                    Bundle bundle = new Bundle();
                    bundle.putInt(ON_START_KEY, ON_START);
                    if (receiver != null) {
                        receiver.send(Activity.RESULT_OK, bundle);
                    }
                } catch (Exception e) {
                    // From the tests I've done, this can happen if another application is using the mic or if an unsupported video encoder was selected
                    ResultReceiver receiver = intent.getParcelableExtra(ScreenRecorderHandler.BUNDLED_LISTENER);
                    Bundle bundle = new Bundle();
                    bundle.putInt(ERROR_KEY, SETTINGS_ERROR);
                    bundle.putString(ERROR_REASON_KEY, Log.getStackTraceString(e));
                    if (receiver != null) {
                        receiver.send(Activity.RESULT_OK, bundle);
                    }
                }
            }
        }
        return isActionable;
    }

    //Pause Recording
    @RequiresApi(api = Build.VERSION_CODES.N)
    public void pauseRecording() {
        mMediaRecorder.pause();
        ResultReceiver receiver = mIntent.getParcelableExtra(ScreenRecorderHandler.BUNDLED_LISTENER);
        Bundle bundle = new Bundle();
        bundle.putString(ON_PAUSE_KEY, ON_PAUSE);
        if (receiver != null) {
            receiver.send(Activity.RESULT_OK, bundle);
        }
    }

    //Resume Recording
    @RequiresApi(api = Build.VERSION_CODES.N)
    public void resumeRecording() {
        mMediaRecorder.resume();
        ResultReceiver receiver = mIntent.getParcelableExtra(ScreenRecorderHandler.BUNDLED_LISTENER);
        Bundle bundle = new Bundle();
        bundle.putString(ON_RESUME_KEY, ON_RESUME);
        if (receiver != null) {
            receiver.send(Activity.RESULT_OK, bundle);
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public void onDestroyService() {
        resetAll();
        callOnComplete();
    }

    //Set output format as int based on what developer has provided
    //It is important to provide one of the following and nothing else.
    private void setOutputFormatAsInt(String outputFormat) {
        switch (outputFormat) {
            case "DEFAULT":
                outputFormatAsInt = 0;
                break;
            case "THREE_GPP":
                outputFormatAsInt = 1;
                break;
            case "AMR_NB":
                outputFormatAsInt = 3;
                break;
            case "AMR_WB":
                outputFormatAsInt = 4;
                break;
            case "AAC_ADTS":
                outputFormatAsInt = 6;
                break;
            case "MPEG_2_TS":
                outputFormatAsInt = 8;
                break;
            case "WEBM":
                outputFormatAsInt = 9;
                break;
            case "OGG":
                outputFormatAsInt = 11;
                break;
            case "MPEG_4":
            default:
                outputFormatAsInt = 2;
        }
    }

    //Set video encoder as int based on what developer has provided
    //It is important to provide one of the following and nothing else.
    private void setVideoEncoderAsInt(String encoder) {
        switch (encoder) {
            case "DEFAULT":
                videoEncoderAsInt = 0;
                break;
            case "H263":
                videoEncoderAsInt = 1;
                break;
            case "H264":
                videoEncoderAsInt = 2;
                break;
            case "MPEG_4_SP":
                videoEncoderAsInt = 3;
                break;
            case "VP8":
                videoEncoderAsInt = 4;
                break;
            case "HEVC":
                videoEncoderAsInt = 5;
                break;
        }
    }

    //Set audio source as int based on what developer has provided
    //It is important to provide one of the following and nothing else.
    private void setAudioSourceAsInt(String audioSource) {
        switch (audioSource) {
            case "DEFAULT":
                audioSourceAsInt = 0;
                break;
            case "MIC":
                audioSourceAsInt = 1;
                break;
            case "VOICE_UPLINK":
                audioSourceAsInt = 2;
                break;
            case "VOICE_DOWNLINK":
                audioSourceAsInt = 3;
                break;
            case "VOICE_CALL":
                audioSourceAsInt = 4;
                break;
            case "CAMCODER":
                audioSourceAsInt = 5;
                break;
            case "VOICE_RECOGNITION":
                audioSourceAsInt = 6;
                break;
            case "VOICE_COMMUNICATION":
                audioSourceAsInt = 7;
                break;
            case "REMOTE_SUBMIX":
                audioSourceAsInt = 8;
                break;
            case "UNPROCESSED":
                audioSourceAsInt = 9;
                break;
            case "VOICE_PERFORMANCE":
                audioSourceAsInt = 10;
                break;
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void initMediaProjection() {
        mMediaProjection = ((MediaProjectionManager) Objects.requireNonNull(context.getSystemService(Context.MEDIA_PROJECTION_SERVICE))).getMediaProjection(mResultCode, mResultData);
    }

    //Return the output file path as string
    public static String getFilePath() {
        return filePath;
    }

    //Return the name of the output file
    public static String getFileName() {
        return fileName;
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void initRecorder() throws Exception {
        String curTime = String.valueOf(System.currentTimeMillis());
        String videoQuality = (isVideoHD ? "HD_" : "SD_");
        if (TextUtils.isEmpty(name)) {
            name = "ScreenCapture_" + videoQuality + curTime;
        }

        filePath = path + "/" + name + ".mp4";
        fileName = name + ".mp4";
        System.out.println("------------------------->");
        System.out.println(filePath);
        File file = new File(filePath);
        System.out.println("Is File exists : " + file.exists());
        if (!file.exists()) file.createNewFile();

        mMediaRecorder = new MediaRecorder();

        if (isAudioEnabled) {
            mMediaRecorder.setAudioSource(audioSourceAsInt);
        }
        mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE);
        mMediaRecorder.setOutputFormat(outputFormatAsInt);

        if (orientationHint != 400) {
            mMediaRecorder.setOrientationHint(orientationHint);
        }

        if (isAudioEnabled) {
            mMediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
            mMediaRecorder.setAudioEncodingBitRate(audioBitrate);
            mMediaRecorder.setAudioSamplingRate(audioSamplingRate);
        }

        mMediaRecorder.setVideoEncoder(videoEncoderAsInt);


        if (returnedUri != null) {
            try {
                ContentResolver contentResolver = context.getContentResolver();
                FileDescriptor inputPFD = Objects.requireNonNull(contentResolver.openFileDescriptor(returnedUri, "rw")).getFileDescriptor();
                mMediaRecorder.setOutputFile(inputPFD);
            } catch (Exception e) {
                ResultReceiver receiver = mIntent.getParcelableExtra(ScreenRecorderHandler.BUNDLED_LISTENER);
                Bundle bundle = new Bundle();
                bundle.putString(ERROR_REASON_KEY, Log.getStackTraceString(e));
                if (receiver != null) {
                    receiver.send(Activity.RESULT_OK, bundle);
                }
            }
        } else {
            mMediaRecorder.setOutputFile(filePath);
        }
        mMediaRecorder.setVideoSize(mScreenWidth, mScreenHeight);

        if (!isCustomSettingsEnabled) {
            if (!isVideoHD) {
                mMediaRecorder.setVideoEncodingBitRate(12000000);
                mMediaRecorder.setVideoFrameRate(30);
            } else {
                mMediaRecorder.setVideoEncodingBitRate(5 * mScreenWidth * mScreenHeight);
                mMediaRecorder.setVideoFrameRate(60); //after setVideoSource(), setOutFormat()
            }
        } else {
            mMediaRecorder.setVideoEncodingBitRate(videoBitrate);
            mMediaRecorder.setVideoFrameRate(videoFrameRate);
        }

        // Catch approaching file limit
        if (maxFileSize > NO_SPECIFIED_MAX_SIZE) {
            mMediaRecorder.setMaxFileSize(maxFileSize); // in bytes
        }

        mMediaRecorder.prepare();
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void initVirtualDisplay() {
        mVirtualDisplay = mMediaProjection.createVirtualDisplay(TAG, mScreenWidth, mScreenHeight, mScreenDensity, DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR, mMediaRecorder.getSurface(), null, null);
    }

    private void callOnComplete() {
        if (mIntent != null) {
            ResultReceiver receiver = mIntent.getParcelableExtra(ScreenRecorderHandler.BUNDLED_LISTENER);
            Bundle bundle = new Bundle();
            bundle.putString(ON_COMPLETE_KEY, ON_COMPLETE);
            if (receiver != null) {
                receiver.send(Activity.RESULT_OK, bundle);
            }
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void resetAll() {
        if (mVirtualDisplay != null) {
            mVirtualDisplay.release();
            mVirtualDisplay = null;
        }
        if (mMediaRecorder != null) {
            mMediaRecorder.setOnErrorListener(null);
            mMediaRecorder.reset();
        }
        if (mMediaProjection != null) {
            mMediaProjection.stop();
            mMediaProjection = null;
        }
    }

}