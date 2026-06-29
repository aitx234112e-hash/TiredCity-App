package com.tiredcity.app.data.local;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Lưu số thông báo CHƯA ĐỌC để hiển thị badge ở màn hình chính.
 * Mặc định seed bằng số thông báo mẫu chưa đọc (2). Khi mở trang Thông báo
 * sẽ được đánh dấu đã đọc hết (về 0).
 */
public class NotificationStore {

    private static final String PREF_NAME    = "notification_local";
    private static final String KEY_UNREAD   = "unread_count";
    private static final int    DEFAULT_SEED = 2;

    private final SharedPreferences prefs;

    public NotificationStore(Context context) {
        prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    /** Số thông báo chưa đọc hiện tại. */
    public int getUnreadCount() {
        return prefs.getInt(KEY_UNREAD, DEFAULT_SEED);
    }

    /** Cộng thêm n thông báo mới (dùng khi "nhận thêm thông báo"). */
    public void addUnread(int n) {
        prefs.edit().putInt(KEY_UNREAD, Math.max(0, getUnreadCount() + n)).apply();
    }

    /** Đánh dấu đã đọc tất cả. */
    public void markAllRead() {
        prefs.edit().putInt(KEY_UNREAD, 0).apply();
    }
}
