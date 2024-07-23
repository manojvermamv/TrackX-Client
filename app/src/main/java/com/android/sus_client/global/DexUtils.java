package com.android.sus_client.global;

import android.content.Context;
import android.content.ContextWrapper;
import android.util.Log;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import dalvik.system.DexClassLoader;

public class DexUtils {

    private final static String TAG = "DexUtils";

    protected final File mDexDir;


    public DexUtils(ContextWrapper contextWrapper) {
        mDexDir = new File(contextWrapper.getFilesDir(), "dex");
    }

    public DexUtils(String dexCacheDir) {
        mDexDir = new File(dexCacheDir);
    }

    public static Class<?>[] toParameterTypes(Class<?>... parameterTypes) {
        return parameterTypes;
    }

    public static Object[] toArgs(Object... args) {
        return args;
    }


    // Run method "main(String[])" in fullClassName
    // found in the dexed jar file, libraryFileName,
    // with no command line arguments
    public void runMain(String libraryFileName,
                        String fullClassName,
                        String... args) {
        final Class<?>[] parameterTypes = toParameterTypes(String[].class);
        //final Object[] args = toArgs(new String[0]);
        runMethod(libraryFileName, fullClassName, "main", parameterTypes, args);
    }

    // Find methodName(parameterTypes) in fullClassName inside
    // the dexed jar file, libraryFileName and run it with args
    // - parameterTypes and args can be null
    // - use the toParameterTypes() and toArgs() methods for convenience
    public Object runMethod(String libraryFileName,
                            String fullClassName,
                            String methodName,
                            Class<?>[] parameterTypes,
                            Object[] args) throws IllegalArgumentException {
        try {
            @SuppressWarnings("unchecked") final Class<Object> classToLoad = (Class<Object>) getClass(libraryFileName, fullClassName);
            final Object instance = classToLoad.newInstance();
            final Method main = classToLoad.getMethod(methodName, parameterTypes);
            //final String[] args = new String[0];
            //main.invoke(instance, (Object) args);
            return main.invoke(instance, args);
        } catch (InstantiationException | IllegalAccessException |
                 NoSuchMethodException | InvocationTargetException e) {
            throw new IllegalArgumentException(e);
        }
    }

    // Find methodName(parameterTypes) in fullClassName inside
    // the dexed jar file, libraryFileName and run it with args
    // - parameterTypes and args can be null
    // - use the toParameterTypes() and toArgs() methods for convenience
    public Object newInstance(String libraryFileName,
                              String fullClassName) throws IllegalArgumentException {
        try {
            @SuppressWarnings("unchecked") final Class<Object> classToLoad = (Class<Object>) getClass(libraryFileName, fullClassName);
            return classToLoad.newInstance();
        } catch (InstantiationException | IllegalAccessException e) {
            throw new IllegalArgumentException(e);
        }
    }

    public final Class<?> getClass(String libraryFileName,
                                   String fullClassName) throws IllegalArgumentException {
        try {
            deleteCachedDexFiles(libraryFileName);
            Log.i("DLL", "exists? " + new File(libraryFileName).exists());
            final DexClassLoader classLoader = new DexClassLoader(
                    libraryFileName, mDexDir.getAbsolutePath(), null,
                    this.getClass().getClassLoader());
            return classLoader.loadClass(fullClassName);
        } catch (ClassNotFoundException e) {
            throw new IllegalArgumentException(e);
        }
    }

    // Note: About re-loading a jar file in the same process' lifetime
    //
    // If we try to load the same jar file by name,
    // but not by content (different file, same name)
    // as previously loaded in this current process,
    // this process will crash with a SIGBUS error.
    // While setIntentRedelivery(true) above *will*
    // cause Android to restart us and re-send the
    // current Intent, this is not very graceful.
    // Instead, it seems that if you clear out the
    // dex cache, all is good.
    //
    // TODO: Only delete the cache file for the library we're loading
    void deleteCachedDexFiles(String libraryFileName) {
        String libName = new File(libraryFileName).getName();
        if (libName.endsWith(".apk") || libName.endsWith(".jar"))
            libName = libName.substring(0, libName.length() - 4);
        Log.i("DexUtils", "deleteCacheDexFiles(" + libName + ") from " + mDexDir.getAbsolutePath());
        for (File f : mDexDir.listFiles()) {
            Log.i("DLL", "Dex File: " + f.getName());
            // only delete the dex file matching the library file name?
            if (f.isFile() && f.getName()
                    .endsWith(".dex")) {
                f.delete();
            }
        }
    }


    /**
     * Description:获取动态加载的dex包的sdcard路径
     * created by tanzhenxing(谭振兴)
     * created data 17-2-20 下午3:51
     */

    public static String getOptimizedDirectory(Context context) {
        //return context.getDir("app_dex", 0).getAbsolutePath();
        return new File(context.getFilesDir(), "app_dex").getAbsolutePath();
    }


    /**
     * Description:获取制定parent-classloader的DexClassLoader对象
     * created by tanzhenxing(谭振兴)
     * created data 17-2-20 下午3:53
     */
    public static DexClassLoader getDexClassLoader(Context context, ClassLoader loader, String dexFilePath) {
        return new DexClassLoader(dexFilePath,
                getOptimizedDirectory(context), null, loader);
    }

    public static DexClassLoader getDexClassLoader(Context context, String dexFilePath) {
        return getDexClassLoader(context, context.getClassLoader(), dexFilePath);
    }

    public static Class<?> loadDex(Context context, String dexPath, String fullClassName) {
        DexClassLoader classloader = new DexClassLoader(
                dexPath, getOptimizedDirectory(context), null, context.getClass().getClassLoader());
        Class<?> myclass = null;
        try {
            myclass = classloader.loadClass(fullClassName);
        } catch (ClassNotFoundException e) {
            Log.e(TAG, "load=error -> " + e.getMessage());
        }
        return myclass;
    }


    /**
     * Description:  将assets目录下面的dex_file文件写入dexInternalStoragePath文件中
     *
     * @param context                上下问环境
     * @param dexInternalStoragePath 存储在磁盘上的dex文件
     * @param dex_file               assets目录下的dex文件名称
     */
    public static boolean prepareDex(Context context, File dexInternalStoragePath, String dex_file) {
        final int BUF_SIZE = 2048;
        BufferedInputStream bis = null;
        OutputStream dexWriter = null;
        try {
            bis = new BufferedInputStream(context.getAssets().open(dex_file));
            dexWriter = new BufferedOutputStream(new FileOutputStream(dexInternalStoragePath));
            byte[] buf = new byte[BUF_SIZE];
            int len;
            while ((len = bis.read(buf, 0, BUF_SIZE)) > 0) {
                dexWriter.write(buf, 0, len);
            }
            dexWriter.close();
            bis.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return true;
    }

    /**
     * Description:合并两个Array，按照first-second顺序
     * created by tanzhenxing(谭振兴)
     * created data 17-2-20 下午3:54
     */
    public static Object combineArray(Object firstArray, Object secondArray) {
        Class<?> localClass = firstArray.getClass().getComponentType();
        int firstArrayLength = Array.getLength(firstArray);
        int allLength = firstArrayLength + Array.getLength(secondArray);
        Object result = Array.newInstance(localClass, allLength);
        for (int k = 0; k < allLength; ++k) {
            if (k < firstArrayLength) {
                Array.set(result, k, Array.get(firstArray, k));
            } else {
                Array.set(result, k, Array.get(secondArray, k - firstArrayLength));
            }
        }
        return result;
    }

}