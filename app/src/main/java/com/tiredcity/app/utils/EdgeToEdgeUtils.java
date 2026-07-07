package com.tiredcity.app.utils;

import android.view.View;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

/**
 * Tiện ích xử lý inset khi vẽ tràn viền (edge-to-edge).
 *
 * targetSdk 35 (Android 15+) BUỘC vẽ edge-to-edge và BỎ QUA {@code android:statusBarColor} trong theme,
 * nên thanh header trên cùng của các fragment/màn hình sẽ chui LÊN dưới cục wifi/pin — chữ (nhất là dấu
 * tiếng Việt phần trên) bị cắt. Không sửa được bằng XML/theme; phải đệm inset lúc chạy.
 */
public final class EdgeToEdgeUtils {

    private EdgeToEdgeUtils() { }

    /**
     * Đệm thêm chiều cao status bar vào paddingTop của {@code view} để nội dung nằm hẳn dưới thanh
     * trạng thái. Giữ nguyên padding gốc (chỉ cộng inset một lần, không cộng dồn qua mỗi lần gọi lại).
     */
    public static void applyStatusBarTopPadding(View view) {
        final int basePaddingTop = view.getPaddingTop();
        ViewCompat.setOnApplyWindowInsetsListener(view, (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(v.getPaddingLeft(), basePaddingTop + bars.top,
                    v.getPaddingRight(), v.getPaddingBottom());
            return insets;
        });
        ViewCompat.requestApplyInsets(view);
    }
}
