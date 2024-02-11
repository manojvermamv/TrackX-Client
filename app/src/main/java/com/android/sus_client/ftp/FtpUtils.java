package com.android.sus_client.ftp;

import android.app.Dialog;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Color;
import android.text.method.ScrollingMovementMethod;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;


public class FtpUtils {


    /**
     * Convert byte array to hex string
     *
     * @param bytes
     */
    public static String bytesToHex(byte[] bytes) {
        StringBuilder sbuf = new StringBuilder();
        for (int idx = 0; idx < bytes.length; idx++) {
            int intVal = bytes[idx] & 0xff;
            if (intVal < 0x10) sbuf.append("0");
            sbuf.append(Integer.toHexString(intVal).toUpperCase());
        }
        return sbuf.toString();
    }

    /**
     * Get utf8 byte array.
     *
     * @param str
     * @return array of NULL if error was found
     */
    public static byte[] getUTF8Bytes(String str) {
        try {
            return str.getBytes("UTF-8");
        } catch (Exception ex) {
            return null;
        }
    }

    /**
     * Load UTF8withBOM or any ansi text file.
     *
     * @param filename
     * @return
     * @throws java.io.IOException
     */
    public static String loadFileAsString(String filename) throws java.io.IOException {
        final int BUFLEN = 1024;
        try (BufferedInputStream is = new BufferedInputStream(new FileInputStream(filename), BUFLEN)) {
            ByteArrayOutputStream baos = new ByteArrayOutputStream(BUFLEN);
            byte[] bytes = new byte[BUFLEN];
            boolean isUTF8 = false;
            int read, count = 0;
            while ((read = is.read(bytes)) != -1) {
                if (count == 0 && bytes[0] == (byte) 0xEF && bytes[1] == (byte) 0xBB && bytes[2] == (byte) 0xBF) {
                    isUTF8 = true;
                    baos.write(bytes, 3, read - 3); // drop UTF8 bom marker
                } else {
                    baos.write(bytes, 0, read);
                }
                count += read;
            }
            return isUTF8 ? baos.toString("UTF-8") : baos.toString();
        }
    }

    /**
     * Returns MAC address of the given interface name.
     *
     * @param interfaceName eth0, wlan0 or NULL=use first interface
     * @return mac address or empty string
     */
    public static String getMACAddress(String interfaceName) {
        try {
            List<NetworkInterface> interfaces = Collections.list(NetworkInterface.getNetworkInterfaces());
            for (NetworkInterface intf : interfaces) {
                if (interfaceName != null) {
                    if (!intf.getName().equalsIgnoreCase(interfaceName)) continue;
                }
                byte[] mac = intf.getHardwareAddress();
                if (mac == null) return "";
                StringBuilder buf = new StringBuilder();
                for (int idx = 0; idx < mac.length; idx++)
                    buf.append(String.format("%02X:", mac[idx]));
                if (buf.length() > 0) buf.deleteCharAt(buf.length() - 1);
                return buf.toString();
            }
        } catch (Exception ignored) {
        }
        return "";
        /*try {
            // this is so Linux hack
            return loadFileAsString("/sys/class/net/" +interfaceName + "/address").toUpperCase().trim();
        } catch (IOException ex) {
            return null;
        }*/
    }

    /**
     * Get IP address from first non-localhost interface
     *
     * @param useIPv4 true=return ipv4, false=return ipv6
     * @return address or empty string
     */
    public static String getIPAddress(boolean useIPv4) {
        try {
            for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements(); ) {
                NetworkInterface anInterface = en.nextElement();
                for (Enumeration<InetAddress> enumIpAddr = anInterface.getInetAddresses(); enumIpAddr.hasMoreElements(); ) {
                    InetAddress inetAddress = enumIpAddr.nextElement();
                    if (!inetAddress.isLoopbackAddress()) {
                        String sAddr = inetAddress.getHostAddress();
                        if (sAddr == null) throw new Exception("host address not found");
                        boolean isIPv4 = sAddr.indexOf(':') < 0;
                        if (useIPv4) {
                            if (isIPv4)
                                return sAddr;
                        } else {
                            if (!isIPv4) {
                                int delim = sAddr.indexOf('%'); // drop ip6 zone suffix
                                return delim < 0 ? sAddr.toUpperCase() : sAddr.substring(0, delim).toUpperCase();
                            }
                        }
                    }
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return null;
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

    private static float spToPx(float sp) {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, sp, Resources.getSystem().getDisplayMetrics());
    }

    private static int getDisplayHeight(Context context) {
        DisplayMetrics displayMetrics = new DisplayMetrics();
        WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        wm.getDefaultDisplay().getMetrics(displayMetrics);
        return displayMetrics.heightPixels;
    }

    private static int getDisplayHeight(Context context, float percentageHeight) {
        int displayHeight = getDisplayHeight(context);
        if (percentageHeight >= 0f && percentageHeight < 100f) {
            return calPercentage(displayHeight, percentageHeight);
        }
        return displayHeight;
    }

    private static int calPercentage(int value, float percentage) {
        return (int) (value * (percentage / 100.0f));
    }

    private static void log(String tag, String msg) {
        Log.e("FtpUtils: " + tag, msg);
    }

}