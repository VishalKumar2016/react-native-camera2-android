package com.camera2.permission;

import android.app.Activity;
import android.content.DialogInterface;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;

import com.camera2.PermissionViewBase;

/**
 * Created by rusfearuth on 03.03.17.
 */

public class PermissionUtils
{
    public static @Nullable
        // AlertDialog explainingDialog(@NonNull final Camera2RecorderView module,
    AlertDialog explainingDialog(@NonNull final PermissionViewBase module,
                                 @NonNull final OnExplainingPermissionCallback callback)
    {
        if (module.getContext() == null)
        {
            return null;
        }

        final String title = "Permission denied";
        final String text = "Grant Permission to be able to take pictures or video with your camera.";
        final String btnReTryTitle = "Re-try";
        final String btnOkTitle = "I am sure";

        final Activity activity = module.getActivity();

        if (activity == null)
        {
            return null;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(activity, android.R.style.Theme_DeviceDefault_Light_Dialog_Alert);
        builder
                .setTitle(title)
                .setMessage(text)
                .setCancelable(false)
                .setNegativeButton(btnOkTitle, new DialogInterface.OnClickListener()
                {
                    @Override
                    public void onClick(final DialogInterface dialogInterface,
                                        int i)
                    {
                        callback.onCancel(dialogInterface);
                    }
                })
                .setPositiveButton(btnReTryTitle, new DialogInterface.OnClickListener()
                {
                    @Override
                    public void onClick(DialogInterface dialogInterface,
                                        int i)
                    {
                        callback.onReTry(dialogInterface);
                    }
                });

        return builder.create();
    }

    public interface OnExplainingPermissionCallback {
        void onCancel(DialogInterface dialogInterface);
        void onReTry(DialogInterface dialogInterface);
    }
}