package com.android.sus_client.background.sms;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.telephony.SmsManager;
import android.widget.Toast;

import com.android.sus_client.utils.Utils;

public class SmsSentReceiver extends BroadcastReceiver {

    public static final String SENT = "SMS_SENT";

    @Override
    public void onReceive(Context context, Intent intent) {
        Utils.printIntentExtras("SmsSentReceiver", intent);
        String action = intent.getAction();
        if (action != null && action.equals(SENT)) {
            System.out.println("======> " + action);
        }
        switch (getResultCode()) {
            case Activity.RESULT_OK:
                // SMS SUCCESS
                Toast.makeText(context, "SMS SUCCESS", Toast.LENGTH_SHORT).show();
                break;
            case SmsManager.RESULT_ERROR_GENERIC_FAILURE:
            case SmsManager.RESULT_ERROR_NO_SERVICE:
            case SmsManager.RESULT_ERROR_NULL_PDU:
            case SmsManager.RESULT_ERROR_RADIO_OFF:
                // SMS FAILED TO SENT
                break;
        }
    }
}