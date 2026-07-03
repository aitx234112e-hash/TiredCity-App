package com.tiredcity.admin.utils;

import android.content.Context;

import com.google.firebase.firestore.DocumentSnapshot;
import com.tiredcity.admin.data.AdminModule;
import com.tiredcity.admin.data.model.DetailField;

import java.util.ArrayList;
import java.util.List;

/** Xay danh sach nhan/gia tri chi tiet cho dialog xem 1 ban ghi (moi module khac ORDERS). */
public final class DetailFormatter {
    private DetailFormatter() {}

    public static List<DetailField> format(Context ctx, AdminModule module, DocumentSnapshot d) {
        List<DetailField> f = new ArrayList<>();
        switch (module) {
            case PRODUCTS:
                f.add(new DetailField("Tên sản phẩm", DocUtils.str(d, "product_name")));
                f.add(new DetailField("Danh mục", DocUtils.str(d, "product_dept")));
                f.add(new DetailField("Giá", DocUtils.money(DocUtils.num(d, "unit_price"))));
                f.add(new DetailField("Giảm giá", ((long) DocUtils.num(d, "discount")) + "%"));
                f.add(new DetailField("Tồn kho", String.valueOf((long) DocUtils.num(d, "stocked_quantity", "stock"))));
                f.add(new DetailField("Chất liệu", DocUtils.str(d, "material")));
                f.add(new DetailField("Xuất xứ", DocUtils.str(d, "origin")));
                f.add(new DetailField("Mô tả", DocUtils.str(d, "short_description", "description")));
                break;
            case USERS:
                f.add(new DetailField("Họ tên", DocUtils.str(d, "fullName", "profileName")));
                f.add(new DetailField("Email", DocUtils.str(d, "email")));
                f.add(new DetailField("SĐT", DocUtils.str(d, "phone")));
                f.add(new DetailField("Giới tính", DocUtils.str(d, "gender")));
                f.add(new DetailField("Vai trò", DocUtils.str(d, "role")));
                break;
            case VOUCHERS:
                f.add(new DetailField("Mã voucher", DocUtils.str(d, "code")));
                f.add(new DetailField("Loại giảm", "percent".equals(DocUtils.str(d, "type")) ? "Giảm %" : "Giảm tiền"));
                f.add(new DetailField("Giá trị", String.valueOf((long) DocUtils.num(d, "value"))));
                f.add(new DetailField("Đơn tối thiểu", DocUtils.money(DocUtils.num(d, "minOrder"))));
                f.add(new DetailField("Hạn sử dụng", DocUtils.str(d, "expiry").isEmpty() ? "Không giới hạn" : DocUtils.date(d, "expiry")));
                f.add(new DetailField("Mô tả", DocUtils.str(d, "description")));
                break;
            case EVENTS:
                f.add(new DetailField("Tên sự kiện", DocUtils.str(d, "title")));
                f.add(new DetailField("Ngày diễn ra", DocUtils.date(d, "date")));
                f.add(new DetailField("Địa điểm", DocUtils.str(d, "location")));
                f.add(new DetailField("Mô tả", DocUtils.str(d, "description")));
                break;
            case SHIPPING:
                f.add(new DetailField("Tên phương thức", DocUtils.str(d, "name")));
                double fee = DocUtils.num(d, "fee");
                f.add(new DetailField("Phí vận chuyển", fee <= 0 ? "Miễn phí" : DocUtils.money(fee)));
                f.add(new DetailField("Thời gian dự kiến", DocUtils.str(d, "estimatedTime")));
                f.add(new DetailField("Khu vực áp dụng", DocUtils.str(d, "area")));
                f.add(new DetailField("Mô tả", DocUtils.str(d, "description")));
                break;
            case FEEDBACK:
                f.add(new DetailField("Họ tên", DocUtils.str(d, "fullName")));
                f.add(new DetailField("Email", DocUtils.str(d, "email")));
                f.add(new DetailField("SĐT", DocUtils.str(d, "phone")));
                f.add(new DetailField("Nội dung", DocUtils.str(d, "message")));
                f.add(new DetailField("Trạng thái", Boolean.TRUE.equals(d.getBoolean("replied")) ? "Đã phản hồi" : "Chờ xử lý"));
                break;
            case BLOGS:
                f.add(new DetailField("Tiêu đề", DocUtils.str(d, "title")));
                f.add(new DetailField("Mô tả ngắn", DocUtils.str(d, "excerpt")));
                f.add(new DetailField("Tác giả", DocUtils.str(d, "authorName")));
                f.add(new DetailField("Trạng thái", "published".equals(DocUtils.str(d, "status")) ? "Đã xuất bản" : "Bản nháp"));
                f.add(new DetailField("Cập nhật", DocUtils.date(d, "updatedAt", "createdAt")));
                break;
            case AUDIT:
                f.add(new DetailField("Hành động", RowFormatter.actionLabel(DocUtils.str(d, "action"))));
                f.add(new DetailField("Người thực hiện", DocUtils.str(d, "actor")));
                f.add(new DetailField("Đối tượng", DocUtils.str(d, "target")));
                f.add(new DetailField("Chi tiết", DocUtils.str(d, "detail")));
                f.add(new DetailField("Thời gian", DocUtils.date(d, "timestamp")));
                break;
            default:
                break;
        }
        return f;
    }
}
