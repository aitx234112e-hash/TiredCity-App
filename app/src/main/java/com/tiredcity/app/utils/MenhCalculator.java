package com.tiredcity.app.utils;

import android.content.Context;

import androidx.annotation.ColorRes;
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

    /** Tiêu đề lớn "Mệnh Kim"/"Metal Element"... ở phần mở đầu trang AI Styling, riêng theo từng
     *  mệnh (đã dịch sẵn theo ngôn ngữ, không ghép chuỗi động). */
    public static String getMenhTitleText(Context ctx, String menh) {
        if (menh == null) return ctx.getString(R.string.aistyle_menh_title_kim);
        switch (menh) {
            case "Kim":  return ctx.getString(R.string.aistyle_menh_title_kim);
            case "Mộc":  return ctx.getString(R.string.aistyle_menh_title_moc);
            case "Thủy": return ctx.getString(R.string.aistyle_menh_title_thuy);
            case "Hỏa":  return ctx.getString(R.string.aistyle_menh_title_hoa);
            case "Thổ":  return ctx.getString(R.string.aistyle_menh_title_tho);
            default:     return ctx.getString(R.string.aistyle_menh_title_kim);
        }
    }

    /** Câu mô tả ngắn về khí chất + màu sắc đặc trưng của mệnh, ngay dưới tiêu đề lớn ở phần mở
     *  đầu trang AI Styling. */
    public static String getMenhDescText(Context ctx, String menh) {
        if (menh == null) return ctx.getString(R.string.aistyle_menh_desc_kim);
        switch (menh) {
            case "Kim":  return ctx.getString(R.string.aistyle_menh_desc_kim);
            case "Mộc":  return ctx.getString(R.string.aistyle_menh_desc_moc);
            case "Thủy": return ctx.getString(R.string.aistyle_menh_desc_thuy);
            case "Hỏa":  return ctx.getString(R.string.aistyle_menh_desc_hoa);
            case "Thổ":  return ctx.getString(R.string.aistyle_menh_desc_tho);
            default:     return ctx.getString(R.string.aistyle_menh_desc_kim);
        }
    }

    /** Tranh banner thương hiệu riêng cho từng mệnh (carousel "Khám phá Ngũ Hành" ở trang AI
     *  Styling) — mỗi ảnh đã có sẵn tên mệnh + hoạ tiết + nhân vật minh hoạ theo đúng tông màu
     *  ngũ hành, không cần chồng chữ/lớp phủ. */
    @DrawableRes
    public static int getMenhBanner(String menh) {
        if (menh == null) return R.drawable.menh_banner_kim;
        switch (menh) {
            case "Kim":  return R.drawable.menh_banner_kim;
            case "Mộc":  return R.drawable.menh_banner_moc;
            case "Thủy": return R.drawable.menh_banner_thuy;
            case "Hỏa":  return R.drawable.menh_banner_hoa;
            case "Thổ":  return R.drawable.menh_banner_tho;
            default:     return R.drawable.menh_banner_kim;
        }
    }

    /** Màu riêng theo mệnh dùng cho tiêu đề + từ khoá highlight trong 3 mục "Sắc màu bản mệnh",
     *  "Điểm xuyết đúng mệnh", "Hoạ tiết truyền đời" ở trang AI Styling. */
    @ColorRes
    public static int getMenhTitleColorRes(String menh) {
        if (menh == null) return R.color.tc_menh_kim;
        switch (menh) {
            case "Kim":  return R.color.tc_menh_kim;
            case "Mộc":  return R.color.tc_menh_moc;
            case "Thủy": return R.color.tc_menh_thuy;
            case "Hỏa":  return R.color.tc_menh_hoa;
            case "Thổ":  return R.color.tc_menh_tho;
            default:     return R.color.tc_menh_kim;
        }
    }

    /** Nền nhạt riêng theo mệnh — lớp nền phía sau mục "Sắc màu bản mệnh", "Điểm xuyết đúng mệnh",
     *  "Hoạ tiết truyền đời" và thẻ "Lời khuyên phong cách" ở trang AI Styling. */
    @ColorRes
    public static int getMenhPanelBgColorRes(String menh) {
        if (menh == null) return R.color.tc_menh_kim_bg;
        switch (menh) {
            case "Kim":  return R.color.tc_menh_kim_bg;
            case "Mộc":  return R.color.tc_menh_moc_bg;
            case "Thủy": return R.color.tc_menh_thuy_bg;
            case "Hỏa":  return R.color.tc_menh_hoa_bg;
            case "Thổ":  return R.color.tc_menh_tho_bg;
            default:     return R.color.tc_menh_kim_bg;
        }
    }

    /** Từ khoá cần highlight màu trong bài viết "Sắc màu bản mệnh", riêng theo từng mệnh. */
    public static String[] getMenhColorKeywords(Context ctx, String menh) {
        return ctx.getResources().getStringArray(colorKeywordsArrayRes(menh));
    }

    /** Từ khoá cần highlight màu trong bài viết "Điểm xuyết đúng mệnh", riêng theo từng mệnh. */
    public static String[] getMenhAccessoryKeywords(Context ctx, String menh) {
        return ctx.getResources().getStringArray(accessoryKeywordsArrayRes(menh));
    }

    /** Từ khoá cần highlight màu trong bài viết "Hoạ tiết truyền đời", riêng theo từng mệnh. */
    public static String[] getMenhPatternKeywords(Context ctx, String menh) {
        return ctx.getResources().getStringArray(patternKeywordsArrayRes(menh));
    }

    private static int colorKeywordsArrayRes(String menh) {
        if (menh == null) return R.array.aistyle_colors_keywords_kim;
        switch (menh) {
            case "Kim":  return R.array.aistyle_colors_keywords_kim;
            case "Mộc":  return R.array.aistyle_colors_keywords_moc;
            case "Thủy": return R.array.aistyle_colors_keywords_thuy;
            case "Hỏa":  return R.array.aistyle_colors_keywords_hoa;
            case "Thổ":  return R.array.aistyle_colors_keywords_tho;
            default:     return R.array.aistyle_colors_keywords_kim;
        }
    }

    private static int accessoryKeywordsArrayRes(String menh) {
        if (menh == null) return R.array.aistyle_accessories_keywords_kim;
        switch (menh) {
            case "Kim":  return R.array.aistyle_accessories_keywords_kim;
            case "Mộc":  return R.array.aistyle_accessories_keywords_moc;
            case "Thủy": return R.array.aistyle_accessories_keywords_thuy;
            case "Hỏa":  return R.array.aistyle_accessories_keywords_hoa;
            case "Thổ":  return R.array.aistyle_accessories_keywords_tho;
            default:     return R.array.aistyle_accessories_keywords_kim;
        }
    }

    private static int patternKeywordsArrayRes(String menh) {
        if (menh == null) return R.array.aistyle_patterns_keywords_kim;
        switch (menh) {
            case "Kim":  return R.array.aistyle_patterns_keywords_kim;
            case "Mộc":  return R.array.aistyle_patterns_keywords_moc;
            case "Thủy": return R.array.aistyle_patterns_keywords_thuy;
            case "Hỏa":  return R.array.aistyle_patterns_keywords_hoa;
            case "Thổ":  return R.array.aistyle_patterns_keywords_tho;
            default:     return R.array.aistyle_patterns_keywords_kim;
        }
    }

    /** Ảnh minh hoạ cho mục "Màu hợp mệnh" (hiển thị nguyên khổ, không cắt xén), riêng theo từng
     *  mệnh. */
    public static int[] getMenhColorPhotos(String menh) {
        if (menh == null) return new int[0];
        switch (menh) {
            case "Kim":  return new int[]{R.drawable.menh_kim_colors_1, R.drawable.menh_kim_colors_2};
            case "Mộc":  return new int[]{R.drawable.menh_moc_colors_1, R.drawable.menh_moc_colors_2};
            case "Thủy": return new int[]{R.drawable.menh_thuy_colors_1, R.drawable.menh_thuy_colors_2, R.drawable.menh_thuy_colors_3};
            case "Hỏa":  return new int[]{R.drawable.menh_hoa_colors_1, R.drawable.menh_hoa_colors_2};
            case "Thổ":  return new int[]{R.drawable.menh_tho_colors_1, R.drawable.menh_tho_colors_2};
            default:     return new int[0];
        }
    }

    /** Bài viết ngắn về sắc màu hợp mệnh, riêng theo từng mệnh. */
    public static String getMenhColorText(Context ctx, String menh) {
        if (menh == null) return "";
        switch (menh) {
            case "Kim":  return ctx.getString(R.string.aistyle_colors_text_kim);
            case "Mộc":  return ctx.getString(R.string.aistyle_colors_text_moc);
            case "Thủy": return ctx.getString(R.string.aistyle_colors_text_thuy);
            case "Hỏa":  return ctx.getString(R.string.aistyle_colors_text_hoa);
            case "Thổ":  return ctx.getString(R.string.aistyle_colors_text_tho);
            default:     return "";
        }
    }

    /** Lời khuyên phong cách tĩnh (offline) cho mục "Lời khuyên phong cách", riêng theo từng mệnh
     *  — dùng khi chưa cấu hình Gemini hoặc gọi API lỗi (xem AiStylingActivity#loadAiStylingTip). */
    public static String getMenhTipFallback(Context ctx, String menh) {
        if (menh == null) return ctx.getString(R.string.aistyle_tip_desc_kim);
        switch (menh) {
            case "Kim":  return ctx.getString(R.string.aistyle_tip_desc_kim);
            case "Mộc":  return ctx.getString(R.string.aistyle_tip_desc_moc);
            case "Thủy": return ctx.getString(R.string.aistyle_tip_desc_thuy);
            case "Hỏa":  return ctx.getString(R.string.aistyle_tip_desc_hoa);
            case "Thổ":  return ctx.getString(R.string.aistyle_tip_desc_tho);
            default:     return ctx.getString(R.string.aistyle_tip_desc_kim);
        }
    }

    /** Ảnh minh hoạ cho mục "Phụ kiện hợp mệnh", riêng theo từng mệnh. */
    public static int[] getMenhAccessoryPhotos(String menh) {
        if (menh == null) return new int[0];
        switch (menh) {
            case "Kim":  return new int[]{R.drawable.menh_kim_accessories_1, R.drawable.menh_kim_accessories_2, R.drawable.menh_kim_accessories_3, R.drawable.menh_kim_accessories_4, R.drawable.menh_kim_accessories_5};
            case "Mộc":  return new int[]{R.drawable.menh_moc_accessories_1, R.drawable.menh_moc_accessories_2, R.drawable.menh_moc_accessories_3, R.drawable.menh_moc_accessories_4, R.drawable.menh_moc_accessories_5};
            case "Thủy": return new int[]{R.drawable.menh_thuy_accessories_1, R.drawable.menh_thuy_accessories_2, R.drawable.menh_thuy_accessories_3, R.drawable.menh_thuy_accessories_4};
            case "Hỏa":  return new int[]{R.drawable.menh_hoa_accessories_1, R.drawable.menh_hoa_accessories_2, R.drawable.menh_hoa_accessories_3, R.drawable.menh_hoa_accessories_4, R.drawable.menh_hoa_accessories_5};
            case "Thổ":  return new int[]{R.drawable.menh_tho_accessories_1, R.drawable.menh_tho_accessories_2, R.drawable.menh_tho_accessories_3, R.drawable.menh_tho_accessories_4, R.drawable.menh_tho_accessories_5, R.drawable.menh_tho_accessories_6};
            default:     return new int[0];
        }
    }

    /** Bài viết ngắn về phụ kiện hợp mệnh, riêng theo từng mệnh. */
    public static String getMenhAccessoryText(Context ctx, String menh) {
        if (menh == null) return "";
        switch (menh) {
            case "Kim":  return ctx.getString(R.string.aistyle_accessories_text_kim);
            case "Mộc":  return ctx.getString(R.string.aistyle_accessories_text_moc);
            case "Thủy": return ctx.getString(R.string.aistyle_accessories_text_thuy);
            case "Hỏa":  return ctx.getString(R.string.aistyle_accessories_text_hoa);
            case "Thổ":  return ctx.getString(R.string.aistyle_accessories_text_tho);
            default:     return "";
        }
    }

    /** Ảnh minh hoạ cho mục "Hoạ tiết hợp mệnh", riêng theo từng mệnh. */
    public static int[] getMenhPatternPhotos(String menh) {
        if (menh == null) return new int[0];
        switch (menh) {
            case "Kim":  return new int[]{R.drawable.menh_kim_patterns_1, R.drawable.menh_kim_patterns_2, R.drawable.menh_kim_patterns_3, R.drawable.menh_kim_patterns_4, R.drawable.menh_kim_patterns_5};
            case "Mộc":  return new int[]{R.drawable.menh_moc_patterns_1, R.drawable.menh_moc_patterns_2, R.drawable.menh_moc_patterns_3, R.drawable.menh_moc_patterns_4, R.drawable.menh_moc_patterns_5, R.drawable.menh_moc_patterns_6, R.drawable.menh_moc_patterns_7};
            case "Thủy": return new int[]{R.drawable.menh_thuy_patterns_1, R.drawable.menh_thuy_patterns_2, R.drawable.menh_thuy_patterns_3, R.drawable.menh_thuy_patterns_4};
            case "Hỏa":  return new int[]{R.drawable.menh_hoa_patterns_1, R.drawable.menh_hoa_patterns_2, R.drawable.menh_hoa_patterns_3, R.drawable.menh_hoa_patterns_4, R.drawable.menh_hoa_patterns_5, R.drawable.menh_hoa_patterns_6, R.drawable.menh_hoa_patterns_7};
            case "Thổ":  return new int[]{R.drawable.menh_tho_patterns_1, R.drawable.menh_tho_patterns_2, R.drawable.menh_tho_patterns_3, R.drawable.menh_tho_patterns_4, R.drawable.menh_tho_patterns_5, R.drawable.menh_tho_patterns_6};
            default:     return new int[0];
        }
    }

    /** Bài viết ngắn về hoạ tiết hợp mệnh, riêng theo từng mệnh. */
    public static String getMenhPatternText(Context ctx, String menh) {
        if (menh == null) return "";
        switch (menh) {
            case "Kim":  return ctx.getString(R.string.aistyle_patterns_text_kim);
            case "Mộc":  return ctx.getString(R.string.aistyle_patterns_text_moc);
            case "Thủy": return ctx.getString(R.string.aistyle_patterns_text_thuy);
            case "Hỏa":  return ctx.getString(R.string.aistyle_patterns_text_hoa);
            case "Thổ":  return ctx.getString(R.string.aistyle_patterns_text_tho);
            default:     return "";
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
