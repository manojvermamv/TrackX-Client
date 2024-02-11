package com.android.sus_client.utils;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.LightingColorFilter;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.media.ExifInterface;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.webkit.MimeTypeMap;

import com.android.sus_client.annotation.Nullable;
import com.android.sus_client.annotation.RequiresApi;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLDecoder;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public class FileUtil {

    private Context context;
    public static String TAG = FileUtil.class.getSimpleName();

    private FileUtil(Context context) {
        this.context = context;
    }

    public static FileUtil getInstance(Context context) {
        return new FileUtil(context);
    }

    public static String getFolderName(Context context, int type) {
        if (type == 0) {
            return "/" + ApkUtils.getApplicationName(context) + "/";
        } else {
            String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
            return "/" + ApkUtils.getApplicationName(context) + "/" + "photo_" + timeStamp + ".jpg";
        }
    }

    public static String getFileName(String ext) {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        if (TextUtils.isEmpty(ext)) {
            return "File_" + timeStamp;
        } else {
            return "File_" + timeStamp + "." + ext;
        }
    }

    public static String getAppDir(Context context) {
        return ApkUtils.getApplicationName(context) + "/";
    }

    public static File getOutputMediaFile(Context context) {
        // Create a media folder
        File createDir = new File(Environment.getExternalStorageDirectory(), getFolderName(context, 0));
        if (!createDir.exists()) {
            if (!createDir.mkdirs()) return null;
        }

        // Create a media file name
        File photoFile = new File(Environment.getExternalStorageDirectory(), getFolderName(context, 1));

        return new File(photoFile.getAbsolutePath());
    }

    public static Intent getIntentForFile(Context mContext, File file) {
        if (file == null) return null;
        //if (!file.exists()) return null;

        //String extension = android.webkit.MimeTypeMap.getFileExtensionFromUrl(Uri.fromFile(file).toString());
        //String mimeType = android.webkit.MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension);

        String mimeType = getMimeType(file.getName());
        Uri uri;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            //uri = FileProvider.getUriForFile(mContext, MyApp.getFileProvider(), file);
            uri = null;
        } else {
            uri = Uri.fromFile(file);
        }

        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_VIEW);
        intent.putExtra(Intent.EXTRA_STREAM, uri);
        intent.setDataAndType(uri, mimeType);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_GRANT_READ_URI_PERMISSION);
        return intent;
    }


    public static Intent getIntentForFile(Context mContext, Uri uri) {
        if (uri == null) return null;
        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_VIEW);
        intent.putExtra(Intent.EXTRA_STREAM, uri);
        intent.setDataAndType(uri, getMimeType(mContext, uri));
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_GRANT_READ_URI_PERMISSION);
        return intent;
    }

    public static Uri getUriFromAsset(Context context, String assetFileName) {
        InputStream inputStream = null;
        FileOutputStream outputStream = null;
        File tempFile = null;
        Uri uri = null;
        try {
            inputStream = context.getAssets().open(assetFileName);
            tempFile = File.createTempFile("temp_asset", assetFileName, context.getCacheDir());
            outputStream = new FileOutputStream(tempFile);
            IOUtils.copy(inputStream, outputStream);
            uri = Uri.fromFile(tempFile);
        } catch (IOException ignored) {
        } finally {
            try {
                if (inputStream != null) inputStream.close();
                if (outputStream != null) outputStream.close();
                if (tempFile != null) tempFile.deleteOnExit();
            } catch (Exception ignored) {
            }
        }
        return uri;
    }

    public static long getDurationFile(String path) {
        try (final MediaMetadataRetriever retriever = new MediaMetadataRetriever()) {
            retriever.setDataSource(path);
            long duration = Long.parseLong(retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION));
            //String seconds = String.valueOf((duration % 60000 / 1000));
            //String minutes = String.valueOf((duration / 60000));
            retriever.release();
            return duration;
            //return seconds.length() == 1 ? (minutes + ":0" + seconds) : (minutes + ":" + seconds);
        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }
    }

    /**
     * @param fileName fileName must have with extension
     * @return MimeType of the file
     */
    public static String getMimeType(String fileName) {
        //String[] parts = filePath.split("\\.");
        //String extension = parts[parts.length-1];
        String extension = MimeTypeMap.getFileExtensionFromUrl(fileName);
        String type = null;
        if (extension != null) {
            MimeTypeMap mimeType = MimeTypeMap.getSingleton();
            type = mimeType.getMimeTypeFromExtension(extension);
        }

        if (type == null) {
            type = "*/*";
        }
        return type;
    }

    public static String getMimeType(Context context, Uri uri) {
        String mimeType = null;
        if (ContentResolver.SCHEME_CONTENT.equals(uri.getScheme())) {
            ContentResolver cr = context.getContentResolver();
            mimeType = cr.getType(uri);
        } else {
            String fileExtension = MimeTypeMap.getFileExtensionFromUrl(uri.toString());
            mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(fileExtension.toLowerCase());
        }
        return mimeType;
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    public static String getFileSizeNIO(String fileName) {
        Path path = Paths.get(fileName);
        try {
            // size of a file (in bytes)
            long bytes = Files.size(path);
            System.out.println(String.format("%,d bytes", bytes));
            System.out.println(String.format("%,d kilobytes", bytes / 1024));
            return "" + bytes / 1024;
        } catch (IOException e) {
            e.printStackTrace();
            return "0";
        }
    }

    public static boolean isExternalStorageWritable() {
        return Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState());
    }

    public static boolean isExternalStorageReadable() {
        String externalStorageState = Environment.getExternalStorageState();
        return Environment.MEDIA_MOUNTED.equals(externalStorageState) || Environment.MEDIA_MOUNTED_READ_ONLY.equals(externalStorageState);
    }

    public static String getSizeForFile(Context context, File file) {
        return getSizeForFile(context, file.length());
    }

    public static String getSizeForFile(Context context, long length) {
        if (length < 1000) {
            return String.format("%d B", new Object[]{Long.valueOf(length)});
        } else if (length < 1000000) {
            return String.format("%.2f KB", new Object[]{Float.valueOf(((float) length) / 1000.0f)});
        } else {
            return String.format("%.2f MB", new Object[]{Float.valueOf(((float) length) / 1000000.0f)});
        }
    }

    public static int getConvertedSize(long j) {
        if (j < 1000) {
            return (int) j;
        }
        if (j < 1000000) {
            j /= 1000;
        } else {
            j /= 1000000;
        }
        return (int) j;
    }

    public static String getDateForFile(File file) {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("dd/MM/yyyy, hh:mm:ss", Locale.getDefault());
        Calendar instance = Calendar.getInstance();
        instance.setTimeInMillis(file.lastModified());
        return simpleDateFormat.format(instance.getTime());
    }

    public static String getFileExtension(String fileName) {
        char ch;
        int len;
        if (fileName == null || (len = fileName.length()) == 0 || (ch = fileName.charAt(len - 1)) == '/' || ch == '\\' || //in the case of a directory
                ch == '.') //in the case of . or ..
            return "";
        int dotInd = fileName.lastIndexOf('.'), sepInd = Math.max(fileName.lastIndexOf('/'), fileName.lastIndexOf('\\'));
        if (dotInd <= sepInd) return "";
        else return fileName.substring(dotInd + 1).toLowerCase();
    }

    public static String getNameNoExtension(File file) {
        return getNameNoExtension(file.getName());
    }

    public static String getNameNoExtension(String name) {
        String extensionForFileName = getFileExtension(name);
        if (extensionForFileName == null || extensionForFileName.isEmpty() || extensionForFileName.length() >= name.length()) {
            return name;
        }
        int lastIndexOf = name.lastIndexOf(extensionForFileName);
        return lastIndexOf > 0 ? name.substring(0, lastIndexOf - 1) : name;
    }

    public static String getNameNoExtensionFromUrl(String url) {
        return getNameNoExtensionFromUrl(url, "");
    }

    public static String getNameNoExtensionFromUrl(String url, String extension) {
        String fileName = "";
        try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                fileName = Paths.get(new URI(url).getPath()).getFileName().toString();
            } else {
                throw new Exception();
            }
        } catch (Exception e) {
            fileName = url.substring(url.lastIndexOf('/') + 1);
        }
        try {
            String fileNameNoExt = fileName.substring(0, fileName.lastIndexOf('.')).replace(" ", "");
            return extension.isEmpty() ? fileNameNoExt : (fileNameNoExt + "." + extension);
        } catch (Exception ignored) {
            return "";
        }
    }

    public static boolean isFileLessThan(File file, int MB) {
        int maxFileSize = 1024 * 1024 * MB;
        Long l = file.length();
        String fileSize = l.toString();
        int finalFileSize = Integer.parseInt(fileSize);
        return finalFileSize >= maxFileSize;
    }

    public static int getMaxBufferSize() {
        Runtime runtime = Runtime.getRuntime();
        int maxMemory = (int) (runtime.maxMemory() - runtime.totalMemory());
        if (maxMemory < 50000000) {
            return maxMemory / 10;
        }
        return 50000000;
    }

    public static String getFolderSizeLabel(File file) {
        // Get size & convert bytes into KB. (here both are usable 1024.0 or 1000.0)
        double size = (double) getFolderSize(file) / 1000.0;
        if (size >= 1024) {
            return (size / 1024) + " MB";
        } else {
            return size + " KB";
        }
    }

    public static long getFolderSize(File file) {
        long size = 0;
        if (file.isDirectory()) {
            for (File child : file.listFiles()) {
                size += getFolderSize(child);
            }
        } else {
            size = file.length();
        }
        return size;
    }

    public static long getFilesSize(File[] filesList) {
        long size = 0;
        if (filesList.length != 0) {
            for (File f : filesList) {
                size += getFolderSize(f);
            }
        }
        return size;
    }

    public static boolean equalsDir(File file, File file2) {
        File[] files = file.listFiles();
        File[] files2 = file2.listFiles();
        if (files != null && files2 != null) {
            if (file.hashCode() == file2.hashCode()) {
                return true;
            }
            if (files.length != files2.length) {
                return false;
            }
            for (int i = 0; i < files.length; i++) {
                if (files[i].getName().equals(files2[i].getName())) {
                    return true;
                }
            }
        }
        return false;
    }

    public static boolean isInternalStorage(String str) {
        return str.contains(Environment.getExternalStorageDirectory().getAbsolutePath());
    }

    public static boolean isValidDir(File file) {
        //return file.isDirectory() && file.canRead() && file.listFiles() != null && file.listFiles().length > 0;
        return file.isDirectory() && file.listFiles() != null && file.listFiles().length > 0;
    }

    public static boolean directoryContainsFile(File fileDir, File file) {
        for (File f : fileDir.listFiles()) {
            if (equalsDir(f, file)) {
                return true;
            }
        }
        return false;
    }

    public static ArrayList<File> sortFoldersFirst(List<File> list) {
        ArrayList<File> arrayList = new ArrayList<>();
        Collection<File> arrayList2 = new ArrayList<>();
        Collection<File> arrayList3 = new ArrayList<>();
        for (File file : list) {
            if (file.isFile()) {
                arrayList3.add(file);
            } else {
                arrayList2.add(file);
            }
        }
        arrayList.addAll(arrayList2);
        arrayList.addAll(arrayList3);
        return arrayList;
    }

    public static boolean isDocumentFile(String name) {
        return name.endsWith(".pdf") || name.endsWith(".doc") || name.endsWith(".docx") || name.endsWith(".txt") || name.endsWith(".text") || name.endsWith(".rtf") || name.endsWith(".log");
    }

    public static boolean isAudioFile(String name) {
        return name.endsWith(".mp3") || name.endsWith(".aac") || name.endsWith(".wma") || name.endsWith(".flac") || name.endsWith(".alac") || name.endsWith(".wav") || name.endsWith(".aiff") || name.endsWith(".eac3") || name.endsWith(".ac3") || name.endsWith(".pcm") || name.endsWith(".he-aac") || name.endsWith(".m4a");
    }

    public static boolean isVideoFile(String name) {
        return name.endsWith(".mp4") || name.endsWith(".mkv") || name.endsWith(".m4v") || name.endsWith(".f4a") || name.endsWith(".m4b") || name.endsWith(".m4r") || name.endsWith(".f4b") || name.endsWith(".mov") || name.endsWith(".3gp") || name.endsWith(".3gp2") || name.endsWith(".3g2") || name.endsWith(".3gpp") || name.endsWith(".3gpp2") || name.endsWith(".ogg") || name.endsWith(".oga") || name.endsWith(".ogv") || name.endsWith(".ogx") || name.endsWith(".wmv") || name.endsWith(".asf") || name.endsWith(".webm") || name.endsWith(".flv") || name.endsWith(".avi") || name.endsWith(".quickTime") || name.endsWith(".hdv") || name.endsWith(".mxf") || name.endsWith(".mpeg-ts") || name.endsWith(".mpeg-2 ps");
    }

    public static boolean isImageFile(String name) {
        return name.endsWith(".tiff") || name.endsWith(".jpeg") || name.endsWith(".gif") || name.endsWith(".png") || name.endsWith(".bmp") || name.endsWith(".jpg");
    }

    public static boolean isApksFile(String name) {
        String nname = name.toLowerCase();
        return nname.endsWith(".apk") || nname.endsWith(".xapk");
    }

    public static boolean isArchiveFile(String name) {
        return name.endsWith(".zip") || name.endsWith(".7z") || name.endsWith(".rar") || name.endsWith(".tar");
    }


    public static File createNewPictureFile(Context context) {
        SimpleDateFormat date = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault());
        String fileName = date.format(new Date()) + ".jpg";
        return new File(context.getExternalFilesDir(Environment.DIRECTORY_DCIM).getAbsolutePath() + File.separator + fileName);
    }

    public static void createNewFile(String path) {
        int lastSep = path.lastIndexOf(File.separator);
        if (lastSep > 0) {
            String dirPath = path.substring(0, lastSep);
            makeDir(dirPath);
        }

        File file = new File(path);
        try {
            if (!file.exists()) file.createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static boolean renameFile(File dir, String fromName, String toName) {
        boolean isRenamed = false;
        if (dir != null && dir.exists()) {
            File from = new File(dir, fromName);
            File to = new File(dir, toName);
            if (from.exists()) {
                isRenamed = from.renameTo(to);
            }
        }
        return isRenamed;
    }

    public static String readFile(String path) {
        createNewFile(path);

        StringBuilder sb = new StringBuilder();
        FileReader fr = null;
        try {
            fr = new FileReader(new File(path));

            char[] buff = new char[1024];
            int length = 0;

            while ((length = fr.read(buff)) > 0) {
                sb.append(new String(buff, 0, length));
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (fr != null) {
                try {
                    fr.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        return sb.toString();
    }

    public static void writeTextInFile(String filePath, String text, boolean append) {
        File file = new File(filePath);
        if (!append) {
            if (file.exists()) {
                file.getAbsoluteFile().delete();
            }
        }
        createNewFile(filePath);
        FileWriter fileWriter = null;

        try {
            fileWriter = new FileWriter(file.getAbsoluteFile(), append);
            fileWriter.write(text);
            fileWriter.flush();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (fileWriter != null) fileWriter.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static void storeTextInFile(Context context, String text) {
        storeTextInFile(context, text, getFileName("txt"));
    }

    public static void storeTextInFile(Context context, String text, String fileName) {
        try {
            File file = new File(context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), fileName);
            FileWriter out = new FileWriter(file);
            out.write(text);
            out.close();
        } catch (Exception e) {
            Log.d(TAG, e.toString());
        }
    }

    public static void copyFileOrDir(File srcFileOrDir, File destFileOrDir) throws IOException {
        File newFile = new File(destFileOrDir, srcFileOrDir.getName());
        try (FileChannel outputChannel = new FileOutputStream(newFile).getChannel(); FileChannel inputChannel = new FileInputStream(srcFileOrDir).getChannel()) {
            inputChannel.transferTo(0, inputChannel.size(), outputChannel);
        }
    }

    public static void moveFileOrDir(File srcFileOrDir, File destFileOrDir) throws IOException {
        File newFile = new File(destFileOrDir, srcFileOrDir.getName());

        try (FileChannel outputChannel = new FileOutputStream(newFile).getChannel(); FileChannel inputChannel = new FileInputStream(srcFileOrDir).getChannel()) {
            inputChannel.transferTo(0, inputChannel.size(), outputChannel);
            inputChannel.close();
            deleteFileOrDir(srcFileOrDir);
        }
    }

    public static void deleteFileOrDir(File fileOrDir) {
        if (!fileOrDir.exists()) return;
        if (fileOrDir.isDirectory()) {
            for (File child : Objects.requireNonNull(fileOrDir.listFiles()))
                deleteFileOrDir(child);
        } else {
            fileOrDir.delete();
        }
    }

    public static void copyFile(String sourcePath, String destPath) {
        if (!isExistFile(sourcePath)) return;
        createNewFile(destPath);

        FileInputStream fis = null;
        FileOutputStream fos = null;

        try {
            fis = new FileInputStream(sourcePath);
            fos = new FileOutputStream(destPath, false);

            byte[] buff = new byte[1024];
            int length = 0;

            while ((length = fis.read(buff)) > 0) {
                fos.write(buff, 0, length);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (fis != null) {
                try {
                    fis.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (fos != null) {
                try {
                    fos.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public static void moveFile(String sourcePath, String destPath) {
        copyFile(sourcePath, destPath);
        deleteFile(sourcePath);
    }

    public static void deleteFile(String path) {
        File file = new File(path);
        deleteFile(file.getAbsoluteFile());
    }

    public static void deleteFile(File file) {
        if (!file.exists()) return;

        if (file.isFile()) {
            file.delete();
            return;
        }

        File[] fileArr = file.listFiles();

        if (fileArr != null) {
            for (File subFile : fileArr) {
                if (subFile.isDirectory()) {
                    deleteFile(subFile.getAbsolutePath());
                }

                if (subFile.isFile()) {
                    subFile.delete();
                }
            }
        }

        file.delete();
    }

    public static boolean isExistFile(String path) {
        File file = new File(path);
        return file.exists();
    }

    public static void makeDir(String path) {
        if (!isExistFile(path)) {
            File file = new File(path);
            file.mkdirs();
        }
    }

    public static void listDir(String path, ArrayList<String> list) {
        File dir = new File(path);
        if (!dir.exists() || dir.isFile()) return;

        File[] listFiles = dir.listFiles();
        if (listFiles == null || listFiles.length <= 0) return;

        if (list == null) return;
        list.clear();
        for (File file : listFiles) {
            list.add(file.getAbsolutePath());
        }
    }

    public static boolean isDirectory(String path) {
        if (!isExistFile(path)) return false;
        return new File(path).isDirectory();
    }

    public static boolean isFile(String path) {
        if (!isExistFile(path)) return false;
        return new File(path).isFile();
    }

    public static long getFileLength(String path) {
        if (!isExistFile(path)) return 0;
        return new File(path).length();
    }

    public static String getExternalStorageDir() {
        return Environment.getExternalStorageDirectory().getAbsolutePath();
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public static String getRemovableStorageDir(Context context) {
        try {
            String externalStorageDir = null;
            File[] externalCacheDirs = context.getExternalCacheDirs();
            if (externalCacheDirs != null) {
                for (File file : externalCacheDirs) {
                    if (Environment.isExternalStorageRemovable(file)) {
                        externalStorageDir = file.getPath().split("/Android")[0];
                        break;
                    }
                }
                return externalStorageDir;
            } else {
                return null;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static String getPackageDataDir(Context context) {
        return context.getExternalFilesDir(null).getAbsolutePath();
    }

    public static String getPublicDir(String type) {
        return Environment.getExternalStoragePublicDirectory(type).getAbsolutePath();
    }

    public static String getParentDirPath(String fileOrDirPath) {
        try {
            boolean endsWithSlash = fileOrDirPath.endsWith(File.separator);
            return fileOrDirPath.substring(0, fileOrDirPath.lastIndexOf(File.separatorChar, endsWithSlash ? fileOrDirPath.length() - 2 : fileOrDirPath.length() - 1));
        } catch (Exception ignored) {
            return fileOrDirPath;
        }
    }

    public static String convertUriToFilePath(final Context context, final Uri uri) {
        String path = null;
        if (DocumentsContract.isDocumentUri(context, uri)) {
            if (isExternalStorageDocument(uri)) {
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                final String type = split[0];

                if ("primary".equalsIgnoreCase(type)) {
                    path = Environment.getExternalStorageDirectory() + "/" + split[1];
                }
            } else if (isDownloadsDocument(uri)) {
                final String id = DocumentsContract.getDocumentId(uri);
                if (!TextUtils.isEmpty(id)) {
                    if (id.startsWith("raw:")) {
                        return id.replaceFirst("raw:", "");
                    }
                }

                final Uri contentUri = ContentUris.withAppendedId(Uri.parse("content://downloads/public_downloads"), Long.valueOf(id));

                path = getDataColumn(context, contentUri, null, null);
            } else if (isMediaDocument(uri)) {
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                final String type = split[0];

                Uri contentUri = null;
                if ("image".equals(type)) {
                    contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
                } else if ("video".equals(type)) {
                    contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
                } else if ("audio".equals(type)) {
                    contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
                }

                final String selection = MediaStore.Audio.Media._ID + "=?";
                final String[] selectionArgs = new String[]{split[1]};

                path = getDataColumn(context, contentUri, selection, selectionArgs);
            }
        } else if (ContentResolver.SCHEME_CONTENT.equalsIgnoreCase(uri.getScheme())) {
            if (isGooglePhotosUri(uri)) {
                return uri.getLastPathSegment();
            }

            path = getDataColumn(context, uri, null, null);
        } else if (ContentResolver.SCHEME_FILE.equalsIgnoreCase(uri.getScheme())) {
            path = uri.getPath();
        }

        if (path != null) {
            try {
                return URLDecoder.decode(path, "UTF-8");
            } catch (Exception e) {
                return null;
            }
        }
        return null;
    }

    private static String getDataColumn(Context context, Uri uri, String selection, String[] selectionArgs) {
        Cursor cursor = null;

        final String column = MediaStore.Images.Media.DATA;
        final String[] projection = {column};

        try {
            cursor = context.getContentResolver().query(uri, projection, selection, selectionArgs, null);
            if (cursor != null && cursor.moveToFirst()) {
                final int column_index = cursor.getColumnIndexOrThrow(column);
                return cursor.getString(column_index);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return null;
    }

    private static boolean isExternalStorageDocument(Uri uri) {
        return "com.android.externalstorage.documents".equals(uri.getAuthority());
    }

    private static boolean isDownloadsDocument(Uri uri) {
        return "com.android.providers.downloads.documents".equals(uri.getAuthority());
    }

    private static boolean isMediaDocument(Uri uri) {
        return "com.android.providers.media.documents".equals(uri.getAuthority());
    }

    public static boolean isGooglePhotosUri(Uri uri) {
        return "com.google.android.apps.photos.content".equals(uri.getAuthority());
    }

    public static String encodeImage(File imageFile, int quality) {
        try {
            FileInputStream fis = new FileInputStream(imageFile);
            return encodeImage(BitmapFactory.decodeStream(fis), quality);
        } catch (Exception e) {
            return "";
        }
    }

    public static String encodeImage(Bitmap bitmap, int quality) {
        ByteArrayOutputStream stream = null;
        String result = "";
        try {
            stream = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, quality, stream);
            result = encodeImage(stream);
        } catch (Exception ignored) {
        } finally {
            try {
                if (stream != null) stream.close();
            } catch (Exception ignored) {
            }
        }
        return result;
    }

    public static String encodeImage(ByteArrayOutputStream stream) {
        try {
            byte[] byteArray = stream.toByteArray();
            Log.e("Image Size", " ----------------> " + formatFileSize(byteArray.length));
            return Base64.encodeToString(byteArray, Base64.DEFAULT);
        } catch (Exception e) {
            return "";
        }
    }

    public static Bitmap decodeImage(String imgData) {
        byte[] decodedString = Base64.decode(imgData, Base64.DEFAULT);
        return BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
    }

    public static String formatFileSize(long size) {
        String hrSize = null;

        double b = size;
        double k = size / 1024.0;
        double m = ((size / 1024.0) / 1024.0);
        double g = (((size / 1024.0) / 1024.0) / 1024.0);
        double t = ((((size / 1024.0) / 1024.0) / 1024.0) / 1024.0);

        DecimalFormat dec = new DecimalFormat("0.00");
        if (t > 1) {
            hrSize = dec.format(t).concat(" TB");
        } else if (g > 1) {
            hrSize = dec.format(g).concat(" GB");
        } else if (m > 1) {
            hrSize = dec.format(m).concat(" MB");
        } else if (k > 1) {
            hrSize = dec.format(k).concat(" KB");
        } else {
            hrSize = dec.format(b).concat(" Bytes");
        }
        return hrSize;
    }

    public static Comparable<? extends Comparable<? extends Comparable<?>>> bitmapCompressFormat(ImageType imageType) {
        return imageType == ImageType.PNG ? Bitmap.CompressFormat.PNG : (imageType == ImageType.WEBP ? Bitmap.CompressFormat.WEBP : Bitmap.CompressFormat.JPEG);
    }

    public static String imgFormat(ImageType imageType) {
        return imageType == ImageType.PNG ? ".png" : (imageType == ImageType.WEBP ? ".webp" : ".jpg");
    }

    public static void saveBitmapToFile(Bitmap bitmap, String destPath) {
        FileOutputStream out = null;
        FileUtil.createNewFile(destPath);
        try {
            out = new FileOutputStream(new File(destPath));
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (out != null) out.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static void viewToImage(Context context, View view) {
        try {
            view.measure(View.MeasureSpec.makeMeasureSpec(view.getWidth(), View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(view.getHeight(), View.MeasureSpec.EXACTLY));
            view.layout((int) view.getX(), (int) view.getY(), (int) view.getX() + view.getMeasuredWidth(), (int) view.getY() + view.getMeasuredHeight());

            view.setDrawingCacheEnabled(true);
            view.buildDrawingCache(true);
            Bitmap bitmap = Bitmap.createBitmap(view.getDrawingCache());
            view.setDrawingCacheEnabled(false);

            //saveBitmapToFile(context, bitmap);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static Bitmap drawableToBitmap(Drawable drawable) {
        Bitmap bitmap = null;
        if (drawable instanceof BitmapDrawable) {
            BitmapDrawable bitmapDrawable = (BitmapDrawable) drawable;
            if (bitmapDrawable.getBitmap() != null) {
                return bitmapDrawable.getBitmap();
            }
        }
        //int width = drawable.getIntrinsicWidth();
        //width = width > 0 ? width : 1;
        //int height = drawable.getIntrinsicHeight();
        //height = height > 0 ? height : 1;
        if (drawable.getIntrinsicWidth() <= 0 || drawable.getIntrinsicHeight() <= 0) {
            bitmap = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888);
        } else {
            bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
        }
        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        drawable.draw(canvas);
        return bitmap;
    }

    public static InputStream bitmapToInputStream(Bitmap bitmap) {
        int size = bitmap.getHeight() * bitmap.getRowBytes();
        ByteBuffer buffer = ByteBuffer.allocate(size);
        bitmap.copyPixelsToBuffer(buffer);
        return new ByteArrayInputStream(buffer.array());
    }

    public static Bitmap getBitmapFromURL(String url) {
        return getBitmapFromURL(url, 0, 0);
    }

    public static Bitmap getBitmapFromURL(String url, int reqWidth, int reqHeight) {
        try {
            URL mUrl = new URL(url);
            HttpURLConnection connection = (HttpURLConnection) mUrl.openConnection();
            connection.setDoInput(true);
            connection.connect();
            if (reqWidth <= 0 || reqHeight <= 0) {
                return BitmapFactory.decodeStream(connection.getInputStream());
            }
            return decodeSampleBitmapFromStream(connection.getInputStream(), reqWidth, reqHeight);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    // Makes HttpURLConnection and returns InputStream
    public static InputStream getHttpConnection(String urlString) throws IOException {
        InputStream stream = null;
        URL url = new URL(urlString);
        URLConnection connection = url.openConnection();
        try {
            HttpURLConnection httpConnection = (HttpURLConnection) connection;
            httpConnection.setRequestMethod("GET");
            httpConnection.connect();
            if (httpConnection.getResponseCode() == HttpURLConnection.HTTP_OK) {
                stream = httpConnection.getInputStream();
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return stream;
    }

    public static Bitmap getAlbumArtCover(String audioFilePath) {
        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        try {
            retriever.setDataSource(audioFilePath);
            byte[] picture = retriever.getEmbeddedPicture();
            InputStream inputStream;
            if (picture != null) {
                inputStream = new ByteArrayInputStream(picture);
            } else {
                inputStream = getPictureFallbackInputStream(audioFilePath);
            }

            if (inputStream != null) {
                return FileUtil.decodeSampleBitmapFromStream(inputStream, 80, 80);
            }
        } finally {
            retriever.release();
        }
        return null;
    }

    public static InputStream getPictureFallbackInputStream(String path) {
        String[] FALLBACKS = {"cover.jpg", "album.jpg", "folder.jpg"};
        File parent = new File(path).getParentFile();
        for (String fallback : FALLBACKS) {
            File cover = new File(parent, fallback);
            if (cover.exists()) {
                try {
                    return new FileInputStream(cover);
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }
            }
        }
        return null;
    }

    public static Bitmap getScaledBitmap(String path, int max) {
        Bitmap src = BitmapFactory.decodeFile(path);

        int width = src.getWidth();
        int height = src.getHeight();
        float rate = 0.0f;

        if (width > height) {
            rate = max / (float) width;
            height = (int) (height * rate);
            width = max;
        } else {
            rate = max / (float) height;
            width = (int) (width * rate);
            height = max;
        }

        return Bitmap.createScaledBitmap(src, width, height, true);
    }

    public static int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
        final int width = options.outWidth;
        final int height = options.outHeight;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {
            final int halfHeight = height / 2;
            final int halfWidth = width / 2;

            while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2;
            }
        }

        return inSampleSize;
    }

    public static Bitmap decodeSampleBitmapFromStream(@Nullable InputStream inputStream, int reqWidth, int reqHeight) {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        try {
            if (inputStream == null) return null;
            int n;
            byte[] buffer = new byte[1024];
            while ((n = inputStream.read(buffer)) > 0) {
                outputStream.write(buffer, 0, n);
            }
            return decodeSampleBitmapFromByteArray(outputStream.toByteArray(), reqWidth, reqHeight);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                outputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

//    public static Bitmap decodeSampleBitmapFromStream(InputStream inputStream, int reqWidth, int reqHeight) {
//        final BitmapFactory.Options options = new BitmapFactory.Options();
//        options.inJustDecodeBounds = true;
//        BitmapFactory.decodeStream(inputStream, null, options);
//
//        options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);
//
//        options.inJustDecodeBounds = false;
//        return BitmapFactory.decodeStream(inputStream, null, options);
//    }

    public static Bitmap decodeSampleBitmapFromByteArray(byte[] data, int reqWidth, int reqHeight) {
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeByteArray(data, 0, data.length, options);

        options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);

        options.inJustDecodeBounds = false;
        return BitmapFactory.decodeByteArray(data, 0, data.length, options);
    }

    public static Bitmap decodeSampleBitmapFromPath(String path, int reqWidth, int reqHeight) {
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(path, options);

        options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);

        options.inJustDecodeBounds = false;
        return BitmapFactory.decodeFile(path, options);
    }

    public static void resizeBitmapFileRetainRatio(String fromPath, String destPath, int max) {
        if (!isExistFile(fromPath)) return;
        Bitmap bitmap = getScaledBitmap(fromPath, max);
        saveBitmapToFile(bitmap, destPath);
    }

    public static void resizeBitmapFileToSquare(String fromPath, String destPath, int max) {
        if (!isExistFile(fromPath)) return;
        Bitmap src = BitmapFactory.decodeFile(fromPath);
        Bitmap bitmap = Bitmap.createScaledBitmap(src, max, max, true);
        saveBitmapToFile(bitmap, destPath);
    }

    public static void resizeBitmapFileToCircle(String fromPath, String destPath) {
        if (!isExistFile(fromPath)) return;
        Bitmap src = BitmapFactory.decodeFile(fromPath);
        Bitmap bitmap = Bitmap.createBitmap(src.getWidth(), src.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);

        final int color = 0xff424242;
        final Paint paint = new Paint();
        final Rect rect = new Rect(0, 0, src.getWidth(), src.getHeight());

        paint.setAntiAlias(true);
        canvas.drawARGB(0, 0, 0, 0);
        paint.setColor(color);
        canvas.drawCircle(src.getWidth() / 2, src.getHeight() / 2, src.getWidth() / 2, paint);
        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
        canvas.drawBitmap(src, rect, rect, paint);

        saveBitmapToFile(bitmap, destPath);
    }

    public static void resizeBitmapFileWithRoundedBorder(String fromPath, String destPath, int pixels) {
        if (!isExistFile(fromPath)) return;
        Bitmap src = BitmapFactory.decodeFile(fromPath);
        Bitmap bitmap = Bitmap.createBitmap(src.getWidth(), src.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);

        final int color = 0xff424242;
        final Paint paint = new Paint();
        final Rect rect = new Rect(0, 0, src.getWidth(), src.getHeight());
        final RectF rectF = new RectF(rect);
        final float roundPx = pixels;

        paint.setAntiAlias(true);
        canvas.drawARGB(0, 0, 0, 0);
        paint.setColor(color);
        canvas.drawRoundRect(rectF, roundPx, roundPx, paint);

        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
        canvas.drawBitmap(src, rect, rect, paint);

        saveBitmapToFile(bitmap, destPath);
    }

    public static void cropBitmapFileFromCenter(String fromPath, String destPath, int w, int h) {
        if (!isExistFile(fromPath)) return;
        Bitmap src = BitmapFactory.decodeFile(fromPath);

        int width = src.getWidth();
        int height = src.getHeight();

        if (width < w && height < h) return;

        int x = 0;
        int y = 0;

        if (width > w) x = (width - w) / 2;

        if (height > h) y = (height - h) / 2;

        int cw = w;
        int ch = h;

        if (w > width) cw = width;

        if (h > height) ch = height;

        Bitmap bitmap = Bitmap.createBitmap(src, x, y, cw, ch);
        saveBitmapToFile(bitmap, destPath);
    }

    public static void rotateBitmapFile(String fromPath, String destPath, float angle) {
        if (!isExistFile(fromPath)) return;
        Bitmap src = BitmapFactory.decodeFile(fromPath);
        Matrix matrix = new Matrix();
        matrix.postRotate(angle);
        Bitmap bitmap = Bitmap.createBitmap(src, 0, 0, src.getWidth(), src.getHeight(), matrix, true);
        saveBitmapToFile(bitmap, destPath);
    }

    public static void scaleBitmapFile(String fromPath, String destPath, float x, float y) {
        if (!isExistFile(fromPath)) return;
        Bitmap src = BitmapFactory.decodeFile(fromPath);
        Matrix matrix = new Matrix();
        matrix.postScale(x, y);

        int w = src.getWidth();
        int h = src.getHeight();

        Bitmap bitmap = Bitmap.createBitmap(src, 0, 0, w, h, matrix, true);
        saveBitmapToFile(bitmap, destPath);
    }

    public static void skewBitmapFile(String fromPath, String destPath, float x, float y) {
        if (!isExistFile(fromPath)) return;
        Bitmap src = BitmapFactory.decodeFile(fromPath);
        Matrix matrix = new Matrix();
        matrix.postSkew(x, y);

        int w = src.getWidth();
        int h = src.getHeight();

        Bitmap bitmap = Bitmap.createBitmap(src, 0, 0, w, h, matrix, true);
        saveBitmapToFile(bitmap, destPath);
    }

    public static void setBitmapFileColorFilter(String fromPath, String destPath, int color) {
        if (!isExistFile(fromPath)) return;
        Bitmap src = BitmapFactory.decodeFile(fromPath);
        Bitmap bitmap = Bitmap.createBitmap(src, 0, 0, src.getWidth() - 1, src.getHeight() - 1);
        Paint p = new Paint();
        ColorFilter filter = new LightingColorFilter(color, 1);
        p.setColorFilter(filter);
        Canvas canvas = new Canvas(bitmap);
        canvas.drawBitmap(bitmap, 0, 0, p);
        saveBitmapToFile(bitmap, destPath);
    }

    public static void setBitmapFileBrightness(String fromPath, String destPath, float brightness) {
        if (!isExistFile(fromPath)) return;
        Bitmap src = BitmapFactory.decodeFile(fromPath);
        ColorMatrix cm = new ColorMatrix(new float[]{1, 0, 0, 0, brightness, 0, 1, 0, 0, brightness, 0, 0, 1, 0, brightness, 0, 0, 0, 1, 0});

        Bitmap bitmap = Bitmap.createBitmap(src.getWidth(), src.getHeight(), src.getConfig());
        Canvas canvas = new Canvas(bitmap);
        Paint paint = new Paint();
        paint.setColorFilter(new ColorMatrixColorFilter(cm));
        canvas.drawBitmap(src, 0, 0, paint);
        saveBitmapToFile(bitmap, destPath);
    }

    public static void setBitmapFileContrast(String fromPath, String destPath, float contrast) {
        if (!isExistFile(fromPath)) return;
        Bitmap src = BitmapFactory.decodeFile(fromPath);
        ColorMatrix cm = new ColorMatrix(new float[]{contrast, 0, 0, 0, 0, 0, contrast, 0, 0, 0, 0, 0, contrast, 0, 0, 0, 0, 0, 1, 0});

        Bitmap bitmap = Bitmap.createBitmap(src.getWidth(), src.getHeight(), src.getConfig());
        Canvas canvas = new Canvas(bitmap);
        Paint paint = new Paint();
        paint.setColorFilter(new ColorMatrixColorFilter(cm));
        canvas.drawBitmap(src, 0, 0, paint);

        saveBitmapToFile(bitmap, destPath);
    }

    public static int getJpegRotate(String filePath) {
        int rotate = 0;
        try {
            ExifInterface exif = new ExifInterface(filePath);
            int iOrientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, -1);

            switch (iOrientation) {
                case ExifInterface.ORIENTATION_ROTATE_90:
                    rotate = 90;
                    break;
                case ExifInterface.ORIENTATION_ROTATE_180:
                    rotate = 180;
                    break;
                case ExifInterface.ORIENTATION_ROTATE_270:
                    rotate = 270;
                    break;
                default:
                    rotate = 0;
                    break;
            }
        } catch (IOException e) {
            return 0;
        }

        return rotate;
    }

    public static int compareText(String str, String str2) {
        if (str.length() == 0 && str2.length() == 0) {
            return 0;
        }
        if (str.length() == 0) {
            return -1;
        }
        if (str2.length() == 0) {
            return 1;
        }
        int max = Math.max(str.length(), str2.length());
        int i = 0;
        while (i < max) {
            if (i >= str.length()) {
                return -1;
            }
            if (i >= str2.length()) {
                return 1;
            }
            char charAt = str.charAt(i);
            char charAt2 = str2.charAt(i);
            if (charAt != '0' || charAt2 != '0') {
                if (Character.isDigit(charAt) && Character.isDigit(charAt2)) {
                    if (charAt == '0') {
                        return -1;
                    }
                    if (charAt2 == '0') {
                        return 1;
                    }
                    int i2;
                    char charAt3;
                    StringBuilder stringBuilder;
                    String str3 = "";
                    String str4 = str3;
                    for (i2 = i; i2 < max; i2++) {
                        if (i2 >= str.length()) {
                            return -1;
                        }
                        charAt3 = str.charAt(i2);
                        if (!Character.isDigit(charAt3)) {
                            break;
                        }
                        stringBuilder = new StringBuilder();
                        stringBuilder.append(str4);
                        stringBuilder.append(charAt3);
                        str4 = stringBuilder.toString();
                    }
                    i2 = i;
                    while (i2 < max) {
                        if (i2 >= str2.length()) {
                            return 1;
                        }
                        charAt3 = str2.charAt(i2);
                        if (Character.isDigit(charAt3)) {
                            stringBuilder = new StringBuilder();
                            stringBuilder.append(str3);
                            stringBuilder.append(charAt3);
                            str3 = stringBuilder.toString();
                            i2++;
                        }
                    }
                    try {
                        int intValue = Integer.valueOf(str4).intValue();
                        try {
                            int intValue2 = Integer.valueOf(str3).intValue();
                            if (intValue < intValue2) {
                                return -1;
                            }
                            if (intValue > intValue2) {
                                return 1;
                            }
                            if (intValue == intValue2) {
                                i = i2;
                            }
                        } catch (NumberFormatException unused) {
                            return 1;
                        }
                    } catch (NumberFormatException unused2) {
                        return -1;
                    }
                } else if (charAt != charAt2) {
                    return charAt - charAt2;
                }
            }
            i++;
        }
        return 0;
    }

    public static class FileDateComparator implements Comparator<File> {
        int mOrder;

        public FileDateComparator(int i) {
            this.mOrder = i;
        }

        public int compare(File file, File file2) {
            if (this.mOrder == 2) {
                if (file.lastModified() < file2.lastModified()) {
                    return 1;
                }
                return file.lastModified() == file2.lastModified() ? 0 : -1;
            } else if (file2.lastModified() < file.lastModified()) {
                return 1;
            } else {
                return file.lastModified() == file2.lastModified() ? 0 : -1;
            }
        }
    }

    public static class FileNameComparator implements Comparator<File> {
        int mOrder;

        public FileNameComparator(int i) {
            this.mOrder = i;
        }

        public int compare(File file, File file2) {
            if (this.mOrder == 1) {
                return compareText(file.getName().toLowerCase(), file2.getName().toLowerCase());
            }
            return compareText(file2.getName().toLowerCase(), file.getName().toLowerCase());
        }
    }

    public static class FileSizeComparator implements Comparator<File> {
        int mOrder;

        public FileSizeComparator(int i) {
            this.mOrder = i;
        }

        public int compare(File file, File file2) {
            if (this.mOrder == 2) {
                if (file.length() < file2.length()) {
                    return 1;
                }
                return file.length() == file2.length() ? 0 : -1;
            } else if (file2.length() < file.length()) {
                return 1;
            } else {
                return file.length() == file2.length() ? 0 : -1;
            }
        }
    }

    public enum MediaType {
        None, Image, Video, Audio, Apk, Other
    }

    public enum ImageType {
        JPG, PNG, WEBP
    }

}