package com.tiredcity.app.ui.profile;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.android.material.tabs.TabLayout;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.tiredcity.app.adapter.OrderAdapter;
import com.tiredcity.app.data.model.Order;
import com.tiredcity.app.data.model.OrderItemPreview;
import com.tiredcity.app.databinding.ActivityOrderHistoryBinding;
import com.tiredcity.app.ui.base.BaseActivity;
import com.tiredcity.app.ui.cart.OrderTrackingActivity;
import com.tiredcity.app.ui.main.MainActivity;
import com.tiredcity.app.utils.Constants;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

/**
 * Lịch sử đơn hàng — đọc thẳng từ Cloud Firestore (collection "orders"),
 * lọc theo email khách hàng, sắp xếp mới nhất trước. Tabs lọc theo trạng thái.
 */
public class OrderHistoryActivity extends BaseActivity {

    private ActivityOrderHistoryBinding binding;
    private final List<Order> allOrders = new ArrayList<>();
    private int selectedTab = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityOrderHistoryBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        binding.toolbar.setNavigationOnClickListener(v -> finish());

        binding.rvOrders.setLayoutManager(new LinearLayoutManager(this));

        binding.swipeRefresh.setOnRefreshListener(this::loadOrders);
        binding.swipeRefresh.setColorSchemeColors(
                getResources().getColor(com.tiredcity.app.R.color.tc_red, getTheme()));

        binding.tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override public void onTabSelected(TabLayout.Tab tab) {
                selectedTab = tab.getPosition();
                renderList();
            }
            @Override public void onTabUnselected(TabLayout.Tab tab) {}
            @Override public void onTabReselected(TabLayout.Tab tab) {}
        });

        binding.btnShopNow.setOnClickListener(v -> goShopping());

        loadOrders();
    }

    private void loadOrders() {
        binding.swipeRefresh.setRefreshing(true);

        String email = preferenceManager.getUser() != null
                ? preferenceManager.getUser().getEmail() : null;

        // Đọc tất cả đơn rồi ƯU TIÊN đơn khớp email khách hiện tại. Nếu phiên đăng nhập
        // (offline) này không khớp đơn nào — ví dụ vừa cài lại app đổi danh tính — thì hiện
        // tất cả để danh sách không bị trống oan. Đơn đặt mới sẽ tự khớp email và thu hẹp lại.
        FirebaseFirestore.getInstance().collection("orders").get()
                .addOnSuccessListener(snap -> {
                    List<QueryDocumentSnapshot> matching = new ArrayList<>();
                    List<QueryDocumentSnapshot> all = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : snap) {
                        all.add(doc);
                        if (email != null && email.equalsIgnoreCase(doc.getString("userEmail"))) {
                            matching.add(doc);
                        }
                    }
                    List<QueryDocumentSnapshot> use = !matching.isEmpty() ? matching : all;

                    allOrders.clear();
                    for (QueryDocumentSnapshot doc : use) {
                        allOrders.add(mapOrder(doc));
                    }
                    // Mới nhất lên đầu (sắp xếp phía client để khỏi cần composite index).
                    Collections.sort(allOrders, (a, b) -> {
                        long ta = a.getCreatedAt() != null ? a.getCreatedAt().getTime() : 0;
                        long tb = b.getCreatedAt() != null ? b.getCreatedAt().getTime() : 0;
                        return Long.compare(tb, ta);
                    });
                    binding.swipeRefresh.setRefreshing(false);
                    renderList();
                })
                .addOnFailureListener(e -> {
                    binding.swipeRefresh.setRefreshing(false);
                    allOrders.clear();
                    renderList();
                });
    }

    /** Map 1 document Firestore → model Order (chỉ các field cần cho danh sách). */
    private Order mapOrder(QueryDocumentSnapshot doc) {
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
    private java.util.Date parseCreatedAt(Object raw) {
        if (raw instanceof com.google.firebase.Timestamp) return ((com.google.firebase.Timestamp) raw).toDate();
        if (raw instanceof java.util.Date) return (java.util.Date) raw;
        if (raw instanceof String) return parseIso((String) raw);
        return null;
    }

    /** Map 1 phần tử trong mảng items của Firestore → OrderItemPreview. */
    private OrderItemPreview mapPreview(Map<?, ?> m) {
        String name  = safeString(m.get("product_name"));
        String image = safeString(m.get("image"));
        String size  = safeString(m.get("size"));
        String color = safeString(m.get("color"));
        int qty = m.get("quantity") instanceof Number ? ((Number) m.get("quantity")).intValue() : 1;
        double line = m.get("lineTotal") instanceof Number ? ((Number) m.get("lineTotal")).doubleValue() : 0;
        return new OrderItemPreview(name, image, size, color, qty, line);
    }

    /** Trả String nếu là chuỗi; null nếu rỗng — tránh crash khi field là kiểu khác. */
    private String safeString(Object o) {
        return o instanceof String ? (String) o : (o != null ? String.valueOf(o) : null);
    }

    /** Chuẩn hoá status chữ thường của web/app về hằng số Constants (chữ hoa). */
    private String normalizeStatus(String raw) {
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

    private java.util.Date parseIso(String iso) {
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

    /** Áp bộ lọc theo tab đang chọn rồi cập nhật danh sách / trạng thái rỗng. */
    private void renderList() {
        List<Order> filtered = new ArrayList<>();
        for (Order o : allOrders) {
            if (matchesTab(o.getStatus(), selectedTab)) filtered.add(o);
        }

        if (filtered.isEmpty()) {
            binding.layoutEmpty.setVisibility(View.VISIBLE);
            binding.swipeRefresh.setVisibility(View.GONE);
        } else {
            binding.layoutEmpty.setVisibility(View.GONE);
            binding.swipeRefresh.setVisibility(View.VISIBLE);
            binding.rvOrders.setAdapter(new OrderAdapter(filtered, this::openOrderTracking));
        }
    }

    /** tab: 0 Tất cả · 1 Chờ xử lý · 2 Đang giao · 3 Đã nhận · 4 Đã hủy */
    private boolean matchesTab(String status, int tab) {
        switch (tab) {
            case 1: return Constants.ORDER_PENDING.equals(status)
                        || Constants.ORDER_CONFIRMED.equals(status);
            case 2: return Constants.ORDER_SHIPPING.equals(status);
            case 3: return Constants.ORDER_DELIVERED.equals(status);
            case 4: return Constants.ORDER_CANCELLED.equals(status);
            default: return true;
        }
    }

    private void openOrderTracking(Order order) {
        Intent intent = new Intent(this, OrderTrackingActivity.class);
        intent.putExtra("order_id", order.getId());
        startActivity(intent);
    }

    private void goShopping() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(intent);
        finish();
    }
}
