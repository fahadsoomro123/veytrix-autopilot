package com.veytrix.autopilot;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.graphics.drawable.GradientDrawable;

/**
 * Phase 2 flagship Home surface.
 *
 * Bounded native composition: no page scrolling, no fake operational metrics,
 * and mission state is sourced from VeytrixAutopilotClient callbacks.
 */
public final class VeytrixHomeView extends LinearLayout {
    public interface Host {
        void openDrawer();
        void openConnection();
    }

    private final VeytrixAutopilotClient client;
    private final VeytrixControlPreferences prefs;
    private final Host host;
    private EditText missionInput;
    private TextView connectionText;
    private TextView targetText;
    private TextView stateText;
    private TextView runText;
    private Button executeButton;

    public VeytrixHomeView(Context context, VeytrixAutopilotClient client, Host host) {
        super(context);
        this.client = client;
        this.prefs = new VeytrixControlPreferences(context);
        this.host = host;

        setOrientation(VERTICAL);
        setPadding(dp(14), dp(8), dp(14), dp(10));
        setBackgroundColor(VeytrixDesignTokens.PEARL);

        addHeader();
        addConnectionRail();

        FrameLayout coreStage = new FrameLayout(context);
        coreStage.setBackgroundColor(Color.TRANSPARENT);
        LayoutParams coreLp = new LayoutParams(
                LayoutParams.MATCH_PARENT, 0, 1f
        );
        coreLp.topMargin = dp(3);
        coreLp.bottomMargin = dp(5);
        addView(coreStage, coreLp);

        FrameLayout coreFrame = new FrameLayout(context);
        coreFrame.setBackground(round(
                Color.argb(0, 255, 255, 255),
                dp(150),
                Color.TRANSPARENT
        ));
        FrameLayout.LayoutParams coreFrameLp = new FrameLayout.LayoutParams(
                dp(296), dp(296), Gravity.CENTER
        );
        coreStage.addView(coreFrame, coreFrameLp);

        FlagshipCoreView core = new FlagshipCoreView(context);
        coreFrame.addView(core, new FrameLayout.LayoutParams(
                LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT
        ));

        addComposer();
        addExecutionRail();
        addProofRail();

        refreshConnectionState();
    }

    private void addHeader() {
        LinearLayout row = new LinearLayout(getContext());
        row.setGravity(Gravity.CENTER_VERTICAL);

        Button menu = iconButton("☰", "Open navigation");
        menu.setOnClickListener(v -> host.openDrawer());
        row.addView(menu, new LayoutParams(dp(48), dp(48)));

        LinearLayout brand = new LinearLayout(getContext());
        brand.setOrientation(VERTICAL);
        brand.setGravity(Gravity.CENTER_VERTICAL);
        TextView title = label("VEYTRIX", 18, VeytrixDesignTokens.TEXT_PRIMARY, true);
        TextView subtitle = label(
                "AUTONOMOUS ENGINEERING CONTROL",
                9,
                VeytrixDesignTokens.TEXT_SECONDARY,
                true
        );
        brand.addView(title);
        brand.addView(subtitle);

        LayoutParams brandLp = new LayoutParams(0, dp(48), 1f);
        brandLp.leftMargin = dp(7);
        row.addView(brand, brandLp);

        Button connect = iconButton("◈", "GitHub connection");
        connect.setOnClickListener(v -> host.openConnection());
        row.addView(connect, new LayoutParams(dp(48), dp(48)));

        addView(row, new LayoutParams(
                LayoutParams.MATCH_PARENT, dp(48)
        ));
    }

    private void addConnectionRail() {
        LinearLayout rail = new LinearLayout(getContext());
        rail.setGravity(Gravity.CENTER_VERTICAL);
        rail.setPadding(dp(12), 0, dp(12), 0);
        rail.setBackground(round(
                Color.TRANSPARENT,
                dp(14),
                VeytrixDesignTokens.SILVER
        ));

        TextView dot = label("●", 11, VeytrixDesignTokens.STATE_IDLE, true);
        rail.addView(dot, new LayoutParams(dp(20), dp(32)));

        connectionText = label("NOT CONNECTED", 10, VeytrixDesignTokens.TEXT_PRIMARY, true);
        rail.addView(connectionText, new LayoutParams(
                0, dp(32), 1f
        ));

        targetText = label("Target not configured", 9, VeytrixDesignTokens.TEXT_SECONDARY, false);
        targetText.setGravity(Gravity.CENTER_VERTICAL | Gravity.RIGHT);
        rail.addView(targetText, new LayoutParams(
                0, dp(32), 1f
        ));

        LayoutParams lp = new LayoutParams(
                LayoutParams.MATCH_PARENT, dp(36)
        );
        lp.topMargin = dp(4);
        addView(rail, lp);
    }

    private void addComposer() {
        LinearLayout composer = new LinearLayout(getContext());
        composer.setOrientation(VERTICAL);
        composer.setPadding(dp(13), dp(10), dp(13), dp(10));
        composer.setBackground(round(
                VeytrixDesignTokens.WHITE,
                dp(18),
                VeytrixDesignTokens.SILVER
        ));

        TextView heading = label(
                "MISSION COMMAND",
                11,
                VeytrixDesignTokens.TEXT_PRIMARY,
                true
        );
        composer.addView(heading, new LayoutParams(
                LayoutParams.MATCH_PARENT, dp(20)
        ));

        missionInput = new EditText(getContext());
        missionInput.setSingleLine(false);
        missionInput.setMaxLines(3);
        missionInput.setTextSize(15);
        missionInput.setTextColor(VeytrixDesignTokens.TEXT_PRIMARY);
        missionInput.setHintTextColor(VeytrixDesignTokens.TEXT_TERTIARY);
        missionInput.setHint("Describe the engineering mission…");
        missionInput.setInputType(
                InputType.TYPE_CLASS_TEXT
                        | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
                        | InputType.TYPE_TEXT_FLAG_MULTI_LINE
        );
        missionInput.setPadding(dp(11), dp(5), dp(11), dp(5));
        missionInput.setBackground(round(
                VeytrixDesignTokens.PEARL,
                dp(12),
                VeytrixDesignTokens.SILVER
        ));

        LayoutParams inputLp = new LayoutParams(
                LayoutParams.MATCH_PARENT, dp(62)
        );
        inputLp.topMargin = dp(5);
        composer.addView(missionInput, inputLp);

        TextView recipe = label(
                "engine  ·  verification depth " + prefs.getVerificationDepth()
                        + "  ·  target branch main",
                8,
                VeytrixDesignTokens.TEXT_SECONDARY,
                false
        );
        recipe.setGravity(Gravity.CENTER_VERTICAL);
        composer.addView(recipe, new LayoutParams(
                LayoutParams.MATCH_PARENT, dp(18)
        ));

        LayoutParams lp = new LayoutParams(
                LayoutParams.MATCH_PARENT, dp(113)
        );
        lp.bottomMargin = dp(6);
        addView(composer, lp);
    }

    private void addExecutionRail() {
        LinearLayout row = new LinearLayout(getContext());
        row.setGravity(Gravity.CENTER_VERTICAL);

        stateText = label(
                "READY",
                11,
                VeytrixDesignTokens.STATE_IDLE,
                true
        );
        row.addView(stateText, new LayoutParams(
                0, dp(50), 1f
        ));

        executeButton = actionButton("EXECUTE MISSION");
        executeButton.setOnClickListener(v -> executeMission());
        row.addView(executeButton, new LayoutParams(
                dp(192), dp(50)
        ));

        LayoutParams lp = new LayoutParams(
                LayoutParams.MATCH_PARENT, dp(50)
        );
        lp.bottomMargin = dp(6);
        addView(row, lp);
    }

    private void addProofRail() {
        LinearLayout rail = new LinearLayout(getContext());
        rail.setGravity(Gravity.CENTER_VERTICAL);
        rail.setPadding(dp(12), 0, dp(12), 0);
        rail.setBackground(round(
                VeytrixDesignTokens.WHITE,
                dp(15),
                VeytrixDesignTokens.SILVER
        ));

        TextView proof = label(
                "CONTROL PLANE",
                9,
                VeytrixDesignTokens.TEXT_SECONDARY,
                true
        );
        rail.addView(proof, new LayoutParams(dp(105), dp(48)));

        runText = label(
                "No active run",
                9,
                VeytrixDesignTokens.TEXT_PRIMARY,
                true
        );
        runText.setGravity(Gravity.CENTER_VERTICAL);
        rail.addView(runText, new LayoutParams(
                0, dp(48), 1f
        ));

        TextView connect = label(
                "SECURE",
                9,
                VeytrixDesignTokens.STATE_VERIFIED,
                true
        );
        connect.setGravity(Gravity.CENTER);
        rail.addView(connect, new LayoutParams(dp(70), dp(48)));

        addView(rail, new LayoutParams(
                LayoutParams.MATCH_PARENT, dp(52)
        ));
    }

    private void refreshConnectionState() {
        boolean connected = client.hasToken() && !client.getTargetRepository().isEmpty();
        connectionText.setText(connected ? "CONNECTED" : "NOT CONNECTED");
        connectionText.setTextColor(
                connected
                        ? VeytrixDesignTokens.STATE_VERIFIED
                        : VeytrixDesignTokens.STATE_IDLE
        );
        targetText.setText(
                connected
                        ? client.getTargetRepository()
                        : "Target not configured"
        );
        executeButton.setEnabled(connected);
        executeButton.setAlpha(connected ? 1f : 0.55f);
    }

    public void setMissionText(String value) {
        if (missionInput != null) {
            missionInput.setText(value == null ? "" : value);
            missionInput.setSelection(missionInput.length());
        }
    }

    private void executeMission() {
        final String mission = missionInput.getText().toString().trim();
        try {
            VeytrixInputValidator.validateMission(mission);
        } catch (IllegalArgumentException error) {
            missionInput.requestFocus();
            missionInput.setError(error.getMessage());
            return;
        }

        final String target = client.getTargetRepository();
        if (target.isEmpty() || !client.hasToken()) {
            host.openConnection();
            return;
        }

        executeButton.setEnabled(false);
        executeButton.setText("STARTING…");
        stateText.setText("DISPATCHING");
        stateText.setTextColor(VeytrixDesignTokens.STATE_PLANNING);
        runText.setText("Waiting for GitHub run…");

        client.startMission(
                mission,
                target,
                "main",
                prefs.getEngine(),
                prefs.getVerificationDepth(),
                new VeytrixAutopilotClient.Callback() {
                    @Override
                    public void onStarted() {
                        stateText.setText("PLANNING");
                        stateText.setTextColor(VeytrixDesignTokens.STATE_PLANNING);
                    }

                    @Override
                    public void onRunLocated(VeytrixAutopilotClient.RunInfo run) {
                        runText.setText("Run #" + run.number + " · " + safeStatus(run.status));
                        stateText.setText("RUNNING");
                        stateText.setTextColor(VeytrixDesignTokens.STATE_IMPLEMENTING);
                    }

                    @Override
                    public void onRunUpdated(VeytrixAutopilotClient.RunInfo run) {
                        runText.setText(
                                "Run #" + run.number + " · " + safeStatus(
                                        run.isFinished() ? run.conclusion : run.status
                                )
                        );
                        stateText.setTextColor(
                                run.isSuccessful()
                                        ? VeytrixDesignTokens.STATE_VERIFIED
                                        : VeytrixDesignTokens.STATE_VERIFYING
                        );
                        if (run.isFinished()) {
                            stateText.setText(
                                    run.isSuccessful() ? "VERIFIED" : "FAILED"
                            );
                        }
                    }

                    @Override
                    public void onCompleted(VeytrixAutopilotClient.RunInfo run) {
                        stateText.setText(
                                run.isSuccessful() ? "DELIVERED" : "FAILED"
                        );
                        stateText.setTextColor(
                                run.isSuccessful()
                                        ? VeytrixDesignTokens.STATE_DELIVERED
                                        : VeytrixDesignTokens.STATE_FAILED
                        );
                        runText.setText(
                                "Run #" + run.number + " · "
                                        + (run.isSuccessful() ? "success" : safeStatus(run.conclusion))
                        );
                        executeButton.setEnabled(true);
                        executeButton.setText("EXECUTE MISSION");
                    }

                    @Override
                    public void onError(String message) {
                        stateText.setText("BLOCKED");
                        stateText.setTextColor(VeytrixDesignTokens.STATE_FAILED);
                        runText.setText(message);
                        executeButton.setEnabled(true);
                        executeButton.setText("EXECUTE MISSION");
                    }
                }
        );
    }

    private static String safeStatus(String value) {
        return value == null || value.isEmpty() ? "unknown" : value.toUpperCase();
    }

    private Button actionButton(String value) {
        Button button = new Button(getContext());
        button.setText(value);
        button.setTextSize(11);
        button.setTextColor(Color.WHITE);
        button.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        button.setAllCaps(false);
        button.setMinHeight(dp(50));
        button.setMinWidth(dp(160));

        GradientDrawable bg = new GradientDrawable(
                GradientDrawable.Orientation.LEFT_RIGHT,
                new int[]{
                        VeytrixDesignTokens.VIOLET,
                        VeytrixDesignTokens.MAGENTA,
                        VeytrixDesignTokens.ROSE
                }
        );
        bg.setCornerRadius(dp(16));
        bg.setStroke(dp(1), Color.argb(70, 255, 255, 255));
        button.setBackground(bg);
        return button;
    }

    private Button iconButton(String value, String description) {
        Button button = new Button(getContext());
        button.setText(value);
        button.setTextSize(20);
        button.setTextColor(VeytrixDesignTokens.TEXT_PRIMARY);
        button.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        button.setAllCaps(false);
        button.setContentDescription(description);
        button.setMinWidth(dp(48));
        button.setMinHeight(dp(48));
        button.setPadding(0, 0, 0, 0);
        button.setBackground(round(
                VeytrixDesignTokens.WHITE,
                dp(14),
                VeytrixDesignTokens.SILVER
        ));
        return button;
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

    private GradientDrawable round(int color, float radius, int strokeColor) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(color);
        drawable.setCornerRadius(radius);
        if (strokeColor != Color.TRANSPARENT) {
            drawable.setStroke(dp(1), strokeColor);
        }
        return drawable;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
