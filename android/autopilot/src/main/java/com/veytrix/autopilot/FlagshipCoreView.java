package com.veytrix.autopilot;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.Shader;
import android.view.View;

/** Lightweight native-rendered autonomous core for the flagship Android surface. */
public final class FlagshipCoreView extends View {
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint stroke = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final float density;
    private float phase;

    public FlagshipCoreView(Context context) {
        super(context);
        density = getResources().getDisplayMetrics().density;
        stroke.setStyle(Paint.Style.STROKE);
        stroke.setStrokeWidth(dp(1));
        stroke.setAntiAlias(true);
        setLayerType(View.LAYER_TYPE_SOFTWARE, null);
    }

    private float dp(float value) { return value * density; }

    public void animateCore(long elapsedMs) {
        phase = (elapsedMs % 4800L) / 4800f;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        final float cx = getWidth() / 2f;
        final float cy = getHeight() / 2f;
        final float size = Math.min(getWidth(), getHeight());
        final float orbit = size * .42f;
        final float core = size * .23f;
        final float drift = (float) Math.sin(phase * Math.PI * 2.0) * dp(5);

        canvas.save();
        canvas.translate(cx, cy + drift);

        stroke.setShader(null);
        stroke.setColor(Color.argb(88, 216, 189, 135));
        canvas.drawOval(-orbit, -orbit * .72f, orbit, orbit * .72f, stroke);

        canvas.rotate(26f);
        stroke.setColor(Color.argb(62, 216, 189, 135));
        canvas.drawOval(-orbit * .82f, -orbit * .50f, orbit * .82f, orbit * .50f, stroke);
        canvas.rotate(-58f);
        stroke.setColor(Color.argb(48, 154, 134, 255));
        canvas.drawOval(-orbit * .70f, -orbit * .44f, orbit * .70f, orbit * .44f, stroke);
        canvas.rotate(32f);

        paint.setShader(new RadialGradient(-core * .28f, -core * .34f, core * 1.35f,
                new int[]{Color.rgb(255, 253, 247), Color.rgb(241, 223, 189), Color.rgb(191, 162, 103), Color.rgb(60, 53, 41), Color.rgb(16, 16, 14)},
                new float[]{0f, .12f, .34f, .68f, 1f}, Shader.TileMode.CLAMP));
        paint.setShadowLayer(dp(30), 0, dp(16), Color.argb(100, 216, 189, 135));
        canvas.drawCircle(0, 0, core, paint);
        paint.clearShadowLayer();

        paint.setShader(new RadialGradient(-core * .16f, -core * .18f, core * .32f,
                new int[]{Color.WHITE, Color.rgb(246, 230, 196), Color.argb(0, 216, 189, 135)},
                new float[]{0f, .42f, 1f}, Shader.TileMode.CLAMP));
        canvas.drawCircle(-core * .04f, -core * .08f, core * .34f, paint);
        paint.setShader(null);
        canvas.restore();
    }
}
