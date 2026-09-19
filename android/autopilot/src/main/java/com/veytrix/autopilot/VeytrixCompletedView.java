package com.veytrix.autopilot;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

/** Real successful workflow history surface. */
public final class VeytrixCompletedView extends LinearLayout {
    public interface Host {
        void openConnection();
        void openDrawer();
        void openRunDetails(VeytrixAutopilotClient.RunInfo run);
    }

    private final VeytrixAutopilotClient client;
    private final LinearLayout list;
    private final TextView status;

    public VeytrixCompletedView(Context context, VeytrixAutopilotClient client, Host host) {
        super(context);
        this.client = client;
        setOrientation(VERTICAL);
        setPadding(dp(14), dp(8), dp(14), dp(10));
        setBackgroundColor(VeytrixDesignTokens.PEARL);

        LinearLayout header = new LinearLayout(context);
        header.setGravity(Gravity.CENTER_VERTICAL);
        TextView menu = textButton("☰", 21);
        menu.setOnClickListener(v -> host.openDrawer());
        header.addView(menu, new LayoutParams(dp(48), dp(48)));
        TextView title = label("COMPLETED", 22, VeytrixDesignTokens.TEXT_PRIMARY, true);
        title.setPadding(dp(8), 0, 0, 0);
        header.addView(title, new LayoutParams(0, dp(48), 1f));
        TextView connection = label(
                client.hasToken() ? "CONNECTED" : "CONNECT",
                10,
                client.hasToken()
                        ? VeytrixDesignTokens.STATE_VERIFIED
                        : VeytrixDesignTokens.VIOLET,
                true
        );
        connection.setGravity(Gravity.CENTER);
        connection.setMinWidth(dp(90));
        connection.setMinHeight(dp(48));
        connection.setBackground(round(
                VeytrixDesignTokens.WHITE, dp(14), VeytrixDesignTokens.SILVER
        ));
        connection.setOnClickListener(v -> {
            if (!client.hasToken()) host.openConnection();
        });
        header.addView(connection, new LayoutParams(dp(96), dp(48)));
        addView(header, new LayoutParams(-1, dp(48)));

        status = label("Loading real successful workflow runs…", 10,
                VeytrixDesignTokens.TEXT_SECONDARY, false);
        status.setGravity(Gravity.CENTER_VERTICAL);
        addView(status, new LayoutParams(-1, dp(38)));

        list = new LinearLayout(context);
        list.setOrientation(VERTICAL);
        addView(list, new LayoutParams(-1, 0, 1f));

        load(host);
    }

    private void load(Host host) {
        if (!client.hasToken()) {
            status.setText("Connect GitHub to inspect real completed runs.");
            empty("NO GITHUB CONNECTION");
            return;
        }

        client.fetchRecentRuns(10, new VeytrixAutopilotClient.SimpleCallback<List<VeytrixAutopilotClient.RunInfo>>() {
            @Override public void onSuccess(List<VeytrixAutopilotClient.RunInfo> runs) {
                list.removeAllViews();
                List<VeytrixAutopilotClient.RunInfo> completed = new ArrayList<>();
                for (VeytrixAutopilotClient.RunInfo run : runs) {
                    if (run.isFinished() && run.isSuccessful()) completed.add(run);
                }
                status.setText(
                        completed.isEmpty()
                                ? "No successful completed runs in the returned history."
                                : completed.size() + " successful run"
                                        + (completed.size() == 1 ? "" : "s")
                                        + " in the returned history"
                );
                if (completed.isEmpty()) {
                    empty("NO SUCCESSFUL RUNS RETURNED");
                    return;
                }
                int count = 0;
                for (VeytrixAutopilotClient.RunInfo run : completed) {
                    if (count++ == 6) break;
                    addRun(run, host);
                }
            }

            @Override public void onError(String message) {
                list.removeAllViews();
                status.setText("Could not read completed workflow history.");
                empty(message.toUpperCase());
            }
        });
    }

    private void addRun(VeytrixAutopilotClient.RunInfo run, Host host) {
        LinearLayout row = new LinearLayout(getContext());
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(12), dp(5), dp(10), dp(5));
        row.setBackground(round(
                VeytrixDesignTokens.WHITE, dp(15), VeytrixDesignTokens.SILVER
        ));
        TextView marker = label("✓", 18, VeytrixDesignTokens.STATE_VERIFIED, true);
        marker.setGravity(Gravity.CENTER);
        row.addView(marker, new LayoutParams(dp(34), dp(50)));
        LinearLayout copy = new LinearLayout(getContext());
        copy.setOrientation(VERTICAL);
        copy.addView(label("RUN #" + run.number, 12,
                VeytrixDesignTokens.TEXT_PRIMARY, true), new LayoutParams(-1, dp(22)));
        copy.addView(label(
                "completed · " + safe(run.updatedAt),
                9, VeytrixDesignTokens.TEXT_SECONDARY, false
        ), new LayoutParams(-1, dp(20)));
        row.addView(copy, new LayoutParams(0, dp(52), 1f));
        TextView open = label("OPEN", 9, VeytrixDesignTokens.VIOLET, true);
        open.setGravity(Gravity.CENTER);
        row.addView(open, new LayoutParams(dp(56), dp(48)));
        row.setOnClickListener(v -> host.openRunDetails(run));
        LayoutParams lp = new LayoutParams(-1, dp(62));
        lp.bottomMargin = dp(7);
        list.addView(row, lp);
    }

    private void empty(String message) {
        TextView empty = label(message, 11, VeytrixDesignTokens.TEXT_SECONDARY, true);
        empty.setGravity(Gravity.CENTER);
        empty.setBackground(round(
                VeytrixDesignTokens.WHITE, dp(18), VeytrixDesignTokens.SILVER
        ));
        list.addView(empty, new LayoutParams(-1, dp(90)));
    }

    private String safe(String value) {
        return value == null || value.isEmpty() ? "timestamp unavailable" : value;
    }

    private TextView textButton(String value, int size) {
        TextView text = label(value, size, VeytrixDesignTokens.TEXT_PRIMARY, true);
        text.setGravity(Gravity.CENTER);
        return text;
    }

    private TextView label(String value, int size, int color, boolean bold) {
        TextView text = new TextView(getContext());
        text.setText(value);
        text.setTextSize(size);
        text.setTextColor(color);
        text.setTypeface(Typeface.DEFAULT, bold ? Typeface.BOLD : Typeface.NORMAL);
        text.setGravity(Gravity.CENTER_VERTICAL);
        return text;
    }

    private android.graphics.drawable.GradientDrawable round(int color, float radius, int stroke) {
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