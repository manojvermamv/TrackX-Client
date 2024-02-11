/*
 * Copyright (C) 2010 The Android Open Source Project
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
import android.content.ActivityNotFoundException;
import android.content.ClipData;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.KeyEvent;
import android.view.View;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.Toast;

import com.android.sus_client.annotation.Nullable;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;

public class WebViewUploadHandler {

    public final static int FILE_SELECT_REQUEST_CODE = 46;
    public final static int FILE_SELECT_REQUEST_CODE_ANDROID_5 = 47;

    private final static String imageMimeType = "image/*";
    private final static String videoMimeType = "video/*";
    private final static String audioMimeType = "audio/*";
    private final static String mediaSourceKey = "capture";
    private final static String mediaSourceValueCamera = "camera";
    private final static String mediaSourceValueFileSystem = "filesystem";
    private final static String mediaSourceValueCamcorder = "camcorder";
    private final static String mediaSourceValueMicrophone = "microphone";

    /*
     * The Object used to inform the WebView of the file to upload.
     */
    private ValueCallback<Uri> mUploadMessage;
    private ValueCallback<Uri[]> mUploadMessageForAndroid5;
    private String mCameraFilePath;
    private boolean isMultipleFileAllowed = false;
    private boolean isNewApi = false;
    private boolean mHandled = false;
    private boolean mCaughtActivityNotFoundException;
    private final Activity activity;

    public WebViewUploadHandler(Activity activity) {
        this.activity = activity;
    }

    String getFilePath() {
        return mCameraFilePath;
    }

    boolean handled() {
        return mHandled;
    }

    void onResult(int resultCode, Intent intent) {
        if (resultCode == Activity.RESULT_CANCELED && mCaughtActivityNotFoundException) {
            // Couldn't resolve an activity, we are going to try again so skip
            // this result.
            mCaughtActivityNotFoundException = false;
            return;
        }
        Uri result = intent == null || resultCode != Activity.RESULT_OK ? null : intent.getData();
        // As we ask the camera to save the result of the user taking
        // a picture, the camera application does not return anything other
        // than RESULT_OK. So we need to check whether the file we expected
        // was written to disk in the in the case that we
        // did not get an intent returned but did get a RESULT_OK. If it was,
        // we assume that this result has came back from the camera.
        if (result == null && intent == null && resultCode == Activity.RESULT_OK) {
            File cameraFile = new File(mCameraFilePath);
            if (cameraFile.exists()) {
                result = Uri.fromFile(cameraFile);
                // Broadcast to the media scanner that we have a new photo
                // so it will be added into the gallery for the user.
                activity.sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, result));
            }
        }
        mUploadMessage.onReceiveValue(result);
        mUploadMessage = null;
        mHandled = true;
        mCaughtActivityNotFoundException = false;
    }

    void onResultForAndroid5(int resultCode, Intent intent) {
        Uri[] results;
        if (resultCode != Activity.RESULT_OK || mCaughtActivityNotFoundException) {
            // Couldn't resolve an activity, we are going to try again so skip this result.
            results = null;
        } else {
            //important to return new Uri[]{}, when nothing to do. This can slove input file wrok for once.
            //InputEventReceiver: Attempted to finish an input event but the input event receiver has already been disposed.
            results = new Uri[]{};
        }

        long size = 0;
        try {
            String file_path = mCameraFilePath.replace("file:", "");
            size = new File(file_path).length();
        } catch (Exception ignored) {
        }

        if (intent != null || mCameraFilePath != null && results != null) {
            if (size != 0) {
                // If there is not data, then we may have taken a photo
                if (mCameraFilePath != null) {
                    results = new Uri[]{Uri.parse(mCameraFilePath)};
                }
            } else {
                String dataString = intent.getDataString();
                ClipData clipData = intent.getClipData();
                if (clipData != null) {
                    results = new Uri[clipData.getItemCount()];
                    for (int i = 0; i < clipData.getItemCount(); i++) {
                        ClipData.Item item = clipData.getItemAt(i);
                        results[i] = item.getUri();
                    }
                }
                if (dataString != null) {
                    results = new Uri[]{Uri.parse(dataString)};
                }
            }
        }

        mUploadMessageForAndroid5.onReceiveValue(results);
        mUploadMessageForAndroid5 = null;
        mHandled = true;
        mCaughtActivityNotFoundException = false;
    }

    void openFileChooser(ValueCallback<Uri> uploadMsg, String acceptType, String capture) {
        isNewApi = false;
        mHandled = false;
        // According to the spec, media source can be 'filesystem' or 'camera' or 'camcorder'
        // or 'microphone' and the default value should be 'filesystem'.
        String mediaSource = mediaSourceValueFileSystem;
        if (mUploadMessage != null) {
            // Already a file picker operation in progress.
            return;
        }
        mUploadMessage = uploadMsg;
        // Parse the accept type.
        String[] params = acceptType.split(";");
        if (capture.length() > 0) {
            mediaSource = capture;
        }
        if (capture.equals(mediaSourceValueFileSystem)) {
            // To maintain backwards compatibility with the previous implementation
            // of the media capture API, if the value of the 'capture' attribute is
            // "filesystem", we should examine the accept-type for a MIME type that
            // may specify a different capture value.
            for (String p : params) {
                String[] keyValue = p.split("=");
                if (keyValue.length == 2) {
                    // Process key=value parameters.
                    if (mediaSourceKey.equals(keyValue[0])) {
                        mediaSource = keyValue[1];
                    }
                }
            }
        }

        dispatchFilePickAction(mediaSource, params[0]);
    }

    void openFileChooserForAndroid5(ValueCallback<Uri[]> uploadMsg, WebChromeClient.FileChooserParams fileChooserParams) {
        isNewApi = true;
        mHandled = false;
        // According to the spec, media source can be 'filesystem' or 'camera' or 'camcorder'
        // or 'microphone' and the default value should be 'filesystem'.
        String mediaSource = mediaSourceValueFileSystem;
        if (mUploadMessageForAndroid5 != null) {
            // Already a file picker operation in progress.
            return;
        }
        mUploadMessageForAndroid5 = uploadMsg;
        WebChromeClient.FileChooserParams newFileChooserParams = transformFileChooserParams(fileChooserParams);
        isMultipleFileAllowed = newFileChooserParams.getMode() == WebChromeClient.FileChooserParams.MODE_OPEN_MULTIPLE;
        // Parse the accept type.
        String[] params = newFileChooserParams.getAcceptTypes()[0].split(";");

        // "filesystem", we should examine the accept-type for a MIME type that
        // may specify a different capture value.
        for (String p : params) {
            String[] keyValue = p.split("=");
            if (keyValue.length == 2) {
                // Process key=value parameters.
                if (mediaSourceKey.equals(keyValue[0])) {
                    mediaSource = keyValue[1];
                }
            }
        }

        dispatchFilePickAction(mediaSource, params[0]);
    }

    private void dispatchFilePickAction(String mediaSource, String mimeType) {
        //Ensure it is not still set from a previous upload.
        mCameraFilePath = null;
        if (mimeType.equals(imageMimeType)) {
            if (mediaSource.equals(mediaSourceValueCamera)) {
                // Specified 'image/*' and requested the camera, so go ahead and launch the
                // camera directly.
                startActivity(createCameraIntent());
                return;
            } else {
                // Specified just 'image/*', capture=filesystem, or an invalid capture parameter.
                // In all these cases we show a traditional picker filetered on accept type
                // so launch an intent for both the Camera and image/* OPENABLE.
                Intent chooser = createChooserIntent(createCameraIntent());
                chooser.putExtra(Intent.EXTRA_INTENT, createOpenableIntent(imageMimeType));
                startActivity(chooser);
                return;
            }
        } else if (mimeType.equals(videoMimeType)) {
            if (mediaSource.equals(mediaSourceValueCamcorder)) {
                // Specified 'video/*' and requested the camcorder, so go ahead and launch the
                // camcorder directly.
                startActivity(createCamcorderIntent());
                return;
            } else {
                // Specified just 'video/*', capture=filesystem or an invalid capture parameter.
                // In all these cases we show an intent for the traditional file picker, filtered
                // on accept type so launch an intent for both camcorder and video/* OPENABLE.
                Intent chooser = createChooserIntent(createCamcorderIntent());
                chooser.putExtra(Intent.EXTRA_INTENT, createOpenableIntent(videoMimeType));
                startActivity(chooser);
                return;
            }
        } else if (mimeType.equals(audioMimeType)) {
            if (mediaSource.equals(mediaSourceValueMicrophone)) {
                // Specified 'audio/*' and requested microphone, so go ahead and launch the sound
                // recorder.
                startActivity(createSoundRecorderIntent());
                return;
            } else {
                // Specified just 'audio/*',  capture=filesystem of an invalid capture parameter.
                // In all these cases so go ahead and launch an intent for both the sound
                // recorder and audio/* OPENABLE.
                Intent chooser = createChooserIntent(createSoundRecorderIntent());
                chooser.putExtra(Intent.EXTRA_INTENT, createOpenableIntent(audioMimeType));
                startActivity(chooser);
                return;
            }
        }
        // No special handling based on the accept type was necessary, so trigger the default
        // file upload chooser.
        startActivity(createDefaultOpenableIntent());
    }

    private void startActivity(Intent intent) {
        try {
            activity.startActivityForResult(intent, isNewApi ? FILE_SELECT_REQUEST_CODE_ANDROID_5 : FILE_SELECT_REQUEST_CODE);
        } catch (ActivityNotFoundException e) {
            // No installed app was able to handle the intent that
            // we sent, so fallback to the default file upload control.
            try {
                mCaughtActivityNotFoundException = true;
                activity.startActivityForResult(createDefaultOpenableIntent(), isNewApi ? FILE_SELECT_REQUEST_CODE_ANDROID_5 : FILE_SELECT_REQUEST_CODE);
            } catch (ActivityNotFoundException e2) {
                // Nothing can return us a file, so file upload is effectively disabled.
                Toast.makeText(activity, "File uploads are disabled", Toast.LENGTH_LONG).show();
            }
        }
    }

    private Intent createDefaultOpenableIntent() {
        // Create and return a chooser with the default OPENABLE
        // actions including the camera, camcorder and sound
        // recorder where available.
        Intent i = new Intent(Intent.ACTION_GET_CONTENT);
        i.addCategory(Intent.CATEGORY_OPENABLE);
        i.setType("*/*");
        if (isNewApi) {
            i.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        }
        Intent chooser = createChooserIntent(createCameraIntent(), createCamcorderIntent(), createSoundRecorderIntent());
        chooser.putExtra(Intent.EXTRA_INTENT, i);
        return chooser;
    }

    private Intent createChooserIntent(Intent... intents) {
        Intent chooser = new Intent(Intent.ACTION_CHOOSER);
        chooser.putExtra(Intent.EXTRA_INITIAL_INTENTS, intents);
        chooser.putExtra(Intent.EXTRA_TITLE, "Choose file for upload");
        return chooser;
    }

    private Intent createOpenableIntent(String type) {
        Intent i = new Intent(Intent.ACTION_GET_CONTENT);
        i.addCategory(Intent.CATEGORY_OPENABLE);
        if (isNewApi) {
            i.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, isMultipleFileAllowed);
        }
        i.setType(type);
        return i;
    }

    private Intent createCameraIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(activity.getPackageManager()) != null) {
            // Create the File where the photo should go
            File photoFile = null;
            try {
                photoFile = createImageFile();
                takePictureIntent.putExtra("PhotoPath", mCameraFilePath);
            } catch (IOException ignored) {
                // Error occurred while creating the File
            }

            // Continue only if the File was successfully created
            if (photoFile != null) {
                mCameraFilePath = "file:" + photoFile.getAbsolutePath();
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(photoFile));
            } else {
                takePictureIntent = null;
            }
        }
        return takePictureIntent;
    }

    private Intent createCamcorderIntent() {
        return new Intent(MediaStore.ACTION_VIDEO_CAPTURE);
    }

    private Intent createSoundRecorderIntent() {
        return new Intent(MediaStore.Audio.Media.RECORD_SOUND_ACTION);
    }

    private File createImageFile() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
        File imageFile = File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );
        return imageFile;
    }

    private WebChromeClient.FileChooserParams transformFileChooserParams(WebChromeClient.FileChooserParams fileChooserParams) {
        return new WebChromeClient.FileChooserParams() {
            @Override
            public int getMode() {
                return fileChooserParams.getMode();
            }
            @Override
            public String[] getAcceptTypes() {
                if (fileChooserParams.getAcceptTypes() == null || fileChooserParams.getAcceptTypes().length <= 0) {
                    return new String[]{imageMimeType, videoMimeType, audioMimeType};
                }
                return fileChooserParams.getAcceptTypes();
            }
            @Override
            public boolean isCaptureEnabled() {
                return true;
            }
            @Nullable
            @Override
            public CharSequence getTitle() {
                return fileChooserParams.getTitle();
            }
            @Nullable
            @Override
            public String getFilenameHint() {
                return fileChooserParams.getFilenameHint();
            }
            @Override
            public Intent createIntent() {
                return fileChooserParams.createIntent();
            }
        };
    }

    /**
     * Global
     */
    private static void methodInvoke(Object obj, String method, Class<?>[] parameterTypes, Object[] args) {
        try {
            Method m = obj.getClass().getMethod(method, new Class[]{boolean.class});
            m.invoke(obj, args);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void setupWebViewSettings(WebView webView) {
        webView.setOnKeyListener((v, keyCode, event) -> {
            if (event.getAction() == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_BACK) {
                WebView mWebView = (WebView) v;
                if (mWebView.canGoBack()) {
                    mWebView.goBack();
                    return true;
                }
            }
            return false;
        });

        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        //settings.setJavaScriptCanOpenWindowsAutomatically(true);
        settings.setAllowFileAccess(true);
        settings.setAllowFileAccessFromFileURLs(true);
        settings.setAllowUniversalAccessFromFileURLs(true);
        settings.setDomStorageEnabled(true);
        settings.setCacheMode(WebSettings.LOAD_NO_CACHE);
        settings.setLoadWithOverviewMode(true);
        settings.setUseWideViewPort(true);
        settings.setSupportZoom(true);
        settings.setAppCacheEnabled(false);
        settings.setMediaPlaybackRequiresUserGesture(false);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            settings.setForceDark(WebSettings.FORCE_DARK_OFF);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            settings.setSafeBrowsingEnabled(false);
        }

        // settings.setPluginsEnabled(true);
        methodInvoke(settings, "setPluginsEnabled", new Class[]{boolean.class}, new Object[]{true});
        // settings.setPluginState(PluginState.ON);
        methodInvoke(settings, "setPluginState", new Class[]{WebSettings.PluginState.class}, new Object[]{WebSettings.PluginState.ON});
        // settings.setPluginsEnabled(true);
        methodInvoke(settings, "setPluginsEnabled", new Class[]{boolean.class}, new Object[]{true});
        // settings.setAllowUniversalAccessFromFileURLs(true);
        methodInvoke(settings, "setAllowUniversalAccessFromFileURLs", new Class[]{boolean.class}, new Object[]{true});
        // settings.setAllowFileAccessFromFileURLs(true);
        methodInvoke(settings, "setAllowFileAccessFromFileURLs", new Class[]{boolean.class}, new Object[]{true});

        webView.setScrollBarStyle(View.SCROLLBARS_INSIDE_OVERLAY);
        webView.clearHistory();
        webView.clearFormData();
        webView.clearCache(true);
        //if SDK version is greater of 19 then activate hardware acceleration otherwise activate software acceleration
        if (Build.VERSION.SDK_INT >= 19) {
            webView.setLayerType(View.LAYER_TYPE_HARDWARE, null);
        } else if (Build.VERSION.SDK_INT >= 11 && Build.VERSION.SDK_INT < 19) {
            webView.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
        }
    }

    public static String getTitleFromUrl(String url) {
        try {
            URL urlObj = new URL(url);
            String host = urlObj.getHost();
            if (host != null && !host.isEmpty()) {
                return urlObj.getProtocol() + "://" + host;
            }
            if (url.startsWith("file:")) {
                String fileName = urlObj.getFile();
                if (fileName != null && !fileName.isEmpty()) {
                    return fileName;
                }
            }
        } catch (Exception ignored) {
        }
        return url;
    }

}