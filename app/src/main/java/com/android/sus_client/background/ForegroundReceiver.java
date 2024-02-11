package com.android.sus_client.background;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.android.sus_client.services.ForegroundService;

public class ForegroundReceiver extends BroadcastReceiver {

    public static String TAG = ForegroundReceiver.class.getSimpleName();

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction().equals("RestartForeground")) {
            // start foreground service or enqueue work to start foreground service
            ForegroundService.startService(context);
            ForegroundWorker.enqueueWork(context);
        }
    }

}