package com.tiredcity.app.ui.widget;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.DrawableRes;

import com.tiredcity.app.R;

import java.util.Random;

/**
 * Captcha ghép hình: hiển thị một tấm ảnh có ô khuyết hình mảnh ghép; một mảnh ghép (cắt đúng
 * phần ảnh ở ô khuyết) trượt ngang theo thanh {@link PuzzleSliderView}. Khớp khi mảnh ghép về
 * đúng vị trí ô khuyết.
 */
public class PuzzleCaptchaView extends View {

    /** Kho ảnh nền (ảnh Việt phục có sẵn trong app). */
    @DrawableRes
    private static final int[] IMAGE_POOL = {
            R.drawable.carousel_aodai_1, R.drawable.carousel_aodai_3, R.drawable.carousel_aodai_5,
            R.drawable.carousel_nhatbinh_2, R.drawable.carousel_nhatbinh_4,
            R.drawable.carousel_aotac_1, R.drawable.carousel_aotac_3,
            R.drawable.carousel_giaolinh_2, R.drawable.carousel_giaolinh_4,
            R.drawable.carousel_yemdao_1, R.drawable.carousel_yemdao_3
    };

    private final Paint bitmapPaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
    private final Paint holeFill = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint holeStroke = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint pieceStroke = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Random random = new Random();

    private Bitmap base;
    private Path piecePath;
    private float side;        // cạnh thân mảnh ghép
    private float bump;        // độ nhô của núm
    private float pieceY;      // mép trên thân mảnh ghép
    private float startX;      // vị trí bắt đầu (mép trái thân)
    private float maxX;        // vị trí xa nhất
    private float targetX;     // vị trí ô khuyết
    private float currentX;    // vị trí hiện tại của mảnh ghép
    private float tolerance;
    private int poolIndex;
    private boolean solved;

    public PuzzleCaptchaView(Context context) {
        this(context, null);
    }

    public PuzzleCaptchaView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setLayerType(LAYER_TYPE_SOFTWARE, null);

        holeFill.setColor(0x73000000);           // đen ~45% cho ô khuyết
        holeStroke.setStyle(Paint.Style.STROKE);
        holeStroke.setColor(0xB3FFFFFF);
        pieceStroke.setStyle(Paint.Style.STROKE);
        pieceStroke.setColor(Color.WHITE);

        poolIndex = random.nextInt(IMAGE_POOL.length);
    }

    /** Đổi ảnh + vị trí ô khuyết mới (nút làm mới). */
    public void reload() {
        poolIndex = (poolIndex + 1 + random.nextInt(IMAGE_POOL.length - 1)) % IMAGE_POOL.length;
        prepare(getWidth(), getHeight());
        invalidate();
    }

    /** Cập nhật vị trí mảnh ghép theo tiến độ 0..1 của thanh trượt. */
    public void setProgress(float progress) {
        if (solved) return;
        currentX = startX + progress * (maxX - startX);
        invalidate();
    }

    /** Kiểm tra mảnh ghép có khớp ô khuyết không; nếu khớp thì chốt lại. */
    public boolean checkSolved() {
        if (Math.abs(currentX - targetX) <= tolerance) {
            solved = true;
            currentX = targetX;
            invalidate();
            return true;
        }
        return false;
    }

    /** Trả mảnh ghép về đầu khi kéo trượt. */
    public void resetPiece() {
        if (solved) return;
        currentX = startX;
        invalidate();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldW, int oldH) {
        super.onSizeChanged(w, h, oldW, oldH);
        prepare(w, h);
    }

    private void prepare(int w, int h) {
        if (w <= 0 || h <= 0) return;

        side = Math.min(h * 0.34f, dp(56));
        bump = side * 0.26f;
        tolerance = dp(4);   // siết dung sai → khó hơn, phải canh chính xác
        pieceStroke.setStrokeWidth(dp(1.5f));
        holeStroke.setStrokeWidth(dp(1.5f));
        piecePath = buildPuzzlePath(side, bump);

        base = decodeCropped(IMAGE_POOL[poolIndex], w, h);

        startX = dp(12);
        maxX = w - side - bump * 1.35f - dp(3);
        // ô khuyết nằm ở nửa phải để có quãng kéo rõ ràng
        targetX = startX + (maxX - startX) * (0.45f + random.nextFloat() * 0.5f);
        // mảnh ghép lơ lửng canh giữa theo chiều dọc
        pieceY = (h - side) / 2f;
        currentX = startX;
        solved = false;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (base == null || piecePath == null) return;

        canvas.drawBitmap(base, 0, 0, bitmapPaint);

        // Ô khuyết tại vị trí đích
        canvas.save();
        canvas.translate(targetX, pieceY);
        canvas.drawPath(piecePath, holeFill);
        canvas.drawPath(piecePath, holeStroke);
        canvas.restore();

        // Mảnh ghép trượt (nội dung ảnh của ô khuyết, vẽ ở vị trí hiện tại)
        canvas.save();
        canvas.translate(currentX, pieceY);
        canvas.save();
        canvas.clipPath(piecePath);
        canvas.drawBitmap(base, -targetX, -pieceY, bitmapPaint);
        canvas.restore();
        canvas.drawPath(piecePath, pieceStroke);
        canvas.restore();
    }

    /**
     * Dựng đường viền mảnh ghép: thân vuông cạnh {@code s}, núm lồi hướng lên ở cạnh trên và
     * núm lồi hướng phải ở cạnh phải. Gốc (0,0) là mép trên-trái thân; núm trên nhô lên vùng y âm.
     */
    private Path buildPuzzlePath(float s, float knob) {
        float c = s * 0.5f;
        float k = knob;
        Path p = new Path();
        p.moveTo(0, 0);
        // Cạnh trên: núm lồi lên
        p.lineTo(c - k, 0);
        p.cubicTo(c - k, -k * 1.35f, c + k, -k * 1.35f, c + k, 0);
        p.lineTo(s, 0);
        // Cạnh phải: núm lồi ra ngoài
        p.lineTo(s, c - k);
        p.cubicTo(s + k * 1.35f, c - k, s + k * 1.35f, c + k, s, c + k);
        p.lineTo(s, s);
        // Cạnh dưới + trái
        p.lineTo(0, s);
        p.close();
        return p;
    }

    /** Giải mã ảnh và cắt center-crop vừa khít khung view. */
    private Bitmap decodeCropped(@DrawableRes int resId, int w, int h) {
        BitmapFactory.Options bounds = new BitmapFactory.Options();
        bounds.inJustDecodeBounds = true;
        BitmapFactory.decodeResource(getResources(), resId, bounds);

        BitmapFactory.Options opts = new BitmapFactory.Options();
        opts.inSampleSize = Math.max(1, Math.min(bounds.outWidth / Math.max(1, w),
                bounds.outHeight / Math.max(1, h)));
        Bitmap src = BitmapFactory.decodeResource(getResources(), resId, opts);
        if (src == null) return Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);

        float scale = Math.max((float) w / src.getWidth(), (float) h / src.getHeight());
        int scaledW = Math.round(src.getWidth() * scale);
        int scaledH = Math.round(src.getHeight() * scale);
        Bitmap scaled = Bitmap.createScaledBitmap(src, scaledW, scaledH, true);

        int left = Math.max(0, (scaledW - w) / 2);
        int top = Math.max(0, (scaledH - h) / 2);
        // Card bao ngoài lo phần bo góc; ở đây trả ảnh phẳng đã cắt vừa khung
        return Bitmap.createBitmap(scaled, left, top,
                Math.min(w, scaledW - left), Math.min(h, scaledH - top));
    }

    private float dp(float v) {
        return v * getResources().getDisplayMetrics().density;
    }
}
