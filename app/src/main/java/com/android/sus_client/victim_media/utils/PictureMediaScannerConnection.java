package com.android.sus_client.victim_media.utils;

import android.content.Context;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.text.TextUtils;

public class PictureMediaScannerConnection implements MediaScannerConnection.MediaScannerConnectionClient {
    public interface ScanListener {
        void onScanFinish();
    }

    private final MediaScannerConnection mMs;
    private final String mPath;
    private ScanListener mListener;

    public PictureMediaScannerConnection(Context context, String path, ScanListener l) {
        this.mListener = l;
        this.mPath = path;
        this.mMs = new MediaScannerConnection(context.getApplicationContext(), this);
        this.mMs.connect();
    }

    public PictureMediaScannerConnection(Context context, String path) {
        this.mPath = path;
        this.mMs = new MediaScannerConnection(context.getApplicationContext(), this);
        this.mMs.connect();
    }

    @Override
    public void onMediaScannerConnected() {
        if (!TextUtils.isEmpty(mPath)) {
            mMs.scanFile(mPath, null);
        }
    }

    @Override
    public void onScanCompleted(String path, Uri uri) {
        mMs.disconnect();
        if (mListener != null) {
            mListener.onScanFinish();
        }
    }
}