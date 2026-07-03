package com.tiredcity.app.utils;

import android.content.Context;
import androidx.annotation.StringRes;
import com.tiredcity.app.R;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Chuẩn hoá các tên màu tự do trong dữ liệu sản phẩm (vd "Xanh lam đậm",
 * "Đỏ thẫm hoa văn vàng kim", "Xanh - Hồng") về các nhóm màu chuẩn dùng cho
 * danh mục con theo màu của từng loại trang phục.
 */
public final class ColorTaxonomy {

    public static final String DO      = "Đỏ";
    public static final String XANH    = "Xanh";
    public static final String VANG    = "Vàng";
    public static final String TRANG   = "Trắng";
    public static final String DEN     = "Đen";
    public static final String HONG    = "Hồng";
    public static final String TIM     = "Tím";
    public static final String XANH_LA = "Xanh lá";
    public static final String CAM     = "Cam";

    /** Thứ tự hiển thị trên thanh danh mục màu. */
    public static final String[] CANONICAL_ORDER = {
            DO, XANH, VANG, TRANG, DEN, HONG, TIM, XANH_LA, CAM
    };

    private ColorTaxonomy() {}

    /**
     * Quy đổi một chuỗi màu thô (có thể ghép nhiều màu bằng "-", "/", ",")
     * thành danh sách các nhóm màu chuẩn mà nó thuộc về.
     */
    public static List<String> normalize(String rawColor) {
        List<String> buckets = new ArrayList<>();
        if (rawColor == null || rawColor.trim().isEmpty()) return buckets;

        for (String token : rawColor.split("[-/,]")) {
            collectBuckets(token.trim(), buckets);
        }
        return buckets;
    }

    private static void collectBuckets(String token, List<String> out) {
        String t = token.toLowerCase(Locale.ROOT);
        if (t.isEmpty()) return;

        // "Xanh lá / rêu / lục / bạc hà" phải khớp trước "xanh" nói chung.
        if (t.contains("xanh lá") || t.contains("xanh la") || t.contains("xanh rêu")
                || t.contains("xanh reu") || t.contains("xanh lục") || t.contains("xanh luc")
                || t.contains("bạc hà") || t.contains("bac ha")) {
            addOnce(out, XANH_LA);
        } else if (t.contains("xanh")) {
            addOnce(out, XANH); // lam, lam đậm, navy,…
        }
        if (t.contains("đỏ") || t.contains("do")) addOnce(out, DO);
        if (t.contains("vàng") || t.contains("vang")) addOnce(out, VANG);
        if (t.contains("trắng") || t.contains("trang")) addOnce(out, TRANG);
        if (t.contains("đen") || t.contains("den")) addOnce(out, DEN);
        if (t.contains("hồng") || t.contains("hong")) addOnce(out, HONG);
        if (t.contains("tím") || t.contains("tim")) addOnce(out, TIM);
        if (t.contains("cam")) addOnce(out, CAM);
    }

    private static void addOnce(List<String> list, String bucket) {
        if (!list.contains(bucket)) list.add(bucket);
    }

    /** True nếu bất kỳ màu nào của sản phẩm thuộc nhóm màu chỉ định. */
    public static boolean matchesBucket(List<String> productColors, String bucket) {
        if (productColors == null || bucket == null) return false;
        for (String raw : productColors) {
            if (normalize(raw).contains(bucket)) return true;
        }
        return false;
    }

    /** Nhãn màu ngắn gọn để hiển thị trên thẻ sản phẩm (nhóm màu đầu tiên tìm được). */
    public static String primaryTag(List<String> productColors) {
        if (productColors == null) return null;
        for (String raw : productColors) {
            List<String> buckets = normalize(raw);
            if (!buckets.isEmpty()) return buckets.get(0);
        }
        return null;
    }

    /**
     * String resource cho nhãn hiển thị của một nhóm màu — tách riêng khỏi hằng số khoá
     * (DO/XANH/VANG/…) vốn phải giữ nguyên tiếng Việt để khớp với dữ liệu sản phẩm.
     * Trả về 0 nếu {@code bucket} không thuộc bảng màu chuẩn.
     */
    @StringRes
    public static int displayNameRes(String bucket) {
        if (DO.equals(bucket))      return R.string.color_bucket_do;
        if (XANH.equals(bucket))    return R.string.color_bucket_xanh;
        if (VANG.equals(bucket))    return R.string.color_bucket_vang;
        if (TRANG.equals(bucket))   return R.string.color_bucket_trang;
        if (DEN.equals(bucket))     return R.string.color_bucket_den;
        if (HONG.equals(bucket))    return R.string.color_bucket_hong;
        if (TIM.equals(bucket))     return R.string.color_bucket_tim;
        if (XANH_LA.equals(bucket)) return R.string.color_bucket_xanh_la;
        if (CAM.equals(bucket))     return R.string.color_bucket_cam;
        return 0;
    }

    /** Nhãn màu đã dịch theo ngôn ngữ hiện tại; trả về nguyên văn {@code bucket} nếu không nhận diện được. */
    public static String displayName(Context context, String bucket) {
        int res = displayNameRes(bucket);
        return res != 0 ? context.getString(res) : bucket;
    }
}
