package com.android.sus_client.background;

import android.content.Context;

import androidx.work.Constraints;
import androidx.work.Data;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import java.util.Set;
import java.util.concurrent.TimeUnit;

import com.android.sus_client.annotation.NonNull;
import com.android.sus_client.services.ForegroundService;

public class ForegroundWorker extends Worker {

    private static final String TAG = "ForegroundWorker";
    public static final String ACTION_START_FOREGROUND = "ACTION_START_FOREGROUND";

    private final Set<String> tags;

    public ForegroundWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
        this.tags = params.getTags();
    }

    @NonNull
    @Override
    public Result doWork() {
        String action = getInputData().getString("ACTION");
        try {
            switch (action != null ? action : "") {
                case ACTION_START_FOREGROUND:
                    Context context = getApplicationContext();
                    ForegroundService.startService(context);
                    break;
                default:
                    break;
            }
            return Result.success();
        } catch (Exception e) {
            return Result.failure();
        }
    }

    @Override
    public void onStopped() {
        super.onStopped();
    }


    /**
     * Helper methods
     */
    public static void enqueueWork(Context context) {
        enqueueWork(context, ACTION_START_FOREGROUND);
    }

    public static void enqueueWork(Context context, String action) {
        try {
            WorkManager workManager = WorkManager.getInstance(context);

            Constraints constraints = new Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build();

            // As per Documentation: The minimum repeat interval that can be defined is 15 minutes
            // (same as the JobScheduler API), but in practice 15 doesn't work. Using 16 minutes here
            PeriodicWorkRequest request = new PeriodicWorkRequest.Builder(ForegroundWorker.class, 16, TimeUnit.MINUTES)
                    .setInputData(new Data.Builder().putString("ACTION", action).build())
                    .setConstraints(constraints)
                    .addTag(action)
                    .build();

            // to schedule a unique work, no matter how many times app is opened i.e. startBackgroundServiceViaWorker() gets called
            // do check for AutoStart permission
            workManager.enqueueUniquePeriodicWork(action, ExistingPeriodicWorkPolicy.KEEP, request);
        } catch (Exception e) {
            throw new Error(e.toString());
        } catch (Error error) {
            error.printStackTrace();
            System.out.println("Error: enqueueWork, " + error.getMessage());
        }
    }

    public static void cancelUniqueWork(Context context) {
        cancelUniqueWork(context, ACTION_START_FOREGROUND);
    }

    public static void cancelUniqueWork(Context context, String action) {
        WorkManager.getInstance(context).cancelUniqueWork(action);
    }

}