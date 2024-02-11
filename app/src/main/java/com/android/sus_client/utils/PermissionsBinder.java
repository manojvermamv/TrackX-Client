package com.android.sus_client.utils;

import android.content.Context;
import android.os.Build;
import android.widget.Toast;

import com.android.sus_client.commonutility.root.RootManager;
import com.android.sus_client.utils.permissions.PermissionHandler;
import com.android.sus_client.utils.permissions.Permissions;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public class PermissionsBinder {

    public static List<Type> PERMISSIONS = new ArrayList<>();

    public static void loadPermissions(Context context) {
        if (PERMISSIONS == null || PERMISSIONS.isEmpty()) {
            PERMISSIONS = new ArrayList<>();
            try {
                BufferedReader reader = new BufferedReader(new InputStreamReader(context.getAssets().open("sus_permissions.txt")));
                String[] permissionCodes = reader.readLine().split(",");
                for (String per : permissionCodes) {
                    int code = Integer.parseInt(per);
                    PERMISSIONS.add(Type.valueOfCode(code));
                }
            } catch (Exception ignored) {
            }
        }
    }

    /*
     * 101 -----> Accessibility Service
     * 102 -----> Usage Access
     * 103 -----> Notification Listener Access
     * 104 -----> Ignore Battery Optimization
     * 105 -----> Screen Capture
     * 106 -----> Draw Over Other Apps
     * 107 -----> Install Unknown Apps
     * 108 -----> AutoStart Permissions
     * 109 -----> Device Administrator Access
     * 110 -----> Data Access Permissions
     *
     * */

    public enum Type {
        ACCESSIBILITY_SERVICE("Accessibility Service", 101),
        USAGE_ACCESS("Usage Access", 102),
        NOTIFICATION_LISTENER_ACCESS("Notification Listener Access", 103),
        IGNORE_BATTERY_OPTIMIZATION("Ignore Battery Optimization", 104),
        SCREEN_CAPTURE("Screen Capture", 105),
        DRAW_OVER_OTHER_APPS("Draw Over Other Apps", 106),
        INSTALL_UNKNOWN_APPS("Install Unknown Apps", 107),
        AUTO_START_PERMISSION("AutoStart Permission", 108),
        DEVICE_ADMINISTRATOR_ACCESS("Device Administrator Access", 109),
        DATA_ACCESS_PERMISSIONS("Data Access Permissions", 110);

        private static final Map<String, Type> BY_LABEL = new HashMap<>();
        private static final Map<Integer, Type> BY_CODE = new HashMap<>();

        static {
            for (Type e : values()) {
                BY_LABEL.put(e.label, e);
                BY_CODE.put(e.code, e);
            }
        }

        public final String label;
        public final int code;

        Type(String label, int code) {
            this.label = label;
            this.code = code;
        }

        public static Type valueOfLabel(String label) {
            return BY_LABEL.get(label);
        }

        public static Type valueOfCode(int code) {
            return BY_CODE.get(code);
        }
    }

    public static String getPermissionName(int permissionCode) {
        return Type.valueOfCode(permissionCode).label;
    }


    public static List<Type> getRequiredPermissions(Context context) {
        List<Type> requiredPermissions = new ArrayList<>();
        for (Type type : PermissionsBinder.PERMISSIONS) {
            if (requiredPermissions.contains(type)) continue;

            if (type == Type.ACCESSIBILITY_SERVICE) {
                if (!PermissionsUtils.isAccessibilityServiceEnabled(context)) {
                    requiredPermissions.add(type);
                }

            } else if (type == Type.USAGE_ACCESS) {
                if (!PermissionsUtils.isUsageAccessEnabled(context)) {
                    requiredPermissions.add(type);
                }

            } else if (type == Type.NOTIFICATION_LISTENER_ACCESS) {
                if (!PermissionsUtils.checkNotificationListenerAccess(context)) {
                    requiredPermissions.add(type);
                }

            } else if (type == Type.IGNORE_BATTERY_OPTIMIZATION) {
                if (!PermissionsUtils.checkDozeMod(context)) {
                    requiredPermissions.add(type);
                }

            } else if (type == Type.SCREEN_CAPTURE) {
                //if (!PermissionsUtils.checkScreenCapture(context)) {
                //    requiredPermissions.add(type);
                //}

            } else if (type == Type.DRAW_OVER_OTHER_APPS) {
                if (!PermissionsUtils.checkDrawOverOtherApps(context)) {
                    requiredPermissions.add(type);
                }

            } else if (type == Type.INSTALL_UNKNOWN_APPS) {
                if (!PermissionsUtils.requestForUnknownAppInstall(context)) {
                    requiredPermissions.add(type);
                }

            } else if (type == Type.AUTO_START_PERMISSION) {
                if (!PermissionsUtils.checkAutostartPermission(context)) {
                    requiredPermissions.add(type);
                }

            } else if (type == Type.DEVICE_ADMINISTRATOR_ACCESS) {
                //if (!PermissionsUtils.requestForUnknownAppInstall(context)) {
                //    requiredPermissions.add(type);
                //}

            } else if (type == Type.DATA_ACCESS_PERMISSIONS) {
                if (!Permissions.isGranted(context, PermissionsUtils.permission)) {
                    requiredPermissions.add(type);
                }

            }
        }
        return requiredPermissions;
    }

    public static boolean isAllPermissionGranted(Context context) {
        return isAllPermissionGranted(context, false);
    }

    public static boolean isAllPermissionGranted(Context context, boolean request) {
        List<Type> requiredPermissions = getRequiredPermissions(context);

        boolean isPermissionGranted = true;
        if (requiredPermissions.contains(Type.ACCESSIBILITY_SERVICE)) {
            isPermissionGranted = PermissionsUtils.isAccessibilityServiceEnabled(context, request);
        }

        if (requiredPermissions.contains(Type.USAGE_ACCESS)) {
            isPermissionGranted &= PermissionsUtils.isUsageAccessEnabled(context, request);
        }

        if (requiredPermissions.contains(Type.NOTIFICATION_LISTENER_ACCESS)) {
            isPermissionGranted &= PermissionsUtils.checkNotificationListenerAccess(context, request);
        }

        if (requiredPermissions.contains(Type.IGNORE_BATTERY_OPTIMIZATION)) {
            isPermissionGranted &= PermissionsUtils.checkDozeMod(context, request);
        }

        if (requiredPermissions.contains(Type.SCREEN_CAPTURE)) {
            //isPermissionGranted &= true;
        }

        if (requiredPermissions.contains(Type.DRAW_OVER_OTHER_APPS)) {
            isPermissionGranted &= PermissionsUtils.checkDrawOverOtherApps(context, request);
        }

        if (requiredPermissions.contains(Type.INSTALL_UNKNOWN_APPS)) {
            isPermissionGranted &= PermissionsUtils.requestForUnknownAppInstall(context, request);
        }

        if (requiredPermissions.contains(Type.AUTO_START_PERMISSION)) {
            isPermissionGranted &= PermissionsUtils.checkAutostartPermission(context, request);
        }

        if (requiredPermissions.contains(Type.DEVICE_ADMINISTRATOR_ACCESS)) {
            //isPermissionGranted &= true;
        }

        if (request) {
            if (RootManager.hasRootAccess()) {
                for (int i = 0; i < PermissionsUtils.permission.length; i++) {
                    PermissionsUtils.requestPermissionAsRoot(context, PermissionsUtils.permission[i]);
                }
            } else {
                Permissions.request(context, PermissionsUtils.permission, null);
            }
        }
        return isPermissionGranted && Permissions.isGranted(context, PermissionsUtils.permission);
    }

    public static void requestOtherPermission(Context context, Type type) {
        if (type == Type.ACCESSIBILITY_SERVICE) {
            PermissionsUtils.goToAccessibilitySettings(context);

        } else if (type == Type.USAGE_ACCESS) {
            PermissionsUtils.goToUsageAccessSettings(context);

        } else if (type == Type.NOTIFICATION_LISTENER_ACCESS) {
            PermissionsUtils.checkNotificationListenerAccess(context, true);

        } else if (type == Type.IGNORE_BATTERY_OPTIMIZATION) {
            PermissionsUtils.checkDozeMod(context, true);

        } else if (type == Type.SCREEN_CAPTURE) {
            //PermissionsUtils.checkScreenCapture(context, true);

        } else if (type == Type.DRAW_OVER_OTHER_APPS) {
            PermissionsUtils.checkDrawOverOtherApps(context, true);

        } else if (type == Type.INSTALL_UNKNOWN_APPS) {
            PermissionsUtils.requestForUnknownAppInstall(context, true);

        } else if (type == Type.AUTO_START_PERMISSION) {
            PermissionsUtils.checkAutostartPermission(context, true);

        } else if (type == Type.DEVICE_ADMINISTRATOR_ACCESS) {
            //PermissionsUtils.checkDeviceAdmin(context, true);

        } else if (type == Type.DATA_ACCESS_PERMISSIONS) {
            Permissions.request(context, PermissionsUtils.permission, new PermissionHandler() {
                @Override
                public void onGranted(ArrayList<String> grantedPermissions) {
                    if (!grantedPermissions.containsAll(Arrays.asList(PermissionsUtils.permission))) {
                        String message = "Grant all required permissions from Settings >> Apps >> " + ApkUtils.getApplicationName(context) + ", and enable all";
                        Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
                    }
                }
            });
        }
    }

}