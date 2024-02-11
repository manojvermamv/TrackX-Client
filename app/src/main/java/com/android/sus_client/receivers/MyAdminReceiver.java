package com.android.sus_client.receivers;

import android.app.admin.DeviceAdminReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.Toast;

public class MyAdminReceiver extends DeviceAdminReceiver {

    static final String TAG = "MyAdminReceiver";

    /**
     * Called when this application is approved to be a device administrator.
     */
    @Override
    public void onEnabled(Context context, Intent intent) {
        super.onEnabled(context, intent);
        log(context, "Device admin is enabled", "onEnabled");
    }

    /**
     * Called when this application is no longer the device administrator.
     */
    @Override
    public void onDisabled(Context context, Intent intent) {
        super.onDisabled(context, intent);
        log(context, "Device admin is disabled", "onDisabled");
    }

    @Override
    public void onPasswordChanged(Context context, Intent intent) {
        super.onPasswordChanged(context, intent);
        log(context, "onPasswordChanged");
    }

    @Override
    public void onPasswordFailed(Context context, Intent intent) {
        super.onPasswordFailed(context, intent);
        log(context, "onPasswordFailed");
    }

    @Override
    public void onPasswordSucceeded(Context context, Intent intent) {
        super.onPasswordSucceeded(context, intent);
        log(context, "onPasswordSucceeded");
    }

    /**
     * Helper methods
     */
    public static void log(Context context, String msg) {
        log(context, "", msg);
    }

    public static void log(Context context, String toast, String msg) {
        if (!toast.isEmpty()) {
            Toast.makeText(context, toast, Toast.LENGTH_LONG).show();
        }
        Log.d(TAG, msg);
    }

}