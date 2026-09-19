package com.veytrix.autopilot;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.io.File;

/** Native OTA discovery, digest verification and user-approved package installation. */
public final class VeytrixUpdateView extends LinearLayout {
    public interface Host {
        void openDrawer();
    }

    private final VeytrixUpdateClient client;
    private final VeytrixApkInstaller installer;
    private final TextView installed;
    private final TextView status;
    private final TextView release;
    private final Button sourceButton;
    private final Button downloadButton;
    private final Button installButton;
    private VeytrixUpdateClient.UpdateInfo updateInfo;
    private File downloadedApk;

    private final BroadcastReceiver installReceiver = new BroadcastReceiver() {
        @Override public void onReceive(Context context, Intent intent) {
            int installerStatus = intent.getIntExtra(
                    VeytrixInstallResultReceiver.EXTRA_STATUS,
                    PackageManager.COMPONENT_ENABLED_STATE_DEFAULT);
            String message = intent.getStringExtra(
                    VeytrixInstallResultReceiver.EXTRA_MESSAGE);
            if (installerStatus == android.content.pm.PackageInstaller.STATUS_SUCCESS) {
                status.setText("UPDATE INSTALLED. ANDROID MAY RESTART VEYTRIX.");
                status.setTextColor(VeytrixDesignTokens.STATE_VERIFIED);
            } else {
                status.setText("UPDATE INSTALL FAILED" +
                        (message == null || message.isEmpty() ? "" : ": " + message));
                status.setTextColor(VeytrixDesignTokens.STATE_FAILED);
            }
            installButton.setEnabled(false);
        }
    };

    public VeytrixUpdateView(Context context, VeytrixUpdateClient client, Host host) {
        super(context);
        this.client = client;
        this.installer = new VeytrixApkInstaller(context);
        setOrientation(VERTICAL);
        setPadding(dp(14), dp(8), dp(14), dp(10));
        setBackgroundColor(VeytrixDesignTokens.PEARL);

        LinearLayout header = new LinearLayout(context);
        header.setGravity(Gravity.CENTER_VERTICAL);
        TextView menu = text("☰", 21, VeytrixDesignTokens.TEXT_PRIMARY, true);
        menu.setGravity(Gravity.CENTER);
        menu.setOnClickListener(v -> host.openDrawer());
        header.addView(menu, new LayoutParams(dp(48), dp(48)));
        TextView title = text("UPDATES", 21, VeytrixDesignTokens.TEXT_PRIMARY, true);
        title.setPadding(dp(8), 0, 0, 0);
        header.addView(title, new LayoutParams(0, dp(48), 1f));
        addView(header, new LayoutParams(-1, dp(48)));

        installed = text("", 11, VeytrixDesignTokens.TEXT_SECONDARY, true);
        addView(installed, new LayoutParams(-1, dp(34)));

        status = text("CHECKING LATEST PUBLISHED RELEASE…", 11,
                VeytrixDesignTokens.VIOLET, true);
        status.setBackground(round(VeytrixDesignTokens.WHITE, 16, VeytrixDesignTokens.SILVER));
        status.setPadding(dp(12), dp(8), dp(12), dp(8));
        addView(status, new LayoutParams(-1, dp(58)));

        release = text("", 12, VeytrixDesignTokens.TEXT_PRIMARY, false);
        release.setBackground(round(VeytrixDesignTokens.WHITE, 16, VeytrixDesignTokens.SILVER));
        release.setPadding(dp(12), dp(8), dp(12), dp(8));
        addView(release, marginBottom(8));

        sourceButton = secondaryButton("ALLOW VEYTRIX TO INSTALL UPDATES");
        sourceButton.setOnClickListener(v -> openInstallSourceSettings());
        addView(sourceButton, marginBottom(8));

        downloadButton = primaryButton("DOWNLOAD & VERIFY RELEASE");
        downloadButton.setEnabled(false);
        downloadButton.setOnClickListener(v -> downloadUpdate());
        addView(downloadButton, marginBottom(8));

        installButton = secondaryButton("INSTALL VERIFIED UPDATE");
        installButton.setEnabled(false);
        installButton.setOnClickListener(v -> installUpdate());
        addView(installButton, marginBottom(8));

        TextView note = text(
                "Only a GitHub release APK with a published SHA-256 digest and a signing certificate compatible with the installed VEYTRIX package can be staged. Installation is handed to Android PackageInstaller for user approval.",
                10, VeytrixDesignTokens.TEXT_SECONDARY, false);
        note.setGravity(Gravity.CENTER);
        addView(note, marginBottom(8));

        View spacer = new View(context);
        addView(spacer, new LayoutParams(-1, 0, 1f));

        refreshInstalledVersion();
        refreshInstallPermission();
        registerReceiver();
        checkForUpdate();
    }

    private void refreshInstalledVersion() {
        try {
            PackageInfo info = getContext().getPackageManager().getPackageInfo(
                    getContext().getPackageName(), 0);
            long code = Build.VERSION.SDK_INT >= 28
                    ? info.getLongVersionCode() : info.versionCode;
            installed.setText("INSTALLED: " + info.versionName + "  •  VERSION CODE " + code);
        } catch (Exception error) {
            installed.setText("INSTALLED VERSION: UNAVAILABLE");
        }
    }

    private void refreshInstallPermission() {
        if (Build.VERSION.SDK_INT >= 26) {
            boolean allowed = getContext().getPackageManager().canRequestPackageInstalls();
            sourceButton.setVisibility(allowed ? GONE : VISIBLE);
        } else {
            sourceButton.setVisibility(GONE);
        }
    }

    private void checkForUpdate() {
        status.setText("CHECKING LATEST PUBLISHED RELEASE…");
        client.checkLatest(new VeytrixUpdateClient.Callback<VeytrixUpdateClient.UpdateInfo>() {
            @Override public void onSuccess(VeytrixUpdateClient.UpdateInfo value) {
                updateInfo = value;
                release.setText("LATEST RELEASE: " + value.releaseName +
                        "\nTAG: " + value.tag +
                        "\nSHA-256: " + value.sha256);
                status.setText("RELEASE FOUND — VERIFY BEFORE INSTALL");
                status.setTextColor(VeytrixDesignTokens.VIOLET);
                downloadButton.setEnabled(true);
            }

            @Override public void onError(String message) {
                updateInfo = null;
                release.setText("NO VERIFIED RELEASE DATA AVAILABLE");
                status.setText("UPDATE CHECK FAILED: " + message);
                status.setTextColor(VeytrixDesignTokens.STATE_FAILED);
                downloadButton.setEnabled(false);
            }
        });
    }

    private void downloadUpdate() {
        if (updateInfo == null) return;
        downloadButton.setEnabled(false);
        status.setText("DOWNLOADING RELEASE APK…");
        client.download(updateInfo, new VeytrixUpdateClient.Callback<File>() {
            @Override public void onSuccess(File value) {
                downloadedApk = value;
                status.setText("APK DOWNLOADED AND SHA-256 VERIFIED");
                status.setTextColor(VeytrixDesignTokens.STATE_VERIFIED);
                installButton.setEnabled(true);
                refreshInstallPermission();
            }

            @Override public void onError(String message) {
                downloadedApk = null;
                status.setText("DOWNLOAD VERIFICATION FAILED: " + message);
                status.setTextColor(VeytrixDesignTokens.STATE_FAILED);
                downloadButton.setEnabled(true);
            }
        });
    }

    private void installUpdate() {
        if (downloadedApk == null || updateInfo == null) return;
        if (Build.VERSION.SDK_INT >= 26 &&
                !getContext().getPackageManager().canRequestPackageInstalls()) {
            status.setText("ALLOW VEYTRIX TO INSTALL FROM THIS SOURCE FIRST");
            status.setTextColor(VeytrixDesignTokens.STATE_FAILED);
            openInstallSourceSettings();
            return;
        }
        installButton.setEnabled(false);
        status.setText("STAGING UPDATE WITH ANDROID PACKAGEINSTALLER…");
        installer.install(downloadedApk, updateInfo.sha256, new VeytrixApkInstaller.Callback() {
            @Override public void onSuccess(String message) {
                status.setText("INSTALL REQUEST SUBMITTED — ANDROID WILL SHOW APPROVAL IF REQUIRED");
                status.setTextColor(VeytrixDesignTokens.VIOLET);
            }
            @Override public void onError(String message) {
                status.setText("INSTALL PREFLIGHT FAILED: " + message);
                status.setTextColor(VeytrixDesignTokens.STATE_FAILED);
                installButton.setEnabled(true);
            }
        });
    }

    private void openInstallSourceSettings() {
        if (!(getContext() instanceof Activity)) return;
        try {
            Intent intent = new Intent(
                    Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                    Uri.parse("package:" + getContext().getPackageName()));
            getContext().startActivity(intent);
        } catch (Exception error) {
            Intent fallback = new Intent(Settings.ACTION_SECURITY_SETTINGS);
            getContext().startActivity(fallback);
        }
    }

    private void registerReceiver() {
        Activity activity = (Activity) getContext();
        IntentFilter filter = new IntentFilter(
                VeytrixInstallResultReceiver.ACTION_INSTALL_RESULT);
        if (Build.VERSION.SDK_INT >= 33) {
            activity.registerReceiver(installReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
        } else {
            activity.registerReceiver(installReceiver, filter);
        }
    }

    @Override protected void onDetachedFromWindow() {
        try { ((Activity) getContext()).unregisterReceiver(installReceiver); }
        catch (Exception ignored) {}
        installer.shutdown();
        client.shutdown();
        super.onDetachedFromWindow();
    }

    private Button primaryButton(String value) {
        Button button = new Button(getContext());
        button.setText(value);
        button.setTextSize(11);
        button.setTextColor(VeytrixDesignTokens.WHITE);
        button.setTypeface(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD);
        button.setAllCaps(false);
        button.setBackground(round(VeytrixDesignTokens.PURPLE, dp(16), VeytrixDesignTokens.PURPLE));
        button.setMinHeight(dp(50));
        return button;
    }

    private Button secondaryButton(String value) {
        Button button = new Button(getContext());
        button.setText(value);
        button.setTextSize(10);
        button.setTextColor(VeytrixDesignTokens.VIOLET);
        button.setTypeface(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD);
        button.setAllCaps(false);
        button.setBackground(round(VeytrixDesignTokens.WHITE, dp(16), VeytrixDesignTokens.SILVER));
        button.setMinHeight(dp(48));
        return button;
    }

    private TextView text(String value, int size, int color, boolean bold) {
        TextView output = new TextView(getContext());
        output.setText(value);
        output.setTextSize(size);
        output.setTextColor(color);
        output.setTypeface(android.graphics.Typeface.DEFAULT, bold
                ? android.graphics.Typeface.BOLD : android.graphics.Typeface.NORMAL);
        output.setGravity(Gravity.CENTER_VERTICAL);
        return output;
    }

    private LayoutParams marginBottom(int px) {
        LayoutParams params = new LayoutParams(-1, -2);
        params.bottomMargin = dp(px);
        return params;
    }

    private android.graphics.drawable.GradientDrawable round(int color, int radius, int stroke) {
        android.graphics.drawable.GradientDrawable drawable =
                new android.graphics.drawable.GradientDrawable();
        drawable.setColor(color);
        drawable.setCornerRadius(radius);
        if (stroke != Color.TRANSPARENT) drawable.setStroke(dp(1), stroke);
        return drawable;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}