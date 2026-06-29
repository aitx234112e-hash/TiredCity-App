package com.tiredcity.app;

import android.app.Application;
import com.tiredcity.app.utils.DevTools;

/** Application gốc. Chạy reset dữ liệu khi debug (xem DevTools.RESET_ON_LAUNCH). */
public class TiredCityApp extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        // Bản debug: tuỳ chọn xoá sạch dữ liệu để test luồng đăng ký như người dùng mới.
        DevTools.maybeResetOnLaunch(this);
    }
}
