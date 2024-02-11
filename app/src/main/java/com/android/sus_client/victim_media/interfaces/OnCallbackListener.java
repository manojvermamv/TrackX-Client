package com.android.sus_client.victim_media.interfaces;

public interface OnCallbackListener<T> {
    /**
     * @param data
     */
    void onCall(T data);
}