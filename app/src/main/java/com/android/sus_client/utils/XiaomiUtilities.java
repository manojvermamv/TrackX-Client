package com.android.sus_client.utils;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.AppOpsManager;
import android.content.Context;
import android.content.Intent;
import android.text.TextUtils;
import android.util.Log;

import java.lang.reflect.Method;

// MIUI. Redefining Android.
// (not in the very best way I'd say)
public class XiaomiUtilities {

    // custom permissions
    public static final int OP_ACCESS_XIAOMI_ACCOUNT = 10015;
    public static final int OP_AUTO_START = 10008;
    public static final int OP_BACKGROUND_START_ACTIVITY = 10021;
    public static final int OP_BLUETOOTH_CHANGE = 10002;
    public static final int OP_BOOT_COMPLETED = 10007;
    public static final int OP_DATA_CONNECT_CHANGE = 10003;
    public static final int OP_DELETE_CALL_LOG = 10013;
    public static final int OP_DELETE_CONTACTS = 10012;
    public static final int OP_DELETE_MMS = 10011;
    public static final int OP_DELETE_SMS = 10010;
    public static final int OP_EXACT_ALARM = 10014;
    public static final int OP_GET_INSTALLED_APPS = 10022;
    public static final int OP_GET_TASKS = 10019;
    public static final int OP_INSTALL_SHORTCUT = 10017;
    public static final int OP_NFC = 10016;
    public static final int OP_NFC_CHANGE = 10009;
    public static final int OP_READ_MMS = 10005;
    public static final int OP_READ_NOTIFICATION_SMS = 10018;
    public static final int OP_SEND_MMS = 10004;
    public static final int OP_SERVICE_FOREGROUND = 10023;
    public static final int OP_SHOW_WHEN_LOCKED = 10020;
    public static final int OP_WIFI_CHANGE = 10001;
    public static final int OP_WRITE_MMS = 10006;

    public static boolean isMIUI() {
        return !TextUtils.isEmpty(getSystemProperty("ro.miui.ui.version.name"));
    }

    @SuppressLint("PrivateApi")
    private static String getSystemProperty(String key) {
        try {
            Class props = Class.forName("android.os.SystemProperties");
            return (String) props.getMethod("get", String.class).invoke(null, key);
        } catch (Exception ignore) {
        }
        return null;
    }

    @SuppressWarnings("JavaReflectionMemberAccess")
    @TargetApi(19)
    public static boolean isPermissionGranted(Context context, int permission) {
        try {
            AppOpsManager mgr = (AppOpsManager) context.getSystemService(Context.APP_OPS_SERVICE);
            Method m = AppOpsManager.class.getMethod("checkOpNoThrow", int.class, int.class, String.class);
            int result = (int) m.invoke(mgr, permission, android.os.Process.myUid(), context.getPackageName());
            return result == AppOpsManager.MODE_ALLOWED;
        } catch (Exception e) {
            Log.e("XiaomiUtils", e.toString());
        }
        return true;
    }

    public static final String ACTION_APP_PERM_EDITOR = "miui.intent.action.APP_PERM_EDITOR";
    public static final String ACTION_APP_SEC_CENTER = "miui.intent.action.APP_SEC_CENTER";
    public static final String ACTION_RECENTS_APP_SETTINGS = "miui.intent.action.RECENTS_APP_SETTINGS";

    /**
     * The following actions are supported in MIUI devices programmatically:
     * <p>
     * miui.intent.action.APP_PERM_EDITOR: Opens the app permissions editor for the given app.
     * miui.intent.action.APP_SEC_CENTER: Opens the app security center for the given app.
     * miui.intent.action.BATTERY_INFO_SETTINGS: Opens the battery info settings page.
     * miui.intent.action.BLUETOOTH_SETTINGS: Opens the Bluetooth settings page.
     * miui.intent.action.BRIGHTNESS_SETTINGS: Opens the brightness settings page.
     * miui.intent.action.DATA_USAGE_SETTINGS: Opens the data usage settings page.
     * miui.intent.action.DEVICE_INFO_SETTINGS: Opens the device info settings page.
     * miui.intent.action.DISPLAY_SETTINGS: Opens the display settings page.
     * miui.intent.action.DNS_SETTINGS: Opens the DNS settings page.
     * miui.intent.action.DOZE_SETTINGS: Opens the Doze settings page.
     * miui.intent.action.EXTERNAL_STORAGE_SETTINGS: Opens the external storage settings page.
     * miui.intent.action.FINGERPRINT_SETTINGS: Opens the fingerprint settings page.
     * miui.intent.action.FLOAT_WINDOW_SETTINGS: Opens the floating window settings page.
     * miui.intent.action.GAME_TURBO_SETTINGS: Opens the Game Turbo settings page.
     * miui.intent.action.HOME_SCREEN_SETTINGS: Opens the home screen settings page.
     * miui.intent.action.INSTALL_APK: Installs the given APK file.
     * miui.intent.action.LANGUAGE_SETTINGS: Opens the language settings page.
     * miui.intent.action.LOCATION_SETTINGS: Opens the location settings page.
     * miui.intent.action.LOCK_SCREEN_SETTINGS: Opens the lock screen settings page.
     * miui.intent.action.MANAGE_APP_PERMISSIONS: Opens the app permissions manager page.
     * miui.intent.action.MEMORY_STORAGE_SETTINGS: Opens the memory and storage settings page.
     * miui.intent.action.MORE_SETTINGS: Opens the more settings page.
     * miui.intent.action.NETWORK_SETTINGS: Opens the network settings page.
     * miui.intent.action.NFC_SETTINGS: Opens the NFC settings page.
     * miui.intent.action.NOTIFICATION_SETTINGS: Opens the notification settings page.
     * miui.intent.action.OTHER_SETTINGS: Opens the other settings page.
     * miui.intent.action.POWER_SETTINGS: Opens the power settings page.
     * miui.intent.action.PRIVACY_PROTECTION_SETTINGS: Opens the privacy protection settings page.
     * miui.intent.action.RECENTS_APP_SETTINGS: Opens the recents app settings page.
     * miui.intent.action.SEARCH_SETTINGS: Opens the search settings page.
     * miui.intent.action.SECOND_SPACE_SETTINGS: Opens the second space settings page.
     * miui.intent.action.SECURITY_CENTER_SETTINGS: Opens the security center settings page.
     * miui.intent.action.SETTINGS: Opens the main settings page.
     * miui.intent.action.SIM_CARD_SETTINGS: Opens the SIM card settings page.
     * miui.intent.action.SOUND_SETTINGS: Opens the sound settings page.
     * miui.intent.action.STATUS_BAR_SETTINGS: Opens the status bar settings page.
     * miui.intent.action.SYSTEM_UPDATE_SETTINGS: Opens the system update settings page.
     * miui.intent.action.THEME_SETTINGS: Opens the theme settings page.
     * miui.intent.action.TIME_AND_DATE_SETTINGS: Opens the time and date settings page.
     * miui.intent.action.USAGE_STATS_SETTINGS: Opens the usage stats settings page.
     * miui.intent.action.USER_CENTER_SETTINGS: Opens the user center settings page.
     * miui.intent.action.WIFI_SETTINGS: Opens the Wi-Fi settings page.
     */

    public static Intent getPermissionManagerIntent(Context context) {
        Intent intent = new Intent(ACTION_APP_PERM_EDITOR);
        intent.putExtra("extra_package_uid", android.os.Process.myUid());
        intent.putExtra("extra_pkgname", context.getPackageName());
        intent.putExtra("extra_package_name", context.getPackageName());
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        return intent;
    }

    public static Intent getIntent(Context context, String action) {
        Intent intent = new Intent(action);
        intent.putExtra("extra_package_uid", android.os.Process.myUid());
        intent.putExtra("extra_pkgname", context.getPackageName());
        intent.putExtra("extra_package_name", context.getPackageName());
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        return intent;
    }

}