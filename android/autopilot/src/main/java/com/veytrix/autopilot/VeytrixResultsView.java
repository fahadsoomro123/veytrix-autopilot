package com.veytrix.autopilot;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.List;

/**
 * Phase 2 Results surface.
 *
 * It resolves the newest real Autopilot run and then displays only artifacts
 * actually returned by GitHub for that run.
 */
public final class VeytrixResultsView extends LinearLayout {
    public interface Host {
        void openConnection();
    }

    private final VeytrixAutopilotClient client;
    private final TextView summary;
    private final LinearLayout artifactRail;

    public VeytrixResultsView(Context context, VeytrixAutopilotClient client, Host host) {
        super(context);
        this.client = client;

        setOrientation(VERTICAL);
        setPadding(dp(14), dp(8), dp(14), dp(8));
        setBackgroundColor(VeytrixDesignTokens.PEARL);

        LinearLayout header = new LinearLayout(context);
        header.setGravity(Gravity.CENTER_VERTICAL);

        LinearLayout titleCol = new LinearLayout(context);
        titleCol.setOrientation(VERTICAL);
        titleCol.addView(label(
                "RESULTS",
                22,
                VeytrixDesignTokens.TEXT_PRIMARY,
                true
        ));
        titleCol.addView(label(
                "Verified outputs from the real control plane",
                11,
                VeytrixDesignTokens.TEXT_SECONDARY,
                false
        ));
        header.addView(titleCol, new LayoutParams(0, dp(48), 1f));

        TextView connect = label(
                client.hasToken() ? "CONNECTED" : "CONNECT",
                10,
                client.hasToken()
                        ? VeytrixDesignTokens.STATE_VERIFIED
                        : VeytrixDesignTokens.VIOLET,
                true
        );
        connect.setGravity(Gravity.CENTER);
        connect.setMinWidth(dp(96));
        connect.setMinHeight(dp(48));
        connect.setBackground(round(
                VeytrixDesignTokens.WHITE,
                dp(14),
                VeytrixDesignTokens.SILVER
        ));
        connect.setOnClickListener(v -> {
            if (!client.hasToken()) {
                host.openConnection();
            }
        });
        header.addView(connect, new LayoutParams(dp(104), dp(48)));
        addView(header, new LayoutParams(
                LayoutParams.MATCH_PARENT,
                dp(48)
        ));

        summary = label(
                "Resolving latest run…",
                11,
                VeytrixDesignTokens.TEXT_SECONDARY,
                false
        );
        summary.setGravity(Gravity.CENTER_VERTICAL);
        LayoutParams summaryLp = new LayoutParams(
                LayoutParams.MATCH_PARENT, dp(38)
        );
        summaryLp.topMargin = dp(6);
        addView(summary, summaryLp);

        artifactRail = new LinearLayout(context);
        artifactRail.setOrientation(VERTICAL);
        LayoutParams railLp = new LayoutParams(
                LayoutParams.MATCH_PARENT, 0, 1f
        );
        railLp.topMargin = dp(5);
        addView(artifactRail, railLp);

        loadLatestArtifacts();
    }

    private void loadLatestArtifacts() {
        if (!client.hasToken()) {
            summary.setText("No GitHub connection. Connect to inspect real artifacts.");
            addEmpty("CONNECT GITHUB TO VIEW VERIFIED OUTPUTS");
            return;
        }

        client.fetchRecentRuns(1, new VeytrixAutopilotClient.SimpleCallback<List<VeytrixAutopilotClient.RunInfo>>() {
            @Override
            public void onSuccess(List<VeytrixAutopilotClient.RunInfo> runs) {
                if (runs.isEmpty()) {
                    summary.setText("No Autopilot workflow runs have been recorded.");
                    addEmpty("NO VERIFIED RUNS FOUND");
                    return;
                }

                VeytrixAutopilotClient.RunInfo latest = runs.get(0);
                summary.setText(
                        "LATEST RUN #" + latest.number + "  ·  "
                                + (latest.isFinished()
                                ? safe(latest.conclusion).toUpperCase()
                                : safe(latest.status).toUpperCase())
                );

                client.fetchArtifacts(
                        latest.id,
                        8,
                        new VeytrixAutopilotClient.SimpleCallback<List<VeytrixAutopilotClient.ArtifactInfo>>() {
                            @Override
                            public void onSuccess(List<VeytrixAutopilotClient.ArtifactInfo> artifacts) {
                                artifactRail.removeAllViews();
                                if (artifacts.isEmpty()) {
                                    addEmpty("LATEST RUN HAS NO ARTIFACTS");
                                    return;
                                }

                                for (VeytrixAutopilotClient.ArtifactInfo artifact : artifacts) {
                                    addArtifact(artifact);
                                }
                            }

                            @Override
                            public void onError(String message) {
                                addEmpty("ARTIFACT LOOKUP FAILED: " + message.toUpperCase());
                            }
                        }
                );
            }

            @Override
            public void onError(String message) {
                addEmpty("RUN LOOKUP FAILED: " + message.toUpperCase());
            }
        });
    }

    private void addArtifact(VeytrixAutopilotClient.ArtifactInfo artifact) {
        LinearLayout row = new LinearLayout(getContext());
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(12), dp(6), dp(12), dp(6));
        row.setBackground(round(
                VeytrixDesignTokens.WHITE,
                dp(15),
                VeytrixDesignTokens.SILVER
        ));

        TextView marker = label(
                artifact.expired ? "!" : "◆",
                13,
                artifact.expired
                        ? VeytrixDesignTokens.STATE_FAILED
                        : VeytrixDesignTokens.STATE_VERIFIED,
                true
        );
        marker.setGravity(Gravity.CENTER);
        row.addView(marker, new LayoutParams(dp(28), dp(48)));

        LinearLayout copy = new LinearLayout(getContext());
        copy.setOrientation(VERTICAL);
        copy.addView(label(
                artifact.name.isEmpty() ? "Unnamed artifact" : artifact.name,
                12,
                VeytrixDesignTokens.TEXT_PRIMARY,
                true
        ), new LayoutParams(
                LayoutParams.MATCH_PARENT, dp(22)
        ));
        copy.addView(label(
                artifact.expired
                        ? "EXPIRED ON GITHUB"
                        : formatBytes(artifact.sizeBytes),
                10,
                artifact.expired
                        ? VeytrixDesignTokens.STATE_FAILED
                        : VeytrixDesignTokens.TEXT_SECONDARY,
                true
        ), new LayoutParams(
                LayoutParams.MATCH_PARENT, dp(20)
        ));

        row.addView(copy, new LayoutParams(
                0, dp(48), 1f
        ));

        TextView id = label(
                "ID " + artifact.id,
                8,
                VeytrixDesignTokens.TEXT_TERTIARY,
                false
        );
        id.setGravity(Gravity.CENTER_VERTICAL | Gravity.RIGHT);
        row.addView(id, new LayoutParams(
                dp(62), dp(48)
        ));

        LayoutParams rowLp = new LayoutParams(
                LayoutParams.MATCH_PARENT, dp(62)
        );
        rowLp.bottomMargin = dp(7);
        artifactRail.addView(row, rowLp);
    }

    private void addEmpty(String message) {
        artifactRail.removeAllViews();
        TextView empty = label(
                message,
                11,
                VeytrixDesignTokens.TEXT_SECONDARY,
                true
        );
        empty.setGravity(Gravity.CENTER);
        empty.setBackground(round(
                VeytrixDesignTokens.WHITE,
                dp(18),
                VeytrixDesignTokens.SILVER
        ));
        artifactRail.addView(empty, new LayoutParams(
                LayoutParams.MATCH_PARENT, dp(104)
        ));
    }

    private String formatBytes(long bytes) {
        if (bytes < 1024L) {
            return bytes + " B";
        }
        if (bytes < 1024L * 1024L) {
            return String.format(java.util.Locale.US, "%.1f KB", bytes / 1024.0);
        }
        if (bytes < 1024L * 1024L * 1024L) {
            return String.format(
                    java.util.Locale.US,
                    "%.1f MB",
                    bytes / (1024.0 * 1024.0)
            );
        }
        return String.format(
                java.util.Locale.US,
                "%.1f GB",
                bytes / (1024.0 * 1024.0 * 1024.0)
        );
    }

    private String safe(String value) {
        return value == null || value.isEmpty() ? "unknown" : value;
    }

    private TextView label(String value, int size, int color, boolean bold) {
        TextView text = new TextView(getContext());
        text.setText(value);
        text.setTextSize(size);
        text.setTextColor(color);
        text.setTypeface(
                Typeface.DEFAULT,
                bold ? Typeface.BOLD : Typeface.NORMAL
        );
        text.setGravity(Gravity.CENTER_VERTICAL);
        return text;
    }

    private android.graphics.drawable.GradientDrawable round(
            int color,
            float radius,
            int strokeColor
    ) {
        android.graphics.drawable.GradientDrawable drawable =
                new android.graphics.drawable.GradientDrawable();
        drawable.setColor(color);
        drawable.setCornerRadius(radius);
        if (strokeColor != Color.TRANSPARENT) {
            drawable.setStroke(dp(1), strokeColor);
        }
        return drawable;
    }

    private int dp(int value) {
        return Math.round(
                value * getResources().getDisplayMetrics().density
        );
    }
}
