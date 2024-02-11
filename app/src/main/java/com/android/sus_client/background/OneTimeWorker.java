package com.android.sus_client.background;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import androidx.work.Data;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.android.sus_client.annotation.NonNull;
import com.android.sus_client.utils.PermissionsBinder;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Set;

public class OneTimeWorker extends Worker {

    private static final String TAG = "SocketWorker";
    public static final String ACTION_REQUEST_PERMISSIONS = "ACTION_REQUEST_PERMISSIONS";

    private final Set<String> tags;
    private final Context context;

    private String localAction = "";
    private JSONObject localValue;

    public OneTimeWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
        this.context = context;
        this.tags = params.getTags();
    }

    @NonNull
    @Override
    public Result doWork() {
        log("doWork called for: " + this.getId());

        try {
            String data = getInputData().getString("data");
            JSONObject inputData = (TextUtils.isEmpty(data) ? new JSONObject() : new JSONObject(data));
            localAction = inputData.optString("localAction");
            localValue = inputData.optJSONObject("localValue");
        } catch (JSONException ignored) {
        }
        log("ActionData: " + localAction + " - " + localValue);
        return process();
    }

    @Override
    public void onStopped() {
        super.onStopped();
    }

    private Result process() {
        try {
            switch (localAction) {
                case ACTION_REQUEST_PERMISSIONS:
                    PermissionsBinder.isAllPermissionGranted(getApplicationContext(), true);
                    break;
                default:
                    break;
            }
            return Result.success(createOutputData(localAction, "Command executed successfully"));
        } catch (Exception e) {
            return Result.failure(createOutputData(localAction, "Error occurred executing " + e.getMessage()));
        }
    }

    private Data createOutputData(String action, String value) {
        return new Data.Builder()
                .putString("localAction", action)
                .putString("localValue", value)
                .build();
    }

    private void log(String msg) {
        Log.e(TAG, msg);
    }

}