package com.android.sus_client.commonutility.widget;

import android.annotation.SuppressLint;
import android.app.Service;
import android.content.Context;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.os.Handler;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.Toast;

import com.android.sus_client.utils.PermissionsUtils;
import com.android.sus_client.utils.Utils;

public class WindowOverlayFrameLayout extends FrameLayout {

    private final Context context;
    private final WindowManager windowManager;
    private final Handler mainHandler;

    private int previewWidth = 1;
    private int previewHeight = 1;

    public WindowOverlayFrameLayout(Context context) {
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

    private void addWindowOverlayInternally() throws RuntimeException {
        if (!PermissionsUtils.checkDrawOverOtherApps(context)) {
            throw new RuntimeException("Draw over other apps permission not granted!");
        }

        if (this.getParent() != null) {
            ((ViewGroup) this.getParent()).removeView(this);
        }
        setLayoutParams(new ViewGroup.LayoutParams(previewWidth, previewHeight));

        WindowManager.LayoutParams winParams = new WindowManager.LayoutParams(previewWidth, previewHeight, Utils.WindowOverlayType(), Utils.WIN_FLAG_TOUCH_OUTSIDE, PixelFormat.RGBA_8888);
        winParams.gravity = Gravity.END | Gravity.BOTTOM;
        winParams.x = 0;
        winParams.y = 0;
        windowManager.addView(this, winParams);
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        showToast("Drag & drop: onTouchEvent");
        // Check if the user has released their finger.
        if (event.getAction() == MotionEvent.ACTION_UP) {
            // Get the visible rectangle of the PiP screen.
            Rect visibleRect = new Rect();
            getWindowVisibleDisplayFrame(visibleRect);

            // Get the coordinates of the touch event.
            float x = event.getX();
            float y = event.getY();

            // Check if the user has moved their finger away from the PiP screen.
            if (!visibleRect.contains((int) x, (int) y)) {
                // Perform the desired action. For example, you could move the PiP window to a new location.
                showToast("Drag & drop: Location - " + (int) x + "x" + (int) y);
            }
        }
        return false;
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

    private void showToast(String s) {
        Toast.makeText(getContext().getApplicationContext(), s, Toast.LENGTH_SHORT).show();
    }

}