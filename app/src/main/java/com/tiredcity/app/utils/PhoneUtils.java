package com.tiredcity.app.utils;

import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;

/**
 * Số điện thoại Việt Nam: 10 chữ số, hiển thị nhóm 4-3-3 (vd "0912 345 678").
 *
 * <p>Lưu trữ luôn là chuỗi CHỈ CHỮ SỐ ({@link #digits}) — khoảng trắng chỉ để dễ đọc trên UI.
 * Gắn {@link #attach(EditText)} vào ô nhập để vừa tự chèn khoảng trắng vừa chặn quá 10 số.
 */
public final class PhoneUtils {

    /** Đầu số hợp lệ sau số 0: 2 (cố định), 3/5/7/8/9 (di động). 4 và 6 đã bị thu hồi. */
    private static final String VALID_SECOND_DIGITS = "235789";

    private PhoneUtils() {}

    /** Bỏ mọi ký tự không phải chữ số. */
    public static String digits(String raw) {
        if (raw == null) return "";
        StringBuilder sb = new StringBuilder(raw.length());
        for (int i = 0; i < raw.length(); i++) {
            char c = raw.charAt(i);
            if (c >= '0' && c <= '9') sb.append(c);
        }
        return sb.toString();
    }

    /** Chèn khoảng trắng theo nhóm 4-3-3. Nhận cả chuỗi đã/chưa định dạng, dài ngắn tuỳ ý. */
    public static String format(String raw) {
        String d = digits(raw);
        if (d.length() > 10) d = d.substring(0, 10);

        StringBuilder sb = new StringBuilder(12);
        for (int i = 0; i < d.length(); i++) {
            if (i == 4 || i == 7) sb.append(' ');
            sb.append(d.charAt(i));
        }
        return sb.toString();
    }

    /** Đủ 10 số, bắt đầu bằng 0, đầu số thuộc dải đang lưu hành. */
    public static boolean isValid(String raw) {
        String d = digits(raw);
        return d.length() == 10
                && d.charAt(0) == '0'
                && VALID_SECOND_DIGITS.indexOf(d.charAt(1)) >= 0;
    }

    /** Rỗng (chưa nhập gì) — dùng để phân biệt "bỏ trống" với "nhập sai". */
    public static boolean isEmpty(String raw) {
        return digits(raw).isEmpty();
    }

    /**
     * Tự định dạng 4-3-3 khi gõ và chặn cứng ở 10 chữ số.
     *
     * <p>Con trỏ được đặt lại theo SỐ CHỮ SỐ đứng trước nó (không theo vị trí ký tự), nên chèn
     * thêm khoảng trắng không làm con trỏ nhảy lung tung khi sửa ở giữa chuỗi.
     */
    public static void attach(EditText field) {
        field.addTextChangedListener(new TextWatcher() {
            private boolean editing;
            private int digitsBeforeCursor;

            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (editing) return;
                // Đếm chữ số nằm trước vị trí con trỏ SAU khi gõ, để khôi phục đúng chỗ.
                digitsBeforeCursor = digits(s.subSequence(0, start + count).toString()).length();
            }

            @Override public void afterTextChanged(Editable s) {
                if (editing) return;

                String formatted = format(s.toString());
                if (!formatted.equals(s.toString())) {
                    editing = true;
                    s.replace(0, s.length(), formatted);
                    editing = false;
                }

                // Đi tới ký tự đứng sau chữ số thứ digitsBeforeCursor.
                int seen = 0, pos = 0;
                while (pos < formatted.length() && seen < digitsBeforeCursor) {
                    if (Character.isDigit(formatted.charAt(pos))) seen++;
                    pos++;
                }
                field.setSelection(Math.min(pos, formatted.length()));

                // Chỉ báo lỗi khi đã gõ đủ 10 số mà vẫn sai đầu số — không "mắng" giữa chừng.
                String d = digits(formatted);
                if (d.length() == 10 && !isValid(formatted)) {
                    field.setError(field.getContext().getString(
                            com.tiredcity.app.R.string.error_phone_invalid));
                } else {
                    field.setError(null);
                }
            }
        });
    }
}
