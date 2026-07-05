package com.tiredcity.app.utils;

import android.content.Context;
import androidx.appcompat.app.AppCompatDelegate;

/** Quản lý chế độ sáng/tối (Day/Night) toàn app — lưu lựa chọn, mặc định là chế độ sáng. */
public class ThemeManager {
    private ThemeManager() {}

    private static final String KEY_DARK_MODE = "key_dark_mode";

    /** Áp dụng chế độ đã lưu — gọi sớm nhất (Application.onCreate) để không nháy sai theme. */
    public static void applySavedTheme(Context context) {
        AppCompatDelegate.setDefaultNightMode(isDarkMode(context)
                ? AppCompatDelegate.MODE_NIGHT_YES
                : AppCompatDelegate.MODE_NIGHT_NO);
    }

    public static boolean isDarkMode(Context context) {
        return new PreferenceManager(context).getToggle(KEY_DARK_MODE, false);
    }

    /** Đổi qua lại sáng/tối, lưu lựa chọn và áp dụng ngay. Trả về true nếu giờ là chế độ tối. */
    public static boolean toggleTheme(Context context) {
        PreferenceManager prefs = new PreferenceManager(context);
        boolean nowDark = !prefs.getToggle(KEY_DARK_MODE, false);
        prefs.setToggle(KEY_DARK_MODE, nowDark);
        AppCompatDelegate.setDefaultNightMode(nowDark
                ? AppCompatDelegate.MODE_NIGHT_YES
                : AppCompatDelegate.MODE_NIGHT_NO);
        return nowDark;
    }
}
