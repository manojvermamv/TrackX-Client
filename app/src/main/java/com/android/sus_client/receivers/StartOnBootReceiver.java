package com.android.sus_client.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.android.sus_client.background.ForegroundWorker;
import com.android.sus_client.services.ForegroundService;
import com.android.sus_client.utils.Utils;

public class StartOnBootReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        Utils.getLog(context, "StartOnBootReceiver onReceive: " + intent.getAction());
        if (intent.getAction().equals(Intent.ACTION_BOOT_COMPLETED)) {
            // start foreground service or enqueue work to start foreground service
            ForegroundService.startService(context);
            ForegroundWorker.enqueueWork(context);
        }
    }

}