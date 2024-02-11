package com.android.sus_client.oncrashactivity;

import static com.android.sus_client.utils.Utils.addOrRemoveProperty;
import static com.android.sus_client.utils.Utils.dpToPxInt;
import static com.android.sus_client.utils.Utils.getBorderlessButtonStyle;
import static com.android.sus_client.utils.Utils.getColorPrimary;
import static com.android.sus_client.utils.Utils.getWindowBackgroundColor;
import static com.android.sus_client.utils.Utils.setStatusBarBackgroundColor;
import static com.android.sus_client.utils.Utils.spToPx;
import static com.android.sus_client.utils.Utils.updateMargin;
import static com.android.sus_client.utils.Utils.updateWidthHeight;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
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
import com.android.sus_client.annotation.Nullable;
import com.android.sus_client.utils.Utils;
import com.android.sus_client.utils.ViewUtils;


public class DefaultErrorActivity extends Activity {

    private Button restartButton, moreInfoButton;

    private RelativeLayout getMainView() {
        RelativeLayout root = new RelativeLayout(this);
        LinearLayout layout = new LinearLayout(this);
        // define main layout characteristics
        layout.setGravity(Gravity.CENTER);
        layout.setOrientation(LinearLayout.VERTICAL);

        // Set generic layout parameters
        final LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);

        final ImageView ivCrashIcon = new ImageView(this);
        ivCrashIcon.setImageResource(android.R.drawable.stat_notify_error);
        ivCrashIcon.setImageTintList(ColorStateList.valueOf(Color.parseColor("#E0E63946")));
        layout.addView(ivCrashIcon, dpToPxInt(88), dpToPxInt(88));

        final TextView txtTitle = new TextView(this);
        txtTitle.setText("An unexpected error occurred.\nSorry for the inconvenience.");
        txtTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
        txtTitle.setTypeface(Typeface.DEFAULT_BOLD);
        txtTitle.setGravity(Gravity.CENTER);
        layout.addView(txtTitle, params);
        updateMargin(txtTitle, 0f, 20f, 0f, 0f);

        restartButton = new Button(this);
        restartButton.setText("Close app");
        layout.addView(restartButton, params);
        updateMargin(restartButton, 0f, 16f, 0f, 0f);

        moreInfoButton = new Button(this);
        moreInfoButton.setText("Error details");
        moreInfoButton.setTextColor(getColorPrimary(this));
        moreInfoButton.setBackgroundResource(getBorderlessButtonStyle(this));
        moreInfoButton.setPadding(16, 0, 16, 0);
        layout.addView(moreInfoButton, params);

        root.addView(layout);
        updateWidthHeight(layout, ViewGroup.MarginLayoutParams.MATCH_PARENT, ViewGroup.MarginLayoutParams.WRAP_CONTENT);
        updateMargin(layout, 16f);
        addOrRemoveProperty(layout, RelativeLayout.CENTER_IN_PARENT, true);
        return root;
    }

    private LinearLayout getDialogView(String title, String msg, View.OnClickListener onClickListener) {
        //Create LinearLayout Dynamically
        LinearLayout layout = new LinearLayout(this);

        //Setup Layout Attributes
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        layout.setLayoutParams(params);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setGravity(Gravity.START);
        layout.setBackgroundColor(Color.WHITE);
        layout.setPadding(16, 16, 16, 16);

        //Create a TextView to add to layout
        TextView txtTitle = new TextView(this);
        txtTitle.setText(title);
        txtTitle.setTextColor(Color.BLACK);
        txtTitle.setPadding(32, 32, 32, 32);
        txtTitle.setTextSize(TypedValue.COMPLEX_UNIT_PX, spToPx(17));

        TextView txtMsg = new TextView(this);
        txtMsg.setText(msg);
        txtMsg.setTextSize(TypedValue.COMPLEX_UNIT_PX, spToPx(12));
        txtMsg.setPadding(18, 18, 18, 18);
        txtMsg.setVerticalScrollBarEnabled(true);
        txtMsg.setMovementMethod(new ScrollingMovementMethod());
        txtMsg.setMaxHeight(ViewUtils.getDisplayHeight(this, 76));

        //Create button
        Button button = new Button(this);
        button.setText("Copy to clipboard");
        button.setOnClickListener(onClickListener);

        //Add Views to the layout
        layout.addView(txtTitle);
        layout.addView(txtMsg);
        layout.addView(button, LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        return layout;
    }

    @SuppressLint("PrivateResource")
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //This is needed to avoid a crash if the developer has not specified
        //an app-level theme that extends Theme.AppCompat
        setTheme(R.style.Theme_AppCompat_Light_DarkActionBar);
        setContentView(getMainView());
        setStatusBarBackgroundColor(this, getWindowBackgroundColor(this));

        //Close/restart button logic:
        //If a class if set, use restart.
        //Else, use close and just finish the app.
        //It is recommended that you follow this logic if implementing a custom error activity.

        final CaocConfig config = CustomActivityOnCrash.getConfigFromIntent(getIntent());
        if (config == null) {
            //This should never happen - Just finish the activity to avoid a recursive crash.
            finish();
            return;
        }

        if (config.isShowRestartButton() && config.getRestartActivityClass() != null) {
            restartButton.setText("Restart app");
            restartButton.setOnClickListener(v -> CustomActivityOnCrash.restartApplication(DefaultErrorActivity.this, config));
        } else {
            restartButton.setOnClickListener(v -> CustomActivityOnCrash.closeApplication(DefaultErrorActivity.this, config));
        }

        if (config.isShowErrorDetails()) {
            moreInfoButton.setOnClickListener(v -> {
                //We retrieve all the error data and show it
                String msg = CustomActivityOnCrash.getAllErrorDetailsFromIntent(DefaultErrorActivity.this, getIntent());
                try {
                    androidx.appcompat.app.AlertDialog dialog = new androidx.appcompat.app.AlertDialog.Builder(DefaultErrorActivity.this).setTitle("Error details").setMessage(msg).setPositiveButton("Close", null).setNeutralButton("Copy to clipboard", (dialog1, which) -> copyErrorToClipboard()).show();
                    TextView textView = dialog.findViewById(android.R.id.message);
                    if (textView != null) {
                        textView.setTextSize(TypedValue.COMPLEX_UNIT_PX, spToPx(12));
                    }
                } catch (Error ignored) {
                    Dialog dialog = new Dialog(DefaultErrorActivity.this);
                    dialog.setCancelable(true);
                    dialog.setContentView(getDialogView("Error details", msg, v1 -> {
                        copyErrorToClipboard();
                        dialog.dismiss();
                    }), new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
                    dialog.show();
                }
            });
        } else {
            moreInfoButton.setVisibility(View.GONE);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        Utils.setStatusBarTranslucent(this, getWindowBackgroundColor(this));
    }

    private void copyErrorToClipboard() {
        String errorInformation = CustomActivityOnCrash.getAllErrorDetailsFromIntent(DefaultErrorActivity.this, getIntent());

        ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);

        //Are there any devices without clipboard...?
        if (clipboard != null) {
            ClipData clip = ClipData.newPlainText("Error information", errorInformation);
            clipboard.setPrimaryClip(clip);
            Toast.makeText(DefaultErrorActivity.this, "Copied to clipboard", Toast.LENGTH_SHORT).show();
        }
    }

}
