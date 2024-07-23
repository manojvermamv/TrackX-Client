package com.android.sus_client.extra_feature.utils;

import android.util.Log;

import net.lingala.zip4j.ZipFile;
import net.lingala.zip4j.model.ZipParameters;
import net.lingala.zip4j.model.enums.CompressionLevel;
import net.lingala.zip4j.model.enums.CompressionMethod;

import java.io.File;
import java.util.ArrayList;

public class ZipUtils {

    public String zip(ArrayList<File> files, File outputZipFile) {
        outputZipFile.getParentFile().mkdirs();
        try {
            ZipParameters zipParameters = new ZipParameters();
            zipParameters.setCompressionMethod(CompressionMethod.DEFLATE);
            zipParameters.setCompressionLevel(CompressionLevel.FASTEST);
            zipParameters.setIncludeRootFolder(true);
            zipParameters.setReadHiddenFiles(true);
            zipParameters.setReadHiddenFolders(true);
            ZipFile zip = new ZipFile(outputZipFile);
            for (File f : files) {
                if (f.isDirectory()) {
                    zip.addFolder(f, zipParameters);
                } else {
                    zip.addFile(f, zipParameters);
                }
            }
        } catch (Exception e) {
            Log.d("ZipUtils", "Zip Error: " + e);
        }
        return outputZipFile.getAbsolutePath();
    }

}