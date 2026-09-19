package com.veytrix.autopilot;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

/** Explicit support/status surface; no claims of capabilities that are not wired. */
public final class VeytrixSupportView extends LinearLayout {
    public interface Host {
        void openDrawer();
        void openConnection();
        void openControl();
    }

    public VeytrixSupportView(Context context, VeytrixAutopilotClient client, Host host) {
        super(context);
        setOrientation(VERTICAL);
        setPadding(dp(14), dp(8), dp(14), dp(10));
        setBackgroundColor(VeytrixDesignTokens.PEARL);

        LinearLayout header = new LinearLayout(context);
        header.setGravity(Gravity.CENTER_VERTICAL);
        TextView menu = text("☰", 21, VeytrixDesignTokens.TEXT_PRIMARY, true);
        menu.setGravity(Gravity.CENTER);
        menu.setOnClickListener(v -> host.openDrawer());
        header.addView(menu, new LayoutParams(dp(48), dp(48)));
        TextView title = text("HELP & SUPPORT", 20, VeytrixDesignTokens.TEXT_PRIMARY, true);
        title.setPadding(dp(8), 0, 0, 0);
        header.addView(title, new LayoutParams(0, dp(48), 1f));
        addView(header, new LayoutParams(-1, dp(48)));

        addView(info(
                "CONNECTION",
                client.hasToken()
                        ? "GitHub credential is configured. Use Profile to verify or change the target."
                        : "No GitHub credential is configured. Connect before launching a mission."
        ), marginBottom(8));

        addView(info(
                "AUTOPILOT CONTROL",
                "Engine selection and verification depth are available from Control."
        ), marginBottom(8));

        addView(info(
                "SECURITY",
                "The Android client refuses cleartext transport and uses the secure credential store."
        ), marginBottom(8));

        View spacer = new View(context);
        addView(spacer, new LayoutParams(-1, 0, 1f));

        Button connection = action("OPEN GITHUB CONNECTION");
        connection.setOnClickListener(v -> host.openConnection());
        addView(connection, marginBottom(8));

        Button control = secondary("OPEN CONTROL");
        control.setOnClickListener(v -> host.openControl());
        addView(control, marginBottom(4));
    }

    private LinearLayout info(String title, String body) {
        LinearLayout card = new LinearLayout(getContext());
        card.setOrientation(VERTICAL);
        card.setPadding(dp(12), dp(10), dp(12), dp(10));
        card.setBackground(round(
                VeytrixDesignTokens.WHITE, dp(16), VeytrixDesignTokens.SILVER
        ));
        card.addView(text(title, 9, VeytrixDesignTokens.TEXT_SECONDARY, true),
                new LayoutParams(-1, dp(20)));
        card.addView(text(body, 11, VeytrixDesignTokens.TEXT_PRIMARY, false),
                new LayoutParams(-1, dp(42)));
        return card;
    }

    private Button action(String value) {
        Button button = new Button(getContext());
        button.setText(value);
        button.setTextSize(11);
        button.setTextColor(VeytrixDesignTokens.WHITE);
        button.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        button.setAllCaps(false);
        button.setBackground(round(
                VeytrixDesignTokens.PURPLE, dp(16), VeytrixDesignTokens.PURPLE
        ));
        button.setMinHeight(dp(50));
        return button;
    }

    private Button secondary(String value) {
        Button button = new Button(getContext());
        button.setText(value);
        button.setTextSize(10);
        button.setTextColor(VeytrixDesignTokens.VIOLET);
        button.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        button.setAllCaps(false);
        button.setBackground(round(
                VeytrixDesignTokens.PEARL, dp(16), VeytrixDesignTokens.SILVER
        ));
        button.setMinHeight(dp(48));
        return button;
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