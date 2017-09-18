package com.reactlibrary.permission;

import android.support.annotation.NonNull;

import com.facebook.react.modules.core.PermissionListener;

public interface OnImagePickerPermissionsCallback
{
     void setPermissionListener(@NonNull PermissionListener listener);
}
