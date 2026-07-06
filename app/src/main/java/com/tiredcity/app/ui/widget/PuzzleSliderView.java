package com.tiredcity.app.ui.widget;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import androidx.core.content.ContextCompat;

import com.tiredcity.app.R;

/**
 * Thanh trượt điều khiển mảnh ghép: khách kéo tay cầm từ trái sang phải, mảnh ghép trên ảnh
 * dịch chuyển theo. Báo tiến độ liên tục (onProgress) và thời điểm thả tay (onReleased) cho
 * {@link PuzzleCaptchaView} kiểm tra có khớp ô khuyết hay không.
 */
public class PuzzleSliderView extends View {

    public interface Listener {
        /** Tiến độ 0..1 khi đang kéo. */
        void onProgress(float progress);
        /** Nhả tay ở tiến độ 0..1 — trả về true nếu khớp (đã xác minh). */
        boolean onReleased(float progress);
    }

    private final Paint trackPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint progressPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint thumbPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint iconPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF trackRect = new RectF();

    private float thumbRadius;
    private float minX;
    private float maxX;
    private float thumbX;
    private boolean dragging;
    private boolean locked;
    private boolean solved;
    private Listener listener;

    public PuzzleSliderView(Context context) {
        this(context, null);
    }

    public PuzzleSliderView(Context context, AttributeSet attrs) {
        super(context, attrs);
        trackPaint.setColor(ContextCompat.getColor(context, R.color.bg_subtle));
        progressPaint.setColor(ContextCompat.getColor(context, R.color.tc_spx_orange_pale));
        thumbPaint.setColor(ContextCompat.getColor(context, R.color.tc_spx_orange));
        iconPaint.setColor(ContextCompat.getColor(context, R.color.white));
        iconPaint.setTextAlign(Paint.Align.CENTER);
        iconPaint.setFakeBoldText(true);
    }

    public void setListener(Listener listener) {
        this.listener = listener;
    }

    /** Đưa tay cầm về đầu (thất bại), mở khoá kéo lại. */
    public void reset() {
        solved = false;
        locked = false;
        animateTo(minX);
    }

    /** Khoá ở trạng thái đã xác minh: tay cầm sáng xanh, hiện dấu ✓. */
    public void lockSolved() {
        solved = true;
        locked = true;
        dragging = false;
        thumbPaint.setColor(ContextCompat.getColor(getContext(), R.color.tc_success));
        progressPaint.setColor(ContextCompat.getColor(getContext(), R.color.tc_success_pale));
        animateTo(maxX);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldW, int oldH) {
        super.onSizeChanged(w, h, oldW, oldH);
        thumbRadius = h / 2f - dp(3);
        minX = thumbRadius + dp(3);
        maxX = w - thumbRadius - dp(3);
        thumbX = minX;
        iconPaint.setTextSize(thumbRadius * 1.1f);
        trackRect.set(0, 0, w, h);
        progressPaint.setShader(new LinearGradient(0, 0, w, 0,
                ContextCompat.getColor(getContext(), R.color.tc_spx_orange_pale),
                ContextCompat.getColor(getContext(), R.color.tc_spx_orange),
                Shader.TileMode.CLAMP));
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float radius = getHeight() / 2f;
        canvas.drawRoundRect(trackRect, radius, radius, trackPaint);

        RectF progressRect = new RectF(0, 0, thumbX + thumbRadius, getHeight());
        canvas.drawRoundRect(progressRect, radius, radius, progressPaint);

        canvas.drawCircle(thumbX, getHeight() / 2f, thumbRadius, thumbPaint);
        String icon = solved ? "✓" : "❯";
        Paint.FontMetrics fm = iconPaint.getFontMetrics();
        float textY = getHeight() / 2f - (fm.ascent + fm.descent) / 2f;
        canvas.drawText(icon, thumbX, textY, iconPaint);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (locked) return true;
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                if (Math.abs(event.getX() - thumbX) <= thumbRadius * 2f) {
                    dragging = true;
                    if (getParent() != null) getParent().requestDisallowInterceptTouchEvent(true);
                }
                return true;
            case MotionEvent.ACTION_MOVE:
                if (dragging) {
                    thumbX = clamp(event.getX(), minX, maxX);
                    invalidate();
                    if (listener != null) listener.onProgress(progress());
                }
                return true;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                if (dragging) {
                    dragging = false;
                    if (getParent() != null) getParent().requestDisallowInterceptTouchEvent(false);
                    boolean ok = listener != null && listener.onReleased(progress());
                    if (!ok) reset();
                }
                return true;
            default:
                return super.onTouchEvent(event);
        }
    }

    private float progress() {
        return (thumbX - minX) / (maxX - minX);
    }

    private void animateTo(float targetX) {
        ValueAnimator animator = ValueAnimator.ofFloat(thumbX, targetX);
        animator.addUpdateListener(a -> {
            thumbX = (float) a.getAnimatedValue();
            invalidate();
        });
        animator.setDuration(220);
        animator.start();
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    private float dp(float v) {
        return v * getResources().getDisplayMetrics().density;
    }
}
