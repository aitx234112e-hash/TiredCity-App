package com.tiredcity.app.data.repository;

import androidx.annotation.NonNull;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.tiredcity.app.data.model.Product;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Đọc sản phẩm trực tiếp từ Firestore collection {@code products} — CÙNG nguồn với
 * web-admin và app-admin (project {@code tiredcity-daf1e}). Dùng khi REST backend
 * ({@code tiredcity.vn/api}) chưa sẵn sàng, để app khách hiển thị đúng sản phẩm và
 * ảnh giống hệt bên admin thay vì dữ liệu mẫu ({@link com.tiredcity.app.data.mock.MockProductCatalog})
 * vốn không có ảnh.
 */
public class FirestoreProductRepository {

    public interface Callback { void onResult(List<Product> products); }
    public interface SingleCallback { void onResult(Product product); }

    private static final String COLLECTION = "products";

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    /** Tải toàn bộ sản phẩm; trả về {@code null} nếu lỗi (để nơi gọi fallback về mock). */
    public void getProducts(@NonNull Callback cb) {
        db.collection(COLLECTION).get()
                .addOnSuccessListener(snap -> {
                    List<Product> list = new ArrayList<>();
                    for (QueryDocumentSnapshot d : snap) list.add(map(d));
                    cb.onResult(list);
                })
                .addOnFailureListener(e -> cb.onResult(null));
    }

    /** Tải một sản phẩm theo document id; trả về {@code null} nếu không có/lỗi. */
    public void getProductById(String id, @NonNull SingleCallback cb) {
        if (id == null || id.trim().isEmpty()) { cb.onResult(null); return; }
        db.collection(COLLECTION).document(id).get()
                .addOnSuccessListener(d -> cb.onResult(d != null && d.exists() ? map(d) : null))
                .addOnFailureListener(e -> cb.onResult(null));
    }

    // ── Ánh xạ Firestore doc → Product model của app khách ──────────────────────

    public static Product map(DocumentSnapshot d) {
        Product p = new Product();
        p.setId(d.getId());
        p.setName(str(d, "product_name"));
        p.setCategory(categoryLabel(str(d, "product_dept")));
        p.setMaterial(str(d, "material"));
        p.setOrigin(str(d, "origin"));

        String desc = str(d, "description");
        if (desc != null) p.setDescription(desc);

        p.setPrice(num(d, "unit_price"));
        p.setRating(num(d, "rating"));
        p.setDiscount((int) num(d, "discount"));
        p.setStock((int) num(d, "stock"));

        // Ảnh: mảng URL http(s) (hoặc data-URL base64) — giữ nguyên cho Glide tải.
        Object imgs = d.get("images");
        if (imgs instanceof List) {
            List<String> images = new ArrayList<>();
            for (Object o : (List<?>) imgs) {
                if (o != null && !o.toString().trim().isEmpty()) images.add(o.toString());
            }
            if (!images.isEmpty()) p.setImages(images);
        }

        // Trang phục: tồn kho = tổng số lượng các size.
        Object sizes = d.get("sizes");
        if (sizes instanceof List && !((List<?>) sizes).isEmpty()) {
            int total = 0;
            for (Object o : (List<?>) sizes) {
                if (o instanceof Map) {
                    Object q = ((Map<?, ?>) o).get("quantity");
                    if (q instanceof Number) total += ((Number) q).intValue();
                }
            }
            if (total > 0) p.setStock(total);
        }

        // Màu để lọc danh mục con: ưu tiên field "color" do admin chọn (chuẩn, có
        // thể lọc chắc chắn). Sản phẩm cũ chưa có field này thì fallback: lấy cụm đầu
        // của description ("Trắng. Chất liệu:...") làm màu như trước đây.
        String color = str(d, "color");
        if (color == null || color.trim().isEmpty()) color = colorFromDescription(desc);
        if (color != null && !color.trim().isEmpty()) {
            p.setColors(new ArrayList<>(Arrays.asList(color.trim())));
        }

        return p;
    }

    private static String colorFromDescription(String desc) {
        if (desc == null) return null;
        String first = desc.split("\\.")[0].trim();
        return first.isEmpty() ? null : first;
    }

    /** Quy đổi mã danh mục Firestore sang nhãn danh mục app khách dùng để lọc lưới. */
    private static String categoryLabel(String dept) {
        if (dept == null) return null;
        switch (dept) {
            case "ao-dai":    return "ÁO DÀI";
            case "nhat-binh": return "NHẬT BÌNH";
            case "ao-tac":    return "ÁO TẤC";
            case "giao-linh": return "GIAO LĨNH";
            case "yem-dao":   return "YẾM ĐÀO";
            case "phu-kien":  return "PHỤ KIỆN";
            case "viet-phuc": return "VIỆT PHỤC";
            default:          return dept;
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
