package com.veytrix.autopilot;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Shader;
import android.view.View;

/** Native animated rendering of the VEYTRIX autonomous core. */
public final class FlagshipCoreView extends View {
    private final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint stroke = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint glow = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF ring = new RectF();
    private final RectF ringTall = new RectF();
    private final RectF ringTilt = new RectF();
    private final RectF baseOuter = new RectF();
    private final RectF baseInner = new RectF();
    private final Path mark = new Path();

    private RadialGradient haloShader;
    private RadialGradient sphereShader;
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
        setLayerType(View.LAYER_TYPE_SOFTWARE, null);
        setClickable(false);
    }

    private float dp(float value) { return value * density; }

    @Override protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        running = true;
        animationStartNanos = System.nanoTime();
        postInvalidateOnAnimation();
    }

    @Override protected void onDetachedFromWindow() {
        running = false;
        super.onDetachedFromWindow();
    }

    @Override protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        float size = Math.min(w, h);
        coreRadius = size * 0.19f;
        haloShader = new RadialGradient(w * .5f, h * .39f, coreRadius * 3.2f,
                new int[]{Color.argb(120, 92, 117, 255), Color.argb(58, 117, 72, 255), Color.argb(12, 73, 211, 255), Color.TRANSPARENT},
                new float[]{0f, .28f, .62f, 1f}, Shader.TileMode.CLAMP);
        sphereShader = new RadialGradient(w * .43f, h * .27f, coreRadius * 1.48f,
                new int[]{Color.WHITE, Color.rgb(221, 238, 255), Color.rgb(121, 148, 255), Color.rgb(76, 61, 196), Color.rgb(11, 18, 40)},
                new float[]{0f, .10f, .32f, .64f, 1f}, Shader.TileMode.CLAMP);
        mark.reset();
        float cx = w * .5f;
        float cy = h * .39f;
        float r = coreRadius * .74f;
        mark.moveTo(cx - r * .95f, cy - r * .95f);
        mark.lineTo(cx - r * .30f, cy + r * .08f);
        mark.lineTo(cx, cy + r * .62f);
        mark.lineTo(cx + r * .30f, cy + r * .08f);
        mark.lineTo(cx + r * .95f, cy - r * .95f);
        mark.lineTo(cx + r * .51f, cy - r * .95f);
        mark.lineTo(cx, cy - r * .06f);
        mark.lineTo(cx - r * .51f, cy - r * .95f);
        mark.close();
    }

    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        final float w = getWidth();
        final float h = getHeight();
        if (w <= 0 || h <= 0) return;

        final float t = (System.nanoTime() - animationStartNanos) / 1_000_000_000f;
        final float drift = (float) Math.sin(t * 1.25f) * dp(3.5f);
        final float pulse = 1f + (float) Math.sin(t * 2.1f) * .018f;
        final float cx = w * .5f;
        final float cy = h * .39f + drift;
        final float outer = Math.min(w, h) * .40f;

        fill.setShader(haloShader);
        canvas.drawCircle(cx, cy, coreRadius * 3.0f, fill);
        fill.setShader(null);

        drawAtmosphericRings(canvas, cx, cy, outer, t);
        drawParticles(canvas, cx, cy, outer, t);
        drawPedestal(canvas, cx, h * .74f, outer * .78f, t);
        drawSphere(canvas, cx, cy, coreRadius * pulse, t);

        if (running) postInvalidateOnAnimation();
    }

    private void drawAtmosphericRings(Canvas c, float cx, float cy, float outer, float t) {
        ring.set(cx - outer, cy - outer * .43f, cx + outer, cy + outer * .43f);
        ringTall.set(cx - outer * .58f, cy - outer * .86f, cx + outer * .58f, cy + outer * .86f);
        ringTilt.set(cx - outer * .78f, cy - outer * .30f, cx + outer * .78f, cy + outer * .30f);

        stroke.setPathEffect(null);
        stroke.setStrokeWidth(dp(1.1f));
        stroke.setColor(Color.argb(205, 104, 149, 255));
        c.save(); c.rotate(-16f + t * 7f, cx, cy); c.drawOval(ring, stroke); c.restore();

        stroke.setColor(Color.argb(125, 181, 104, 255));
        stroke.setStrokeWidth(dp(.9f));
        c.save(); c.rotate(28f - t * 5f, cx, cy); c.drawOval(ringTall, stroke); c.restore();

        stroke.setColor(Color.argb(125, 70, 215, 255));
        stroke.setStrokeWidth(dp(.85f));
        c.save(); c.rotate(-34f - t * 4f, cx, cy); c.drawOval(ringTilt, stroke); c.restore();

        stroke.setPathEffect(new DashPathEffect(new float[]{dp(7), dp(10)}, dp(4) * t));
        stroke.setColor(Color.argb(100, 129, 91, 255));
        stroke.setStrokeWidth(dp(.8f));
        c.save(); c.rotate(52f + t * 10f, cx, cy); c.drawOval(ring, stroke); c.restore();
        stroke.setPathEffect(null);
    }

    private void drawParticles(Canvas c, float cx, float cy, float orbit, float t) {
        for (int i = 0; i < 7; i++) {
            double a = t * (.38 + i * .025) + i * (Math.PI * 2 / 7.0);
            float x = cx + (float) Math.cos(a) * orbit * (i % 2 == 0 ? 1.0f : .78f);
            float y = cy + (float) Math.sin(a) * orbit * (i % 2 == 0 ? .43f : .72f);
            float r = dp(i % 3 == 0 ? 2.2f : 1.4f);
            glow.setColor(Color.argb(i % 2 == 0 ? 170 : 110, i % 2 == 0 ? 114 : 78, i % 2 == 0 ? 191 : 121, 255));
            glow.setShadowLayer(r * 2.5f, 0, 0, Color.argb(80, 98, 128, 255));
            c.drawCircle(x, y, r, glow);
            glow.clearShadowLayer();
        }
    }

    private void drawSphere(Canvas c, float cx, float cy, float radius, float t) {
        fill.setShader(sphereShader);
        fill.setShadowLayer(radius * .75f, 0, radius * .20f, Color.argb(125, 100, 95, 255));
        c.drawCircle(cx, cy, radius, fill);
        fill.clearShadowLayer();
        fill.setShader(null);

        fill.setColor(Color.argb(95, 15, 24, 58));
        c.drawCircle(cx, cy, radius * .79f, fill);
        fill.setColor(Color.argb(255, 255, 255, 255));
        fill.setShadowLayer(radius * .30f, -radius * .20f, -radius * .20f, Color.argb(100, 90, 130, 255));
        c.drawCircle(cx - radius * .28f, cy - radius * .31f, radius * .12f, fill);
        fill.clearShadowLayer();

        stroke.setColor(Color.argb(210, 191, 219, 255));
        stroke.setStrokeWidth(dp(1.3f));
        c.drawPath(mark, stroke);

        stroke.setColor(Color.argb(80, 76, 225, 255));
        stroke.setStrokeWidth(dp(.7f));
        c.drawCircle(cx, cy, radius * 1.08f, stroke);
    }

    private void drawPedestal(Canvas c, float cx, float cy, float width, float t) {
        float left = cx - width;
        float right = cx + width;
        float top = cy - dp(12);
        float bottom = cy + dp(28);
        baseOuter.set(left, top, right, bottom);
        baseInner.set(left + dp(10), top + dp(8), right - dp(10), bottom - dp(10));

        fill.setColor(Color.argb(115, 52, 86, 155));
        fill.setShadowLayer(dp(20), 0, dp(4), Color.argb(85, 60, 128, 255));
        c.drawOval(baseOuter, fill);
        fill.clearShadowLayer();

        fill.setColor(Color.rgb(7, 16, 31));
        c.drawOval(baseInner, fill);

        stroke.setStrokeWidth(dp(1.2f));
        stroke.setColor(Color.argb(185, 87, 124, 213));
        c.drawOval(baseOuter, stroke);
        stroke.setColor(Color.argb(125, 130, 87, 255));
        c.drawOval(baseInner, stroke);

        stroke.setStrokeWidth(dp(.75f));
        for (int i = 0; i < 8; i++) {
            float x = left + (right - left) * (i + .5f) / 8f;
            float alpha = 65f + 45f * (float) Math.sin(t * 2.0f + i);
            stroke.setColor(Color.argb((int) alpha, 92, 140, 255));
            c.drawLine(x, top + dp(3), x, top + dp(8), stroke);
        }
    }
}
