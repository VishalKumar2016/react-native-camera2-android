package com.reactlibrary;


import android.Manifest;
import android.annotation.TargetApi;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.RectF;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.media.MediaRecorder;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.util.Size;
import android.util.SparseIntArray;
import android.view.Surface;
import android.widget.Toast;

import com.facebook.react.bridge.ReactApplicationContext;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

@TargetApi(Build.VERSION_CODES.LOLLIPOP)
public class Camera2RecorderView extends PermissionViewBase {

    private static final int SENSOR_ORIENTATION_DEFAULT_DEGREES = 90;
    private static final int SENSOR_ORIENTATION_INVERSE_DEGREES = 270;
    private static final SparseIntArray DEFAULT_ORIENTATIONS = new SparseIntArray();
    private static final SparseIntArray INVERSE_ORIENTATIONS = new SparseIntArray();

    static {
        DEFAULT_ORIENTATIONS.append(Surface.ROTATION_0, 90);
        DEFAULT_ORIENTATIONS.append(Surface.ROTATION_90, 0);
        DEFAULT_ORIENTATIONS.append(Surface.ROTATION_180, 270);
        DEFAULT_ORIENTATIONS.append(Surface.ROTATION_270, 180);
    }

    static {
        INVERSE_ORIENTATIONS.append(Surface.ROTATION_0, 270);
        INVERSE_ORIENTATIONS.append(Surface.ROTATION_90, 180);
        INVERSE_ORIENTATIONS.append(Surface.ROTATION_180, 90);
        INVERSE_ORIENTATIONS.append(Surface.ROTATION_270, 0);
    }


    protected boolean mSurfaceTextureIsAvailable = false;

    int tempWidth = -1;
    int tempHeight = -1;

    protected SurfaceTextureListener mSurfaceTextureListener
            = new SurfaceTextureListener() {

        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
            mSurfaceTextureIsAvailable = true;
            tempWidth = width;
            tempHeight = height;
            if (permissionsCheck(REQUEST_PERMISSIONS_OPEN2)) {
                openCamera(width, height);
            }
        }

        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
            configureTransform(width, height);
        }

        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
            return true;
        }

        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture surface) {

        }
    };
    protected HandlerThread mBackgroundThread;
    protected Surface mRecorderSurface;
    protected boolean mIsRecordingVideo = false;
    protected String mNextVideoAbsolutePath;
    protected int mFacing = CameraCharacteristics.LENS_FACING_BACK;
    protected int mVideoEncodingBitrate;
    protected int mVideoEncodingFrameRate;


    /**
     * The {@link CameraCharacteristics} for the currently configured camera device.
     */
    protected boolean mNoAFRun = false;

    protected void startBackgroundThread() {
        mBackgroundThread = new HandlerThread("CameraBackground");
        mBackgroundThread.start();
        mBackgroundHandler = new Handler(mBackgroundThread.getLooper());
    }

    protected void stopBackgroundThread() {
        if (mBackgroundThread == null) {
            return;
        }
        mBackgroundThread.quitSafely();
        try {
            mBackgroundThread.join();
            mBackgroundThread = null;
            mBackgroundHandler = null;
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    protected Semaphore mCameraOpenCloseLock = new Semaphore(1);
    protected String mCameraId;
    protected Integer mSensorOrientation;
    protected Size mVideoSize;
    protected Size mPreviewSize;
    protected MediaRecorder mMediaRecorder;
    protected CameraDevice mCameraDevice;
    protected Camera2Base mBase = this;
    protected CameraDevice.StateCallback mStateCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(CameraDevice camera) {
            mCameraDevice = camera;
            startPreview();
            mCameraOpenCloseLock.release();
            configureTransform(getWidth(), getHeight());
        }

        @Override
        public void onDisconnected(CameraDevice camera) {
            mCameraOpenCloseLock.release();
            camera.close();
            mCameraDevice = null;
        }

        @Override
        public void onError(CameraDevice camera, int error) {
            mCameraOpenCloseLock.release();
            camera.close();
            mCameraDevice = null;
            if (reactApplicationContext != null && reactApplicationContext.getCurrentActivity() != null) {
                reactApplicationContext.getCurrentActivity().finish();
            }
        }
    };
    protected CameraCaptureSession mPreviewSession;
    protected CaptureRequest.Builder mPreviewBuilder;
    protected Handler mBackgroundHandler;


    protected void startPreview() {
        if (mCameraDevice == null || !isAvailable() || mPreviewSize == null) {
            return;
        }
        try {
            closePreviewSession();
            SurfaceTexture texture = getSurfaceTexture();
            assert texture != null;
            texture.setDefaultBufferSize(mPreviewSize.getWidth(), mPreviewSize.getHeight());
            mPreviewBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            Surface previewSurface = new Surface(texture);
            mPreviewBuilder.addTarget(previewSurface);
            setup3AControlsLocked(mPreviewBuilder);
            mCameraDevice.createCaptureSession(Arrays.asList(previewSurface), new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(CameraCaptureSession cameraCaptureSession) {
                    mPreviewSession = cameraCaptureSession;
                    updatePreview();
                }

                @Override
                public void onConfigureFailed(CameraCaptureSession session) {
                    mBase.onCameraFailed();
                }
            }, mBackgroundHandler);


        } catch (CameraAccessException e) {
            e.printStackTrace();
            onCameraAccessException();
        }
    }

    protected void updatePreview() {
        if (mCameraDevice == null) {
            return;
        }
        try {
            setUpCaptureRequestBuilder(mPreviewBuilder);
            HandlerThread thread = new HandlerThread("CameraPreview");
            thread.start();
            mPreviewSession.setRepeatingRequest(mPreviewBuilder.build(), null, mBackgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
            onCameraAccessException();
        } catch (IllegalStateException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    protected void setUpCaptureRequestBuilder(CaptureRequest.Builder builder) {
        builder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO);
    }


    /**
     * Configure the given {@link CaptureRequest.Builder} to use auto-focus, auto-exposure, and
     * auto-white-balance controls if available.
     * <p/>
     * Call this only with {@link // #mCameraStateLock} held.
     *
     * @param builder the builder to configure.
     */
    private void setup3AControlsLocked(CaptureRequest.Builder builder) {
        // Enable auto-magical 3A run by camera device
        builder.set(CaptureRequest.CONTROL_MODE,
                CaptureRequest.CONTROL_MODE_AUTO);

        CameraCharacteristics mCharacteristics = null;
        try {
            mCharacteristics = manager.getCameraCharacteristics(mCameraDevice.getId());
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }

        Float minFocusDist =
                mCharacteristics.get(CameraCharacteristics.LENS_INFO_MINIMUM_FOCUS_DISTANCE);

        // If MINIMUM_FOCUS_DISTANCE is 0, lens is fixed-focus and we need to skip the AF run.

        mNoAFRun = (minFocusDist == null || minFocusDist == 0);

        if (!mNoAFRun) {
            // If there is a "continuous picture" mode available, use it, otherwise default to AUTO.
            if (contains(mCharacteristics.get(
                    CameraCharacteristics.CONTROL_AF_AVAILABLE_MODES),
                    CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE)) {
                builder.set(CaptureRequest.CONTROL_AF_MODE,
                        CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
            } else {
                builder.set(CaptureRequest.CONTROL_AF_MODE,
                        CaptureRequest.CONTROL_AF_MODE_AUTO);
            }
        }

        // If there is an auto-magical flash control mode available, use it, otherwise default to
        // the "on" mode, which is guaranteed to always be available.
        if (contains(mCharacteristics.get(
                CameraCharacteristics.CONTROL_AE_AVAILABLE_MODES),
                CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH)) {
            builder.set(CaptureRequest.CONTROL_AE_MODE,
                    CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH);
        } else {
            builder.set(CaptureRequest.CONTROL_AE_MODE,
                    CaptureRequest.CONTROL_AE_MODE_ON);
        }

        // If there is an auto-magical white balance control mode available, use it.
        if (contains(mCharacteristics.get(
                CameraCharacteristics.CONTROL_AWB_AVAILABLE_MODES),
                CaptureRequest.CONTROL_AWB_MODE_AUTO)) {
            // Allow AWB to run auto-magically if this device supports this
            builder.set(CaptureRequest.CONTROL_AWB_MODE,
                    CaptureRequest.CONTROL_AWB_MODE_AUTO);
        }
    }

    private void closePreviewSession() {
        if (mPreviewSession != null) {
            mPreviewSession.close();
            mPreviewSession = null;
        }
    }

    private void configureTransform(int width, int height) {
        if (mPreviewSize == null || reactApplicationContext == null) {
            return;
        }
        int rotation = reactApplicationContext.getCurrentActivity().getWindowManager().getDefaultDisplay().getRotation();
        Matrix matrix = new Matrix();
        RectF viewRect = new RectF(0, 0, width, height);
        RectF bufferRect = new RectF(0, 0, mPreviewSize.getHeight(), mPreviewSize.getWidth());
        float centerX = viewRect.centerX();
        float centerY = viewRect.centerY();
        if (Surface.ROTATION_90 == rotation || Surface.ROTATION_270 == rotation) {
            bufferRect.offset(centerX - bufferRect.centerX(), centerY - bufferRect.centerY());
            matrix.setRectToRect(viewRect, bufferRect, Matrix.ScaleToFit.FILL);
            float scale = Math.max(
                    (float) height / mPreviewSize.getHeight(),
                    (float) width / mPreviewSize.getWidth());
            matrix.postScale(scale, scale, centerX, centerY);
            matrix.postRotate(90 * (rotation - 2), centerX, centerY);
        } else {
            bufferRect.offset(centerX - bufferRect.centerX(), centerY - bufferRect.centerY());
            matrix.setRectToRect(viewRect, bufferRect, Matrix.ScaleToFit.FILL);
            float scale = Math.max(
                    (float) height / mPreviewSize.getWidth(),
                    (float) width / mPreviewSize.getHeight());
            matrix.postScale(scale, scale, centerX, centerY);
        }
        this.setTransform(matrix);
    }

    CameraManager manager = null;

    private void openCamera(int width, int height) {
        // TODO: Add M permission support
        if (reactApplicationContext == null || (reactApplicationContext.getCurrentActivity() != null && reactApplicationContext.getCurrentActivity().isFinishing())) {
            return;
        }

        if (manager != null) {
            manager = null;
        }
        manager = (CameraManager) reactApplicationContext.getSystemService(Context.CAMERA_SERVICE);

        try {
            if (!mCameraOpenCloseLock.tryAcquire(2500, TimeUnit.MILLISECONDS)) {
                throw new RuntimeException("Time out waiting to lock camera opening.");
            }

            CameraCharacteristics characteristics = null;
            for (String cameraId : manager.getCameraIdList()) {
                characteristics = manager.getCameraCharacteristics(cameraId);
                if (characteristics.get(CameraCharacteristics.LENS_FACING) == mFacing) {
                    mCameraId = cameraId;
                    break;
                }
            }

            StreamConfigurationMap map = characteristics
                    .get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
            mSensorOrientation = characteristics.get(CameraCharacteristics.SENSOR_ORIENTATION);
            mVideoSize = chooseVideoSize(map.getOutputSizes(MediaRecorder.class));
            mPreviewSize = chooseOptimalSize(map.getOutputSizes(SurfaceTexture.class),
                    width, height, mVideoSize);
            int orientation = reactApplicationContext.getResources().getConfiguration().orientation;
            if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
                this.setAspectRatio(mPreviewSize.getWidth(), mPreviewSize.getHeight());
            } else {
                this.setAspectRatio(mPreviewSize.getHeight(), mPreviewSize.getWidth());
            }
            configureTransform(width, height);
            mMediaRecorder = new MediaRecorder();
            if (ActivityCompat.checkSelfPermission(getActivity(), Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                doOnCancel();
                return;
            }
            manager.openCamera(mCameraId, mStateCallback, null);

        } catch (CameraAccessException e) {
            e.printStackTrace();
            onCameraAccessException();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void switchCameraFlash(CaptureRequest.Builder captureBuilder) {
        if (isFlashSupport()) {
            if (isAutoOrOnFlash() && mFacing == CameraCharacteristics.LENS_FACING_BACK && manager != null &&
                    mPreviewBuilder != null && mPreviewSession != null) {
                captureBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER, CameraMetadata.CONTROL_AF_TRIGGER_CANCEL);
                if (isAutoFlashRequired()) {
                    captureBuilder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH);
                } else {
                    captureBuilder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON_ALWAYS_FLASH);
                }
                captureBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER, CameraMetadata.CONTROL_AF_TRIGGER_START);
                captureBuilder.set(CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER, CameraMetadata.CONTROL_AE_PRECAPTURE_TRIGGER_START);
            }
        } else {
            Toast.makeText(getActivity(),"Flash not available",Toast.LENGTH_SHORT);
        }
    }

    public void switchTorch(boolean isFlash) {
        try {
            if (isFlashSupport()) {
            if (isTorchOn() && mFacing == CameraCharacteristics.LENS_FACING_BACK && manager != null &&
                    mPreviewBuilder != null && mPreviewSession != null) {
                    if (!isFlash) {
                        mPreviewBuilder.set(CaptureRequest.FLASH_MODE, CaptureRequest.FLASH_MODE_OFF);
                        mPreviewSession.setRepeatingRequest(mPreviewBuilder.build(), null, null);
                    } else {
                        mPreviewBuilder.set(CaptureRequest.FLASH_MODE, CaptureRequest.FLASH_MODE_TORCH);
                        mPreviewSession.setRepeatingRequest(mPreviewBuilder.build(), null, null);
                    }
            }
            } else {
                Toast.makeText(getActivity(),"Torch not available",Toast.LENGTH_SHORT);
            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
        } catch (IllegalStateException e) {
            e.printStackTrace();
        }
    }

    /**
     * Return true if the given array contains the given integer.
     *
     * @param modes array to check.
     * @param mode  integer to get for.
     * @return true if the array contains the given integer, otherwise false.
     */
    private static boolean contains(int[] modes, int mode) {
        if (modes == null) {
            return false;
        }
        for (int i : modes) {
            if (i == mode) {
                return true;
            }
        }
        return false;
    }

    public boolean isFlashSupport() {
        Boolean available = false;
        try {
            available = manager.getCameraCharacteristics(mCameraId).get(CameraCharacteristics.FLASH_INFO_AVAILABLE);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
        return (available == null ? false : available);
    }

    private Size chooseOptimalSize(Size[] choices, int width, int height, Size aspectRation) {
        List<Size> bigEnough = new ArrayList<Size>();
        int w = aspectRation.getWidth();
        int h = aspectRation.getHeight();
        for (Size option : choices) {
            if (option.getHeight() == option.getWidth() * h / w &&
                    option.getWidth() >= width && option.getHeight() >= height) {
                bigEnough.add(option);
            }
        }

        if (bigEnough.size() > 0) {
            return Collections.min(bigEnough, new CompareSizesByArea());
        } else {
            return choices[0];
        }
    }

    private static Size chooseVideoSize(Size[] choices) {
        for (Size size : choices) {
            if (size.getWidth() == size.getHeight() * 4 / 3 && size.getWidth() <= 1080) {
                return size;
            }
        }
        return choices[choices.length - 1];
    }

    public Camera2RecorderView(Context context, ReactApplicationContext reactApplicationContext) {
        super(context, reactApplicationContext);
        if(permissionsCheck(REQUEST_PERMISSIONS_SET_SURFACE)){
            setSurfaceTextureListener(mSurfaceTextureListener);
        }
    }

    @Override
    void record() {
        if (mCameraDevice == null || !isAvailable() || mPreviewSize == null) {
            return;
        }
        try {
            closePreviewSession();
            setUpMediaRecorder();
            SurfaceTexture texture = getSurfaceTexture();
            assert texture != null;
            texture.setDefaultBufferSize(mPreviewSize.getWidth(), mPreviewSize.getHeight());
            mPreviewBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_RECORD);
            List<Surface> surfaces = new ArrayList<>();

            // Surface for camera preview
            Surface previewSurface = new Surface(texture);
            surfaces.add(previewSurface);
            mPreviewBuilder.addTarget(previewSurface);

            /// Surface for MediaRecorder
            mRecorderSurface = mMediaRecorder.getSurface();
            surfaces.add(mRecorderSurface);
            mPreviewBuilder.addTarget(mRecorderSurface);

            mCameraDevice.createCaptureSession(surfaces, new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(CameraCaptureSession session) {
                    mPreviewSession = session;
                    updatePreview();
                    switchTorch(true);
                    mMediaRecorder.start();
                    mIsRecordingVideo = true;
                    onRecordingStarted();
                    Log.d("MWIT", "Recording started: " + mNextVideoAbsolutePath);
                }

                @Override
                public void onConfigureFailed(CameraCaptureSession session) {
                    Log.e("MWIT", "Recording failed!");
                    onCameraFailed();
                }
            }, mBackgroundHandler);

        } catch (IOException e) {
            e.printStackTrace();
        } catch (CameraAccessException e) {
            e.printStackTrace();
            onCameraAccessException();
        }
    }

    private void setUpMediaRecorder() throws IOException {
        Log.d("MWIT", "Record with frame rate: " + mVideoEncodingFrameRate);
        if (reactApplicationContext == null) {
            return;
        }
        mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE);
        mMediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
        // TODO: filename
        String fileName = String.format("VID_%s.mp4", new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date()));
        mNextVideoAbsolutePath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM) + "/" + fileName;
        mMediaRecorder.setOutputFile(mNextVideoAbsolutePath);
        mMediaRecorder.setVideoEncodingBitRate(mVideoEncodingBitrate);
        mMediaRecorder.setVideoFrameRate(mVideoEncodingFrameRate);
        mMediaRecorder.setVideoSize(mVideoSize.getWidth(), mVideoSize.getHeight());
        mMediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);
        mMediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
        int rotation = reactApplicationContext.getCurrentActivity().getWindowManager().getDefaultDisplay().getRotation();
        switch (mSensorOrientation) {
            case SENSOR_ORIENTATION_DEFAULT_DEGREES:
                mMediaRecorder.setOrientationHint(DEFAULT_ORIENTATIONS.get(rotation));
                break;
            case SENSOR_ORIENTATION_INVERSE_DEGREES:
                mMediaRecorder.setOrientationHint(INVERSE_ORIENTATIONS.get(rotation));
                break;
        }
        mMediaRecorder.prepare();
    }

    @Override
    void stop() {
        if (mIsRecordingVideo) {
            mIsRecordingVideo = false;
            switchTorch(false);
            try {
                mPreviewSession.abortCaptures();
            } catch (CameraAccessException e) {
                e.printStackTrace();
            }
            mMediaRecorder.stop();
            mMediaRecorder.reset();
            onRecordingStopped(mNextVideoAbsolutePath);
            mNextVideoAbsolutePath = null;
            goBack();
            startPreview();
        }
    }

    @Override
    void onResume() {
        startBackgroundThread();
        Log.d("MWIT", "onResume");
        if (isAvailable()) {
            if (isPermissionGranted(getActivity())) {
                openCamera(getWidth(), getHeight());
            } else {
                showMessage();
            }
        } else {
            setSurfaceTextureListener(mSurfaceTextureListener);
        }
    }

    @Override
    void onPause() {
        closeCamera();
        stopBackgroundThread();
    }

    @Override
    void setType(String type) {
        switch (type) {
            case "front":
                mFacing = CameraCharacteristics.LENS_FACING_FRONT;
                break;
            case "back":
            default:
                mFacing = CameraCharacteristics.LENS_FACING_BACK;
        }

        if (mSurfaceTextureIsAvailable && mCameraDevice != null) {
            closeCamera();
            if (isPermissionGranted(getActivity())) {
                openCamera(getWidth(), getHeight());
            } else {
                showMessage();
            }
        }
    }

    @Override
    void setVideoEncodingBitrate(int bitrate) {
        mVideoEncodingBitrate = bitrate;
    }

    @Override
    void setVideoEncodingFrameRate(int frameRate) {
        mVideoEncodingFrameRate = frameRate;
    }

    private void closeCamera() {
        try {
            mCameraOpenCloseLock.acquire();
            closePreviewSession();
            if (mCameraDevice != null) {
                mCameraDevice.close();
                mCameraDevice = null;
            }
            if (mMediaRecorder != null) {
                mMediaRecorder.release();
                mMediaRecorder = null;
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            mCameraOpenCloseLock.release();
        }
    }

    @Override
    boolean isRecording() {
        return mIsRecordingVideo;
    }

    public boolean isAutoFlashRequired() {
        return flashStatus.equals(FLASH_AUTO);
    }

    public boolean isAutoOrOnFlash() {
        return flashStatus.equals(FLASH_ON) || isAutoFlashRequired();
    }

    static class CompareSizesByArea implements Comparator<Size> {

        @Override
        public int compare(Size lhs, Size rhs) {
            return Long.signum((long) lhs.getWidth() * lhs.getHeight() -
                    (long) rhs.getWidth() * rhs.getHeight());
        }
    }

    /* Image Capture */
    @Override
    protected void takePicture() {
        if (null == mCameraDevice) {
            Log.e("takePicture", "cameraDevice is null");
            return;
        }
        CameraManager manager = (CameraManager) reactApplicationContext.getSystemService(Context.CAMERA_SERVICE);
        try {
            CameraCharacteristics characteristics = manager.getCameraCharacteristics(mCameraDevice.getId());
            Size[] jpegSizes = null;
            if (characteristics != null) {
                jpegSizes = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP).getOutputSizes(ImageFormat.JPEG);
            }
            int width = 640;
            int height = 480;
            if (jpegSizes != null && 0 < jpegSizes.length) {
                width = jpegSizes[0].getWidth();
                height = jpegSizes[0].getHeight();
            }
            ImageReader reader = ImageReader.newInstance(width, height, ImageFormat.JPEG, 1);
            List<Surface> outputSurfaces = new ArrayList<Surface>(2);
            outputSurfaces.add(reader.getSurface());
            outputSurfaces.add(new Surface(getSurfaceTexture()));
            final CaptureRequest.Builder captureBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
            captureBuilder.addTarget(reader.getSurface());
            captureBuilder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO);
            // Orientation
            int rotation = reactApplicationContext.getCurrentActivity().getWindowManager().getDefaultDisplay().getRotation();
            captureBuilder.set(CaptureRequest.JPEG_ORIENTATION, DEFAULT_ORIENTATIONS.get(rotation));
            String fileName = String.format("IMG_%s.jpg", new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date()));
            final File file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM) +"/" +fileName);
            ImageReader.OnImageAvailableListener readerListener = new ImageReader.OnImageAvailableListener() {
                @Override
                public void onImageAvailable(ImageReader reader) {
                    Image image = null;
                    try {
                        image = reader.acquireLatestImage();
                        ByteBuffer buffer = image.getPlanes()[0].getBuffer();
                        byte[] bytes = new byte[buffer.capacity()];
                        buffer.get(bytes);
                        save(bytes);
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    } catch (IOException e) {
                        e.printStackTrace();
                    } finally {
                        if (image != null) {
                            image.close();
                        }
                    }
                }

                private void save(byte[] bytes) throws IOException {
                    OutputStream output = null;
                    try {
                        output = new FileOutputStream(file);
                        output.write(bytes);
                    } finally {
                        if (null != output) {
                            output.close();
                        }
                    }
                }
            };

            reader.setOnImageAvailableListener(readerListener, mBackgroundHandler);
            final CameraCaptureSession.CaptureCallback captureListener = new CameraCaptureSession.CaptureCallback() {
                @Override
                public void onCaptureCompleted(CameraCaptureSession session, CaptureRequest request, TotalCaptureResult result) {
                    super.onCaptureCompleted(session, request, result);
                    onCaptureFinished(file.getAbsolutePath());
                    goBack();
                    startPreview();
                }
            };

            mCameraDevice.createCaptureSession(outputSurfaces, new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(CameraCaptureSession session) {
                    try {
                        switchCameraFlash(captureBuilder);
                        // session.capture(captureBuilder.build(), captureListener, mBackgroundHandler);

                        // Finally, we start displaying the camera preview.
                        session.setRepeatingRequest(
                                captureBuilder.build(),
                                captureListener, mBackgroundHandler);
                    } catch (CameraAccessException e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onConfigureFailed(CameraCaptureSession session) {
                }
            }, mBackgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    public void permissionResultActions(int requestCode) {
        switch (requestCode) {
            case REQUEST_PERMISSIONS_IMAGE:

                break;
            case REQUEST_PERMISSIONS_SET_SURFACE:
                setSurfaceTextureListener(mSurfaceTextureListener);
                break;
            case REQUEST_PERMISSIONS_VIDEO:

                break;
            case REQUEST_PERMISSIONS_OPEN:
                if(isPermissionGranted(getActivity()) && getWidth() >= 0 && getHeight() >= 0) {
                    openCamera(getWidth(), getHeight());
                } else {
                    doOnCancel();
                }
                break;
            case REQUEST_PERMISSIONS_OPEN2:
                if(isPermissionGranted(getActivity()) && tempWidth >= 0 && tempHeight >= 0) {
                    openCamera(tempWidth, tempHeight);
                    tempWidth = -1;
                    tempHeight = -1;
                } else {
                    doOnCancel();
                }
                break;

        }
    }

}
