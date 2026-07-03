package com.tiredcity.admin.data;

import com.google.firebase.firestore.DocumentSnapshot;
import com.tiredcity.admin.data.model.FormField;
import com.tiredcity.admin.utils.DocUtils;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

/**
 * Dinh nghia form them/sua cho tung module — mirror cac form CRUD ben web-admin
 * (product-management, vouchers, events, shipping, blog-management, user-management).
 *
 * Cung cap:
 *  - quyen thao tac ({@link #canCreate}, {@link #canEdit}, {@link #canDelete})
 *  - danh sach truong nhap lieu ({@link #fields}) — prefill khi sua
 *  - dung payload ghi Firestore ({@link #payload})
 *  - ten hanh dong audit + nhan doi tuong
 */
public final class ModuleForm {
    private ModuleForm() {}

    // ---------------------------------------------------------------- quyen

    public static boolean canCreate(AdminModule m) {
        switch (m) {
            case PRODUCTS: case VOUCHERS: case EVENTS: case SHIPPING: case BLOGS:
                return true;
            default:
                return false;
        }
    }

    public static boolean canEdit(AdminModule m) {
        switch (m) {
            case PRODUCTS: case VOUCHERS: case EVENTS: case SHIPPING: case BLOGS: case USERS:
                return true;
            default:
                return false;
        }
    }

    public static boolean canDelete(AdminModule m) {
        switch (m) {
            case PRODUCTS: case VOUCHERS: case EVENTS: case SHIPPING: case BLOGS: case USERS: case FEEDBACK:
                return true;
            default:
                return false;
        }
    }

    // ------------------------------------------------------------ dinh nghia field

    /** Danh sach truong cua form. {@code d == null} -> them moi; nguoc lai prefill de sua. */
    public static List<FormField> fields(AdminModule m, DocumentSnapshot d) {
        List<FormField> f = new ArrayList<>();
        switch (m) {
            case PRODUCTS:
                f.add(new FormField("product_name", "Tên sản phẩm", FormField.Type.TEXT, true)
                        .value(s(d, "product_name")));
                f.add(new FormField("product_dept", "Danh mục", FormField.Type.SELECT, true,
                        new String[]{"Áo dài", "Việt phục", "Áo bà ba", "Phụ kiện"},
                        new String[]{"ao-dai", "viet-phuc", "ao-ba-ba", "phu-kien"})
                        .value(s(d, "product_dept")));
                f.add(new FormField("unit_price", "Giá bán (₫)", FormField.Type.NUMBER, true)
                        .value(n(d, "unit_price")));
                f.add(new FormField("discount", "Giảm giá (%)", FormField.Type.NUMBER, false)
                        .value(n(d, "discount")));
                f.add(new FormField("stock", "Tồn kho (phụ kiện)", FormField.Type.NUMBER, false)
                        .value(n(d, "stock", "stocked_quantity"))
                        .hint("Chỉ dùng cho danh mục Phụ kiện"));
                f.add(new FormField("quantityS", "Số lượng size S", FormField.Type.NUMBER, false)
                        .value(sizeQty(d, "S")));
                f.add(new FormField("quantityM", "Số lượng size M", FormField.Type.NUMBER, false)
                        .value(sizeQty(d, "M")));
                f.add(new FormField("quantityL", "Số lượng size L", FormField.Type.NUMBER, false)
                        .value(sizeQty(d, "L")));
                f.add(new FormField("quantityXL", "Số lượng size XL", FormField.Type.NUMBER, false)
                        .value(sizeQty(d, "XL")));
                f.add(new FormField("material", "Chất liệu", FormField.Type.TEXT, false)
                        .value(s(d, "material")));
                f.add(new FormField("origin", "Xuất xứ", FormField.Type.TEXT, false)
                        .value(s(d, "origin")));
                f.add(new FormField("description", "Mô tả", FormField.Type.TEXTAREA, false)
                        .value(s(d, "description", "short_description")));
                f.add(new FormField("image", "Link ảnh (URL)", FormField.Type.TEXT, false)
                        .value(firstImage(d)).hint("Dán URL ảnh sản phẩm"));
                break;

            case VOUCHERS:
                f.add(new FormField("code", "Mã voucher", FormField.Type.TEXT, true)
                        .value(s(d, "code")).hint("VD: TIREDCITY10"));
                f.add(new FormField("type", "Loại giảm", FormField.Type.SELECT, true,
                        new String[]{"Giảm theo %", "Giảm tiền (VND)"},
                        new String[]{"percent", "fixed"})
                        .value(s(d, "type").isEmpty() ? "percent" : s(d, "type")));
                f.add(new FormField("value", "Giá trị giảm", FormField.Type.NUMBER, true)
                        .value(n(d, "value")));
                f.add(new FormField("minOrder", "Đơn tối thiểu (₫)", FormField.Type.NUMBER, false)
                        .value(n(d, "minOrder")));
                f.add(new FormField("expiry", "Hạn sử dụng", FormField.Type.DATE, false)
                        .value(isoDate(d, "expiry")));
                f.add(new FormField("description", "Mô tả / điều kiện", FormField.Type.TEXTAREA, false)
                        .value(s(d, "description")));
                f.add(new FormField("image", "Link ảnh (URL)", FormField.Type.TEXT, false)
                        .value(s(d, "image")));
                break;

            case EVENTS:
                f.add(new FormField("title", "Tên sự kiện", FormField.Type.TEXT, true)
                        .value(s(d, "title")));
                f.add(new FormField("date", "Ngày diễn ra", FormField.Type.DATE, false)
                        .value(isoDate(d, "date")));
                f.add(new FormField("location", "Địa điểm", FormField.Type.TEXT, false)
                        .value(s(d, "location")));
                f.add(new FormField("description", "Mô tả", FormField.Type.TEXTAREA, false)
                        .value(s(d, "description")));
                f.add(new FormField("image", "Link ảnh (URL)", FormField.Type.TEXT, false)
                        .value(s(d, "image")));
                break;

            case SHIPPING:
                f.add(new FormField("name", "Tên phương thức / đơn vị", FormField.Type.TEXT, true)
                        .value(s(d, "name")));
                f.add(new FormField("fee", "Phí vận chuyển (₫)", FormField.Type.NUMBER, false)
                        .value(n(d, "fee")).hint("0 = miễn phí"));
                f.add(new FormField("estimatedTime", "Thời gian dự kiến", FormField.Type.TEXT, false)
                        .value(s(d, "estimatedTime")).hint("VD: 2-3 ngày"));
                f.add(new FormField("area", "Khu vực áp dụng", FormField.Type.TEXT, false)
                        .value(s(d, "area")));
                f.add(new FormField("description", "Mô tả", FormField.Type.TEXTAREA, false)
                        .value(s(d, "description")));
                break;

            case BLOGS:
                f.add(new FormField("title", "Tiêu đề", FormField.Type.TEXT, true)
                        .value(s(d, "title")));
                f.add(new FormField("excerpt", "Mô tả ngắn", FormField.Type.TEXTAREA, false)
                        .value(s(d, "excerpt")));
                f.add(new FormField("content", "Nội dung", FormField.Type.TEXTAREA, true)
                        .value(s(d, "content")));
                f.add(new FormField("authorName", "Tác giả", FormField.Type.TEXT, false)
                        .value(s(d, "authorName").isEmpty() ? "Admin" : s(d, "authorName")));
                f.add(new FormField("thumbnail", "Link ảnh bìa (URL)", FormField.Type.TEXT, false)
                        .value(s(d, "thumbnail")));
                f.add(new FormField("status", "Trạng thái", FormField.Type.SELECT, true,
                        new String[]{"Bản nháp", "Đã xuất bản"},
                        new String[]{"draft", "published"})
                        .value(s(d, "status").isEmpty() ? "draft" : s(d, "status")));
                break;

            case USERS:
                f.add(new FormField("profileName", "Họ tên", FormField.Type.TEXT, true)
                        .value(s(d, "profileName", "fullName")));
                f.add(new FormField("phone", "Số điện thoại", FormField.Type.TEXT, false)
                        .value(s(d, "phone")));
                f.add(new FormField("gender", "Giới tính", FormField.Type.SELECT, false,
                        new String[]{"Nam", "Nữ", "Khác"},
                        new String[]{"Nam", "Nữ", "Khác"})
                        .value(s(d, "gender").isEmpty() ? "Nam" : s(d, "gender")));
                f.add(new FormField("role", "Vai trò", FormField.Type.SELECT, false,
                        new String[]{"Người dùng", "Admin", "Super admin"},
                        new String[]{"user", "admin", "superadmin"})
                        .value(s(d, "role").isEmpty() ? "user" : s(d, "role")));
                f.add(new FormField("disabled", "Vô hiệu hoá tài khoản", FormField.Type.SWITCH, false)
                        .value(d != null && Boolean.TRUE.equals(d.getBoolean("disabled")) ? "true" : "false"));
                break;

            default:
                break;
        }
        return f;
    }

    // -------------------------------------------------------------- dung payload

    /** Dung map ghi Firestore tu gia tri nguoi dung nhap. */
    public static Map<String, Object> payload(AdminModule m, Map<String, String> v, boolean isCreate) {
        Map<String, Object> p = new HashMap<>();
        switch (m) {
            case PRODUCTS: {
                String dept = val(v, "product_dept");
                p.put("product_name", val(v, "product_name"));
                p.put("product_dept", dept);
                p.put("unit_price", toLong(v, "unit_price"));
                p.put("discount", toLong(v, "discount"));
                p.put("material", val(v, "material"));
                p.put("origin", val(v, "origin"));
                p.put("description", val(v, "description"));
                p.put("images", imageList(val(v, "image")));
                if ("phu-kien".equals(dept)) {
                    long stock = toLong(v, "stock");
                    p.put("sizes", new ArrayList<>());
                    p.put("stock", stock);
                    p.put("stocked_quantity", stock);
                } else {
                    List<Map<String, Object>> sizes = new ArrayList<>();
                    long total = 0;
                    for (String size : new String[]{"S", "M", "L", "XL"}) {
                        long q = toLong(v, "quantity" + size);
                        if (q > 0) {
                            Map<String, Object> s = new HashMap<>();
                            s.put("size", size);
                            s.put("quantity", q);
                            sizes.add(s);
                            total += q;
                        }
                    }
                    p.put("sizes", sizes);
                    p.put("stock", 0L);
                    p.put("stocked_quantity", total);
                }
                if (isCreate) {
                    p.put("rating", 4L);
                    p.put("createdAt", nowIso());
                }
                break;
            }
            case VOUCHERS:
                p.put("code", val(v, "code").toUpperCase(Locale.ROOT));
                p.put("type", val(v, "type"));
                p.put("value", toLong(v, "value"));
                p.put("minOrder", toLong(v, "minOrder"));
                p.put("expiry", val(v, "expiry"));
                p.put("description", val(v, "description"));
                p.put("image", val(v, "image"));
                if (isCreate) p.put("createdAt", nowIso());
                break;

            case EVENTS:
                p.put("title", val(v, "title"));
                p.put("date", val(v, "date"));
                p.put("location", val(v, "location"));
                p.put("description", val(v, "description"));
                p.put("image", val(v, "image"));
                if (isCreate) p.put("createdAt", nowIso());
                break;

            case SHIPPING:
                p.put("name", val(v, "name"));
                p.put("fee", toLong(v, "fee"));
                p.put("estimatedTime", val(v, "estimatedTime"));
                p.put("area", val(v, "area"));
                p.put("description", val(v, "description"));
                if (isCreate) p.put("createdAt", nowIso());
                break;

            case BLOGS: {
                String status = val(v, "status");
                if (status.isEmpty()) status = "draft";
                String author = val(v, "authorName");
                p.put("title", val(v, "title"));
                p.put("excerpt", val(v, "excerpt"));
                p.put("content", val(v, "content"));
                p.put("thumbnail", val(v, "thumbnail"));
                p.put("authorName", author.isEmpty() ? "Admin" : author);
                p.put("status", status);
                p.put("publishedAt", "published".equals(status) ? nowIso() : null);
                p.put("updatedAt", nowIso());
                if (isCreate) p.put("createdAt", nowIso());
                break;
            }
            case USERS: {
                String name = val(v, "profileName");
                p.put("profileName", name);
                p.put("fullName", name);
                p.put("phone", val(v, "phone"));
                p.put("gender", val(v, "gender"));
                p.put("role", val(v, "role"));
                p.put("disabled", "true".equals(val(v, "disabled")));
                break;
            }
            default:
                break;
        }
        return p;
    }

    // ------------------------------------------------------------------ audit

    public static String auditCreate(AdminModule m) {
        return actionKey(m) + ".create";
    }

    public static String auditUpdate(AdminModule m) {
        return actionKey(m) + ".update";
    }

    public static String auditDelete(AdminModule m) {
        return actionKey(m) + ".delete";
    }

    private static String actionKey(AdminModule m) {
        switch (m) {
            case PRODUCTS: return "product";
            case VOUCHERS: return "voucher";
            case EVENTS:   return "event";
            case SHIPPING: return "shipping";
            case BLOGS:    return "blog";
            case USERS:    return "user";
            case FEEDBACK: return "feedback";
            default:       return "record";
        }
    }

    /** Nhan doi tuong dung ghi vao audit log. */
    public static String targetOf(AdminModule m, DocumentSnapshot d) {
        if (d == null) return "";
        switch (m) {
            case PRODUCTS: return firstNonEmpty(DocUtils.str(d, "product_name"), d.getId());
            case VOUCHERS: return firstNonEmpty(DocUtils.str(d, "code"), d.getId());
            case EVENTS:   return firstNonEmpty(DocUtils.str(d, "title"), d.getId());
            case SHIPPING: return firstNonEmpty(DocUtils.str(d, "name"), d.getId());
            case BLOGS:    return firstNonEmpty(DocUtils.str(d, "title"), d.getId());
            case USERS:    return firstNonEmpty(DocUtils.str(d, "profileName", "fullName", "email"), d.getId());
            case FEEDBACK: return firstNonEmpty(DocUtils.str(d, "fullName", "email"), d.getId());
            default:       return d.getId();
        }
    }

    /** Nhan doi tuong tu gia tri form (dung khi vua tao/sua xong, chua co doc). */
    public static String targetOf(AdminModule m, Map<String, String> v) {
        switch (m) {
            case PRODUCTS: return val(v, "product_name");
            case VOUCHERS: return val(v, "code").toUpperCase(Locale.ROOT);
            case EVENTS:   return val(v, "title");
            case SHIPPING: return val(v, "name");
            case BLOGS:    return val(v, "title");
            case USERS:    return val(v, "profileName");
            default:       return "";
        }
    }

    // ------------------------------------------------------------- helper prefill

    private static String s(DocumentSnapshot d, String... keys) {
        return d == null ? "" : DocUtils.str(d, keys);
    }

    private static String n(DocumentSnapshot d, String... keys) {
        if (d == null) return "";
        double v = DocUtils.num(d, keys);
        return v == 0 ? "" : String.valueOf((long) v);
    }

    private static String isoDate(DocumentSnapshot d, String key) {
        String raw = s(d, key);
        if (raw.length() >= 10 && raw.charAt(4) == '-') return raw.substring(0, 10);
        return raw;
    }

    @SuppressWarnings("unchecked")
    private static String sizeQty(DocumentSnapshot d, String size) {
        if (d == null) return "";
        Object o = d.get("sizes");
        if (o instanceof List) {
            for (Object it : (List<Object>) o) {
                if (it instanceof Map) {
                    Map<?, ?> mm = (Map<?, ?>) it;
                    if (size.equals(String.valueOf(mm.get("size")))) {
                        Object q = mm.get("quantity");
                        if (q instanceof Number) {
                            long lq = ((Number) q).longValue();
                            return lq == 0 ? "" : String.valueOf(lq);
                        }
                    }
                }
            }
        }
        return "";
    }

    @SuppressWarnings("unchecked")
    private static String firstImage(DocumentSnapshot d) {
        if (d == null) return "";
        Object o = d.get("images");
        if (o instanceof List && !((List<Object>) o).isEmpty()) {
            Object f = ((List<Object>) o).get(0);
            if (f != null) return String.valueOf(f);
        }
        return DocUtils.str(d, "image", "thumbnail");
    }

    // -------------------------------------------------------------- helper payload

    private static List<String> imageList(String url) {
        List<String> list = new ArrayList<>();
        if (url != null && !url.trim().isEmpty()) list.add(url.trim());
        return list;
    }

    private static String val(Map<String, String> v, String key) {
        String s = v.get(key);
        return s == null ? "" : s.trim();
    }

    private static long toLong(Map<String, String> v, String key) {
        String s = v.get(key);
        if (s == null || s.trim().isEmpty()) return 0L;
        try {
            return (long) Double.parseDouble(s.trim().replaceAll("[^0-9.\\-]", ""));
        } catch (NumberFormatException e) {
            return 0L;
        }
    }

    private static String firstNonEmpty(String a, String b) {
        return a == null || a.trim().isEmpty() ? b : a;
    }

    private static String nowIso() {
        SimpleDateFormat iso = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US);
        iso.setTimeZone(TimeZone.getTimeZone("UTC"));
        return iso.format(new Date());
    }
}
