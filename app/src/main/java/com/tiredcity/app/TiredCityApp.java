package com.tiredcity.app;

import android.app.Application;
import com.tiredcity.app.utils.DevTools;
import com.tiredcity.app.utils.ThemeManager;

/** Application gốc. Chạy reset dữ liệu khi debug (xem DevTools.RESET_ON_LAUNCH). */
public class TiredCityApp extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        // Áp dụng chế độ sáng/tối đã lưu ngay từ đầu tiến trình, tránh nháy sai theme.
        ThemeManager.applySavedTheme(this);
        // Bản debug: tuỳ chọn xoá sạch dữ liệu để test luồng đăng ký như người dùng mới.
        DevTools.maybeResetOnLaunch(this);
    }
}
