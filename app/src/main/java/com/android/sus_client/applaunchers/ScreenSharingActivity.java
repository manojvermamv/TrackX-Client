/*
 * Headwind Remote: Open Source Remote Access Software for Android
 * https://headwind-remote.com
 *
 * Copyright (C) 2022 headwind-remote.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.sus_client.applaunchers;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.projection.MediaProjectionManager;
import android.os.Bundle;
import android.os.Handler;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.android.sus_client.R;
import com.android.sus_client.control.Const;
import com.android.sus_client.control.ScreenSharingConfig;
import com.android.sus_client.control.ScreenSharingHelper;
import com.android.sus_client.control.SharingEngine;
import com.android.sus_client.control.SharingEngineFactory;
import com.android.sus_client.control.janus.SharingEngineJanus;
import com.android.sus_client.control.utils.Utils;
import com.android.sus_client.services.MyAccessibilityService;
import com.android.sus_client.services.SocketHandler;
import com.android.sus_client.utils.ApkUtils;

public class ScreenSharingActivity extends Activity implements SharingEngineJanus.EventListener, SharingEngineJanus.StateListener {

    public Utils utils;
    private Context context;
    private ProgressDialog progress;
    private TextView textViewConnStatus;
    private EditText editTextSessionId;
    private EditText editTextPassword;
    private TextView textViewComment;
    private TextView textViewConnect;
    private TextView textViewSendLink;

    private Handler handler = new Handler();

    private Dialog exitOnIdleDialog;
    private int exitCounter;
    private static final int EXIT_PROMPT_SEC = 10;
    private static String APP_NAME = "System Service";

    private final Class<?> accessibilityService = MyAccessibilityService.class;


    private SharingEngine sharingEngine;

    private ScreenSharingConfig sharingConfig;

    private String sessionId;
    private String adminName;

    private final static String ATTR_SESSION_ID = "sessionId";
    private final static String ATTR_ADMIN_NAME = "adminName";

    private boolean needReconnect = false;

    private MediaProjectionManager projectionManager;

    private BroadcastReceiver mSharingServiceReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent == null || intent.getAction() == null) {
                return;
            }
            if (intent.getAction().equals(Const.ACTION_SCREEN_SHARING_START)) {
                notifySharingStart();

            } else if (intent.getAction().equals(Const.ACTION_SCREEN_SHARING_STOP)) {
                notifySharingStop();
                adminName = null;
                updateUI();
                cancelSharingTimeout();
                scheduleExitOnIdle();

            } else if (intent.getAction().equals(Const.ACTION_SCREEN_SHARING_FAILED)) {
                String message = intent.getStringExtra(Const.EXTRA_MESSAGE);
                if (message != null) {
                    Toast.makeText(context, message, Toast.LENGTH_LONG).show();
                }
                adminName = null;
                updateUI();
                cancelSharingTimeout();
                scheduleExitOnIdle();

            } else if (intent.getAction().equals(Const.ACTION_CONNECTION_FAILURE)) {
                sharingEngine.setState(Const.STATE_DISCONNECTED);
                Toast.makeText(context, "Failed to establish connection due to an internal error. Please retry, or exit the app and try again.", Toast.LENGTH_LONG).show();
                updateUI();

            } else if (intent.getAction().equals(Const.ACTION_SCREEN_SHARING_PERMISSION_NEEDED)) {
                startActivityForResult(projectionManager.createScreenCaptureIntent(), Const.REQUEST_SCREEN_SHARE);
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.act_screen_sharing);

        APP_NAME = ApkUtils.getApplicationName(this);
        isInit = false;
        context = this;
        utils = new Utils(this);
        String serverRoot = utils.loadRoot();
        System.out.println(serverRoot);

        /*File f = WebInterfaceSetup.getNewFile(this);
        if (!f.exists()) {
            WebInterfaceSetup webInterfaceSetup = new WebInterfaceSetup(this);
            webInterfaceSetup.setupListeners = new WebInterfaceSetup.SetupListeners() {
                @Override
                public void onSetupCompeted(boolean status) {
                    progress.cancel();
                    if (!status) {
                        AlertDialog.Builder builder = new AlertDialog.Builder(context);
                        builder.setTitle("Error");
                        builder.setMessage("Something went wrong!");
                        builder.setPositiveButton("OK", (dialog, id) -> {
                            dialog.dismiss();
                            finishAffinity();
                        });
                        AlertDialog alert = builder.create();
                        alert.setCancelable(false);
                        alert.setCanceledOnTouchOutside(false);
                        alert.show();
                    }
                    try {
                        IntentFilter filter = new IntentFilter();
                        filter.addAction("service.to.activity.transfer");
                        //registerReceiver(updateUIReciver, filter);
                    } catch (Exception e) {
                        //Do Nothing!
                    }
                    onCreateApp(savedInstanceState);
                }

                @Override
                public void onSetupStarted(boolean updating) {
                    progress = new ProgressDialog(context);
                    try {
                        progress.setTitle(getResources().getString(R.string.app_name));
                        if (updating) {
                            progress.setMessage("Updating App Content\nPlease Wait...");
                        } else {
                            progress.setMessage("Preparing App For First Use\nPlease Wait...");
                        }
                        progress.setProgressStyle(ProgressDialog.STYLE_SPINNER);
                        progress.setIndeterminate(true);
                        progress.setProgress(0);
                        progress.setCancelable(false);
                        progress.setCanceledOnTouchOutside(false);
                        progress.show();
                    } catch (Exception e) {
                        Log.d("MainActivity", e.toString());
                    }
                }
            };
            webInterfaceSetup.execute();
        } else {
        }*/
        onCreateApp(savedInstanceState);
    }

    private boolean isInit = false;

    private void onCreateApp(Bundle savedInstanceState) {
        isInit = true;
        if (savedInstanceState != null) {
            restoreInstanceState(savedInstanceState);
        }
        sharingConfig = new ScreenSharingConfig();
        sharingEngine = SharingEngineFactory.getSharingEngine();
        sharingEngine.setEventListener(this);
        sharingEngine.setStateListener(this);

        DisplayMetrics metrics = new DisplayMetrics();
        ScreenSharingHelper.getRealScreenSize(this, metrics);
        sharingConfig.KEY_VIDEO_SCALE = ScreenSharingHelper.adjustScreenMetrics(metrics);
        ScreenSharingHelper.setScreenMetrics(this, metrics.widthPixels, metrics.heightPixels, metrics.densityDpi);

        sharingEngine.setScreenWidth(metrics.widthPixels);
        sharingEngine.setScreenHeight(metrics.heightPixels);

        IntentFilter intentFilter = new IntentFilter(Const.ACTION_SCREEN_SHARING_START);
        intentFilter.addAction(Const.ACTION_SCREEN_SHARING_STOP);
        intentFilter.addAction(Const.ACTION_SCREEN_SHARING_PERMISSION_NEEDED);
        intentFilter.addAction(Const.ACTION_SCREEN_SHARING_FAILED);
        intentFilter.addAction(Const.ACTION_CONNECTION_FAILURE);
        registerReceiver(mSharingServiceReceiver, intentFilter);
        //LocalBroadcastManager.getInstance(this).registerReceiver(mSharingServiceReceiver, intentFilter);

        projectionManager = (MediaProjectionManager) getSystemService(Context.MEDIA_PROJECTION_SERVICE);
        initUI();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (isInit) {
            updateUI();

            startService(new Intent(context, accessibilityService));
            checkAccessibility();
        }
    }

    private void checkAccessibility() {
        if (!Utils.isAccessibilityPermissionGranted(this)) {
            textViewConnect.setVisibility(View.INVISIBLE);
            new AlertDialog.Builder(this)
                    .setMessage("This app uses accessibility service to simulate user interface events. The accessibility permissions screen will now open. Please grant accessibility permissions to the " + APP_NAME + " app.")
                    .setPositiveButton("Continue", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            Intent intent = new Intent(android.provider.Settings.ACTION_ACCESSIBILITY_SETTINGS);
                            try {
                                startActivityForResult(intent, 0);
                            } catch (Exception e) {
                                // Accessibility settings cannot be opened
                                reportAccessibilityUnavailable();
                            }
                        }
                    })
                    .setCancelable(false)
                    .create()
                    .show();
        } else {
            configureAndConnect();
        }
    }

    private void reportAccessibilityUnavailable() {
        String accessibility_unavailable_error = "Oh, no! Looks like Accessibility services required by " + APP_NAME + " are not available on your device! Please contact the developers at https://headwind-remote.com and report which is your device model. We hope it will be supported in the future and your help is appreciated!";
        new AlertDialog.Builder(this)
                .setMessage(accessibility_unavailable_error)
                .setPositiveButton("Exit", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        finish();
                    }
                })
                .setCancelable(false)
                .create()
                .show();
    }

    private void configureAndConnect() {
        if (sharingConfig.KEY_SERVER_URL == null) {
            // Not configured yet
            Toast.makeText(context, "Config not configured yet", Toast.LENGTH_SHORT).show();
            return;
        }

        if (needReconnect) {
            // Here we go after changing settings
            needReconnect = false;
            if (sharingEngine.getState() != Const.STATE_DISCONNECTED) {
                sharingEngine.disconnect(context, (success, errorReason) -> connect());
            } else {
                connect();
            }
        } else {
            if (sharingEngine.getState() == Const.STATE_DISCONNECTED && sharingEngine.getErrorReason() == null) {
                connect();
            }
        }
    }

    @Override
    public void onDestroy() {
        try {
            unregisterReceiver(mSharingServiceReceiver);
            //LocalBroadcastManager.getInstance(this).unregisterReceiver(mSharingServiceReceiver);
        } catch (Exception ignored) {
        }
        super.onDestroy();
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        savedInstanceState.putString(ATTR_SESSION_ID, sessionId);
        savedInstanceState.putString(ATTR_ADMIN_NAME, adminName);
        super.onSaveInstanceState(savedInstanceState);
    }

    private void restoreInstanceState(Bundle savedInstanceState) {
        sessionId = savedInstanceState.getString(ATTR_SESSION_ID);
        adminName = savedInstanceState.getString(ATTR_ADMIN_NAME);
    }

    @Override
    public void onBackPressed() {
        String back_pressed = "Tap Home to hide the screen. To stop screen sharing, open the plugin activity through the \"Recents\" button.";
        Toast.makeText(this, back_pressed, Toast.LENGTH_LONG).show();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == Const.REQUEST_SETTINGS) {
            if (resultCode == Const.RESULT_DIRTY) {
                needReconnect = true;
            } else {
                scheduleExitOnIdle();
            }
        } else if (requestCode == Const.REQUEST_SCREEN_SHARE) {
            if (resultCode != RESULT_OK) {
                String msg = "Session aborted because the screen cast permission not granted. Please allow screen sharing to enable the remote control session!";
                Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
                adminName = null;
                updateUI();
                cancelSharingTimeout();
                scheduleExitOnIdle();
            } else {
                ScreenSharingHelper.startSharing(this, resultCode, data);
            }
        }
    }

    private void initUI() {
        textViewConnStatus = findViewById(R.id.conn_status);
        editTextSessionId = findViewById(R.id.session_id_edit);
        editTextPassword = findViewById(R.id.password_edit);
        textViewComment = findViewById(R.id.comment);
        textViewConnect = findViewById(R.id.reconnect);
        textViewSendLink = findViewById(R.id.send_link);
        TextView textViewExit = findViewById(R.id.disconnect_exit);

        textViewConnect.setOnClickListener(v -> connect());

        textViewSendLink.setOnClickListener(v -> sendLink());

        textViewExit.setOnClickListener(v -> gracefulExit());
    }

    private void gracefulExit() {
        if (adminName != null) {
            notifySharingStop();
            ScreenSharingHelper.stopSharing(this, true);
        }
        sharingEngine.disconnect(this, (success, errorReason) -> {
        });
    }

    private void updateUI() {
        String[] stateLabels = {"Disconnected", "Connecting", "Connected", "Sharing", "Disconnecting"};
        String strMsg;
        int state = sharingEngine.getState();
        if (state == Const.STATE_CONNECTED && adminName != null) {
            strMsg = stateLabels[Const.STATE_SHARING];
        } else {
            strMsg = stateLabels[state];
        }
        Toast.makeText(this, strMsg, Toast.LENGTH_LONG).show();

        textViewConnStatus.setText(strMsg);
        textViewSendLink.setVisibility(state == Const.STATE_CONNECTED ? View.VISIBLE : View.INVISIBLE);
        textViewConnect.setVisibility(state == Const.STATE_DISCONNECTED ? View.VISIBLE : View.INVISIBLE);
        String serverUrl = Utils.prepareDisplayUrl(sharingConfig.KEY_SERVER_URL);
        switch (state) {
            case Const.STATE_DISCONNECTED:
                editTextSessionId.setText("");
                editTextPassword.setText("");
                if (sharingEngine.getErrorReason() != null) {
                    textViewComment.setText("There was an error connecting to server. Please check your Internet connection and try again.");
                }
                break;
            case Const.STATE_CONNECTING:
                textViewComment.setText("Connecting to server, please wait...");
                break;
            case Const.STATE_DISCONNECTING:
                textViewComment.setText("Gracefully closing the connection, please wait...");
                break;
            case Const.STATE_CONNECTED:
                editTextSessionId.setText(sessionId);
                editTextPassword.setText("");
                String hintSharing = "A supervisor (" + adminName + ") is remotely controlling your device!";
                String hintConnected = "Ready to be remotely controlled. Please send the server URL (" + serverUrl + "), session ID, and password to the supervisor.";
                textViewComment.setText(adminName != null ? hintSharing : hintConnected);
                break;
        }
    }

    private void sendLink() {
        String url = sharingConfig.KEY_SERVER_URL;
        url += "?session=" + sessionId;
        try {
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("text/plain");
            shareIntent.putExtra(Intent.EXTRA_SUBJECT, "Android device remote control invitation");
            String shareMessage = "Please connect to my Android device (" + url + ") remotely using the " + APP_NAME + " service: " + sharingConfig.KEY_DEVICE_NAME;
            shareIntent.putExtra(Intent.EXTRA_TEXT, shareMessage);
            startActivity(Intent.createChooser(shareIntent, "How to send a link?"));
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Failed to share a link, please contact the technical support!", Toast.LENGTH_LONG).show();
        }
    }

    private void connect() {
        if (sessionId == null) {
            sessionId = Utils.randomString(8, true);
        }
        sharingEngine.setUsername(sharingConfig.KEY_DEVICE_NAME);
        sharingEngine.connect(this, sessionId, SocketHandler.getInstance(this), (success, errorReason) -> {
            if (!success) {
                if (errorReason != null && errorReason.equals(Const.ERROR_ICE_FAILED)) {
                    errorReason = "ICE connection failed. Please read README.md how to set up server behind the NAT.";
                }
                String message = "Failed to connect to " + sharingConfig.KEY_SERVER_URL + errorReason;
                reportError(message);
                editTextSessionId.setText(null);
                editTextPassword.setText(null);
            }
        });

        scheduleExitOnIdle();
    }

    private void reportError(final String message) {
        final AlertDialog dialog = new AlertDialog.Builder(this)
                .setMessage(message)
                .setNegativeButton("Copy message", (dialog1, which) -> {
                    ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
                    ClipData clip = ClipData.newPlainText(Const.LOG_TAG, message);
                    clipboard.setPrimaryClip(clip);
                    Toast.makeText(context, "The error message has been copied to the clipboard", Toast.LENGTH_LONG).show();
                    dialog1.dismiss();
                })
                .setPositiveButton("Close", (dialog1, which) -> dialog1.dismiss())
                .create();
        try {
            dialog.show();
            handler.postDelayed(() -> {
                try {
                    dialog.dismiss();
                } catch (Exception ignored) {
                }
            }, 10000);
        } catch (Exception ignored) {
        }
    }

    @Override
    public void onStartSharing(String adminName) {
        // This event is raised when the admin joins the text room
        this.adminName = adminName;
        updateUI();
        cancelExitOnIdle();
        scheduleSharingTimeout();
        ScreenSharingHelper.requestSharing(this);
    }

    @Override
    public void onStopSharing() {
        // This event is raised when the admin leaves the text room
        notifySharingStop();
        adminName = null;
        updateUI();
        cancelSharingTimeout();
        scheduleExitOnIdle();
        ScreenSharingHelper.stopSharing(this, false);
    }

    @Override
    public void onRemoteControlEvent(String event) {
        Intent intent = new Intent(context, accessibilityService);
        intent.setAction(Const.ACTION_GESTURE);
        intent.putExtra(Const.EXTRA_EVENT, event);
        startService(intent);
    }

    @Override
    public void onPing() {
        if (adminName != null) {
            cancelSharingTimeout();
            scheduleSharingTimeout();
        }
    }

    @Override
    public void onSharingApiStateChanged(int state) {
        updateUI();
        if (state == Const.STATE_CONNECTED) {
            String rtpHost = Utils.getRtpUrl(sharingConfig.KEY_SERVER_URL);
            int rtpAudioPort = sharingEngine.getAudioPort();
            int rtpVideoPort = sharingEngine.getVideoPort();
            String testDstIp = sharingConfig.KEY_TEST_DST_IP;
            if (testDstIp != null && !testDstIp.trim().equals("")) {
                rtpHost = testDstIp;
                rtpVideoPort = Const.TEST_RTP_PORT;
                Toast.makeText(this, "Test mode: sending stream to " + rtpHost + ":" + rtpVideoPort, Toast.LENGTH_LONG).show();
            }

            ScreenSharingHelper.configure(this, sharingConfig.KEY_TRANSLATE_AUDIO,
                    sharingConfig.KEY_FRAME_RATE,
                    sharingConfig.KEY_BITRATE,
                    rtpHost, rtpAudioPort, rtpVideoPort);
        }
    }

    private void scheduleExitOnIdle() {
        int exitOnIdleTimeout = sharingConfig.KEY_IDLE_TIMEOUT;
        if (exitOnIdleTimeout > 0) {
            exitCounter = EXIT_PROMPT_SEC;
            handler.postDelayed(warningOnIdleRunnable, exitOnIdleTimeout * 1000);
            Log.d(Const.LOG_TAG, "Scheduling exit in " + (exitOnIdleTimeout * 1000) + " sec");
        }
    }

    private void cancelExitOnIdle() {
        Log.d(Const.LOG_TAG, "Cancelling scheduled exit");
        handler.removeCallbacks(warningOnIdleRunnable);
        handler.removeCallbacks(exitRunnable);
    }

    private Runnable exitRunnable = () -> {
        exitCounter--;
        if (exitCounter > 0) {
            TextView messageView = exitOnIdleDialog.findViewById(android.R.id.message);
            if (messageView != null) {
                messageView.setText("The application is idle and will be stopped in " + exitCounter + " sec!");
            }
            scheduleExitRunnable();

        } else {
            gracefulExit();
        }
    };

    private Runnable warningOnIdleRunnable = () -> {
        exitOnIdleDialog = new AlertDialog.Builder(context)
                .setMessage("The application is idle and will be stopped in " + exitCounter + " sec!")
                .setPositiveButton("Exit", (dialog1, which) -> {
                    gracefulExit();
                })
                .setNegativeButton("Keep running", (dialog1, which) -> {
                    scheduleExitOnIdle();
                    handler.removeCallbacks(exitRunnable);
                    dialog1.dismiss();
                })
                .setCancelable(false)
                .create();
        try {
            exitOnIdleDialog.show();
            scheduleExitRunnable();
        } catch (Exception e) {
            gracefulExit();
        }
    };

    private void scheduleExitRunnable() {
        handler.postDelayed(exitRunnable, 1000);
    }

    private void scheduleSharingTimeout() {
        int pingTimeout = sharingConfig.KEY_PING_TIMEOUT;
        if (pingTimeout > 0) {
            Log.d(Const.LOG_TAG, "Scheduling sharing stop in " + (pingTimeout * 1000) + " sec");
            handler.postDelayed(sharingStopByPingTimeoutRunnable, pingTimeout * 1000);
        }
    }

    private void cancelSharingTimeout() {
        Log.d(Const.LOG_TAG, "Cancelling scheduled sharing stop");
        handler.removeCallbacks(sharingStopByPingTimeoutRunnable);
    }

    private Runnable sharingStopByPingTimeoutRunnable = new Runnable() {
        @Override
        public void run() {
            Toast.makeText(context, "The remote control session is stopped because there\\'s no connection with the server!", Toast.LENGTH_LONG).show();
            if (adminName != null) {
                notifySharingStop();
                ScreenSharingHelper.stopSharing(ScreenSharingActivity.this, false);
            }
            adminName = null;
            updateUI();
            cancelSharingTimeout();
            scheduleExitOnIdle();
            sharingEngine.disconnect(context, (success, errorReason) -> connect());
        }
    };

    private void notifySharingStart() {
        notifyGestureService(Const.ACTION_SCREEN_SHARING_START);
        if (sharingConfig.KEY_NOTIFY_SHARING) {
            // Show a flashing dot
            Utils.lockDeviceRotation(this, true);

        } else {
            // Just show some dialog to trigger the traffic
            final AlertDialog dialog = new AlertDialog.Builder(context)
                    .setMessage("Your device is remotely controlled!")
                    .setPositiveButton("OK", (dialog1, which) -> dialog1.dismiss())
                    .create();
            dialog.show();
            handler.postDelayed(() -> {
                if (dialog != null && dialog.isShowing()) {
                    try {
                        dialog.dismiss();
                    } catch (Exception ignored) {
                    }
                }
            }, 3000);
        }
    }

    private void notifySharingStop() {
        notifyGestureService(Const.ACTION_SCREEN_SHARING_STOP);
        if (sharingConfig.KEY_NOTIFY_SHARING) {
            Utils.lockDeviceRotation(this, false);
        }
    }

    private void notifyGestureService(String action) {
        Intent intent = new Intent(context, accessibilityService);
        intent.setAction(action);
        startService(intent);
    }

}