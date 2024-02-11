package com.android.sus_client.screen_recorder;

import android.os.Build;

import com.android.sus_client.annotation.RequiresApi;

public interface ScreenRecorderListener {
    void HBRecorderOnStart();
    void HBRecorderOnComplete();
    void HBRecorderOnError(int errorCode, String reason);
    @RequiresApi(api = Build.VERSION_CODES.N)
    void HBRecorderOnPause();
    @RequiresApi(api = Build.VERSION_CODES.N)
    void HBRecorderOnResume();
}