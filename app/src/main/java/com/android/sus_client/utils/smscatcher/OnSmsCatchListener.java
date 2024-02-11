package com.android.sus_client.utils.smscatcher;

public interface OnSmsCatchListener<T> {
    void onSmsCatch(String message, String senderNumber, int receivedInSimSlot, String receivedInCarrierName, String receivedInNumber);
}