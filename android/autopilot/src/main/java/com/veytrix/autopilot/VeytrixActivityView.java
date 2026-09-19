package com.veytrix.autopilot;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.List;

/**
 * Phase 2 Activity surface.
 *
 * Displays only GitHub workflow runs actually returned by the control plane.
 * No synthetic mission cards, percentages, durations or completion claims.
 */
public final class VeytrixActivityView extends LinearLayout {
    public interface Host {
        void openConnection();
        void openRunDetails(VeytrixAutopilotClient.RunInfo run);
    }

    private final VeytrixAutopilotClient client;
    private final Host host;
    private final TextView status;
    private final LinearLayout runRail;

    public VeytrixActivityView(Context context, VeytrixAutopilotClient client, Host host) {
        super(context);
        this.client = client;
        this.host = host;

        setOrientation(VERTICAL);
        setPadding(dp(14), dp(8), dp(14), dp(8));
        setBackgroundColor(VeytrixDesignTokens.PEARL);

        LinearLayout header = new LinearLayout(context);
        header.setGravity(Gravity.CENTER_VERTICAL);

        LinearLayout titleCol = new LinearLayout(context);
        titleCol.setOrientation(VERTICAL);
        TextView title = label("ACTIVITY", 22, VeytrixDesignTokens.TEXT_PRIMARY, true);
        TextView subtitle = label(
                "Real control-plane workflow history",
                11,
                VeytrixDesignTokens.TEXT_SECONDARY,
                false
        );
        titleCol.addView(title);
        titleCol.addView(subtitle);

        header.addView(titleCol, new LayoutParams(0, dp(48), 1f));

        TextView connection = label(
                client.hasToken() ? "CONNECTED" : "CONNECT",
                10,
                client.hasToken()
                        ? VeytrixDesignTokens.STATE_VERIFIED
                        : VeytrixDesignTokens.VIOLET,
                true
        );
        connection.setGravity(Gravity.CENTER);
        connection.setMinWidth(dp(96));
        connection.setMinHeight(dp(48));
        connection.setBackground(round(
                VeytrixDesignTokens.WHITE,
                dp(14),
                VeytrixDesignTokens.SILVER
        ));
        connection.setOnClickListener(v -> {
            if (!client.hasToken()) {
                host.openConnection();
            }
        });
        header.addView(connection, new LayoutParams(dp(104), dp(48)));
        addView(header, new LayoutParams(
                LayoutParams.MATCH_PARENT, dp(48)
        ));

        status = label(
                "Loading recent runs…",
                11,
                VeytrixDesignTokens.TEXT_SECONDARY,
                false
        );
        status.setGravity(Gravity.CENTER_VERTICAL);
        LayoutParams statusLp = new LayoutParams(
                LayoutParams.MATCH_PARENT, dp(36)
        );
        statusLp.topMargin = dp(6);
        addView(status, statusLp);

        LinearLayout divider = new LinearLayout(context);
        divider.setBackgroundColor(VeytrixDesignTokens.SILVER);
        addView(divider, new LayoutParams(
                LayoutParams.MATCH_PARENT, dp(1)
        ));

        runRail = new LinearLayout(context);
        runRail.setOrientation(VERTICAL);
        LayoutParams railLp = new LayoutParams(
                LayoutParams.MATCH_PARENT, 0, 1f
        );
        railLp.topMargin = dp(6);
        addView(runRail, railLp);

        loadRuns();
    }

    private void loadRuns() {
        if (!client.hasToken()) {
            status.setText("No GitHub connection. Connect to inspect real runs.");
            addEmptyState("CONNECT GITHUB TO VIEW REAL RUN HISTORY");
            return;
        }

        client.fetchRecentRuns(6, new VeytrixAutopilotClient.SimpleCallback<List<VeytrixAutopilotClient.RunInfo>>() {
            @Override
            public void onSuccess(List<VeytrixAutopilotClient.RunInfo> runs) {
                runRail.removeAllViews();

                if (runs.isEmpty()) {
                    status.setText("GitHub returned no workflow_dispatch history.");
                    addEmptyState("NO MISSION RUNS FOUND");
                    return;
                }

                status.setText(runs.size() + " real run" + (runs.size() == 1 ? "" : "s") + " returned");
                for (VeytrixAutopilotClient.RunInfo run : runs) {
                    addRun(run);
                }
            }

            @Override
            public void onError(String message) {
                runRail.removeAllViews();
                status.setText("Could not read workflow history.");
                addEmptyState(message.toUpperCase());
            }
        });
    }

    private void addRun(VeytrixAutopilotClient.RunInfo run) {
        LinearLayout row = new LinearLayout(getContext());
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(12), dp(6), dp(12), dp(6));
        row.setBackground(round(
                VeytrixDesignTokens.WHITE,
                dp(15),
                VeytrixDesignTokens.SILVER
        ));

        TextView marker = label(
                "•",
                22,
                statusColor(run),
                true
        );
        marker.setGravity(Gravity.CENTER);
        row.addView(marker, new LayoutParams(dp(28), dp(48)));

        LinearLayout copy = new LinearLayout(getContext());
        copy.setOrientation(VERTICAL);

        TextView primary = label(
                "RUN #" + run.number,
                12,
                VeytrixDesignTokens.TEXT_PRIMARY,
                true
        );
        TextView secondary = label(
                run.isFinished()
                        ? safe(run.conclusion).toUpperCase()
                        : safe(run.status).toUpperCase(),
                10,
                VeytrixDesignTokens.TEXT_SECONDARY,
                true
        );
        copy.addView(primary, new LayoutParams(
                LayoutParams.MATCH_PARENT, dp(22)
        ));
        copy.addView(secondary, new LayoutParams(
                LayoutParams.MATCH_PARENT, dp(20)
        ));

        row.addView(copy, new LayoutParams(
                0, dp(48), 1f
        ));

        TextView id = label(
                String.valueOf(run.id),
                8,
                VeytrixDesignTokens.TEXT_TERTIARY,
                false
        );
        id.setGravity(Gravity.CENTER_VERTICAL | Gravity.RIGHT);
        row.addView(id, new LayoutParams(
                dp(84), dp(48)
        ));

        LayoutParams rowLp = new LayoutParams(
                LayoutParams.MATCH_PARENT, dp(62)
        );
        rowLp.bottomMargin = dp(7);
        row.setOnClickListener(v -> host.openRunDetails(run));
        runRail.addView(row, rowLp);
    }

    private void addEmptyState(String message) {
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
        runRail.addView(empty, new LayoutParams(
                LayoutParams.MATCH_PARENT, dp(96)
        ));
    }

    private int statusColor(VeytrixAutopilotClient.RunInfo run) {
        if (!run.isFinished()) {
            return VeytrixDesignTokens.STATE_IMPLEMENTING;
        }
        return run.isSuccessful()
                ? VeytrixDesignTokens.STATE_VERIFIED
                : VeytrixDesignTokens.STATE_FAILED;
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
