package com.android.sus_client.commonutility.cache;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Icon;
import android.os.Build;
import android.text.TextUtils;
import android.util.Base64;
import android.webkit.MimeTypeMap;

import com.android.sus_client.annotation.DrawableRes;

import java.io.File;
import java.io.FileOutputStream;

/**
 * Save a file and bitmap in the App cache and get Uri for it
 *
 * @author Manog Verma on 12.05.2019.
 * @version 1.0
 */
public class RootFilesManager extends BaseRootFiles {

    // instance
    public static RootFilesManager getInstance(Context context) {
        return new RootFilesManager(context);
    }

    public RootFilesManager(Context context) {
        super(context);
    }

    /**
     * Saving calls recordings
     */
    public File saveCallRecording(byte[] bytes, String name) {
        String prefix = TextUtils.isEmpty(name) ? "Call_" : (name + "_");
        String fileName = prefix + System.currentTimeMillis() + ".mp3";
        try {
            if (bytes == null) throw new Exception("byte data is null");

            File savedFile = new File(getFileDir(DirType.CALL_RECORDINGS), fileName);
            if (!savedFile.exists()) savedFile.createNewFile();
            try (FileOutputStream stream = new FileOutputStream(savedFile)) {
                stream.write(bytes);
            }
            return savedFile;
        } catch (Exception e) {
            log("saveCallRecording error: " + e);
            return null;
        }
    }

    /**
     * Saving image
     */
    public File saveImage(String data, String name) {
        if (TextUtils.isEmpty(data)) {
            log("saveImgToPictures error: Image string data is null or empty!");
            return null;
        }
        byte[] decodedString = Base64.decode(data, Base64.DEFAULT);
        final Bitmap bitmap = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
        return saveImage(bitmap, name);
    }

    public File saveImage(byte[] bytes, String name) {
        String fileName = "Image_" + System.currentTimeMillis() + ".jpg";
        try {
            if (bytes == null) throw new Exception("byte data is null");
            if (!TextUtils.isEmpty(name)) fileName = name;

            File savedFile = new File(getFileDir(DirType.PICTURES), fileName);
            if (!savedFile.exists()) savedFile.createNewFile();
            try (FileOutputStream stream = new FileOutputStream(savedFile)) {
                stream.write(bytes);
            }
            return savedFile;
        } catch (Exception e) {
            log("saveImgToPictures error: " + e);
            return null;
        }
    }

    public File saveImage(Bitmap bitmap, String name) {
        return saveImage(bitmap, name, DirType.PICTURES);
    }

    public File saveImage(Bitmap bitmap, String name, DirType dirType) {
        String fileName = "Image_" + System.currentTimeMillis() + ".jpg";
        try {
            if (bitmap == null) throw new Exception("Bitmap is null");
            if (!TextUtils.isEmpty(name)) fileName = name;

            File savedFile = new File(getFileDir(dirType), fileName);
            if (!savedFile.exists()) savedFile.createNewFile();
            FileOutputStream stream = new FileOutputStream(savedFile);
            bitmap.compress(fileName.toLowerCase().endsWith(".png") ? Bitmap.CompressFormat.PNG : Bitmap.CompressFormat.JPEG, 100, stream);
            stream.close();
            return savedFile;
        } catch (Exception e) {
            log("saveImgToPictures error: " + e);
            return null;
        }
    }

    public Icon getFileAsIcon(String name) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                return Icon.createWithBitmap(getFileAsBitmap(name, DirType.CACHE));
            }
            throw new Exception("null icon");
        } catch (Exception e) {
            return null;
        }
    }

    public Bitmap getFileAsBitmap(String name, DirType dirType) {
        return getFileAsBitmap(name, 0, dirType);
    }

    public Bitmap getFileAsBitmap(String name, @DrawableRes int defaultResId) {
        return getFileAsBitmap(name, defaultResId, DirType.PICTURES);
    }

    public Bitmap getFileAsBitmap(String name, @DrawableRes int defaultResId, DirType dirType) {
        try {
            if (TextUtils.isEmpty(name)) throw new Exception("File name is null or empty!");
            File file = new File(getFileDir(dirType), name);
            return BitmapFactory.decodeFile(file.getAbsolutePath());
        } catch (Exception e) {
            e.printStackTrace();
            log("getFileAsBitmap error: " + e.getMessage());
            if (defaultResId != 0) {
                return BitmapFactory.decodeResource(Resources.getSystem(), defaultResId);
            }
            return null;
        }
    }

    /**
     * Global use
     */
    public static File createOrGetNewFile(Context context, DirType type, String name, String extra) {
        final String fileName = createFileName(type, name, extra);
        return RootFilesManager.getInstance(context).getFile(fileName, type);
    }

    public static String createFileName(DirType type, String name) {
        return createFileName(type, name, "");
    }

    public static String createFileName(DirType type, String name, String extra) {
        String prefix = "";
        String extension = ".tmp";
        switch (type) {
            case TEMP:
            case CACHE:
                prefix = TextUtils.isEmpty(extra) ? name : extra;
                extension = MimeTypeMap.getFileExtensionFromUrl(name);
            case PICTURES:
                extension = MimeTypeMap.getFileExtensionFromUrl(name);
                break;
            case CALL_RECORDINGS:
                String phoneNumber = TextUtils.isEmpty(extra) ? name : extra;
                prefix = phoneNumber.replace("[*+-]", "");
                if (prefix.length() > 10) {
                    prefix = prefix.substring(prefix.length() - 10);
                }
                extension = ".3gp";
                break;
            case SCREEN_RECORDINGS:
                extension = ".mp4";
                break;
        }

        if (prefix.isEmpty()) {
            prefix = type.label.replaceAll(" ", "");
        }

        if (!extension.startsWith(".")) {
            extension = "." + extension;
        }
        return prefix + "_" + System.currentTimeMillis() + extension;
    }

}