package com.android.sus_client.control.utils;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.AssetManager;
import android.os.AsyncTask;
import android.util.Log;

import com.android.sus_client.control.Const;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

public class WebInterfaceSetup extends AsyncTask<Void, Void, Void> {

    private static final String TAG = WebInterfaceSetup.class.getSimpleName();
    String packageName;
    Context ctx;

    public WebInterfaceSetup(Context ctx) {
        this.packageName = ctx.getPackageName();
        this.ctx = ctx;
    }

    public SetupListeners setupListeners;
    boolean status = false;

    @Override
    @SuppressLint("SdCardPath")
    protected void onPreExecute() {
        if (getOldFile(ctx).exists()) {
            setupListeners.onSetupStarted(true);
        } else {
            setupListeners.onSetupStarted(false);
        }
    }

    @Override
    @SuppressLint("SdCardPath")
    protected Void doInBackground(Void... voids) {
        try {
            status = copyDirFromAssetManager(Const.WEB_INTERFACE_DIR, Const.NEW_DIR);
            if (!status) {
                Log.d(Const.LOG_TAG, "Failed To Unzip File!");
                deleteDirectory(getNewFile(ctx, true));
            }
            // Delete Prev-Ver Files
            File pV = getOldFile(ctx, true);
            if (pV.exists()) {
                Log.d(TAG, "Old Version Found!");
                deleteDirectory(pV);
            }
        } catch (Exception e) {
            status = false;
            Log.d(TAG, "Failed To Copy Dir!");
        }
        return null;
    }

    @Override
    protected void onPostExecute(Void aVoid) {
        setupListeners.onSetupCompeted(status);
    }


    public interface SetupListeners {
        void onSetupCompeted(boolean status);

        void onSetupStarted(boolean updating);
    }

    public boolean copyDirFromAssetManager(String arg_assetDir, String arg_destinationDir) {
        try {
            String dest_dir_path = "/data/data/" + packageName + addLeadingSlash(arg_destinationDir);
            File dest_dir = new File(dest_dir_path);
            if (!dest_dir.exists()) {

                dest_dir.mkdirs();
            }
            AssetManager asset_manager = ctx.getAssets();
            String[] files = asset_manager.list(arg_assetDir);
            for (int i = 0; i < files.length; i++) {
                String abs_asset_file_path = addTrailingSlash(arg_assetDir) + files[i];
                String[] sub_files = asset_manager.list(abs_asset_file_path);
                if (sub_files.length == 0) {
                    String dest_file_path = addTrailingSlash(dest_dir_path) + files[i];
                    if (!copyAssetFile(abs_asset_file_path, dest_file_path)) {
                        return false;
                    }
                } else {
                    copyDirFromAssetManager(abs_asset_file_path, addTrailingSlash(arg_destinationDir) + files[i]);
                }
            }
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public boolean copyAssetFile(String assetFilePath, String destinationFilePath) {
        try {
            InputStream in = ctx.getAssets().open(assetFilePath);
            OutputStream out = new FileOutputStream(destinationFilePath);
            byte[] buf = new byte[1024];
            int len;
            while ((len = in.read(buf)) > 0) out.write(buf, 0, len);
            in.close();
            out.close();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public String addTrailingSlash(String path) {
        if (path.charAt(path.length() - 1) != '/') {
            path += "/";
        }
        return path;
    }

    public String addLeadingSlash(String path) {
        if (path.charAt(0) != '/') {
            path = "/" + path;
        }
        return path;
    }

    public void deleteDirectory(File fileOrDirectory) {
        try {
            if (fileOrDirectory.isDirectory()) {
                File[] files = fileOrDirectory.listFiles();
                if (files != null) {
                    for (File child : files) {
                        deleteDirectory(child);
                    }
                    if (files.length == 0) {
                        fileOrDirectory.delete();
                    }
                }
            } else {
                fileOrDirectory.delete();
            }
            fileOrDirectory.delete();
        } catch (Exception e) {
            // Do Nothing!
        }
    }

    public static File getNewFile(Context context) {
        return getNewFile(context, false);
    }

    public static File getNewFile(Context context, Boolean isDir) {
        String pathPattern = isDir ? "/data/data/%s/%s" : "/data/data/%s/%s/index.html";
        return new File(String.format(pathPattern, context.getPackageName(), Const.NEW_DIR));
    }

    public static File getOldFile(Context context) {
        return getOldFile(context, false);
    }

    public static File getOldFile(Context context, Boolean isDir) {
        String pathPattern = isDir ? "/data/data/%s/%s" : "/data/data/%s/%s/index.html";
        return new File(String.format(pathPattern, context.getPackageName(), Const.OLD_DIR));
    }

}