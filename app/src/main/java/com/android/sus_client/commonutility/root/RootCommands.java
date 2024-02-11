package com.android.sus_client.commonutility.root;

import android.content.Context;
import android.os.Build;
import android.system.Os;
import android.util.Log;

import com.lucemanb.RootTools.RootTools;
import com.lucemanb.RootTools.execution.Command;
import com.lucemanb.RootTools.execution.CommandCapture;
import com.lucemanb.RootTools.execution.Shell;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RootCommands {

    private static final String UNIX_ESCAPE_EXPRESSION = "(\\(|\\)|\\[|\\]|\\s|\'|\"|`|\\{|\\}|&|\\\\|\\?)";
    private static SimpleDateFormat simpledateformat = new SimpleDateFormat("yyyy-MM-dd HH:mm");

    public static String getCommandLineString(String input) {
        return input.replaceAll(UNIX_ESCAPE_EXPRESSION, "\\\\$1");
    }

    public static InputStream getFile(String path) {
        InputStream in = null;

        try {
            in = openFile("cat " + getCommandLineString(path));
        } catch (Exception e) {
            e.printStackTrace();
        }

        return in;
    }

    public static InputStream putFile(String path, String text) {
        InputStream in = null;

        try {
            in = openFile("echo \"" + text + "\" > " + getCommandLineString(path));
        } catch (Exception e) {
            e.printStackTrace();
        }

        return in;
    }

    public static BufferedReader listFiles(String path) {
        BufferedReader in = null;

        try {
            in = execute("ls -ls " + getCommandLineString(path));
        } catch (Exception e) {
            e.printStackTrace();
        }

        return in;
    }

    public static ArrayList<String> listFiles(String path, boolean showhidden) {
        ArrayList<String> mDirContent = new ArrayList<>();
        BufferedReader in;

        try {
            in = execute("ls -a " + getCommandLineString(path));

            String line;
            while ((line = in.readLine()) != null) {
                if (!showhidden) {
                    if (line.charAt(0) != '.') mDirContent.add(path + "/" + line);
                } else {
                    mDirContent.add(path + "/" + line);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return mDirContent;
    }

    public static BufferedReader findFiles(String path, String query) {
        ArrayList<String> mDirContent = new ArrayList<>();
        BufferedReader in = null;

        try {
            in = execute("find " + getCommandLineString(path) + " -type f -iname " + '*' + getCommandLineString(query) + '*' + " -exec ls -ls {} \\;");
        } catch (Exception e) {
            e.printStackTrace();
        }

        return in;
    }

    public static ArrayList<String> findFile(String path, String query) {
        ArrayList<String> mDirContent = new ArrayList<>();
        BufferedReader in;

        try {
            in = execute("find " + getCommandLineString(path) + " -type f -iname " + '*' + getCommandLineString(query) + '*' + " -exec ls -a {} \\;");

            String line;
            while ((line = in.readLine()) != null) {
                mDirContent.add(line);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return mDirContent;
    }

    // Create Directory with root
    public static boolean createRootdir(String parentPath, String name) {
        File dir = new File(parentPath + File.separator + name);
        if (dir.exists()) return false;

        try {
            if (!readReadWriteFile()) RootTools.remount(parentPath, "rw");

            execute("mkdir " + getCommandLineString(dir.getAbsolutePath()));
            return true;
        } catch (Exception e) {
            e.printStackTrace();
        }

        return false;
    }

    // Create file with root
    public static boolean createRootFile(String parentPath, String name) {
        File dir = new File(parentPath + File.separator + name);

        if (dir.exists()) return false;

        try {
            if (!readReadWriteFile()) RootTools.remount(parentPath, "rw");

            execute("touch " + getCommandLineString(dir.getAbsolutePath()));
            return true;
        } catch (Exception e) {
            e.printStackTrace();
        }

        return false;
    }

    // Move or Copy with Root Access using RootTools library
    public static boolean moveCopyRoot(String old, String newDir) {
        try {
            if (!readReadWriteFile()) RootTools.remount(newDir, "rw");

            execute("cp -fr " + getCommandLineString(old) + " " + getCommandLineString(newDir));
            return true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    // path = currentDir
    // oldName = currentDir + "/" + selected Item
    // name = new name
    public static boolean renameRootTarget(String path, String oldname, String name) {
        File file = new File(path + File.separator + oldname);
        File newf = new File(path + File.separator + name);

        if (name.length() < 1) return false;

        try {
            if (!readReadWriteFile()) RootTools.remount(path, "rw");

            execute("mv " + getCommandLineString(file.getAbsolutePath()) + " " + getCommandLineString(newf.getAbsolutePath()));
            return true;
        } catch (Exception e) {
            e.printStackTrace();
        }

        return false;
    }

    // path = currentDir
    // oldName = currentDir + "/" + selected Item
    // name = new name
    public static boolean renameRootTarget(RootFile before, RootFile after) {
        File file = new File(before.getParent() + File.separator + before.getName());
        File newf = new File(after.getParent() + File.separator + after.getName());

        if (after.getName().length() < 1) return false;

        try {
            if (!readReadWriteFile()) RootTools.remount(before.getPath(), "rw");

            execute("mv " + getCommandLineString(file.getAbsolutePath()) + " " + getCommandLineString(newf.getAbsolutePath()));
            return true;
        } catch (Exception e) {
            e.printStackTrace();
        }

        return false;
    }

    // Delete file with root
    public static boolean deleteFileRoot(String path) {
        try {
            if (!readReadWriteFile()) RootTools.remount(path, "rw");

            if (new File(path).isDirectory()) {
                execute("rm -f -r " + getCommandLineString(path));
            } else {
                execute("rm -r " + getCommandLineString(path));
            }
            return true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    // Check if system is mounted
    private static boolean readReadWriteFile() {
        File mountFile = new File("/proc/mounts");
        StringBuilder procData = new StringBuilder();
        if (mountFile.exists()) {
            try {
                FileInputStream fis = new FileInputStream(mountFile.toString());
                DataInputStream dis = new DataInputStream(fis);
                BufferedReader br = new BufferedReader(new InputStreamReader(dis));
                String data;
                while ((data = br.readLine()) != null) {
                    procData.append(data).append("\n");
                }

                br.close();
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }

            String[] tmp = procData.toString().split("\n");
            for (String aTmp : tmp) {
                // Kept simple here on purpose different devices have
                // different blocks
                if (aTmp.contains("/dev/block") && aTmp.contains("/system")) {
                    if (aTmp.contains("rw")) {
                        // system is rw
                        return true;
                    } else if (aTmp.contains("ro")) {
                        // system is ro
                        return false;
                    } else {
                        return false;
                    }
                }
            }
        }
        return false;
    }

    public static boolean containsIllegals(String toExamine) {
        // checks for "+" sign so the program doesn't throw an error when its
        // not erroring.
        Pattern pattern = Pattern.compile("[+]");
        Matcher matcher = pattern.matcher(toExamine);
        return matcher.find();
    }

    private static BufferedReader execute(String cmd) {
        BufferedReader reader;
        try {
            Process process = Runtime.getRuntime().exec("su");
            DataOutputStream os = new DataOutputStream(process.getOutputStream());
            os.writeBytes(cmd + "\n");
            os.writeBytes("exit\n");
            reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String err = (new BufferedReader(new InputStreamReader(process.getErrorStream()))).readLine();
            os.flush();

            if (process.waitFor() != 0 || (!"".equals(err) && null != err) && !containsIllegals(err)) {
                Log.e("Root Error, cmd: " + cmd, err);
                return null;
            }
            return reader;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private static InputStream openFile(String cmd) {
        InputStream inputStream;
        try {
            Process process = Runtime.getRuntime().exec("su");
            DataOutputStream os = new DataOutputStream(process.getOutputStream());
            os.writeBytes(cmd + "\n");
            os.writeBytes("exit\n");
            inputStream = process.getInputStream();
            String err = (new BufferedReader(new InputStreamReader(process.getErrorStream()))).readLine();
            os.flush();

            if (process.waitFor() != 0 || (!"".equals(err) && null != err) && !containsIllegals(err)) {
                Log.e("Root Error, cmd: " + cmd, err);
                return null;
            }
            return inputStream;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static boolean changeGroupOwner(File file, String owner, String group) {
        try {
            if (!readReadWriteFile()) RootTools.remount(file.getAbsolutePath(), "rw");

            execute("chown " + owner + "." + group + " " + getCommandLineString(file.getAbsolutePath()));
            return true;
        } catch (Exception e) {
            e.printStackTrace();
        }

        return false;
    }

    public static boolean applyPermissions(File file, RootFilePermissions rootFilePermissions) {
        try {
            if (!readReadWriteFile()) RootTools.remount(file.getAbsolutePath(), "rw");

            execute("chmod " + rootFilePermissions.toOctalPermission() + " " + getCommandLineString(file.getAbsolutePath()));
            return true;
        } catch (Exception e) {
            e.printStackTrace();
        }

        return false;
    }

    public static String[] getFileProperties(File file) {
        BufferedReader in;
        String[] info = null;
        String line;

        try {
            in = execute("ls -l " + getCommandLineString(file.getAbsolutePath()));

            while ((line = in.readLine()) != null) {
                info = getAttrs(line);
            }
            in.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return info;
    }

    private static String[] getAttrs(String string) {
        if (string.length() < 44) {
            throw new IllegalArgumentException("Bad ls -l output: " + string);
        }
        final char[] chars = string.toCharArray();

        final String[] results = new String[11];
        int ind = 0;
        final StringBuilder current = new StringBuilder();

        Loop:
        for (int i = 0; i < chars.length; i++) {
            switch (chars[i]) {
                case ' ':
                case '\t':
                    if (current.length() != 0) {
                        results[ind] = current.toString();
                        ind++;
                        current.setLength(0);
                        if (ind == 10) {
                            results[ind] = string.substring(i).trim();
                            break Loop;
                        }
                    }
                    break;

                default:
                    current.append(chars[i]);
                    break;
            }
        }

        return results;
    }

    public static long getTimeinMillis(String date) {
        long timeInMillis = 0;
        try {
            timeInMillis = simpledateformat.parse(date).getTime();
        } catch (Exception ignored) {
        }
        return timeInMillis;
    }

    /**
     * Custom methods
     */
    public static String[] getRootDir() {
        return getRootDir("/");
    }

    public static String[] getRootDir(String path) {
        String result = "";
        try {
            String command = "cd " + path.trim() + " && ls -l";
            //ShellUtils shellUtils = ShellUtils.getInstance();
            //shellUtils.getProcess(BaseService.hasRootAccess);
            //result = shellUtils.execute(command);

            CommandCapture commandCapture = new CommandCapture(0, false, command);
            Shell.runRootCommand(commandCapture);
            commandWait(commandCapture);
            result = commandCapture.toString();
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (result.trim().isEmpty()) {
            return new String[]{};
        }
        return result.trim().split("\n");
    }

    private static File getSymLinkedFile(Context context, String name) {
        File directory = new File(context.getApplicationInfo().dataDir, "symlink_files");
        if (directory.exists()) directory.delete();
        directory.mkdir();
        return new File(directory.getAbsolutePath(), name);
    }

    public static File createOrGetSymLink(Context context, String originalFilePath) {
        File originalFile = new File(originalFilePath);
        File symLinkFile = getSymLinkedFile(context, originalFile.getName());
        if (createSymLink(symLinkFile.getAbsolutePath(), originalFilePath)) {
            return symLinkFile.getAbsoluteFile();
        }
        return originalFile;
    }

    public static boolean createSymLink(String symLinkFilePath, String originalFilePath) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                Os.symlink(originalFilePath, symLinkFilePath);
                return true;
            }
            final Class<?> libcore = Class.forName("libcore.io.Libcore");
            final java.lang.reflect.Field fOs = libcore.getDeclaredField("os");
            fOs.setAccessible(true);
            final Object os = fOs.get(null);
            final java.lang.reflect.Method method = os.getClass().getMethod("symlink", String.class, String.class);
            method.invoke(os, originalFilePath, symLinkFilePath);
            return true;
        } catch (Exception e) {
            throw new Error(e.getMessage());
        } catch (Error e2) {
            e2.printStackTrace();
        }
        return false;
    }

    private static void commandWait(Command cmd) {
        synchronized (cmd) {
            try {
                if (!cmd.isFinished()) {
                    cmd.wait(2000);
                }
            } catch (InterruptedException ex) {
                Log.e(RootCommands.class.getSimpleName(), ex.toString());
            }
        }
    }

}