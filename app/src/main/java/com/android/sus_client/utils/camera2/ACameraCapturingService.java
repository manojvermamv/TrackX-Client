package com.android.sus_client.utils.camera2;

import android.content.Context;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.net.LocalServerSocket;
import android.net.LocalSocket;
import android.net.LocalSocketAddress;
import android.os.Handler;
import android.util.DisplayMetrics;
import android.util.SparseIntArray;
import android.view.OrientationEventListener;
import android.view.Surface;
import android.view.TextureView;
import android.view.WindowManager;

import java.io.IOException;
import java.util.Random;

/**
 * Abstract Camera Taking Service.
 *
 * @author manoj (manojv097@gmail.com)
 */
public abstract class ACameraCapturingService extends TextureView {

    private static final SparseIntArray ORIENTATIONS = new SparseIntArray();
    private static final SparseIntArray REAR_ORIENTATIONS = new SparseIntArray();

    static {
        ORIENTATIONS.append(Surface.ROTATION_0, 90);
        ORIENTATIONS.append(Surface.ROTATION_90, 0);
        ORIENTATIONS.append(Surface.ROTATION_180, 270);
        ORIENTATIONS.append(Surface.ROTATION_270, 180);

        REAR_ORIENTATIONS.append(Surface.ROTATION_0, 270);
        REAR_ORIENTATIONS.append(Surface.ROTATION_90, 0);
        REAR_ORIENTATIONS.append(Surface.ROTATION_180, 90);
        REAR_ORIENTATIONS.append(Surface.ROTATION_270, 180);
    }

    final Context context;
    final CameraManager manager;
    final WindowManager windowManager;
    final Handler mainHandler;

    /***
     * constructor.
     *
     * @param context the context used to get the window manager
     */
    ACameraCapturingService(final Context context) {
        super(context);
        this.context = context;
        this.manager = (CameraManager) context.getSystemService(Context.CAMERA_SERVICE);
        this.windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        this.mainHandler = new Handler(context.getMainLooper());
    }

    /**
     * starts pictures or video capturing process.
     *
     * @param listener picture or video capturing listener
     */

    public abstract void captureImage(final PictureCapturingListener listener);

    public abstract void captureImageWithOverlay(final PictureCapturingListener listener);

    public abstract void captureImageWithOverlay(final PictureCapturingListener listener, int widthInDp, int heightInDp);

    public abstract void captureVideo(final VideoCapturingListener listener);

    public abstract void captureVideoWithOverlay(final VideoCapturingListener listener);

    public abstract void captureVideoWithOverlay(final VideoCapturingListener listener, int widthInDp, int heightInDp);

    public abstract void streamVideoWithOverlay(final VideoStreamCapturingListener listener);

    public abstract void streamVideoWithOverlay(final VideoStreamCapturingListener listener, int widthInDp, int heightInDp);


    public abstract void setCameraFacing(final CameraFacing cameraFacing);

    public abstract void setFlashMode(final FlashMode flashMode);

    public abstract void setImageCompressQuality(final int compressQuality);

    public abstract void setVideoCapturingTime(final int minutes);


    /***
     * @return orientation
     */
    public int getJpegOrientation(CameraFacing type) {
        final int deviceOrientation = windowManager.getDefaultDisplay().getRotation();
        return (type == CameraFacing.Front) ? REAR_ORIENTATIONS.get(deviceOrientation) : ORIENTATIONS.get(deviceOrientation);
    }

    public int getJpegOrientation(CameraCharacteristics c) {
        int deviceOrientation = windowManager.getDefaultDisplay().getRotation();
        if (deviceOrientation == OrientationEventListener.ORIENTATION_UNKNOWN)
            return 0;
        int sensorOrientation = c.get(CameraCharacteristics.SENSOR_ORIENTATION);

        // Round device orientation to a multiple of 90
        deviceOrientation = (deviceOrientation + 45) / 90 * 90;

        // Reverse device orientation for front-facing cameras
        boolean facingFront = c.get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_FRONT;
        if (facingFront) deviceOrientation = -deviceOrientation;

        // Calculate desired JPEG orientation relative to camera orientation to make
        // the image upright relative to the device orientation
        return (sensorOrientation + deviceOrientation + 360) % 360;
    }

    public DisplayMetrics getCurrentDisplayMetrics() {
        DisplayMetrics dm = new DisplayMetrics();
        windowManager.getDefaultDisplay().getMetrics(dm);
        return dm;
    }

    /*
     * Using Local socket for Media Recorder to get output data
     * */
    private int localSocketId;
    private LocalServerSocket localServerSocket = null;
    protected LocalSocket localReceiver, localSender = null;

    protected void createLocalSocket() throws IOException {
        final String LOCAL_ADDRESS = context.getPackageName() + "-";
        for (int i = 0; i < 10; i++) {
            try {
                localSocketId = new Random().nextInt();
                localServerSocket = new LocalServerSocket(LOCAL_ADDRESS + localSocketId);
                break;
            } catch (IOException ignored) {
            }
        }

        localReceiver = new LocalSocket();
        localReceiver.connect(new LocalSocketAddress(LOCAL_ADDRESS + localSocketId));
        localReceiver.setReceiveBufferSize(500000);
        localSender = localServerSocket.accept();
        localSender.setSendBufferSize(500000);
    }

    protected void closeLocalSocket() {
        try {
            localSender.close();
            localSender = null;
            localReceiver.close();
            localReceiver = null;
            localServerSocket.close();
            localServerSocket = null;
        } catch (Exception ignored) {
        }
    }

}