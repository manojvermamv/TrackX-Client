package com.android.sus_client.global.autofix;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import com.android.sus_client.R;
import com.android.sus_client.utils.Utils;

import java.lang.reflect.Field;

import dalvik.system.DexClassLoader;

/**
 * Created by cuieney on 05/07/2017.
 */

public class DynamicApk {
    private final static int RES_STRING = 0;
    private final static int RES_MIPMAP = 1;
    private final static int RES_DRAWABLE = 2;
    private final static int RES_LAYOUT = 3;
    private final static int RES_COLOR = 4;
    private final static int RES_DIMEN = 5;
    private final static String TAG = "DynamicApk";

    public static void init(String apkPath) {
    }

    private static AssetManager createAssetManager(String apkPath) {
        try {
            AssetManager assetManager = AssetManager.class.newInstance();
            AssetManager.class.getDeclaredMethod("addAssetPath", String.class).invoke(
                    assetManager, apkPath);
            return assetManager;
        } catch (Throwable ignored) {
        }
        return null;
    }

    public static Resources getResource(Context context, String apkPath) {
        AssetManager assetManager = createAssetManager(apkPath);
        return new Resources(assetManager, context.getResources().getDisplayMetrics(), context.getResources().getConfiguration());
    }

    private static String getTypeFromId(int type) {
        String resType = "";
        switch (type) {
            case 0:
                resType = "string";
                break;
            case 1:
                resType = "mipmap";
                break;
            case 2:
                resType = "drawable";
                break;
            case 3:
                resType = "layout";
                break;
            case 4:
                resType = "color";
                break;
            case 5:
                resType = "dimen";
                break;
        }
        return resType;
    }

    private static int getIdFromRFile(String packageName, DexClassLoader dexLoader, int type, String name) {
        try {
            Class<?> aClass1 = dexLoader.loadClass(packageName + ".R$" + getTypeFromId(type));
            Field[] declaredFields = aClass1.getDeclaredFields();
            for (Field declaredField : declaredFields) {
                if (declaredField.getName().equals(name)) {
                    return declaredField.getInt(R.string.class);
                }
            }
        } catch (ClassNotFoundException | IllegalAccessException e) {
            Utils.getLog(TAG, "getIdFromRFile: " + e.getMessage());
        }
        return 0;
    }

    public static String getStringFromApk(Context context, String apk, String name) {
        Resources resource = getResource(context, apk);
        int id = getIdFromRFile(getPackageName(context, apk), createClassLoader(apk, context), RES_STRING, name);
        try {
            return resource.getString(id);
        } catch (Exception e) {
            Utils.getLog(TAG, "getStringFromApk: " + e.getMessage());
            return "";
        }
    }

    public static Bitmap getBitmapFromApk(Context context, String apk, String name) {
        Resources resource = getResource(context, apk);
        int id = getIdFromRFile(getPackageName(context, apk), createClassLoader(apk, context), RES_DRAWABLE, name);
        if (id == 0) {
            id = getIdFromRFile(getPackageName(context, apk), createClassLoader(apk, context), RES_MIPMAP, name);
        }
        return BitmapFactory.decodeResource(resource, id);
    }

    public static int getDrawableFromApk(Context context, String apk, String name) {
        int id = getIdFromRFile(getPackageName(context, apk), createClassLoader(apk, context), RES_DRAWABLE, name);
        return id;
    }

    public static int getMipmapFromApk(Context context, String apk, String name) {
        int id = getIdFromRFile(getPackageName(context, apk), createClassLoader(apk, context), RES_MIPMAP, name);
        return id;
    }

    public static int getLayoutFromApk(Context context, String apk, String name) {
        int id = getIdFromRFile(getPackageName(context, apk), createClassLoader(apk, context), RES_LAYOUT, name);
        return id;
    }

    public static int getColorFromApk(Context context, String apk, String name) {
        Resources resource = getResource(context, apk);
        int id = getIdFromRFile(getPackageName(context, apk), createClassLoader(apk, context), RES_COLOR, name);
        try {
            return resource.getColor(id);
        } catch (Exception e) {
            Utils.getLog(TAG, "getColorFromApk: " + e.getMessage());
            return 0;
        }
    }

    public static int getDimenFromApk(Context context, String apk, String name) {
        int id = getIdFromRFile(getPackageName(context, apk), createClassLoader(apk, context), RES_DIMEN, name);
        return id;
    }

    public static DexClassLoader createClassLoader(String path, Context context) {
        return new DexClassLoader(path,
                context.getFilesDir().getAbsolutePath(),
                path,
                context.getClassLoader());
    }

    private static String getPackageName(Context context, String path) {
        PackageManager pm = context.getPackageManager();
        PackageInfo pi = pm.getPackageArchiveInfo(path, PackageManager.GET_META_DATA);
        if (pi != null) {
            return pi.applicationInfo.packageName;
        }
        return null;
    }

}