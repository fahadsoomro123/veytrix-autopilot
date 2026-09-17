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
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.inputmethod.InputMethodManager;
import android.content.Context;
import android.text.InputType;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsControllerCompat;

/** Native VEYTRIX flagship Android surface. */
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

    private static final int HOME_BG = Color.rgb(3, 10, 21);
    private static final int HOME_PANEL = Color.rgb(7, 23, 39);
    private static final int HOME_PANEL_2 = Color.rgb(10, 29, 49);
    private static final int HOME_BLUE = Color.rgb(86, 150, 255);
    private static final int HOME_CYAN = Color.rgb(76, 218, 255);
    private static final int HOME_PURPLE = Color.rgb(154, 91, 255);
    private static final int HOME_PURPLE_2 = Color.rgb(110, 70, 255);
    private static final int HOME_TEXT = Color.rgb(242, 247, 255);
    private static final int HOME_MUTED = Color.rgb(164, 188, 219);
    private static final int HOME_DIM = Color.rgb(103, 127, 159);
    private static final int HOME_GREEN = Color.rgb(44, 231, 177);

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
        window.setSoftInputMode(android.view.WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN);
        WindowInsetsControllerCompat controller = WindowCompat.getInsetsController(window, window.getDecorView());
        controller.setAppearanceLightStatusBars(false);
        controller.setAppearanceLightNavigationBars(false);
    }

    private void build() {
        root = new FrameLayout(this);
        surface = new VeytrixView(this);
        root.addView(surface, new FrameLayout.LayoutParams(-1, -1));

        coreView = new FlagshipCoreView(this);
        coreView.setContentDescription("VEYTRIX autonomous core");
        root.addView(coreView, new FrameLayout.LayoutParams(dp(176), dp(188)));

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
        prompt.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        prompt.setPadding(dp(14), dp(11), dp(10), dp(8));
        prompt.setBackgroundColor(Color.TRANSPARENT);
        root.addView(prompt, new FrameLayout.LayoutParams(dp(332), dp(60)));

        contextAction = transparentAction("Context options");
        contextAction.setOnClickListener(v -> Toast.makeText(this, "Context tools are not connected in this build", Toast.LENGTH_SHORT).show());
        root.addView(contextAction, new FrameLayout.LayoutParams(dp(76), dp(38)));

        mic = transparentAction("Voice input");
        mic.setOnClickListener(v -> Toast.makeText(this, "Voice input is not connected in this build", Toast.LENGTH_SHORT).show());
        root.addView(mic, new FrameLayout.LayoutParams(dp(74), dp(38)));

        attachAction = transparentAction("Attach files");
        attachAction.setOnClickListener(v -> Toast.makeText(this, "Attachments are not connected in this build", Toast.LENGTH_SHORT).show());
        root.addView(attachAction, new FrameLayout.LayoutParams(dp(76), dp(38)));

        send = transparentAction("Launch mission");
        send.setOnClickListener(v -> startMission());
        root.addView(send, new FrameLayout.LayoutParams(dp(92), dp(38)));

        modeAction = transparentAction("Mission mode");
        modeAction.setOnClickListener(v -> {
            deepMode = !deepMode;
            surface.invalidate();
        });
        root.addView(modeAction, new FrameLayout.LayoutParams(dp(80), dp(35)));

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

    private void positionHomeControls() {
        if (surface == null || surface.getWidth() <= 0 || surface.getHeight() <= 0) return;
        float density = getResources().getDisplayMetrics().density;
        float widthDp = surface.getWidth() / density;
        float heightDp = surface.getHeight() / density;
        float scale = Math.min(widthDp / 390f, heightDp / 850f);
        int contentWidth = Math.round(390f * scale * density);
        int xOffset = Math.max(0, (surface.getWidth() - contentWidth) / 2);

        place(coreView, xOffset + Math.round(163f * scale * density), Math.round(72f * scale * density), Math.round(176f * scale * density), Math.round(188f * scale * density));
        place(prompt, xOffset + Math.round(27f * scale * density), Math.round(287f * scale * density), Math.round(332f * scale * density), Math.round(60f * scale * density));
        place(modeAction, xOffset + Math.round(284f * scale * density), Math.round(259f * scale * density), Math.round(80f * scale * density), Math.round(35f * scale * density));
        place(contextAction, xOffset + Math.round(25f * scale * density), Math.round(350f * scale * density), Math.round(76f * scale * density), Math.round(38f * scale * density));
        place(mic, xOffset + Math.round(106f * scale * density), Math.round(350f * scale * density), Math.round(74f * scale * density), Math.round(38f * scale * density));
        place(attachAction, xOffset + Math.round(185f * scale * density), Math.round(350f * scale * density), Math.round(76f * scale * density), Math.round(38f * scale * density));
        place(send, xOffset + Math.round(266f * scale * density), Math.round(350f * scale * density), Math.round(92f * scale * density), Math.round(38f * scale * density));
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

    private int dp(int value) { return Math.round(value * getResources().getDisplayMetrics().density); }

    private GradientDrawable roundDrawable(int fill, int stroke, float radius) {
        GradientDrawable d = new GradientDrawable(); d.setColor(fill); d.setCornerRadius(dp((int) radius));
        if (stroke != Color.TRANSPARENT) d.setStroke(dp(1), stroke); return d;
    }

    private final class VeytrixView extends View {
        static final int HOME = 0, RUNS = 1, DETAILS = 2, ARTIFACTS = 3, TOOLS = 4, PROFILE = 5, SETTINGS = 6, VOICE = 7, COMPLETED = 8;
        private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint stroke = new Paint(Paint.ANTI_ALIAS_FLAG);
        private int page = HOME;
        private boolean drawer = false;
        private boolean paused = false;
        private boolean completed = false;
        private float pulse = 0f;
        private long animationStartNanos;
        private String mission = "";

        VeytrixView(Context context) {
            super(context);
            stroke.setStyle(Paint.Style.STROKE);
            stroke.setStrokeWidth(1f);
            setLayerType(View.LAYER_TYPE_SOFTWARE, null);
            animationStartNanos = System.nanoTime();
        }

        private float s() { return getWidth() / 390f; }
        private float X(float n) { return n * s(); }
        private float Y(float n) { return n * s(); }
        private float hs() { return Math.min(getWidth() / 390f, getHeight() / 850f); }
        private float HX(float n) { return (getWidth() - 390f * hs()) * .5f + n * hs(); }
        private float HY(float n) { return n * hs(); }

        @Override protected void onAttachedToWindow() {
            super.onAttachedToWindow();
            animationStartNanos = System.nanoTime();
            postInvalidateOnAnimation();
        }

        @Override protected void onDetachedFromWindow() {
            removeCallbacksAndMessages();
            super.onDetachedFromWindow();
        }

        private void removeCallbacksAndMessages() {
            removeCallbacks(invalidateRunnable);
        }

        private final Runnable invalidateRunnable = this::invalidate;

        @Override protected void onSizeChanged(int w, int h, int oldw, int oldh) {
            super.onSizeChanged(w, h, oldw, oldh);
            post(MainActivity.this::positionHomeControls);
        }

        @Override protected void onDraw(Canvas c) {
            pulse = (System.nanoTime() - animationStartNanos) / 1_000_000_000f;
            c.drawColor(BG);
            if (page == HOME) drawHome(c); else if (page == RUNS) drawRuns(c); else if (page == DETAILS) drawDetails(c);
            else if (page == ARTIFACTS) drawArtifacts(c); else if (page == TOOLS) drawTools(c); else if (page == PROFILE) drawProfile(c);
            else if (page == SETTINGS) drawSettings(c); else if (page == VOICE) drawVoice(c); else drawCompleted(c);
            drawBottomNav(c);
            if (drawer) drawDrawer(c);
            if (getVisibility() == View.VISIBLE) postInvalidateOnAnimation();
        }

        private void top(Canvas c, String title, boolean back) {
            p.setShader(null); p.setColor(Color.rgb(7, 10, 11)); c.drawRect(0, 0, getWidth(), Y(70), p);
            if (back) text(c, "‹", 23, WHITE, 22, 43, true); else { round(c,18,12,58,52,Color.rgb(20,24,24),LINE,15); text(c,"☰",21,WHITE,28,35,true); }
            text(c, "VEYTRIX", 15, GOLD_LIGHT, 82, 29, true); text(c, "AUTOPILOT", 8, DIM, 83, 45, true);
            if (!back) { round(c,323,17,369,44,Color.rgb(39,37,31),GOLD,16); text(c,"Pro",11,GOLD_LIGHT,346,35,true); }
            else text(c, title, 16, WHITE, 55, 36, true);
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
                    new int[]{Color.rgb(2, 8, 17), Color.rgb(4, 13, 26), Color.rgb(2, 7, 17)},
                    null, Shader.TileMode.CLAMP));
            c.drawRect(0, 0, getWidth(), getHeight(), p);
            p.setShader(null);
            p.setColor(Color.argb(42, 47, 102, 215)); c.drawCircle(HX(250), HY(145), HX(165), p);
            p.setColor(Color.argb(25, 153, 75, 255)); c.drawCircle(HX(104), HY(300), HX(170), p);
            p.setColor(Color.argb(22, 16, 177, 255)); c.drawCircle(HX(310), HY(690), HX(210), p);
            stroke.setStrokeWidth(Math.max(1f, hs()));
            stroke.setColor(Color.argb(18, 73, 129, 205));
            for (int i = -2; i < 7; i++) c.drawLine(HX(i * 95f), HY(860), HX(i * 95f + 340f), HY(550), stroke);
            stroke.setColor(Color.argb(28, 99, 122, 177));
            c.drawLine(HX(0), HY(392), HX(390), HY(392), stroke);
        }

        private void drawHomeHeader(Canvas c) {
            float glow = 16f + 2f * (float) Math.sin(pulse * 1.2f);
            p.setShader(null); p.setStyle(Paint.Style.FILL); p.setColor(Color.argb(90, 77, 141, 230)); p.setShadowLayer(HX(glow), 0, 0, Color.argb(80, 77, 141, 230));
            c.drawRoundRect(new RectF(HX(15), HY(26), HX(59), HY(64)), HX(13), HX(13), p); p.clearShadowLayer();
            p.setColor(Color.rgb(7, 19, 35)); c.drawRoundRect(new RectF(HX(15), HY(26), HX(59), HY(64)), HX(13), HX(13), p);
            stroke.setColor(Color.argb(150, 93, 150, 228)); stroke.setStrokeWidth(HX(1)); c.drawRoundRect(new RectF(HX(15), HY(26), HX(59), HY(64)), HX(13), HX(13), stroke);
            stroke.setColor(Color.WHITE); stroke.setStrokeCap(Paint.Cap.ROUND); stroke.setStrokeWidth(HX(2.6f));
            c.drawLine(HX(26), HY(38), HX(48), HY(38), stroke); c.drawLine(HX(26), HY(45), HX(48), HY(45), stroke); c.drawLine(HX(26), HY(52), HX(48), HY(52), stroke);

            drawBrandMark(c, 120, 47);
            textHome(c, "VEYTRIX", 18, HOME_TEXT, 194, 45, true, true);
            textHome(c, "AUTONOMOUS AI DEVELOPMENT", 6.3f, HOME_MUTED, 194, 60, true, false);

            p.setColor(Color.rgb(7, 17, 31)); c.drawCircle(HX(318), HY(45), HX(17), p);
            stroke.setColor(Color.argb(130, 85, 146, 223)); stroke.setStrokeWidth(HX(1)); c.drawCircle(HX(318), HY(45), HX(17), stroke);
            drawBell(c, 318, 44);
            p.setColor(Color.rgb(255, 86, 110)); c.drawCircle(HX(329), HY(28), HX(3.5f), p);

            p.setColor(Color.rgb(7, 17, 31)); c.drawCircle(HX(363), HY(45), HX(17), p);
            stroke.setColor(Color.argb(150, 89, 153, 230)); stroke.setStrokeWidth(HX(1)); c.drawCircle(HX(363), HY(45), HX(17), stroke);
            textHome(c, "FH", 11, HOME_TEXT, 363, 49, true, true);
            p.setColor(HOME_GREEN); c.drawCircle(HX(371), HY(57), HX(4), p);
        }

        private void drawHomeHero(Canvas c) {
            p.setColor(Color.argb(205, 142, 81, 255)); p.setShadowLayer(HX(9), 0, 0, Color.argb(140, 132, 77, 255)); c.drawRoundRect(new RectF(HX(18), HY(86), HX(21), HY(196)), HX(1.5f), HX(1.5f), p); p.clearShadowLayer();
            textHome(c, "Build", 29, HOME_TEXT, 36, 116, false, true);
            textHome(c, "Without", 29, Color.rgb(191, 173, 255), 36, 145, false, true);
            textHome(c, "Limits.", 29, Color.rgb(205, 233, 255), 36, 174, false, true);
            textHome(c, "I D E A S   →   R E A L I T Y", 7.5f, HOME_MUTED, 36, 195, false, false);
            textHome(c, "A U T O N O M O U S L Y", 7.5f, HOME_TEXT, 36, 208, false, false);

            roundGlowHome(c, 15, 218, 135, 248, Color.rgb(5, 20, 35), Color.argb(145, 55, 122, 190), 18, Color.argb(40, 60, 169, 255));
            p.setColor(HOME_GREEN); p.setShadowLayer(HX(8),0,0,Color.argb(120,44,231,177)); c.drawCircle(HX(31), HY(233), HX(4.3f), p); p.clearShadowLayer();
            textHome(c, "AI CORE ", 7.2f, HOME_TEXT, 43, 236, false, false);
            textHome(c, "ONLINE", 7.2f, HOME_GREEN, 70, 236, false, true);
            textHome(c, "›", 16, HOME_TEXT, 124, 236, true, false);

            textHome(c, "THINK", 7.2f, HOME_TEXT, 346, 195, true, false);
            textHome(c, "PLAN", 7.2f, HOME_TEXT, 346, 207, true, false);
            textHome(c, "EXECUTE", 7.2f, HOME_TEXT, 346, 219, true, false);
            textHome(c, "VERIFY", 7.2f, HOME_TEXT, 346, 231, true, false);
        }

        private void drawHomeComposer(Canvas c) {
            roundGlowHome(c, 15, 250, 375, 396, HOME_PANEL, Color.argb(190, 127, 103, 255), 20, Color.argb(50, 93, 151, 255));
            drawSparkle(c, 35, 271);
            textHome(c, "MISSION / COMMAND", 8.5f, HOME_TEXT, 50, 279, false, true);
            roundGlowHome(c, 284, 260, 365, 294, Color.rgb(7, 20, 37), Color.argb(150, 87, 127, 213), 12, Color.TRANSPARENT);
            textHome(c, deepMode ? "Deep Mode" : "Quick Mode", 7.8f, HOME_TEXT, 306, 281, false, false);
            textHome(c, "⌄", 12, HOME_MUTED, 352, 280, true, false);

            roundGlowHome(c, 26, 287, 358, 345, Color.rgb(5, 16, 30), Color.argb(170, 109, 149, 237), 14, Color.argb(24, 76, 151, 255));

            drawComposerButton(c, 25, 350, 101, 388, "Context", 0);
            drawComposerButton(c, 106, 350, 181, 388, "Voice", 1);
            drawComposerButton(c, 185, 350, 261, 388, "Attach", 2);
            drawLaunchButton(c, 266, 350, 358, 388);
        }

        private void drawComposerButton(Canvas c, float l, float t, float r, float b, String label, int icon) {
            roundGlowHome(c, l, t, r, b, Color.rgb(7, 23, 41), Color.argb(145, 71, 116, 179), 12, Color.TRANSPARENT);
            if (icon == 0) drawContextIcon(c, l + 17, t + 19);
            else if (icon == 1) drawMicIcon(c, l + 17, t + 19);
            else drawAttachIcon(c, l + 17, t + 19);
            textHome(c, label, 7.8f, HOME_TEXT, l + 32, t + 24, false, false);
        }

        private void drawLaunchButton(Canvas c, float l, float t, float r, float b) {
            p.setShader(new LinearGradient(HX(l), HY(t), HX(r), HY(b),
                    new int[]{Color.rgb(113, 75, 245), Color.rgb(95, 96, 255), Color.rgb(62, 170, 255)},
                    null, Shader.TileMode.CLAMP));
            p.setStyle(Paint.Style.FILL); p.setShadowLayer(HX(14), 0, 0, Color.argb(120, 95, 87, 255));
            c.drawRoundRect(new RectF(HX(l), HY(t), HX(r), HY(b)), HX(12), HX(12), p); p.clearShadowLayer(); p.setShader(null);
            drawPlayIcon(c, l + 22, t + 19);
            textHome(c, "Launch", 8.4f, Color.WHITE, l + 43, t + 24, false, true);
        }

        private void drawHomeFeatures(Canvas c) {
            String[] titles = {"Smart Plan", "Multi-Agent", "Auto Test", "Deploy"};
            String[] subs = {"Break down ideas", "Execute with AI", "Verify & secure", "Ship to production"};
            float[] xs = {15, 107, 199, 291};
            for (int i = 0; i < 4; i++) {
                roundGlowHome(c, xs[i], 408, xs[i] + 84, 481, Color.rgb(5, 20, 36), Color.argb(140, 62, 125, 203), 17, Color.argb(18, 69, 149, 255));
                drawFeatureIcon(c, xs[i] + 19, 430, i);
                textHome(c, titles[i], 8.3f, HOME_TEXT, xs[i] + 12, 459, false, true);
                textHome(c, subs[i], 6.7f, HOME_MUTED, xs[i] + 12, 472, false, false);
                textHome(c, "›", 16, Color.rgb(205, 220, 242), xs[i] + 73, 433, true, false);
            }
        }

        private void drawHomeMetrics(Canvas c) {
            roundGlowHome(c, 15, 495, 375, 557, Color.rgb(5, 21, 38), Color.argb(170, 67, 129, 220), 17, Color.argb(20, 68, 148, 255));
            stroke.setStrokeWidth(HX(1)); stroke.setColor(Color.argb(80, 84, 124, 177));
            c.drawLine(HX(108), HY(507), HX(108), HY(545), stroke); c.drawLine(HX(199), HY(507), HX(199), HY(545), stroke); c.drawLine(HX(289), HY(507), HX(289), HY(545), stroke);
            drawMetric(c, 23, 526, "128", "Missions", 0); drawMetric(c, 114, 526, "24", "Projects", 1); drawMetric(c, 205, 526, "98%", "Success Rate", 2); drawMetric(c, 296, 526, "2.4x", "Faster", 3);
        }

        private void drawMetric(Canvas c, float x, float y, String value, String label, int icon) {
            drawMetricIcon(c, x + 11, y - 14, icon);
            textHome(c, value, 12.5f, HOME_TEXT, x + 33, y - 4, false, true);
            textHome(c, label, 7.1f, HOME_MUTED, x + 33, y + 11, false, false);
        }

        private void drawHomeRecent(Canvas c) {
            roundGlowHome(c, 15, 570, 375, 746, Color.rgb(4, 18, 33), Color.argb(175, 66, 127, 219), 18, Color.argb(18, 60, 137, 255));
            drawRecentHeaderIcon(c, 31, 591);
            textHome(c, "Recent Activity", 9.6f, HOME_TEXT, 48, 596, false, true);
            roundGlowHome(c, 315, 580, 365, 608, Color.rgb(6, 20, 37), Color.argb(115, 78, 128, 206), 12, Color.TRANSPARENT);
            textHome(c, "View All", 7.2f, HOME_TEXT, 340, 598, true, false);
            drawRecentItem(c, 22, 618, "Build authentication system", "Completed successfully", "2h ago", "Success", 0);
            drawRecentItem(c, 22, 669, "Design modern UI components", "Generated 24 components", "5h ago", "Completed", 1);
            drawRecentItem(c, 22, 720, "Optimize database queries", "Performance improved by 70%", "1d ago", "Optimized", 2);
        }

        private void drawRecentItem(Canvas c, float x, float y, String title, String sub, String time, String status, int icon) {
            roundGlowHome(c, x, y, 368, y + 43, Color.rgb(6, 24, 42), Color.argb(120, 73, 118, 187), 14, Color.TRANSPARENT);
            int accent = icon == 0 ? Color.rgb(34, 209, 161) : icon == 1 ? Color.rgb(47, 118, 255) : HOME_PURPLE;
            p.setColor(Color.argb(95, Color.red(accent), Color.green(accent), Color.blue(accent))); p.setShadowLayer(HX(10),0,0,Color.argb(100,accent)); c.drawRoundRect(new RectF(HX(x + 8), HY(y + 8), HX(x + 38), HY(y + 36)), HX(8), HX(8), p); p.clearShadowLayer();
            p.setColor(accent); c.drawRoundRect(new RectF(HX(x + 8), HY(y + 8), HX(x + 38), HY(y + 36)), HX(8), HX(8), p);
            if (icon == 0) drawCodeIcon(c, x + 23, y + 22); else if (icon == 1) drawDocIcon(c, x + 23, y + 22); else drawGearIcon(c, x + 23, y + 22);
            textHome(c, title, 7.8f, HOME_TEXT, x + 48, y + 17, false, true);
            textHome(c, sub, 6.8f, HOME_MUTED, x + 48, y + 31, false, false);
            textHome(c, time, 6.8f, HOME_MUTED, 338, y + 14, true, false);
            roundGlowHome(c, 278, y + 22, 334, y + 38, Color.rgb(6, 31, 33), Color.argb(115, 37, 194, 156), 9, Color.TRANSPARENT);
            textHome(c, status, 6.6f, HOME_GREEN, 306, y + 33, true, true);
            textHome(c, "⋮", 14, HOME_TEXT, 355, y + 28, true, false);
        }

        private void drawBottomNav(Canvas c) {
            if (page == HOME) {
                roundGlowHome(c, 11, 757, 379, 840, Color.rgb(5, 16, 31), Color.argb(185, 76, 125, 224), 20, Color.argb(18, 96, 112, 255));
                navItemHome(c, 45, "⌂", "Home", true);
                navItemHome(c, 120, "▤", "Activity", false);
                navItemHome(c, 195, "□", "Results", false);
                navItemHome(c, 270, "♙", "Control", false);
                navItemHome(c, 345, "•••", "More", false);
            } else {
                round(c,8,690,382,760,Color.rgb(10,14,15),Color.rgb(33,38,37),24);
                navItem(c,40,"⌂","Home",page==HOME); navItem(c,117,"◌","Runs",page==RUNS||page==DETAILS); navItem(c,195,"▣","Artifacts",page==ARTIFACTS); navItem(c,273,"⌘","Tools",page==TOOLS); navItem(c,350,"◯","Profile",page==PROFILE||page==SETTINGS||page==VOICE);
            }
        }

        private void navItemHome(Canvas c, float x, String icon, String label, boolean active) {
            if (active) {
                p.setShader(new RadialGradient(HX(x), HY(806), HX(36), new int[]{Color.argb(90, 131, 79, 255), Color.TRANSPARENT}, null, Shader.TileMode.CLAMP));
                c.drawCircle(HX(x), HY(806), HX(36), p); p.setShader(null);
            }
            textHome(c, icon, label.equals("More") ? 14 : 18, active ? Color.WHITE : Color.rgb(206, 219, 238), x, 785, true, active);
            textHome(c, label, 7.2f, active ? Color.WHITE : HOME_MUTED, x, 826, true, false);
        }

        private void drawDrawer(Canvas c) {
            p.setColor(Color.argb(150,0,0,0)); c.drawRect(0,0,getWidth(),getHeight(),p); float dh = getHeight() / s();
            round(c,0,0,320,Math.max(760, dh),Color.rgb(13,17,18),Color.rgb(47,50,46),0);
            text(c,"VEYTRIX",17,GOLD_LIGHT,30,42,false); text(c,"AUTOPILOT",8,DIM,31,59,false);
            round(c,22,83,298,140,PANEL_2,LINE,18); circle(c,53,111,19,Color.rgb(186,164,122)); text(c,"V",15,Color.rgb(36,31,23),53,117,true);
            text(c,"Local workspace",12,WHITE,83,106,false); text(c,"PRO PLAN",8,GOLD,83,124,false);
            drawerRow(c,166,"⌂","Home",HOME); drawerRow(c,218,"◌","Mission History",RUNS); drawerRow(c,270,"▣","Artifacts",ARTIFACTS); drawerRow(c,322,"⌘","Tools",TOOLS);
            drawerRow(c,374,"⚙","Settings",SETTINGS); drawerRow(c,426,"♩","Voice Input",VOICE); drawerRow(c,478,"✓","Verification",COMPLETED);
            round(c,22,610,298,674,Color.rgb(31,30,26),GOLD,20); text(c,"✦",15,GOLD_LIGHT,43,649,true); text(c,"Pro workspace",12,WHITE,64,643,false); text(c,"Unlock more power",9,MUTED,64,658,false); text(c,"›",20,GOLD_LIGHT,279,651,true);
        }

        private void drawerRow(Canvas c,float y,String icon,String label,int target){ boolean active=page==target; if(active) round(c,18,y-24,302,y+20,Color.rgb(29,32,31),Color.TRANSPARENT,14); text(c,icon,16,active?GOLD_LIGHT:MUTED,35,y+2,true); text(c,label,12,active?WHITE:MUTED,62,y+2,false); }

        private void drawHomeHeaderPlaceholder() {}

        private void drawRuns(Canvas c) {
            top(c,"",false);
            round(c,18,89,132,120,Color.rgb(16,35,30),Color.rgb(47,118,88),18); circle(c,35,104,6,GREEN); text(c,"EXECUTING",10,GREEN,51,110,true); text(c,"00:02:17",10,MUTED,337,110,true);
            String m = mission.isEmpty()?"Create a research report on renewable energy trends":mission;
            text(c,m,13.5f,WHITE,24,147,false); drawCore(c,195,285,64); checklist(c,24,382);
            round(c,24,560,366,614,Color.rgb(247,225,181),GOLD_LIGHT,26); text(c,paused?"Resume Mission":"Pause Mission",13,Color.rgb(34,32,28),195,593,true);
            round(c,24,628,366,674,PANEL_2,LINE,22); text(c,"Open run details",12,WHITE,195,656,true);
        }

        private void drawDetails(Canvas c) {
            top(c,"Mission Run",true); round(c,18,85,372,146,PANEL,LINE,18);
            round(c,30,98,132,124,Color.rgb(20,49,40),Color.rgb(55,140,105),14); circle(c,43,111,5,GREEN); text(c,"COMPLETED",9,GREEN,54,115,true); text(c,"2m 14s",10,MUTED,336,115,true);
            String m = mission.isEmpty()?"Research report on renewable energy trends":mission; text(c,m,13,WHITE,30,139,false);
            text(c,"Timeline",10,GOLD_LIGHT,42,175,true); text(c,"Files",10,MUTED,120,175,true); text(c,"Summary",10,MUTED,167,175,true); timeline(c,32,211);
        }

        private void drawArtifacts(Canvas c) {
            top(c,"Artifacts",true); chip(c,18,84,66,"All",true); chip(c,90,84,80,"Docs",false); chip(c,176,84,88,"Images",false); chip(c,270,84,86,"Code",false);
            artifact(c,18,134,Color.rgb(239,93,79),"Research_Report.pdf","2.4 MB · Today 09:43"); artifact(c,18,198,Color.rgb(80,205,157),"Market_Analysis.xlsx","1.1 MB · Today 09:40");
            artifact(c,18,262,Color.rgb(88,155,223),"Summary.md","12 KB · Today 09:38"); artifact(c,18,326,Color.rgb(92,151,239),"Chart.png","542 KB · Today 09:36");
            artifact(c,18,390,Color.rgb(73,186,235),"Code_Snippet.py","4 KB · Today 09:35"); text(c,"24 outputs",10,DIM,345,452,true);
        }

        private void drawTools(Canvas c) {
            top(c,"Tools",true); text(c,"POWERFUL BUILT-IN TOOLS",9,GOLD,20,95,false);
            tool(c,18,112,"⌕","Web Search","Research & gather",GOLD); tool(c,202,112,"<>" ,"Code Runner","Execute code",TEAL);
            tool(c,18,220,"▣","File Manager","Organize files",GOLD_LIGHT); tool(c,202,220,"◉","Browser","Automate web",TEAL);
            tool(c,18,328,"▥","Data Analyzer","Analyze data",GOLD); tool(c,202,328,"▣","Image Generator","Create visuals",GREEN);
            tool(c,18,436,"▤","Report Builder","Generate docs",GOLD_LIGHT); tool(c,202,436,"•••","More Tools","Coming soon",DIM);
        }

        private void drawProfile(Canvas c) {
            top(c,"Profile",true); round(c,18,86,372,212,PANEL,LINE,20); circle(c,58,126,30,Color.rgb(177,157,116)); text(c,"V",24,Color.rgb(31,28,22),58,134,true);
            text(c,"VEYTRIX USER",9,GOLD,104,118,false); text(c,"Local workspace",16,WHITE,104,144,false); pill(c,300,108,352,130,"PRO",GOLD);
            text(c,"48",23,WHITE,82,196,true); text(c,"36",23,WHITE,195,196,true); text(c,"12h",23,WHITE,309,196,true);
            text(c,"MISSIONS",8,DIM,82,214,true); text(c,"ARTIFACTS",8,DIM,195,214,true); text(c,"SAVED TIME",8,DIM,309,214,true);
            row(c,18,320,"⌂","Account Settings","Profile & preferences"); row(c,18,378,"◫","Usage & Billing","Plan and usage");
            row(c,18,436,"?","Help & Support","Get help with VEYTRIX"); row(c,18,494,"◈","Privacy & Security","Your data controls");
        }

        private void drawSettings(Canvas c) {
            top(c,"Settings",true); setting(c,18,92,"☾","Dark Mode","Always on",true); setting(c,18,148,"♩","Voice Input","Hands-free missions",true); setting(c,18,204,"♢","Notifications","Mission events and results",true);
            row(c,18,276,"✦","AI Models","Choose planning and execution models"); row(c,18,334,"◈","Privacy & Security","Permissions, storage and policy");
            row(c,18,392,"◉","Appearance","Theme, motion and density"); row(c,18,450,"i","About VEYTRIX","Autonomous execution platform");
        }

        private void drawVoice(Canvas c) {
            top(c,"Voice Input",true); text(c,"SPEAK YOUR MISSION CLEARLY",9,GOLD,195,105,true); drawVoiceCore(c,195,292);
            text(c,"Listening...",22,WHITE,195,445,true); text(c,"Say what you want VEYTRIX to accomplish.",11,MUTED,195,470,true);
            round(c,166,514,224,572,PANEL_2,LINE,29); text(c,"×",24,WHITE,195,551,true);
        }

        private void drawCompleted(Canvas c) {
            top(c,"",false); drawCheckCore(c,195,235); text(c,"Mission Completed",22,WHITE,195,390,true);
            text(c,"Your result has been generated",11,MUTED,195,420,true); text(c,"and verified successfully.",11,MUTED,195,438,true);
            round(c,24,474,366,528,Color.rgb(247,225,181),GOLD_LIGHT,27); text(c,"View Results",13,Color.rgb(34,32,27),195,507,true);
            round(c,24,540,366,592,PANEL_2,LINE,26); text(c,"Start New Mission",12,WHITE,195,573,true);
        }

        private void drawCore(Canvas c,float cx,float cy,float r) {
            float rr=r*(1f+(float)Math.sin(pulse)*.015f);
            p.setShader(new RadialGradient(X(cx),Y(cy),X(rr*1.7f),new int[]{Color.argb(90,228,194,128),Color.argb(20,228,194,128),Color.TRANSPARENT},null,Shader.TileMode.CLAMP));
            c.drawCircle(X(cx),Y(cy),X(rr*1.7f),p); p.setShader(null);
            stroke.setColor(Color.argb(170,215,184,117)); stroke.setStrokeWidth(X(1));
            RectF ring=new RectF(X(cx-rr*1.7f),Y(cy-rr*.72f),X(cx+rr*1.7f),Y(cy+rr*.72f)); c.save(); c.rotate(-15,X(cx),Y(cy)); c.drawOval(ring,stroke); c.restore();
            RectF ring2=new RectF(X(cx-rr*1.25f),Y(cy-rr*1.6f),X(cx+rr*1.25f),Y(cy+rr*1.6f)); stroke.setColor(Color.argb(90,215,184,117)); c.save(); c.rotate(19,X(cx),Y(cy)); c.drawOval(ring2,stroke); c.restore();
            p.setShader(new RadialGradient(X(cx-rr*.30f),Y(cy-rr*.38f),X(rr*1.15f),new int[]{Color.rgb(255,248,225),Color.rgb(224,198,143),Color.rgb(116,88,43),Color.rgb(24,21,17)},new float[]{0f,.18f,.52f,1f},Shader.TileMode.CLAMP));
            c.drawCircle(X(cx),Y(cy),X(rr),p); p.setShader(null); p.setColor(Color.argb(190,255,248,224)); c.drawCircle(X(cx-rr*.28f),Y(cy-rr*.33f),X(rr*.16f),p); text(c,"V",Math.max(16,rr*.43f),Color.rgb(250,238,211),cx,cy+rr*.20f,true);
        }

        private void drawVoiceCore(Canvas c,float cx,float cy){ for(int i=0;i<4;i++){stroke.setColor(Color.argb(90-i*14,225,190,124));stroke.setStrokeWidth(X(1));c.drawCircle(X(cx),Y(cy),X(70+i*19),stroke);} drawCore(c,cx,cy,54); }
        private void drawCheckCore(Canvas c,float cx,float cy){ p.setShader(new RadialGradient(X(cx),Y(cy),X(90),new int[]{Color.argb(70,228,194,128),Color.TRANSPARENT},null,Shader.TileMode.CLAMP)); c.drawCircle(X(cx),Y(cy),X(90),p); p.setShader(null); stroke.setColor(GOLD_LIGHT);stroke.setStrokeWidth(X(3));c.drawCircle(X(cx),Y(cy),X(55),stroke);Path path=new Path();path.moveTo(X(cx-20),Y(cy+3));path.lineTo(X(cx-5),Y(cy+18));path.lineTo(X(cx+28),Y(cy-20));c.drawPath(path,stroke); }
        private void checklist(Canvas c,float x,float y){String[] rows={"Understanding objective","Planning approach","Preparing environment","Executing tasks","Verifying results"};for(int i=0;i<rows.length;i++){float yy=y+i*30;circle(c,x+6,yy,4,i<3?GOLD_LIGHT:(i==3?TEAL:DIM));text(c,rows[i],11,i<3?MUTED:DIM,x+20,yy+4,false);if(i<3)text(c,"✓",11,GREEN,335,yy+4,true);}}
        private void timeline(Canvas c,float x,float y){stroke.setColor(Color.argb(150,110,205,163));stroke.setStrokeWidth(X(1.5f));c.drawLine(X(x+12),Y(y),X(x+12),Y(y+235),stroke);String[] a={"Request received","Planning complete","Data collection","Content generation","Verification","Completed"};String[] b={"Mission initialized","6 steps generated","Sources analyzed (12)","Report created","Quality check passed","All tasks finished"};for(int i=0;i<a.length;i++){float yy=y+i*40;circle(c,x+12,yy,4,i==0||i>=3?GREEN:GOLD_LIGHT);text(c,a[i],10.5f,WHITE,x+28,yy+2,false);text(c,b[i],8.5f,DIM,x+28,yy+15,false);text(c,String.format("09:%02d",41+i),8,MUTED,340,yy+3,true);}}
        private void artifact(Canvas c,float x,float y,int color,String name,String meta){round(c,x,y,372,y+52,Color.rgb(12,17,18),Color.rgb(30,35,34),15);round(c,x+10,y+10,x+42,y+42,Color.rgb(Color.red(color)/2+40,Color.green(color)/2+40,Color.blue(color)/2+40),color,9);text(c,"▤",16,WHITE,x+26,y+31,true);text(c,name,10.5f,WHITE,x+54,y+22,false);text(c,meta,8.3f,DIM,x+54,y+38,false);text(c,"⋮",17,MUTED,350,y+29,true);}
        private void tool(Canvas c,float x,float y,String icon,String title,String sub,int accent){round(c,x,y,x+170,y+96,Color.rgb(18,22,22),Color.rgb(41,44,40),18);round(c,x+10,y+10,x+47,y+47,Color.rgb(30,32,29),accent,11);text(c,icon,16,accent,x+28,y+35,true);text(c,title,11.5f,WHITE,x+12,y+67,false);text(c,sub,8.3f,DIM,x+12,y+82,false);}
        private void setting(Canvas c,float x,float y,String icon,String title,String sub,boolean on){round(c,x,y,372,y+48,Color.rgb(16,20,20),Color.rgb(39,43,41),15);text(c,icon,14,GOLD_LIGHT,x+19,y+29,true);text(c,title,11,WHITE,x+42,y+20,false);text(c,sub,8.5f,DIM,x+42,y+35,false);pill(c,325,y+11,359,y+37,on?"ON":"OFF",on?GOLD:DIM);}
        private void row(Canvas c,float x,float y,String icon,String title,String sub){round(c,x,y,372,y+48,Color.rgb(15,19,19),Color.rgb(37,42,40),15);text(c,icon,14,MUTED,x+19,y+29,true);text(c,title,11,WHITE,x+42,y+20,false);text(c,sub,8.5f,DIM,x+42,y+35,false);text(c,"›",18,MUTED,351,y+30,true);}
        private void chip(Canvas c,float x,float y,float w,String label,boolean active){round(c,x,y,x+w,y+36,active?Color.rgb(247,225,181):PANEL_2,active?GOLD_LIGHT:LINE,18);text(c,label,10,active?Color.rgb(31,29,23):MUTED,x+w/2,y+23,true);}
        private void navItem(Canvas c,float x,String icon,String label,boolean active){if(active)round(c,x-29,699,x+29,751,Color.rgb(28,31,29),Color.TRANSPARENT,19);text(c,icon,18,active?GOLD_LIGHT:DIM,x,719,true);text(c,label,8.5f,active?WHITE:DIM,x,743,true);}
        private void miniStatus(Canvas c,float x,float y,String left,String right,int color){round(c,x,y,x+158,y+42,Color.rgb(14,18,18),Color.rgb(36,40,37),13);text(c,left,8,DIM,x+12,y+17,false);text(c,right,9,color,x+12,y+31,false);}
        private void pill(Canvas c,float l,float t,float r,float b,String label,int color){round(c,l,t,r,b,Color.rgb(33,31,26),color,12);text(c,label,8.5f,color,(l+r)/2,t+17,true);}
        private void round(Canvas c,float l,float t,float r,float b,int fill,int border,float radius){p.setShader(null);p.setStyle(Paint.Style.FILL);p.setColor(fill);RectF rr=new RectF(X(l),Y(t),X(r),Y(b));c.drawRoundRect(rr,X(radius),X(radius),p);if(border!=Color.TRANSPARENT){p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(X(1));p.setColor(border);c.drawRoundRect(rr,X(radius),X(radius),p);p.setStyle(Paint.Style.FILL);}}
        private void roundGlowHome(Canvas c,float l,float t,float r,float b,int fill,int border,float radius,int glow){p.setShader(null);p.setStyle(Paint.Style.FILL);p.setColor(fill);if(glow!=Color.TRANSPARENT)p.setShadowLayer(HX(10),0,0,glow);RectF rr=new RectF(HX(l),HY(t),HX(r),HY(b));c.drawRoundRect(rr,HX(radius),HX(radius),p);p.clearShadowLayer();if(border!=Color.TRANSPARENT){p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(Math.max(1f,HX(1)));p.setColor(border);c.drawRoundRect(rr,HX(radius),HX(radius),p);p.setStyle(Paint.Style.FILL);}}
        private void circle(Canvas c,float x,float y,float r,int color){p.setShader(null);p.setColor(color);c.drawCircle(X(x),Y(y),X(r),p);}
        private void text(Canvas c,String str,float size,int color,float x,float y,boolean center){p.setShader(null);p.setColor(color);p.setTextSize(X(size));p.setTypeface(Typeface.create("sans",size>=14?Typeface.BOLD:Typeface.NORMAL));p.setTextAlign(center?Paint.Align.CENTER:Paint.Align.LEFT);c.drawText(str,X(x),Y(y),p);}
        private void textHome(Canvas c,String str,float size,int color,float x,float y,boolean center,boolean bold){p.setShader(null);p.setColor(color);p.setTextSize(HX(size));p.setTypeface(Typeface.create("sans-serif",bold?Typeface.BOLD:Typeface.NORMAL));p.setTextAlign(center?Paint.Align.CENTER:Paint.Align.LEFT);c.drawText(str,HX(x),HY(y),p);}

        private void drawBrandMark(Canvas c,float cx,float cy){Path v=new Path();v.moveTo(HX(cx-13),HY(cy-14));v.lineTo(HX(cx-4),HY(cy+3));v.lineTo(HX(cx+4),HY(cy+18));v.lineTo(HX(cx+13),HY(cy-14));v.lineTo(HX(cx+5),HY(cy-14));v.lineTo(HX(cx),HY(cy+1));v.lineTo(HX(cx-5),HY(cy-14));v.close();p.setShader(new LinearGradient(HX(cx-12),HY(cy-14),HX(cx+13),HY(cy+15),new int[]{Color.rgb(84,208,255),Color.rgb(116,96,255),Color.rgb(192,67,255)},null,Shader.TileMode.CLAMP));c.drawPath(v,p);p.setShader(null);}
        private void drawBell(Canvas c,float cx,float cy){stroke.setColor(Color.WHITE);stroke.setStyle(Paint.Style.STROKE);stroke.setStrokeCap(Paint.Cap.ROUND);stroke.setStrokeWidth(HX(1.8f));Path q=new Path();q.moveTo(HX(cx-6),HY(cy+2));q.quadTo(HX(cx-5),HY(cy-7),HX(cx),HY(cy-7));q.quadTo(HX(cx+5),HY(cy-7),HX(cx+6),HY(cy+2));q.lineTo(HX(cx+7),HY(cy+5));q.lineTo(HX(cx-7),HY(cy+5));q.close();c.drawPath(q,stroke);c.drawLine(HX(cx-3),HY(cy+8),HX(cx+3),HY(cy+8),stroke);stroke.setStrokeCap(Paint.Cap.BUTT);}
        private void drawSparkle(Canvas c,float cx,float cy){Path q=new Path();q.moveTo(HX(cx),HY(cy-9));q.lineTo(HX(cx+4),HY(cy-3));q.lineTo(HX(cx+10),HY(cy));q.lineTo(HX(cx+4),HY(cy+4));q.lineTo(HX(cx),HY(cy+11));q.lineTo(HX(cx-4),HY(cy+4));q.lineTo(HX(cx-10),HY(cy));q.lineTo(HX(cx-4),HY(cy-3));q.close();p.setShader(new LinearGradient(HX(cx-8),HY(cy-8),HX(cx+8),HY(cy+8),new int[]{Color.rgb(197,171,255),Color.rgb(100,174,255)},null,Shader.TileMode.CLAMP));c.drawPath(q,p);p.setShader(null);}
        private void drawContextIcon(Canvas c,float cx,float cy){stroke.setColor(Color.WHITE);stroke.setStrokeWidth(HX(1.3f));Path a=new Path();a.moveTo(HX(cx-9),HY(cy-4));a.lineTo(HX(cx),HY(cy-10));a.lineTo(HX(cx+9),HY(cy-4));a.lineTo(HX(cx),HY(cy+2));a.close();c.drawPath(a,stroke);Path b=new Path();b.moveTo(HX(cx-9),HY(cy+2));b.lineTo(HX(cx),HY(cy+8));b.lineTo(HX(cx+9),HY(cy+2));b.close();c.drawPath(b,stroke);}
        private void drawMicIcon(Canvas c,float cx,float cy){stroke.setColor(HOME_BLUE);stroke.setStrokeWidth(HX(1.8f));stroke.setStyle(Paint.Style.STROKE);c.drawRoundRect(new RectF(HX(cx-4),HY(cy-9),HX(cx+4),HY(cy+3)),HX(4),HX(4),stroke);c.drawArc(new RectF(HX(cx-8),HY(cy-3),HX(cx+8),HY(cy+8)),0,180,false,stroke);c.drawLine(HX(cx),HY(cy+8),HX(cx),HY(cy+11),stroke);c.drawLine(HX(cx-4),HY(cy+11),HX(cx+4),HY(cy+11),stroke);}
        private void drawAttachIcon(Canvas c,float cx,float cy){stroke.setColor(Color.WHITE);stroke.setStrokeWidth(HX(1.7f));stroke.setStyle(Paint.Style.STROKE);Path q=new Path();q.moveTo(HX(cx+6),HY(cy-7));q.cubicTo(HX(cx+11),HY(cy-2),HX(cx+7),HY(cy+4),HX(cx+3),HY(cy+8));q.cubicTo(HX(cx-3),HY(cy+13),HX(cx-10),HY(cy+7),HX(cx-6),HY(cy+2));q.lineTo(HX(cx+4),HY(cy-8));c.drawPath(q,stroke);}
        private void drawPlayIcon(Canvas c,float cx,float cy){p.setColor(Color.WHITE);Path q=new Path();q.moveTo(HX(cx-5),HY(cy-8));q.lineTo(HX(cx+8),HY(cy));q.lineTo(HX(cx-5),HY(cy+8));q.close();c.drawPath(q,p);}
        private void drawFeatureIcon(Canvas c,float cx,float cy,int kind){int accent=kind==0?Color.rgb(194,165,255):kind==1?Color.rgb(225,231,255):kind==2?Color.WHITE:Color.rgb(196,218,255);stroke.setColor(accent);stroke.setStrokeWidth(HX(2));stroke.setStyle(Paint.Style.STROKE);if(kind==0){c.drawCircle(HX(cx),HY(cy),HX(8),stroke);c.drawLine(HX(cx),HY(cy-11),HX(cx),HY(cy+11),stroke);c.drawLine(HX(cx-11),HY(cy),HX(cx+11),HY(cy),stroke);}else if(kind==1){c.drawCircle(HX(cx-5),HY(cy-3),HX(4),stroke);c.drawCircle(HX(cx+5),HY(cy-3),HX(4),stroke);c.drawArc(new RectF(HX(cx-11),HY(cy+1),HX(cx+1),HY(cy+11)),180,180,false,stroke);c.drawArc(new RectF(HX(cx-1),HY(cy+1),HX(cx+11),HY(cy+11)),180,180,false,stroke);}else if(kind==2){Path q=new Path();q.moveTo(HX(cx-10),HY(cy));q.lineTo(HX(cx-4),HY(cy+6));q.lineTo(HX(cx+9),HY(cy-9));c.drawPath(q,stroke);c.drawRect(HX(cx-11),HY(cy-11),HX(cx+11),HY(cy+11),stroke);}else{Path q=new Path();q.moveTo(HX(cx-10),HY(cy+6));q.lineTo(HX(cx),HY(cy-10));q.lineTo(HX(cx+10),HY(cy+6));q.lineTo(HX(cx),HY(cy+1));q.close();c.drawPath(q,stroke);c.drawCircle(HX(cx),HY(cy-11),HX(2),stroke);}}
        private void drawMetricIcon(Canvas c,float cx,float cy,int kind){stroke.setColor(kind==0?Color.rgb(189,164,255):kind==1?Color.rgb(170,132,255):kind==2?Color.rgb(167,147,255):Color.rgb(165,181,255));stroke.setStrokeWidth(HX(2.2f));if(kind==0){Path q=new Path();q.moveTo(HX(cx-5),HY(cy-11));q.lineTo(HX(cx+2),HY(cy-2));q.lineTo(HX(cx-2),HY(cy-2));q.lineTo(HX(cx+5),HY(cy+9));q.lineTo(HX(cx-3),HY(cy));q.lineTo(HX(cx+1),HY(cy));q.close();p.setColor(stroke.getColor());c.drawPath(q,p);}else if(kind==1){p.setColor(stroke.getColor());c.drawRect(HX(cx-10),HY(cy-6),HX(cx-2),HY(cy+9),p);c.drawRoundRect(new RectF(HX(cx-1),HY(cy-10),HX(cx+7),HY(cy+9)),HX(2),HX(2),p);c.drawRoundRect(new RectF(HX(cx+8),HY(cy-8),HX(cx+16),HY(cy+9)),HX(2),HX(2),p);}else if(kind==2){p.setColor(stroke.getColor());c.drawRect(HX(cx-9),HY(cy-4),HX(cx-1),HY(cy+10),p);c.drawRect(HX(cx+2),HY(cy-10),HX(cx+10),HY(cy+10),p);c.drawRect(HX(cx+13),HY(cy-1),HX(cx+21),HY(cy+10),p);}else{stroke.setStyle(Paint.Style.STROKE);c.drawCircle(HX(cx+1),HY(cy),HX(9),stroke);c.drawLine(HX(cx+1),HY(cy),HX(cx+1),HY(cy-5),stroke);c.drawLine(HX(cx+1),HY(cy),HX(cx+6),HY(cy+3),stroke);}}
        private void drawRecentHeaderIcon(Canvas c,float cx,float cy){p.setColor(Color.rgb(179,139,255));c.drawCircle(HX(cx),HY(cy),HX(8),p);stroke.setColor(Color.rgb(236,231,255));stroke.setStrokeWidth(HX(1.3f));c.drawLine(HX(cx),HY(cy-4),HX(cx),HY(cy+4),stroke);c.drawLine(HX(cx-3),HY(cy),HX(cx+3),HY(cy),stroke);}
        private void drawCodeIcon(Canvas c,float cx,float cy){stroke.setColor(Color.WHITE);stroke.setStrokeWidth(HX(1.6f));c.drawLine(HX(cx-7),HY(cy),HX(cx-2),HY(cy-4),stroke);c.drawLine(HX(cx-7),HY(cy),HX(cx-2),HY(cy+4),stroke);c.drawLine(HX(cx+7),HY(cy),HX(cx+2),HY(cy-4),stroke);c.drawLine(HX(cx+7),HY(cy),HX(cx+2),HY(cy+4),stroke);c.drawLine(HX(cx-2),HY(cy+5),HX(cx+2),HY(cy-5),stroke);}
        private void drawDocIcon(Canvas c,float cx,float cy){p.setColor(Color.WHITE);c.drawRoundRect(new RectF(HX(cx-6),HY(cy-8),HX(cx+6),HY(cy+8)),HX(2),HX(2),p);p.setColor(Color.rgb(77,130,255));c.drawRect(HX(cx-3),HY(cy-3),HX(cx+3),HY(cy-1),p);c.drawRect(HX(cx-3),HY(cy+1),HX(cx+3),HY(cy+3),p);}
        private void drawGearIcon(Canvas c,float cx,float cy){stroke.setColor(Color.WHITE);stroke.setStrokeWidth(HX(1.6f));c.drawCircle(HX(cx),HY(cy),HX(6),stroke);c.drawCircle(HX(cx),HY(cy),HX(2),stroke);for(int i=0;i<8;i++){double a=i*Math.PI/4;float x1=(float)Math.cos(a)*8,y1=(float)Math.sin(a)*8;float x2=(float)Math.cos(a)*11,y2=(float)Math.sin(a)*11;c.drawLine(HX(cx+x1),HY(cy+y1),HX(cx+x2),HY(cy+y2),stroke);}}

        @Override public boolean onTouchEvent(MotionEvent e){
            if(e.getAction()!=MotionEvent.ACTION_UP)return true;
            float x=e.getX()/s(),y=e.getY()/s();
            if(drawer){if(x>320){drawer=false;if(page==HOME)showComposer();invalidate();return true;}if(y>145&&y<205)select(HOME);else if(y>=205&&y<255)select(RUNS);else if(y>=255&&y<315)select(ARTIFACTS);else if(y>=315&&y<365)select(TOOLS);else if(y>=365&&y<418)select(SETTINGS);else if(y>=418&&y<470)select(VOICE);else if(y>=470&&y<520)select(COMPLETED);return true;}
            if(y<68&&x<70){drawer=true;hideComposer();invalidate();return true;}
            if(page==HOME){
                if(y<72&&x>292&&x<338){Toast.makeText(MainActivity.this,"Notifications are not connected in this build",Toast.LENGTH_SHORT).show();return true;}
                if(y<72&&x>=338){select(PROFILE);return true;}
                if(y>=408&&y<482){Toast.makeText(MainActivity.this,"Feature shortcut not connected in this build",Toast.LENGTH_SHORT).show();return true;}
                if(y>=570&&y<612){select(RUNS);return true;}
                if(y>=612&&y<746){select(RUNS);return true;}
                if(y>=748){if(x<78)select(HOME);else if(x<156)select(RUNS);else if(x<234)select(ARTIFACTS);else if(x<312)select(TOOLS);else select(PROFILE);return true;}
                return true;
            }
            if(y>680){if(x<78)select(HOME);else if(x<156)select(RUNS);else if(x<234)select(ARTIFACTS);else if(x<312)select(TOOLS);else select(PROFILE);return true;}
            if(page==RUNS&&y>550&&y<620){paused=!paused;invalidate();return true;}if(page==RUNS&&y>=620&&y<680){select(DETAILS);return true;}if(page==COMPLETED&&y>530&&y<610){select(HOME);showComposer();return true;}if(page==PROFILE&&y>365&&y<435){select(SETTINGS);return true;}if(page==VOICE&&y>500){select(HOME);showComposer();return true;}return true;
        }
        private void select(int target){page=target;drawer=false;if(target==HOME)showComposer();else hideComposer();invalidate();}
    }
}
