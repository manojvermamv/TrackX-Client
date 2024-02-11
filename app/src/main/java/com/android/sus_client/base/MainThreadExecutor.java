package com.android.sus_client.base;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import java.util.concurrent.Executor;

public class MainThreadExecutor implements Executor {

    private final Handler mainThreadHandler;

    public MainThreadExecutor(Context context) {
        this.mainThreadHandler = new Handler(context.getMainLooper());
    }

    public Handler getHandler() {
        return mainThreadHandler;
    }

    public Handler getNewHandler() {
        return new Handler(Looper.getMainLooper());
    }

    @Override
    public void execute(Runnable command) {
        mainThreadHandler.post(command);
    }

}