package com.android.sus_client.utils.permissions;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.ActivityOptions;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.text.TextUtils;
import android.view.View;
import android.view.Window;
import android.widget.TextView;

import com.android.sus_client.utils.Utils;

import java.lang.reflect.Method;
import java.util.ArrayList;

/**
 * Created by Manoj Verma on 5/16/2022
 */

@SuppressWarnings("unchecked")
@TargetApi(Build.VERSION_CODES.M)
public class PermissionsActivity extends Activity {

    private static final int RC_SETTINGS = 6739;
    private static final int RC_PERMISSION = 6937;

    static final String EXTRA_PERMISSIONS = "permissions";
    static final String EXTRA_RATIONALE = "rationale";
    static final String EXTRA_OPTIONS = "options";

    static PermissionHandler permissionHandler;

    private ArrayList<String> allPermissions, deniedPermissions, grantedPermissions, noRationaleList;
    private Permissions.Options options;

    private final DialogInterface.OnCancelListener onDialogCancelListener =
            new DialogInterface.OnCancelListener() {
                @Override
                public void onCancel(DialogInterface dialog) {
                    deny();
                }
            };

    @TargetApi(Build.VERSION_CODES.M)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        convertActivityToTranslucent(this);
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setFinishOnTouchOutside(false);
        getWindow().setStatusBarColor(0);
        getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

        Intent intent = getIntent();
        if (intent == null || !intent.hasExtra(EXTRA_PERMISSIONS)) {
            finish();
            return;
        }

        allPermissions = (ArrayList<String>) intent.getSerializableExtra(EXTRA_PERMISSIONS);
        options = (Permissions.Options) intent.getSerializableExtra(EXTRA_OPTIONS);
        if (options == null) {
            options = new Permissions.Options();
        }
        deniedPermissions = new ArrayList<>();
        grantedPermissions = new ArrayList<>();
        noRationaleList = new ArrayList<>();

        boolean noRationale = true;
        for (String permission : allPermissions) {
            if (checkSelfPermission(permission) != PackageManager.PERMISSION_GRANTED) {
                deniedPermissions.add(permission);
                if (shouldShowRequestPermissionRationale(permission)) {
                    noRationale = false;
                } else {
                    noRationaleList.add(permission);
                }
            } else {
                grantedPermissions.add(permission);
            }
        }

        if (deniedPermissions.isEmpty()) {
            grant();
            return;
        }

        String rationale = intent.getStringExtra(EXTRA_RATIONALE);
        if (noRationale || TextUtils.isEmpty(rationale)) {
            Permissions.log("No rationale.");
            requestPermissions(toArray(deniedPermissions), RC_PERMISSION);
        } else {
            Permissions.log("Show rationale.");
            showRationale(rationale);
        }
    }

    private void showRationale(String rationale) {
        if (options.settingsLayoutResId == 0) {
            DialogInterface.OnClickListener listener = new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    if (which == DialogInterface.BUTTON_POSITIVE) {
                        requestPermissions(toArray(deniedPermissions), RC_PERMISSION);
                    } else {
                        deny();
                    }
                }
            };

            new AlertDialog.Builder(this, options.settingsThemeResId)
                    .setTitle(options.rationaleDialogTitle)
                    .setMessage(rationale)
                    .setPositiveButton(android.R.string.ok, listener)
                    .setNegativeButton(android.R.string.cancel, listener)
                    .setOnCancelListener(onDialogCancelListener).create().show();

        } else {
            View.OnClickListener listener = new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (view.getId() == options.settingsPositiveButtonId) {
                        requestPermissions(toArray(deniedPermissions), RC_PERMISSION);
                    } else {
                        deny();
                    }
                }
            };
            showCustomDialog(options.rationaleDialogTitle, rationale,
                    listener, listener, onDialogCancelListener);
        }
    }

    @Override
    protected void onStart() {
        overridePendingTransition(0, 0);
        super.onStart();
    }

    @Override
    protected void onResume() {
        super.onResume();
        Utils.setStatusBarTranslucent(this, Color.TRANSPARENT);
    }

    @SuppressWarnings("NullableProblems")
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (grantResults.length == 0) {
            deny();
        } else {
            deniedPermissions.clear();
            for (int i = 0; i < grantResults.length; i++) {
                if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                    deniedPermissions.add(permissions[i]);
                } else {
                    if (!grantedPermissions.contains(permissions[i])) {
                        grantedPermissions.add(permissions[i]);
                    }
                }
            }

            if (deniedPermissions.size() == 0) {
                Permissions.log("Just allowed.");
                grant();
            } else {
                ArrayList<String> blockedList = new ArrayList<>(); //set not to ask again.
                ArrayList<String> justBlockedList = new ArrayList<>(); //just set not to ask again.
                ArrayList<String> justDeniedList = new ArrayList<>();
                for (String permission : deniedPermissions) {
                    if (shouldShowRequestPermissionRationale(permission)) {
                        justDeniedList.add(permission);
                    } else {
                        blockedList.add(permission);
                        if (!noRationaleList.contains(permission)) {
                            justBlockedList.add(permission);
                        }
                    }
                }

                if (justBlockedList.size() > 0) { //checked don't ask again for at least one.
                    PermissionHandler permissionHandler = PermissionsActivity.permissionHandler;
                    finish();
                    if (permissionHandler != null) {
                        permissionHandler.onJustBlocked(getApplicationContext(), justBlockedList,
                                deniedPermissions, grantedPermissions);
                    }

                } else if (justDeniedList.size() > 0) { //clicked deny for at least one.
                    deny();

                } else { //unavailable permissions were already set not to ask again.
                    if (permissionHandler != null &&
                            !permissionHandler.onBlocked(getApplicationContext(), blockedList)) {
                        sendToSettings();

                    } else finish();
                }
            }
        }
    }

    private void sendToSettings() {
        if (!options.sendBlockedToSettings) {
            deny();
            return;
        }
        Permissions.log("Ask to go to settings.");

        if (options.settingsLayoutResId == 0) {
            DialogInterface.OnClickListener listener = new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    if (which == DialogInterface.BUTTON_POSITIVE) {
                        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                Uri.fromParts("package", getPackageName(), null));
                        startActivityForResult(intent, RC_SETTINGS);
                    } else {
                        deny();
                    }
                }
            };

            new AlertDialog.Builder(this, options.settingsThemeResId)
                    .setTitle(options.settingsDialogTitle)
                    .setMessage(options.settingsDialogMessage)
                    .setPositiveButton(options.settingsText, listener)
                    .setNegativeButton(android.R.string.cancel, listener)
                    .setOnCancelListener(onDialogCancelListener).create().show();

        } else {
            View.OnClickListener listener = new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (view.getId() == options.settingsPositiveButtonId) {
                        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                Uri.fromParts("package", getPackageName(), null));
                        startActivityForResult(intent, RC_SETTINGS);
                    } else if (view.getId() == options.settingsNegativeButtonId) {
                        deny();
                    }
                }
            };

            showCustomDialog(options.settingsDialogTitle, options.settingsDialogMessage,
                    listener, listener, onDialogCancelListener);
        }

    }

    private void showCustomDialog(String title, String message, View.OnClickListener positiveBtnListener, View.OnClickListener negativeBtnListener, DialogInterface.OnCancelListener onCancelListener) {
        if (options.settingsLayoutResId == 0) return;

        AlertDialog.Builder builder = new AlertDialog.Builder(this, options.settingsThemeResId);
        View dialogView = getLayoutInflater().inflate(options.settingsLayoutResId, null);
        builder.setView(dialogView);

        View titleView = dialogView.findViewById(options.settingsTitleViewId);
        View messageView = dialogView.findViewById(options.settingsMessageViewId);
        if (titleView instanceof TextView) {
            ((TextView) titleView).setText(title);
        }
        if (messageView instanceof TextView) {
            ((TextView) messageView).setText(message);
        }

        View positiveButton = dialogView.findViewById(options.settingsPositiveButtonId);
        View negativeButton = dialogView.findViewById(options.settingsNegativeButtonId);

        if (positiveButton != null) {
            positiveButton.setOnClickListener(positiveBtnListener);
        }
        if (negativeButton != null) {
            negativeButton.setOnClickListener(negativeBtnListener);
        }

        builder.setOnCancelListener(onCancelListener);
        builder.create().show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == RC_SETTINGS && permissionHandler != null) {
            Permissions.request(this, toArray(allPermissions), null, options,
                    permissionHandler);
        }
        // super, because overridden method will make the handler null, and we don't want that.
        super.finish();
        overridePendingTransition(0, 0);
    }

    private String[] toArray(ArrayList<String> arrayList) {
        int size = arrayList.size();
        String[] array = new String[size];
        for (int i = 0; i < size; i++) {
            array[i] = arrayList.get(i);
        }
        return array;
    }

    @Override
    public void finish() {
        permissionHandler = null;
        super.finish();
        overridePendingTransition(0, 0);
    }

    private void deny() {
        PermissionHandler permissionHandler = PermissionsActivity.permissionHandler;
        finish();
        if (permissionHandler != null) {
            permissionHandler.onDenied(getApplicationContext(), deniedPermissions, grantedPermissions);
        }
    }

    private void grant() {
        PermissionHandler permissionHandler = PermissionsActivity.permissionHandler;
        finish();
        if (permissionHandler != null) {
            permissionHandler.onGranted(grantedPermissions);
        }
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
            Method method = Activity.class.getDeclaredMethod("convertToTranslucent",
                    translucentConversionListenerClazz);
            method.setAccessible(true);
            method.invoke(activity, new Object[]{
                    null
            });
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
            Method convertToTranslucent = Activity.class.getDeclaredMethod("convertToTranslucent",
                    translucentConversionListenerClazz, ActivityOptions.class);
            convertToTranslucent.setAccessible(true);
            convertToTranslucent.invoke(activity, null, options);
        } catch (Throwable ignored) {
        }
    }

}