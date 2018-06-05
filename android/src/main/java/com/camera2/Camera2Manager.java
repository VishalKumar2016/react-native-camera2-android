package com.camera2;


import android.os.Build;
import android.util.Log;

import com.facebook.infer.annotation.Assertions;
import com.facebook.react.bridge.LifecycleEventListener;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReadableArray;
import com.facebook.react.common.MapBuilder;
import com.facebook.react.uimanager.SimpleViewManager;
import com.facebook.react.uimanager.ThemedReactContext;
import com.facebook.react.uimanager.annotations.ReactProp;

import java.util.Map;

import javax.annotation.Nullable;

public class Camera2Manager extends SimpleViewManager<Camera2Base> implements LifecycleEventListener {
    private static final int COMMAND_RECORD = 559;
    private static final int COMMAND_STOP = 365;
    private static final int TAKE_PICTURE = 111;
    private ReactApplicationContext mActivity;
    private Camera2Base mView;
    public Camera2Manager(ReactApplicationContext activity) {
        mActivity = activity;
    }

    @Override
    public void onHostResume() {
        mView.onResume();
    }

    @Override
    public void onHostPause() {
        mView.onPause();
    }

    @Override
    public void onHostDestroy() {

    }

    @ReactProp(name = "type")
    public void setType(Camera2Base view, @Nullable String type) {
        view.setType(type);
    }

    @ReactProp(name = "flash")
    public void switchFlash(Camera2Base view, @Nullable String flashStatus) {
        view.setFlashStatus(flashStatus);
    }

    @ReactProp(name = "isVideo")
    public void setCameraState(Camera2Base view, @Nullable String isVideo) {
        view.setCameraState(isVideo.equals("yes"));
    }

    @ReactProp(name = "torch")
    public void switchTorch(Camera2Base view, @Nullable String torchStatus) {
        view.setTorchStatus(torchStatus);
    }

    @ReactProp(name = "videoEncodingBitrate", defaultInt = 7000000)
    public void setVideoEncodingBitrate(Camera2Base view, int bitrate) {
        view.setVideoEncodingBitrate(bitrate);
    }

    @ReactProp(name = "videoEncodingFrameRate", defaultInt = 30)
    public void setVideoEncodingFrameRate(Camera2Base view, int frameRate) {
        view.setVideoEncodingFrameRate(frameRate);
    }

    @Override
    public String getName() {
        return "Camera2";
    }

    @Override
    protected Camera2Base createViewInstance(ThemedReactContext reactContext) {
        Log.d("MWIT", "Started cam thing");
        reactContext.addLifecycleEventListener(this);
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP) {
            mView = new Camera2RecorderView(reactContext, mActivity);
        } else {
            mView = new CameraRecorderView(reactContext);
        }

        return mView;
    }

    @Override
    public @Nullable
    Map getExportedCustomDirectEventTypeConstants() {
        return MapBuilder.of(
                "recordingStart", MapBuilder.of("registrationName", "onRecordingStarted"),
                "imageCaptureFinish", MapBuilder.of("registrationName", "onImageCaptureFinish"),
                "recordingFinish", MapBuilder.of("registrationName", "onRecordingFinished"),
                "cameraAccessException", MapBuilder.of("registrationName", "onCameraAccessException"),
                "cameraFailed", MapBuilder.of("registrationName", "onCameraFailed"),
                "permissionDenied", MapBuilder.of("registrationName", "onPermissionDenied")
        );
    }

    @Override
    public Map<String, Integer> getCommandsMap() {
        return MapBuilder.of(
                "record", COMMAND_RECORD,
                "stop", COMMAND_STOP,
                "image", TAKE_PICTURE
        );
    }

    @Override
    public void receiveCommand(
            Camera2Base view,
            int commandType,
            @Nullable ReadableArray args
    ) {
        Assertions.assertNotNull(view);

        switch (commandType) {
            case COMMAND_RECORD:
                view.record();
                break;
            case COMMAND_STOP:
                view.stop();
                break;
            case TAKE_PICTURE:
                view.takePicture();
                break;
        }
    }
}