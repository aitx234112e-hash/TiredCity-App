package com.tiredcity.app.utils;

import android.graphics.Bitmap;
import android.graphics.Color;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;

import java.util.HashMap;
import java.util.Map;

/**
 * Sinh mã QR (Bitmap) từ một chuỗi nội dung — dùng ZXing core.
 * QR vẽ trên nền TRẮNG cho dễ quét, hai màu tuỳ biến để hợp tông thương hiệu.
 */
public final class QrGenerator {

    private QrGenerator() {}

    /** Tạo QR vuông cạnh {@code sizePx}px, ô đậm màu {@code fgColor} trên nền {@code bgColor}. */
    public static Bitmap create(String content, int sizePx, int fgColor, int bgColor) {
        if (content == null || content.isEmpty()) content = " ";
        Map<EncodeHintType, Object> hints = new HashMap<>();
        hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M);
        hints.put(EncodeHintType.MARGIN, 1);
        hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");

        try {
            BitMatrix matrix = new QRCodeWriter()
                    .encode(content, BarcodeFormat.QR_CODE, sizePx, sizePx, hints);
            int w = matrix.getWidth();
            int h = matrix.getHeight();
            int[] pixels = new int[w * h];
            for (int y = 0; y < h; y++) {
                int offset = y * w;
                for (int x = 0; x < w; x++) {
                    pixels[offset + x] = matrix.get(x, y) ? fgColor : bgColor;
                }
            }
            Bitmap bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
            bitmap.setPixels(pixels, 0, w, 0, 0, w, h);
            return bitmap;
        } catch (WriterException e) {
            return null;
        }
    }

    /** Mã QR mặc định: đen trên trắng. */
    public static Bitmap create(String content, int sizePx) {
        return create(content, sizePx, Color.BLACK, Color.WHITE);
    }
}
