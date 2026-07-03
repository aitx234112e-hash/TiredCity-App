package com.tiredcity.admin.utils;

import android.content.Context;

import com.google.firebase.firestore.DocumentSnapshot;
import com.tiredcity.admin.R;
import com.tiredcity.admin.data.AdminModule;
import com.tiredcity.admin.data.model.Row;

import java.util.ArrayList;
import java.util.List;

/**
 * Chuyen doi List<DocumentSnapshot> Firestore -> List<Row> hien thi,
 * mirror dung ten truong va nhan hien thi tren tung man web-admin tuong ung.
 */
public final class RowFormatter {
    private RowFormatter() {}

    public static List<Row> format(Context ctx, AdminModule module, List<DocumentSnapshot> docs) {
        List<Row> rows = new ArrayList<>();
        for (DocumentSnapshot d : docs) {
            rows.add(formatOne(ctx, module, d));
        }
        return rows;
    }

    public static Row formatOne(Context ctx, AdminModule module, DocumentSnapshot d) {
        switch (module) {
            case PRODUCTS: return product(ctx, d);
            case ORDERS: return order(ctx, d);
            case USERS: return user(ctx, d);
            case VOUCHERS: return voucher(ctx, d);
            case EVENTS: return event(ctx, d);
            case SHIPPING: return shipping(ctx, d);
            case FEEDBACK: return feedback(ctx, d);
            case BLOGS: return blog(ctx, d);
            case AUDIT: return audit(ctx, d);
            default: return new Row(d.getId(), "", "");
        }
    }

    private static Row product(Context ctx, DocumentSnapshot d) {
        String name = DocUtils.str(d, "product_name");
        String dept = DocUtils.str(d, "product_dept");
        double stock = DocUtils.num(d, "stocked_quantity", "stock");
        double price = DocUtils.num(d, "unit_price");
        double discount = DocUtils.num(d, "discount");
        String subtitle = (dept.isEmpty() ? "" : dept + " • ") + "Tồn: " + (long) stock;
        String badge = discount > 0 ? "-" + (long) discount + "%" : null;
        return new Row(name.isEmpty() ? "(Chưa đặt tên)" : name, subtitle, DocUtils.money(price), badge, R.color.st_danger);
    }

    private static Row order(Context ctx, DocumentSnapshot d) {
        String orderId = DocUtils.str(d, "orderID", "id");
        String shortId = (orderId.isEmpty() ? d.getId() : orderId);
        if (shortId.length() > 8) shortId = shortId.substring(shortId.length() - 8);
        String userName = DocUtils.str(d, "userName");
        String date = DocUtils.date(d, "createdAt");
        double total = DocUtils.num(d, "totalPrice", "total", "amount");
        String status = DocUtils.str(d, "status");

        String title = "Đơn #" + shortId;
        String subtitle = (userName.isEmpty() ? "Khách hàng" : userName) + (date.isEmpty() ? "" : " • " + date);
        return new Row(title, subtitle, DocUtils.money(total), orderStatusLabel(ctx, status), orderStatusColor(status));
    }

    public static String orderStatusLabel(Context ctx, String status) {
        if (status == null) return "";
        switch (status) {
            case "pending": return ctx.getString(R.string.status_pending);
            case "processing": return ctx.getString(R.string.status_processing);
            case "shipped": return ctx.getString(R.string.status_shipped);
            case "delivered": return ctx.getString(R.string.status_delivered);
            case "cancelled": return ctx.getString(R.string.status_cancelled);
            default: return status;
        }
    }

    public static int orderStatusColor(String status) {
        if (status == null) return R.color.st_neutral;
        switch (status) {
            case "pending": return R.color.st_pending;
            case "processing": return R.color.st_info;
            case "shipped": return R.color.st_info;
            case "delivered": return R.color.st_success;
            case "cancelled": return R.color.st_danger;
            default: return R.color.st_neutral;
        }
    }

    private static Row user(Context ctx, DocumentSnapshot d) {
        String name = DocUtils.str(d, "fullName", "profileName", "email");
        String email = DocUtils.str(d, "email");
        String phone = DocUtils.str(d, "phone");
        String role = DocUtils.str(d, "role");
        if (role.isEmpty()) role = "user";

        String subtitle = email + (phone.isEmpty() ? "" : " • " + phone);
        int color = "superadmin".equals(role) ? R.color.st_danger
                : "admin".equals(role) ? R.color.st_info : R.color.st_neutral;
        return new Row(name.isEmpty() ? "(Chưa đặt tên)" : name, subtitle, "", role, color);
    }

    private static Row voucher(Context ctx, DocumentSnapshot d) {
        String code = DocUtils.str(d, "code");
        String desc = DocUtils.str(d, "description");
        String type = DocUtils.str(d, "type");
        double value = DocUtils.num(d, "value");
        double minOrder = DocUtils.num(d, "minOrder");
        String expiry = DocUtils.str(d, "expiry");

        String discountLabel = "percent".equals(type) ? "-" + (long) value + "%" : "-" + DocUtils.money(value);
        boolean expired = isExpired(expiry);
        String subtitle = desc.isEmpty() ? "Đơn tối thiểu " + DocUtils.money(minOrder) : desc;
        return new Row(code, subtitle, discountLabel, expired ? "Hết hạn" : "Đang áp dụng",
                expired ? R.color.st_neutral : R.color.st_success);
    }

    private static boolean isExpired(String expiry) {
        if (expiry == null || expiry.isEmpty()) return false;
        try {
            String s = expiry.length() >= 10 ? expiry.substring(0, 10) : expiry;
            java.text.SimpleDateFormat f = new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US);
            return f.parse(s).getTime() < System.currentTimeMillis();
        } catch (Exception e) {
            return false;
        }
    }

    private static Row event(Context ctx, DocumentSnapshot d) {
        String title = DocUtils.str(d, "title");
        String location = DocUtils.str(d, "location");
        String rawDate = DocUtils.str(d, "date");
        String date = DocUtils.date(d, "date");
        boolean upcoming = isUpcoming(rawDate);
        return new Row(title, location, date.isEmpty() ? "Chưa đặt ngày" : date,
                upcoming ? "Sắp diễn ra" : "Đã qua", upcoming ? R.color.st_success : R.color.st_neutral);
    }

    private static boolean isUpcoming(String date) {
        if (date == null || date.isEmpty()) return false;
        return DocUtils.millisFromIso(date) >= System.currentTimeMillis();
    }

    private static Row shipping(Context ctx, DocumentSnapshot d) {
        String name = DocUtils.str(d, "name");
        String area = DocUtils.str(d, "area");
        String time = DocUtils.str(d, "estimatedTime");
        double fee = DocUtils.num(d, "fee");
        String subtitle = area + (time.isEmpty() ? "" : " • " + time);
        String feeLabel = fee <= 0 ? "Miễn phí" : DocUtils.money(fee);
        return new Row(name, subtitle, feeLabel);
    }

    private static Row feedback(Context ctx, DocumentSnapshot d) {
        String name = DocUtils.str(d, "fullName");
        String message = DocUtils.str(d, "message");
        String phone = DocUtils.str(d, "phone");
        boolean replied = Boolean.TRUE.equals(d.getBoolean("replied"));
        return new Row(name.isEmpty() ? "(Ẩn danh)" : name, message, phone,
                replied ? ctx.getString(R.string.feedback_replied) : ctx.getString(R.string.feedback_pending),
                replied ? R.color.st_success : R.color.st_pending);
    }

    private static Row blog(Context ctx, DocumentSnapshot d) {
        String title = DocUtils.str(d, "title");
        String excerpt = DocUtils.str(d, "excerpt");
        String status = DocUtils.str(d, "status");
        String updated = DocUtils.date(d, "updatedAt", "createdAt");
        boolean published = "published".equals(status);
        return new Row(title, excerpt.isEmpty() ? "Chưa có mô tả ngắn" : excerpt, updated,
                published ? "Đã xuất bản" : "Bản nháp", published ? R.color.st_success : R.color.st_neutral);
    }

    private static Row audit(Context ctx, DocumentSnapshot d) {
        String action = DocUtils.str(d, "action");
        String actor = DocUtils.str(d, "actor");
        String target = DocUtils.str(d, "target");
        String detail = DocUtils.str(d, "detail");
        String time = DocUtils.date(d, "timestamp");

        String title = actionLabel(action);
        String subtitle = actor + (target.isEmpty() ? "" : " → " + target) + (detail.isEmpty() ? "" : " (" + detail + ")");
        return new Row(title, subtitle, time);
    }

    public static String actionLabel(String action) {
        if (action == null) return "";
        switch (action) {
            case "login": return "Đăng nhập";
            case "logout": return "Đăng xuất";
            case "order.status": return "Cập nhật đơn";
            case "order.cancel": return "Huỷ đơn";
            case "voucher.create": return "Tạo voucher";
            case "voucher.update": return "Sửa voucher";
            case "voucher.delete": return "Xoá voucher";
            case "shipping.create": return "Tạo vận chuyển";
            case "shipping.update": return "Sửa vận chuyển";
            case "shipping.delete": return "Xoá vận chuyển";
            case "user.update": return "Sửa user";
            case "user.delete": return "Xoá user";
            case "user.disable": return "Vô hiệu hoá user";
            case "user.enable": return "Kích hoạt user";
            case "product.create": return "Tạo sản phẩm";
            case "product.update": return "Sửa sản phẩm";
            case "product.delete": return "Xoá sản phẩm";
            case "event.create": return "Tạo sự kiện";
            case "event.update": return "Sửa sự kiện";
            case "event.delete": return "Xoá sự kiện";
            case "blog.create": return "Tạo bài viết";
            case "blog.update": return "Sửa bài viết";
            case "blog.delete": return "Xoá bài viết";
            case "feedback.delete": return "Xoá liên hệ";
            case "export.csv": return "Xuất CSV";
            default: return action;
        }
    }
}
