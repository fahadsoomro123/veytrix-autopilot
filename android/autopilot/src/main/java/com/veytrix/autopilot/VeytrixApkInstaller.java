package com.veytrix.autopilot;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Preflights and stages a signed APK through Android PackageInstaller. */
public final class VeytrixApkInstaller {
    private final Context context;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler main = new Handler(Looper.getMainLooper());

    public VeytrixApkInstaller(Context context) {
        this.context = context.getApplicationContext();
    }

    public void install(File apk, String expectedSha256, Callback callback) {
        executor.execute(() -> {
            try {
                validatePackage(apk, expectedSha256);
                PackageManager packageManager = context.getPackageManager();
                android.content.pm.PackageInstaller installer = packageManager.getPackageInstaller();
                android.content.pm.PackageInstaller.SessionParams params =
                        new android.content.pm.PackageInstaller.SessionParams(
                                android.content.pm.PackageInstaller.SessionParams.MODE_FULL_INSTALL);
                params.setAppPackageName(context.getPackageName());
                params.setSize(apk.length());
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    params.setRequireUserAction(
                            android.content.pm.PackageInstaller.SessionParams.USER_ACTION_REQUIRED);
                }

                int sessionId = installer.createSession(params);
                android.content.pm.PackageInstaller.Session session = installer.openSession(sessionId);
                try {
                    try (BufferedInputStream input = new BufferedInputStream(new FileInputStream(apk))) {
                        java.io.OutputStream output = session.openWrite("base.apk", 0, apk.length());
                        try {
                            byte[] buffer = new byte[64 * 1024];
                            int read;
                            while ((read = input.read(buffer)) != -1) {
                                output.write(buffer, 0, read);
                            }
                            session.fsync(output);
                        } finally {
                            output.close();
                        }
                    }
                    Intent intent = new Intent(context, VeytrixInstallResultReceiver.class);
                    intent.setAction(VeytrixInstallResultReceiver.ACTION_INSTALL_RESULT);
                    android.app.PendingIntent pendingIntent = android.app.PendingIntent.getBroadcast(
                            context, sessionId, intent,
                            android.app.PendingIntent.FLAG_UPDATE_CURRENT |
                                    android.app.PendingIntent.FLAG_IMMUTABLE);
                    session.commit(pendingIntent.getIntentSender());
                } finally {
                    session.close();
                }
                main.post(() -> callback.onSuccess(
                        "Android PackageInstaller session " + sessionId + " submitted."));
            } catch (Exception error) {
                String message = safeMessage(error);
                main.post(() -> callback.onError(message));
            }
        });
    }

    public void shutdown() {
        executor.shutdownNow();
    }

    private void validatePackage(File apk, String expectedSha256) throws Exception {
        if (apk == null || !apk.isFile() || apk.length() == 0) {
            throw new IOException("Update APK is missing");
        }
        String actualDigest = sha256(apk);
        if (expectedSha256 == null || !actualDigest.equalsIgnoreCase(expectedSha256)) {
            throw new SecurityException("Update APK SHA-256 verification failed");
        }

        PackageManager packageManager = context.getPackageManager();
        int signingFlags = Build.VERSION.SDK_INT >= 28
                ? PackageManager.GET_SIGNING_CERTIFICATES
                : PackageManager.GET_SIGNATURES;
        PackageInfo current = packageManager.getPackageInfo(
                context.getPackageName(), signingFlags);
        PackageInfo update = packageManager.getPackageArchiveInfo(
                apk.getAbsolutePath(), signingFlags);
        if (update == null || !context.getPackageName().equals(update.packageName)) {
            throw new SecurityException("Update package identity mismatch");
        }

        long currentVersion = versionCode(current);
        long updateVersion = versionCode(update);
        if (updateVersion <= currentVersion) {
            throw new IllegalStateException(
                    "Update version must be newer than installed version");
        }
        if (!sameSigner(current, update)) {
            throw new SecurityException(
                    "Update APK signing certificate does not match this installation");
        }
    }

    private boolean sameSigner(PackageInfo current, PackageInfo update) throws Exception {
        if (Build.VERSION.SDK_INT >= 28) {
            if (current.signingInfo == null || update.signingInfo == null) return false;
            Signature[] installed = current.signingInfo.getApkContentsSigners();
            Signature[] candidateHistory = update.signingInfo.getSigningCertificateHistory();
            for (Signature installedSigner : installed) {
                for (Signature candidate : candidateHistory) {
                    if (Arrays.equals(certificateDigest(installedSigner), certificateDigest(candidate))) {
                        return true;
                    }
                }
            }
            return false;
        }
        if (current.signatures == null || update.signatures == null) return false;
        for (Signature installedSigner : current.signatures) {
            for (Signature candidate : update.signatures) {
                if (Arrays.equals(certificateDigest(installedSigner), certificateDigest(candidate))) {
                    return true;
                }
            }
        }
        return false;
    }

    private byte[] certificateDigest(Signature signature) throws Exception {
        return MessageDigest.getInstance("SHA-256").digest(signature.toByteArray());
    }

    private long versionCode(PackageInfo info) {
        if (Build.VERSION.SDK_INT >= 28) return info.getLongVersionCode();
        return info.versionCode;
    }

    private String sha256(File file) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        try (BufferedInputStream input = new BufferedInputStream(new FileInputStream(file))) {
            byte[] buffer = new byte[64 * 1024];
            int read;
            while ((read = input.read(buffer)) != -1) digest.update(buffer, 0, read);
        }
        StringBuilder output = new StringBuilder(64);
        for (byte value : digest.digest()) output.append(String.format("%02x", value & 0xff));
        return output.toString();
    }

    private String safeMessage(Exception error) {
        String message = error.getMessage();
        if (message == null || message.isEmpty()) return error.getClass().getSimpleName();
        return message.length() > 240 ? message.substring(0, 240) : message;
    }

    public interface Callback {
        void onSuccess(String message);
        void onError(String message);
    }
}