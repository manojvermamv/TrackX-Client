package com.android.sus_client.commonutility;

import android.os.Handler;
import android.os.Looper;

import com.android.sus_client.commonutility.basic.Callback1;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TaskRunner {

    // Change according to your requirements
    private final ExecutorService executor = Executors.newFixedThreadPool(3);
    private final Handler handler;

    public TaskRunner() {
        this(true);
    }

    public TaskRunner(boolean onMainThread) {
        handler = (onMainThread ? new Handler(Looper.getMainLooper()) : new Handler());
    }

    public <R> void executeAsync(Callable<R> callable, Callback1<R> callback) {
        executor.submit(() -> {
            R result = null;
            try {
                result = callable.call();
            } catch (Exception e) {
                e.printStackTrace();
            }

            final R finalResult = result;
            if (callback != null) {
                handler.post(() -> {
                    callback.onComplete(finalResult);
                });
            }
        });
    }

}