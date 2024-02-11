package com.android.sus_client.utils;

import android.util.Base64;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public class Compressor {

    public static String pack(Object object) {
        try {
            Object data;
            if (object instanceof JSONObject) {
                data = ((JSONObject) object).toString();
            } else if (object instanceof JSONArray) {
                data = ((JSONArray) object).toString();
            } else {
                data = object;
            }

            ByteArrayOutputStream arrayOutputStream = new ByteArrayOutputStream();
            GZIPOutputStream gzipOutputStream = new GZIPOutputStream(arrayOutputStream);
            ObjectOutputStream objectOutputStream = new ObjectOutputStream(gzipOutputStream);
            objectOutputStream.writeObject(data);
            objectOutputStream.close();
            byte[] byteArray = arrayOutputStream.toByteArray();
            return Base64.encodeToString(byteArray, Base64.DEFAULT);
        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }
    }

    public static String unpackAsString(String string) {
        return (String) unpack(string);
    }

    public static Object unpack(String string) {
        try {
            byte[] byteArray = Base64.decode(string, Base64.DEFAULT);
            ByteArrayInputStream arrayInputStream = new ByteArrayInputStream(byteArray);
            GZIPInputStream gzipInputStream = new GZIPInputStream(arrayInputStream);
            ObjectInputStream objectInputStream = new ObjectInputStream(gzipInputStream);
            return objectInputStream.readObject();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

}