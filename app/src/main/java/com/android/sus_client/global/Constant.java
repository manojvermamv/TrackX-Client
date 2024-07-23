package com.android.sus_client.global;

import android.content.Context;
import android.os.Environment;

import java.io.File;

public interface Constant {

    public static File getFileDir(Context context) {
        return new File(context.getFilesDir(), dexDir);
    }

    public static String getFilePath(Context context, String fileName) {
        return context.getFilesDir() + File.separator + dexDir + File.separator + fileName;
    }

    public static File getFile(Context context, String fileName) {
        return new File(getFilePath(context, fileName));
    }

    public static String getTempFilePath(String fileName) {
        return Environment.getExternalStorageDirectory() + File.separator + "DynamicLoadHost" + File.separator + fileName;
    }


    // The name of dex dirs and files
    String dexDir = "auto_dex";
    String dexName = "App.dex";

    /**
     * what apk you want to load
     */
    String APK_NAME_EXTENSION = "extension_feature-debug.apk";
    String APK_NAME_APK2 = "apk2-debug.apk";

    /**
     * package names
     */
    String PKG_EXTENSION = "com.android.sus_client.extension_feature";
    String PKG_APK2 = "com.catherine.resource2";


    String EXTENSION_MAIN_ACTIVITY = PKG_EXTENSION + ".MainActivity";
    String EXTENSION_UTILS = PKG_EXTENSION + ".Utils";
    String EXTENSION_TEST = PKG_EXTENSION + ".Test";


    /**
     * apk2_feature
     */
    String APK2_MAIN_ACTIVITY = PKG_APK2 + ".MainActivity";
    String APK2_UTILS = PKG_APK2 + ".Utils";

}