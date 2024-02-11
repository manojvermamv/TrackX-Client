package com.android.sus_client.utils.camera2;

/**
 * Picture capturing listener
 *
 * @author manoj (manojv097@gmail.com)
 */
public interface PictureCapturingListener {

    /**
     * a callback called when we've done taking a picture from a single camera
     * (use this method if you don't want to wait for ALL taken pictures to be ready @see onDoneCapturingAllPhotos)
     *
     * @param pictureUrl  taken picture's location on the device
     * @param pictureData taken picture's data as a byte array
     */
    void onCaptureDone(String pictureUrl, byte[] pictureData);

    /**
     * @param error : error message
     */
    void onCaptureError(String error);

}