package com.veytrix.autopilot;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;

/** Native voice command capture using Android speech recognition when available. */
public final class VeytrixVoiceView extends LinearLayout {
    public interface Host {
        void openDrawer();
        void sendCommandToMission(String command);
    }

    private static final int RECORD_AUDIO_REQUEST = 4101;
    private final SpeechRecognizer recognizer;
    private final TextView status;
    private final TextView transcript;
    private final Button action;

    public VeytrixVoiceView(Context context, Host host) {
        super(context);
        setOrientation(VERTICAL);
        setGravity(Gravity.CENTER_HORIZONTAL);
        setPadding(dp(14), dp(8), dp(14), dp(10));
        setBackgroundColor(VeytrixDesignTokens.PEARL);

        LinearLayout header = new LinearLayout(context);
        header.setGravity(Gravity.CENTER_VERTICAL);
        TextView menu = text("☰", 21, VeytrixDesignTokens.TEXT_PRIMARY, true);
        menu.setGravity(Gravity.CENTER);
        menu.setOnClickListener(v -> host.openDrawer());
        header.addView(menu, new LayoutParams(dp(48), dp(48)));
        TextView title = text("VOICE COMMAND", 20, VeytrixDesignTokens.TEXT_PRIMARY, true);
        title.setPadding(dp(8), 0, 0, 0);
        header.addView(title, new LayoutParams(0, dp(48), 1f));
        addView(header, new LayoutParams(-1, dp(48)));

        status = text("READY TO LISTEN", 11,
                VeytrixDesignTokens.STATE_IDLE, true);
        status.setGravity(Gravity.CENTER);
        addView(status, new LayoutParams(-1, dp(42)));

        transcript = text(
                "Tap the control and speak a mission command.",
                14,
                VeytrixDesignTokens.TEXT_PRIMARY,
                false
        );
        transcript.setGravity(Gravity.CENTER);
        transcript.setPadding(dp(18), dp(12), dp(18), dp(12));
        transcript.setBackground(round(
                VeytrixDesignTokens.WHITE, dp(18), VeytrixDesignTokens.SILVER
        ));
        addView(transcript, new LayoutParams(-1, dp(110)));

        recognizer = SpeechRecognizer.isRecognitionAvailable(context)
                ? SpeechRecognizer.createSpeechRecognizer(context)
                : null;

        action = new Button(context);
        action.setText(recognizer == null ? "VOICE UNAVAILABLE" : "START LISTENING");
        action.setTextSize(11);
        action.setTextColor(VeytrixDesignTokens.WHITE);
        action.setAllCaps(false);
        action.setBackground(round(
                VeytrixDesignTokens.PURPLE, dp(18), VeytrixDesignTokens.PURPLE
        ));
        action.setMinHeight(dp(52));
        action.setEnabled(recognizer != null);
        action.setOnClickListener(v -> toggleRecognition(context));
        addView(action, marginTop(14));

        TextView note = text(
                "Voice text is passed into the real mission composer; no automatic execution is triggered.",
                10,
                VeytrixDesignTokens.TEXT_SECONDARY,
                false
        );
        note.setGravity(Gravity.CENTER);
        addView(note, marginTop(10));

        SpaceView spacer = new SpaceView(context);
        addView(spacer, new LayoutParams(-1, 0, 1f));

        if (recognizer != null) {
            recognizer.setRecognitionListener(new RecognitionListener() {
                @Override public void onReadyForSpeech(Bundle params) {
                    status.setText("LISTENING");
                    status.setTextColor(VeytrixDesignTokens.STATE_PLANNING);
                    action.setText("STOP LISTENING");
                }
                @Override public void onBeginningOfSpeech() {}
                @Override public void onRmsChanged(float rmsdB) {}
                @Override public void onBufferReceived(byte[] buffer) {}
                @Override public void onEndOfSpeech() {
                    status.setText("PROCESSING");
                    status.setTextColor(VeytrixDesignTokens.STATE_VERIFYING);
                    action.setText("START LISTENING");
                }
                @Override public void onError(int error) {
                    status.setText("VOICE INPUT ERROR");
                    status.setTextColor(VeytrixDesignTokens.STATE_FAILED);
                    action.setText("START LISTENING");
                }
                @Override public void onResults(Bundle results) {
                    ArrayList<String> matches =
                            results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                    if (matches == null || matches.isEmpty()) {
                        status.setText("NO COMMAND CAPTURED");
                        status.setTextColor(VeytrixDesignTokens.STATE_FAILED);
                        action.setText("START LISTENING");
                        return;
                    }
                    String command = matches.get(0).trim();
                    transcript.setText(command);
                    status.setText("COMMAND CAPTURED");
                    status.setTextColor(VeytrixDesignTokens.STATE_VERIFIED);
                    action.setText("START LISTENING");
                    host.sendCommandToMission(command);
                }
                @Override public void onPartialResults(Bundle partialResults) {}
                @Override public void onEvent(int eventType, Bundle params) {}
            });
        }

        setOnClickListener(v -> {
            if (recognizer != null) toggleRecognition(context);
        });
    }

    private void toggleRecognition(Context context) {
        if (recognizer == null) return;
        if (!(context instanceof Activity)) return;

        Activity activity = (Activity) context;
        if (activity.checkSelfPermission(Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            activity.requestPermissions(
                    new String[]{Manifest.permission.RECORD_AUDIO},
                    RECORD_AUDIO_REQUEST
            );
            return;
        }

        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
        );
        intent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false);
        recognizer.startListening(intent);
    }

    public void release() {
        if (recognizer != null) {
            recognizer.destroy();
        }
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

    private Button dummy() {
        return new Button(getContext());
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

    private LayoutParams marginTop(int value) {
        LayoutParams lp = new LayoutParams(-1, -2);
        lp.topMargin = dp(value);
        return lp;
    }

    private final class SpaceView extends android.view.View {
        SpaceView(Context context) { super(context); }
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}