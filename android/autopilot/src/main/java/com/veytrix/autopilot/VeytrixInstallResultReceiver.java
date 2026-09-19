package com.veytrix.autopilot;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInstaller;

/** Receives Android PackageInstaller results and forwards them to the update surface. */
public final class VeytrixInstallResultReceiver extends BroadcastReceiver {
    public static final String ACTION_INSTALL_RESULT =
            "com.veytrix.autopilot.ACTION_INSTALL_RESULT";
    public static final String EXTRA_STATUS = "status";
    public static final String EXTRA_MESSAGE = "message";

    @Override
    public void onReceive(Context context, Intent intent) {
        int status = intent.getIntExtra(PackageInstaller.EXTRA_STATUS,
                PackageInstaller.STATUS_FAILURE);

        if (status == PackageInstaller.STATUS_PENDING_USER_ACTION) {
            Object raw = intent.getExtras() == null
                    ? null
                    : intent.getExtras().get(PackageInstaller.EXTRA_INTENT);
            if (raw instanceof Intent) {
                Intent confirmation = (Intent) raw;
                confirmation.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(confirmation);
            }
            return;
        }

        String message = intent.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE);
        Intent result = new Intent(ACTION_INSTALL_RESULT);
        result.setPackage(context.getPackageName());
        result.putExtra(EXTRA_STATUS, status);
        result.putExtra(EXTRA_MESSAGE, message == null ? "" : message);
        context.sendBroadcast(result);
    }

}