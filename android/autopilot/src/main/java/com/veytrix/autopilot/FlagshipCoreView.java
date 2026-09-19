package com.veytrix.autopilot;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Shader;
import android.view.View;

/**
 * Native animated rendering of the VEYTRIX autonomous core.
 *
 * The view is intentionally self-contained: no bitmaps, HTML, CSS or runtime
 * allocations are required for the animation loop.
 */
public final class FlagshipCoreView extends View {
    private final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint stroke = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint glow = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF ring = new RectF();
    private final RectF ringTall = new RectF();
    private final RectF ringTilt = new RectF();
    private final RectF ringInner = new RectF();
    private final RectF baseOuter = new RectF();
    private final RectF baseMid = new RectF();
    private final RectF baseInner = new RectF();
    private final RectF sphereHighlight = new RectF();
    private final Path mark = new Path();
    private final Path markGlow = new Path();

    private RadialGradient haloShader;
    private RadialGradient sphereShader;
    private RadialGradient sphereCoreShader;
    private LinearGradient markShader;
    private DashPathEffect orbitDash;
    private float density;
    private float coreRadius;
    private long animationStartNanos;
    private boolean running;

    public FlagshipCoreView(Context context) {
        super(context);
        density = getResources().getDisplayMetrics().density;

        stroke.setStyle(Paint.Style.STROKE);
        stroke.setStrokeCap(Paint.Cap.ROUND);
        stroke.setStrokeJoin(Paint.Join.ROUND);
        glow.setStyle(Paint.Style.FILL);
        orbitDash = new DashPathEffect(new float[]{dp(6), dp(11)}, 0f);

        setLayerType(View.LAYER_TYPE_SOFTWARE, null);
        setClickable(false);
        setFocusable(false);
        setWillNotDraw(false);
    }

    private static int tint(int alpha, int color) {
        int a = Math.max(0, Math.min(255, alpha));
        return Color.argb(a, Color.red(color), Color.green(color), Color.blue(color));
    }

    private float dp(float value) {
        return value * density;
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        running = true;
        animationStartNanos = System.nanoTime();
        postInvalidateOnAnimation();
    }

    @Override
    protected void onDetachedFromWindow() {
        running = false;
        super.onDetachedFromWindow();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        if (w <= 0 || h <= 0) return;

        float size = Math.min(w, h);
        coreRadius = size * 0.205f;
        float cx = w * 0.5f;
        float cy = h * 0.385f;

        haloShader = new RadialGradient(
                cx, cy, coreRadius * 3.45f,
                new int[]{
                        Color.argb(128, 87, 128, 255),
                        Color.argb(70, 127, 75, 255),
                        Color.argb(28, 70, 219, 255),
                        Color.TRANSPARENT
                },
                new float[]{0f, .26f, .61f, 1f},
                Shader.TileMode.CLAMP
        );

        sphereShader = new RadialGradient(
                w * .405f, h * .245f, coreRadius * 1.58f,
                new int[]{
                        Color.WHITE,
                        VeytrixDesignTokens.SILVER,
                        VeytrixDesignTokens.PINK,
                        VeytrixDesignTokens.VIOLET,
                        VeytrixDesignTokens.TEXT_PRIMARY
                },
                new float[]{0f, .09f, .30f, .62f, 1f},
                Shader.TileMode.CLAMP
        );

        sphereCoreShader = new RadialGradient(
                w * .44f, h * .29f, coreRadius * .78f,
                new int[]{
                        Color.argb(255, 255, 255, 255),
                        Color.argb(235, 193, 215, 255),
                        Color.argb(90, 111, 140, 255),
                        Color.TRANSPARENT
                },
                new float[]{0f, .18f, .56f, 1f},
                Shader.TileMode.CLAMP
        );

        markShader = new LinearGradient(
                cx - coreRadius * .78f, cy - coreRadius * .85f,
                cx + coreRadius * .78f, cy + coreRadius * .70f,
                new int[]{
                        VeytrixDesignTokens.PEARL,
                        VeytrixDesignTokens.SILVER_STRONG,
                        VeytrixDesignTokens.PURPLE,
                        VeytrixDesignTokens.MAGENTA
                },
                null,
                Shader.TileMode.CLAMP
        );

        buildMark(cx, cy);
    }

    private void buildMark(float cx, float cy) {
        mark.reset();
        markGlow.reset();

        float r = coreRadius * .82f;
        float top = cy - r * .86f;
        float mid = cy - r * .02f;
        float bottom = cy + r * .72f;
        float left = cx - r * .92f;
        float right = cx + r * .92f;
        float inner = r * .27f;

        mark.moveTo(left, top);
        mark.lineTo(cx - inner, mid);
        mark.lineTo(cx, bottom);
        mark.lineTo(cx + inner, mid);
        mark.lineTo(right, top);
        mark.lineTo(cx + r * .47f, top);
        mark.lineTo(cx, cy + r * .24f);
        mark.lineTo(cx - r * .47f, top);
        mark.close();

        markGlow.addPath(mark);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        final float w = getWidth();
        final float h = getHeight();
        if (w <= 0 || h <= 0 || haloShader == null || sphereShader == null) return;

        final float t = (System.nanoTime() - animationStartNanos) / 1_000_000_000f;
        final float breathing = 1f + (float) Math.sin(t * 2.05f) * .021f;
        final float drift = (float) Math.sin(t * 1.20f) * dp(3.2f);
        final float cx = w * .5f;
        final float baseCy = h * .385f;
        final float cy = baseCy + drift;
        final float orbital = Math.min(w, h) * .405f;
        final float radius = coreRadius * breathing;

        fill.setShader(haloShader);
        canvas.drawCircle(cx, baseCy, coreRadius * 3.18f, fill);
        fill.setShader(null);

        drawOrbitSystem(canvas, cx, cy, orbital, t);
        drawParticles(canvas, cx, cy, orbital, t);
        drawPedestal(canvas, cx, h * .745f, orbital * .78f, t);

        canvas.save();
        canvas.translate(0f, drift);
        drawSphere(canvas, cx, baseCy, radius, t);
        canvas.restore();

        if (running) {
            postInvalidateOnAnimation();
        }
    }

    private void drawOrbitSystem(Canvas c, float cx, float cy, float outer, float t) {
        ring.set(cx - outer, cy - outer * .43f, cx + outer, cy + outer * .43f);
        ringTall.set(cx - outer * .56f, cy - outer * .88f, cx + outer * .56f, cy + outer * .88f);
        ringTilt.set(cx - outer * .78f, cy - outer * .28f, cx + outer * .78f, cy + outer * .28f);
        ringInner.set(cx - outer * .62f, cy - outer * .34f, cx + outer * .62f, cy + outer * .34f);

        stroke.setStyle(Paint.Style.STROKE);
        stroke.setPathEffect(null);
        stroke.setStrokeCap(Paint.Cap.ROUND);

        stroke.setStrokeWidth(dp(1.0f));
        stroke.setColor(Color.argb(52, 146, 177, 255));
        c.save();
        c.rotate(-15f + t * 6f, cx, cy);
        c.drawOval(ringTall, stroke);
        c.restore();

        stroke.setStrokeWidth(dp(.9f));
        stroke.setColor(Color.argb(58, 202, 123, 255));
        c.save();
        c.rotate(31f - t * 5f, cx, cy);
        c.drawOval(ringTilt, stroke);
        c.restore();

        stroke.setStrokeWidth(dp(1.15f));
        stroke.setColor(Color.argb(205, 105, 154, 255));
        c.save();
        c.rotate(-17f + t * 7.5f, cx, cy);
        c.drawOval(ring, stroke);
        c.restore();

        stroke.setStrokeWidth(dp(.95f));
        stroke.setColor(Color.argb(148, 187, 104, 255));
        c.save();
        c.rotate(28f - t * 6.0f, cx, cy);
        c.drawOval(ringInner, stroke);
        c.restore();

        stroke.setStrokeWidth(dp(.85f));
        stroke.setColor(Color.argb(135, 79, 219, 255));
        c.save();
        c.rotate(-34f - t * 4.4f, cx, cy);
        c.drawArc(ringTilt, 18f, 112f, false, stroke);
        c.drawArc(ringTilt, 202f, 80f, false, stroke);
        c.restore();

        stroke.setPathEffect(orbitDash);
        stroke.setStrokeWidth(dp(.72f));
        stroke.setColor(Color.argb(112, 126, 101, 255));
        c.save();
        c.rotate(53f + t * 10f, cx, cy);
        c.drawOval(ring, stroke);
        c.restore();
        stroke.setPathEffect(null);

        stroke.setStrokeWidth(dp(1.35f));
        stroke.setColor(Color.argb(220, 144, 184, 255));
        c.save();
        c.rotate(-17f + t * 7.5f, cx, cy);
        c.drawArc(ring, 194f, 108f, false, stroke);
        c.restore();

        stroke.setStrokeWidth(dp(1.0f));
        stroke.setColor(Color.argb(180, 192, 126, 255));
        c.save();
        c.rotate(28f - t * 6.0f, cx, cy);
        c.drawArc(ringInner, 18f, 118f, false, stroke);
        c.restore();
    }

    private void drawParticles(Canvas c, float cx, float cy, float orbital, float t) {
        final int count = 9;
        for (int i = 0; i < count; i++) {
            double a = t * (.34 + i * .016) + i * (Math.PI * 2.0 / count);
            float x = cx + (float) Math.cos(a) * orbital * (i % 2 == 0 ? 1.00f : .79f);
            float y = cy + (float) Math.sin(a) * orbital * (i % 2 == 0 ? .44f : .68f);
            float r = dp(i % 4 == 0 ? 2.35f : 1.35f);
            int base = i % 3 == 0 ? 115 : 83;
            glow.setColor(tint(
                    base + (int) (28 * (0.5f + 0.5f * Math.sin(t * 1.7f + i))),
                    VeytrixDesignTokens.MAGENTA
            ));
            glow.setShadowLayer(r * 3.2f, 0, 0, tint(110, VeytrixDesignTokens.VIOLET));
            c.drawCircle(x, y, r, glow);
            glow.clearShadowLayer();

            if (i == 2 || i == 6) {
                glow.setColor(Color.argb(155, 224, 235, 255));
                c.drawCircle(x, y, r * .46f, glow);
            }
        }
    }

    private void drawSphere(Canvas c, float cx, float cy, float radius, float t) {
        fill.setShader(sphereShader);
        fill.setShadowLayer(radius * .82f, 0, radius * .18f, Color.argb(138, 87, 92, 255));
        c.drawCircle(cx, cy, radius, fill);
        fill.clearShadowLayer();
        fill.setShader(null);

        fill.setShader(sphereCoreShader);
        c.drawCircle(cx, cy, radius * .77f, fill);
        fill.setShader(null);

        fill.setColor(tint(62, VeytrixDesignTokens.PINK));
        c.drawOval(
                cx - radius * .76f,
                cy - radius * .55f,
                cx + radius * .68f,
                cy + radius * .50f,
                fill
        );

        fill.setColor(Color.WHITE);
        fill.setShadowLayer(radius * .22f, -radius * .18f, -radius * .18f, Color.argb(115, 104, 149, 255));
        c.drawCircle(cx - radius * .31f, cy - radius * .34f, radius * .105f, fill);
        fill.clearShadowLayer();

        float lx = cx + (float) Math.sin(t * .72f) * radius * .26f;
        float ly = cy - radius * .58f;
        fill.setColor(Color.argb(45, 255, 255, 255));
        c.drawCircle(lx, ly, radius * .055f, fill);

        stroke.setStyle(Paint.Style.STROKE);
        stroke.setStrokeWidth(dp(.72f));
        stroke.setColor(Color.argb(150, 152, 195, 255));
        c.drawCircle(cx, cy, radius * 1.035f, stroke);

        fill.setShader(markShader);
        fill.setShadowLayer(radius * .18f, 0, 0, Color.argb(155, 110, 104, 255));
        c.drawPath(markGlow, fill);
        fill.clearShadowLayer();
        fill.setShader(null);

        sphereHighlight.set(
                cx - radius * .61f,
                cy - radius * .60f,
                cx + radius * .61f,
                cy + radius * .60f
        );
        stroke.setStrokeWidth(dp(.65f));
        stroke.setColor(Color.argb(120, 232, 241, 255));
        c.drawArc(sphereHighlight, 208f, 98f, false, stroke);
    }

    private void drawPedestal(Canvas c, float cx, float cy, float width, float t) {
        float left = cx - width;
        float right = cx + width;
        float top = cy - dp(12);
        float bottom = cy + dp(30);

        baseOuter.set(left, top, right, bottom);
        baseMid.set(left + dp(7), top + dp(6), right - dp(7), bottom - dp(7));
        baseInner.set(left + dp(17), top + dp(11), right - dp(17), bottom - dp(12));

        fill.setColor(tint(70, VeytrixDesignTokens.VIOLET));
        fill.setShadowLayer(dp(22), 0, dp(5), Color.argb(115, 62, 124, 255));
        c.drawOval(baseOuter, fill);
        fill.clearShadowLayer();

        fill.setColor(VeytrixDesignTokens.PEARL);
        c.drawOval(baseOuter, fill);

        fill.setColor(VeytrixDesignTokens.SILVER);
        c.drawOval(baseMid, fill);

        fill.setColor(Color.argb(210, 7, 13, 25));
        c.drawOval(baseInner, fill);

        stroke.setStyle(Paint.Style.STROKE);
        stroke.setStrokeWidth(dp(1.25f));
        stroke.setColor(Color.argb(180, 99, 141, 231));
        c.drawOval(baseOuter, stroke);

        stroke.setStrokeWidth(dp(1.0f));
        stroke.setColor(Color.argb(125, 152, 104, 255));
        c.drawOval(baseMid, stroke);

        stroke.setStrokeWidth(dp(.75f));
        stroke.setColor(Color.argb(92, 79, 212, 255));
        c.drawOval(baseInner, stroke);

        float start = (t * 34f) % 360f;
        for (int i = 0; i < 12; i++) {
            float alpha = 74f + 70f * (float) Math.sin(t * 2.0f + i * .85f);
            stroke.setStrokeWidth(dp(i % 3 == 0 ? 1.15f : .65f));
            stroke.setColor(Color.argb((int) Math.max(28, alpha), 94, 145, 255));
            c.drawArc(baseMid, start + i * 30f, 9f, false, stroke);
        }

        for (int i = -3; i <= 3; i++) {
            float x = cx + i * dp(7.5f);
            float beamAlpha = 18f + 20f * (float) Math.sin(t * 1.9f + i);
            fill.setColor(Color.argb((int) Math.max(8, beamAlpha), 91, 150, 255));
            c.drawRoundRect(
                    x - dp(.75f),
                    top - dp(2),
                    x + dp(.75f),
                    top + dp(15),
                    dp(.75f),
                    dp(.75f),
                    fill
            );
        }

        glow.setColor(Color.argb(85, 104, 159, 255));
        glow.setShadowLayer(dp(10), 0, 0, Color.argb(100, 84, 127, 255));
        c.drawOval(
                cx - width * .30f,
                top + dp(1),
                cx + width * .30f,
                top + dp(8),
                glow
        );
        glow.clearShadowLayer();
    }
}
