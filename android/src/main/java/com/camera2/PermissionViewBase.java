package com.camera2;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.widget.Toast;

import com.facebook.react.ReactActivity;
import com.facebook.react.bridge.ActivityEventListener;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.modules.core.PermissionListener;
import com.camera2.permission.OnImagePickerPermissionsCallback;
import com.camera2.permission.PermissionUtils;

import java.lang.annotation.Target;


/**
 * Created by Vishal on 12/09/17.
 */
@TargetApi(Build.VERSION_CODES.M)
public abstract class PermissionViewBase extends Camera2Base implements ActivityEventListener {

    protected ReactApplicationContext reactApplicationContext;

    public PermissionViewBase(Context context, ReactApplicationContext reactApplicationContext) {
        super(context);
        this.reactApplicationContext = reactApplicationContext;
        this.reactApplicationContext.addActivityEventListener(this);
    }

    public static final int REQUEST_PERMISSIONS_IMAGE = 14000;
    public static final int REQUEST_PERMISSIONS_VIDEO = 14001;
    public static final int REQUEST_PERMISSIONS_OPEN = 14003;
    public static final int REQUEST_PERMISSIONS_OPEN2 = 14004;
    public static final int REQUEST_PERMISSIONS_SET_SURFACE = 14005;


    public Activity getActivity() {
        return reactApplicationContext.getCurrentActivity();
    }

    // private final int dialogThemeId = R.style.defaultExplainingPermissionsTheme;
    private PermissionListener listener = new PermissionListener() {
        public boolean onRequestPermissionsResult(final int requestCode,
                                                  @NonNull final String[] permissions,
                                                  @NonNull final int[] grantResults) {
            boolean permissionsGranted = true;
            for (int i = 0; i < permissions.length; i++) {
                final boolean granted = grantResults[i] == PackageManager.PERMISSION_GRANTED;
                permissionsGranted = permissionsGranted && granted;
            }

            if (!permissionsGranted) {
                //  responseHelper.invokeError(callback, "Permissions weren't granted");
                onPermissionDenied();
                return false;
            }
            permissionResultActions(requestCode);
            return true;
        }
    };


    protected boolean isPermissionGranted(Activity activity){
        final int writePermission = ActivityCompat
                .checkSelfPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE);
        final int readPermission = ActivityCompat
                .checkSelfPermission(activity, Manifest.permission.READ_EXTERNAL_STORAGE);
        final int recordAudioPermission = ActivityCompat
                .checkSelfPermission(activity, Manifest.permission.RECORD_AUDIO);
        final int cameraPermission = ActivityCompat
                .checkSelfPermission(activity, Manifest.permission.CAMERA);

        final boolean permissionsGrated =
                writePermission == PackageManager.PERMISSION_GRANTED &&
                        readPermission == PackageManager.PERMISSION_GRANTED &&
                        recordAudioPermission == PackageManager.PERMISSION_GRANTED &&
                        cameraPermission == PackageManager.PERMISSION_GRANTED;

        return permissionsGrated;
    }

    protected boolean permissionsCheck(@NonNull final int requestPermission) {
        if (reactApplicationContext.getCurrentActivity() == null) {
            throw new NullPointerException("Activity can't be null");
        }
        final Activity activity = reactApplicationContext.getCurrentActivity();


//        this.callback = callback;
//        this.options = options;

        if (!isPermissionGranted(activity)) {
            final Boolean dontAskAgain =
                    ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE) &&
                            ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.READ_EXTERNAL_STORAGE) &&
                            ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.RECORD_AUDIO) &&
                            ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.CAMERA);

            if (dontAskAgain) {
                final AlertDialog dialog = PermissionUtils
                        .explainingDialog(this, new PermissionUtils.OnExplainingPermissionCallback() {
                            @Override
                            public void onCancel(DialogInterface dialogInterface) {
                                onPermissionDenied();
                            }

                            @Override
                            public void onReTry(DialogInterface dialogInterface) {

                                Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                                Uri uri = Uri.fromParts("package", activity.getPackageName(), null);
                                intent.setData(uri);
                                if (activity == null) {
                                    return;
                                }
                                activity.startActivityForResult(intent, 1);
                            }
                        });
                dialog.show();
                return false;
            } else {
                String[] PERMISSIONS = {Manifest.permission.WRITE_EXTERNAL_STORAGE,
                        Manifest.permission.READ_EXTERNAL_STORAGE,
                        Manifest.permission.RECORD_AUDIO,
                        Manifest.permission.CAMERA};
                if (activity instanceof ReactActivity) {
                    ((ReactActivity) activity).requestPermissions(PERMISSIONS, requestPermission, listener);
                } else if (activity instanceof OnImagePickerPermissionsCallback) {
                    ((OnImagePickerPermissionsCallback) activity).setPermissionListener(listener);
                    ActivityCompat.requestPermissions(activity, PERMISSIONS, requestPermission);
                } else {
                    final String errorDescription = new StringBuilder(activity.getClass().getSimpleName())
                            .append(" must implement ")
                            .append(OnImagePickerPermissionsCallback.class.getSimpleName())
                            .toString();
                    throw new UnsupportedOperationException(errorDescription);
                }
                return false;
            }
        }
        return true;
    }

    @Override
    public void onActivityResult(Activity activity, int requestCode, int resultCode, Intent data) {
        if(!isPermissionGranted(getActivity())){
            onPermissionDenied();
        }
    }

    @Override
    public void onNewIntent(Intent intent) {

    }

    public void onPermissionDenied() {
        super.onPermissionDenied("You have to grant permissions to use camera");
    }

    protected void goBack() {
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                getActivity().onBackPressed();
            }
        });
    }

    protected void doOnCancel() {
        onPermissionDenied();
        goBack();
    }

    abstract void permissionResultActions(int requestCode);
}
