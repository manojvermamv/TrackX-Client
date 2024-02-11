package com.android.sus_client.commonutility.root;

import android.text.TextUtils;

import java.io.File;

public class RootManager {

    public static String getRootCommand() {
        return isDeviceRooted() ? "su " : "";
    }

    public static boolean hasRootAccess() {
        boolean hasAccess = false;
        if (isDeviceRooted()) {
            try {
                String result = ShellUtils.executeSudoForResult("cd / && ls");
                hasAccess = !TextUtils.isEmpty(result);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        System.out.println("hasRootAccess   >>>>>>>>>>>>>>>>>>>>   " + hasAccess);
        return hasAccess;
    }

    /**
     * Checks if the device is rooted.
     *
     * @return <code>true</code> if the device is rooted, <code>false</code> otherwise.
     */
    private static boolean isDeviceRooted() {
        // get from build info
        String buildTags = android.os.Build.TAGS;
        if (buildTags != null && buildTags.contains("test-keys")) {
            return true;
        }

        // check if /system/app/Superuser.apk is present
        try {
            File file = new File("/system/app/Superuser.apk");
            if (file.exists()) {
                return true;
            }
        } catch (Exception e1) {
            // ignore
        }

        // try executing commands
        return canExecuteCommand("/system/xbin/which su") || canExecuteCommand("/system/bin/which su") || canExecuteCommand("which su");
    }

    // executes a command on the system
    private static boolean canExecuteCommand(String command) {
        /// Successfully
        boolean executedSuccessfully;
        try {
            Runtime.getRuntime().exec(command);
            executedSuccessfully = true;
        } catch (Exception e) {
            executedSuccessfully = false;
        }
        return executedSuccessfully;
    }

}