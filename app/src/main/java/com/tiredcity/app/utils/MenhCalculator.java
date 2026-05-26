package com.tiredcity.app.utils;

public class MenhCalculator {
    private MenhCalculator() {}

    /**
     * Tính Mệnh Ngũ Hành từ năm sinh dương lịch.
     * Quy tắc phổ biến: chữ số cuối của năm.
     *   0,1 → Thủy  |  2,3 → Mộc  |  4,5 → Hỏa  |  6,7 → Thổ  |  8,9 → Kim
     */
    public static String tinhMenh(int namSinh) {
        switch (namSinh % 10) {
            case 0: case 1: return Constants.MENH_THUY;
            case 2: case 3: return Constants.MENH_MOC;
            case 4: case 5: return Constants.MENH_HOA;
            case 6: case 7: return Constants.MENH_THO;
            case 8: case 9: return Constants.MENH_KIM;
            default:        return Constants.MENH_THO;
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
