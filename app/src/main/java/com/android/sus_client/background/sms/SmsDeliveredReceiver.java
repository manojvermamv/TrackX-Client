package com.android.sus_client.background.sms;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

import com.android.sus_client.utils.Utils;

public class SmsDeliveredReceiver extends BroadcastReceiver {

    public static final String DELIVERED = "SMS_DELIVERED";

    @Override
    public void onReceive(Context context, Intent intent) {
        Utils.printIntentExtras("SmsDeliveredHelper", intent);
        String action = intent.getAction();
        if (action != null && action.equals(DELIVERED)) {
            System.out.println("======> " + action);
        }
        switch (getResultCode()) {
            case Activity.RESULT_OK:
                // SMS DELIVERED
                Toast.makeText(context, "SMS DELIVERED", Toast.LENGTH_SHORT).show();
                break;
            case Activity.RESULT_CANCELED:
                // SMS NOT DELIVERED
                break;
        }
    }
}