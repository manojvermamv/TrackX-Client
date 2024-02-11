package com.android.sus_client.utils;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.ActivityOptions;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.StrictMode;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.provider.OpenableColumns;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.webkit.MimeTypeMap;
import android.webkit.WebView;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.android.sus_client.annotation.NonNull;
import com.android.sus_client.annotation.Nullable;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class Utils {

    private static SimpleDateFormat sdf = new SimpleDateFormat("MMM d, yyyy, hh:mm:ss a", Locale.getDefault());

    // use these flag for touch outside in window overlay
    public static int WIN_FLAG_TOUCH_OUTSIDE = WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL | WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;

    public static int WIN_FLAG_TOUCH_OUTSIDE1 = WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL | WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS;

    public static int WIN_FLAG_TOUCH_OUTSIDE2 = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL | WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS;

    public static Map<String, Integer> WindowFlags;

    static {
        Map<String, Integer> aMap = new HashMap<>();
        aMap.put("FLAG_TOUCH_OUTSIDE", WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL | WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE);

        WindowFlags = Collections.unmodifiableMap(aMap);
    }

    public static void showToast(Context context, String s) {
        Toast.makeText(context, s, Toast.LENGTH_SHORT).show();
    }

    public static void showToastLong(Context context, String s) {
        Toast.makeText(context, s, Toast.LENGTH_LONG).show();
    }

    /**
     * Try this code to manually disable & re-enable the service
     * Note: You can catch the broadcast Intent.ACTION_PACKAGE_CHANGED to know when the service gets disabled.
     * Note: And you can use this code to check if your notification service is enabled.
     */
    public static void toggleComponentRestart(Context context, Class<?> clz) {
        PackageManager pm = context.getPackageManager();
        pm.setComponentEnabledSetting(new ComponentName(context, clz), PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP);
        pm.setComponentEnabledSetting(new ComponentName(context, clz), PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP);
    }

    public static boolean isServiceRunning(Context context, Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        if (manager == null) return false;
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Layout generation helper function
     */
    public static int resolveAttribute(Context context, int resId) {
        TypedValue outValue = new TypedValue();
        context.getTheme().resolveAttribute(resId, outValue, true);
        return outValue.data;
    }

    public static int resolveAttributeResId(Context context, int resId) {
        TypedValue outValue = new TypedValue();
        context.getTheme().resolveAttribute(resId, outValue, true);
        return outValue.resourceId;
    }

    public static int getWindowBackgroundColor(Context context) {
        int color = Color.WHITE;
        try {
            TypedValue a = new TypedValue();
            context.getTheme().resolveAttribute(android.R.attr.windowBackground, a, true);
            if (Build.VERSION.SDK_INT >= 29 && a.isColorType()) {
                color = a.data;  // windowBackground is a color
            }
        } catch (Exception ignored) {
        }
        return color;
    }

    public static void setStatusBarTranslucent(Activity activity, int backgroundColor) {
        Window window = activity.getWindow();
        if (Build.VERSION.SDK_INT > 11 && Build.VERSION.SDK_INT < 19) { // lower api
            window.getDecorView().setSystemUiVisibility(View.GONE);
        } else if (Build.VERSION.SDK_INT >= 19) {
            //for new api versions.
            window.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
            window.setFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS, WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        }

        // switch StatusBar text color
        View decorView = window.getDecorView();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (isDarkColor(backgroundColor)) {    //  set status text light
                decorView.setSystemUiVisibility(decorView.getSystemUiVisibility() & ~View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
            } else {                                        //  set status text dark
                decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
            }
        }
    }

    public static boolean isDarkColor(int color) {
        // Calculate the brightness of the color.
        float brightness = 0.0F;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            brightness = Color.luminance(color);
        }

        // If the brightness is less than 0.5, the color is considered dark.
        return brightness < 0.5;
    }

    // check if the app is in dark mode or not
    public static boolean isDarkModeOn(Context context) {
        int nightModeFlags = context.getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
        return nightModeFlags == Configuration.UI_MODE_NIGHT_YES;
    }

    // If the App is in Dark Mode then change it to Light Mode
    public static void changeToLightMode() {
        try {
            androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_NO);
        } catch (Exception e) {
            throw new Error(e.toString());
        } catch (Error e2) {
            e2.printStackTrace();
        }
    }

    @SuppressLint("ObsoleteSdkInt")
    public static int getBorderlessButtonStyle(Context context) {
        int attr;
        if (Build.VERSION.SDK_INT >= 21) {
            attr = android.R.attr.selectableItemBackground;
        } else {
            attr = context.getResources().getIdentifier("selectableItemBackground", "attr", context.getPackageName());
        }
        return resolveAttributeResId(context, attr);
    }

    public static int getTextColorPrimary(Context context) {
        int colorAttr;
        if (Build.VERSION.SDK_INT >= 21) {
            colorAttr = android.R.attr.textColorPrimary;
        } else {
            colorAttr = context.getResources().getIdentifier("textColorPrimary", "attr", context.getPackageName());
        }
        return resolveAttribute(context, colorAttr);
    }

    public static int getTextColorSecondary(Context context) {
        int colorAttr;
        if (Build.VERSION.SDK_INT >= 21) {
            colorAttr = android.R.attr.textColorSecondary;
        } else {
            colorAttr = context.getResources().getIdentifier("textColorSecondary", "attr", context.getPackageName());
        }
        return resolveAttribute(context, colorAttr);
    }

    @SuppressLint("ObsoleteSdkInt")
    public static int getColorPrimary(Context context) {
        int colorAttr;
        if (Build.VERSION.SDK_INT >= 21) {
            colorAttr = android.R.attr.colorPrimary;
        } else {
            //Get colorPrimary defined for AppCompat
            colorAttr = context.getResources().getIdentifier("colorPrimary", "attr", context.getPackageName());
        }
        return resolveAttribute(context, colorAttr);
    }

    @SuppressLint("ObsoleteSdkInt")
    public static int getColorAccent(Context context) {
        int colorAttr;
        if (Build.VERSION.SDK_INT >= 21) {
            colorAttr = android.R.attr.colorAccent;
        } else {
            //Get colorAccent defined for AppCompat
            colorAttr = context.getResources().getIdentifier("colorAccent", "attr", context.getPackageName());
        }
        return resolveAttribute(context, colorAttr);
    }

    public static void updateWidthHeight(View view, int width, int height) {
        ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) view.getLayoutParams();
        if (width == ViewGroup.MarginLayoutParams.MATCH_PARENT || width == ViewGroup.MarginLayoutParams.WRAP_CONTENT) {
            params.width = width;
        } else {
            params.width = (int) dpToPx(width);
        }
        if (height == ViewGroup.MarginLayoutParams.MATCH_PARENT || height == ViewGroup.MarginLayoutParams.WRAP_CONTENT) {
            params.height = height;
        } else {
            params.height = (int) dpToPx(height);
        }
        view.setLayoutParams(params);
    }

    /**
     * set relative layout with Example ->
     * addOrRemoveProperty(mView, RelativeLayout.CENTER_IN_PARENT, true);
     * addOrRemoveProperty(mView, RelativeLayout.CENTER_HORIZONTAL, false);
     */
    public static void addOrRemoveProperty(View view, int property, boolean flag) {
        RelativeLayout.LayoutParams layoutParams = (RelativeLayout.LayoutParams) view.getLayoutParams();
        if (flag) {
            layoutParams.addRule(property);
        } else {
            layoutParams.removeRule(property);
        }
        view.setLayoutParams(layoutParams);
    }

    public static void addOrRemoveProperty(View view, int property, int id, boolean flag) {
        RelativeLayout.LayoutParams layoutParams = (RelativeLayout.LayoutParams) view.getLayoutParams();
        if (flag) {
            layoutParams.addRule(property, id);
        } else {
            layoutParams.removeRule(property);
        }
        view.setLayoutParams(layoutParams);
    }

    public static void updateMargin(View view, float margin) {
        updateMargin(view, margin, margin, margin, margin);
    }

    public static void updateMargin(View view, float left, float top, float right, float bottom) {
        ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) view.getLayoutParams();
        params.leftMargin = (int) dpToPx(left);
        params.topMargin = (int) dpToPx(top);
        params.rightMargin = (int) dpToPx(right);
        params.bottomMargin = (int) dpToPx(bottom);
        view.setLayoutParams(params);
    }

    public static int dpToPxInt(int dp) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, Resources.getSystem().getDisplayMetrics());
    }

    public static float dpToPx(float dp) {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, Resources.getSystem().getDisplayMetrics());
    }

    public static float spToPx(float sp) {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, sp, Resources.getSystem().getDisplayMetrics());
    }

    /**
     * Status Bar utils
     */
    public static void setStatusBarViewHeight(Activity activity, View view) {
        ViewGroup.LayoutParams params = view.getLayoutParams();
        params.height = getStatusBarHeight(activity);
        view.setLayoutParams(params);
    }

    public static int getStatusBarHeight(Activity activity) {
        ContextWrapper contextWrapper = (ContextWrapper) activity;
        int result = 0;
        int resourceId = contextWrapper.getResources().getIdentifier("status_bar_height", "dimen", "android");
        if (resourceId > 0) {
            result = contextWrapper.getResources().getDimensionPixelSize(resourceId);
        } else {
            Rect rect = new Rect();
            activity.getWindow().getDecorView().getWindowVisibleDisplayFrame(rect);
            result = rect.top;
        }
        return result;
    }

    public static void setStatusBarFullScreen(Activity activity) {
        setStatusBarFullScreen(activity, ViewUtils.getColor(activity, android.R.color.transparent));
    }

    /**
     * Sets the status bar fullscreen with color.
     *
     * @param activity activity reference required.
     * @param color    background color for statusBar background.
     */
    public static void setStatusBarFullScreen(Activity activity, int color) {
        Window window = activity.getWindow();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);

            View decorView = window.getDecorView();
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
            } else {
                decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
            }
        }
        //set status background black
        window.setStatusBarColor(color);
    }

    public static void setStatusBarFullScreen(Window window, int color) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            //set status background black
            window.setStatusBarColor(color);
        }
    }

    public static void setLightStatusBar(Activity activity) {
        setLightStatusBar(activity, ViewUtils.getColor(activity, android.R.color.transparent));
    }

    /**
     * Sets the light status bar with color.
     *
     * @param activity activity reference required.
     * @param color    background color for statusBar background.
     */
    public static void setLightStatusBar(Activity activity, int color) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Window window = activity.getWindow();
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);

            View decorView = window.getDecorView();
            //set status text light
            decorView.setSystemUiVisibility(decorView.getSystemUiVisibility() & ~View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
            //set status background black
            window.setStatusBarColor(color);
        }
    }

    /**
     * Sets the status bar background color.
     *
     * @param activity activity reference required.
     * @param color    background color for statusBar background.
     */
    public static void setStatusBarBackgroundColor(Activity activity, int color) {
        setStatusBarBackgroundColor(activity, color, false);
    }

    /**
     * Sets the status bar background color.
     *
     * @param activity  activity reference required.
     * @param color     background color for statusBar background.
     * @param lightText set true to show light text color in status bar.
     */
    public static void setStatusBarBackgroundColor(Activity activity, int color, boolean lightText) {
        Window window = activity.getWindow();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);

            View decorView = window.getDecorView();
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (lightText) {
                    //set status text light
                    decorView.setSystemUiVisibility(decorView.getSystemUiVisibility() & ~View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
                } else {
                    decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
                }
            }
        }
        //set status background color
        window.setStatusBarColor(color);
    }

    // For more information, see https://issuetracker.google.com/issues/36911528
    public static void adjustResizeInFullscreenMode(Activity activity, boolean isIncludeCustomStatusBar) {
        adjustResizeInFullscreenMode(activity, null, isIncludeCustomStatusBar);
    }

    public static void adjustResizeInFullscreenMode(Activity activity, final WebView webView) {
        adjustResizeInFullscreenMode(activity, webView, false);
    }

    public static void adjustResizeInFullscreenMode(Activity activity, final WebView webView, boolean isIncludeCustomStatusBar) {
        FrameLayout content = (FrameLayout) activity.findViewById(android.R.id.content);
        View rootView = content.getChildAt(0);
        View decorView = activity.getWindow().getDecorView();
        decorView.getViewTreeObserver().addOnGlobalLayoutListener(() -> {
            Rect rect = new Rect();
            decorView.getWindowVisibleDisplayFrame(rect);
            final int diff;
            if (isIncludeCustomStatusBar) {
                diff = rootView.getRootView().getHeight() - rect.bottom;
            } else {
                diff = rootView.getContext().getResources().getDisplayMetrics().heightPixels - rect.bottom;
            }

            if (webView != null) {
                if (rootView.getPaddingBottom() != diff) {
                    // showing/hiding the soft keyboard
                    rootView.setPadding(rootView.getPaddingLeft(), rootView.getPaddingTop(), rootView.getPaddingRight(), diff);
                } else {
                    // soft keyboard shown/hidden and padding changed
                    if (diff != 0) {
                        // soft keyboard shown, scroll active element into view in case it is blocked by the soft keyboard
                        webView.evaluateJavascript("if (document.activeElement) { document.activeElement.scrollIntoView({behavior: \"smooth\", block: \"center\", inline: \"nearest\"}); }", null);
                    }
                }

            } else {
                if (diff != 0) {
                    if (rootView.getPaddingBottom() != diff) {
                        rootView.setPadding(0, 0, 0, diff);
                    }
                } else {
                    if (rootView.getPaddingBottom() != 0) {
                        rootView.setPadding(0, 0, 0, 0);
                    }
                }

            }
        });
    }

    public static int WindowOverlayType() {
        // https://stackoverflow.com/questions/45867533/system-alert-window-permission-on-api-26-not-working-as-expected-permission-den
        //if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        //    return WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        //} else {
        //    return WindowManager.LayoutParams.TYPE_SYSTEM_ERROR;
        //}

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            return WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            return WindowManager.LayoutParams.TYPE_SYSTEM_OVERLAY;
        } else {
            return WindowManager.LayoutParams.TYPE_PHONE;
        }
    }

    /**
     * Color utils
     */
    public static int mixColorForDrawable(float fraction, int startValue, int endValue) {
        int startA = (startValue >> 24) & 0xff;
        int startR = (startValue >> 16) & 0xff;
        int startG = (startValue >> 8) & 0xff;
        int startB = startValue & 0xff;

        int endA = (endValue >> 24) & 0xff;
        int endR = (endValue >> 16) & 0xff;
        int endG = (endValue >> 8) & 0xff;
        int endB = endValue & 0xff;

        return (startA + (int) (fraction * (endA - startA))) << 24 | (startR + (int) (fraction * (endR - startR))) << 16 | (startG + (int) (fraction * (endG - startG))) << 8 | (startB + (int) (fraction * (endB - startB)));
    }

    public static boolean isColorGrayscale(int pixel) {
        int alpha = (pixel & 0xFF000000) >> 24;
        int red = (pixel & 0x00FF0000) >> 16;
        int green = (pixel & 0x0000FF00) >> 8;
        int blue = (pixel & 0x000000FF);

        return red == green && green == blue;

    }

    public static boolean isColorTooDark(int color) {
        final byte RED_CHANNEL = 16;
        final byte GREEN_CHANNEL = 8;
        //final byte BLUE_CHANNEL = 0;

        int r = ((int) ((float) (color >> RED_CHANNEL & 0xff) * 0.3f)) & 0xff;
        int g = ((int) ((float) (color >> GREEN_CHANNEL & 0xff) * 0.59)) & 0xff;
        int b = ((int) ((float) (color /* >> BLUE_CHANNEL */ & 0xff) * 0.11)) & 0xff;
        int gr = (r + g + b) & 0xff;
        int gray = gr /* << BLUE_CHANNEL */ + (gr << GREEN_CHANNEL) + (gr << RED_CHANNEL);

        return gray < 0x727272;
    }

    public static int mixTwoColors(int color1, int color2, float amount) {
        final byte ALPHA_CHANNEL = 24;
        final byte RED_CHANNEL = 16;
        final byte GREEN_CHANNEL = 8;
        //final byte BLUE_CHANNEL = 0;

        final float inverseAmount = 1.0f - amount;

        int r = ((int) (((float) (color1 >> RED_CHANNEL & 0xff) * amount) + ((float) (color2 >> RED_CHANNEL & 0xff) * inverseAmount))) & 0xff;
        int g = ((int) (((float) (color1 >> GREEN_CHANNEL & 0xff) * amount) + ((float) (color2 >> GREEN_CHANNEL & 0xff) * inverseAmount))) & 0xff;
        int b = ((int) (((float) (color1 & 0xff) * amount) + ((float) (color2 & 0xff) * inverseAmount))) & 0xff;

        return 0xff << ALPHA_CHANNEL | r << RED_CHANNEL | g << GREEN_CHANNEL | b;
    }

    public static int getDominantColor(Bitmap bitmap, int defaultColor) {
        if (bitmap == null) {
            //throw new NullPointerException();
            return defaultColor;
        }

        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        int size = width * height;
        int pixels[] = new int[size];

        Bitmap bitmap2 = bitmap.copy(Bitmap.Config.ARGB_4444, false);
        bitmap2.getPixels(pixels, 0, width, 0, 0, width, height);

        final List<HashMap<Integer, Integer>> colorMap = new ArrayList<HashMap<Integer, Integer>>();
        colorMap.add(new HashMap<Integer, Integer>());
        colorMap.add(new HashMap<Integer, Integer>());
        colorMap.add(new HashMap<Integer, Integer>());

        int color = 0;
        int r = 0;
        int g = 0;
        int b = 0;
        Integer rC, gC, bC;
        for (int i = 0; i < pixels.length; i++) {
            color = pixels[i];

            r = Color.red(color);
            g = Color.green(color);
            b = Color.blue(color);

            rC = colorMap.get(0).get(r);
            if (rC == null) rC = 0;
            colorMap.get(0).put(r, ++rC);

            gC = colorMap.get(1).get(g);
            if (gC == null) gC = 0;
            colorMap.get(1).put(g, ++gC);

            bC = colorMap.get(2).get(b);
            if (bC == null) bC = 0;
            colorMap.get(2).put(b, ++bC);
        }

        int[] rgb = new int[3];
        for (int i = 0; i < 3; i++) {
            int max = 0;
            int val = 0;
            for (Map.Entry<Integer, Integer> entry : colorMap.get(i).entrySet()) {
                if (entry.getValue() > max) {
                    max = entry.getValue();
                    val = entry.getKey();
                }
            }
            rgb[i] = val;
        }

        int dominantColor;
        try {
            dominantColor = Color.rgb(rgb[0], rgb[1], rgb[2]);
        } catch (Exception ignored) {
            dominantColor = defaultColor;
        }
        return dominantColor;
    }


    public static String getMimeType(String url) {
        String extension = MimeTypeMap.getFileExtensionFromUrl(url);
        String type = null;
        if (extension != null) {
            MimeTypeMap mimeType = MimeTypeMap.getSingleton();
            type = mimeType.getMimeTypeFromExtension(extension);
        }

        if (type == null) {
            type = "*/*";
        }
        return type;
    }

    public static String getRealPathFromURI(Context context, Uri contentUri) {
        String[] proj = {MediaStore.Audio.Media.DATA};
        try (Cursor cursor = context.getContentResolver().query(contentUri, proj, null, null, null)) {
            int column_index = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA);
            cursor.moveToFirst();
            return cursor.getString(column_index);
        }
    }

    /**
     * Convert File Uri To File Path
     */
    public static String convertUriToFilePath(final Context context, final Uri uri) {
        String path = null;
        if (DocumentsContract.isDocumentUri(context, uri)) {
            if (isExternalStorageDocument(uri)) {
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                final String type = split[0];

                if ("primary".equalsIgnoreCase(type)) {
                    path = Environment.getExternalStorageDirectory() + "/" + split[1];
                }
            } else if (isDownloadsDocument(uri)) {
                final String id = DocumentsContract.getDocumentId(uri);
                if (!TextUtils.isEmpty(id)) {
                    if (id.startsWith("raw:")) {
                        return id.replaceFirst("raw:", "");
                    }
                }

                final Uri contentUri = ContentUris.withAppendedId(Uri.parse("content://downloads/public_downloads"), Long.valueOf(id));

                path = getDataColumn(context, contentUri, null, null);
            } else if (isMediaDocument(uri)) {
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                final String type = split[0];

                Uri contentUri = null;
                if ("image".equals(type)) {
                    contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
                } else if ("video".equals(type)) {
                    contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
                } else if ("audio".equals(type)) {
                    contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
                }

                final String selection = MediaStore.Audio.Media._ID + "=?";
                final String[] selectionArgs = new String[]{split[1]};

                path = getDataColumn(context, contentUri, selection, selectionArgs);
            }
        } else if (ContentResolver.SCHEME_CONTENT.equalsIgnoreCase(uri.getScheme())) {
            if (isGooglePhotosUri(uri)) {
                return uri.getLastPathSegment();
            }

            path = getDataColumn(context, uri, null, null);
        } else if (ContentResolver.SCHEME_FILE.equalsIgnoreCase(uri.getScheme())) {
            path = uri.getPath();
        }

        if (path != null) {
            try {
                return URLDecoder.decode(path, "UTF-8");
            } catch (Exception e) {
                return null;
            }
        }
        return null;
    }

    private static String getDataColumn(Context context, Uri uri, String selection, String[] selectionArgs) {
        Cursor cursor = null;
        final String column = MediaStore.Images.Media.DATA;
        final String[] projection = {column};

        try {
            cursor = context.getContentResolver().query(uri, projection, selection, selectionArgs, null);
            if (cursor != null && cursor.moveToFirst()) {
                final int column_index = cursor.getColumnIndexOrThrow(column);
                return cursor.getString(column_index);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return null;
    }

    private static boolean isExternalStorageDocument(Uri uri) {
        return "com.android.externalstorage.documents".equals(uri.getAuthority());
    }

    private static boolean isDownloadsDocument(Uri uri) {
        return "com.android.providers.downloads.documents".equals(uri.getAuthority());
    }

    private static boolean isMediaDocument(Uri uri) {
        return "com.android.providers.media.documents".equals(uri.getAuthority());
    }

    private static boolean isGooglePhotosUri(Uri uri) {
        return "com.google.android.apps.photos.content".equals(uri.getAuthority());
    }

    private static boolean isGoogleDriveUri(Uri uri) {
        return "com.google.android.apps.docs.storage".equals(uri.getAuthority()) || "com.google.android.apps.docs.storage.legacy".equals(uri.getAuthority());
    }

    public static boolean isWhatsAppFile(Uri uri) {
        return "com.whatsapp.provider.media".equals(uri.getAuthority());
    }
    /* convertUriToFilePath */

    public static String getPath(Context context, final Uri uri) {
        String selection = null;
        String[] selectionArgs = null;

        // check here to KITKAT or new version
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            // ExternalStorageProvider
            if (isExternalStorageDocument(uri)) {
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                final String type = split[0];

                String fullPath = getPathFromExtSD(split);
                if (!fullPath.equals("")) {
                    return fullPath;
                } else {
                    return null;
                }
            }

            // DownloadsProvider
            if (isDownloadsDocument(uri)) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    final String id;
                    try (Cursor cursor = context.getContentResolver().query(uri, new String[]{MediaStore.MediaColumns.DISPLAY_NAME}, null, null, null)) {
                        if (cursor != null && cursor.moveToFirst()) {
                            String fileName = cursor.getString(0);
                            String path = Environment.getExternalStorageDirectory().toString() + "/Download/" + fileName;
                            if (!TextUtils.isEmpty(path)) {
                                return path;
                            }
                        }
                    }
                    id = DocumentsContract.getDocumentId(uri);
                    if (!TextUtils.isEmpty(id)) {
                        if (id.startsWith("raw:")) {
                            return id.replaceFirst("raw:", "");
                        }
                        String[] contentUriPrefixesToTry = new String[]{"content://downloads/public_downloads", "content://downloads/my_downloads"};
                        for (String contentUriPrefix : contentUriPrefixesToTry) {
                            try {
                                final Uri contentUri = ContentUris.withAppendedId(Uri.parse(contentUriPrefix), Long.parseLong(id));
                                return getDataColumn(context, contentUri, null, null);
                            } catch (NumberFormatException e) {
                                //In Android 8 and Android P the id is not a number
                                return uri.getPath().replaceFirst("^/document/raw:", "").replaceFirst("^raw:", "");
                            }
                        }

                    }
                } else {
                    final String id = DocumentsContract.getDocumentId(uri);
                    if (id.startsWith("raw:")) {
                        return id.replaceFirst("raw:", "");
                    }
                    Uri contentUri = null;
                    try {
                        contentUri = ContentUris.withAppendedId(Uri.parse("content://downloads/public_downloads"), Long.parseLong(id));
                    } catch (NumberFormatException e) {
                        e.printStackTrace();
                    }
                    if (contentUri != null) {
                        return getDataColumn(context, contentUri, null, null);
                    }
                }
            }

            // MediaProvider
            if (isMediaDocument(uri)) {
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                final String type = split[0];

                Uri contentUri = null;
                if ("image".equals(type)) {
                    contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
                } else if ("video".equals(type)) {
                    contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
                } else if ("audio".equals(type)) {
                    contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
                }
                selection = "_id=?";
                selectionArgs = new String[]{split[1]};
                return getDataColumn(context, contentUri, selection, selectionArgs);
            }

            if (isGoogleDriveUri(uri)) {
                return getDriveFilePath(context, uri);
            }
            if (isWhatsAppFile(uri)) {
                return getFilePathForWhatsApp(context, uri);
            }
            if ("content".equalsIgnoreCase(uri.getScheme())) {
                if (isGooglePhotosUri(uri)) {
                    return uri.getLastPathSegment();
                }
                if (isGoogleDriveUri(uri)) {
                    return getDriveFilePath(context, uri);
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    return copyFileToInternalStorage(context, uri, "userfiles");
                } else {
                    return getDataColumn(context, uri, null, null);
                }
            }
            if ("file".equalsIgnoreCase(uri.getScheme())) {
                return uri.getPath();
            }

        } else {
            if (isWhatsAppFile(uri)) {
                return getFilePathForWhatsApp(context, uri);
            }
            if ("content".equalsIgnoreCase(uri.getScheme())) {
                String[] projection = {MediaStore.Images.Media.DATA};
                try (Cursor cursor = context.getContentResolver().query(uri, projection, selection, selectionArgs, null)) {
                    int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
                    if (cursor.moveToFirst()) {
                        return cursor.getString(column_index);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

        }
        return null;
    }


    public static File fileFromUri(Uri uri) {
        File backupFile = new File(uri.getPath());
        String absolutePath = backupFile.getAbsolutePath();
        String filePath = absolutePath.substring(absolutePath.indexOf(":") + 1);
        return new File(filePath);
    }

    public static Bitmap bitmapFromFile(File file) {
        if (!file.exists()) return null;
        Bitmap bitmap;
        try {
            bitmap = BitmapFactory.decodeFile(file.getAbsolutePath());
        } catch (Exception e) {
            bitmap = null;
        }
        return bitmap;
    }

    private static boolean fileExists(String filePath) {
        File file = new File(filePath);
        return file.exists();
    }

    private static String getPathFromExtSD(String[] pathData) {
        final String type = pathData[0];
        final String relativePath = "/" + pathData[1];
        String fullPath = "";

        // on my Sony devices (4.4.4 & 5.1.1), `type` is a dynamic string
        // something like "71F8-2C0A", some kind of unique id per storage
        // don't know any API that can get the root path of that storage based on its id.
        //
        // so no "primary" type, but let the check here for other devices
        if ("primary".equalsIgnoreCase(type)) {
            fullPath = Environment.getExternalStorageDirectory() + relativePath;
            if (fileExists(fullPath)) {
                return fullPath;
            }
        }

        // Environment.isExternalStorageRemovable() is `true` for external and internal storage
        // so we cannot relay on it.
        //
        // instead, for each possible path, check if file exists
        // we'll start with secondary storage as this could be our (physically) removable sd card
        fullPath = System.getenv("SECONDARY_STORAGE") + relativePath;
        if (fileExists(fullPath)) {
            return fullPath;
        }

        fullPath = System.getenv("EXTERNAL_STORAGE") + relativePath;
        if (fileExists(fullPath)) {
            return fullPath;
        }

        return fullPath;
    }

    private static String getDriveFilePath(Context context, Uri uri) {
        Cursor returnCursor = context.getContentResolver().query(uri, null, null, null, null);
        /*
         * Get the column indexes of the data in the Cursor,
         *     * move to the first row in the Cursor, get the data,
         *     * and display it.
         * */
        int nameIndex = returnCursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
        int sizeIndex = returnCursor.getColumnIndex(OpenableColumns.SIZE);
        returnCursor.moveToFirst();
        String name = (returnCursor.getString(nameIndex));
        String size = (Long.toString(returnCursor.getLong(sizeIndex)));
        File file = new File(context.getCacheDir(), name);
        try {
            InputStream inputStream = context.getContentResolver().openInputStream(uri);
            FileOutputStream outputStream = new FileOutputStream(file);
            int read = 0;
            int maxBufferSize = 1024 * 1024;
            int bytesAvailable = inputStream.available();

            int bufferSize = Math.min(bytesAvailable, maxBufferSize);

            final byte[] buffers = new byte[bufferSize];
            while ((read = inputStream.read(buffers)) != -1) {
                outputStream.write(buffers, 0, read);
            }
            Log.e("File Size", "Size " + file.length());
            inputStream.close();
            outputStream.close();
            Log.e("File Path", "Path " + file.getPath());
            Log.e("File Size", "Size " + file.length());
        } catch (Exception e) {
            Log.e("Exception", e.getMessage());
        } finally {
            returnCursor.close();
        }
        return file.getPath();
    }

    /***
     * Used for Android Q+
     * @param uri file uri
     * @param newDirName if you want to create a directory, you can set this variable
     * @return String
     */
    private static String copyFileToInternalStorage(Context context, Uri uri, String newDirName) {
        Cursor returnCursor = context.getContentResolver().query(uri, new String[]{OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE}, null, null, null);

        /*
         * Get the column indexes of the data in the Cursor,
         *     * move to the first row in the Cursor, get the data,
         *     * and display it.
         * */
        int nameIndex = returnCursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
        int sizeIndex = returnCursor.getColumnIndex(OpenableColumns.SIZE);
        returnCursor.moveToFirst();
        String name = (returnCursor.getString(nameIndex));
        String size = (Long.toString(returnCursor.getLong(sizeIndex)));

        File output;
        if (!newDirName.equals("")) {
            File dir = new File(context.getFilesDir() + "/" + newDirName);
            if (!dir.exists()) {
                dir.mkdir();
            }
            output = new File(context.getFilesDir() + "/" + newDirName + "/" + name);
        } else {
            output = new File(context.getFilesDir() + "/" + name);
        }
        try {
            InputStream inputStream = context.getContentResolver().openInputStream(uri);
            FileOutputStream outputStream = new FileOutputStream(output);
            int read = 0;
            int bufferSize = 1024;
            final byte[] buffers = new byte[bufferSize];
            while ((read = inputStream.read(buffers)) != -1) {
                outputStream.write(buffers, 0, read);
            }

            inputStream.close();
            outputStream.close();

        } catch (Exception e) {
            Log.e("Exception", e.getMessage());
        } finally {
            returnCursor.close();
        }

        return output.getPath();
    }

    public static String getFilePathForWhatsApp(Context context, Uri uri) {
        return copyFileToInternalStorage(context, uri, "whatsapp");
    }


    /**
     * file uses
     */
    public static void saveStringContent(Context context, String fileName, String content) throws Exception {
        String data = "";
        if (!TextUtils.isEmpty(content)) data = content;
        FileOutputStream outputStream = context.openFileOutput(fileName, Context.MODE_PRIVATE);
        outputStream.write(data.getBytes(StandardCharsets.UTF_8));
    }

    public static void saveJsonObjectInArray(Context context, String fileName, JSONObject object) throws Exception {
        String strJsonArray = getStringFromFileName(context, fileName);
        if (TextUtils.isEmpty(strJsonArray)) strJsonArray = "[]";
        JSONArray array = new JSONArray(strJsonArray);
        array.put(object);
        FileOutputStream outputStream = context.openFileOutput(fileName, Context.MODE_PRIVATE);
        outputStream.write(array.toString().getBytes(StandardCharsets.UTF_8));
    }

    public static String getStringFromFileName(Context context, String fileName) throws IOException {
        try {
            FileInputStream fin = context.openFileInput(fileName);
            String ret = convertStreamToString(fin);
            fin.close();
            return ret;
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return "";
        }
    }

    public static String getStringFromFilePath(Context context, String filePath) throws IOException {
        try {
            FileInputStream fin = new FileInputStream(filePath);
            String ret = convertStreamToString(fin);
            fin.close();
            return ret;
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return "";
        }
    }

    public static String convertStreamToString(InputStream is) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        StringBuilder sb = new StringBuilder();
        String line = null;
        while ((line = reader.readLine()) != null) {
            sb.append(line).append("\n");
        }
        reader.close();
        return sb.toString();
    }

    public static List<String> convertStreamToStringList(InputStream is) throws IOException {
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(is));
            List<String> lines = new ArrayList<>();
            String line = null;
            while ((line = reader.readLine()) != null) {
                lines.add(line);
            }
            reader.close();
            return lines;
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    public static void checkAndClearFilesDir(Context context) {
        for (String fileName : context.fileList()) {
            if (fileName.startsWith("Log_") && fileName.endsWith(".txt")) {
                try {
                    FileInputStream fin = context.openFileInput(fileName);
                    List<String> lines = convertStreamToStringList(fin);
                    fin.close();

                    String firstLineTimeStamp = lines.get(0).split(" --> ")[0];
                    if (isOlderOneDay(firstLineTimeStamp)) {
                        context.deleteFile(fileName);
                    }
                } catch (Exception ignoreds) {
                    ignoreds.printStackTrace();
                }

            } else if (fileName.equals(Constants.USER_SMS_FILENAME)) {
                try {
                    String content = getStringFromFileName(context, fileName);
                    JSONArray jsonArray = new JSONArray(content);
                    if (jsonArray.length() != 0) {
                        JSONObject jsonObject = jsonArray.getJSONObject(0);
                        if (jsonObject.has("timeStamp") && isOlderThirtyDays(jsonObject.getString("timeStamp"))) {
                            context.deleteFile(fileName);
                        }
                    }
                } catch (Exception ignoreds) {
                    ignoreds.printStackTrace();
                }
            }
        }
    }

    /**
     * Date Time Utils
     */
    public static String getTimeStamp() {
        return sdf.format(new Date());
    }

    public static long dateToMillis(String dateString) throws ParseException {
        Date date = sdf.parse(dateString);
        assert date != null;
        return date.getTime();
    }

    public static boolean isOlderOneDay(String sourceDateTime) {
        Calendar twentyFourHourAgo = Calendar.getInstance();
        twentyFourHourAgo.add(Calendar.HOUR_OF_DAY, -24);
        Date hoursAgoDate = twentyFourHourAgo.getTime();

        try {
            Date sourceDate = sdf.parse(sourceDateTime);
            if (sourceDate == null) return false;
            return sourceDate.before(hoursAgoDate);
        } catch (ParseException e) {
            e.printStackTrace();
            return false;
        }
    }

    public static boolean isOlderThirtyDays(String sourceDateTime) {
        Calendar thirtyDaysAgo = Calendar.getInstance();
        thirtyDaysAgo.add(Calendar.DAY_OF_MONTH, -30);
        Date thirtyDaysAgoDate = thirtyDaysAgo.getTime();

        try {
            Date sourceDate = sdf.parse(sourceDateTime);
            if (sourceDate == null) return false;
            return sourceDate.before(thirtyDaysAgoDate);
        } catch (ParseException e) {
            e.printStackTrace();
            return false;
        }
    }

    public static PendingIntent getNotificationPendingIntent(Context context, int notificationId, Class<?> clz) {
        Intent intent = new Intent(context, clz);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        return PendingIntent.getActivity(context, notificationId, intent, getPendingIntentFlag());
    }

    public static int getPendingIntentFlag() {
        // Build.VERSION_CODES.S -> 31
        if (Build.VERSION.SDK_INT >= 31) {
            return PendingIntent.FLAG_IMMUTABLE;
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
            return PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT;
        else {
            return PendingIntent.FLAG_UPDATE_CURRENT;
        }
    }

    /**
     * For device info parameters
     */
    @SuppressLint("HardwareIds")
    public static String getDeviceID(Context context) {
        String deviceID = Build.SERIAL;
        if (deviceID == null || deviceID.trim().isEmpty() || deviceID.equals("unknown")) {
            try {
                deviceID = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID);
            } catch (Exception ignored) {
            }
        }
        return "model=" + Uri.encode(Build.MODEL) + "&manf=" + Build.MANUFACTURER + "&release=" + Build.VERSION.RELEASE + "&id=" + deviceID;
    }

    public static String getDeviceName() {
        String manufacturer = Build.MANUFACTURER;
        String model = Build.MODEL;
        String release = Build.VERSION.RELEASE;
        if (model.startsWith(manufacturer)) {
            return model + " " + release;
        } else {
            return manufacturer + " " + model + " " + release;
        }
    }

    public static boolean isAppInForeground() {
        ActivityManager.RunningAppProcessInfo appProcessInfo = new ActivityManager.RunningAppProcessInfo();
        ActivityManager.getMyMemoryState(appProcessInfo);
        return (appProcessInfo.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND || appProcessInfo.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_VISIBLE);
    }

    public static boolean isAppInForeground(Context context) {
        ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningAppProcessInfo> services = activityManager.getRunningAppProcesses();
        boolean isActivityFound = false;
        if (services != null && services.get(0).processName.equalsIgnoreCase(context.getPackageName()) && services.get(0).importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND) {
            isActivityFound = true;
        }
        return isActivityFound;
    }

    public static void enabledStrictMode() {
        // StrictMode.enableDefaults();
        StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder().build());
        StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder().build());
    }

    /**
     * Convert a translucent themed Activity
     * {@link android.R.attr#windowIsTranslucent} to a fullscreen opaque
     * Activity.
     * <p>
     * Call this whenever the background of a translucent Activity has changed
     * to become opaque. Doing so will allow the {@link android.view.Surface} of
     * the Activity behind to be released.
     * <p>
     * This call has no effect on non-translucent activities or on activities
     * with the {@link android.R.attr#windowIsFloating} attribute.
     */
    public static void convertActivityFromTranslucent(Activity activity) {
        try {
            Method method = Activity.class.getDeclaredMethod("convertFromTranslucent");
            method.setAccessible(true);
            method.invoke(activity);
        } catch (Throwable ignored) {
        }
    }

    /**
     * Convert a translucent themed Activity
     * {@link android.R.attr#windowIsTranslucent} back from opaque to
     * translucent following a call to
     * {@link #convertActivityFromTranslucent(android.app.Activity)} .
     * <p>
     * Calling this allows the Activity behind this one to be seen again. Once
     * all such Activities have been redrawn
     * <p>
     * This call has no effect on non-translucent activities or on activities
     * with the {@link android.R.attr#windowIsFloating} attribute.
     */
    public static void convertActivityToTranslucent(Activity activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            convertActivityToTranslucentAfterL(activity);
        } else {
            convertActivityToTranslucentBeforeL(activity);
        }
    }

    /**
     * Calling the convertToTranslucent method on platforms before Android 5.0
     */
    public static void convertActivityToTranslucentBeforeL(Activity activity) {
        try {
            Class<?>[] classes = Activity.class.getDeclaredClasses();
            Class<?> translucentConversionListenerClazz = null;
            for (Class clazz : classes) {
                if (clazz.getSimpleName().contains("TranslucentConversionListener")) {
                    translucentConversionListenerClazz = clazz;
                }
            }
            Method method = Activity.class.getDeclaredMethod("convertToTranslucent", translucentConversionListenerClazz);
            method.setAccessible(true);
            method.invoke(activity, new Object[]{null});
        } catch (Throwable ignored) {
        }
    }

    /**
     * Calling the convertToTranslucent method on platforms after Android 5.0
     */
    private static void convertActivityToTranslucentAfterL(Activity activity) {
        try {
            Method getActivityOptions = Activity.class.getDeclaredMethod("getActivityOptions");
            getActivityOptions.setAccessible(true);
            Object options = getActivityOptions.invoke(activity);

            Class<?>[] classes = Activity.class.getDeclaredClasses();
            Class<?> translucentConversionListenerClazz = null;
            for (Class clazz : classes) {
                if (clazz.getSimpleName().contains("TranslucentConversionListener")) {
                    translucentConversionListenerClazz = clazz;
                }
            }
            Method convertToTranslucent = Activity.class.getDeclaredMethod("convertToTranslucent", translucentConversionListenerClazz, ActivityOptions.class);
            convertToTranslucent.setAccessible(true);
            convertToTranslucent.invoke(activity, null, options);
        } catch (Throwable ignored) {
        }
    }


    /**
     * INTERNAL method used to get the default launcher activity for the app.
     * If there is no launchable activity, this returns null.
     *
     * @param context A valid context. Must not be null.
     * @return A valid activity class, or null if no suitable one is found
     */
    @SuppressWarnings("unchecked")
    @Nullable
    private static Class<? extends Activity> getLauncherActivity(@NonNull Context context) {
        Intent intent = context.getPackageManager().getLaunchIntentForPackage(context.getPackageName());
        if (intent != null && intent.getComponent() != null) {
            try {
                return (Class<? extends Activity>) Class.forName(intent.getComponent().getClassName());
            } catch (ClassNotFoundException e) {
                //Should not happen, print it to the log!
                Log.e("getLauncherActivity", "Failed when resolving the restart activity class via getLaunchIntentForPackage, stack trace follows!", e);
            }
        }

        return null;
    }

    /**
     * Logs Utils
     */
    public static void getLog(String msg) {
        Log.e("ErrorLog", msg);
    }

    public static void getLog(String tag, String msg) {
        Log.e(tag, msg);
    }

    public static void getLog(Context context, String content) {
        Log.e(context.getClass().getSimpleName(), content);
        try {
            String fileName = "Log_" + context.getClass().getSimpleName() + ".txt";
            OutputStreamWriter outputWriter = new OutputStreamWriter(context.openFileOutput(fileName, Context.MODE_APPEND));
            try (BufferedWriter bufferedWriter = new BufferedWriter(outputWriter)) {
                bufferedWriter.write(getTimeStamp() + " --> " + content + "\n");
            } catch (Exception ignored) {
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void printIntentExtras(Intent intent) {
        printIntentExtras("", intent);
    }

    public static void printIntentExtras(String tag, Intent intent) {
        if (intent.getData() != null) {
            Log.e(tag + " Intent Data --> ", "" + intent.getData());
        }
        Bundle extras = intent.getExtras();
        if (extras != null) {
            for (String key : extras.keySet()) {
                Object obj = extras.get(key);
                if (obj == null) {
                    Log.e(tag + " Intent Extras --> ", key + " - Null");
                } else if (obj instanceof String) {
                    Log.e(tag + " Intent Extras --> ", key + " - " + (String) obj);
                } else if (obj instanceof Boolean) {
                    Log.e(tag + " Intent Extras --> ", key + " - " + (Boolean) obj);
                } else if (obj instanceof Integer) {
                    Log.e(tag + " Intent Extras --> ", key + " - " + (Integer) obj);
                } else {
                    Log.e(tag + " Intent Extras --> ", key + " - " + obj);
                }
            }
        } else {
            Log.e(tag + " Intent --> ", "extra null");
        }
    }

}