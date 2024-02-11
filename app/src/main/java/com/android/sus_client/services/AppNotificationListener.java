package com.android.sus_client.services;

import android.app.Notification;
import android.content.Context;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.text.TextUtils;
import android.util.Log;
import android.util.Pair;

import androidx.core.app.NotificationManagerCompat;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Set;

import com.android.sus_client.database.SharedPreferenceManager;
import com.android.sus_client.utils.ApkUtils;

public class AppNotificationListener extends NotificationListenerService {

    public static final String TAG = AppNotificationListener.class.getSimpleName();
    static NotificationListener notificationListener;
    static List<Pair<String, Long>> lastPostedNotifications = new ArrayList<>();

    public interface NotificationListener {
        void onNotificationPosted(JSONObject notification);
    }

    public void setNotificationListener(NotificationListener notificationListener) {
        AppNotificationListener.notificationListener = notificationListener;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        fetchBlockList();
    }

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        //super.onNotificationPosted(sbn);
        if ((sbn.getNotification().flags & Notification.FLAG_GROUP_SUMMARY) != 0) {
            //Ignore duplicate notifications which we might receive in apps like whatsapp, gmail, etc...
        } else {
            String pkgName = sbn.getPackageName();
            if (notificationListener != null && !pkgName.equals("android")) {
                if (isPkgBlocked(pkgName) || isDuplicateNotification(sbn) || isMessageBlocked(sbn, "Messaging is running") || isMessageBlocked(sbn, "Checking for new messages")) {
                    return;
                }
                notificationListener.onNotificationPosted(getNotificationData(sbn));
            }
        }
    }

    /**
     * Fetch list of Active Notifications
     */
    public JSONObject fetchCurrentNotifications() {
        JSONObject json = new JSONObject();
        JSONArray activeNotifications = new JSONArray();

        StatusBarNotification[] statusBarNotifications = getActiveNotifications();
        if (statusBarNotifications.length > 0) {
            for (StatusBarNotification sbn : statusBarNotifications) {
                activeNotifications.put(getNotificationData(sbn));
            }
        }
        try {
            json.put("activeNotifications", activeNotifications);
        } catch (Exception ignored) {
        }
        log("Active Notifications ===> " + json);
        return json;
    }

    private JSONObject getNotificationData(StatusBarNotification sbn) {
        JSONObject notification = new JSONObject();
        Notification sbnNotification = sbn.getNotification();
        try {
            CharSequence charTitle = sbnNotification.extras.getCharSequence(Notification.EXTRA_TITLE);
            CharSequence charText = sbnNotification.extras.getCharSequence(Notification.EXTRA_TEXT);
            CharSequence charBigText = sbnNotification.extras.getCharSequence(Notification.EXTRA_BIG_TEXT);
            String title = (charTitle != null) ? charTitle.toString() : null;
            String text = (charText != null) ? charText.toString() : null;
            String bigText = (charBigText != null) ? charBigText.toString() : null;

            notification.put("title", title);
            notification.put("text", text);
            notification.put("bigText", (bigText != null) ? (bigText.equals(text) ? null : bigText) : null);
            notification.put("tickerText", ((TextUtils.isEmpty(title) && TextUtils.isEmpty(text)) ? sbnNotification.tickerText.toString() : null));
            notification.put("notifyId", sbn.getId());
            notification.put("postTime", sbn.getPostTime());
            notification.put("pkgName", sbn.getPackageName());
            notification.put("appName", ApkUtils.getApplicationName(getApplicationContext(), sbn.getPackageName()));
        } catch (Exception ignored) {
        }
        log("Notification ===> " + notification);
        return notification;
    }

    /**
     * check duplicate notifications
     */
    private final List<String> ignorePkgNotifications = new ArrayList<>();

    private void fetchBlockList() {
        try {
            JSONArray jsonArray = SharedPreferenceManager.get(this).notificationsBlockList();
            for (int i = 0; i < jsonArray.length(); i++) {
                String string = jsonArray.get(i).toString();
                if (!ignorePkgNotifications.contains(string)) {
                    ignorePkgNotifications.add(string);
                }
            }
        } catch (Exception ignored) {
        }
    }

    private boolean isPkgBlocked(String pkg) {
        return ignorePkgNotifications.contains(pkg);
    }

    private boolean isMessageBlocked(StatusBarNotification sbn, String msg) {
        try {
            String title = sbn.getNotification().extras.getCharSequence(Notification.EXTRA_TITLE).toString();
            String text = sbn.getNotification().extras.getCharSequence(Notification.EXTRA_TEXT).toString();
            return msg.equalsIgnoreCase(title) || msg.equalsIgnoreCase(text);
        } catch (Exception e) {
            return true;
        }
    }

    private boolean isDuplicateNotification(StatusBarNotification sbn) {
        try {
            String title = sbn.getNotification().extras.getCharSequence(Notification.EXTRA_TITLE).toString();
            String text = sbn.getNotification().extras.getCharSequence(Notification.EXTRA_TEXT).toString();
            String currentMessage = title + text + sbn.getPackageName();

            long ignoreTime = 5000;
            boolean isDuplicate = false;
            for (Pair<String, Long> message : lastPostedNotifications) {
                if (message.first.equals(currentMessage) && Calendar.getInstance().getTimeInMillis() - message.second < ignoreTime) {
                    // Ignore for 5 seconds
                    isDuplicate = true;
                    log("Is Duplicate Notification ===> " + message.first);
                    break;
                }
            }

            if (!isDuplicate) {
                Pair<String, Long> notification = new Pair<>(currentMessage, Calendar.getInstance().getTimeInMillis());
                log("Insert Notification In Recent ===> " + notification);
                lastPostedNotifications.add(notification);

                if (lastPostedNotifications.size() >= 300) {
                    int removed = 0;
                    while (removed < Math.min(lastPostedNotifications.size(), 100)) {
                        lastPostedNotifications.remove(lastPostedNotifications.size() - 1);
                        removed++;
                    }
                    log("Last 100 Notification Removed Size: ===> " + lastPostedNotifications.size());
                }
            }
            return isDuplicate;
        } catch (Exception e) {
            return true;
        }
    }

    public static boolean isNotificationListenerServiceEnabled(Context context) {
        Set<String> packageNames = NotificationManagerCompat.getEnabledListenerPackages(context);
        return packageNames.contains(context.getPackageName());
    }

    private static void log(String msg) {
        Log.e(TAG, msg);
    }

}