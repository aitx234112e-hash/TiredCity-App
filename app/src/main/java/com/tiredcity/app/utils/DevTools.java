package com.tiredcity.app.utils;

import android.content.Context;
import android.widget.Toast;
import com.tiredcity.app.BuildConfig;

/**
 * Tiện ích chỉ dùng khi phát triển (DEBUG).
 *
 * Mục đích: mỗi lần Rebuild/Run, tự xoá sạch dữ liệu cục bộ để app coi bạn là
 * NGƯỜI DÙNG MỚI HOÀN TOÀN — phục vụ test lại luồng Đăng ký + hiển thị thẻ Mệnh.
 */
public final class DevTools {
    private DevTools() {}

    /**
     * Công tắc bật/tắt tự reset khi khởi động bản debug.
     *   true  → mỗi lần chạy app sẽ xoá sạch (token, hồ sơ, mệnh, giỏ hàng, thông báo).
     *   false → giữ nguyên dữ liệu như bình thường.
     * ➜ Khi test xong, đổi về false (hoặc build bản release — release luôn bỏ qua).
     */
    public static final boolean RESET_ON_LAUNCH = true;

    /** Các file SharedPreferences mà app đang dùng. */
    private static final String[] PREF_FILES = {
            Constants.PREF_NAME,   // "tiredcity_prefs" — token, hồ sơ, mệnh, ngôn ngữ...
            "cart_local",          // CartLocalStore
            "notification_local"   // NotificationStore
    };

    /** Gọi trong Application.onCreate. Chỉ chạy ở bản DEBUG và khi RESET_ON_LAUNCH = true. */
    public static void maybeResetOnLaunch(Context context) {
        if (!BuildConfig.DEBUG || !RESET_ON_LAUNCH) return;
        clearAllLocalData(context);
        try {
            Toast.makeText(context, "DEV: đã xoá dữ liệu — người dùng mới", Toast.LENGTH_SHORT).show();
        } catch (Exception ignored) { /* Toast quá sớm có thể lỗi trên vài thiết bị */ }
    }

    /** Xoá toàn bộ dữ liệu cục bộ (đồng bộ để hoàn tất trước khi mở màn hình đầu tiên). */
    public static void clearAllLocalData(Context context) {
        for (String name : PREF_FILES) {
            context.getSharedPreferences(name, Context.MODE_PRIVATE)
                    .edit().clear().commit();
        }
    }
}
