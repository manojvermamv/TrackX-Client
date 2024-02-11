package com.android.sus_client.utils;

import android.media.MediaRecorder;

import com.android.sus_client.commonutility.basic.Function0;

public class AudioRecorderUtils extends MediaRecorder {

    // implement realtime call recording in android 11 programmatically during a incoming call or outgoing call
    // implement low level call recording in android 11 programmatically when a incoming call or outgoing call happens
    private int flag = 0;
    private final Function0<Void> errorCallback;

    public AudioRecorderUtils(Function0<Void> errorCallback) {
        this.errorCallback = errorCallback;
    }

    public void startRecording(int audioSource, String filePath) {
        try {
            if (flag == 1) {
                // It means recorder is already on recording state.
            } else {
                setAudioSource(audioSource);
                setOutputFormat(OutputFormat.THREE_GPP);
                setAudioEncoder(AudioEncoder.AMR_NB);
                setOutputFile(filePath);

                setOnErrorListener((mr, what, extra) -> errorCallback.invoke());

                prepare();
                start();
                flag = 1;
            }

        } catch (Exception e) {
            e.printStackTrace();
            errorCallback.invoke();
        }
    }

    public void stopRecording(Function0<Void> callback) {
        try {
            if (flag == 0) {
                // It means recorder is already stopped state.
            } else {
                stop();
                release();
                flag = 0;
                callback.invoke();
            }
        } catch (Exception e) {
            e.printStackTrace();
            errorCallback.invoke();
        }
    }

    public boolean isRecording() {
        return flag == 1;
    }

}