package com.tiredcity.app.utils;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class DateUtils {
    private static final Locale VN_LOCALE = new Locale("vi", "VN");
    private static final SimpleDateFormat DISPLAY_FORMAT = new SimpleDateFormat("dd/MM/yyyy", VN_LOCALE);
    private static final SimpleDateFormat DATETIME_FORMAT = new SimpleDateFormat("dd/MM/yyyy HH:mm", VN_LOCALE);
    private static final SimpleDateFormat API_FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US);

    private DateUtils() {}

    public static String formatDisplayDate(Date date) {
        if (date == null) return "";
        return DISPLAY_FORMAT.format(date);
    }

    public static String formatDisplayDateTime(Date date) {
        if (date == null) return "";
        return DATETIME_FORMAT.format(date);
    }

    public static String formatApiDate(Date date) {
        if (date == null) return "";
        return API_FORMAT.format(date);
    }

    public static Date parseApiDate(String dateStr) {
        if (dateStr == null || dateStr.isEmpty()) return null;
        try {
            return API_FORMAT.parse(dateStr);
        } catch (Exception e) {
            return null;
        }
    }

    /** Alias for formatDisplayDate — used by activities that call DateUtils.formatDisplay(). */
    public static String formatDisplay(Date date) {
        return formatDisplayDate(date);
    }

    public static String getRelativeTime(Date date) {
        if (date == null) return "";
        long diffMs = new Date().getTime() - date.getTime();
        long diffMins = diffMs / 60_000;
        if (diffMins < 1) return "Vừa xong";
        if (diffMins < 60) return diffMins + " phút trước";
        long diffHours = diffMins / 60;
        if (diffHours < 24) return diffHours + " giờ trước";
        long diffDays = diffHours / 24;
        if (diffDays < 30) return diffDays + " ngày trước";
        return formatDisplayDate(date);
    }
}
