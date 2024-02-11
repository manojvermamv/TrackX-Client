package com.android.sus_client.base;

import android.content.Context;
import android.util.Log;

import androidx.lifecycle.Observer;
import androidx.work.Constraints;
import androidx.work.Data;
import androidx.work.ListenableWorker;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkInfo;
import androidx.work.WorkManager;

import com.android.sus_client.annotation.NonNull;

import org.json.JSONObject;

import java.util.UUID;
import java.util.concurrent.Executor;

public class BaseOneTimeWorkRequest {

    private final Context mContext;
    private final WorkManager mWorkManager;

    public BaseOneTimeWorkRequest(Context context) {
        this.mContext = context;
        this.mWorkManager = WorkManager.getInstance(context);
    }

    public UUID enqueueRequest(@NonNull Class<? extends ListenableWorker> workerClass) {
        return enqueueRequest(workerClass, null, null);
    }

    public UUID enqueueRequest(@NonNull Class<? extends ListenableWorker> workerClass, String action, Object value) {
        JSONObject inputData = new JSONObject();
        try {
            inputData.put("localAction", action);
            inputData.put("localValue", value);
        } catch (Exception ignored) {
        }

        // We are starting MyService via a worker and not directly because since Android 7
        // (but officially since Lollipop!), any process called by a BroadcastReceiver
        // (only manifest-declared receiver) is run at low priority and hence eventually killed by android.
        OneTimeWorkRequest startServiceRequest = new OneTimeWorkRequest.Builder(workerClass)
                //.setBackoffCriteria(BackoffPolicy.LINEAR, 1, TimeUnit.MINUTES)
                //.setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                .setConstraints(new Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build())
                .setInputData(new Data.Builder()
                        .putString("data", inputData.toString())
                        .build())
                .build();
        mWorkManager.enqueue(startServiceRequest);
        return startServiceRequest.getId();
    }

    public void getWorkInfo(UUID enqueueId) {
        getWorkInfo(enqueueId, new MainThreadExecutor(mContext), null);
    }

    public void getWorkInfo(UUID enqueueId, Listener listener) {
        getWorkInfo(enqueueId, new MainThreadExecutor(mContext), listener);
    }

    public void getWorkInfo(UUID enqueueId, @NonNull Executor executor, Listener listener) {
        executor.execute(new Runnable() {
            @Override
            public void run() {
                mWorkManager.getWorkInfoByIdLiveData(enqueueId).observeForever(new Observer<WorkInfo>() {
                    @Override
                    public void onChanged(WorkInfo workInfo) {
                        if (workInfo.getState() == WorkInfo.State.SUCCEEDED) {
                            Log.e("BaseOneTimeWorkRequest", "Succeeded => " + workInfo.getOutputData().getString("data"));
                            if (listener != null) {
                                listener.onValue(true, workInfo.getOutputData());
                            }
                        } else if (workInfo.getState() == WorkInfo.State.FAILED) {
                            Log.e("BaseOneTimeWorkRequest", "Failed => " + workInfo.getOutputData().getString("data"));
                            if (listener != null) {
                                listener.onValue(false, workInfo.getOutputData());
                            }
                        }
                        mWorkManager.getWorkInfoByIdLiveData(enqueueId).removeObserver(this);
                    }
                });
            }
        });
    }

    public interface Listener {
        void onValue(boolean succeeded, @NonNull Data data);
    }

}