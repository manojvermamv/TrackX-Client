package com.android.sus_client.screen_recorder;

import static com.android.sus_client.utils.Constants.ERROR_KEY;
import static com.android.sus_client.utils.Constants.ERROR_REASON_KEY;
import static com.android.sus_client.utils.Constants.GENERAL_ERROR;
import static com.android.sus_client.utils.Constants.MAX_FILE_SIZE_KEY;
import static com.android.sus_client.utils.Constants.ON_COMPLETE_KEY;
import static com.android.sus_client.utils.Constants.ON_PAUSE_KEY;
import static com.android.sus_client.utils.Constants.ON_RESUME_KEY;
import static com.android.sus_client.utils.Constants.ON_START_KEY;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.ResultReceiver;
import android.text.TextUtils;
import android.util.Log;

import com.android.sus_client.annotation.RequiresApi;
import com.android.sus_client.commonutility.Countdown;
import com.android.sus_client.commonutility.cache.RootFilesManager;
import com.android.sus_client.services.ForegroundService;

import java.io.File;

/**
 * Created by HBiSoft on 13 Aug 2019
 * Copyright (c) 2019 . All rights reserved.
 */

@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
public class ScreenRecorder implements MyListener {

    private final Context context;
    private FileObserver observer;
    private final ScreenRecorderListener screenRecorderListener;
    private ScreenRecorderConfig config;
    private final Handler mainHandler;

    boolean wasOnErrorCalled = false;
    private Intent service;
    private boolean isPaused = false;
    private boolean isRecording = false;

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public ScreenRecorder(Context context, ScreenRecorderListener listener) {
        this(context, listener, null);
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public ScreenRecorder(Context context, ScreenRecorderListener listener, ScreenRecorderConfig config) {
        this.context = context.getApplicationContext();
        this.screenRecorderListener = listener;
        this.mainHandler = new Handler(context.getMainLooper());
        if (config != null) setConfig(config);
    }

    Uri mUri;
    boolean mWasUriSet = false;

    @RequiresApi(api = Build.VERSION_CODES.Q)
    public void setOutputUri(Uri uri) {
        mWasUriSet = true;
        mUri = uri;
    }

    public boolean wasUriSet() {
        return mWasUriSet;
    }

    // WILL IMPLEMENT THIS AT A LATER STAGE
    // DEVELOPERS ARE WELCOME TO LOOK AT THIS AND CREATE A PULL REQUEST
    /*Mute microphone*/
    /*public void setMicMuted(boolean state){
        if (context!=null) {
            try {
                ((AudioManager)context.getSystemService(Context.AUDIO_SERVICE)).setStreamMute(AudioManager.STREAM_SYSTEM,true);

                AudioManager myAudioManager = (AudioManager)context.getSystemService(Context.AUDIO_SERVICE);

                // get the working mode and keep it
                int workingAudioMode = myAudioManager.getMode();

                myAudioManager.setMode(AudioManager.MODE_IN_COMMUNICATION);

                // change mic state only if needed
                if (myAudioManager.isMicrophoneMute() != state) {
                    myAudioManager.setMicrophoneMute(state);
                }

                // set back the original working mode
                myAudioManager.setMode(workingAudioMode);
            }catch (Exception e){
                Log.e("HBRecorder", "Muting mic failed with the following exception:");
                e.printStackTrace();
            }

        }
    }*/

    // Set screen densityDpi
    public void setConfig(ScreenRecorderConfig config) {
        this.config = config;
        this.config.setScreenDensity(Resources.getSystem().getDisplayMetrics().densityDpi);
    }

    //Get default width
    public int getDefaultWidth() {
        ScreenRecorderCodecInfo screenRecorderCodecInfo = new ScreenRecorderCodecInfo();
        screenRecorderCodecInfo.setContext(context);
        return screenRecorderCodecInfo.getMaxSupportedWidth();
    }

    //Get default height
    public int getDefaultHeight() {
        ScreenRecorderCodecInfo screenRecorderCodecInfo = new ScreenRecorderCodecInfo();
        screenRecorderCodecInfo.setContext(context);
        return screenRecorderCodecInfo.getMaxSupportedHeight();
    }

    /*Get file path including file name and extension*/
    public String getFilePath() {
        return ScreenRecorderHandler.getFilePath();
    }

    /*Get file name and extension*/
    public String getFileName() {
        return ScreenRecorderHandler.getFileName();
    }

    /*Start screen recording*/
    public void startScreenRecording(Intent data, int resultCode) {
        try {
            if (config == null) throw new Exception("ScreenRecorderConfig cannot be null");
            if (!mWasUriSet) {
                String outputPath = "";
                if (!TextUtils.isEmpty(config.getOutputPath())) {
                    File file = new File(config.getOutputPath());
                    outputPath = file.getParent();
                } else {
                    outputPath = new RootFilesManager(context).getFileDir().getAbsolutePath();
                }
                observer = new FileObserver(outputPath, ScreenRecorder.this);
                observer.startWatching();
                config.setOutputPath(outputPath);
            }

            service = new Intent(context, ForegroundService.class);
            service.setAction("screen_recorder");
            if (mWasUriSet) {
                service.putExtra("mUri", mUri.toString());
            }
            service.putExtra("code", resultCode);
            service.putExtra("data", data);
            service.putExtra("audio", config.isAudioEnabled());
            service.putExtra("width", config.getScreenWidth());
            service.putExtra("height", config.getScreenHeight());
            service.putExtra("density", config.getScreenDensity());
            service.putExtra("quality", config.isVideoHDEnabled());
            service.putExtra("path", config.getOutputPath());
            service.putExtra("fileName", config.getFileName());
            service.putExtra("orientation", config.getOrientation());
            service.putExtra("audioBitrate", config.getAudioBitrate());
            service.putExtra("audioSamplingRate", config.getAudioSamplingRate());
            service.putExtra("enableCustomSettings", config.isEnableCustomSettings());
            service.putExtra("audioSource", config.getAudioSource());
            service.putExtra("videoEncoder", config.getVideoEncoder());

            service.putExtra("videoFrameRate", config.getVideoFrameRate());
            service.putExtra("videoBitrate", config.getVideoBitrate());
            service.putExtra("outputFormat", config.getOutputFormat());
            service.putExtra(ScreenRecorderHandler.BUNDLED_LISTENER, new ResultReceiver(new Handler()) {
                @Override
                protected void onReceiveResult(int resultCode, Bundle resultData) {
                    super.onReceiveResult(resultCode, resultData);
                    if (resultCode == Activity.RESULT_OK) {
                        String errorListener = resultData.getString(ERROR_REASON_KEY);
                        String onComplete = resultData.getString(ON_COMPLETE_KEY);
                        int onStartCode = resultData.getInt(ON_START_KEY);
                        int errorCode = resultData.getInt(ERROR_KEY);
                        // There was an error
                        if (errorListener != null) {
                            //Stop countdown if it was set
                            stopCountDown();
                            if (!mWasUriSet) {
                                observer.stopWatching();
                            }
                            isRecording = false;
                            wasOnErrorCalled = true;
                            if (errorCode > 0) {
                                screenRecorderListener.HBRecorderOnError(errorCode, errorListener);
                            } else {
                                screenRecorderListener.HBRecorderOnError(GENERAL_ERROR, errorListener);
                            }
                            try {
                                stopScreenRecording();
                            } catch (Exception e) {
                                // Can be ignored
                            }

                        }
                        // OnComplete was called
                        else if (onComplete != null) {
                            isRecording = false;
                            //Stop countdown if it was set
                            stopCountDown();
                            //OnComplete for when Uri was passed
                            if (mWasUriSet && !wasOnErrorCalled) {
                                screenRecorderListener.HBRecorderOnComplete();
                            }
                            wasOnErrorCalled = false;
                        }
                        // OnStart was called
                        else if (onStartCode != 0) {
                            isRecording = true;
                            screenRecorderListener.HBRecorderOnStart();
                            //Check if max duration was set and start count down
                            if (config.getMaxDuration() > 0) {
                                startCountdown();
                            }
                        }
                        // OnPause/onResume was called
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                            String onPause = resultData.getString(ON_PAUSE_KEY);
                            String onResume = resultData.getString(ON_RESUME_KEY);
                            if (onPause != null) {
                                isRecording = false;
                                screenRecorderListener.HBRecorderOnPause();
                            } else if (onResume != null) {
                                isRecording = true;
                                screenRecorderListener.HBRecorderOnResume();
                            }
                        }
                    }
                }
            });
            // Max file size
            service.putExtra(MAX_FILE_SIZE_KEY, config.getMaxFileSize());
            context.startService(service);
        } catch (Exception e) {
            isRecording = false;
            screenRecorderListener.HBRecorderOnError(0, Log.getStackTraceString(e));
        }
    }

    /*Stop screen recording*/
    public void stopScreenRecording() {
        if (service != null) {
            isPaused = true;
            service.setAction("screen_recorder.stop");
            context.startService(service);
        }
    }

    /*Pause screen recording*/
    @RequiresApi(api = Build.VERSION_CODES.N)
    public void pauseScreenRecording() {
        if (service != null) {
            isPaused = true;
            service.setAction("screen_recorder.pause");
            context.startService(service);
        }
    }

    /*Pause screen recording*/
    @RequiresApi(api = Build.VERSION_CODES.N)
    public void resumeScreenRecording() {
        if (service != null) {
            isPaused = false;
            service.setAction("screen_recorder.resume");
            context.startService(service);
        }
    }

    /*Check if video is paused*/
    public boolean isRecordingPaused() {
        return isPaused;
    }

    /*Check if recording is in progress*/
    public boolean isBusyRecording() {
        //return Utils.isServiceRunning(context, ForegroundService.class);
        return isRecording;
    }

    /*CountdownTimer for when max duration is set*/
    Countdown countDown = null;

    private void startCountdown() {
        countDown = new Countdown(config.getMaxDuration(), 1000, 0) {
            @Override
            public void onTick(long timeLeft) {
                // Could add a callback to provide the time to the user
                // Will add if users request this
            }

            @Override
            public void onFinished() {
                onTick(0);
                // Since the timer is running on a different thread
                // UI chances should be called from the UI Thread
                new Handler(Looper.getMainLooper()).post(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            isRecording = false;
                            stopScreenRecording();
                            observer.stopWatching();
                            screenRecorderListener.HBRecorderOnComplete();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                });
            }

            @Override
            public void onStopCalled() {
                // Currently unused, but might be helpful in the future
            }
        };
        countDown.start();
    }

    private void stopCountDown() {
        if (countDown != null) {
            countDown.stop();
        }
    }

    /*Complete callback method*/
    @Override
    public void onCompleteCallback() {
        observer.stopWatching();
        screenRecorderListener.HBRecorderOnComplete();
    }
}