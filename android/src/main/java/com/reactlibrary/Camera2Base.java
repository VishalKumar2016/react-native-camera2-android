package com.reactlibrary;


import android.content.Context;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.ReactContext;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.uimanager.events.RCTEventEmitter;


abstract class Camera2Base extends AutoFitTextureView {


    protected static final String FLASH_ON = "on";
    protected static final String FLASH_AUTO = "auto";
    protected static final String TORCH_ON = "torch_on";
    protected static final String TORCH_OFF = "torch_off";

    protected String flashStatus = FLASH_AUTO;
    protected String torchStatus = TORCH_OFF;

    public Camera2Base(Context context) {
        super(context);
    }

    abstract void takePicture();
    abstract void record();
    abstract void stop();
    abstract void onResume();
    abstract void onPause();
    abstract void setCameraState(boolean isVideo);
    abstract void switchTorch(boolean isFlash);

    abstract void setType(String type);
    abstract void setVideoEncodingBitrate(int bitrate);
    abstract void setVideoEncodingFrameRate(int frameRate);

    abstract boolean isRecording();

    public void setFlashStatus(String flashStatus){
        this.flashStatus = flashStatus;
    }

    public void setTorchStatus(String torchStatus){
        this.torchStatus = torchStatus;
    }

    public boolean isTorchOn(){
        return torchStatus.equals(TORCH_ON);
    }

    public void onRecordingStarted() {
        WritableMap event = Arguments.createMap();
        event.putBoolean("ok", true);
        ReactContext reactContext = (ReactContext) getContext();
        reactContext.getJSModule(RCTEventEmitter.class).receiveEvent(
                getId(),
                "recordingStart",
                event
        );
    }

    public void onRecordingStopped(String path) {
        WritableMap event = Arguments.createMap();
        event.putString("file", path);
        event.putString("file_type", "video");
        ReactContext reactContext = (ReactContext) getContext();
        reactContext.getJSModule(RCTEventEmitter.class).receiveEvent(
                getId(),
                "recordingFinish",
                event
        );
    }

    public void onCaptureFinished(String path) {
        WritableMap event = Arguments.createMap();
        event.putString("file", path);
        event.putString("file_type", "image");
        ReactContext reactContext = (ReactContext) getContext();
        reactContext.getJSModule(RCTEventEmitter.class).receiveEvent(
                getId(),
                "imageCaptureFinish",
                event
        );
    }

    public void onCameraAccessException() {
        WritableMap event = Arguments.createMap();
        ReactContext reactContext = (ReactContext) getContext();
        reactContext.getJSModule(RCTEventEmitter.class).receiveEvent(
                getId(),
                "cameraAccessException",
                event
        );
    }

    public void onCameraFailed() {
        WritableMap event = Arguments.createMap();
        ReactContext reactContext = (ReactContext) getContext();
        reactContext.getJSModule(RCTEventEmitter.class).receiveEvent(
                getId(),
                "cameraFailed",
                event
        );
    }

    public void onPermissionDenied(String message) {
        WritableMap event = Arguments.createMap();
        event.putString("message", message);
        ReactContext reactContext = (ReactContext) getContext();
        reactContext.getJSModule(RCTEventEmitter.class).receiveEvent(
                getId(),
                "permissionDenied",
                event
        );
    }


}
