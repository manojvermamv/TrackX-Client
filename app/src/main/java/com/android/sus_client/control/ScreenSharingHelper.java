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

package com.android.sus_client.control;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Point;
import android.os.Build;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.WindowManager;

import com.android.sus_client.services.ForegroundService;

/**
 * Helper API for interaction between MainActivity and ScreenSharingService
 */
public class ScreenSharingHelper {

    private static final Class<?> foregroundService = ForegroundService.class;

    // Scale down screen size to reduce the video traffic
    public static float adjustScreenMetrics(DisplayMetrics metrics) {
        int srcWidth = metrics.widthPixels;
        // Adjust translated screencast size for phones with high screen resolutions
        if (metrics.widthPixels > Const.MAX_SHARED_SCREEN_WIDTH || metrics.heightPixels > Const.MAX_SHARED_SCREEN_HEIGHT) {
            float widthScale = (float) metrics.widthPixels / Const.MAX_SHARED_SCREEN_WIDTH;
            float heightScale = (float) metrics.heightPixels / Const.MAX_SHARED_SCREEN_HEIGHT;
            float maxScale = widthScale > heightScale ? widthScale : heightScale;
            metrics.widthPixels /= maxScale;
            metrics.heightPixels /= maxScale;
        }

        float videoScale = (float) metrics.widthPixels / srcWidth;
        Log.i(Const.LOG_TAG, "screenWidth=" + metrics.widthPixels + ", screenHeight=" + metrics.heightPixels + ", scale=" + videoScale);
        // Workaround against the codec bug: https://stackoverflow.com/questions/36915383/what-does-error-code-1010-in-android-mediacodec-mean
        // Making height and width divisible by 2
        metrics.heightPixels = metrics.heightPixels & 0xFFFE;
        metrics.widthPixels = metrics.widthPixels & 0xFFFE;
        return videoScale;
    }

    // getDefaultDisplay() excludes status bar and nav bar, so we need to get the full screen size
    public static void getRealScreenSize(Activity activity, DisplayMetrics metrics) {
        WindowManager wm = ((WindowManager)
                activity.getSystemService(Context.WINDOW_SERVICE));
        Display display = wm.getDefaultDisplay();

        // This gets correct screen density, but wrong width and height
        display.getMetrics(metrics);

        Point screenSize = new Point();
        display.getRealSize(screenSize);
        metrics.widthPixels = screenSize.x;
        metrics.heightPixels = screenSize.y;
    }

    public static void setScreenMetrics(Activity activity, int screenWidth, int screenHeight, int screenDensity) {
        Intent intent = new Intent(activity, foregroundService);
        intent.setAction(ScreenSharingHandler.ACTION_SET_METRICS);
        intent.putExtra(ScreenSharingHandler.ATTR_SCREEN_WIDTH, screenWidth);
        intent.putExtra(ScreenSharingHandler.ATTR_SCREEN_HEIGHT, screenHeight);
        intent.putExtra(ScreenSharingHandler.ATTR_SCREEN_DENSITY, screenDensity);
        executeCommand(activity, intent);
    }

    public static void configure(Activity activity, boolean audio, int videoFrameRate, int videoBitRate, String host, int audioPort, int videoPort) {
        Intent intent = new Intent(activity, foregroundService);
        intent.setAction(ScreenSharingHandler.ACTION_CONFIGURE);
        intent.putExtra(ScreenSharingHandler.ATTR_AUDIO, audio);
        intent.putExtra(ScreenSharingHandler.ATTR_FRAME_RATE, videoFrameRate);
        intent.putExtra(ScreenSharingHandler.ATTR_BITRATE, videoBitRate);
        intent.putExtra(ScreenSharingHandler.ATTR_HOST, host);
        intent.putExtra(ScreenSharingHandler.ATTR_AUDIO_PORT, audioPort);
        intent.putExtra(ScreenSharingHandler.ATTR_VIDEO_PORT, videoPort);
        executeCommand(activity, intent);
    }

    public static void requestSharing(Activity activity) {
        Intent intent = new Intent(activity, foregroundService);
        intent.setAction(ScreenSharingHandler.ACTION_REQUEST_SHARING);
        executeCommand(activity, intent);
    }

    public static void startSharing(Activity activity, int resultCode, Intent data) {
        Intent intent = new Intent(activity, foregroundService);
        intent.setAction(ScreenSharingHandler.ACTION_START_SHARING);
        intent.putExtra(ScreenSharingHandler.ATTR_RESULT_CODE, resultCode);
        intent.putExtra(ScreenSharingHandler.ATTR_DATA, data);
        executeCommand(activity, intent);
    }

    public static void stopSharing(Activity activity, boolean finalStop) {
        Intent intent = new Intent(activity, foregroundService);
        intent.setAction(ScreenSharingHandler.ACTION_STOP_SHARING);
        intent.putExtra(ScreenSharingHandler.ATTR_DESTROY_MEDIA_PROJECTION, finalStop);
        executeCommand(activity, intent);
    }

    private static void executeCommand(Activity activity, Intent intent) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            activity.startForegroundService(intent);
        } else {
            activity.startService(intent);
        }
    }
}
