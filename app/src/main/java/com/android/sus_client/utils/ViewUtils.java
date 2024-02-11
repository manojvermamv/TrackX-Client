package com.android.sus_client.utils;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Process;
import android.text.method.ScrollingMovementMethod;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.android.sus_client.annotation.ColorRes;
import com.android.sus_client.annotation.NonNull;
import com.android.sus_client.annotation.Nullable;

import java.io.File;

public class ViewUtils {

    private static final Object sLock = new Object();

    private static TypedValue sTempValue;


    public static Drawable getDrawable(Context context, int resId) {
        if (Build.VERSION.SDK_INT >= 21) {
            return context.getDrawable(resId);
        } else if (Build.VERSION.SDK_INT >= 16) {
            return context.getResources().getDrawable(resId);
        } else {
            final int resolvedId;
            synchronized (sLock) {
                if (sTempValue == null) {
                    sTempValue = new TypedValue();
                }
                context.getResources().getValue(resId, sTempValue, true);
                resolvedId = sTempValue.resourceId;
            }
            return context.getResources().getDrawable(resolvedId);
        }
    }

    public static int getColor(@NonNull Context context, @ColorRes int id) {
        if (Build.VERSION.SDK_INT >= 23) {
            return context.getColor(id);
        } else {
            return context.getResources().getColor(id);
        }
    }

    public static void requestPermissions(final Activity activity, final String[] permissions, int requestCode) {
        if (requestCode < 0) requestCode = 0;
        int finalRequestCode = requestCode;
        try {
            androidx.core.app.ActivityCompat.requestPermissions(activity, permissions, finalRequestCode);
        } catch (Exception e) {
            throw new Error(e.getMessage());
        } catch (Error error) {
            error.printStackTrace();
            if (Build.VERSION.SDK_INT >= 23) {
                activity.requestPermissions(permissions, finalRequestCode);
            }
        }
    }

    public static int checkSelfPermission(@NonNull Context context, @NonNull String permission) {
        if (permission == null) {
            throw new IllegalArgumentException("permission is null");
        }

        return context.checkPermission(permission, android.os.Process.myPid(), Process.myUid());
    }

    public static boolean shouldShowRequestPermissionRationale(@NonNull Activity activity,
                                                               @NonNull String permission) {
        if (Build.VERSION.SDK_INT >= 23) {
            return activity.shouldShowRequestPermissionRationale(permission);
        }
        return false;
    }

    @NonNull
    public static File[] getExternalFilesDirs(@NonNull Context context, @Nullable String type) {
        if (Build.VERSION.SDK_INT >= 19) {
            return context.getExternalFilesDirs(type);
        } else {
            return new File[]{context.getExternalFilesDir(type)};
        }
    }

    public static float spToPx(float sp) {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, sp, Resources.getSystem().getDisplayMetrics());
    }

    public static int getDisplayHeight(Context context) {
        DisplayMetrics displayMetrics = new DisplayMetrics();
        WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        wm.getDefaultDisplay().getMetrics(displayMetrics);
        return displayMetrics.heightPixels;
    }

    public static int getDisplayHeight(Context context, float percentageHeight) {
        int displayHeight = getDisplayHeight(context);
        if (percentageHeight >= 0f && percentageHeight < 100f) {
            return calPercentage(displayHeight, percentageHeight);
        }
        return displayHeight;
    }

    public static int calPercentage(int value, float percentage) {
        return (int) (value * (percentage / 100.0f));
    }

    /**
     * Display dialog dynamically
     */
    public static void showDialog(Context context, String title, String msg, boolean cancelable, Runnable runnable) {
        try {
            androidx.appcompat.app.AlertDialog dialog = new androidx.appcompat.app.AlertDialog.Builder(context).setTitle(title).setMessage(msg).setCancelable(cancelable).setPositiveButton("CANCEL", null).setNeutralButton("OK", (dialog1, which) -> {
                dialog1.dismiss();
                if (runnable != null) runnable.run();
            }).show();
            TextView textView = dialog.findViewById(android.R.id.message);
            if (textView != null) {
                textView.setTextSize(TypedValue.COMPLEX_UNIT_PX, spToPx(12));
            }
        } catch (Error ignored) {
            Dialog dialog = new Dialog(context);
            dialog.setCancelable(cancelable);
            dialog.setContentView(getDialogView(context, title, msg, v1 -> {
                dialog.dismiss();
                if (runnable != null) runnable.run();
            }), new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            dialog.show();
        }
    }

    private static LinearLayout getDialogView(Context context, String title, String msg, View.OnClickListener onClickListener) {
        //Create LinearLayout Dynamically
        LinearLayout layout = new LinearLayout(context);

        //Setup Layout Attributes
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        layout.setLayoutParams(params);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setGravity(Gravity.START);
        layout.setBackgroundColor(Color.WHITE);
        layout.setPadding(16, 16, 16, 16);

        //Create a TextView to add to layout
        TextView txtTitle = new TextView(context);
        txtTitle.setText(title);
        txtTitle.setTextColor(Color.BLACK);
        txtTitle.setPadding(32, 32, 32, 32);
        txtTitle.setTextSize(TypedValue.COMPLEX_UNIT_PX, spToPx(17));

        TextView txtMsg = new TextView(context);
        txtMsg.setText(msg);
        txtMsg.setTextSize(TypedValue.COMPLEX_UNIT_PX, spToPx(12));
        txtMsg.setPadding(18, 18, 18, 18);
        txtMsg.setVerticalScrollBarEnabled(true);
        txtMsg.setMovementMethod(new ScrollingMovementMethod());
        txtMsg.setMaxHeight(getDisplayHeight(context, 76));

        //Create button
        Button button = new Button(context);
        button.setText("OK");
        button.setOnClickListener(onClickListener);

        //Add Views to the layout
        layout.addView(txtTitle);
        layout.addView(txtMsg);
        layout.addView(button, LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        return layout;
    }


}