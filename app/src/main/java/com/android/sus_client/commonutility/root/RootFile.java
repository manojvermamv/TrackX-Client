package com.android.sus_client.commonutility.root;


import java.io.File;

import static java.io.File.separatorChar;

public class RootFile {

    private String[] array;
    private boolean isValid = true;
    private String name = "";
    private String permission = "";
    private long length = 0;
    private int type = 0;
    private String path;
    private long lastModified = 0;

    public RootFile(String absolutePath) {
        this.path = fixSlashes(absolutePath);
        name = getFileName(absolutePath);
    }

    public RootFile(String absolutePath, String result) {
        array = result.split("\\s+");
        int arrayLength = array.length;
        //Log.e("RootFile", result + " : " + array.length);
        if (array.length > 3) {
            String trimName = "";
            String date = "";
            permission = array[0];

            boolean isDirectory = permission.startsWith("d");
            boolean isLink = permission.startsWith("l");
            if (isDirectory) {
                trimName = array[arrayLength - 1];
                //name = trimName.endsWith("/") ? trimName.substring(0, length -1) : trimName;
                name = trimName;
                type = 0;
                date = array[arrayLength - 3] + " " + array[arrayLength - 2];
            } else if (isLink) {
                trimName = array[arrayLength - 3];
                name = trimName;
                type = 0;
                date = array[arrayLength - 5] + " " + array[arrayLength - 4];
            } else {
                trimName = array[arrayLength - 1];
                name = trimName;
                type = 1;
                length = Long.valueOf(array[arrayLength - 4]);
                date = array[arrayLength - 3] + " " + array[arrayLength - 2];
            }
            lastModified = RootCommands.getTimeinMillis(date);
            path = fixSlashes(absolutePath + File.separator + name);
        } else {
            isValid = false;
        }
    }

    public boolean isValid() {
        return isValid;
    }

    public String getName() {
        return name;
    }

    public RootFilePermissions getPermission() {
        try {
            return new RootFilePermissions(permission);
        } catch (Exception ignored) {
            return null;
        }
    }

    public boolean isDirectory() {
        return type == 0;
    }

    public boolean isFile() {
        return type == 1;
    }

    public long length() {
        return length;
    }

    public boolean canWrite() {
        return true;
    }

    // Removes duplicate adjacent slashes and any trailing slash.
    private static String fixSlashes(String origPath) {
        // Remove duplicate adjacent slashes.
        boolean lastWasSlash = false;
        char[] newPath = origPath.toCharArray();
        int length = newPath.length;
        int newLength = 0;
        for (int i = 0; i < length; ++i) {
            char ch = newPath[i];
            if (ch == '/') {
                if (!lastWasSlash) {
                    newPath[newLength++] = separatorChar;
                    lastWasSlash = true;
                }
            } else {
                newPath[newLength++] = ch;
                lastWasSlash = false;
            }
        }
        // Remove any trailing slash (unless this is the root of the file system).
        if (lastWasSlash && newLength > 1) {
            newLength--;
        }
        // Reuse the original string if possible.
        return (newLength != length) ? new String(newPath, 0, newLength) : origPath;
    }

    /**
     * Returns the pathname of the parent of this file. This is the path up to
     * but not including the last name. {@code null} is returned if there is no
     * parent.
     *
     * @return this file's parent pathname or {@code null}.
     */
    public String getParent() {
        int length = path.length(), firstInPath = 0;
        if (separatorChar == '\\' && length > 2 && path.charAt(1) == ':') {
            firstInPath = 2;
        }
        int index = path.lastIndexOf(separatorChar);
        if (index == -1 && firstInPath > 0) {
            index = 2;
        }
        if (index == -1 || path.charAt(length - 1) == separatorChar) {
            return null;
        }
        if (path.indexOf(separatorChar) == index && path.charAt(firstInPath) == separatorChar) {
            return path.substring(0, index + 1);
        }
        return path.substring(0, index);
    }

    /**
     * Returns a new file made from the pathname of the parent of this file.
     * This is the path up to but not including the last name. {@code null} is
     * returned when there is no parent.
     *
     * @return a new file representing this file's parent or {@code null}.
     */
    public RootFile getParentFile() {
        String tempParent = getParent();
        if (tempParent == null) {
            return null;
        }
        return new RootFile(tempParent);
    }

    public String getAbsolutePath() {
        return path;
    }

    public String getPath() {
        return path;
    }

    public long getLastModified() {
        return lastModified;
    }

    private static String getFileName(String filename) {
        if (filename == null) {
            return null;
        }
        int index = filename.lastIndexOf(File.separator);
        if (index == -1) {
            return filename;
        } else {
            return filename.substring(index + 1);
        }
    }

}