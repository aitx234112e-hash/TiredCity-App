package com.tiredcity.app.utils;

import java.text.NumberFormat;
import java.util.Locale;

public class PriceUtils {
    private static final Locale VN_LOCALE = new Locale("vi", "VN");
    private static final NumberFormat VND_FORMAT = NumberFormat.getCurrencyInstance(VN_LOCALE);

    private PriceUtils() {}

    public static String formatVnd(double amount) {
        return VND_FORMAT.format(amount);
    }

    /** Alias for formatVnd — used by activities that call PriceUtils.format(). */
    public static String format(double amount) {
        return formatVnd(amount);
    }

    public static String formatVndShort(double amount) {
        if (amount >= 1_000_000) {
            return String.format(VN_LOCALE, "%.1f triệu", amount / 1_000_000);
        }
        if (amount >= 1_000) {
            return String.format(VN_LOCALE, "%.0f nghìn", amount / 1_000);
        }
        return formatVnd(amount);
    }

    public static double calculateDiscount(double originalPrice, double salePrice) {
        if (originalPrice <= 0) return 0;
        return Math.round((1 - salePrice / originalPrice) * 100);
    }

    public static String formatDiscountPercent(double originalPrice, double salePrice) {
        int percent = (int) calculateDiscount(originalPrice, salePrice);
        return "-" + percent + "%";
    }
}
