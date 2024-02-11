package com.android.sus_client.commonutility.root;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.DatagramSocket;
import java.net.Socket;
import java.util.Objects;


public class ShellUtils {

    private Process mProcess;
    private DataOutputStream outputStream;
    private String prefixCommand = "";

    public static ShellUtils getInstance() {
        return new ShellUtils();
    }

    private ShellUtils() {
        getProcess(false);
    }

    public boolean getProcess(boolean root) {
        try {
            String prefix = root ? "su" : "/system/bin/sh";
            if (mProcess == null || !prefixCommand.equals(prefix)) {
                prefixCommand = prefix;
                ProcessBuilder builder = new ProcessBuilder(prefix);
                builder.redirectErrorStream(true);
                mProcess = builder.start();
                outputStream = new DataOutputStream(mProcess.getOutputStream());
            }
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    // Checkout: https://stackoverflow.com/questions/20932102/execute-shell-command-from-android
    public String execute(String command) {
        try {
            outputStream.writeBytes(command.trim() + "\n");
            outputStream.flush();

            outputStream.writeBytes("echo ---EOF---\n");
            outputStream.flush();

            BufferedReader reader = new BufferedReader(new InputStreamReader(mProcess.getInputStream()));
            StringBuilder result = new StringBuilder();
            while (true) {
                String line = reader.readLine();
                if (Objects.equals(line, "---EOF---")) break;
                result.append(line).append("\n");
            }
            return result.toString();
        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }
    }

    public void exit() {
        try {
            outputStream.writeBytes("exit\n");
            outputStream.flush();
            mProcess.waitFor();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            closeSilently(outputStream);
        }
    }


    /**
     * Global functions for Run commands
     */
    public static String executeSudoForResult(String... strings) {
        return executeForResultInternally("su", strings);
    }

    public static String executeForResult(String... strings) {
        String result = "";
        if (RootManager.hasRootAccess()) {
            result = executeForResultInternally("su", strings);
        } else {
            result = executeForResultInternally("/system/bin/sh", strings);
        }
        System.out.println("executeForResult:: " + result);
        return result;
    }

    private static String executeForResultInternally(String baseCmd, String... strings) {
        String res = "";
        DataOutputStream outputStream = null;
        InputStream response = null;
        try {
            Process process = Runtime.getRuntime().exec(baseCmd);
            outputStream = new DataOutputStream(process.getOutputStream());
            response = process.getInputStream();

            for (String s : strings) {
                outputStream.writeBytes(s + "\n");
                outputStream.flush();
            }

            outputStream.writeBytes("exit\n");
            outputStream.flush();
            try {
                process.waitFor();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            res = readFully(response);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            closeSilently(outputStream, response);
        }
        return res;
    }

    private static String readFully(InputStream is) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int length = 0;
        while ((length = is.read(buffer)) != -1) {
            baos.write(buffer, 0, length);
        }
        return baos.toString("UTF-8");
    }

    private static void closeSilently(Object... xs) {
        // Note: on Android API levels prior to 19 Socket does not implement Closeable
        for (Object x : xs) {
            if (x != null) {
                try {
                    if (x instanceof Closeable) {
                        ((Closeable) x).close();
                    } else if (x instanceof Socket) {
                        ((Socket) x).close();
                    } else if (x instanceof DatagramSocket) {
                        ((DatagramSocket) x).close();
                    } else {
                        throw new RuntimeException("cannot close " + x);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

}