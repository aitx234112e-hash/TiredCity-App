package com.tiredcity.app.data.repository;

import androidx.annotation.NonNull;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.tiredcity.app.utils.CheckoutPriceCalculator.Voucher;
import com.tiredcity.app.utils.CheckoutPriceCalculator.VoucherType;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Đọc mã giảm giá trực tiếp từ Firestore collection {@code vouchers} — CÙNG nguồn với
 * web-admin / app-admin. Nạp vào {@link com.tiredcity.app.utils.CheckoutPriceCalculator}
 * để màn Thanh toán dùng voucher admin cấu hình thay vì danh sách cứng.
 *
 * Field khớp schema admin (ModuleForm.VOUCHERS): code, type (percent|fixed|freeship),
 * value, minOrder, expiry (yyyy-MM-dd), description; thêm title/maxDiscount nếu có.
 * Mã hết hạn (expiry đã qua) sẽ bị loại.
 */
public class FirestoreVoucherRepository {

    public interface Callback { void onResult(List<Voucher> vouchers); }

    private static final String COLLECTION = "vouchers";

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    /** Tải voucher còn hạn; trả về {@code null} nếu lỗi (nơi gọi giữ danh sách fallback). */
    public void getVouchers(@NonNull Callback cb) {
        db.collection(COLLECTION).get()
                .addOnSuccessListener(snap -> {
                    List<Voucher> list = new ArrayList<>();
                    for (QueryDocumentSnapshot d : snap) {
                        Voucher v = map(d);
                        if (v != null) list.add(v);
                    }
                    cb.onResult(list);
                })
                .addOnFailureListener(e -> cb.onResult(null));
    }

    private static Voucher map(DocumentSnapshot d) {
        if (isExpired(str(d, "expiry"))) return null;

        String code = str(d, "code");
        if (code == null || code.trim().isEmpty()) code = d.getId();
        code = code.trim().toUpperCase(Locale.ROOT);

        String title = str(d, "title");
        if (title == null || title.trim().isEmpty()) title = code;
        String description = str(d, "description");
        if (description == null) description = "";

        VoucherType type = mapType(str(d, "type"));
        double value = num(d, "value");
        double maxDiscount = num(d, "maxDiscount");   // 0 = không giới hạn
        double minSpend = num(d, "minOrder");

        return new Voucher(code, title, description, type, value, maxDiscount, minSpend);
    }

    private static VoucherType mapType(String t) {
        if (t == null) return VoucherType.FLAT;
        switch (t.trim().toLowerCase(Locale.ROOT)) {
            case "percent":  return VoucherType.PERCENT;
            case "freeship": return VoucherType.FREE_SHIP;
            case "fixed":
            default:         return VoucherType.FLAT;
        }
    }

    /** Hết hạn nếu qua hết NGÀY expiry (cộng 1 ngày để còn dùng trong ngày cuối). */
    private static boolean isExpired(String expiry) {
        if (expiry == null || expiry.isEmpty()) return false;
        try {
            String s = expiry.length() >= 10 ? expiry.substring(0, 10) : expiry;
            long end = new SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(s).getTime();
            return end + 86_400_000L < System.currentTimeMillis();
        } catch (Exception e) {
            return false;
        }
    }

    private static String str(DocumentSnapshot d, String key) {
        Object v = d.get(key);
        return v != null ? v.toString() : null;
    }

    private static double num(DocumentSnapshot d, String key) {
        Object v = d.get(key);
        return v instanceof Number ? ((Number) v).doubleValue() : 0;
    }
}
