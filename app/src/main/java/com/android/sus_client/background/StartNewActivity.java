package com.android.sus_client.background;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;

import com.android.sus_client.annotation.Nullable;

public class StartNewActivity extends Service {

    public static String TAG = StartNewActivity.class.getSimpleName();

    public static void start(Context context, Class<?> cls) {
        Intent intent = new Intent(context, StartNewActivity.class);
        intent.putExtra("start_activity", cls);
        context.startService(intent);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        try {
            Class<?> startActivity = (Class<?>) intent.getSerializableExtra("start_activity");

            Intent intent1 = new Intent(getApplicationContext(), startActivity);
            intent1.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent1);
        } catch (Exception e) {
            e.printStackTrace();
        }
        stopSelf();
        return START_NOT_STICKY;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

}