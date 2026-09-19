package com.veytrix.autopilot;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

/** Real account/connection surface. No synthetic usage metrics. */
public final class VeytrixProfileView extends LinearLayout {
    public interface Host {
        void openConnection();
        void openDrawer();
        void closeConnection();
    }

    private final VeytrixAutopilotClient client;
    private final TextView connection;
    private final TextView target;

    public VeytrixProfileView(Context context, VeytrixAutopilotClient client, Host host) {
        super(context);
        this.client = client;
        setOrientation(VERTICAL);
        setPadding(dp(14), dp(8), dp(14), dp(10));
        setBackgroundColor(VeytrixDesignTokens.PEARL);

        addHeader(host);

        LinearLayout statusRail = rail();
        connection = label(
                client.hasToken() ? "CONNECTED" : "NOT CONNECTED",
                12,
                client.hasToken() ? VeytrixDesignTokens.STATE_VERIFIED : VeytrixDesignTokens.STATE_IDLE,
                true
        );
        statusRail.addView(connection, new LayoutParams(0, dp(32), 1f));
        TextView access = label(
                client.hasToken() ? "GitHub credential present" : "GitHub credential not configured",
                10,
                VeytrixDesignTokens.TEXT_SECONDARY,
                false
        );
        access.setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL);
        statusRail.addView(access, new LayoutParams(0, dp(32), 1f));
        addView(statusRail, marginBottom(8));

        LinearLayout targetRail = rail();
        TextView caption = label("TARGET REPOSITORY", 9, VeytrixDesignTokens.TEXT_SECONDARY, true);
        targetRail.addView(caption, new LayoutParams(dp(132), dp(42)));
        target = label(
                client.getTargetRepository().isEmpty()
                        ? "Not configured"
                        : client.getTargetRepository(),
                11,
                VeytrixDesignTokens.TEXT_PRIMARY,
                true
        );
        target.setGravity(Gravity.CENTER_VERTICAL | Gravity.RIGHT);
        targetRail.addView(target, new LayoutParams(0, dp(42), 1f));
        addView(targetRail, marginBottom(10));

        TextView security = label(
                "Credentials are kept in the Android Keystore-backed secure store. "
                        + "The token is not displayed by this surface.",
                10,
                VeytrixDesignTokens.TEXT_SECONDARY,
                false
        );
        security.setBackground(round(
                VeytrixDesignTokens.WHITE, dp(16), VeytrixDesignTokens.SILVER
        ));
        security.setPadding(dp(12), dp(10), dp(12), dp(10));
        addView(security, new LayoutParams(-1, dp(70)));

        SpaceRow spacer = new SpaceRow(getContext());
        addView(spacer, new LayoutParams(-1, 0, 1f));

        Button connect = action("VERIFY / CHANGE CONNECTION");
        connect.setOnClickListener(v -> host.openConnection());
        addView(connect, marginBottom(8));

        Button clear = secondaryAction("CLEAR STORED GITHUB CREDENTIAL");
        clear.setOnClickListener(v -> {
            client.clearConnection();
            refresh();
            host.closeConnection();
        });
        addView(clear, marginBottom(4));
    }

    private void refresh() {
        boolean connected = client.hasToken();
        connection.setText(connected ? "CONNECTED" : "NOT CONNECTED");
        connection.setTextColor(
                connected ? VeytrixDesignTokens.STATE_VERIFIED : VeytrixDesignTokens.STATE_IDLE
        );
        target.setText(
                client.getTargetRepository().isEmpty()
                        ? "Not configured"
                        : client.getTargetRepository()
        );
    }

    private void addHeader(Host host) {
        LinearLayout row = new LinearLayout(getContext());
        row.setGravity(Gravity.CENTER_VERTICAL);
        Button menu = iconButton("☰");
        menu.setOnClickListener(v -> host.openDrawer());
        row.addView(menu, new LayoutParams(dp(48), dp(48)));
        TextView title = label("PROFILE", 22, VeytrixDesignTokens.TEXT_PRIMARY, true);
        title.setPadding(dp(8), 0, 0, 0);
        row.addView(title, new LayoutParams(0, dp(48), 1f));
        addView(row, new LayoutParams(-1, dp(48)));
    }

    private LinearLayout rail() {
        LinearLayout row = new LinearLayout(getContext());
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(12), dp(6), dp(12), dp(6));
        row.setBackground(round(
                VeytrixDesignTokens.WHITE, dp(15), VeytrixDesignTokens.SILVER
        ));
        return row;
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

    private Button secondaryAction(String value) {
        Button button = new Button(getContext());
        button.setText(value);
        button.setTextSize(10);
        button.setTextColor(VeytrixDesignTokens.CRIMSON);
        button.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        button.setAllCaps(false);
        button.setBackground(round(
                VeytrixDesignTokens.PEARL, dp(16), VeytrixDesignTokens.SILVER
        ));
        button.setMinHeight(dp(48));
        return button;
    }

    private Button iconButton(String value) {
        Button button = new Button(getContext());
        button.setText(value);
        button.setTextSize(21);
        button.setTextColor(VeytrixDesignTokens.TEXT_PRIMARY);
        button.setAllCaps(false);
        button.setBackgroundColor(Color.TRANSPARENT);
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

    private android.graphics.drawable.GradientDrawable round(int color, float radius, int stroke) {
        android.graphics.drawable.GradientDrawable drawable =
                new android.graphics.drawable.GradientDrawable();
        drawable.setColor(color);
        drawable.setCornerRadius(radius);
        if (stroke != Color.TRANSPARENT) drawable.setStroke(dp(1), stroke);
        return drawable;
    }

    private LayoutParams marginBottom(int value) {
        LayoutParams params = new LayoutParams(-1, dp(52));
        params.bottomMargin = dp(value);
        return params;
    }

    private final class SpaceRow extends View {
        SpaceRow(Context context) { super(context); }
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}