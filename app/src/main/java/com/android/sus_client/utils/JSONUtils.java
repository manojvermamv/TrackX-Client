package com.android.sus_client.utils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public class JSONUtils {

    //on JSONObject
    public static Boolean isEmpty(JSONObject json) {
        if (json != null) {
            return !json.keys().hasNext();
        }
        return true;
    }

    //on JSONArray
    public static Boolean isEmpty(JSONArray json) {
        if (json != null) {
            return json.length() == 0;
        }
        return true;
    }

    public static double checkObjectSize(byte[] bytes) {
        double size_bytes = bytes.length;
        double size_kb = size_bytes / 1024;
        System.out.println("Converted Data Size : " + size_kb + " KB");
        return size_kb;
    }

    public static void printObjectSizeKB(byte[] bytes) {
        double size_bytes = bytes.length;
        double size_kb = size_bytes / 1024;
        String cnt_size = size_kb + " KB";
        System.out.println("Converted Data Size : " + cnt_size);
    }

    public static String serializeObjectToString(Object object) {
        try {
            ByteArrayOutputStream arrayOutputStream = new ByteArrayOutputStream();
            GZIPOutputStream gzipOutputStream = new GZIPOutputStream(arrayOutputStream);
            ObjectOutputStream objectOutputStream = new ObjectOutputStream(gzipOutputStream);

            objectOutputStream.writeObject(object);
            objectOutputStream.close();

            byte[] byteArray = arrayOutputStream.toByteArray();
            return android.util.Base64.encodeToString(byteArray, android.util.Base64.DEFAULT);
        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }
    }

    public static Object deserializeObjectFromString(String objectString) {
        try {
            byte[] byteArray = android.util.Base64.decode(objectString, android.util.Base64.DEFAULT);

            ByteArrayInputStream arrayInputStream = new ByteArrayInputStream(byteArray);
            GZIPInputStream gzipInputStream = new GZIPInputStream(arrayInputStream);
            ObjectInputStream objectInputStream = new ObjectInputStream(gzipInputStream);
            return objectInputStream.readObject();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static Map<String, Object> toMap(JSONObject json) throws JSONException {
        Map<String, Object> map = new HashMap<String, Object>();
        Iterator<String> keys = json.keys();
        while (keys.hasNext()) {
            String key = keys.next();
            Object value = json.get(key);
            if (value instanceof JSONArray) {
                value = toList((JSONArray) value);
            } else if (value instanceof JSONObject) {
                value = toMap((JSONObject) value);
            }
            map.put(key, value);
        }
        return map;
    }

    public static List<Object> toList(JSONArray array) throws JSONException {
        List<Object> list = new ArrayList<Object>();
        for (int i = 0; i < array.length(); i++) {
            Object value = array.get(i);
            if (value instanceof JSONArray) {
                value = toList((JSONArray) value);
            } else if (value instanceof JSONObject) {
                value = toMap((JSONObject) value);
            }
            list.add(value);
        }
        return list;
    }

    public static String getString(JSONObject obj, String name) throws JSONException {
        String strData = "";
        if (obj.has(name)) {
            strData = obj.getString(name);
        }

        if (strData.equals("") || strData.equalsIgnoreCase("null")) {
            strData = "";
        }
        return strData;
    }

    public static boolean getBoolean(JSONObject obj, String name) throws JSONException {
        boolean strData = false;
        if (obj.has(name)) {
            strData = obj.getBoolean(name);
        }
        return strData;
    }

    public static int getInteger(JSONObject obj, String name) throws JSONException {
        int strData = 0;
        if (obj.has(name)) {
            strData = obj.getInt(name);
        }
        return strData;
    }


    public static JSONArray listToJSONArray(List<Integer> list) {
        if (list == null || list.isEmpty()) {
            return new JSONArray();
        }
        JSONArray array = new JSONArray();
        for (Integer obj : list) {
            array.put(obj);
        }
        return array;
    }

    public static <T> List<T> jsonArrayToList(JSONArray array) {
        JSONArray jArray = (array == null ? new JSONArray() : array);
        List<T> list = new ArrayList<>();
        for (int i = 0; i < jArray.length(); i++) {
            try {
                list.add((T) jArray.get(i));
            } catch (JSONException ignored) {
            }
        }
        return list;
    }

    public static String mapToJson(Map<String, Object> map) {
        StringBuilder jsonBuilder = new StringBuilder();
        jsonBuilder.append("{");

        boolean firstEntry = true;
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            if (!firstEntry) {
                jsonBuilder.append(",");
            } else {
                firstEntry = false;
            }

            jsonBuilder.append("\"").append(entry.getKey()).append("\":");
            if (entry.getValue() instanceof String) {
                jsonBuilder.append("\"").append(entry.getValue()).append("\"");
            } else {
                jsonBuilder.append(entry.getValue());
            }
        }

        jsonBuilder.append("}");
        return jsonBuilder.toString();
    }

}