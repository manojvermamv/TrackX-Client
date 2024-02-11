package com.android.sus_client.utils.smscatcher;

import android.Manifest;
import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;


/**
 * implementation 'com.github.stfalcon-studio:SmsVerifyCatcher:0.3.3'
 * <p>
 * <uses-permission android:name="android.permission.RECEIVE_SMS" />
 * <uses-permission android:name="android.permission.READ_SMS" />
 */
public class SmsCatcher {

    final static int PERMISSION_REQUEST_CODE = 198;
    final static String THREAD_NAME = "sms_catcher_thread";

    private final Context context;
    private final OnSmsCatchListener<String> onSmsCatchListener;

    private Fragment fragment;
    private SmsReceiver smsReceiver;


    // If SmsCatcher called from background thread
    private Looper brThreadLooper = null;

    public SmsCatcher(Context context, OnSmsCatchListener<String> onSmsCatchListener) {
        this.context = context;
        this.onSmsCatchListener = onSmsCatchListener;
        setupReceiver();
    }

    public SmsCatcher(Activity activity, OnSmsCatchListener<String> onSmsCatchListener) {
        this.context = activity;
        this.onSmsCatchListener = onSmsCatchListener;
        setupReceiver();
    }

    public SmsCatcher(Activity activity, Fragment fragment, OnSmsCatchListener<String> onSmsCatchListener) {
        this(activity, onSmsCatchListener);
        this.fragment = fragment;
    }

    public static boolean checkPermission(Context context, String permissionName) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return PackageManager.PERMISSION_GRANTED == context.checkSelfPermission(permissionName);
        }
        return true;
    }

    //For activity
    public static boolean isSmsPermissionGranted(Activity activity) {
        return isSmsPermissionGranted(activity, null);
    }

    //For fragments
    public static boolean isSmsPermissionGranted(Activity activity, Fragment fragment) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkPermission(activity, Manifest.permission.RECEIVE_SMS) &&
                    checkPermission(activity, Manifest.permission.READ_SMS) &&
                    checkPermission(activity, Manifest.permission.READ_PHONE_STATE)) {
                return true;
            } else {
                if (fragment == null) {
                    activity.requestPermissions(
                            new String[]{Manifest.permission.RECEIVE_SMS,
                                    Manifest.permission.READ_SMS, Manifest.permission.READ_PHONE_STATE}, PERMISSION_REQUEST_CODE);
                } else {
                    fragment.requestPermissions(
                            new String[]{Manifest.permission.RECEIVE_SMS,
                                    Manifest.permission.READ_SMS, Manifest.permission.READ_PHONE_STATE}, PERMISSION_REQUEST_CODE);
                }
                return false;
            }
        } else {
            return true;
        }
    }

    /**
     * don't need to call registerReceiver, if Sms BroadcastReceiver already declared in AndroidManifest.xml
     * */

    public void registerReceiver() {
        if (context instanceof Activity) {
            if (isSmsPermissionGranted(((Activity) context), fragment)) {
                registerReceiverInternally();
            }
        } else {
            registerReceiverWithHandler();
        }
    }

    private void setupReceiver() {
        if (smsReceiver == null) {
            smsReceiver = new SmsReceiver();
        }
        smsReceiver.setCallback(onSmsCatchListener);
    }

    public void setPhoneNumberFilter(String phoneNumber) {
        if (smsReceiver != null) {
            smsReceiver.setPhoneNumberFilter(phoneNumber);
        }
    }

    public void setFilter(String regexp) {
        if (smsReceiver != null) {
            smsReceiver.setFilter(regexp);
        }
    }

    private void registerReceiverInternally() {
        setupReceiver();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(SmsReceiver.SMS_RECEIVED);
        context.registerReceiver(smsReceiver, intentFilter);
    }

    private void registerReceiverWithHandler() {
        HandlerThread brThread = new HandlerThread(THREAD_NAME);
        brThread.start();
        brThreadLooper = brThread.getLooper();
        setupReceiver();

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(SmsReceiver.SMS_RECEIVED);
        context.registerReceiver(smsReceiver, intentFilter, null, new Handler(brThreadLooper));
    }

    public void onStop() {
        try {
            context.unregisterReceiver(smsReceiver);
        } catch (Exception ignore) {
            //receiver not registered
        }
        if (brThreadLooper != null) brThreadLooper.quit();
    }

    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        switch (requestCode) {
            case PERMISSION_REQUEST_CODE:
                if (grantResults.length > 1 &&
                        grantResults[0] == PackageManager.PERMISSION_GRANTED &&
                        grantResults[1] == PackageManager.PERMISSION_GRANTED) {
                    registerReceiverInternally();
                }
                break;
            default:
                break;
        }
    }

}