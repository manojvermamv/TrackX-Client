package com.android.sus_client.utils;

import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import java.io.FileOutputStream;


public class ZipUtils {

    public static String zip(List<File> files, File outputZipFile) {
        return zip(new ArrayList<>(files), outputZipFile);
    }

    public static String zip(ArrayList<File> files, File outputZipFile) {
        outputZipFile.getParentFile().mkdirs();
        try (ZipOutputStream zipOutputStream = new ZipOutputStream(new FileOutputStream(outputZipFile))) {
            for (File file : files) {
                if (file.isDirectory()) {
                    zipDirectory(file, file.getName(), zipOutputStream);
                } else {
                    zipFile(file, zipOutputStream);
                }
            }
        } catch (Exception e) {
            Log.d("ZipUtils", "Zip Error: " + e);
        }
        return outputZipFile.getAbsolutePath();
    }

    private static void zipFile(File file, ZipOutputStream zipOutputStream) throws IOException {
        try (FileInputStream inputStream = new FileInputStream(file)) {
            ZipEntry zipEntry = new ZipEntry(file.getName());
            zipOutputStream.putNextEntry(zipEntry);
            copyStream(inputStream, zipOutputStream);
            zipOutputStream.closeEntry();
        }
    }

    private static void zipDirectory(File folder, String parentFolder, ZipOutputStream zipOutputStream) throws IOException {
        for (File file : folder.listFiles()) {
            if (file.isDirectory()) {
                zipDirectory(file, parentFolder + "/" + file.getName(), zipOutputStream);
            } else {
                try (FileInputStream inputStream = new FileInputStream(file)) {
                    ZipEntry zipEntry = new ZipEntry(parentFolder + "/" + file.getName());
                    zipOutputStream.putNextEntry(zipEntry);
                    copyStream(inputStream, zipOutputStream);
                    zipOutputStream.closeEntry();
                }
            }
        }
    }

    private static void copyStream(InputStream inputStream, OutputStream outputStream) throws IOException {
        byte[] buffer = new byte[1024]; // Adjust buffer size as needed
        int bytesRead;
        while ((bytesRead = inputStream.read(buffer)) > 0) {
            outputStream.write(buffer, 0, bytesRead);
        }
    }

}