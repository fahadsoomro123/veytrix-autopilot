package com.veytrix.autopilot;

import android.app.Activity;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.content.Context;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsControllerCompat;

/**
 * VEYTRIX flagship native Android surface.
 * Presentation and interaction are intentionally self-contained in this preview pass.
 */
public final class MainActivity extends Activity {
    private static final int BG = Color.rgb(6, 9, 11);
    private static final int PANEL = Color.rgb(15, 19, 20);
    private static final int PANEL_2 = Color.rgb(24, 28, 28);
    private static final int LINE = Color.rgb(64, 63, 55);
    private static final int WHITE = Color.rgb(247, 245, 238);
    private static final int MUTED = Color.rgb(171, 169, 157);
    private static final int DIM = Color.rgb(104, 106, 99);
    private static final int GOLD = Color.rgb(221, 194, 143);
    private static final int GOLD_LIGHT = Color.rgb(248, 228, 190);
    private static final int GREEN = Color.rgb(93, 210, 161);
    private static final int TEAL = Color.rgb(107, 201, 197);
    private static final int RED = Color.rgb(239, 110, 101);

    private FrameLayout root;
    private VeytrixView surface;
    private EditText prompt;
    private TextView send;
    private TextView mic;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        configureWindow();
        build();
    }

    private void configureWindow() {
        Window window = getWindow();
        WindowCompat.setDecorFitsSystemWindows(window, true);
        window.setStatusBarColor(Color.rgb(4, 7, 8));
        window.setNavigationBarColor(Color.rgb(4, 7, 8));
        WindowInsetsControllerCompat controller = WindowCompat.getInsetsController(window, window.getDecorView());
        controller.setAppearanceLightStatusBars(false);
        controller.setAppearanceLightNavigationBars(false);
    }

    private void build() {
        root = new FrameLayout(this);
        root.setBackgroundColor(BG);
        surface = new VeytrixView(this);
        root.addView(surface, new FrameLayout.LayoutParams(-1, -1));

        prompt = new EditText(this);
        prompt.setTextColor(WHITE);
        prompt.setHintTextColor(Color.rgb(132, 136, 132));
        prompt.setTextSize(13.5f);
        prompt.setHint("Describe your goal in detail...");
        prompt.setGravity(Gravity.TOP | Gravity.START);
        prompt.setSingleLine(false);
        prompt.setPadding(dp(14), dp(13), dp(56), dp(34));
        prompt.setBackground(roundDrawable(Color.rgb(27, 30, 29), GOLD, 16));
        prompt.setVisibility(View.VISIBLE);
        root.addView(prompt, new FrameLayout.LayoutParams(dp(338), dp(112)));

        mic = new TextView(this);
        mic.setText("♩");
        mic.setTextColor(WHITE);
        mic.setTextSize(18);
        mic.setGravity(Gravity.CENTER);
        mic.setBackground(roundDrawable(Color.TRANSPARENT, Color.TRANSPARENT, 12));
        mic.setOnClickListener(v -> Toast.makeText(this, "Voice input preview", Toast.LENGTH_SHORT).show());
        root.addView(mic, new FrameLayout.LayoutParams(dp(38), dp(38)));

        send = new TextView(this);
        send.setText("↑");
        send.setTextColor(Color.rgb(31, 29, 23));
        send.setTextSize(22);
        send.setGravity(Gravity.CENTER);
        send.setTypeface(Typeface.DEFAULT_BOLD);
        send.setBackground(roundDrawable(Color.rgb(248, 226, 181), Color.rgb(255, 243, 213), 30));
        send.setOnClickListener(v -> startMission());
        root.addView(send, new FrameLayout.LayoutParams(dp(48), dp(48)));

        positionComposer();
        setContentView(root);
    }

    private void positionComposer() {
        final int y = dp(428);
        FrameLayout.LayoutParams p = (FrameLayout.LayoutParams) prompt.getLayoutParams();
        p.leftMargin = dp(26); p.topMargin = y; prompt.setLayoutParams(p);
        FrameLayout.LayoutParams m = (FrameLayout.LayoutParams) mic.getLayoutParams();
        m.leftMargin = dp(34); m.topMargin = y + dp(63); mic.setLayoutParams(m);
        FrameLayout.LayoutParams s = (FrameLayout.LayoutParams) send.getLayoutParams();
        s.leftMargin = dp(306); s.topMargin = y + dp(54); send.setLayoutParams(s);
    }

    private void startMission() {
        String value = prompt.getText().toString().trim();
        if (value.isEmpty()) {
            prompt.requestFocus();
            ((InputMethodManager) getSystemService(INPUT_METHOD_SERVICE)).showSoftInput(prompt, InputMethodManager.SHOW_IMPLICIT);
            Toast.makeText(this, "Write a mission first", Toast.LENGTH_SHORT).show();
            return;
        }
        surface.mission = value;
        surface.page = VeytrixView.RUNS;
        prompt.clearFocus();
        prompt.setVisibility(View.GONE);
        send.setVisibility(View.GONE);
        mic.setVisibility(View.GONE);
        surface.invalidate();
        ((InputMethodManager) getSystemService(INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(prompt.getWindowToken(), 0);
    }

    private void showComposer() {
        prompt.setVisibility(View.VISIBLE);
        send.setVisibility(View.VISIBLE);
        mic.setVisibility(View.VISIBLE);
        positionComposer();
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private GradientDrawable roundDrawable(int fill, int stroke, float radius) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(fill);
        d.setCornerRadius(dp((int) radius));
        if (stroke != Color.TRANSPARENT) d.setStroke(dp(1), stroke);
        return d;
    }

    private final class VeytrixView extends View {
        static final int HOME = 0;
        static final int RUNS = 1;
        static final int DETAILS = 2;
        static final int ARTIFACTS = 3;
        static final int TOOLS = 4;
        static final int PROFILE = 5;
        static final int SETTINGS = 6;
        static final int VOICE = 7;
        static final int COMPLETED = 8;

        private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint stroke = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Handler handler = new Handler();
        private int page = HOME;
        private boolean drawer = false;
        private boolean paused = false;
        private boolean completed = false;
        private float pulse = 0f;
        private String mission = "";

        VeytrixView(Context context) {
            super(context);
            p.setTypeface(Typeface.create("sans", Typeface.NORMAL));
            stroke.setStyle(Paint.Style.STROKE);
            stroke.setStrokeWidth(1f);
            setLayerType(View.LAYER_TYPE_SOFTWARE, null);
            handler.post(frame);
        }

        private final Runnable frame = new Runnable() {
            @Override public void run() {
                pulse += 0.035f;
                invalidate();
                handler.postDelayed(this, 42);
            }
        };

        @Override protected void onDraw(Canvas c) {
            super.onDraw(c);
            c.drawColor(BG);
            if (page == HOME) drawHome(c);
            else if (page == RUNS) drawRuns(c);
            else if (page == DETAILS) drawDetails(c);
            else if (page == ARTIFACTS) drawArtifacts(c);
            else if (page == TOOLS) drawTools(c);
            else if (page == PROFILE) drawProfile(c);
            else if (page == SETTINGS) drawSettings(c);
            else if (page == VOICE) drawVoice(c);
            else if (page == COMPLETED) drawCompleted(c);
            drawBottomNav(c);
            if (drawer) drawDrawer(c);
        }

        private float sx() { return getWidth() / 390f; }
        private float X(float n) { return n * sx(); }
        private float Y(float n) { return n * sx(); }

        private void top(Canvas c, String title, boolean back) {
            p.setShader(null); p.setColor(Color.rgb(7, 10, 11));
            c.drawRect(0, 0, getWidth(), Y(70), p);
            if (back) {
                text(c, "‹", 22, WHITE, 22, 41, true);
            } else {
                round(c, 18, 12, 58, 52, Color.rgb(20, 24, 24), LINE, 15);
                text(c, "☰", 21, WHITE, 28, 34, true);
            }
            text(c, "VEYTRIX", 15, GOLD_LIGHT, 82, 29, true);
            text(c, "AUTOPILOT", 8, DIM, 83, 45, true);
            if (!back) {
                round(c, 323, 17, 369, 44, Color.rgb(39, 37, 31), GOLD, 16);
                text(c, "Pro", 11, GOLD_LIGHT, 346, 35, true);
            } else {
                text(c, title, 16, WHITE, 55, 36, true);
            }
        }

        private void drawHome(Canvas c) {
            top(c, "", false);
            text(c, "READY FOR YOUR NEXT MISSION", 9, GOLD, 195, 106, true);
            drawCore(c, 195, 205, 76, 1.0f);
            text(c, "What should VEYTRIX", 21, WHITE, 195, 322, true);
            text(c, "accomplish?", 21, WHITE, 195, 348, true);
            text(c, "Turn your goal into a traceable execution.", 11, MUTED, 195, 372, true);
            round(c, 26, 418, 364, 548, PANEL_2, GOLD, 17);
            text(c, "MISSION PROMPT", 8, GOLD, 42, 437, true);
            text(c, "Add context, attachments, or a voice instruction", 9.5f, DIM, 42, 528, false);
            text(c, "•", 14, WHITE, 48, 532, true);
            text(c, "Start typing above...", 9, DIM, 64, 532, false);
            miniStatus(c, 26, 570, "LOCAL WORKSPACE", "READY", GREEN);
            miniStatus(c, 206, 570, "AUTOPILOT", "ONLINE", GOLD);
        }

        private void drawRuns(Canvas c) {
            top(c, "", false);
            if (completed) { drawCompleted(c); return; }
            round(c, 18, 89, 132, 120, Color.rgb(16, 35, 30), Color.rgb(47, 118, 88), 18);
            circle(c, 35, 104, 6, GREEN);
            text(c, "EXECUTING", 10, GREEN, 51, 110, true);
            text(c, "00:02:17", 10, MUTED, 337, 110, true);
            text(c, mission.isEmpty() ? "Create a research report on renewable energy trends" : mission, 14, WHITE, 24, 147, false);
            drawCore(c, 195, 285, 64, 1.0f);
            checklist(c, 24, 382);
            round(c, 24, 560, 366, 614, Color.rgb(247, 225, 181), GOLD_LIGHT, 26);
            text(c, paused ? "Resume Mission" : "Pause Mission", 13, Color.rgb(34, 32, 28), 195, 593, true);
            round(c, 24, 628, 366, 674, PANEL_2, LINE, 22);
            text(c, "Open run details", 12, WHITE, 195, 656, true);
        }

        private void drawDetails(Canvas c) {
            top(c, "Mission Run", true);
            round(c, 18, 85, 372, 146, PANEL, LINE, 18);
            round(c, 30, 98, 132, 124, Color.rgb(20, 49, 40), Color.rgb(55, 140, 105), 14);
            circle(c, 43, 111, 5, GREEN);
            text(c, "COMPLETED", 9, GREEN, 54, 115, true);
            text(c, "2m 14s", 10, MUTED, 336, 115, true);
            text(c, mission.isEmpty() ? "Research report on renewable energy trends" : mission, 13, WHITE, 30, 139, false);
            text(c, "Timeline", 10, GOLD_LIGHT, 42, 175, true);
            text(c, "Files", 10, MUTED, 120, 175, true);
            text(c, "Summary", 10, MUTED, 167, 175, true);
            timeline(c, 32, 211);
        }

        private void drawArtifacts(Canvas c) {
            top(c, "Artifacts", true);
            chip(c, 18, 84, 66, "All", true); chip(c, 90, 84, 80, "Docs", false); chip(c, 176, 84, 88, "Images", false); chip(c, 270, 84, 86, "Code", false);
            artifact(c, 18, 134, Color.rgb(239, 93, 79), "Research_Report.pdf", "2.4 MB · Today 09:43");
            artifact(c, 18, 198, Color.rgb(80, 205, 157), "Market_Analysis.xlsx", "1.1 MB · Today 09:40");
            artifact(c, 18, 262, Color.rgb(88, 155, 223), "Summary.md", "12 KB · Today 09:38");
            artifact(c, 18, 326, Color.rgb(92, 151, 239), "Chart.png", "542 KB · Today 09:36");
            artifact(c, 18, 390, Color.rgb(73, 186, 235), "Code_Snippet.py", "4 KB · Today 09:35");
            text(c, "24 outputs", 10, DIM, 345, 452, true);
        }

        private void drawTools(Canvas c) {
            top(c, "Tools", true);
            text(c, "POWERFUL BUILT-IN TOOLS", 9, GOLD, 20, 95, true);
            tool(c, 18, 112, "⌕", "Web Search", "Research & gather", GOLD);
            tool(c, 202, 112, "<>" , "Code Runner", "Execute code", TEAL);
            tool(c, 18, 220, "▣", "File Manager", "Organize files", GOLD_LIGHT);
            tool(c, 202, 220, "◉", "Browser", "Automate web", TEAL);
            tool(c, 18, 328, "▥", "Data Analyzer", "Analyze data", GOLD);
            tool(c, 202, 328, "▣", "Image Generator", "Create visuals", GREEN);
            tool(c, 18, 436, "▤", "Report Builder", "Generate docs", GOLD_LIGHT);
            tool(c, 202, 436, "•••", "More Tools", "Coming soon", DIM);
        }

        private void drawProfile(Canvas c) {
            top(c, "Profile", true);
            round(c, 18, 86, 372, 212, PANEL, LINE, 20);
            circle(c, 58, 126, 30, Color.rgb(177, 157, 116));
            text(c, "V", 24, Color.rgb(31, 28, 22), 58, 134, true);
            text(c, "VEYTRIX USER", 9, GOLD, 104, 118, true);
            text(c, "Local workspace", 16, WHITE, 104, 144, true);
            pill(c, 300, 108, 352, 130, "PRO", GOLD);
            text(c, "48", 23, WHITE, 82, 196, true); text(c, "36", 23, WHITE, 195, 196, true); text(c, "12h", 23, WHITE, 309, 196, true);
            text(c, "MISSIONS", 8, DIM, 82, 214, true); text(c, "ARTIFACTS", 8, DIM, 195, 214, true); text(c, "SAVED TIME", 8, DIM, 309, 214, true);
            row(c, 18, 320, "⌂", "Account Settings", "Profile & preferences");
            row(c, 18, 378, "◫", "Usage & Billing", "Plan and usage");
            row(c, 18, 436, "?", "Help & Support", "Get help with VEYTRIX");
            row(c, 18, 494, "◈", "Privacy & Security", "Your data controls");
        }

        private void drawSettings(Canvas c) {
            top(c, "Settings", true);
            setting(c, 18, 92, "☾", "Dark Mode", "Always on", true);
            setting(c, 18, 148, "♩", "Voice Input", "Ready for hands-free missions", true);
            setting(c, 18, 204, "♢", "Notifications", "Mission events and results", true);
            row(c, 18, 276, "✦", "AI Models", "Choose planning and execution models");
            row(c, 18, 334, "◈", "Privacy & Security", "Permissions, storage and policy");
            row(c, 18, 392, "◉", "Appearance", "Theme, motion and density");
            row(c, 18, 450, "i", "About VEYTRIX", "Autonomous execution platform");
        }

        private void drawVoice(Canvas c) {
            top(c, "Voice Input", true);
            text(c, "SPEAK YOUR MISSION CLEARLY", 9, GOLD, 195, 105, true);
            drawVoiceCore(c, 195, 292);
            text(c, "Listening...", 22, WHITE, 195, 445, true);
            text(c, "Say what you want VEYTRIX to accomplish.", 11, MUTED, 195, 470, true);
            round(c, 166, 514, 224, 572, PANEL_2, LINE, 29);
            text(c, "×", 24, WHITE, 195, 551, true);
        }

        private void drawCompleted(Canvas c) {
            top(c, "", false);
            drawCheckCore(c, 195, 235);
            text(c, "Mission Completed", 22, WHITE, 195, 390, true);
            text(c, "Your research report has been", 11, MUTED, 195, 420, true);
            text(c, "generated successfully.", 11, MUTED, 195, 438, true);
            round(c, 24, 474, 366, 528, Color.rgb(247, 225, 181), GOLD_LIGHT, 27);
            text(c, "View Results", 13, Color.rgb(34, 32, 27), 195, 507, true);
            round(c, 24, 540, 366, 592, PANEL_2, LINE, 26);
            text(c, "Start New Mission", 12, WHITE, 195, 573, true);
        }

        private void drawBottomNav(Canvas c) {
            round(c, 8, 690, 382, 760, Color.rgb(10, 14, 15), Color.rgb(33, 38, 37), 24);
            navItem(c, 40, "⌂", "Home", page == HOME);
            navItem(c, 117, "◌", "Runs", page == RUNS || page == DETAILS,);
            navItem(c, 195, "▣", "Artifacts", page == ARTIFACTS);
            navItem(c, 273, "⌘", "Tools", page == TOOLS);
            navItem(c, 350, "◯", "Profile", page == PROFILE || page == SETTINGS || page == VOICE,);
        }

        private void drawDrawer(Canvas c) {
            p.setColor(Color.argb(150, 0, 0, 0)); c.drawRect(0, 0, getWidth(), getHeight(), p);
            round(c, 0, 0, 320, 760, Color.rgb(13, 17, 18), Color.rgb(47, 50, 46), 0);
            text(c, "VEYTRIX", 17, GOLD_LIGHT, 30, 42, true);
            text(c, "AUTOPILOT", 8, DIM, 31, 59, true);
            round(c, 22, 83, 298, 140, PANEL_2, LINE, 18);
            circle(c, 53, 111, 19, Color.rgb(186, 164, 122));
            text(c, "V", 15, Color.rgb(36, 31, 23), 53, 117, true);
            text(c, "Local workspace", 12, WHITE, 83, 106, true);
            text(c, "PRO PLAN", 8, GOLD, 83, 124, true);
            drawerRow(c, 166, "⌂", "Home", HOME); drawerRow(c, 218, "◌", "Mission History", RUNS);
            drawerRow(c, 270, "▣", "Artifacts", ARTIFACTS); drawerRow(c, 322, "⌘", "Tools", TOOLS);
            drawerRow(c, 374, "⚙", "Settings", SETTINGS); drawerRow(c, 426, "♩", "Voice Input", VOICE);
            drawerRow(c, 478, "✓", "Verification", COMPLETED); drawerRow(c, 530, "◈", "Privacy & Security", SETTINGS);
            round(c, 22, 610, 298, 674, Color.rgb(31, 30, 26), GOLD, 20);
            text(c, "✦", 15, GOLD_LIGHT, 43, 649, true);
            text(c, "Pro workspace", 12, WHITE, 64, 643, true);
            text(c, "Unlock more power", 9, MUTED, 64, 658, false);
            text(c, "›", 20, GOLD_LIGHT, 279, 651, true);
        }

        private void drawerRow(Canvas c, float y, String icon, String label, int target) {
            boolean active = page == target;
            if (active) round(c, 18, y - 24, 302, y + 20, Color.rgb(29, 32, 31), Color.TRANSPARENT, 14);
            text(c, icon, 16, active ? GOLD_LIGHT : MUTED, 35, y + 2, true);
            text(c, label, 12, active ? WHITE : MUTED, 62, y + 2, true);
        }

        private void drawCore(Canvas c, float cx, float cy, float r, float scale) {
            float rr = r * (1f + (float) Math.sin(pulse) * 0.015f);
            p.setShader(new RadialGradient(X(cx), Y(cy), X(rr * 1.65f), new int[]{Color.argb(80, 228, 194, 128), Color.argb(20, 228, 194, 128), Color.TRANSPARENT}, null, Shader.TileMode.CLAMP));
            c.drawCircle(X(cx), Y(cy), X(rr * 1.65f), p);
            p.setShader(null);
            stroke.setColor(Color.argb(150, 215, 184, 117));
            stroke.setStrokeWidth(X(1));
            RectF ring = new RectF(X(cx - rr * 1.7f), Y(cy - rr * .72f), X(cx + rr * 1.7f), Y(cy + rr * .72f));
            c.save(); c.rotate(-15, X(cx), Y(cy)); c.drawOval(ring, stroke); c.restore();
            RectF ring2 = new RectF(X(cx - rr * 1.25f), Y(cy - rr * 1.6f), X(cx + rr * 1.25f), Y(cy + rr * 1.6f));
            stroke.setColor(Color.argb(90, 215, 184, 117));
            c.save(); c.rotate(19, X(cx), Y(cy)); c.drawOval(ring2, stroke); c.restore();
            p.setShader(new RadialGradient(X(cx - rr * .30f), Y(cy - rr * .38f), X(rr * 1.15f),
                    new int[]{Color.rgb(255, 248, 225), Color.rgb(224, 198, 143), Color.rgb(116, 88, 43), Color.rgb(24, 21, 17)},
                    new float[]{0f, .18f, .52f, 1f}, Shader.TileMode.CLAMP));
            c.drawCircle(X(cx), Y(cy), X(rr), p); p.setShader(null);
            p.setColor(Color.argb(180, 255, 248, 224));
            c.drawCircle(X(cx - rr * .28f), Y(cy - rr * .33f), X(rr * .16f), p);
            p.setColor(Color.argb(160, 255, 239, 196));
            c.drawCircle(X(cx + rr * .20f), Y(cy + rr * .18f), X(rr * .11f), p);
            text(c, "V", Math.max(16, rr * .43f), Color.rgb(250, 238, 211), cx, cy + rr * .20f, true);
        }

        private void drawVoiceCore(Canvas c, float cx, float cy) {
            for (int i = 0; i < 4; i++) {
                stroke.setColor(Color.argb(90 - i * 14, 225, 190, 124));
                stroke.setStrokeWidth(X(1));
                c.drawCircle(X(cx), Y(cy), X(70 + i * 19), stroke);
            }
            drawCore(c, cx, cy, 54, 1);
            round(c, 178, 250, 212, 330, Color.rgb(17, 18, 17), GOLD, 18);
            text(c, "♩", 28, GOLD_LIGHT, 195, 299, true);
        }

        private void drawCheckCore(Canvas c, float cx, float cy) {
            p.setShader(new RadialGradient(X(cx), Y(cy), X(90), new int[]{Color.argb(70, 228, 194, 128), Color.TRANSPARENT}, null, Shader.TileMode.CLAMP));
            c.drawCircle(X(cx), Y(cy), X(90), p); p.setShader(null);
            stroke.setColor(GOLD_LIGHT); stroke.setStrokeWidth(X(3));
            c.drawCircle(X(cx), Y(cy), X(55), stroke); c.drawCircle(X(cx), Y(cy), X(70), stroke);
            Path path = new Path(); path.moveTo(X(cx - 20), Y(cy + 3)); path.lineTo(X(cx - 5), Y(cy + 18)); path.lineTo(X(cx + 28), Y(cy - 20));
            c.drawPath(path, stroke);
        }

        private void checklist(Canvas c, float x, float y) {
            String[] rows = {"Understanding objective", "Planning approach", "Preparing environment", "Executing tasks", "Verifying results"};
            int[] cols = {1,1,1,0,0};
            for (int i = 0; i < rows.length; i++) {
                float yy = y + i * 30;
                circle(c, x + 6, yy, 4, i < 3 ? GOLD_LIGHT : (i == 3 ? TEAL : DIM));
                text(c, rows[i], 11, i < 3 ? MUTED : DIM, x + 20, yy + 4, false);
                if (cols[i] == 1) text(c, "✓", 11, GREEN, 335, yy + 4, true);
            }
        }

        private void timeline(Canvas c, float x, float y) {
            stroke.setColor(Color.argb(150, 110, 205, 163)); stroke.setStrokeWidth(X(1.5f));
            c.drawLine(X(x + 12), Y(y), X(x + 12), Y(y + 235), stroke);
            String[] titles = {"Request received", "Planning complete", "Data collection", "Content generation", "Verification", "Completed"};
            String[] subs = {"Mission initialized", "6 steps generated", "Sources analyzed (12)", "Report created", "Quality check passed", "All tasks finished"};
            for (int i = 0; i < titles.length; i++) {
                float yy = y + i * 40;
                circle(c, x + 12, yy, 4, i == 0 || i >= 3 ? GREEN : GOLD_LIGHT);
                text(c, titles[i], 10.5f, WHITE, x + 28, yy + 2, false);
                text(c, subs[i], 8.5f, DIM, x + 28, yy + 15, false);
                text(c, String.format("09:%02d", 41 + i), 8, MUTED, 340, yy + 3, true);
            }
        }

        private void artifact(Canvas c, float x, float y, int color, String name, String meta) {
            round(c, x, y, 372, y + 52, Color.rgb(12, 17, 18), Color.rgb(30, 35, 34), 15);
            round(c, x + 10, y + 10, x + 42, y + 42, Color.rgb(Color.red(color) / 2 + 40, Color.green(color) / 2 + 40, Color.blue(color) / 2 + 40), color, 9);
            text(c, "▤", 16, WHITE, x + 26, y + 31, true);
            text(c, name, 10.5f, WHITE, x + 54, y + 22, false);
            text(c, meta, 8.3f, DIM, x + 54, y + 38, false);
            text(c, "⋮", 17, MUTED, 350, y + 29, true);
        }

        private void tool(Canvas c, float x, float y, String icon, String title, String sub, int accent) {
            round(c, x, y, x + 170, y + 96, Color.rgb(18, 22, 22), Color.rgb(41, 44, 40), 18);
            round(c, x + 10, y + 10, x + 47, y + 47, Color.rgb(30, 32, 29), accent, 11);
            text(c, icon, 16, accent, x + 28, y + 35, true);
            text(c, title, 11.5f, WHITE, x + 12, y + 67, false);
            text(c, sub, 8.3f, DIM, x + 12, y + 82, false);
        }

        private void setting(Canvas c, float x, float y, String icon, String title, String sub, boolean on) {
            round(c, x, y, 372, y + 48, Color.rgb(16, 20, 20), Color.rgb(39, 43, 41), 15);
            text(c, icon, 14, GOLD_LIGHT, x + 19, y + 29, true);
            text(c, title, 11, WHITE, x + 42, y + 20, false);
            text(c, sub, 8.5f, DIM, x + 42, y + 35, false);
            pill(c, 325, y + 11, 359, y + 37, on ? "ON" : "OFF", on ? GOLD : DIM);
        }

        private void row(Canvas c, float x, float y, String icon, String title, String sub) {
            round(c, x, y, 372, y + 48, Color.rgb(15, 19, 19), Color.rgb(37, 42, 40), 15);
            text(c, icon, 14, MUTED, x + 19, y + 29, true);
            text(c, title, 11, WHITE, x + 42, y + 20, false);
            text(c, sub, 8.5f, DIM, x + 42, y + 35, false);
            text(c, "›", 18, MUTED, 351, y + 30, true);
        }

        private void chip(Canvas c, float x, float y, float w, String label, boolean active) {
            round(c, x, y, x + w, y + 36, active ? Color.rgb(247, 225, 181) : PANEL_2, active ? GOLD_LIGHT : LINE, 18);
            text(c, label, 10, active ? Color.rgb(31, 29, 23) : MUTED, x + w / 2f, y + 23, true);
        }

        private void navItem(Canvas c, float x, String icon, String label, boolean active) {
            if (active) round(c, x - 29, 699, x + 29, 751, Color.rgb(28, 31, 29), Color.TRANSPARENT, 19);
            text(c, icon, 18, active ? GOLD_LIGHT : DIM, x, 719, true);
            text(c, label, 8.5f, active ? WHITE : DIM, x, 743, true);
        }

        private void miniStatus(Canvas c, float x, float y, String left, String right, int color) {
            round(c, x, y, x + 158, y + 42, Color.rgb(14, 18, 18), Color.rgb(36, 40, 37), 13);
            text(c, left, 8, DIM, x + 12, y + 17, true);
            text(c, right, 9, color, x + 12, y + 31, true);
        }

        private void pill(Canvas c, float l, float t, float r, float b, String label, int color) {
            round(c, l, t, r, b, Color.rgb(33, 31, 26), color, 12);
            text(c, label, 8.5f, color, (l + r) / 2f, t + 17, true);
        }

        private void round(Canvas c, float l, float t, float r, float b, int fill, int border, float radius) {
            p.setShader(null); p.setStyle(Paint.Style.FILL); p.setColor(fill);
            RectF rr = new RectF(X(l), Y(t), X(r), Y(b));
            c.drawRoundRect(rr, X(radius), X(radius), p);
            if (border != Color.TRANSPARENT) {
                p.setStyle(Paint.Style.STROKE); p.setStrokeWidth(X(1)); p.setColor(border); c.drawRoundRect(rr, X(radius), X(radius), p); p.setStyle(Paint.Style.FILL);
            }
        }

        private void circle(Canvas c, float x, float y, float r, int color) { p.setShader(null); p.setColor(color); c.drawCircle(X(x), Y(y), X(r), p); }

        private void text(Canvas c, String s, float size, int color, float x, float y, boolean center) {
            p.setShader(null); p.setColor(color); p.setTextSize(X(size)); p.setTypeface(Typeface.create("sans", Typeface.NORMAL));
            if (size >= 14) p.setTypeface(Typeface.create("sans", Typeface.BOLD));
            p.setTextAlign(center ? Paint.Align.CENTER : Paint.Align.LEFT);
            c.drawText(s, X(x), Y(y), p);
        }

        @Override public boolean onTouchEvent(MotionEvent e) {
            if (e.getAction() != MotionEvent.ACTION_UP) return true;
            float x = e.getX() / sx();
            float y = e.getY() / sx();
            if (drawer) {
                if (x > 320) { drawer = false; invalidate(); return true; }
                if (y > 145 && y < 205) select(HOME);
                else if (y < 255 && y > 200) select(RUNS);
                else if (y < 315 && y > 255) select(ARTIFACTS);
                else if (y < 365 && y > 315) select(TOOLS);
                else if (y < 418 && y > 365) select(SETTINGS);
                else if (y < 470 && y > 418) select(VOICE);
                else if (y < 520 && y > 470) select(COMPLETED);
                return true;
            }
            if (y < 68 && x < 70 && page != COMPLETED) { drawer = true; hideComposer(); invalidate(); return true; }
            if (y > 680) {
                if (x < 78) select(HOME);
                else if (x < 156) select(RUNS);
                else if (x < 234) select(ARTIFACTS);
                else if (x < 312) select(TOOLS);
                else select(PROFILE);
                return true;
            }
            if (page == RUNS && y > 550 && y < 620) { paused = !paused; invalidate(); return true; }
            if (page == RUNS && y > 620 && y < 680) { select(DETAILS); return true; }
            if (page == COMPLETED && y > 530 && y < 610) { select(HOME); showComposer(); return true; }
            if (page == PROFILE && y > 365 && y < 435) { select(SETTINGS); return true; }
            if (page == VOICE && y > 500) { select(HOME); showComposer(); return true; }
            return true;
        }

        private void hideComposer() {
            prompt.setVisibility(View.GONE); send.setVisibility(View.GONE); mic.setVisibility(View.GONE);
        }

        private void showComposer() { MainActivity.this.showComposer(); }

        private void select(int target) {
            page = target; drawer = false;
            if (target == HOME) showComposer(); else hideComposer();
            invalidate();
        }
    }
}