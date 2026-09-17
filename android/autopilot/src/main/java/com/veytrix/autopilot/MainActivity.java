package com.veytrix.autopilot;

import android.app.Activity;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsControllerCompat;

/** Native VEYTRIX flagship Android surface. */
public final class MainActivity extends Activity {
    private static final int BG = Color.rgb(2, 8, 17);
    private static final int HOME_PANEL = Color.rgb(7, 22, 39);
    private static final int HOME_PANEL_2 = Color.rgb(8, 27, 47);
    private static final int HOME_BLUE = Color.rgb(90, 156, 255);
    private static final int HOME_CYAN = Color.rgb(74, 220, 255);
    private static final int HOME_PURPLE = Color.rgb(151, 91, 255);
    private static final int HOME_TEXT = Color.rgb(243, 247, 255);
    private static final int HOME_MUTED = Color.rgb(166, 191, 222);
    private static final int HOME_GREEN = Color.rgb(39, 229, 176);
    private static final int HOME_BORDER = Color.rgb(72, 121, 196);

    private FrameLayout root;
    private VeytrixView surface;
    private FlagshipCoreView coreView;
    private EditText prompt;
    private TextView contextAction;
    private TextView mic;
    private TextView attachAction;
    private TextView send;
    private TextView modeAction;
    private boolean deepMode = true;

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        configureWindow();
        build();
    }

    private void configureWindow() {
        Window window = getWindow();
        WindowCompat.setDecorFitsSystemWindows(window, true);
        window.setStatusBarColor(Color.rgb(2, 7, 14));
        window.setNavigationBarColor(Color.rgb(2, 7, 14));
        window.setSoftInputMode(android.view.WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        WindowInsetsControllerCompat controller = WindowCompat.getInsetsController(window, window.getDecorView());
        controller.setAppearanceLightStatusBars(false);
        controller.setAppearanceLightNavigationBars(false);
    }

    private void build() {
        root = new FrameLayout(this);
        surface = new VeytrixView(this);
        surface.setContentDescription("VEYTRIX Home screen");
        root.addView(surface, new FrameLayout.LayoutParams(-1, -1));

        coreView = new FlagshipCoreView(this);
        coreView.setContentDescription("VEYTRIX autonomous core");
        root.addView(coreView, new FrameLayout.LayoutParams(dp(160), dp(170)));

        prompt = new EditText(this);
        prompt.setTextColor(HOME_TEXT);
        prompt.setHintTextColor(Color.rgb(153, 177, 209));
        prompt.setTextSize(15f);
        prompt.setHint("Describe your task in plain language...");
        prompt.setGravity(Gravity.TOP | Gravity.START);
        prompt.setSingleLine(false);
        prompt.setMaxLines(3);
        prompt.setHorizontallyScrolling(false);
        prompt.setVerticalScrollBarEnabled(false);
        prompt.setOverScrollMode(View.OVER_SCROLL_NEVER);
        prompt.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE | android.text.InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        prompt.setPadding(dp(14), dp(10), dp(10), dp(8));
        prompt.setBackgroundColor(Color.TRANSPARENT);
        root.addView(prompt, new FrameLayout.LayoutParams(dp(332), dp(60)));

        contextAction = transparentAction("Context options");
        contextAction.setOnClickListener(v -> Toast.makeText(this, "Context tools are not connected in this build", Toast.LENGTH_SHORT).show());
        root.addView(contextAction, new FrameLayout.LayoutParams(dp(76), dp(40)));

        mic = transparentAction("Voice input");
        mic.setOnClickListener(v -> Toast.makeText(this, "Voice input is not connected in this build", Toast.LENGTH_SHORT).show());
        root.addView(mic, new FrameLayout.LayoutParams(dp(76), dp(40)));

        attachAction = transparentAction("Attach files");
        attachAction.setOnClickListener(v -> Toast.makeText(this, "Attachments are not connected in this build", Toast.LENGTH_SHORT).show());
        root.addView(attachAction, new FrameLayout.LayoutParams(dp(76), dp(40)));

        send = transparentAction("Launch mission");
        send.setOnClickListener(v -> startMission());
        root.addView(send, new FrameLayout.LayoutParams(dp(92), dp(40)));

        modeAction = transparentAction("Mission mode");
        modeAction.setOnClickListener(v -> {
            deepMode = !deepMode;
            surface.invalidate();
        });
        root.addView(modeAction, new FrameLayout.LayoutParams(dp(82), dp(34)));

        setContentView(root);
        root.post(this::positionHomeControls);
    }

    private TextView transparentAction(String description) {
        TextView view = new TextView(this);
        view.setText("");
        view.setTextColor(Color.TRANSPARENT);
        view.setBackgroundColor(Color.TRANSPARENT);
        view.setContentDescription(description);
        view.setGravity(Gravity.CENTER);
        return view;
    }

    private float homeScale() {
        if (surface == null || surface.getWidth() <= 0 || surface.getHeight() <= 0) return 1f;
        return Math.min(surface.getWidth() / (390f * getResources().getDisplayMetrics().density),
                surface.getHeight() / (844f * getResources().getDisplayMetrics().density));
    }

    private void positionHomeControls() {
        if (surface == null || surface.getWidth() <= 0 || surface.getHeight() <= 0) return;
        float density = getResources().getDisplayMetrics().density;
        float scale = homeScale();
        float widthPx = 390f * scale * density;
        int xOffset = Math.round((surface.getWidth() - widthPx) * .5f);
        place(coreView, xOffset + Math.round(206f * scale * density), Math.round(93f * scale * density), Math.round(160f * scale * density), Math.round(170f * scale * density));
        place(prompt, xOffset + Math.round(29f * scale * density), Math.round(347f * scale * density), Math.round(332f * scale * density), Math.round(58f * scale * density));
        place(modeAction, xOffset + Math.round(290f * scale * density), Math.round(319f * scale * density), Math.round(82f * scale * density), Math.round(34f * scale * density));
        place(contextAction, xOffset + Math.round(27f * scale * density), Math.round(418f * scale * density), Math.round(76f * scale * density), Math.round(40f * scale * density));
        place(mic, xOffset + Math.round(110f * scale * density), Math.round(418f * scale * density), Math.round(76f * scale * density), Math.round(40f * scale * density));
        place(attachAction, xOffset + Math.round(193f * scale * density), Math.round(418f * scale * density), Math.round(76f * scale * density), Math.round(40f * scale * density));
        place(send, xOffset + Math.round(277f * scale * density), Math.round(418f * scale * density), Math.round(86f * scale * density), Math.round(40f * scale * density));
    }

    private void place(View view, int left, int top, int width, int height) {
        FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) view.getLayoutParams();
        lp.width = width;
        lp.height = height;
        lp.leftMargin = left;
        lp.topMargin = top;
        view.setLayoutParams(lp);
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
        hideComposer();
        surface.invalidate();
        ((InputMethodManager) getSystemService(INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(prompt.getWindowToken(), 0);
    }

    private void showComposer() {
        prompt.setVisibility(View.VISIBLE);
        contextAction.setVisibility(View.VISIBLE);
        mic.setVisibility(View.VISIBLE);
        attachAction.setVisibility(View.VISIBLE);
        send.setVisibility(View.VISIBLE);
        modeAction.setVisibility(View.VISIBLE);
        coreView.setVisibility(View.VISIBLE);
        positionHomeControls();
    }

    private void hideComposer() {
        prompt.setVisibility(View.GONE);
        contextAction.setVisibility(View.GONE);
        mic.setVisibility(View.GONE);
        attachAction.setVisibility(View.GONE);
        send.setVisibility(View.GONE);
        modeAction.setVisibility(View.GONE);
        coreView.setVisibility(View.GONE);
    }

    private final class VeytrixView extends View {
        static final int HOME = 0, RUNS = 1, DETAILS = 2, ARTIFACTS = 3, TOOLS = 4, PROFILE = 5, SETTINGS = 6, VOICE = 7, COMPLETED = 8;
        private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint stroke = new Paint(Paint.ANTI_ALIAS_FLAG);
        private int page = HOME;
        private boolean drawer = false;
        private boolean paused = false;
        private float pulse = 0f;
        private long animationStartNanos;
        private String mission = "";

        VeytrixView(Context context) {
            super(context);
            stroke.setStyle(Paint.Style.STROKE);
            stroke.setStrokeCap(Paint.Cap.ROUND);
            setLayerType(View.LAYER_TYPE_SOFTWARE, null);
            animationStartNanos = System.nanoTime();
        }

        private float hs() { return Math.min(getWidth() / 390f, getHeight() / 844f); }
        private float HX(float n) { return (getWidth() - 390f * hs()) * .5f + n * hs(); }
        private float HY(float n) { return n * hs(); }
        private float s() { return getWidth() / 390f; }
        private float X(float n) { return n * s(); }
        private float Y(float n) { return n * s(); }

        @Override protected void onAttachedToWindow() {
            super.onAttachedToWindow();
            animationStartNanos = System.nanoTime();
            postInvalidateOnAnimation();
        }

        @Override protected void onDetachedFromWindow() {
            removeCallbacksAndMessages();
            super.onDetachedFromWindow();
        }

        private void removeCallbacksAndMessages() { removeCallbacks(invalidateRunnable); }
        private final Runnable invalidateRunnable = this::invalidate;

        @Override protected void onSizeChanged(int w, int h, int oldw, int oldh) {
            super.onSizeChanged(w, h, oldw, oldh);
            post(MainActivity.this::positionHomeControls);
        }

        @Override protected void onDraw(Canvas c) {
            pulse = (System.nanoTime() - animationStartNanos) / 1_000_000_000f;
            c.drawColor(BG);
            if (page == HOME) drawHome(c);
            else if (page == RUNS) drawRuns(c);
            else if (page == DETAILS) drawDetails(c);
            else if (page == ARTIFACTS) drawArtifacts(c);
            else if (page == TOOLS) drawTools(c);
            else if (page == PROFILE) drawProfile(c);
            else if (page == SETTINGS) drawSettings(c);
            else if (page == VOICE) drawVoice(c);
            else drawCompleted(c);
            drawBottomNav(c);
            if (drawer) drawDrawer(c);
            if (getVisibility() == View.VISIBLE) postInvalidateOnAnimation();
        }

        private void drawHome(Canvas c) {
            drawHomeBackground(c);
            drawHomeHeader(c);
            drawHomeHero(c);
            drawHomeComposer(c);
            drawHomeFeatures(c);
            drawHomeMetrics(c);
            drawHomeRecent(c);
        }

        private void drawHomeBackground(Canvas c) {
            p.setShader(new LinearGradient(0, 0, 0, getHeight(),
                    new int[]{Color.rgb(2, 8, 18), Color.rgb(3, 13, 26), Color.rgb(2, 7, 17)},
                    null, Shader.TileMode.CLAMP));
            c.drawRect(0, 0, getWidth(), getHeight(), p);
            p.setShader(null);
            glowCircle(c, 270, 150, 150, Color.argb(38, 57, 117, 239));
            glowCircle(c, 88, 356, 145, Color.argb(20, 147, 71, 255));
            glowCircle(c, 304, 710, 205, Color.argb(16, 38, 155, 255));
            stroke.setStrokeWidth(Math.max(0.7f, hs()));
            stroke.setColor(Color.argb(12, 70, 129, 205));
            for (int i = -2; i < 7; i++) {
                c.drawLine(HX(i * 95f), HY(844), HX(i * 95f + 340f), HY(520), stroke);
            }
        }

        private void drawHomeHeader(Canvas c) {
            roundGlowHome(c, 17, 31, 61, 75, Color.rgb(6, 20, 38), Color.argb(150, 81, 144, 231), 14, Color.argb(38, 77, 141, 230));
            stroke.setColor(Color.WHITE);
            stroke.setStrokeWidth(HX(2.3f));
            stroke.setStrokeCap(Paint.Cap.ROUND);
            c.drawLine(HX(29), HY(43), HX(49), HY(43), stroke);
            c.drawLine(HX(29), HY(53), HX(49), HY(53), stroke);
            c.drawLine(HX(29), HY(63), HX(49), HY(63), stroke);

            drawBrandMark(c, 145, 54);
            textHome(c, "VEYTRIX", 22, HOME_TEXT, 205, 55, true, true);
            textHome(c, "AUTONOMOUS AI DEVELOPMENT", 7.4f, HOME_MUTED, 205, 73, true, false);

            roundGlowHome(c, 309, 31, 343, 65, Color.rgb(7, 17, 31), Color.argb(125, 84, 146, 223), 17, Color.TRANSPARENT);
            drawBell(c, 326, 48);
            p.setColor(Color.rgb(255, 81, 111)); c.drawCircle(HX(337), HY(31), HX(3.5f), p);
            roundGlowHome(c, 349, 31, 383, 65, Color.rgb(7, 17, 31), Color.argb(130, 89, 153, 230), 17, Color.TRANSPARENT);
            textHome(c, "FH", 11.5f, HOME_TEXT, 366, 54, true, true);
            p.setColor(HOME_GREEN); c.drawCircle(HX(375), HY(63), HX(4), p);
        }

        private void drawHomeHero(Canvas c) {
            p.setColor(Color.argb(200, 143, 79, 255));
            p.setShadowLayer(HX(8), 0, 0, Color.argb(120, 132, 76, 255));
            c.drawRoundRect(new RectF(HX(18), HY(94), HX(21), HY(205)), HX(1.5f), HX(1.5f), p);
            p.clearShadowLayer();
            textHome(c, "Build", 30, HOME_TEXT, 37, 129, false, true);
            textHome(c, "Without", 30, Color.rgb(191, 173, 255), 37, 159, false, true);
            textHome(c, "Limits.", 30, Color.rgb(205, 233, 255), 37, 189, false, true);
            textHome(c, "I D E A S   →   R E A L I T Y", 7.5f, HOME_MUTED, 37, 210, false, false);
            textHome(c, "A U T O N O M O U S L Y", 7.5f, HOME_TEXT, 37, 225, false, false);

            roundGlowHome(c, 17, 239, 143, 270, Color.rgb(5, 20, 35), Color.argb(145, 55, 122, 190), 17, Color.argb(22, 60, 169, 255));
            p.setColor(HOME_GREEN); p.setShadowLayer(HX(7), 0, 0, Color.argb(105, 44, 231, 177));
            c.drawCircle(HX(31), HY(254), HX(4.2f), p); p.clearShadowLayer();
            textHome(c, "AI CORE", 7.2f, HOME_TEXT, 43, 258, false, true);
            textHome(c, "ONLINE", 7.2f, HOME_GREEN, 82, 258, false, true);
            textHome(c, "›", 16, HOME_TEXT, 132, 258, true, false);

            textHome(c, "THINK", 7.2f, HOME_TEXT, 352, 212, true, false);
            textHome(c, "PLAN", 7.2f, HOME_TEXT, 352, 225, true, false);
            textHome(c, "EXECUTE", 7.2f, HOME_TEXT, 352, 238, true, false);
            textHome(c, "VERIFY", 7.2f, HOME_TEXT, 352, 251, true, false);
        }

        private void drawHomeComposer(Canvas c) {
            roundGlowHome(c, 16, 286, 374, 430, HOME_PANEL, Color.argb(190, 125, 101, 255), 19, Color.argb(33, 93, 151, 255));
            drawSparkle(c, 35, 308);
            textHome(c, "MISSION / COMMAND", 9.4f, HOME_TEXT, 51, 315, false, true);
            roundGlowHome(c, 289, 296, 366, 330, Color.rgb(7, 19, 36), Color.argb(135, 87, 127, 213), 12, Color.TRANSPARENT);
            textHome(c, deepMode ? "Deep Mode" : "Quick Mode", 8.0f, HOME_TEXT, 327, 318, true, false);
            textHome(c, "⌄", 12, HOME_MUTED, 355, 318, true, false);

            roundGlowHome(c, 27, 342, 363, 402, Color.rgb(4, 16, 30), Color.argb(165, 102, 144, 231), 14, Color.argb(18, 76, 151, 255));

            drawComposerButton(c, 26, 414, 100, 452, "Context", 0);
            drawComposerButton(c, 108, 414, 184, 452, "Voice", 1);
            drawComposerButton(c, 192, 414, 268, 452, "Attach", 2);
            drawLaunchButton(c, 276, 414, 364, 452);
        }

        private void drawComposerButton(Canvas c, float l, float t, float r, float b, String label, int icon) {
            roundGlowHome(c, l, t, r, b, Color.rgb(7, 23, 40), Color.argb(135, 68, 113, 174), 12, Color.TRANSPARENT);
            if (icon == 0) drawContextIcon(c, l + 17, t + 19);
            else if (icon == 1) drawMicIcon(c, l + 17, t + 19);
            else drawAttachIcon(c, l + 17, t + 19);
            textHome(c, label, 7.9f, HOME_TEXT, l + 32, t + 24, false, false);
        }

        private void drawLaunchButton(Canvas c, float l, float t, float r, float b) {
            p.setShader(new LinearGradient(HX(l), HY(t), HX(r), HY(b),
                    new int[]{Color.rgb(112, 75, 242), Color.rgb(86, 101, 255), Color.rgb(58, 170, 255)},
                    null, Shader.TileMode.CLAMP));
            p.setShadowLayer(HX(11), 0, 0, Color.argb(100, 90, 88, 255));
            c.drawRoundRect(new RectF(HX(l), HY(t), HX(r), HY(b)), HX(12), HX(12), p);
            p.clearShadowLayer(); p.setShader(null);
            drawPlayIcon(c, l + 22, t + 19);
            textHome(c, "Launch", 8.6f, Color.WHITE, l + 44, t + 25, false, true);
        }

        private void drawHomeFeatures(Canvas c) {
            String[] titles = {"Smart Plan", "Multi-Agent", "Auto Test", "Deploy"};
            String[] subs = {"Break down ideas", "Execute with AI", "Verify & secure", "Ship to production"};
            float[] xs = {16, 108, 200, 292};
            for (int i = 0; i < 4; i++) {
                roundGlowHome(c, xs[i], 471, xs[i] + 82, 554, Color.rgb(5, 20, 36), Color.argb(135, 62, 125, 203), 17, Color.argb(15, 69, 149, 255));
                drawFeatureIcon(c, xs[i] + 18, 493, i);
                textHome(c, titles[i], 7.8f, HOME_TEXT, xs[i] + 10, 526, false, true);
                textHome(c, subs[i], 6.2f, HOME_MUTED, xs[i] + 10, 541, false, false);
                textHome(c, "›", 15, Color.rgb(205, 220, 242), xs[i] + 70, 494, true, false);
            }
        }

        private void drawHomeMetrics(Canvas c) {
            roundGlowHome(c, 16, 572, 374, 642, Color.rgb(5, 21, 38), Color.argb(165, 67, 129, 220), 17, Color.argb(16, 68, 148, 255));
            stroke.setStrokeWidth(HX(.8f)); stroke.setColor(Color.argb(75, 84, 124, 177));
            c.drawLine(HX(105), HY(585), HX(105), HY(629), stroke);
            c.drawLine(HX(195), HY(585), HX(195), HY(629), stroke);
            c.drawLine(HX(285), HY(585), HX(285), HY(629), stroke);
            drawMetric(c, 22, 604, "128", "Missions", 0);
            drawMetric(c, 112, 604, "24", "Projects", 1);
            drawMetric(c, 202, 604, "98%", "Success Rate", 2);
            drawMetric(c, 292, 604, "2.4x", "Faster", 3);
        }

        private void drawMetric(Canvas c, float x, float y, String value, String label, int icon) {
            drawMetricIcon(c, x + 11, y - 14, icon);
            textHome(c, value, 12.4f, HOME_TEXT, x + 32, y - 4, false, true);
            textHome(c, label, 6.7f, HOME_MUTED, x + 32, y + 11, false, false);
        }

        private void drawHomeRecent(Canvas c) {
            roundGlowHome(c, 16, 660, 374, 800, Color.rgb(4, 18, 33), Color.argb(165, 66, 127, 219), 18, Color.argb(15, 60, 137, 255));
            drawRecentHeaderIcon(c, 31, 683);
            textHome(c, "Recent Activity", 9.6f, HOME_TEXT, 48, 688, false, true);
            roundGlowHome(c, 316, 671, 365, 700, Color.rgb(6, 20, 37), Color.argb(110, 78, 128, 206), 12, Color.TRANSPARENT);
            textHome(c, "View All", 7.0f, HOME_TEXT, 340, 690, true, false);
            drawRecentItem(c, 23, 711, "Build authentication system", "Completed successfully", "2h ago", "Success", 0);
            drawRecentItem(c, 23, 758, "Design modern UI components", "Generated 24 components", "5h ago", "Completed", 1);
            drawRecentItem(c, 23, 805, "Optimize database queries", "Performance improved by 70%", "1d ago", "Optimized", 2);
        }

        private void drawRecentItem(Canvas c, float x, float y, String title, String sub, String time, String status, int icon) {
            roundGlowHome(c, x, y, 367, y + 39, Color.rgb(6, 24, 42), Color.argb(105, 73, 118, 187), 13, Color.TRANSPARENT);
            int accent = icon == 0 ? Color.rgb(34, 209, 161) : icon == 1 ? Color.rgb(47, 118, 255) : HOME_PURPLE;
            p.setColor(Color.argb(90, Color.red(accent), Color.green(accent), Color.blue(accent))); p.setShadowLayer(HX(7), 0, 0, Color.argb(75, Color.red(accent), Color.green(accent), Color.blue(accent)));
            c.drawRoundRect(new RectF(HX(x + 8), HY(y + 7), HX(x + 36), HY(y + 32)), HX(8), HX(8), p); p.clearShadowLayer();
            p.setColor(accent); c.drawRoundRect(new RectF(HX(x + 8), HY(y + 7), HX(x + 36), HY(y + 32)), HX(8), HX(8), p);
            if (icon == 0) drawCodeIcon(c, x + 22, y + 20); else if (icon == 1) drawDocIcon(c, x + 22, y + 20); else drawGearIcon(c, x + 22, y + 20);
            textHome(c, title, 7.2f, HOME_TEXT, x + 46, y + 15, false, true);
            textHome(c, sub, 6.3f, HOME_MUTED, x + 46, y + 28, false, false);
            textHome(c, time, 6.0f, HOME_MUTED, 338, y + 13, true, false);
            roundGlowHome(c, 286, y + 21, 331, y + 35, Color.rgb(6, 31, 33), Color.argb(105, 37, 194, 156), 8, Color.TRANSPARENT);
            textHome(c, status, 5.9f, HOME_GREEN, 308, y + 31, true, true);
            textHome(c, "⋮", 13, HOME_TEXT, 356, y + 27, true, false);
        }

        private void drawBottomNav(Canvas c) {
            roundGlowHome(c, 12, 806, 378, 840, Color.rgb(5, 16, 31), Color.argb(185, 76, 125, 224), 20, Color.argb(15, 96, 112, 255));
            navItemHome(c, 50, "⌂", "Home", true);
            navItemHome(c, 123, "▤", "Activity", false);
            navItemHome(c, 196, "□", "Results", false);
            navItemHome(c, 269, "♙", "Control", false);
            navItemHome(c, 342, "•••", "More", false);
        }

        private void navItemHome(Canvas c, float x, String icon, String label, boolean active) {
            if (active) {
                p.setShader(new RadialGradient(HX(x), HY(822), HX(26), new int[]{Color.argb(82, 132, 79, 255), Color.TRANSPARENT}, null, Shader.TileMode.CLAMP));
                c.drawCircle(HX(x), HY(822), HX(26), p); p.setShader(null);
            }
            textHome(c, icon, label.equals("More") ? 12.5f : 16.5f, active ? Color.WHITE : Color.rgb(205, 219, 238), x, 821, true, active);
            textHome(c, label, 6.9f, active ? Color.WHITE : HOME_MUTED, x, 837, true, false);
        }

        private void drawDrawer(Canvas c) { /* existing drawer retained */ }
        private void drawRuns(Canvas c) { top(c,"",false); round(c,18,89,132,120,Color.rgb(16,35,30),Color.rgb(47,118,88),18); circle(c,35,104,6,HOME_GREEN); text(c,"EXECUTING",10,HOME_GREEN,51,110,true); text(c,"00:02:17",10,Color.rgb(164,164,157),337,110,true); String m=mission.isEmpty()?"Create a research report on renewable energy trends":mission; text(c,m,13.5f,HOME_TEXT,24,147,false); drawCore(c,195,285,64); checklist(c,24,382); round(c,24,560,366,614,Color.rgb(247,225,181),Color.rgb(248,228,190),26); text(c,paused?"Resume Mission":"Pause Mission",13,Color.rgb(34,32,28),195,593,true); round(c,24,628,366,674,HOME_PANEL_2,Color.rgb(64,63,55),22); text(c,"Open run details",12,HOME_TEXT,195,656,true); }
        private void drawDetails(Canvas c) { top(c,"Mission Run",true); }
        private void drawArtifacts(Canvas c) { top(c,"Artifacts",true); }
        private void drawTools(Canvas c) { top(c,"Tools",true); }
        private void drawProfile(Canvas c) { top(c,"Profile",true); }
        private void drawSettings(Canvas c) { top(c,"Settings",true); }
        private void drawVoice(Canvas c) { top(c,"Voice Input",true); }
        private void drawCompleted(Canvas c) { top(c,"",false); }

        private void top(Canvas c,String title,boolean back){ p.setShader(null); p.setColor(Color.rgb(7,10,11)); c.drawRect(0,0,getWidth(),Y(66),p); if(back) text(c,"‹",23,Color.WHITE,22,40,true); else { round(c,18,12,58,52,Color.rgb(20,24,24),Color.rgb(64,63,55),15); text(c,"☰",21,Color.WHITE,28,35,true);} text(c,"VEYTRIX",15,Color.rgb(248,228,190),82,29,true); text(c,"AUTOPILOT",8,Color.rgb(104,106,99),83,45,true); if(!back){round(c,323,17,369,44,Color.rgb(39,37,31),Color.rgb(221,194,143),16);text(c,"Pro",11,Color.rgb(248,228,190),346,35,true);} else text(c,title,16,Color.WHITE,55,36,true); }

        private void drawCore(Canvas c,float cx,float cy,float r){ float rr=r*(1f+(float)Math.sin(pulse)*.015f); p.setShader(new RadialGradient(X(cx),Y(cy),X(rr*1.7f),new int[]{Color.argb(90,228,194,128),Color.argb(20,228,194,128),Color.TRANSPARENT},null,Shader.TileMode.CLAMP)); c.drawCircle(X(cx),Y(cy),X(rr*1.7f),p); p.setShader(null); stroke.setColor(Color.argb(170,215,184,117)); stroke.setStrokeWidth(X(1)); RectF ring=new RectF(X(cx-rr*1.7f),Y(cy-rr*.72f),X(cx+rr*1.7f),Y(cy+rr*.72f)); c.save(); c.rotate(-15,X(cx),Y(cy)); c.drawOval(ring,stroke); c.restore(); RectF ring2=new RectF(X(cx-rr*1.25f),Y(cy-rr*1.6f),X(cx+rr*1.25f),Y(cy+rr*1.6f)); stroke.setColor(Color.argb(90,215,184,117)); c.save(); c.rotate(19,X(cx),Y(cy)); c.drawOval(ring2,stroke); c.restore(); p.setShader(new RadialGradient(X(cx-rr*.30f),Y(cy-rr*.38f),X(rr*1.15f),new int[]{Color.rgb(255,248,225),Color.rgb(224,198,143),Color.rgb(116,88,43),Color.rgb(24,21,17)},new float[]{0f,.18f,.52f,1f},Shader.TileMode.CLAMP)); c.drawCircle(X(cx),Y(cy),X(rr),p); p.setShader(null); p.setColor(Color.argb(190,255,248,224)); c.drawCircle(X(cx-rr*.28f),Y(cy-rr*.33f),X(rr*.16f),p); text(c,"V",Math.max(16,rr*.43f),Color.rgb(250,238,211),cx,cy+rr*.20f,true); }
        private void checklist(Canvas c,float x,float y){String[] rows={"Understanding objective","Planning approach","Preparing environment","Executing tasks","Verifying results"};for(int i=0;i<rows.length;i++){float yy=y+i*30;circle(c,x+6,yy,4,i<3?Color.rgb(248,228,190):(i==3?Color.rgb(107,201,197):Color.rgb(104,106,99));text(c,rows[i],11,i<3?Color.rgb(171,169,157):Color.rgb(104,106,99),x+20,yy+4,false);if(i<3)text(c,"✓",11,HOME_GREEN,335,yy+4,true);}}
        private void circle(Canvas c,float x,float y,float r,int color){p.setShader(null);p.setColor(color);c.drawCircle(X(x),Y(y),X(r),p);}
        private void glowCircle(Canvas c,float x,float y,float r,int color){p.setShader(new RadialGradient(HX(x),HY(y),HX(r),new int[]{color,Color.TRANSPARENT},null,Shader.TileMode.CLAMP));c.drawCircle(HX(x),HY(y),HX(r),p);p.setShader(null);}
        private void roundGlowHome(Canvas c,float l,float t,float r,float b,int fill,int border,float radius,int glow){p.setShader(null);p.setStyle(Paint.Style.FILL);p.setColor(fill);if(glow!=Color.TRANSPARENT)p.setShadowLayer(HX(8),0,0,glow);RectF rr=new RectF(HX(l),HY(t),HX(r),HY(b));c.drawRoundRect(rr,HX(radius),HX(radius),p);p.clearShadowLayer();if(border!=Color.TRANSPARENT){p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(Math.max(1f,HX(.8f)));p.setColor(border);c.drawRoundRect(rr,HX(radius),HX(radius),p);p.setStyle(Paint.Style.FILL);}}
        private void round(Canvas c,float l,float t,float r,float b,int fill,int border,float radius){p.setShader(null);p.setStyle(Paint.Style.FILL);p.setColor(fill);RectF rr=new RectF(X(l),Y(t),X(r),Y(b));c.drawRoundRect(rr,X(radius),X(radius),p);if(border!=Color.TRANSPARENT){p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(X(1));p.setColor(border);c.drawRoundRect(rr,X(radius),X(radius),p);p.setStyle(Paint.Style.FILL);}}
        private void text(Canvas c,String str,float size,int color,float x,float y,boolean center){p.setShader(null);p.setColor(color);p.setTextSize(X(size));p.setTypeface(Typeface.create("sans",size>=14?Typeface.BOLD:Typeface.NORMAL));p.setTextAlign(center?Paint.Align.CENTER:Paint.Align.LEFT);c.drawText(str,X(x),Y(y),p);}
        private void textHome(Canvas c,String str,float size,int color,float x,float y,boolean center,boolean bold){p.setShader(null);p.setColor(color);p.setTextSize(HX(size));p.setTypeface(Typeface.create("sans-serif",bold?Typeface.BOLD:Typeface.NORMAL));p.setTextAlign(center?Paint.Align.CENTER:Paint.Align.LEFT);c.drawText(str,HX(x),HY(y),p);}
        private void drawBrandMark(Canvas c,float cx,float cy){Path v=new Path();v.moveTo(HX(cx-13),HY(cy-14));v.lineTo(HX(cx-4),HY(cy+3));v.lineTo(HX(cx+4),HY(cy+18));v.lineTo(HX(cx+13),HY(cy-14));v.lineTo(HX(cx+5),HY(cy-14));v.lineTo(HX(cx),HY(cy+1));v.lineTo(HX(cx-5),HY(cy-14));v.close();p.setShader(new LinearGradient(HX(cx-12),HY(cy-14),HX(cx+13),HY(cy+15),new int[]{HOME_CYAN,Color.rgb(116,96,255),Color.rgb(192,67,255)},null,Shader.TileMode.CLAMP));c.drawPath(v,p);p.setShader(null);}
        private void drawBell(Canvas c,float cx,float cy){stroke.setColor(Color.WHITE);stroke.setStyle(Paint.Style.STROKE);stroke.setStrokeCap(Paint.Cap.ROUND);stroke.setStrokeWidth(HX(1.8f));Path q=new Path();q.moveTo(HX(cx-6),HY(cy+2));q.quadTo(HX(cx-5),HY(cy-7),HX(cx),HY(cy-7));q.quadTo(HX(cx+5),HY(cy-7),HX(cx+6),HY(cy+2));q.lineTo(HX(cx+7),HY(cy+5));q.lineTo(HX(cx-7),HY(cy+5));q.close();c.drawPath(q,stroke);c.drawLine(HX(cx-3),HY(cy+8),HX(cx+3),HY(cy+8),stroke);stroke.setStrokeCap(Paint.Cap.BUTT);}
        private void drawSparkle(Canvas c,float cx,float cy){Path q=new Path();q.moveTo(HX(cx),HY(cy-9));q.lineTo(HX(cx+4),HY(cy-3));q.lineTo(HX(cx+10),HY(cy));q.lineTo(HX(cx+4),HY(cy+4));q.lineTo(HX(cx),HY(cy+11));q.lineTo(HX(cx-4),HY(cy+4));q.lineTo(HX(cx-10),HY(cy));q.lineTo(HX(cx-4),HY(cy-3));q.close();p.setShader(new LinearGradient(HX(cx-8),HY(cy-8),HX(cx+8),HY(cy+8),new int[]{Color.rgb(197,171,255),HOME_BLUE},null,Shader.TileMode.CLAMP));c.drawPath(q,p);p.setShader(null);}
        private void drawContextIcon(Canvas c,float cx,float cy){stroke.setColor(Color.WHITE);stroke.setStrokeWidth(HX(1.3f));Path a=new Path();a.moveTo(HX(cx-9),HY(cy-4));a.lineTo(HX(cx),HY(cy-10));a.lineTo(HX(cx+9),HY(cy-4));a.lineTo(HX(cx),HY(cy+2));a.close();c.drawPath(a,stroke);Path b=new Path();b.moveTo(HX(cx-9),HY(cy+2));b.lineTo(HX(cx),HY(cy+8));b.lineTo(HX(cx+9),HY(cy+2));b.close();c.drawPath(b,stroke);}
        private void drawMicIcon(Canvas c,float cx,float cy){stroke.setColor(HOME_BLUE);stroke.setStrokeWidth(HX(1.8f));stroke.setStyle(Paint.Style.STROKE);c.drawRoundRect(new RectF(HX(cx-4),HY(cy-9),HX(cx+4),HY(cy+3)),HX(4),HX(4),stroke);c.drawArc(new RectF(HX(cx-8),HY(cy-3),HX(cx+8),HY(cy+8)),0,180,false,stroke);c.drawLine(HX(cx),HY(cy+8),HX(cx),HY(cy+11),stroke);c.drawLine(HX(cx-4),HY(cy+11),HX(cx+4),HY(cy+11),stroke);}
        private void drawAttachIcon(Canvas c,float cx,float cy){stroke.setColor(Color.WHITE);stroke.setStrokeWidth(HX(1.7f));stroke.setStyle(Paint.Style.STROKE);Path q=new Path();q.moveTo(HX(cx+6),HY(cy-7));q.cubicTo(HX(cx+11),HY(cy-2),HX(cx+7),HY(cy+4),HX(cx+3),HY(cy+8));q.cubicTo(HX(cx-3),HY(cy+13),HX(cx-10),HY(cy+7),HX(cx-6),HY(cy+2));q.lineTo(HX(cx+4),HY(cy-8));c.drawPath(q,stroke);}
        private void drawPlayIcon(Canvas c,float cx,float cy){p.setColor(Color.WHITE);Path q=new Path();q.moveTo(HX(cx-5),HY(cy-8));q.lineTo(HX(cx+8),HY(cy));q.lineTo(HX(cx-5),HY(cy+8));q.close();c.drawPath(q,p);}
        private void drawFeatureIcon(Canvas c,float cx,float cy,int kind){int accent=kind==0?Color.rgb(194,165,255):kind==1?Color.rgb(225,231,255):Color.WHITE:Color.rgb(196,218,255);stroke.setColor(accent);stroke.setStrokeWidth(HX(2));stroke.setStyle(Paint.Style.STROKE);if(kind==0){c.drawCircle(HX(cx),HY(cy),HX(8),stroke);c.drawLine(HX(cx),HY(cy-11),HX(cx),HY(cy+11),stroke);c.drawLine(HX(cx-11),HY(cy),HX(cx+11),HY(cy),stroke);}else if(kind==1){c.drawCircle(HX(cx-5),HY(cy-3),HX(4),stroke);c.drawCircle(HX(cx+5),HY(cy-3),HX(4),stroke);}else if(kind==2){Path q=new Path();q.moveTo(HX(cx-10),HY(cy));q.lineTo(HX(cx-4),HY(cy+6));q.lineTo(HX(cx+9),HY(cy-9));c.drawPath(q,stroke);c.drawRect(HX(cx-11),HY(cy-11),HX(cx+11),HY(cy+11),stroke);}else{Path q=new Path();q.moveTo(HX(cx-10),HY(cy+6));q.lineTo(HX(cx),HY(cy-10));q.lineTo(HX(cx+10),HY(cy+6));q.lineTo(HX(cx),HY(cy+1));q.close();c.drawPath(q,stroke);c.drawCircle(HX(cx),HY(cy-11),HX(2),stroke);}}
        private void drawMetricIcon(Canvas c,float cx,float cy,int kind){stroke.setColor(kind==0?Color.rgb(189,164,255):kind==1?Color.rgb(170,132,255):kind==2?Color.rgb(167,147,255):Color.rgb(165,181,255));stroke.setStrokeWidth(HX(2.0f));if(kind==0){Path q=new Path();q.moveTo(HX(cx-5),HY(cy-11));q.lineTo(HX(cx+2),HY(cy-2));q.lineTo(HX(cx-2),HY(cy-2));q.lineTo(HX(cx+5),HY(cy+9));q.lineTo(HX(cx-3),HY(cy));q.lineTo(HX(cx+1),HY(cy));q.close();p.setColor(stroke.getColor());c.drawPath(q,p);}else if(kind==1){p.setColor(stroke.getColor());c.drawRect(HX(cx-10),HY(cy-6),HX(cx-2),HY(cy+9),p);c.drawRoundRect(new RectF(HX(cx-1),HY(cy-10),HX(cx+7),HY(cy+9)),HX(2),HX(2),p);c.drawRoundRect(new RectF(HX(cx+8),HY(cy-8),HX(cx+16),HY(cy+9)),HX(2),HX(2),p);}else if(kind==2){p.setColor(stroke.getColor());c.drawRect(HX(cx-9),HY(cy-4),HX(cx-1),HY(cy+10),p);c.drawRect(HX(cx+2),HY(cy-10),HX(cx+10),HY(cy+10),p);c.drawRect(HX(cx+13),HY(cy-1),HX(cx+21),HY(cy+10),p);}else{stroke.setStyle(Paint.Style.STROKE);c.drawCircle(HX(cx+1),HY(cy),HX(9),stroke);c.drawLine(HX(cx+1),HY(cy),HX(cx+1),HY(cy-5),stroke);c.drawLine(HX(cx+1),HY(cy),HX(cx+6),HY(cy+3),stroke);}}
        private void drawRecentHeaderIcon(Canvas c,float cx,float cy){p.setColor(Color.rgb(179,139,255));c.drawCircle(HX(cx),HY(cy),HX(8),p);stroke.setColor(Color.rgb(236,231,255));stroke.setStrokeWidth(HX(1.3f));c.drawLine(HX(cx),HY(cy-4),HX(cx),HY(cy+4),stroke);c.drawLine(HX(cx-3),HY(cy),HX(cx+3),HY(cy),stroke);}
        private void drawCodeIcon(Canvas c,float cx,float cy){stroke.setColor(Color.WHITE);stroke.setStrokeWidth(HX(1.6f));c.drawLine(HX(cx-7),HY(cy),HX(cx-2),HY(cy-4),stroke);c.drawLine(HX(cx-7),HY(cy),HX(cx-2),HY(cy+4),stroke);c.drawLine(HX(cx+7),HY(cy),HX(cx+2),HY(cy-4),stroke);c.drawLine(HX(cx+7),HY(cy),HX(cx+2),HY(cy+4),stroke);c.drawLine(HX(cx-2),HY(cy+5),HX(cx+2),HY(cy-5),stroke);}
        private void drawDocIcon(Canvas c,float cx,float cy){p.setColor(Color.WHITE);c.drawRoundRect(new RectF(HX(cx-6),HY(cy-8),HX(cx+6),HY(cy+8)),HX(2),HX(2),p);p.setColor(Color.rgb(77,130,255));c.drawRect(HX(cx-3),HY(cy-3),HX(cx+3),HY(cy-1),p);c.drawRect(HX(cx-3),HY(cy+1),HX(cx+3),HY(cy+3),p);}
        private void drawGearIcon(Canvas c,float cx,float cy){stroke.setColor(Color.WHITE);stroke.setStrokeWidth(HX(1.5f));c.drawCircle(HX(cx),HY(cy),HX(6),stroke);c.drawCircle(HX(cx),HY(cy),HX(2),stroke);for(int i=0;i<8;i++){double a=i*Math.PI/4;float x1=(float)Math.cos(a)*8,y1=(float)Math.sin(a)*8;float x2=(float)Math.cos(a)*10,y2=(float)Math.sin(a)*10;c.drawLine(HX(cx+x1),HY(cy+y1),HX(cx+x2),HY(cy+y2),stroke);}}
        
        @Override public boolean onTouchEvent(MotionEvent e){
            if(e.getAction()!=MotionEvent.ACTION_UP) return true;
            float x=e.getX()/hs(); float y=e.getY()/hs();
            if(page==HOME){
                if(y<82 && x<78){ drawer=true; hideComposer(); invalidate(); return true; }
                if(y<82 && x>339){ select(PROFILE); return true; }
                if(y>=470 && y<560){ Toast.makeText(MainActivity.this,"Feature shortcut not connected in this build",Toast.LENGTH_SHORT).show(); return true; }
                if(y>=660 && y<807){ select(RUNS); return true; }
                if(y>=800){ if(x<82) select(HOME); else if(x<156) select(RUNS); else if(x<230) select(ARTIFACTS); else if(x<304) select(TOOLS); else select(PROFILE); return true; }
                return true;
            }
            if(drawer){ drawer=false; if(page==HOME) showComposer(); invalidate(); return true; }
            if(y>795){ if(x<82) select(HOME); else if(x<156) select(RUNS); else if(x<230) select(ARTIFACTS); else if(x<304) select(TOOLS); else select(PROFILE); return true; }
            return true;
        }
        private void select(int target){ page=target; drawer=false; if(target==HOME) showComposer(); else hideComposer(); invalidate(); }
    }
}
