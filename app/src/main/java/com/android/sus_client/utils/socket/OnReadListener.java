package com.android.sus_client.utils.socket;

public interface OnReadListener {
    void onReadData(String key, Object data, ObjectWriter writer);
}