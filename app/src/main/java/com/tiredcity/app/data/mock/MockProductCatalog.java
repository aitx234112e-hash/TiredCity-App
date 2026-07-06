package com.tiredcity.app.data.mock;

import android.content.Context;
import com.tiredcity.app.R;
import com.tiredcity.app.data.model.Product;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
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

    // Ảnh trang phục THẬT (trích từ thư mục TRANG PHỤC, xem StylingFragment.CAROUSEL_IMAGES) —
    // gán xoay vòng cho sản phẩm mẫu theo đúng danh mục để "Có thể bạn cũng thích" không còn ô
    // ảnh trống (dữ liệu mẫu trước đây không có images[] nên ProductAdapter không có gì để tải).
    private static final String[] IMAGES_AO_DAI    = {"carousel_aodai_1", "carousel_aodai_2", "carousel_aodai_3", "carousel_aodai_4", "carousel_aodai_5"};
    private static final String[] IMAGES_NHAT_BINH = {"carousel_nhatbinh_1", "carousel_nhatbinh_2", "carousel_nhatbinh_3", "carousel_nhatbinh_4", "carousel_nhatbinh_5"};
    private static final String[] IMAGES_AO_TAC    = {"carousel_aotac_1", "carousel_aotac_2", "carousel_aotac_3", "carousel_aotac_4", "carousel_aotac_5"};
    private static final String[] IMAGES_GIAO_LINH = {"carousel_giaolinh_1", "carousel_giaolinh_2", "carousel_giaolinh_3", "carousel_giaolinh_4", "carousel_giaolinh_5"};
    private static final String[] IMAGES_YEM_DAO   = {"carousel_yemdao_1", "carousel_yemdao_2", "carousel_yemdao_3", "carousel_yemdao_4", "carousel_yemdao_5"};
    private static final String[] IMAGES_PHU_KIEN  = {"carousel_phukien_1", "carousel_phukien_2", "carousel_phukien_3", "carousel_phukien_4", "carousel_phukien_5"};

    private MockProductCatalog() {}

    public static List<Product> getProducts(Context context, String categoryId) {
        List<Product> list;
        String[] imagePool;
        if ("ÁO DÀI".equals(categoryId))         { list = buildAoDai(context);    imagePool = IMAGES_AO_DAI; }
        else if ("NHẬT BÌNH".equals(categoryId)) { list = buildNhatBinh(context); imagePool = IMAGES_NHAT_BINH; }
        else if ("ÁO TẤC".equals(categoryId))    { list = buildAoTac(context);    imagePool = IMAGES_AO_TAC; }
        else if ("GIAO LĨNH".equals(categoryId)) { list = buildGiaoLinh(context); imagePool = IMAGES_GIAO_LINH; }
        else if ("YẾM ĐÀO".equals(categoryId))   { list = buildYemDao(context);   imagePool = IMAGES_YEM_DAO; }
        else if ("PHỤ KIỆN".equals(categoryId))  { list = buildPhuKien(context);  imagePool = IMAGES_PHU_KIEN; }
        else                                      { list = buildGeneric(context); imagePool = IMAGES_AO_DAI; }
        List<Product> list = new ArrayList<>();
        if ("ALL".equals(categoryId)) {
            for (String cat : ALL_CATEGORY_IDS) {
                list.addAll(getProducts(context, cat));
            }
            return list;
        }

        if ("ÁO DÀI".equals(categoryId))         list = buildAoDai(context);
        else if ("NHẬT BÌNH".equals(categoryId)) list = buildNhatBinh(context);
        else if ("ÁO TẤC".equals(categoryId))    list = buildAoTac(context);
        else if ("GIAO LĨNH".equals(categoryId)) list = buildGiaoLinh(context);
        else if ("YẾM ĐÀO".equals(categoryId))   list = buildYemDao(context);
        else if ("PHỤ KIỆN".equals(categoryId))  list = buildPhuKien(context);
        else                                      list = buildGeneric(context);

        for (int i = 0; i < list.size(); i++) {
            Product p = list.get(i);
            applyGenericDetail(context, p);
            if (p.getImages() == null || p.getImages().isEmpty()) {
                setLocalImage(context, p, imagePool[i % imagePool.length]);
            }
        }
        return list;
    }

    /** Gán 1 ảnh trang phục THẬT (drawable local) cho sản phẩm mẫu qua URI "android.resource://"
     *  — Glide tải được thẳng từ chuỗi này giống hệt tải ảnh mạng, không cần đổi ProductAdapter. */
    private static void setLocalImage(Context ctx, Product p, String drawableName) {
        p.setImages(Collections.singletonList("android.resource://" + ctx.getPackageName() + "/drawable/" + drawableName));
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
        String prefix = recommended ? "AD" : "NB"; // Use standard prefixes to match categories
        Object[][] data = {
            {"01", R.string.mock_home1_name, R.string.material_lua_to_tam,  850000.0,  10, 4.8, "h_1"},
            {"02", R.string.mock_home2_name, R.string.material_gam_theu,    1200000.0, 15, 4.9, "h_2"},
            {"03", R.string.mock_home3_name, R.string.material_dui_to,      650000.0,  0,  4.5, "onboarding_3"},
            {"04", R.string.mock_home4_name, R.string.material_lua_mem,     250000.0,  5,  4.3, "onboarding_4"},
            {"05", R.string.mock_home5_name, R.string.material_lua_cao_cap, 2500000.0, 20, 5.0, "onboarding_5"},
        };

        // Trộn ảnh từ nhiều danh mục cho khối nổi bật ở Trang chủ (không riêng 1 loại trang phục).
        String[] highlightPool = {
                "carousel_aodai_1", "carousel_nhatbinh_1", "carousel_aotac_1",
                "carousel_giaolinh_1", "carousel_yemdao_1"
        };

        List<Product> list = new ArrayList<>();
        int index = 0;
        for (Object[] row : data) {
            Product p = new Product();
            p.setId(prefix + row[0]);
            p.setName(context.getString((int) row[1]));
            p.setMaterial(context.getString((int) row[2]));
            p.setPrice((double) row[3]);
            p.setDiscount((int) row[4]);
            p.setRating((double) row[5]);
            p.setImage((String) row[6]);
            p.setImages(Arrays.asList((String) row[6], "onboarding_1", "onboarding_2"));
            p.setColors(Arrays.asList("Trắng", "Xanh", "Vàng", "Hồng"));
            p.setStock(20);
            p.setNew(recommended);
            applyGenericDetail(context, p);
            setLocalImage(context, p, highlightPool[index % highlightPool.length]);
            index++;
            list.add(p);
        }
        return list;
    }

    // ── Áo Dài ────────────────────────────────────────────────────────────────

    private static List<Product> buildAoDai(Context ctx) {
        List<Product> list = new ArrayList<>();
        Product khoiTrang = mockProduct(ctx, "AD01", R.string.mock_ad1_name, "ÁO DÀI", 2890000, "Trắng");
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

        list.add(mockProduct(ctx, "AD02", R.string.mock_ad2_name, "ÁO DÀI", 1590000, "Xanh"));
        list.add(mockProduct(ctx, "AD03", R.string.mock_ad3_name, "ÁO DÀI", 1750000, "Vàng"));
        list.add(mockProduct(ctx, "AD04", R.string.mock_ad4_name, "ÁO DÀI", 1290000, "Hồng"));
        list.add(mockProduct(ctx, "AD05", R.string.mock_ad5_name, "ÁO DÀI", 1450000, "Xanh", "Hồng"));
        list.add(mockProduct(ctx, "AD06", R.string.mock_ad6_name, "ÁO DÀI", 1350000, "Hồng"));

        Product phanHoa = mockProduct(ctx, "AD07", R.string.mock_ad7_name, "ÁO DÀI", 1200000, "Trắng");
        phanHoa.setDescription(ctx.getString(R.string.desc_ad7));
        phanHoa.setStock(0);
        list.add(phanHoa);

        return list;
    }

    // ── Nhật Bình ────────────────────────────────────────────────────────────

    private static List<Product> buildNhatBinh(Context ctx) {
        List<Product> list = new ArrayList<>();
        list.add(mockProduct(ctx, "NB01",  R.string.mock_nb1_name,  "NHẬT BÌNH", 3490000, "Trắng"));
        list.add(mockProduct(ctx, "NB02",  R.string.mock_nb2_name,  "NHẬT BÌNH", 3290000, "Xanh lá"));
        list.add(mockProduct(ctx, "NB03",  R.string.mock_nb3_name,  "NHẬT BÌNH", 2890000, "Xanh lá"));
        list.add(mockProduct(ctx, "NB04",  R.string.mock_nb4_name,  "NHẬT BÌNH", 2190000, "Xanh"));
        list.add(mockProduct(ctx, "NB05",  R.string.mock_nb5_name,  "NHẬT BÌNH", 2690000, "Đỏ"));
        list.add(mockProduct(ctx, "NB06",  R.string.mock_nb6_name,  "NHẬT BÌNH", 2990000, "Xanh"));
        list.add(mockProduct(ctx, "NB07",  R.string.mock_nb7_name,  "NHẬT BÌNH", 2750000, "Xanh lá"));
        list.add(mockProduct(ctx, "NB08",  R.string.mock_nb8_name,  "NHẬT BÌNH", 2450000, "Vàng"));
        list.add(mockProduct(ctx, "NB09",  R.string.mock_nb9_name,  "NHẬT BÌNH", 2150000, "Vàng"));
        list.add(mockProduct(ctx, "NB10", R.string.mock_nb10_name, "NHẬT BÌNH", 1890000, "Vàng"));
        return list;
    }

    // ── Áo Tấc ───────────────────────────────────────────────────────────────

    private static List<Product> buildAoTac(Context ctx) {
        List<Product> list = new ArrayList<>();
        list.add(mockProduct(ctx, "AT01", R.string.mock_at1_name, "ÁO TẤC", 1590000, "Xanh"));
        list.add(mockProduct(ctx, "AT02", R.string.mock_at2_name, "ÁO TẤC", 1890000, "Trắng"));
        list.add(mockProduct(ctx, "AT03", R.string.mock_at3_name, "ÁO TẤC", 1290000, "Cam"));
        list.add(mockProduct(ctx, "AT04", R.string.mock_at4_name, "ÁO TẤC", 1450000, "Xanh lá"));
        list.add(mockProduct(ctx, "AT05", R.string.mock_at5_name, "ÁO TẤC", 1690000, "Xanh"));
        list.add(mockProduct(ctx, "AT06", R.string.mock_at6_name, "ÁO TẤC", 1750000, "Trắng"));
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
        p.setRating(4.8);
        p.setStock(20);

        // Map hình ảnh chính xác theo ID (Đồng bộ với tìm kiếm)
        String mainImg = "onboarding_1";
        if (id.contains("02")) mainImg = "onboarding_2";
        else if (id.contains("03")) mainImg = "onboarding_3";
        else if (id.contains("04")) mainImg = "onboarding_4";
        else if (id.contains("05")) mainImg = "onboarding_5";
        else if (id.contains("06")) mainImg = "h_1"; // Họa tiết phụ
        else if (id.contains("07")) mainImg = "onboarding_2";

        List<String> imgs = new ArrayList<>();
        imgs.add(mainImg);
        imgs.add("onboarding_5");
        imgs.add("onboarding_1");

        p.setImage(mainImg);
        p.setImages(imgs);

        applyGenericDetail(ctx, p);
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

    // Tên biến thể mô tả/câu chuyện theo danh mục — mỗi danh mục có 3 biến thể xoay vòng
    // theo id sản phẩm để các sản phẩm liền kề không đọc giống hệt nhau.
    private static final Map<String, int[]> DESC_TEMPLATES_BY_CATEGORY = new LinkedHashMap<>();
    private static final Map<String, int[]> STORY_TEMPLATES_BY_CATEGORY = new LinkedHashMap<>();
    static {
        DESC_TEMPLATES_BY_CATEGORY.put("ÁO DÀI", new int[]{R.string.desc_tmpl_ao_dai_1, R.string.desc_tmpl_ao_dai_2, R.string.desc_tmpl_ao_dai_3});
        DESC_TEMPLATES_BY_CATEGORY.put("NHẬT BÌNH", new int[]{R.string.desc_tmpl_nhat_binh_1, R.string.desc_tmpl_nhat_binh_2, R.string.desc_tmpl_nhat_binh_3});
        DESC_TEMPLATES_BY_CATEGORY.put("ÁO TẤC", new int[]{R.string.desc_tmpl_ao_tac_1, R.string.desc_tmpl_ao_tac_2, R.string.desc_tmpl_ao_tac_3});
        DESC_TEMPLATES_BY_CATEGORY.put("GIAO LĨNH", new int[]{R.string.desc_tmpl_giao_linh_1, R.string.desc_tmpl_giao_linh_2, R.string.desc_tmpl_giao_linh_3});
        DESC_TEMPLATES_BY_CATEGORY.put("YẾM ĐÀO", new int[]{R.string.desc_tmpl_yem_dao_1, R.string.desc_tmpl_yem_dao_2, R.string.desc_tmpl_yem_dao_3});
        DESC_TEMPLATES_BY_CATEGORY.put("PHỤ KIỆN", new int[]{R.string.desc_tmpl_phu_kien_1, R.string.desc_tmpl_phu_kien_2, R.string.desc_tmpl_phu_kien_3});

        STORY_TEMPLATES_BY_CATEGORY.put("ÁO DÀI", new int[]{R.string.story_tmpl_ao_dai_1, R.string.story_tmpl_ao_dai_2, R.string.story_tmpl_ao_dai_3});
        STORY_TEMPLATES_BY_CATEGORY.put("NHẬT BÌNH", new int[]{R.string.story_tmpl_nhat_binh_1, R.string.story_tmpl_nhat_binh_2, R.string.story_tmpl_nhat_binh_3});
        STORY_TEMPLATES_BY_CATEGORY.put("ÁO TẤC", new int[]{R.string.story_tmpl_ao_tac_1, R.string.story_tmpl_ao_tac_2, R.string.story_tmpl_ao_tac_3});
        STORY_TEMPLATES_BY_CATEGORY.put("GIAO LĨNH", new int[]{R.string.story_tmpl_giao_linh_1, R.string.story_tmpl_giao_linh_2, R.string.story_tmpl_giao_linh_3});
        STORY_TEMPLATES_BY_CATEGORY.put("YẾM ĐÀO", new int[]{R.string.story_tmpl_yem_dao_1, R.string.story_tmpl_yem_dao_2, R.string.story_tmpl_yem_dao_3});
        STORY_TEMPLATES_BY_CATEGORY.put("PHỤ KIỆN", new int[]{R.string.story_tmpl_phu_kien_1, R.string.story_tmpl_phu_kien_2, R.string.story_tmpl_phu_kien_3});
    }

    // Phụ kiện dạng vải (khăn/vấn) cần bảo quản như hàng dệt; phụ kiện cứng (mũ/quạt/ô) cần
    // tránh ẩm và lau khô thay vì giặt.
    private static final List<String> FABRIC_ACCESSORY_TYPES = Arrays.asList("Khăn đội đầu");

    // Mô tả rập khuôn do admin để mặc định khi tạo sản phẩm trên Firestore (chưa kịp viết nội
    // dung riêng) — xem như "còn thiếu" để sinh nội dung thay vì hiển thị nguyên câu này.
    private static final String GENERIC_PLACEHOLDER_DESC = "Sản phẩm Việt Phục TiredCity cao cấp.";

    private static boolean isSparse(String s) {
        return s == null || s.trim().isEmpty() || s.trim().equalsIgnoreCase(GENERIC_PLACEHOLDER_DESC);
    }

    /** Điền phần mô tả/câu chuyện/bảo quản/thông số còn thiếu (hoặc còn sơ sài — ví dụ mô tả
     *  mặc định do admin chưa kịp viết) bằng nội dung hợp lý theo danh mục — mỗi sản phẩm nhận
     *  1 trong 3 biến thể xoay vòng (chèn tên + màu/loại + chất liệu riêng) thay vì lặp lại đúng
     *  1 câu mẫu cho mọi sản phẩm. Gọi cho MỌI sản phẩm bất kể nguồn dữ liệu (mẫu offline, REST
     *  hay Firestore) để mục 01/02/03 ở Chi tiết sản phẩm không bao giờ trống hay quá sơ sài. */
    public static void applyGenericDetail(Context ctx, Product p) {
        if (p.getOrigin() == null) p.setOrigin(ctx.getString(R.string.default_origin));

        if (p.getColors() == null || p.getColors().isEmpty()) {
            p.setColors(Arrays.asList("Trắng", "Xanh", "Vàng"));
        }

        if (p.getDescription() == null) {
            p.setDescription(ctx.getString(R.string.desc_template_default,
                    p.getName(), p.getMaterial().toLowerCase(Locale.getDefault())));
        String category = p.getCategory();
        int variant = Math.abs((p.getId() != null ? p.getId() : p.getName()).hashCode()) % 3;
        String colorOrType = (p.getColors() != null && !p.getColors().isEmpty())
                ? String.join(", ", p.getColors()) : ctx.getString(R.string.spec_color_various);
        String materialLower = p.getMaterial() != null ? p.getMaterial().toLowerCase(Locale.getDefault()) : "";

        if (isSparse(p.getDescription())) {
            int[] templates = DESC_TEMPLATES_BY_CATEGORY.get(category);
            if (templates != null) {
                p.setDescription(ctx.getString(templates[variant], p.getName(), colorOrType, materialLower));
            } else {
                p.setDescription(ctx.getString(R.string.desc_template_default, p.getName(), materialLower));
            }
        }
        if (isSparse(p.getStory())) {
            int[] templates = STORY_TEMPLATES_BY_CATEGORY.get(category);
            p.setStory(templates != null
                    ? ctx.getString(templates[variant], p.getName())
                    : ctx.getString(R.string.story_template_default));
        }
        if (p.getCareInstructions() == null) {
            boolean isFabricAccessory = p.getColors() != null && !Collections.disjoint(p.getColors(), FABRIC_ACCESSORY_TYPES);
            if ("PHỤ KIỆN".equals(category) && !isFabricAccessory) {
                // Mũ/quạt/ô: chất liệu cứng hoặc bán cứng, không giặt được — lau khô + tránh ẩm/nắng.
                p.setCareInstructions(Arrays.asList(
                        ctx.getString(R.string.care_wipe_soft_cloth),
                        ctx.getString(R.string.care_avoid_moisture),
                        ctx.getString(R.string.care_avoid_direct_sun),
                        ctx.getString(R.string.care_store_box)));
            } else if ("PHỤ KIỆN".equals(category)) {
                // Khăn/vấn: vải mềm, giặt nhẹ được nhưng vẫn cần tránh nắng gắt khi phơi.
                p.setCareInstructions(Arrays.asList(
                        ctx.getString(R.string.care_hand_wash_cold_gentle),
                        ctx.getString(R.string.care_no_bleach),
                        ctx.getString(R.string.care_avoid_direct_sun),
                        ctx.getString(R.string.care_store_box)));
            } else {
                p.setCareInstructions(Arrays.asList(
                        ctx.getString(R.string.care_hand_wash_cold_gentle),
                        ctx.getString(R.string.care_no_bleach),
                        ctx.getString(R.string.care_iron_low_cloth),
                        ctx.getString(R.string.care_store_dry)));
            }
        }
        if (p.getSpecifications() == null || p.getSpecifications().isEmpty()) {
            String style = "PHỤ KIỆN".equals(category) ? ctx.getString(R.string.spec_style_accessory)
                    : (category != null ? category : ctx.getString(R.string.spec_style_traditional));
            p.setSpecifications(specs(ctx, colorOrType, style, ctx.getString(R.string.spec_occasion_default), p.getOrigin()));
        }
    }
}
