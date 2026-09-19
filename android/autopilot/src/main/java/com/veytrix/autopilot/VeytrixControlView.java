package com.veytrix.autopilot;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * Phase 2 operator control surface.
 *
 * Only exposes controls that the current workflow actually understands:
 * execution engine, bounded verification depth, and local notifications.
 */
public final class VeytrixControlView extends LinearLayout {
    private final VeytrixControlPreferences prefs;
    private final VeytrixAutopilotClient client;

    public VeytrixControlView(
            Context context,
            VeytrixAutopilotClient client,
            Runnable openConnection
    ) {
        super(context);
        this.client = client;
        this.prefs = new VeytrixControlPreferences(context);

        setOrientation(VERTICAL);
        setPadding(dp(14), dp(8), dp(14), dp(8));
        setBackgroundColor(VeytrixDesignTokens.PEARL);

        LinearLayout header = new LinearLayout(context);
        header.setGravity(Gravity.CENTER_VERTICAL);

        LinearLayout titleCol = new LinearLayout(context);
        titleCol.setOrientation(VERTICAL);
        titleCol.addView(label(
                "CONTROL",
                22,
                VeytrixDesignTokens.TEXT_PRIMARY,
                true
        ));
        titleCol.addView(label(
                "Explicit operator configuration",
                11,
                VeytrixDesignTokens.TEXT_SECONDARY,
                false
        ));
        header.addView(titleCol, new LayoutParams(
                0, dp(48), 1f
        ));

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
                openConnection.run();
            }
        });
        header.addView(connection, new LayoutParams(
                dp(104), dp(48)
        ));
        addView(header, new LayoutParams(
                LayoutParams.MATCH_PARENT, dp(48)
        ));

        addView(label(
                "EXECUTION ENGINE",
                10,
                VeytrixDesignTokens.TEXT_PRIMARY,
                true
        ), topBottom(8, 4));

        TextView engineValue = value(prefs.getEngine());
        addView(settingRow(
                "Engine",
                "Sent directly to Smart Autopilot",
                engineValue,
                "EDIT",
                v -> chooseEngine(engineValue)
        ), new LayoutParams(
                LayoutParams.MATCH_PARENT, dp(66)
        ));

        addView(label(
                "VERIFICATION",
                10,
                VeytrixDesignTokens.TEXT_PRIMARY,
                true
        ), topBottom(8, 4));

        TextView depthValue = value(
                String.valueOf(prefs.getVerificationDepth())
        );
        addView(settingRow(
                "Verification depth",
                "Bounded handoffs: 0–2; Mesh can act as fallback.",
                depthValue,
                "EDIT",
                v -> chooseDepth(depthValue)
        ), new LayoutParams(
                LayoutParams.MATCH_PARENT, dp(66)
        ));

        addView(label(
                "SECURITY",
                10,
                VeytrixDesignTokens.TEXT_PRIMARY,
                true
        ), topBottom(8, 4));

        LinearLayout security = new LinearLayout(context);
        security.setOrientation(VERTICAL);
        security.setPadding(dp(12), dp(8), dp(12), dp(8));
        security.setBackground(round(
                VeytrixDesignTokens.WHITE,
                dp(16),
                VeytrixDesignTokens.SILVER
        ));
        security.addView(label(
                "Android Keystore credential protection",
                11,
                VeytrixDesignTokens.TEXT_PRIMARY,
                true
        ), new LayoutParams(
                LayoutParams.MATCH_PARENT, dp(22)
        ));
        security.addView(label(
                client.hasToken()
                        ? "GitHub credential present in encrypted local storage."
                        : "No GitHub credential stored on this device.",
                10,
                VeytrixDesignTokens.TEXT_SECONDARY,
                false
        ), new LayoutParams(
                LayoutParams.MATCH_PARENT, dp(28)
        ));
        addView(security, new LayoutParams(
                LayoutParams.MATCH_PARENT, dp(70)
        ));

        TextView boundary = label(
                "Control changes affect the next mission dispatch only.",
                10,
                VeytrixDesignTokens.TEXT_TERTIARY,
                false
        );
        boundary.setGravity(Gravity.CENTER_VERTICAL);
        addView(boundary, new LayoutParams(
                LayoutParams.MATCH_PARENT, dp(42)
        ));
    }

    private void chooseEngine(TextView value) {
        String[] options = {"auto", "github-free", "ai", "mesh"};
        new android.app.AlertDialog.Builder(getContext())
                .setTitle("Execution engine")
                .setSingleChoiceItems(
                        options,
                        indexOf(options, prefs.getEngine()),
                        (dialog, which) -> {
                            prefs.setEngine(options[which]);
                            value.setText(options[which]);
                            dialog.dismiss();
                        }
                )
                .show();
    }

    private void chooseDepth(TextView value) {
        String[] options = {"0", "1", "2"};
        new android.app.AlertDialog.Builder(getContext())
                .setTitle("Verification depth")
                .setSingleChoiceItems(
                        options,
                        prefs.getVerificationDepth(),
                        (dialog, which) -> {
                            prefs.setVerificationDepth(which);
                            value.setText(String.valueOf(which));
                            dialog.dismiss();
                        }
                )
                .show();
    }

    private int indexOf(String[] options, String selected) {
        for (int i = 0; i < options.length; i++) {
            if (options[i].equals(selected)) {
                return i;
            }
        }
        return 0;
    }

    private LinearLayout settingRow(
            String title,
            String subtitle,
            TextView value,
            String action,
            OnClickListener listener
    ) {
        LinearLayout row = new LinearLayout(getContext());
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(12), dp(6), dp(8), dp(6));
        row.setBackground(round(
                VeytrixDesignTokens.WHITE,
                dp(15),
                VeytrixDesignTokens.SILVER
        ));

        LinearLayout copy = new LinearLayout(getContext());
        copy.setOrientation(VERTICAL);
        copy.addView(label(
                title,
                11,
                VeytrixDesignTokens.TEXT_PRIMARY,
                true
        ), new LayoutParams(
                LayoutParams.MATCH_PARENT, dp(22)
        ));
        copy.addView(label(
                subtitle,
                9,
                VeytrixDesignTokens.TEXT_SECONDARY,
                false
        ), new LayoutParams(
                LayoutParams.MATCH_PARENT, dp(20)
        ));
        row.addView(copy, new LayoutParams(
                0, dp(48), 1f
        ));

        Button actionButton = new Button(getContext());
        actionButton.setText(action);
        actionButton.setTextSize(9);
        actionButton.setTextColor(VeytrixDesignTokens.VIOLET);
        actionButton.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        actionButton.setAllCaps(false);
        actionButton.setMinWidth(dp(74));
        actionButton.setMinHeight(dp(48));
        actionButton.setBackground(round(
                VeytrixDesignTokens.PEARL,
                dp(13),
                VeytrixDesignTokens.SILVER
        ));
        actionButton.setOnClickListener(listener);
        row.addView(actionButton, new LayoutParams(
                dp(78), dp(48)
        ));

        return row;
    }

    private TextView value(String text) {
        TextView value = label(
                text,
                9,
                VeytrixDesignTokens.STATE_VERIFIED,
                true
        );
        value.setGravity(Gravity.CENTER);
        value.setBackground(round(
                Color.argb(30,
                        Color.red(VeytrixDesignTokens.STATE_VERIFIED),
                        Color.green(VeytrixDesignTokens.STATE_VERIFIED),
                        Color.blue(VeytrixDesignTokens.STATE_VERIFIED)
                ),
                dp(11),
                Color.TRANSPARENT
        ));
        return value;
    }

    private LayoutParams topBottom(int top, int bottom) {
        LayoutParams lp = new LayoutParams(
                LayoutParams.MATCH_PARENT, dp(22)
        );
        lp.topMargin = dp(top);
        lp.bottomMargin = dp(bottom);
        return lp;
    }

    private TextView label(String text, int size, int color, boolean bold) {
        TextView view = new TextView(getContext());
        view.setText(text);
        view.setTextSize(size);
        view.setTextColor(color);
        view.setTypeface(
                Typeface.DEFAULT,
                bold ? Typeface.BOLD : Typeface.NORMAL
        );
        view.setGravity(Gravity.CENTER_VERTICAL);
        return view;
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
