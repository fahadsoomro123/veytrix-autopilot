package com.veytrix.autopilot;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.List;

/** Detail surface backed only by one real GitHub Actions run and its artifacts. */
public final class VeytrixRunDetailsView extends LinearLayout {
    public interface Host {
        void openDrawer();
        void openActivity();
    }

    private final VeytrixAutopilotClient client;
    private final VeytrixAutopilotClient.RunInfo run;
    private final LinearLayout artifacts;
    private final TextView artifactStatus;

    public VeytrixRunDetailsView(
            Context context,
            VeytrixAutopilotClient client,
            VeytrixAutopilotClient.RunInfo run,
            Host host
    ) {
        super(context);
        this.client = client;
        this.run = run;
        setOrientation(VERTICAL);
        setPadding(dp(14), dp(8), dp(14), dp(10));
        setBackgroundColor(VeytrixDesignTokens.PEARL);

        LinearLayout header = new LinearLayout(context);
        header.setGravity(Gravity.CENTER_VERTICAL);
        TextView menu = text("☰", 21, VeytrixDesignTokens.TEXT_PRIMARY, true);
        menu.setGravity(Gravity.CENTER);
        menu.setOnClickListener(v -> host.openDrawer());
        header.addView(menu, new LayoutParams(dp(48), dp(48)));

        TextView title = text("RUN #" + run.number, 21,
                VeytrixDesignTokens.TEXT_PRIMARY, true);
        title.setPadding(dp(8), 0, 0, 0);
        header.addView(title, new LayoutParams(0, dp(48), 1f));

        TextView back = text("ACTIVITY", 9, VeytrixDesignTokens.VIOLET, true);
        back.setGravity(Gravity.CENTER);
        back.setBackground(round(
                VeytrixDesignTokens.WHITE, dp(14), VeytrixDesignTokens.SILVER
        ));
        back.setOnClickListener(v -> host.openActivity());
        header.addView(back, new LayoutParams(dp(88), dp(48)));
        addView(header, new LayoutParams(-1, dp(48)));

        LinearLayout facts = new LinearLayout(context);
        facts.setOrientation(VERTICAL);
        facts.setPadding(dp(12), dp(8), dp(12), dp(8));
        facts.setBackground(round(
                VeytrixDesignTokens.WHITE, dp(16), VeytrixDesignTokens.SILVER
        ));
        addFact(facts, "STATUS", safe(run.status).toUpperCase());
        addFact(facts, "CONCLUSION",
                run.isFinished() ? safe(run.conclusion).toUpperCase() : "NOT FINISHED");
        addFact(facts, "RUN ID", String.valueOf(run.id));
        addFact(facts, "UPDATED", safe(run.updatedAt));
        addView(facts, marginBottom(9));

        artifactStatus = text(
                "Loading artifacts for this run…", 10,
                VeytrixDesignTokens.TEXT_SECONDARY, false
        );
        addView(artifactStatus, new LayoutParams(-1, dp(34)));

        artifacts = new LinearLayout(context);
        artifacts.setOrientation(VERTICAL);
        addView(artifacts, new LayoutParams(-1, 0, 1f));
        loadArtifacts();
    }

    private void addFact(LinearLayout parent, String key, String value) {
        LinearLayout row = new LinearLayout(getContext());
        row.setGravity(Gravity.CENTER_VERTICAL);
        TextView label = text(key, 9, VeytrixDesignTokens.TEXT_SECONDARY, true);
        row.addView(label, new LayoutParams(dp(100), dp(28)));
        TextView content = text(value, 10, VeytrixDesignTokens.TEXT_PRIMARY, true);
        content.setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL);
        row.addView(content, new LayoutParams(0, dp(28), 1f));
        parent.addView(row, new LayoutParams(-1, dp(30)));
    }

    private void loadArtifacts() {
        client.fetchArtifacts(run.id, 6,
                new VeytrixAutopilotClient.SimpleCallback<List<VeytrixAutopilotClient.ArtifactInfo>>() {
                    @Override public void onSuccess(List<VeytrixAutopilotClient.ArtifactInfo> list) {
                        artifacts.removeAllViews();
                        if (list.isEmpty()) {
                            artifactStatus.setText("GitHub returned no artifacts for this run.");
                            return;
                        }
                        artifactStatus.setText(
                                list.size() + " artifact" + (list.size() == 1 ? "" : "s")
                                        + " returned"
                        );
                        for (VeytrixAutopilotClient.ArtifactInfo artifact : list) {
                            addArtifact(artifact);
                        }
                    }

                    @Override public void onError(String message) {
                        artifacts.removeAllViews();
                        artifactStatus.setText("Artifact lookup failed: " + message);
                    }
                });
    }

    private void addArtifact(VeytrixAutopilotClient.ArtifactInfo artifact) {
        LinearLayout row = new LinearLayout(getContext());
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(10), dp(6), dp(10), dp(6));
        row.setBackground(round(
                VeytrixDesignTokens.WHITE, dp(14), VeytrixDesignTokens.SILVER
        ));
        TextView name = text(
                artifact.name, 10, VeytrixDesignTokens.TEXT_PRIMARY, true
        );
        row.addView(name, new LayoutParams(0, dp(48), 1f));
        TextView meta = text(
                bytes(artifact.sizeBytes) + (artifact.expired ? " · expired" : ""),
                8,
                artifact.expired
                        ? VeytrixDesignTokens.STATE_FAILED
                        : VeytrixDesignTokens.TEXT_SECONDARY,
                false
        );
        meta.setGravity(Gravity.CENTER_VERTICAL | Gravity.RIGHT);
        row.addView(meta, new LayoutParams(dp(110), dp(48)));
        LayoutParams lp = new LayoutParams(-1, dp(60));
        lp.bottomMargin = dp(7);
        artifacts.addView(row, lp);
    }

    private String bytes(long value) {
        if (value < 1024) return value + " B";
        if (value < 1024 * 1024) return (value / 1024) + " KB";
        return (value / (1024 * 1024)) + " MB";
    }

    private String safe(String value) {
        return value == null || value.isEmpty() ? "unknown" : value;
    }

    private TextView text(String value, int size, int color, boolean bold) {
        TextView text = new TextView(getContext());
        text.setText(value);
        text.setTextSize(size);
        text.setTextColor(color);
        text.setTypeface(Typeface.DEFAULT, bold ? Typeface.BOLD : Typeface.NORMAL);
        text.setGravity(Gravity.CENTER_VERTICAL);
        return text;
    }

    private android.graphics.drawable.GradientDrawable round(
            int color, float radius, int stroke
    ) {
        android.graphics.drawable.GradientDrawable drawable =
                new android.graphics.drawable.GradientDrawable();
        drawable.setColor(color);
        drawable.setCornerRadius(radius);
        if (stroke != Color.TRANSPARENT) drawable.setStroke(dp(1), stroke);
        return drawable;
    }

    private LayoutParams marginBottom(int value) {
        LayoutParams lp = new LayoutParams(-1, -2);
        lp.bottomMargin = dp(value);
        return lp;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}