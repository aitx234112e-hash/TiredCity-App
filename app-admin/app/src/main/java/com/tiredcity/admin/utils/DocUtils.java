package com.tiredcity.admin.utils;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Cac ham tien ich doc du lieu tu DocumentSnapshot cua Firestore.
 * Vi cac collection ben web dung nhieu ten truong fallback (vd: totalPrice || total || amount)
 * nen o day doc "mem": thu lan luot cac key va tra ve gia tri dau tien co.
 */
public final class DocUtils {
    private DocUtils() {}

    private static final Locale VN = new Locale("vi", "VN");
    private static final SimpleDateFormat DATE_FMT = new SimpleDateFormat("dd/MM/yyyy", VN);

    /** Chuoi dau tien khac rong trong danh sach key. */
    public static String str(DocumentSnapshot d, String... keys) {
        for (String k : keys) {
            Object v = d.get(k);
            if (v != null) {
                String s = String.valueOf(v).trim();
                if (!s.isEmpty() && !"null".equals(s)) return s;
            }
        }
        return "";
    }

    /** So dau tien doc duoc trong danh sach key (0 neu khong co). */
    public static double num(DocumentSnapshot d, String... keys) {
        for (String k : keys) {
            Object v = d.get(k);
            if (v instanceof Number) return ((Number) v).doubleValue();
            if (v instanceof String) {
                try {
                    return Double.parseDouble(((String) v).replaceAll("[^0-9.\\-]", ""));
                } catch (NumberFormatException ignored) {}
            }
        }
        return 0d;
    }

    /** Dinh dang tien VND: 1.250.000 ₫ */
    public static String money(double value) {
        NumberFormat nf = NumberFormat.getInstance(VN);
        nf.setMaximumFractionDigits(0);
        return nf.format(value) + " ₫";
    }

    /** Rut gon so lon: 1.2M, 850K... dung cho the thong ke. */
    public static String compact(double value) {
        if (value >= 1_000_000_000d) return trim(value / 1_000_000_000d) + "B";
        if (value >= 1_000_000d) return trim(value / 1_000_000d) + "M";
        if (value >= 1_000d) return trim(value / 1_000d) + "K";
        return String.valueOf((long) value);
    }

    private static String trim(double v) {
        if (v == Math.floor(v)) return String.valueOf((long) v);
        return String.format(Locale.US, "%.1f", v);
    }

    /** Dinh dang ngay dd/MM/yyyy tu Timestamp / Date / Long(millis) / String ISO. */
    public static String date(DocumentSnapshot d, String... keys) {
        for (String k : keys) {
            Object v = d.get(k);
            String out = dateOf(v);
            if (out != null) return out;
        }
        return "";
    }

    /** Parse chuoi ngay (ISO "yyyy-MM-dd..." hoac "yyyy-MM-dd") -> millis; 0 neu khong hop le. */
    public static long millisFromIso(String s) {
        if (s == null || s.isEmpty()) return 0L;
        for (String pat : new String[]{"yyyy-MM-dd'T'HH:mm:ss", "yyyy-MM-dd HH:mm:ss", "yyyy-MM-dd"}) {
            try {
                return new SimpleDateFormat(pat, VN).parse(s.length() > 19 ? s.substring(0, 19) : s).getTime();
            } catch (Exception ignored) {}
        }
        return 0L;
    }

    /** Moc thoi gian (millis) de sap xep client-side; 0 neu khong doc duoc. */
    public static long millis(DocumentSnapshot d, String... keys) {
        for (String k : keys) {
            Object v = d.get(k);
            if (v instanceof Timestamp) return ((Timestamp) v).toDate().getTime();
            if (v instanceof Date) return ((Date) v).getTime();
            if (v instanceof Number) return ((Number) v).longValue();
            if (v instanceof String) {
                String s = ((String) v).trim();
                if (!s.isEmpty()) {
                    for (String pat : new String[]{"yyyy-MM-dd'T'HH:mm:ss", "yyyy-MM-dd HH:mm:ss", "yyyy-MM-dd", "dd/MM/yyyy"}) {
                        try {
                            return new SimpleDateFormat(pat, VN).parse(s.length() > 19 ? s.substring(0, 19) : s).getTime();
                        } catch (Exception ignored) {}
                    }
                }
            }
        }
        return 0L;
    }

    private static String dateOf(Object v) {
        if (v == null) return null;
        try {
            if (v instanceof Timestamp) return DATE_FMT.format(((Timestamp) v).toDate());
            if (v instanceof Date) return DATE_FMT.format((Date) v);
            if (v instanceof Number) return DATE_FMT.format(new Date(((Number) v).longValue()));
            if (v instanceof String) {
                String s = ((String) v).trim();
                if (s.isEmpty()) return null;
                // ISO 2024-01-31T... -> lay phan ngay
                if (s.length() >= 10 && s.charAt(4) == '-') {
                    String[] p = s.substring(0, 10).split("-");
                    if (p.length == 3) return p[2] + "/" + p[1] + "/" + p[0];
                }
                return s;
            }
        } catch (Exception ignored) {}
        return null;
    }
}
