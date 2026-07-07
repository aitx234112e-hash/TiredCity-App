package com.tiredcity.app.data.repository;

import com.google.firebase.firestore.DocumentSnapshot;
import com.tiredcity.app.data.model.Order;
import com.tiredcity.app.data.model.OrderItemPreview;
import com.tiredcity.app.utils.Constants;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

/**
 * Map 1 document Firestore (collection "orders", ghi bởi PaymentActivity) sang model Order.
 * Dùng chung cho OrderHistoryActivity (danh sách) và OrderTrackingActivity (chi tiết) để
 * cả 2 nơi luôn đọc đúng cùng 1 schema, tránh lệch khi đổi field ở 1 nơi mà quên nơi kia.
 */
public final class OrderMapper {

    private OrderMapper() {}

    public static Order mapOrder(DocumentSnapshot doc) {
        Order order = new Order();
        order.setId(doc.getId());
        order.setStatus(normalizeStatus(safeString(doc.get("status"))));
        order.setPaymentMethod(safeString(doc.get("paymentMethod")));
        order.setShippingAddress(safeString(doc.get("shippingAddress")));

        Object total = doc.get("totalPrice");
        order.setTotalPrice(total instanceof Number ? ((Number) total).doubleValue() : 0);

        Object items = doc.get("items");
        List<OrderItemPreview> previews = new ArrayList<>();
        if (items instanceof List) {
            for (Object raw : (List<?>) items) {
                if (raw instanceof Map) previews.add(mapPreview((Map<?, ?>) raw));
            }
        }
        order.setItemCount(previews.size());
        order.setPreviewItems(previews);

        order.setCreatedAt(parseCreatedAt(doc.get("createdAt")));
        return order;
    }

    /** createdAt có thể là String ISO (app ghi) hoặc Firestore Timestamp/Date (web-admin ghi). */
    private static Date parseCreatedAt(Object raw) {
        if (raw instanceof com.google.firebase.Timestamp) return ((com.google.firebase.Timestamp) raw).toDate();
        if (raw instanceof Date) return (Date) raw;
        if (raw instanceof String) return parseIso((String) raw);
        return null;
    }

    /** Map 1 phần tử trong mảng items của Firestore → OrderItemPreview. */
    private static OrderItemPreview mapPreview(Map<?, ?> m) {
        String name  = safeString(m.get("product_name"));
        String image = safeString(m.get("image"));
        String size  = safeString(m.get("size"));
        String color = safeString(m.get("color"));
        int qty = m.get("quantity") instanceof Number ? ((Number) m.get("quantity")).intValue() : 1;
        double line = m.get("lineTotal") instanceof Number ? ((Number) m.get("lineTotal")).doubleValue() : 0;
        return new OrderItemPreview(name, image, size, color, qty, line);
    }

    /** Trả String nếu là chuỗi; null nếu rỗng — tránh crash khi field là kiểu khác. */
    private static String safeString(Object o) {
        return o instanceof String ? (String) o : (o != null ? String.valueOf(o) : null);
    }

    /** Chuẩn hoá status chữ thường của web/app về hằng số Constants (chữ hoa). */
    private static String normalizeStatus(String raw) {
        if (raw == null) return Constants.ORDER_PENDING;
        switch (raw.toLowerCase(Locale.US)) {
            case "processing":
            case "confirmed":  return Constants.ORDER_CONFIRMED;
            case "shipped":
            case "shipping":   return Constants.ORDER_SHIPPING;
            case "delivered":
            case "received":   return Constants.ORDER_DELIVERED;
            case "cancelled":
            case "canceled":   return Constants.ORDER_CANCELLED;
            default:           return Constants.ORDER_PENDING;
        }
    }

    private static Date parseIso(String iso) {
        if (iso == null || iso.isEmpty()) return null;
        // Định dạng PaymentActivity ghi: yyyy-MM-dd'T'HH:mm:ss.SSS'Z' (UTC)
        SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US);
        fmt.setTimeZone(TimeZone.getTimeZone("UTC"));
        try {
            return fmt.parse(iso);
        } catch (Exception e) {
            return null;
        }
    }
}
