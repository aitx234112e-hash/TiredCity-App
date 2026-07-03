package com.tiredcity.app.data.mock;

import android.content.Context;
import com.tiredcity.app.R;
import com.tiredcity.app.data.model.Product;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Dữ liệu Việt phục mẫu dùng cho xem trước offline khi chưa có API/backend trả về
 * (xem {@link com.tiredcity.app.ui.shop.CategoryActivity}). Dùng chung một nguồn duy nhất
 * để {@link com.tiredcity.app.ui.shop.ProductDetailActivity} có thể tra cứu lại đúng sản phẩm
 * bằng id khi backend không nhận diện được id mẫu (xem {@link #findById(Context, String)}).
 * Toàn bộ nội dung hiển thị (tên, chất liệu, mô tả,…) lấy từ string resource để đổi được
 * ngôn ngữ Anh/Việt — chỉ id và các khoá phân loại (danh mục, nhóm màu) giữ nguyên tiếng Việt.
 */
public final class MockProductCatalog {

    private static final String[] ALL_CATEGORY_IDS = {
            "ÁO DÀI", "NHẬT BÌNH", "ÁO TẤC", "GIAO LĨNH", "YẾM ĐÀO", "PHỤ KIỆN"
    };

    private MockProductCatalog() {}

    public static List<Product> getProducts(Context context, String categoryId) {
        List<Product> list;
        if ("ÁO DÀI".equals(categoryId))         list = buildAoDai(context);
        else if ("NHẬT BÌNH".equals(categoryId)) list = buildNhatBinh(context);
        else if ("ÁO TẤC".equals(categoryId))    list = buildAoTac(context);
        else if ("GIAO LĨNH".equals(categoryId)) list = buildGiaoLinh(context);
        else if ("YẾM ĐÀO".equals(categoryId))   list = buildYemDao(context);
        else if ("PHỤ KIỆN".equals(categoryId))  list = buildPhuKien(context);
        else                                      list = buildGeneric(context);

        for (Product p : list) applyGenericDetail(context, p);
        return list;
    }

    /** Tìm một sản phẩm mẫu theo id, quét qua toàn bộ danh mục. Trả về null nếu không có. */
    public static Product findById(Context context, String id) {
        if (id == null) return null;
        for (String categoryId : ALL_CATEGORY_IDS) {
            for (Product p : getProducts(context, categoryId)) {
                if (id.equals(p.getId())) return p;
            }
        }
        for (Product p : buildGeneric(context)) {
            if (id.equals(p.getId())) return p;
        }
        for (Product p : getHomeHighlights(context, true)) {
            if (id.equals(p.getId())) return p;
        }
        for (Product p : getHomeHighlights(context, false)) {
            if (id.equals(p.getId())) return p;
        }
        return null;
    }

    /**
     * Sản phẩm mẫu cho khối "Gợi ý cho bạn" (recommended = true) / "Đang thịnh hành"
     * (recommended = false) ở màn Trang chủ. Id đặt tiền tố riêng để không trùng với
     * id sản phẩm theo danh mục ở trên khi {@link #findById(Context, String)} tra cứu.
     */
    public static List<Product> getHomeHighlights(Context context, boolean recommended) {
        String prefix = recommended ? "home-r" : "home-h";
        Object[][] data = {
            {"1", R.string.mock_home1_name, R.string.material_lua_to_tam,  850000.0,  10, 4.8},
            {"2", R.string.mock_home2_name, R.string.material_gam_theu,    1200000.0, 15, 4.9},
            {"3", R.string.mock_home3_name, R.string.material_dui_to,      650000.0,  0,  4.5},
            {"4", R.string.mock_home4_name, R.string.material_lua_mem,     250000.0,  5,  4.3},
            {"5", R.string.mock_home5_name, R.string.material_lua_cao_cap, 2500000.0, 20, 5.0},
        };

        List<Product> list = new ArrayList<>();
        for (Object[] row : data) {
            Product p = new Product();
            p.setId(prefix + row[0]);
            p.setName(context.getString((int) row[1]));
            p.setMaterial(context.getString((int) row[2]));
            p.setPrice((double) row[3]);
            p.setDiscount((int) row[4]);
            p.setRating((double) row[5]);
            p.setStock(20);
            p.setNew(recommended);
            applyGenericDetail(context, p);
            list.add(p);
        }
        return list;
    }

    // ── Áo Dài ────────────────────────────────────────────────────────────────

    private static List<Product> buildAoDai(Context ctx) {
        List<Product> list = new ArrayList<>();
        Product khoiTrang = mockProduct(ctx, "ad1", R.string.mock_ad1_name, "ÁO DÀI", 2890000, "Trắng");
        khoiTrang.setDescription(ctx.getString(R.string.desc_ad1));
        khoiTrang.setStory(ctx.getString(R.string.story_ad1));
        khoiTrang.setCareInstructions(Arrays.asList(
                ctx.getString(R.string.care_hand_wash_cold_strict),
                ctx.getString(R.string.care_no_bleach),
                ctx.getString(R.string.care_iron_low),
                ctx.getString(R.string.care_store_dry)));
        khoiTrang.setSpecifications(specs(ctx,
                ctx.getString(R.string.color_bucket_trang),
                ctx.getString(R.string.mock_style_ao_dai_truyen_thong),
                ctx.getString(R.string.spec_occasion_default),
                ctx.getString(R.string.default_origin)));
        khoiTrang.setStock(12);
        list.add(khoiTrang);

        list.add(mockProduct(ctx, "ad2", R.string.mock_ad2_name, "ÁO DÀI", 1590000, "Xanh"));
        list.add(mockProduct(ctx, "ad3", R.string.mock_ad3_name, "ÁO DÀI", 1750000, "Vàng"));
        list.add(mockProduct(ctx, "ad4", R.string.mock_ad4_name, "ÁO DÀI", 1290000, "Hồng"));
        list.add(mockProduct(ctx, "ad5", R.string.mock_ad5_name, "ÁO DÀI", 1450000, "Xanh", "Hồng"));
        list.add(mockProduct(ctx, "ad6", R.string.mock_ad6_name, "ÁO DÀI", 1350000, "Hồng"));

        Product phanHoa = mockProduct(ctx, "ad7", R.string.mock_ad7_name, "ÁO DÀI", 1200000, "Trắng");
        phanHoa.setDescription(ctx.getString(R.string.desc_ad7));
        phanHoa.setStock(0);
        list.add(phanHoa);

        return list;
    }

    // ── Nhật Bình ────────────────────────────────────────────────────────────

    private static List<Product> buildNhatBinh(Context ctx) {
        List<Product> list = new ArrayList<>();
        list.add(mockProduct(ctx, "nb1",  R.string.mock_nb1_name,  "NHẬT BÌNH", 3490000, "Trắng"));
        list.add(mockProduct(ctx, "nb2",  R.string.mock_nb2_name,  "NHẬT BÌNH", 3290000, "Xanh lá"));
        list.add(mockProduct(ctx, "nb3",  R.string.mock_nb3_name,  "NHẬT BÌNH", 2890000, "Xanh lá"));
        list.add(mockProduct(ctx, "nb4",  R.string.mock_nb4_name,  "NHẬT BÌNH", 2190000, "Xanh"));
        list.add(mockProduct(ctx, "nb5",  R.string.mock_nb5_name,  "NHẬT BÌNH", 2690000, "Đỏ"));
        list.add(mockProduct(ctx, "nb6",  R.string.mock_nb6_name,  "NHẬT BÌNH", 2990000, "Xanh"));
        list.add(mockProduct(ctx, "nb7",  R.string.mock_nb7_name,  "NHẬT BÌNH", 2750000, "Xanh lá"));
        list.add(mockProduct(ctx, "nb8",  R.string.mock_nb8_name,  "NHẬT BÌNH", 2450000, "Vàng"));
        list.add(mockProduct(ctx, "nb9",  R.string.mock_nb9_name,  "NHẬT BÌNH", 2150000, "Vàng"));
        list.add(mockProduct(ctx, "nb10", R.string.mock_nb10_name, "NHẬT BÌNH", 1890000, "Vàng"));
        return list;
    }

    // ── Áo Tấc ───────────────────────────────────────────────────────────────

    private static List<Product> buildAoTac(Context ctx) {
        List<Product> list = new ArrayList<>();
        list.add(mockProduct(ctx, "at1", R.string.mock_at1_name, "ÁO TẤC", 1590000, "Xanh"));
        list.add(mockProduct(ctx, "at2", R.string.mock_at2_name, "ÁO TẤC", 1890000, "Trắng"));
        list.add(mockProduct(ctx, "at3", R.string.mock_at3_name, "ÁO TẤC", 1290000, "Cam"));
        list.add(mockProduct(ctx, "at4", R.string.mock_at4_name, "ÁO TẤC", 1450000, "Xanh lá"));
        list.add(mockProduct(ctx, "at5", R.string.mock_at5_name, "ÁO TẤC", 1690000, "Xanh"));
        list.add(mockProduct(ctx, "at6", R.string.mock_at6_name, "ÁO TẤC", 1750000, "Trắng"));
        return list;
    }

    // ── Giao Lĩnh ────────────────────────────────────────────────────────────

    private static List<Product> buildGiaoLinh(Context ctx) {
        List<Product> list = new ArrayList<>();
        list.add(mockProduct(ctx, "gl1", R.string.mock_gl1_name, "GIAO LĨNH", 2290000, "Xanh lá"));
        list.add(mockProduct(ctx, "gl2", R.string.mock_gl2_name, "GIAO LĨNH", 2490000, "Xanh lá"));
        list.add(mockProduct(ctx, "gl3", R.string.mock_gl3_name, "GIAO LĨNH", 2450000, "Xanh lá"));
        list.add(mockProduct(ctx, "gl4", R.string.mock_gl4_name, "GIAO LĨNH", 2290000, "Vàng"));
        list.add(mockProduct(ctx, "gl5", R.string.mock_gl5_name, "GIAO LĨNH", 1890000, "Đỏ"));
        list.add(mockProduct(ctx, "gl6", R.string.mock_gl6_name, "GIAO LĨNH", 2690000, "Đỏ"));
        return list;
    }

    // ── Yếm Đào ──────────────────────────────────────────────────────────────

    private static List<Product> buildYemDao(Context ctx) {
        List<Product> list = new ArrayList<>();
        list.add(mockProduct(ctx, "yd1", R.string.mock_yd1_name, "YẾM ĐÀO", 2290000, "Đỏ"));
        list.add(mockProduct(ctx, "yd2", R.string.mock_yd2_name, "YẾM ĐÀO", 2690000, "Xanh lá"));
        list.add(mockProduct(ctx, "yd3", R.string.mock_yd3_name, "YẾM ĐÀO", 1990000, "Vàng", "Đỏ"));
        list.add(mockProduct(ctx, "yd4", R.string.mock_yd4_name, "YẾM ĐÀO", 2350000, "Xanh lá"));
        list.add(mockProduct(ctx, "yd5", R.string.mock_yd5_name, "YẾM ĐÀO", 2450000, "Vàng"));
        list.add(mockProduct(ctx, "yd6", R.string.mock_yd6_name, "YẾM ĐÀO", 2890000, "Hồng", "Xanh lá"));
        return list;
    }

    // ── Phụ kiện ─────────────────────────────────────────────────────────────

    private static List<Product> buildPhuKien(Context ctx) {
        List<Product> list = new ArrayList<>();
        list.add(mockProduct(ctx, "pk1", R.string.mock_pk1_name, "PHỤ KIỆN", 350000, "Khăn đội đầu"));
        list.add(mockProduct(ctx, "pk2", R.string.mock_pk2_name, "PHỤ KIỆN", 80000,  "Mũ đội đầu"));
        list.add(mockProduct(ctx, "pk3", R.string.mock_pk3_name, "PHỤ KIỆN", 420000, "Quạt"));
        list.add(mockProduct(ctx, "pk4", R.string.mock_pk4_name, "PHỤ KIỆN", 650000, "Ô che"));
        list.add(mockProduct(ctx, "pk5", R.string.mock_pk5_name, "PHỤ KIỆN", 890000, "Khăn đội đầu"));
        list.add(mockProduct(ctx, "pk6", R.string.mock_pk6_name, "PHỤ KIỆN", 120000, "Ô che"));
        list.add(mockProduct(ctx, "pk7", R.string.mock_pk7_name, "PHỤ KIỆN", 990000, "Mũ đội đầu"));
        list.add(mockProduct(ctx, "pk8", R.string.mock_pk8_name, "PHỤ KIỆN", 750000, "Mũ đội đầu"));
        list.add(mockProduct(ctx, "pk9", R.string.mock_pk9_name, "PHỤ KIỆN", 550000, "Ô che"));
        return list;
    }

    // ── Việt phục chung (fallback khi danh mục không khớp 6 nhóm trên) ─────────

    private static List<Product> buildGeneric(Context ctx) {
        Object[][] data = {
            {"1", R.string.mock_generic1_name, R.string.material_gam_det_hoa,  1250000.0, 15, 4.8},
            {"2", R.string.mock_generic2_name, R.string.material_dui_to_tam,   980000.0,  10, 4.7},
            {"3", R.string.mock_generic3_name, R.string.material_lua_ha_dong,  850000.0,  0,  4.6},
            {"4", R.string.mock_generic4_name, R.string.material_gam_theu_chi, 320000.0,  20, 4.9},
            {"5", R.string.mock_generic5_name, R.string.material_to_song,      1450000.0, 12, 4.8},
            {"6", R.string.mock_generic6_name, R.string.material_gam_cao_cap,  2350000.0, 18, 5.0},
        };

        List<Product> list = new ArrayList<>();
        for (Object[] row : data) {
            Product p = new Product();
            p.setId((String) row[0]);
            p.setName(ctx.getString((int) row[1]));
            p.setMaterial(ctx.getString((int) row[2]));
            p.setPrice((double) row[3]);
            p.setDiscount((int) row[4]);
            p.setRating((double) row[5]);
            p.setStock(20);
            applyGenericDetail(ctx, p);
            list.add(p);
        }
        return list;
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private static Product mockProduct(Context ctx, String id, int nameRes, String category, double price, String... colors) {
        Product p = new Product();
        p.setId(id);
        p.setName(ctx.getString(nameRes));
        p.setCategory(category);
        p.setColors(Arrays.asList(colors));
        p.setMaterial(ctx.getString("PHỤ KIỆN".equals(category) ? R.string.material_theu_tay : R.string.material_lua_to_tam));
        p.setPrice(price);
        p.setRating(4.7);
        p.setStock(20);
        return p;
    }

    private static Map<String, String> specs(Context ctx, String color, String style, String occasion, String origin) {
        Map<String, String> spec = new LinkedHashMap<>();
        spec.put(ctx.getString(R.string.label_colour), color);
        spec.put(ctx.getString(R.string.label_style), style);
        spec.put(ctx.getString(R.string.label_occasion), occasion);
        spec.put(ctx.getString(R.string.label_origin), origin);
        return spec;
    }

    /** Điền phần mô tả/câu chuyện/bảo quản/thông số còn thiếu bằng nội dung hợp lý theo danh mục. */
    private static void applyGenericDetail(Context ctx, Product p) {
        if (p.getOrigin() == null) p.setOrigin(ctx.getString(R.string.default_origin));

        if (p.getDescription() == null) {
            p.setDescription(ctx.getString(R.string.desc_template_default,
                    p.getName(), p.getMaterial().toLowerCase(Locale.getDefault())));
        }
        if (p.getStory() == null) {
            p.setStory(ctx.getString(R.string.story_template_default));
        }
        if (p.getCareInstructions() == null) {
            p.setCareInstructions(Arrays.asList(
                    ctx.getString(R.string.care_hand_wash_cold_gentle),
                    ctx.getString(R.string.care_no_bleach),
                    ctx.getString(R.string.care_iron_low_cloth),
                    ctx.getString(R.string.care_store_dry)));
        }
        if (p.getSpecifications() == null || p.getSpecifications().isEmpty()) {
            String color = (p.getColors() != null && !p.getColors().isEmpty())
                    ? String.join(", ", p.getColors()) : ctx.getString(R.string.spec_color_various);
            String style = "PHỤ KIỆN".equals(p.getCategory()) ? ctx.getString(R.string.spec_style_accessory)
                    : (p.getCategory() != null ? p.getCategory() : ctx.getString(R.string.spec_style_traditional));
            p.setSpecifications(specs(ctx, color, style, ctx.getString(R.string.spec_occasion_default), p.getOrigin()));
        }
    }
}
