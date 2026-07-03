package com.tiredcity.app.ui.widget;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import androidx.core.content.ContextCompat;

import com.tiredcity.app.R;

/**
 * Slider "kéo mảnh ghép": khách kéo mảnh ghép tròn từ trái sang khớp với ô đích bên phải.
 * Chỉ báo hoàn tất (onSlideComplete) khi thả tay trong vùng đích.
 */
public class PuzzleSliderView extends View {

    public interface OnSlideCompleteListener {
        void onSlideComplete();
    }

    private final Paint trackPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint progressPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint targetPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint thumbPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint iconPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF trackRect = new RectF();

    private float thumbRadius;
    private float minX;
    private float maxX;
    private float thumbX;
    private boolean dragging;
    private boolean solved;
    private OnSlideCompleteListener listener;

    public PuzzleSliderView(Context context) {
        this(context, null);
    }

    public PuzzleSliderView(Context context, AttributeSet attrs) {
        super(context, attrs);
        trackPaint.setColor(ContextCompat.getColor(context, R.color.bg_subtle));
        progressPaint.setColor(ContextCompat.getColor(context, R.color.tc_spx_orange_pale));
        targetPaint.setStyle(Paint.Style.STROKE);
        targetPaint.setStrokeWidth(dp(2));
        targetPaint.setColor(ContextCompat.getColor(context, R.color.text_hint));
        targetPaint.setPathEffect(new DashPathEffect(new float[]{dp(4), dp(4)}, 0));
        thumbPaint.setColor(ContextCompat.getColor(context, R.color.tc_spx_orange));
        iconPaint.setColor(ContextCompat.getColor(context, R.color.white));
        iconPaint.setTextAlign(Paint.Align.CENTER);
    }

    public void setOnSlideCompleteListener(OnSlideCompleteListener listener) {
        this.listener = listener;
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldW, int oldH) {
        super.onSizeChanged(w, h, oldW, oldH);
        thumbRadius = h / 2f - dp(3);
        minX = thumbRadius + dp(3);
        maxX = w - thumbRadius - dp(3);
        thumbX = minX;
        iconPaint.setTextSize(thumbRadius);
        trackRect.set(0, 0, w, h);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float radius = getHeight() / 2f;
        canvas.drawRoundRect(trackRect, radius, radius, trackPaint);

        RectF progressRect = new RectF(0, 0, thumbX + thumbRadius, getHeight());
        canvas.drawRoundRect(progressRect, radius, radius, progressPaint);

        canvas.drawCircle(maxX, getHeight() / 2f, thumbRadius * 0.85f, targetPaint);

        canvas.drawCircle(thumbX, getHeight() / 2f, thumbRadius, thumbPaint);
        String icon = solved ? "✓" : "›";
        Paint.FontMetrics fm = iconPaint.getFontMetrics();
        float textY = getHeight() / 2f - (fm.ascent + fm.descent) / 2f;
        canvas.drawText(icon, thumbX, textY, iconPaint);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (solved) return true;
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                if (Math.abs(event.getX() - thumbX) <= thumbRadius * 1.8f) {
                    dragging = true;
                    if (getParent() != null) getParent().requestDisallowInterceptTouchEvent(true);
                }
                return true;
            case MotionEvent.ACTION_MOVE:
                if (dragging) {
                    thumbX = clamp(event.getX(), minX, maxX);
                    invalidate();
                }
                return true;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                if (dragging) {
                    dragging = false;
                    if (getParent() != null) getParent().requestDisallowInterceptTouchEvent(false);
                    if (thumbX >= maxX - thumbRadius) {
                        thumbX = maxX;
                        solved = true;
                        invalidate();
                        if (listener != null) listener.onSlideComplete();
                    } else {
                        animateBackToStart();
                    }
                }
                return true;
            default:
                return super.onTouchEvent(event);
        }
    }

    private void animateBackToStart() {
        ValueAnimator animator = ValueAnimator.ofFloat(thumbX, minX);
        animator.addUpdateListener(a -> {
            thumbX = (float) a.getAnimatedValue();
            invalidate();
        });
        animator.setDuration(200);
        animator.start();
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    private float dp(float v) {
        return v * getResources().getDisplayMetrics().density;
    }
}
