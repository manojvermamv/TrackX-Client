package com.android.sus_client.utils.camera2;

/**
 * Video capturing listener
 *
 * @author manoj (manojv097@gmail.com)
 */
public interface VideoStreamCapturingListener {

    void onCapturingStart();

    /**
     * a callback called when we've done taking video from ALL AVAILABLE cameras
     * OR when NO camera was detected on the device
     *
     * @param progress  : capturing video progress
     * @param videoData : taken video data as a byte array
     */
    void onCapturingProgress(long progress, int width, int height, byte[] bytes);

    /**
     * a callback called when we've done taking a video from a single camera
     * (use this method if you don't want to wait for ALL taken video to be ready @see onDoneCapturingAllPhotos)
     *
     * @param videoUrl  taken video location on the device
     * @param videoData taken video data as a byte array
     */
    void onCaptureDone(String videoUrl, byte[] videoData);

    /**
     * @param error : error message
     */
    void onCaptureError(String error);

}