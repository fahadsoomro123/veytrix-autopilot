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
import android.text.InputType;
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
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
        root.addView(coreView, new FrameLayout.LayoutParams(dp(140), dp(148)));

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
        prompt.setPadding(dp(14), dp(10), dp(10), dp(8));
        prompt.setBackgroundColor(Color.TRANSPARENT);
        root.addView(prompt, new FrameLayout.LayoutParams(dp(332), dp(58)));

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
        root.addView(send, new FrameLayout.LayoutParams(dp(86), dp(40)));

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

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private float homeScale() {
        if (surface == null || surface.getWidth() <= 0 || surface.getHeight() <= 0) return 1f;
        float density = getResources().getDisplayMetrics().density;
        float widthDp = surface.getWidth() / density;
        float heightDp = surface.getHeight() / density;
        return Math.min(widthDp / 390f, heightDp / 844f);
    }

    private void positionHomeControls() {
        if (surface == null || surface.getWidth() <= 0 || surface.getHeight() <= 0) return;
        float density = getResources().getDisplayMetrics().density;
        float scale = homeScale();
        float contentWidthPx = 390f * scale * density;
        int xOffset = Math.round((surface.getWidth() - contentWidthPx) * .5f);

        place(coreView, xOffset + Math.round(205f * scale * density), Math.round(103f * scale * density), Math.round(140f * scale * density), Math.round(148f * scale * density));
        place(prompt, xOffset + Math.round(29f * scale * density), Math.round(317f * scale * density), Math.round(332f * scale * density), Math.round(58f * scale * density));
        place(modeAction, xOffset + Math.round(289f * scale * density), Math.round(273f * scale * density), Math.round(82f * scale * density), Math.round(34f * scale * density));
        place(contextAction, xOffset + Math.round(26f * scale * density), Math.round(374f * scale * density), Math.round(76f * scale * density), Math.round(40f * scale * density));
        place(mic, xOffset + Math.round(108f * scale * density), Math.round(374f * scale * density), Math.round(76f * scale * density), Math.round(40f * scale * density));
        place(attachAction, xOffset + Math.round(192f * scale * density), Math.round(374f * scale * density), Math.round(76f * scale * density), Math.round(40f * scale * density));
        place(send, xOffset + Math.round(276f * scale * density), Math.round(374f * scale * density), Math.round(86f * scale * density), Math.round(40f * scale * density));
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
            stroke.setStrokeJoin(Paint.Join.ROUND);
            setLayerType(View.LAYER_TYPE_SOFTWARE, null);
            animationStartNanos = System.nanoTime();
        }

        private float hs() { return Math.min(getWidth() / 390f, getHeight() / 844f); }
        private float HX(float n) { return (getWidth() - 390f * hs()) * .5f + n * hs(); }
        private float HY(float n) { return n * hs(); }

        @Override protected void onAttachedToWindow() {
            super.onAttachedToWindow();
            animationStartNanos = System.nanoTime();
            postInvalidateOnAnimation();
        }

        @Override protected void onDetachedFromWindow() {
            removeCallbacks(invalidateRunnable);
            super.onDetachedFromWindow();
        }

        private final Runnable invalidateRunnable = this::invalidate;

        @Override protected void onSizeChanged(int w, int h, int oldw, int oldh) {
            super.onSizeChanged(w, h, oldw, oldh);
            post(MainActivity.this::positionHomeControls);
        }

        @Override protected void onDraw(Canvas c) {
            pulse = (System.nanoTime() - animationStartNanos) / 1_000_000_000f;
            c.drawColor(BG);
            switch (page) {
                case HOME: drawHome(c); break;
                case RUNS: drawRuns(c); break;
                case DETAILS: drawDetails(c); break;
                case ARTIFACTS: drawArtifacts(c); break;
                case TOOLS: drawTools(c); break;
                case PROFILE: drawProfile(c); break;
                case SETTINGS: drawSettings(c); break;
                case VOICE: drawVoice(c); break;
                default: drawCompleted(c); break;
            }
            drawBottomNav(c);
            if (drawer) drawDrawer(c);
            if (getVisibility() == View.VISIBLE) postInvalidateOnAnimation();
        }

        private void drawHome(Canvas c) {
            drawHomeBackground(c); drawHomeHeader(c); drawHomeHero(c); drawHomeComposer(c); drawHomeFeatures(c); drawHomeMetrics(c); drawHomeRecent(c);
        }

        private void drawHomeBackground(Canvas c) {
            p.setShader(new LinearGradient(0, 0, 0, getHeight(), new int[]{Color.rgb(2, 8, 18), Color.rgb(3, 13, 26), Color.rgb(2, 7, 17)}, null, Shader.TileMode.CLAMP));
            c.drawRect(0, 0, getWidth(), getHeight(), p); p.setShader(null);
            glowCircle(c, 275, 122, 142, Color.argb(34, 57, 117, 239));
            glowCircle(c, 84, 360, 138, Color.argb(18, 147, 71, 255));
            glowCircle(c, 310, 690, 190, Color.argb(14, 38, 155, 255));
            stroke.setStrokeWidth(Math.max(.7f, hs())); stroke.setColor(Color.argb(12, 70, 129, 205));
            for (int i = -2; i < 7; i++) c.drawLine(HX(i * 95f), HY(844), HX(i * 95f + 340f), HY(520), stroke);
        }

        private void drawHomeHeader(Canvas c) {
            roundGlowHome(c, 17, 18, 61, 62, Color.rgb(6, 20, 38), Color.argb(150, 81, 144, 231), 14, Color.argb(36, 77, 141, 230));
            stroke.setColor(Color.WHITE); stroke.setStrokeWidth(HX(2.3f));
            c.drawLine(HX(29), HY(30), HX(49), HY(30), stroke); c.drawLine(HX(29), HY(40), HX(49), HY(40), stroke); c.drawLine(HX(29), HY(50), HX(49), HY(50), stroke);
            drawBrandMark(c, 145, 40);
            textHome(c, "VEYTRIX", 21, HOME_TEXT, 205, 44, true, true);
            textHome(c, "AUTONOMOUS AI DEVELOPMENT", 7.1f, HOME_MUTED, 205, 59, true, false);
            roundGlowHome(c, 309, 18, 343, 52, Color.rgb(7, 17, 31), Color.argb(125, 84, 146, 223), 17, Color.TRANSPARENT);
            drawBell(c, 326, 35); p.setColor(Color.rgb(255, 81, 111)); c.drawCircle(HX(337), HY(18), HX(3.5f), p);
            roundGlowHome(c, 349, 18, 383, 52, Color.rgb(7, 17, 31), Color.argb(130, 89, 153, 230), 17, Color.TRANSPARENT);
            textHome(c, "FH", 11.5f, HOME_TEXT, 366, 41, true, true); p.setColor(HOME_GREEN); c.drawCircle(HX(375), HY(50), HX(4), p);
        }

        private void drawHomeHero(Canvas c) {
            p.setColor(Color.argb(200, 143, 79, 255)); p.setShadowLayer(HX(8), 0, 0, Color.argb(115, 132, 76, 255));
            c.drawRoundRect(new RectF(HX(18), HY(78), HX(21), HY(194)), HX(1.5f), HX(1.5f), p); p.clearShadowLayer();
            textHome(c, "Build", 29, HOME_TEXT, 37, 112, false, true); textHome(c, "Without", 29, Color.rgb(191, 173, 255), 37, 142, false, true); textHome(c, "Limits.", 29, Color.rgb(205, 233, 255), 37, 172, false, true);
            textHome(c, "I D E A S  →  R E A L I T Y", 7.1f, HOME_MUTED, 37, 193, false, false); textHome(c, "A U T O N O M O U S L Y", 7.1f, HOME_TEXT, 37, 207, false, false);
            roundGlowHome(c, 17, 219, 143, 250, Color.rgb(5, 20, 35), Color.argb(145, 55, 122, 190), 17, Color.argb(20, 60, 169, 255));
            p.setColor(HOME_GREEN); p.setShadowLayer(HX(7), 0, 0, Color.argb(100, 44, 231, 177)); c.drawCircle(HX(31), HY(234), HX(4.2f), p); p.clearShadowLayer();
            textHome(c, "AI CORE", 7.1f, HOME_TEXT, 43, 238, false, true); textHome(c, "ONLINE", 7.1f, HOME_GREEN, 82, 238, false, true); textHome(c, "›", 16, HOME_TEXT, 132, 238, true, false);
            textHome(c, "THINK", 7.0f, HOME_TEXT, 351, 192, true, false); textHome(c, "PLAN", 7.0f, HOME_TEXT, 351, 205, true, false); textHome(c, "EXECUTE", 7.0f, HOME_TEXT, 351, 218, true, false); textHome(c, "VERIFY", 7.0f, HOME_TEXT, 351, 231, true, false);
        }

        private void drawHomeComposer(Canvas c) {
            roundGlowHome(c, 16, 263, 374, 421, HOME_PANEL, Color.argb(188, 125, 101, 255), 19, Color.argb(30, 93, 151, 255));
            drawSparkle(c, 35, 285); textHome(c, "MISSION / COMMAND", 9.2f, HOME_TEXT, 51, 292, false, true);
            roundGlowHome(c, 289, 273, 366, 307, Color.rgb(7, 19, 36), Color.argb(135, 87, 127, 213), 12, Color.TRANSPARENT);
            textHome(c, deepMode ? "Deep Mode" : "Quick Mode", 7.8f, HOME_TEXT, 326, 296, true, false); textHome(c, "⌄", 11.5f, HOME_MUTED, 355, 296, true, false);
            roundGlowHome(c, 27, 313, 363, 365, Color.rgb(4, 16, 30), Color.argb(165, 102, 144, 231), 14, Color.argb(16, 76, 151, 255));
            drawComposerButton(c, 26, 374, 100, 412, "Context", 0); drawComposerButton(c, 108, 374, 184, 412, "Voice", 1); drawComposerButton(c, 192, 374, 268, 412, "Attach", 2); drawLaunchButton(c, 276, 374, 364, 412);
        }

        private void drawComposerButton(Canvas c, float l, float t, float r, float b, String label, int icon) { roundGlowHome(c, l, t, r, b, Color.rgb(7, 23, 40), Color.argb(135, 68, 113, 174), 12, Color.TRANSPARENT); if (icon == 0) drawContextIcon(c, l + 17, t + 19); else if (icon == 1) drawMicIcon(c, l + 17, t + 19); else drawAttachIcon(c, l + 17, t + 19); textHome(c, label, 7.6f, HOME_TEXT, l + 32, t + 24, false, false); }
        private void drawLaunchButton(Canvas c, float l, float t, float r, float b) { p.setShader(new LinearGradient(HX(l), HY(t), HX(r), HY(b), new int[]{Color.rgb(112, 75, 242), Color.rgb(86, 101, 255), Color.rgb(58, 170, 255)}, null, Shader.TileMode.CLAMP)); p.setShadowLayer(HX(11), 0, 0, Color.argb(95, 90, 88, 255)); c.drawRoundRect(new RectF(HX(l), HY(t), HX(r), HY(b)), HX(12), HX(12), p); p.clearShadowLayer(); p.setShader(null); drawPlayIcon(c, l + 22, t + 19); textHome(c, "Launch", 8.5f, Color.WHITE, l + 44, t + 25, false, true); }

        private void drawHomeFeatures(Canvas c) { String[] titles = {"Smart Plan", "Multi-Agent", "Auto Test", "Deploy"}; String[] subs = {"Break down ideas", "Execute with AI", "Verify & secure", "Ship to production"}; float[] xs = {16, 108, 200, 292}; for (int i = 0; i < 4; i++) { roundGlowHome(c, xs[i], 438, xs[i] + 82, 513, Color.rgb(5, 20, 36), Color.argb(132, 62, 125, 203), 16, Color.argb(13, 69, 149, 255)); drawFeatureIcon(c, xs[i] + 18, 460, i); textHome(c, titles[i], 7.55f, HOME_TEXT, xs[i] + 10, 489, false, true); textHome(c, subs[i], 5.85f, HOME_MUTED, xs[i] + 10, 502, false, false); drawChevron(c, xs[i] + 70, 459); } }
        private void drawHomeMetrics(Canvas c) { roundGlowHome(c, 16, 525, 374, 588, Color.rgb(5, 21, 38), Color.argb(165, 67, 129, 220), 17, Color.argb(15, 68, 148, 255)); stroke.setStrokeWidth(HX(.8f)); stroke.setColor(Color.argb(75, 84, 124, 177)); c.drawLine(HX(105), HY(538), HX(105), HY(576), stroke); c.drawLine(HX(195), HY(538), HX(195), HY(576), stroke); c.drawLine(HX(285), HY(538), HX(285), HY(576), stroke); drawMetric(c, 22, 555, "128", "Missions", 0); drawMetric(c, 112, 555, "24", "Projects", 1); drawMetric(c, 202, 555, "98%", "Success Rate", 2); drawMetric(c, 292, 555, "2.4x", "Faster", 3); }
        private void drawMetric(Canvas c, float x, float y, String value, String label, int icon) { drawMetricIcon(c, x + 10, y - 12, icon); textHome(c, value, 11.6f, HOME_TEXT, x + 31, y - 3, false, true); textHome(c, label, 6.1f, HOME_MUTED, x + 31, y + 10, false, false); }
        private void drawHomeRecent(Canvas c) { roundGlowHome(c, 16, 600, 374, 780, Color.rgb(4, 18, 33), Color.argb(165, 66, 127, 219), 18, Color.argb(13, 60, 137, 255)); drawRecentHeaderIcon(c, 31, 621); textHome(c, "Recent Activity", 9.2f, HOME_TEXT, 48, 626, false, true); roundGlowHome(c, 314, 609, 366, 636, Color.rgb(6, 20, 37), Color.argb(110, 78, 128, 206), 12, Color.TRANSPARENT); textHome(c, "View All", 6.8f, HOME_TEXT, 340, 627, true, false); drawRecentItem(c, 23, 648, "Build authentication system", "Completed successfully", "2h", "Success", 0); drawRecentItem(c, 23, 696, "Design modern UI components", "Generated 24 components", "5h", "Completed", 1); drawRecentItem(c, 23, 744, "Optimize database queries", "Performance improved by 70%", "1d", "Optimized", 2); }
        private void drawRecentItem(Canvas c, float x, float y, String title, String sub, String time, String status, int icon) { roundGlowHome(c, x, y, 367, y + 40, Color.rgb(6, 24, 42), Color.argb(105, 73, 118, 187), 13, Color.TRANSPARENT); int accent = icon == 0 ? Color.rgb(34, 209, 161) : icon == 1 ? Color.rgb(47, 118, 255) : HOME_PURPLE; p.setColor(Color.argb(92, Color.red(accent), Color.green(accent), Color.blue(accent))); c.drawRoundRect(new RectF(HX(x + 8), HY(y + 7), HX(x + 36), HY(y + 33)), HX(8), HX(8), p); p.setColor(accent); c.drawRoundRect(new RectF(HX(x + 8), HY(y + 7), HX(x + 36), HY(y + 33)), HX(8), HX(8), p); if (icon == 0) drawCodeIcon(c, x + 22, y + 20); else if (icon == 1) drawDocIcon(c, x + 22, y + 20); else drawGearIcon(c, x + 22, y + 20); textHome(c, title, 6.8f, HOME_TEXT, x + 46, y + 15, false, true); textHome(c, sub, 5.9f, HOME_MUTED, x + 46, y + 28, false, false); textHome(c, time, 5.9f, HOME_MUTED, 337, y + 12, true, false); roundGlowHome(c, 281, y + 21, 331, y + 35, Color.rgb(6, 31, 33), Color.argb(105, 37, 194, 156), 8, Color.TRANSPARENT); textHome(c, status, 5.55f, HOME_GREEN, 306, y + 31, true, true); drawMoreIcon(c, 354, y + 20); }

        private void drawBottomNav(Canvas c) { roundGlowHome(c, 12, 790, 378, 840, Color.rgb(5, 16, 31), Color.argb(175, 76, 125, 224), 19, Color.argb(13, 96, 112, 255)); navItemHome(c, 50, "Home", true, 0); navItemHome(c, 124, "Activity", false, 1); navItemHome(c, 198, "Results", false, 2); navItemHome(c, 272, "Control", false, 3); navItemHome(c, 346, "More", false, 4); }
        private void navItemHome(Canvas c, float x, String label, boolean active, int kind) { if (active) { p.setShader(new RadialGradient(HX(x), HY(809), HX(25), new int[]{Color.argb(76, 132, 79, 255), Color.TRANSPARENT}, null, Shader.TileMode.CLAMP)); c.drawCircle(HX(x), HY(809), HX(25), p); p.setShader(null); } drawNavIcon(c, x, 807, kind, active); textHome(c, label, 6.7f, active ? Color.WHITE : HOME_MUTED, x, 836, true, false); }
        private void drawNavIcon(Canvas c, float cx, float cy, int kind, boolean active) { int color = active ? Color.WHITE : Color.rgb(205, 219, 238); stroke.setColor(color); stroke.setStyle(Paint.Style.STROKE); stroke.setStrokeWidth(HX(1.8f)); if (kind == 0) { Path q = new Path(); q.moveTo(HX(cx - 9), HY(cy - 1)); q.lineTo(HX(cx), HY(cy - 10)); q.lineTo(HX(cx + 9), HY(cy - 1)); q.lineTo(HX(cx + 9), HY(cy + 9)); q.lineTo(HX(cx + 2), HY(cy + 9)); q.lineTo(HX(cx + 2), HY(cy + 2)); q.lineTo(HX(cx - 2), HY(cy + 2)); q.lineTo(HX(cx - 2), HY(cy + 9)); q.lineTo(HX(cx - 9), HY(cy + 9)); q.close(); c.drawPath(q, stroke); } else if (kind == 1) { c.drawRect(HX(cx - 7), HY(cy - 9), HX(cx + 7), HY(cy + 9), stroke); c.drawLine(HX(cx - 4), HY(cy - 4), HX(cx + 4), HY(cy - 4), stroke); c.drawLine(HX(cx - 4), HY(cy), HX(cx + 4), HY(cy), stroke); c.drawLine(HX(cx - 4), HY(cy + 4), HX(cx + 4), HY(cy + 4), stroke); } else if (kind == 2) { c.drawRect(HX(cx - 8), HY(cy - 8), HX(cx + 8), HY(cy + 8), stroke); c.drawLine(HX(cx - 4), HY(cy), HX(cx - 1), HY(cy + 3), stroke); c.drawLine(HX(cx - 1), HY(cy + 3), HX(cx + 5), HY(cy - 4), stroke); } else if (kind == 3) { c.drawCircle(HX(cx - 5), HY(cy - 5), HX(5), stroke); c.drawCircle(HX(cx + 5), HY(cy - 5), HX(5), stroke); c.drawArc(new RectF(HX(cx - 11), HY(cy), HX(cx + 1), HY(cy + 11)), 180, 180, false, stroke); c.drawArc(new RectF(HX(cx - 1), HY(cy), HX(cx + 11), HY(cy + 11)), 180, 180, false, stroke); } else { fill(p, color); c.drawCircle(HX(cx - 7), HY(cy), HX(2), p); c.drawCircle(HX(cx), HY(cy), HX(2), p); c.drawCircle(HX(cx + 7), HY(cy), HX(2), p); } }

        private void drawRuns(Canvas c) { top(c, "Mission Run", false); roundGlowHome(c, 18, 90, 372, 124, Color.rgb(16, 35, 30), Color.rgb(47, 118, 88), 17, Color.TRANSPARENT); circle(c, 36, 107, 5, HOME_GREEN); textHome(c, "EXECUTING", 8.5f, HOME_GREEN, 52, 112, false, true); textHome(c, "00:02:17", 8.5f, HOME_MUTED, 337, 112, true, false); String m = mission.isEmpty() ? "Create a research report on renewable energy trends" : mission; drawWrappedText(c, m, 13.0f, HOME_TEXT, 24, 151, 340, 20, true); drawCore(c, 195, 252, 52); drawChecklist(c, 24, 354); roundGlowHome(c, 24, 540, 366, 594, Color.rgb(247, 225, 181), Color.rgb(248, 228, 190), 26, Color.TRANSPARENT); textHome(c, paused ? "Resume Mission" : "Pause Mission", 11.5f, Color.rgb(34, 32, 28), 195, 574, true, true); roundGlowHome(c, 24, 610, 366, 658, HOME_PANEL_2, Color.rgb(64, 63, 55), 22, Color.TRANSPARENT); textHome(c, "Open run details", 11.5f, HOME_TEXT, 195, 640, true, true); }
        private void drawDetails(Canvas c) { top(c, "Mission Run", true); roundGlowHome(c, 18, 88, 372, 210, HOME_PANEL, Color.rgb(64, 63, 55), 20, Color.TRANSPARENT); textHome(c, "Mission Timeline", 12, HOME_TEXT, 32, 122, false, true); drawChecklist(c, 30, 154); }
        private void drawArtifacts(Canvas c) { top(c, "Artifacts", true); chip(c, 18, 86, 66, "All", true); chip(c, 92, 86, 78, "Docs", false); chip(c, 178, 86, 88, "Images", false); chip(c, 274, 86, 82, "Code", false); artifact(c, 18, 138, Color.rgb(239, 93, 79), "Research_Report.pdf", "2.4 MB · Today 09:43"); artifact(c, 18, 202, Color.rgb(80, 205, 157), "Market_Analysis.xlsx", "1.1 MB · Today 09:40"); artifact(c, 18, 266, Color.rgb(88, 155, 223), "Summary.md", "12 KB · Today 09:38"); }
        private void drawTools(Canvas c) { top(c, "Tools", true); tool(c, 18, 94, "Web Search", "Research & gather", HOME_GOLD()); tool(c, 202, 94, "Code Runner", "Execute code", HOME_CYAN); tool(c, 18, 202, "File Manager", "Organize files", Color.rgb(248, 228, 190)); tool(c, 202, 202, "Browser", "Automate web", HOME_CYAN); }
        private void drawProfile(Canvas c) { top(c, "Profile", true); roundGlowHome(c, 18, 88, 372, 214, HOME_PANEL, Color.rgb(64, 63, 55), 20, Color.TRANSPARENT); circle(c, 58, 128, 29, Color.rgb(177, 157, 116)); textHome(c, "V", 23, Color.rgb(31, 28, 22), 58, 136, true, true); textHome(c, "VEYTRIX USER", 8.5f, Color.rgb(221, 194, 143), 104, 118, false, false); textHome(c, "Local workspace", 15, HOME_TEXT, 104, 145, false, true); }
        private void drawSettings(Canvas c) { top(c, "Settings", true); setting(c, 18, 98, "Dark Mode", "Always on", true); setting(c, 18, 156, "Voice Input", "Hands-free missions", true); setting(c, 18, 214, "Notifications", "Mission events and results", true); }
        private void drawVoice(Canvas c) { top(c, "Voice Input", true); textHome(c, "SPEAK YOUR MISSION CLEARLY", 8.5f, Color.rgb(221, 194, 143), 195, 106, true, true); drawCore(c, 195, 250, 54); textHome(c, "Listening...", 21, HOME_TEXT, 195, 414, true, true); textHome(c, "Voice input is not connected in this build.", 9.5f, HOME_MUTED, 195, 439, true, false); }
        private void drawCompleted(Canvas c) { top(c, "Mission Completed", true); drawCheckCore(c, 195, 220); textHome(c, "Mission Completed", 21, HOME_TEXT, 195, 370, true, true); textHome(c, "Your result has been generated", 10, HOME_MUTED, 195, 400, true, false); textHome(c, "and verified successfully.", 10, HOME_MUTED, 195, 418, true, false); }

        private void top(Canvas c, String title, boolean back) { p.setShader(null); p.setColor(Color.rgb(6, 11, 18)); c.drawRect(0, 0, getWidth(), HY(67), p); if (back) textHome(c, "‹", 22, HOME_TEXT, 22, 42, true, true); else { roundGlowHome(c, 17, 17, 61, 60, Color.rgb(9, 20, 36), Color.rgb(72, 121, 196), 14, Color.TRANSPARENT); textHome(c, "☰", 21, HOME_TEXT, 39, 44, true, false); } textHome(c, "VEYTRIX", 15, Color.rgb(248, 228, 190), back ? 84 : 95, 29, true, true); textHome(c, "AUTOPILOT", 7.5f, Color.rgb(104, 106, 99), back ? 84 : 95, 44, true, false); if (back) textHome(c, title, 15, HOME_TEXT, 195, 59, true, true); else { roundGlowHome(c, 327, 18, 371, 51, Color.rgb(23, 25, 23), Color.rgb(221, 194, 143), 16, Color.TRANSPARENT); textHome(c, "Pro", 9.5f, Color.rgb(248, 228, 190), 349, 39, true, true); } }

        private void drawDrawer(Canvas c) { p.setColor(Color.argb(150, 0, 0, 0)); c.drawRect(0, 0, getWidth(), getHeight(), p); roundGlowHome(c, 0, 0, 322, 844, Color.rgb(8, 15, 24), Color.rgb(55, 74, 103), 0, Color.TRANSPARENT); textHome(c, "VEYTRIX", 18, HOME_TEXT, 30, 44, false, true); textHome(c, "AUTOPILOT", 7.5f, HOME_MUTED, 31, 59, false, false); roundGlowHome(c, 20, 82, 302, 140, HOME_PANEL_2, Color.rgb(72, 121, 196), 18, Color.TRANSPARENT); circle(c, 54, 110, 20, Color.rgb(116, 96, 255)); textHome(c, "V", 15, Color.WHITE, 54, 116, true, true); textHome(c, "Local workspace", 11.5f, HOME_TEXT, 83, 106, false, true); textHome(c, "PRO PLAN", 7.5f, Color.rgb(221, 194, 143), 83, 123, false, true); drawerRow(c, 168, "Home", HOME); drawerRow(c, 220, "Mission History", RUNS); drawerRow(c, 272, "Artifacts", ARTIFACTS); drawerRow(c, 324, "Tools", TOOLS); drawerRow(c, 376, "Settings", SETTINGS); drawerRow(c, 428, "Voice Input", VOICE); drawerRow(c, 480, "Verification", COMPLETED); }
        private void drawerRow(Canvas c, float y, String label, int target) { boolean active = page == target; if (active) roundGlowHome(c, 16, y - 23, 304, y + 20, Color.rgb(22, 38, 58), Color.TRANSPARENT, 13, Color.TRANSPARENT); textHome(c, label, 11.5f, active ? HOME_TEXT : HOME_MUTED, 34, y + 2, false, active); drawChevron(c, 286, y - 1); }

        private void drawCore(Canvas c, float cx, float cy, float r) { float rr = r * (1f + (float) Math.sin(pulse) * .015f); p.setShader(new RadialGradient(HX(cx), HY(cy), HX(rr * 1.65f), new int[]{Color.argb(90, 228, 194, 128), Color.argb(20, 228, 194, 128), Color.TRANSPARENT}, null, Shader.TileMode.CLAMP)); c.drawCircle(HX(cx), HY(cy), HX(rr * 1.65f), p); p.setShader(null); stroke.setColor(Color.argb(170, 215, 184, 117)); stroke.setStrokeWidth(HX(1)); RectF ring = new RectF(HX(cx - rr * 1.7f), HY(cy - rr * .72f), HX(cx + rr * 1.7f), HY(cy + rr * .72f)); c.save(); c.rotate(-15, HX(cx), HY(cy)); c.drawOval(ring, stroke); c.restore(); RectF ring2 = new RectF(HX(cx - rr * 1.25f), HY(cy - rr * 1.55f), HX(cx + rr * 1.25f), HY(cy + rr * 1.55f)); stroke.setColor(Color.argb(90, 215, 184, 117)); c.save(); c.rotate(19, HX(cx), HY(cy)); c.drawOval(ring2, stroke); c.restore(); p.setShader(new RadialGradient(HX(cx - rr * .3f), HY(cy - rr * .38f), HX(rr * 1.15f), new int[]{Color.rgb(255, 248, 225), Color.rgb(224, 198, 143), Color.rgb(116, 88, 43), Color.rgb(24, 21, 17)}, new float[]{0f, .18f, .52f, 1f}, Shader.TileMode.CLAMP)); c.drawCircle(HX(cx), HY(cy), HX(rr), p); p.setShader(null); p.setColor(Color.argb(190, 255, 248, 224)); c.drawCircle(HX(cx - rr * .28f), HY(cy - rr * .33f), HX(rr * .16f), p); textHome(c, "V", Math.max(16, rr * .43f), Color.rgb(250, 238, 211), cx, cy + rr * .2f, true, true); }
        private void drawChecklist(Canvas c, float x, float y) { String[] rows = {"Understanding objective", "Planning approach", "Preparing environment", "Executing tasks", "Verifying results"}; for (int i = 0; i < rows.length; i++) { float yy = y + i * 28; int dot = i < 3 ? Color.rgb(248, 228, 190) : (i == 3 ? HOME_CYAN : Color.rgb(104, 106, 99)); circle(c, x + 6, yy, 4, dot); textHome(c, rows[i], 9.8f, i < 3 ? HOME_MUTED : Color.rgb(104, 106, 99), x + 20, yy + 4, false, false); if (i < 3) textHome(c, "✓", 10, HOME_GREEN, 335, yy + 4, true, true); } }
        private void chip(Canvas c, float x, float y, float w, String label, boolean active) { roundGlowHome(c, x, y, x + w, y + 32, active ? Color.rgb(247, 225, 181) : HOME_PANEL_2, active ? Color.rgb(248, 228, 190) : Color.rgb(64, 63, 55), 16, Color.TRANSPARENT); textHome(c, label, 9, active ? Color.rgb(31, 29, 23) : HOME_MUTED, x + w / 2, y + 21, true, true); }
        private void artifact(Canvas c, float x, float y, int color, String name, String meta) { roundGlowHome(c, x, y, 372, y + 52, HOME_PANEL, Color.rgb(37, 42, 40), 14, Color.TRANSPARENT); roundGlowHome(c, x + 10, y + 10, x + 42, y + 42, color, color, 9, Color.TRANSPARENT); textHome(c, "▤", 15, Color.WHITE, x + 26, y + 32, true, false); textHome(c, name, 10.5f, HOME_TEXT, x + 54, y + 22, false, true); textHome(c, meta, 8, HOME_MUTED, x + 54, y + 38, false, false); }
        private void tool(Canvas c, float x, float y, String title, String sub, int accent) { roundGlowHome(c, x, y, x + 170, y + 96, HOME_PANEL, Color.rgb(41, 44, 40), 18, Color.TRANSPARENT); roundGlowHome(c, x + 10, y + 10, x + 47, y + 47, Color.rgb(24, 31, 41), accent, 10, Color.TRANSPARENT); textHome(c, title, 11, HOME_TEXT, x + 12, y + 67, false, true); textHome(c, sub, 8, HOME_MUTED, x + 12, y + 82, false, false); }
        private void setting(Canvas c, float x, float y, String title, String sub, boolean on) { roundGlowHome(c, x, y, 372, y + 46, HOME_PANEL, Color.rgb(39, 43, 41), 14, Color.TRANSPARENT); textHome(c, title, 10.5f, HOME_TEXT, x + 18, y + 20, false, true); textHome(c, sub, 8, HOME_MUTED, x + 18, y + 35, false, false); roundGlowHome(c, 322, y + 9, 358, y + 37, on ? Color.rgb(33, 31, 26) : HOME_PANEL_2, on ? Color.rgb(221, 194, 143) : Color.rgb(64, 63, 55), 12, Color.TRANSPARENT); textHome(c, on ? "ON" : "OFF", 7.4f, on ? Color.rgb(221, 194, 143) : HOME_MUTED, 340, y + 27, true, true); }
        private void drawFeatureIcon(Canvas c, float cx, float cy, int kind) { int accent; if (kind == 0) accent = Color.rgb(194, 165, 255); else if (kind == 1) accent = Color.rgb(225, 231, 255); else if (kind == 2) accent = Color.WHITE; else accent = Color.rgb(196, 218, 255); stroke.setColor(accent); stroke.setStrokeWidth(HX(1.9f)); stroke.setStyle(Paint.Style.STROKE); if (kind == 0) { c.drawCircle(HX(cx), HY(cy), HX(7), stroke); c.drawLine(HX(cx), HY(cy - 10), HX(cx), HY(cy + 10), stroke); c.drawLine(HX(cx - 10), HY(cy), HX(cx + 10), HY(cy), stroke); } else if (kind == 1) { c.drawCircle(HX(cx - 5), HY(cy - 3), HX(4), stroke); c.drawCircle(HX(cx + 5), HY(cy - 3), HX(4), stroke); c.drawArc(new RectF(HX(cx - 11), HY(cy + 1), HX(cx + 1), HY(cy + 10)), 180, 180, false, stroke); c.drawArc(new RectF(HX(cx - 1), HY(cy + 1), HX(cx + 11), HY(cy + 10)), 180, 180, false, stroke); } else if (kind == 2) { Path q = new Path(); q.moveTo(HX(cx - 10), HY(cy)); q.lineTo(HX(cx - 4), HY(cy + 6)); q.lineTo(HX(cx + 9), HY(cy - 9)); c.drawPath(q, stroke); c.drawRect(HX(cx - 11), HY(cy - 11), HX(cx + 11), HY(cy + 11), stroke); } else { Path q = new Path(); q.moveTo(HX(cx - 10), HY(cy + 6)); q.lineTo(HX(cx), HY(cy - 10)); q.lineTo(HX(cx + 10), HY(cy + 6)); q.lineTo(HX(cx), HY(cy + 1)); q.close(); c.drawPath(q, stroke); c.drawCircle(HX(cx), HY(cy - 11), HX(2), stroke); } }
        private void drawMetricIcon(Canvas c, float cx, float cy, int kind) { stroke.setColor(kind == 0 ? Color.rgb(189, 164, 255) : kind == 1 ? Color.rgb(170, 132, 255) : kind == 2 ? Color.rgb(167, 147, 255) : Color.rgb(165, 181, 255)); stroke.setStrokeWidth(HX(2)); stroke.setStyle(Paint.Style.STROKE); if (kind == 0) { Path q = new Path(); q.moveTo(HX(cx - 5), HY(cy - 11)); q.lineTo(HX(cx + 2), HY(cy - 2)); q.lineTo(HX(cx - 2), HY(cy - 2)); q.lineTo(HX(cx + 5), HY(cy + 9)); q.lineTo(HX(cx - 3), HY(cy)); q.lineTo(HX(cx + 1), HY(cy)); q.close(); c.drawPath(q, stroke); } else if (kind == 1) { c.drawRect(HX(cx - 9), HY(cy - 6), HX(cx - 2), HY(cy + 9), stroke); c.drawRoundRect(new RectF(HX(cx - 1), HY(cy - 10), HX(cx + 7), HY(cy + 9)), HX(2), HX(2), stroke); c.drawRoundRect(new RectF(HX(cx + 8), HY(cy - 8), HX(cx + 16), HY(cy + 9)), HX(2), HX(2), stroke); } else if (kind == 2) { c.drawRect(HX(cx - 9), HY(cy - 4), HX(cx - 1), HY(cy + 10), stroke); c.drawRect(HX(cx + 2), HY(cy - 10), HX(cx + 10), HY(cy + 10), stroke); c.drawRect(HX(cx + 13), HY(cy - 1), HX(cx + 21), HY(cy + 10), stroke); } else { c.drawCircle(HX(cx), HY(cy), HX(9), stroke); c.drawLine(HX(cx), HY(cy), HX(cx), HY(cy - 5), stroke); c.drawLine(HX(cx), HY(cy), HX(cx + 5), HY(cy + 3), stroke); } }
        private void drawRecentHeaderIcon(Canvas c, float cx, float cy) { fill(p, Color.rgb(179, 139, 255)); c.drawCircle(HX(cx), HY(cy), HX(8), p); stroke.setColor(Color.rgb(236, 231, 255)); stroke.setStrokeWidth(HX(1.3f)); c.drawLine(HX(cx), HY(cy - 4), HX(cx), HY(cy + 4), stroke); c.drawLine(HX(cx - 3), HY(cy), HX(cx + 3), HY(cy), stroke); }
        private void drawCodeIcon(Canvas c, float cx, float cy) { stroke.setColor(Color.WHITE); stroke.setStrokeWidth(HX(1.6f)); c.drawLine(HX(cx - 7), HY(cy), HX(cx - 2), HY(cy - 4), stroke); c.drawLine(HX(cx - 7), HY(cy), HX(cx - 2), HY(cy + 4), stroke); c.drawLine(HX(cx + 7), HY(cy), HX(cx + 2), HY(cy - 4), stroke); c.drawLine(HX(cx + 7), HY(cy), HX(cx + 2), HY(cy + 4), stroke); c.drawLine(HX(cx - 2), HY(cy + 5), HX(cx + 2), HY(cy - 5), stroke); }
        private void drawDocIcon(Canvas c, float cx, float cy) { fill(p, Color.WHITE); c.drawRoundRect(new RectF(HX(cx - 6), HY(cy - 8), HX(cx + 6), HY(cy + 8)), HX(2), HX(2), p); }
        private void drawGearIcon(Canvas c, float cx, float cy) { stroke.setColor(Color.WHITE); stroke.setStrokeWidth(HX(1.5f)); c.drawCircle(HX(cx), HY(cy), HX(6), stroke); c.drawCircle(HX(cx), HY(cy), HX(2), stroke); for (int i = 0; i < 8; i++) { double a = i * Math.PI / 4; float x1 = (float) Math.cos(a) * 8, y1 = (float) Math.sin(a) * 8; float x2 = (float) Math.cos(a) * 10, y2 = (float) Math.sin(a) * 10; c.drawLine(HX(cx + x1), HY(cy + y1), HX(cx + x2), HY(cy + y2), stroke); } }

        private void drawBrandMark(Canvas c, float cx, float cy) {
            Path v = new Path();
            v.moveTo(HX(cx - 13), HY(cy - 14));
            v.lineTo(HX(cx - 4), HY(cy + 3));
            v.lineTo(HX(cx), HY(cy + 16));
            v.lineTo(HX(cx + 13), HY(cy - 14));
            v.lineTo(HX(cx + 5), HY(cy - 14));
            v.lineTo(HX(cx), HY(cy + 1));
            v.lineTo(HX(cx - 5), HY(cy - 14));
            v.close();
            p.setShader(new LinearGradient(HX(cx - 13), HY(cy - 14), HX(cx + 13), HY(cy + 16), new int[]{HOME_CYAN, Color.rgb(116, 96, 255), Color.rgb(192, 67, 255)}, null, Shader.TileMode.CLAMP));
            c.drawPath(v, p);
            p.setShader(null);
        }

        private void drawBell(Canvas c, float cx, float cy) { stroke.setColor(Color.WHITE); stroke.setStyle(Paint.Style.STROKE); stroke.setStrokeCap(Paint.Cap.ROUND); stroke.setStrokeWidth(HX(1.7f)); Path q = new Path(); q.moveTo(HX(cx - 6), HY(cy + 2)); q.quadTo(HX(cx - 5), HY(cy - 7), HX(cx), HY(cy - 7)); q.quadTo(HX(cx + 5), HY(cy - 7), HX(cx + 6), HY(cy + 2)); q.lineTo(HX(cx + 7), HY(cy + 5)); q.lineTo(HX(cx - 7), HY(cy + 5)); q.close(); c.drawPath(q, stroke); c.drawLine(HX(cx - 3), HY(cy + 8), HX(cx + 3), HY(cy + 8), stroke); }
        private void drawSparkle(Canvas c, float cx, float cy) { Path q = new Path(); q.moveTo(HX(cx), HY(cy - 9)); q.lineTo(HX(cx + 4), HY(cy - 3)); q.lineTo(HX(cx + 10), HY(cy)); q.lineTo(HX(cx + 4), HY(cy + 4)); q.lineTo(HX(cx), HY(cy + 11)); q.lineTo(HX(cx - 4), HY(cy + 4)); q.lineTo(HX(cx - 10), HY(cy)); q.lineTo(HX(cx - 4), HY(cy - 3)); q.close(); p.setShader(new LinearGradient(HX(cx - 8), HY(cy - 8), HX(cx + 8), HY(cy + 8), new int[]{Color.rgb(197, 171, 255), HOME_BLUE}, null, Shader.TileMode.CLAMP)); c.drawPath(q, p); p.setShader(null); }
        private void drawContextIcon(Canvas c, float cx, float cy) { stroke.setColor(Color.WHITE); stroke.setStrokeWidth(HX(1.3f)); Path a = new Path(); a.moveTo(HX(cx - 9), HY(cy - 4)); a.lineTo(HX(cx), HY(cy - 10)); a.lineTo(HX(cx + 9), HY(cy - 4)); a.lineTo(HX(cx), HY(cy + 2)); a.close(); c.drawPath(a, stroke); Path b = new Path(); b.moveTo(HX(cx - 9), HY(cy + 2)); b.lineTo(HX(cx), HY(cy + 8)); b.lineTo(HX(cx + 9), HY(cy + 2)); b.close(); c.drawPath(b, stroke); }
        private void drawMicIcon(Canvas c, float cx, float cy) { stroke.setColor(HOME_BLUE); stroke.setStrokeWidth(HX(1.7f)); stroke.setStyle(Paint.Style.STROKE); c.drawRoundRect(new RectF(HX(cx - 4), HY(cy - 9), HX(cx + 4), HY(cy + 3)), HX(4), HX(4), stroke); c.drawArc(new RectF(HX(cx - 8), HY(cy - 3), HX(cx + 8), HY(cy + 8)), 0, 180, false, stroke); c.drawLine(HX(cx), HY(cy + 8), HX(cx), HY(cy + 11), stroke); c.drawLine(HX(cx - 4), HY(cy + 11), HX(cx + 4), HY(cy + 11), stroke); }
        private void drawAttachIcon(Canvas c, float cx, float cy) { stroke.setColor(Color.WHITE); stroke.setStrokeWidth(HX(1.7f)); stroke.setStyle(Paint.Style.STROKE); Path q = new Path(); q.moveTo(HX(cx + 6), HY(cy - 7)); q.cubicTo(HX(cx + 11), HY(cy - 2), HX(cx + 7), HY(cy + 4), HX(cx + 3), HY(cy + 8)); q.cubicTo(HX(cx - 3), HY(cy + 13), HX(cx - 10), HY(cy + 7), HX(cx - 6), HY(cy + 2)); q.lineTo(HX(cx + 4), HY(cy - 8)); c.drawPath(q, stroke); }
        private void drawPlayIcon(Canvas c, float cx, float cy) { fill(p, Color.WHITE); Path q = new Path(); q.moveTo(HX(cx - 5), HY(cy - 8)); q.lineTo(HX(cx + 8), HY(cy)); q.lineTo(HX(cx - 5), HY(cy + 8)); q.close(); c.drawPath(q, p); }
        private void drawCheckCore(Canvas c, float cx, float cy) { p.setShader(new RadialGradient(HX(cx), HY(cy), HX(85), new int[]{Color.argb(70, 228, 194, 128), Color.TRANSPARENT}, null, Shader.TileMode.CLAMP)); c.drawCircle(HX(cx), HY(cy), HX(85), p); p.setShader(null); stroke.setColor(Color.rgb(248, 228, 190)); stroke.setStrokeWidth(HX(3)); c.drawCircle(HX(cx), HY(cy), HX(52), stroke); Path q = new Path(); q.moveTo(HX(cx - 20), HY(cy + 3)); q.lineTo(HX(cx - 5), HY(cy + 18)); q.lineTo(HX(cx + 28), HY(cy - 20)); c.drawPath(q, stroke); }
        private void drawChevron(Canvas c, float cx, float cy) { stroke.setColor(HOME_MUTED); stroke.setStrokeWidth(HX(1.3f)); Path q = new Path(); q.moveTo(HX(cx - 3), HY(cy - 4)); q.lineTo(HX(cx + 1), HY(cy)); q.lineTo(HX(cx - 3), HY(cy + 4)); c.drawPath(q, stroke); }
        private void roundGlowHome(Canvas c, float l, float t, float r, float b, int fillColor, int border, float radius, int glow) { p.setShader(null); p.setStyle(Paint.Style.FILL); p.setColor(fillColor); if (glow != Color.TRANSPARENT) p.setShadowLayer(HX(8), 0, 0, glow); RectF rr = new RectF(HX(l), HY(t), HX(r), HY(b)); c.drawRoundRect(rr, HX(radius), HX(radius), p); p.clearShadowLayer(); if (border != Color.TRANSPARENT) { p.setStyle(Paint.Style.STROKE); p.setStrokeWidth(Math.max(1f, HX(.8f))); p.setColor(border); c.drawRoundRect(rr, HX(radius), HX(radius), p); p.setStyle(Paint.Style.FILL); } }
        private void glowCircle(Canvas c, float x, float y, float r, int color) { p.setShader(new RadialGradient(HX(x), HY(y), HX(r), new int[]{color, Color.TRANSPARENT}, null, Shader.TileMode.CLAMP)); c.drawCircle(HX(x), HY(y), HX(r), p); p.setShader(null); }
        private void circle(Canvas c, float x, float y, float r, int color) { fill(p, color); c.drawCircle(HX(x), HY(y), HX(r), p); }
        private void fill(Paint paint, int color) { paint.setShader(null); paint.setStyle(Paint.Style.FILL); paint.setColor(color); }
        private void textHome(Canvas c, String str, float size, int color, float x, float y, boolean center, boolean bold) { p.setShader(null); p.setStyle(Paint.Style.FILL); p.setColor(color); p.setTextSize(HX(size)); p.setTypeface(Typeface.create("sans-serif", bold ? Typeface.BOLD : Typeface.NORMAL)); p.setTextAlign(center ? Paint.Align.CENTER : Paint.Align.LEFT); c.drawText(str, HX(x), HY(y), p); }
        private void drawWrappedText(Canvas c, String value, float size, int color, float x, float y, float maxWidth, float lineStep, boolean bold) { p.setTextSize(HX(size)); p.setTypeface(Typeface.create("sans-serif", bold ? Typeface.BOLD : Typeface.NORMAL)); p.setTextAlign(Paint.Align.LEFT); String[] words = value.split(" "); StringBuilder line = new StringBuilder(); float yy = y; for (String word : words) { String trial = line.length() == 0 ? word : line + " " + word; if (p.measureText(trial) > HX(maxWidth) && line.length() > 0) { textHome(c, line.toString(), size, color, x, yy, false, bold); line = new StringBuilder(word); yy += lineStep; } else line = new StringBuilder(trial); } if (line.length() > 0) textHome(c, line.toString(), size, color, x, yy, false, bold); }

        @Override public boolean onTouchEvent(MotionEvent e) {
            if (e.getAction() != MotionEvent.ACTION_UP) return true;
            float scale = hs(); if (scale <= 0f) return true; float x = e.getX() / scale; float y = e.getY() / scale;
            if (drawer) { if (x > 322) { drawer = false; if (page == HOME) showComposer(); invalidate(); return true; } if (y >= 145 && y < 196) select(HOME); else if (y >= 196 && y < 248) select(RUNS); else if (y >= 248 && y < 300) select(ARTIFACTS); else if (y >= 300 && y < 352) select(TOOLS); else if (y >= 352 && y < 404) select(SETTINGS); else if (y >= 404 && y < 456) select(VOICE); else if (y >= 456 && y < 508) select(COMPLETED); return true; }
            if (page == HOME) { if (y < 70 && x < 82) { drawer = true; hideComposer(); invalidate(); return true; } if (y < 70 && x >= 340) { select(PROFILE); return true; } if (y >= 438 && y < 515) { Toast.makeText(MainActivity.this, "Feature shortcut not connected in this build", Toast.LENGTH_SHORT).show(); return true; } if (y >= 600 && y < 784) { select(RUNS); return true; } if (y >= 784) { if (x < 82) select(HOME); else if (x < 156) select(RUNS); else if (x < 230) select(ARTIFACTS); else if (x < 304) select(TOOLS); else select(PROFILE); return true; } return true; }
            if (y >= 784) { if (x < 82) select(HOME); else if (x < 156) select(RUNS); else if (x < 230) select(ARTIFACTS); else if (x < 304) select(TOOLS); else select(PROFILE); return true; }
            if (page == RUNS && y >= 530 && y < 600) { paused = !paused; invalidate(); return true; }
            if (page == RUNS && y >= 600 && y < 680) { select(DETAILS); return true; }
            if (page == VOICE && y >= 430) { select(HOME); return true; }
            return true;
        }

        private void select(int target) { page = target; drawer = false; if (target == HOME) showComposer(); else hideComposer(); invalidate(); }
    }

    private int HOME_GOLD() { return Color.rgb(221, 194, 143); }
}
