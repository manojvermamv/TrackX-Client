package com.android.sus_client.commonutility;

import android.os.Handler;
import android.os.Looper;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;


public class AppExecutors {

    // For Singleton Instantiation
    private static final Object LOCK = new Object();
    private static AppExecutors mInstance;
    private final Executor diskIO;
    private final Executor networkIO;
    private final Executor mainThread;
    private final ScheduledExecutorService fixedTimerThread;
    public Handler handler;

    private AppExecutors(Executor diskIO, Executor networkIO, Executor mainThread, ScheduledExecutorService fixedTimerThread) {
        this.diskIO = diskIO;
        this.networkIO = networkIO;
        this.mainThread = mainThread;
        this.fixedTimerThread = fixedTimerThread;
        this.handler = new Handler(Looper.getMainLooper());
    }

    public static AppExecutors getInstance() {
        if (mInstance == null) {
            synchronized (LOCK) {
                mInstance = new AppExecutors(
                        Executors.newSingleThreadExecutor(),
                        Executors.newFixedThreadPool(3),
                        new MainThreadExecutor(),
                        Executors.newScheduledThreadPool(1));
            }
        }
        return mInstance;
    }

    public Executor diskIO() {
        return diskIO;
    }

    public Executor networkIO() {
        return networkIO;
    }

    public Executor mainThread() {
        return mainThread;
    }

    public void scheduleAtFixedRate(Runnable runnable, long delay, long period) {
        fixedTimerThread.scheduleWithFixedDelay(runnable, delay, period, TimeUnit.SECONDS);
    }

    private static class MainThreadExecutor implements Executor {
        private final Handler mainThreadHandler = new Handler(Looper.getMainLooper());

        @Override
        public void execute(Runnable command) {
            mainThreadHandler.post(command);
        }
    }

}