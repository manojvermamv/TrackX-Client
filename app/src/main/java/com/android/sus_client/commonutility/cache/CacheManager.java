package com.android.sus_client.commonutility.cache;

import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;
import android.text.TextUtils;
import android.webkit.MimeTypeMap;

import java.io.File;
import java.io.FileOutputStream;

public class CacheManager {

    public static File saveFileForUrl(Context context, Bitmap bitmap, String url) {
        String extension = MimeTypeMap.getFileExtensionFromUrl(url);
        if (extension.isEmpty()) extension = ".png";
        if (!extension.startsWith(".")) {
            extension = "." + extension;
        }

        try {
            if (bitmap == null) throw new Exception("Bitmap is null");
            File cacheFile = getCacheFileForUrl(context, Uri.parse(url), extension);
            if (!cacheFile.exists()) cacheFile.createNewFile();
            FileOutputStream stream = new FileOutputStream(cacheFile);
            bitmap.compress(cacheFile.getName().toLowerCase().endsWith(".png") ? Bitmap.CompressFormat.PNG : Bitmap.CompressFormat.JPEG, 100, stream);
            stream.close();
            return cacheFile;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static File getCacheFileForUrl(Context context, Uri uri, String extension) {
        final String hash;
        if (uri != null && !TextUtils.isEmpty(uri.getHost())) {
            hash = String.valueOf(uri.getHost().hashCode());
        } else {
            hash = String.valueOf(System.currentTimeMillis());
        }
        return new File(context.getCacheDir(), hash + extension);
    }

}
