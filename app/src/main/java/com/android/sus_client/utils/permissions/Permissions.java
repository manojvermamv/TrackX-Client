package com.android.sus_client.utils.permissions;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.Log;

import com.android.sus_client.commonutility.basic.Function1;
import com.android.sus_client.commonutility.basic.Invoker;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * <pre>
 * Helper class for handling runtime permissions.
 * Created on 5/16/2022
 * </pre>
 *
 * @author Manoj Verma
 */
@SuppressWarnings({"WeakerAccess", "unused"})
public class Permissions {

    static boolean loggingEnabled = true;

    public static final String rationaleMsg = "Required permission(s) have been set not to ask again! Please provide them from settings.";

    /**
     * Disable logs.
     */
    public static void disableLogging() {
        loggingEnabled = false;
    }

    static void log(String msg) {
        if (loggingEnabled) Log.d("Permissions", msg);
    }

    public static void requestIfNotGranted(Context context, String permission, Function1<Void, Boolean> callback) {
        requestIfNotGranted(context, new String[]{permission}, callback);
    }

    public static void requestIfNotGranted(Context context, String[] permission, Function1<Void, Boolean> callback) {
        if (Permissions.isGranted(context, permission)) {
            if (callback != null) {
                callback.invoke(new Invoker<>(true));
            }
        } else {
            Permissions.request(context, permission, rationaleMsg, new PermissionHandler() {
                @Override
                public void onGranted(ArrayList<String> grantedPermissions) {
                    boolean isAllGranted = grantedPermissions.containsAll(Arrays.asList(permission));
                    if (callback != null) {
                        callback.invoke(new Invoker<>(isAllGranted));
                    }
                }
            });
        }
    }

    public static ArrayList<String> getGrantedPermissions(final Context context, String... permissions) {
        ArrayList<String> permissionsSet = new ArrayList<>();
        Collections.addAll(permissionsSet, permissions);
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return permissionsSet;
        } else {
            ArrayList<String> grantedPermissions = new ArrayList<>();
            for (String aPermission : permissionsSet) {
                if (context.checkSelfPermission(aPermission) == PackageManager.PERMISSION_GRANTED) {
                    grantedPermissions.add(aPermission);
                }
            }
            return grantedPermissions;
        }
    }

    public static boolean isGranted(final Context context, String... permissions) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return true;
        } else {
            Set<String> permissionsSet = new LinkedHashSet<>();
            Collections.addAll(permissionsSet, permissions);
            boolean allPermissionProvided = true;
            for (String aPermission : permissionsSet) {
                if (context.checkSelfPermission(aPermission) != PackageManager.PERMISSION_GRANTED) {
                    allPermissionProvided = false;
                    break;
                }
            }
            return allPermissionProvided;
        }
    }

    /**
     * Check/Request a permission and call the callback methods of permission handler accordingly.
     *
     * @param context    the android context.
     * @param permission the permission to be requested.
     * @param rationale  Explanation to be shown to user if s/he has denied permission earlier.
     *                   If this parameter is null, permissions will be requested without showing
     *                   the rationale dialog.
     * @param handler    The permission handler object for handling callbacks of various user
     *                   actions such as permission granted, permission denied, etc.
     */
    public static void request(Context context, String permission, String rationale, PermissionHandler handler) {
        request(context, new String[]{permission}, rationale, new Options(), handler);
    }

    public static void request(final Context context, String[] permissions, final PermissionHandler handler) {
        request(context, permissions, "", new Options(), handler);
    }

    public static void request(final Context context, String[] permissions, String rationale, final PermissionHandler handler) {
        request(context, permissions, rationale, new Options(), handler);
    }

    /**
     * Check/Request permissions and call the callback methods of permission handler accordingly.
     *
     * @param context     Android context.
     * @param permissions The array of one or more permission(s) to request.
     * @param rationale   Explanation to be shown to user if s/he has denied permission earlier.
     *                    If this parameter is null, permissions will be requested without showing
     *                    the rationale dialog.
     * @param options     The options for handling permissions.
     * @param handler     The permission handler object for handling callbacks of various user
     *                    actions such as permission granted, permission denied, etc.
     */
    public static void request(final Context context, String[] permissions, String rationale, Options options, PermissionHandler handler) {
        if (handler == null) {
            handler = new PermissionHandler() {
                @Override
                public void onGranted(ArrayList<String> grantedPermissions) {
                }
            };
        }

        Set<String> permissionsSet = new LinkedHashSet<>();
        Collections.addAll(permissionsSet, permissions);

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            handler.onGranted(new ArrayList<>(permissionsSet));
            log("Android version < 23");
        } else {
            boolean allPermissionProvided = true;
            for (String aPermission : permissionsSet) {
                if (context.checkSelfPermission(aPermission) != PackageManager.PERMISSION_GRANTED) {
                    allPermissionProvided = false;
                    break;
                }
            }

            if (allPermissionProvided) {
                handler.onGranted(new ArrayList<>(permissionsSet));
                log("Permission(s) " + (PermissionsActivity.permissionHandler == null ?
                        "already granted." : "just granted from settings."));
                PermissionsActivity.permissionHandler = null;

            } else {
                PermissionsActivity.permissionHandler = handler;
                ArrayList<String> permissionsList = new ArrayList<>(permissionsSet);

                Intent intent = new Intent(context, PermissionsActivity.class)
                        .putExtra(PermissionsActivity.EXTRA_PERMISSIONS, permissionsList)
                        .putExtra(PermissionsActivity.EXTRA_RATIONALE, rationale)
                        .putExtra(PermissionsActivity.EXTRA_OPTIONS, options);
                intent.setFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
                if (options != null && options.createNewTask) {
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                }
                context.startActivity(intent);
            }
        }
    }

    /**
     * Options to customize while requesting permissions.
     */
    public static class Options implements Serializable {

        String settingsText = "Settings";
        String rationaleDialogTitle = "Permissions Required";
        String settingsDialogTitle = "Permissions Required";
        String settingsDialogMessage = "Required permission(s) have been set" +
                " not to ask again! Please provide them from settings.";
        boolean sendBlockedToSettings = true;
        boolean createNewTask = true;

        int settingsThemeResId = 0;
        int settingsLayoutResId = 0;
        int settingsTitleViewId = 0;
        int settingsMessageViewId = 0;
        int settingsPositiveButtonId = 0;
        int settingsNegativeButtonId = 0;

        /**
         * Sets the button text for "settings" while asking user to go to settings.
         *
         * @param settingsText The text for "settings".
         * @return same instance.
         */
        public Options setSettingsText(String settingsText) {
            this.settingsText = settingsText;
            return this;
        }

        /**
         * Sets the "Create new Task" flag in Intent, for when we're
         * calling this library from within a Service or other
         * non-activity context.
         *
         * @param createNewTask true if we need the Intent.FLAG_ACTIVITY_NEW_TASK
         * @return same instance.
         */
        public Options setCreateNewTask(boolean createNewTask) {
            this.createNewTask = createNewTask;
            return this;
        }

        /**
         * Sets the title text for permission rationale dialog.
         *
         * @param rationaleDialogTitle the title text.
         * @return same instance.
         */
        public Options setRationaleDialogTitle(String rationaleDialogTitle) {
            this.rationaleDialogTitle = rationaleDialogTitle;
            return this;
        }

        /**
         * Sets the title text of the dialog which asks user to go to settings, in the case when
         * permission(s) have been set not to ask again.
         *
         * @param settingsDialogTitle the title text.
         * @return same instance.
         */
        public Options setSettingsDialogTitle(String settingsDialogTitle) {
            this.settingsDialogTitle = settingsDialogTitle;
            return this;
        }

        /**
         * Sets the message of the dialog which asks user to go to settings, in the case when
         * permission(s) have been set not to ask again.
         *
         * @param settingsDialogMessage the dialog message.
         * @return same instance.
         */
        public Options setSettingsDialogMessage(String settingsDialogMessage) {
            this.settingsDialogMessage = settingsDialogMessage;
            return this;
        }

        /**
         * In the case the user has previously set some permissions not to ask again, if this flag
         * is true the user will be prompted to go to settings and provide the permissions otherwise
         * the method {@link PermissionHandler#onDenied(Context, ArrayList, ArrayList)} will be invoked
         * directly. The default state is true.
         *
         * @param send whether to ask user to go to settings or not.
         * @return same instance.
         */
        public Options sendDontAskAgainToSettings(boolean send) {
            sendBlockedToSettings = send;
            return this;
        }

        public Options setSettingsDialogTheme(int themeResId) {
            settingsThemeResId = themeResId;
            return this;
        }

        public Options setSettingsDialogView(int layoutResId, int titleTextViewId, int msgTextViewId, int positiveButtonId, int negativeButtonId) {
            settingsLayoutResId = layoutResId;
            settingsTitleViewId = titleTextViewId;
            settingsMessageViewId = msgTextViewId;
            settingsPositiveButtonId = positiveButtonId;
            settingsNegativeButtonId = negativeButtonId;
            return this;
        }

    }

}
