package com.tiredcity.app.utils;

import android.content.Context;

import androidx.annotation.DrawableRes;
import androidx.annotation.StringRes;

import com.tiredcity.app.R;

public class MenhCalculator {
    private MenhCalculator() {}

    /**
     * Tính Mệnh Ngũ Hành từ năm sinh (âm lịch) theo NẠP ÂM — Lục thập hoa giáp.
     *
     * <p>Ngũ hành bản mệnh KHÔNG suy ra được từ chữ số cuối của năm; nó phụ thuộc vào
     * Thiên Can và Địa Chi (chu kỳ 60 năm). Công thức:
     * <pre>
     *   giá trị Can (Thiên Can theo năm % 10):
     *     Giáp/Ất=1, Bính/Đinh=2, Mậu/Kỷ=3, Canh/Tân=4, Nhâm/Quý=5
     *   giá trị Chi (Địa Chi theo năm % 12):
     *     Tý/Sửu/Ngọ/Mùi=0, Dần/Mão/Thân/Dậu=1, Thìn/Tỵ/Tuất/Hợi=2
     *   tổng = Can + Chi; nếu tổng > 5 thì trừ 5
     *   1→Kim, 2→Thủy, 3→Hỏa, 4→Thổ, 5→Mộc
     * </pre>
     * Đã đối chiếu đúng với nhiều năm quen thuộc: 1984 Giáp Tý = Kim, 1990 Canh Ngọ = Thổ,
     * 1995 Ất Hợi = Hỏa, 1988 Mậu Thìn = Mộc, 1996 Bính Tý = Thủy, 2000 Canh Thìn = Kim.
     *
     * <p>Lưu ý: dùng năm âm lịch. App chỉ có năm sinh dương lịch nên người sinh vào
     * tháng 1 / đầu tháng 2 (trước Tết) về lý thuộc năm can-chi trước đó — đây là giản
     * lược chấp nhận được khi không có ngày sinh đầy đủ.
     */
    public static String tinhMenh(int namSinh) {
        // Can theo năm % 10: bắt đầu từ Canh (0) … Kỷ (9).
        int[] canValue = {4, 4, 5, 5, 1, 1, 2, 2, 3, 3};
        // Chi theo năm % 12: 0→Thân, 1→Dậu, 2→Tuất, 3→Hợi, 4→Tý, 5→Sửu,
        //                    6→Dần, 7→Mão, 8→Thìn, 9→Tỵ, 10→Ngọ, 11→Mùi
        int[] chiValue = {1, 1, 2, 2, 0, 0, 1, 1, 2, 2, 0, 0};

        int sum = canValue[namSinh % 10] + chiValue[namSinh % 12];
        if (sum > 5) sum -= 5;

        switch (sum) {
            case 1:  return Constants.MENH_KIM;
            case 2:  return Constants.MENH_THUY;
            case 3:  return Constants.MENH_HOA;
            case 4:  return Constants.MENH_THO;
            case 5:  return Constants.MENH_MOC;
            default: return Constants.MENH_THO;
        }
    }

    /**
     * Trả về mảng màu sắc hợp mệnh cho mệnh truyền vào.
     */
    public static String[] getMauHopMenh(String menh) {
        if (menh == null) return new String[0];
        switch (menh) {
            case "Kim":  return new String[]{"Trắng", "Vàng", "Bạc", "Xám"};
            case "Mộc":  return new String[]{"Xanh lá", "Xanh lam", "Xanh rêu"};
            case "Thủy": return new String[]{"Đen", "Xanh navy", "Tím"};
            case "Hỏa":  return new String[]{"Đỏ", "Hồng", "Cam", "Tím"};
            case "Thổ":  return new String[]{"Vàng đất", "Nâu", "Be", "Cam nhạt"};
            default:     return new String[]{};
        }
    }

    /**
     * Tính cung hoàng đạo dựa theo ngày/tháng sinh (1-indexed).
     */
    public static String tinhCungHoangDao(int month, int day) {
        if ((month == 3 && day >= 21) || (month == 4 && day <= 19)) return "Bạch Dương";
        if ((month == 4 && day >= 20) || (month == 5 && day <= 20)) return "Kim Ngưu";
        if ((month == 5 && day >= 21) || (month == 6 && day <= 20)) return "Song Tử";
        if ((month == 6 && day >= 21) || (month == 7 && day <= 22)) return "Cự Giải";
        if ((month == 7 && day >= 23) || (month == 8 && day <= 22)) return "Sư Tử";
        if ((month == 8 && day >= 23) || (month == 9 && day <= 22)) return "Xử Nữ";
        if ((month == 9 && day >= 23) || (month == 10 && day <= 22)) return "Thiên Bình";
        if ((month == 10 && day >= 23) || (month == 11 && day <= 21)) return "Thiên Yết";
        if ((month == 11 && day >= 22) || (month == 12 && day <= 21)) return "Nhân Mã";
        if ((month == 12 && day >= 22) || (month == 1 && day <= 19)) return "Ma Kết";
        if ((month == 1 && day >= 20) || (month == 2 && day <= 18)) return "Bảo Bình";
        return "Song Ngư";
    }

    /**
     * Tính con giáp (12 chi) theo năm sinh dương lịch.
     * Công thức: (namSinh % 12) với 1900 = Tý (4).
     */
    public static String tinhConGiap(int namSinh) {
        String[] conGiap = {
            "Thân", "Dậu", "Tuất", "Hợi",
            "Tý",   "Sửu", "Dần",  "Mão",
            "Thìn", "Tỵ",  "Ngọ",  "Mùi"
        };
        return conGiap[namSinh % 12];
    }

    // ── Localization helpers ──────────────────────────────────────────────────
    // Các giá trị mệnh / cung / màu được lưu dạng tiếng Việt (canonical).
    // Những hàm dưới đây chuyển chúng sang chuỗi đa ngôn ngữ theo locale hiện tại.

    /** Tên mệnh đã dịch theo ngôn ngữ ("Thủy" → "Water"). */
    public static String localizeMenh(Context ctx, String menh) {
        return localize(ctx, menhNameRes(menh), menh);
    }

    /** Tên cung hoàng đạo đã dịch ("Cự Giải" → "Cancer"). */
    public static String localizeZodiac(Context ctx, String zodiac) {
        return localize(ctx, zodiacNameRes(zodiac), zodiac);
    }

    /** Tên màu hợp mệnh đã dịch ("Đen" → "Black"). */
    public static String localizeColor(Context ctx, String color) {
        return localize(ctx, colorNameRes(color), color);
    }

    private static String localize(Context ctx, @StringRes int res, String fallback) {
        return res != 0 ? ctx.getString(res) : fallback;
    }

    @StringRes
    private static int menhNameRes(String menh) {
        if (menh == null) return 0;
        switch (menh) {
            case "Kim":  return R.string.menh_name_kim;
            case "Mộc":  return R.string.menh_name_moc;
            case "Thủy": return R.string.menh_name_thuy;
            case "Hỏa":  return R.string.menh_name_hoa;
            case "Thổ":  return R.string.menh_name_tho;
            default:     return 0;
        }
    }

    @StringRes
    private static int zodiacNameRes(String zodiac) {
        if (zodiac == null) return 0;
        switch (zodiac) {
            case "Bạch Dương": return R.string.zodiac_aries;
            case "Kim Ngưu":   return R.string.zodiac_taurus;
            case "Song Tử":    return R.string.zodiac_gemini;
            case "Cự Giải":    return R.string.zodiac_cancer;
            case "Sư Tử":      return R.string.zodiac_leo;
            case "Xử Nữ":      return R.string.zodiac_virgo;
            case "Thiên Bình": return R.string.zodiac_libra;
            case "Thiên Yết":  return R.string.zodiac_scorpio;
            case "Nhân Mã":    return R.string.zodiac_sagittarius;
            case "Ma Kết":     return R.string.zodiac_capricorn;
            case "Bảo Bình":   return R.string.zodiac_aquarius;
            case "Song Ngư":   return R.string.zodiac_pisces;
            default:           return 0;
        }
    }

    @StringRes
    private static int colorNameRes(String color) {
        if (color == null) return 0;
        switch (color) {
            case "Trắng":     return R.string.color_white;
            case "Vàng":      return R.string.color_yellow;
            case "Bạc":       return R.string.color_silver;
            case "Xám":       return R.string.color_gray;
            case "Xanh lá":   return R.string.color_green;
            case "Xanh lam":  return R.string.color_blue;
            case "Xanh rêu":  return R.string.color_moss;
            case "Đen":       return R.string.color_black;
            case "Xanh navy": return R.string.color_navy;
            case "Tím":       return R.string.color_purple;
            case "Đỏ":        return R.string.color_red;
            case "Hồng":      return R.string.color_pink;
            case "Cam":       return R.string.color_orange;
            case "Vàng đất":  return R.string.color_earth_yellow;
            case "Nâu":       return R.string.color_brown;
            case "Be":        return R.string.color_beige;
            case "Cam nhạt":  return R.string.color_light_orange;
            default:          return 0;
        }
    }

    /**
     * Tranh minh hoạ hero theo từng mệnh (dùng cho trang gợi ý phong cách).
     * Trả về 0 nếu mệnh chưa có ảnh riêng — khi đó hero dùng nền gradient sơn mài.
     */
    @DrawableRes
    public static int getMenhArtwork(String menh) {
        if (menh == null) return 0;
        switch (menh) {
            case "Kim": return R.drawable.ai_1;
            default:    return 0;
        }
    }

    /** Emoji đại diện cho mệnh. */
    public static String getEmojiMenh(String menh) {
        if (menh == null) return "✨";
        switch (menh) {
            case "Kim":  return "🪙";
            case "Mộc":  return "🌿";
            case "Thủy": return "💧";
            case "Hỏa":  return "🔥";
            case "Thổ":  return "⛰️";
            default:     return "✨";
        }
    }
}
