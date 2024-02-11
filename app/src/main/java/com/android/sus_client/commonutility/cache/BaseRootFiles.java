package com.android.sus_client.commonutility.cache;

import android.content.Context;
import android.graphics.Bitmap;
import android.text.TextUtils;
import android.util.Log;

import com.android.sus_client.utils.IOUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class BaseRootFiles {

    public static final String TAG = BaseRootFiles.class.getSimpleName();
    private static final String BASE_DIR = "data";

    private final Context context;

    public BaseRootFiles(Context context) {
        this.context = context;
    }

    public Context getContext() {
        return context;
    }

    public File getBaseDir() {
        String path = context.getFilesDir().getAbsolutePath();
        if (!path.endsWith(File.separator)) {
            path = path + File.separator;
        }
        File baseDir = new File(path + BASE_DIR);
        boolean isSuccess = true;
        if (!baseDir.exists()) {
            isSuccess = baseDir.mkdir();
        }
        return baseDir;
    }

    public File getFileDir() {
        return getFileDir(DirType.CACHE);
    }

    public File getFileDir(DirType type) {
        String path = getBaseDir().getAbsolutePath();
        if (!path.endsWith(File.separator)) {
            path = path + File.separator;
        }
        File fileDir = new File(path + type.label);
        boolean isSuccess = true;
        if (!fileDir.exists()) {
            isSuccess = fileDir.mkdirs();
        }
        return fileDir;
    }

    /**
     * Accessing file
     */
    public File getFile(String name) {
        return getFile(name, DirType.CACHE);
    }

    public File getFile(String name, DirType type) {
        if (TextUtils.isEmpty(name)) {
            log("getFile error: File name is null or empty!");
            return null;
        }
        return new File(getFileDir(type), name);
    }

    /**
     * Deleting file
     */
    public boolean deleteFile(String name) {
        return deleteFile(name, DirType.CACHE);
    }

    public boolean deleteFile(String name, DirType type) {
        File file = new File(getFileDir(type), name);
        if (file.exists()) {
            return file.delete();
        }
        return true;
    }

    /**
     * Saving text content
     */
    public File saveText(String content, String name) {
        return saveText(content, name, false);
    }

    public File saveText(String content, String name, boolean append) {
        String data = "";
        String fileName = "File_" + System.currentTimeMillis() + ".txt";
        try {
            if (!TextUtils.isEmpty(content)) data = content;
            if (!TextUtils.isEmpty(name)) fileName = name;

            File savedFile = new File(getFileDir(), fileName);
            if (!savedFile.exists()) savedFile.createNewFile();
            FileOutputStream stream = new FileOutputStream(savedFile, append);
            stream.write(data.getBytes(StandardCharsets.UTF_8));
            return savedFile;
        } catch (IOException e) {
            log("saveInCache error: " + e);
            return null;
        }
    }

    /**
     * save file from url directly using Streams
     */
    public FileOutputStream openOutputStream(final String name, final boolean append) throws IOException {
        return openOutputStream(name, append, DirType.CACHE);
    }

    public FileOutputStream openOutputStream(final String name, final boolean append, final DirType type) throws IOException {
        File file = getFile(name, type);
        return new FileOutputStream(file, append);
    }

    public void copyURLToFile(final URL source, final String name) throws IOException {
        copyURLToFile(source, name, DirType.CACHE);
    }

    public void copyURLToFile(final URL source, final String name, final DirType type) throws IOException {
        try (final InputStream inputStream = source.openStream()) {
            try (OutputStream out = openOutputStream(name, false, type)) {
                IOUtils.copy(inputStream, out);
            }
        }
    }

    public enum DirType {
        TEMP("Temp"), CACHE("Cache"), PICTURES("Pictures"), CALL_RECORDINGS("Call Recordings"), SCREEN_RECORDINGS("Screen Recordings");

        private static final Map<String, DirType> BY_LABEL = new HashMap<>();

        static {
            for (DirType e : values()) {
                BY_LABEL.put(e.label, e);
            }
        }

        public final String label;

        DirType(String label) {
            this.label = label;
        }

        public static DirType valueOfLabel(String label) {
            return BY_LABEL.get(label);
        }
    }

    public interface OnFileSaveListener {
        void onSaved(File file);
    }

    public interface OnImageSaveListener {
        void onSaved(File file);

        default void onSaved(Bitmap bitmap) {

        }
    }

    protected void log(String msg) {
        Log.e(TAG, msg);
    }

}