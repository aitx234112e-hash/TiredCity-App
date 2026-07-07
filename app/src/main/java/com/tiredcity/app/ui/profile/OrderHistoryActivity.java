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
import com.tiredcity.app.data.repository.OrderRepository;
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
    private OrderRepository orderRepository;
    private OrderAdapter orderAdapter;
    private final List<Order> allOrders = new ArrayList<>();
    private String currentStatusFilter = null; // null = "Tất cả"

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityOrderHistoryBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        orderRepository = new OrderRepository(null);

        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        binding.toolbar.setNavigationOnClickListener(v -> finish());

        binding.rvOrders.setLayoutManager(new LinearLayoutManager(this));

        binding.swipeRefresh.setOnRefreshListener(this::loadOrders);
        binding.swipeRefresh.setColorSchemeColors(
                getResources().getColor(com.tiredcity.app.R.color.tc_red, getTheme()));

        binding.btnShopNow.setOnClickListener(v -> goShopping());

        setupTabs();

        loadOrders();
    }

    private void setupTabs() {
        // Khởi tạo filter dựa trên tab đang chọn ban đầu
        updateFilterFromTab(binding.tabLayout.getSelectedTabPosition());

        binding.tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                updateFilterFromTab(tab.getPosition());
                filterAndDisplay();
            }
            @Override public void onTabUnselected(TabLayout.Tab tab) {}
            @Override public void onTabReselected(TabLayout.Tab tab) {}
        });
    }

    private void updateFilterFromTab(int position) {
        switch (position) {
            case 0: currentStatusFilter = null; break; // Tất cả
            case 1: currentStatusFilter = Constants.ORDER_PENDING; break; // Chờ xử lý
            case 2: currentStatusFilter = Constants.ORDER_SHIPPING; break; // Đang giao
            case 3: currentStatusFilter = Constants.ORDER_DELIVERED; break; // Đã nhận
            case 4: currentStatusFilter = Constants.ORDER_CANCELLED; break; // Đã hủy
        }
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
                    filterAndDisplay();
                })
                .addOnFailureListener(e -> {
                    binding.swipeRefresh.setRefreshing(false);
                    allOrders.clear();
                    filterAndDisplay();
                });
    }

    private void filterAndDisplay() {
        List<Order> filtered;
        if (currentStatusFilter == null) {
            filtered = allOrders;
        } else {
            filtered = new ArrayList<>();
            for (Order o : allOrders) {
                String status = o.getStatus() != null ? o.getStatus().toUpperCase(Locale.US) : "";

                // Xử lý bộ lọc cho tab "Đang giao" (chấp nhận cả SHIPPING và SHIPPED)
                if (Constants.ORDER_SHIPPING.equals(currentStatusFilter)) {
                    if (status.equals("SHIPPING") || status.equals("SHIPPED")) {
                        filtered.add(o);
                    }
                }
                // Xử lý bộ lọc cho tab "Chờ xử lý" (bao gồm cả PENDING, CONFIRMED, PROCESSING)
                else if (Constants.ORDER_PENDING.equals(currentStatusFilter)) {
                    if (status.equals("PENDING") || status.equals("CONFIRMED") || status.equals("PROCESSING")) {
                        filtered.add(o);
                    }
                }
                else if (currentStatusFilter.equalsIgnoreCase(o.getStatus())) {
                    filtered.add(o);
                }
            }
        }
        displayOrders(filtered);
    }

    /** Cập nhật danh sách / trạng thái rỗng theo kết quả đã lọc. */
    private void displayOrders(List<Order> orders) {
        if (orders.isEmpty()) {
            binding.layoutEmpty.setVisibility(View.VISIBLE);
            binding.swipeRefresh.setVisibility(View.GONE);
        } else {
            binding.layoutEmpty.setVisibility(View.GONE);
            binding.swipeRefresh.setVisibility(View.VISIBLE);
            orderAdapter = new OrderAdapter(orders, new OrderAdapter.OnOrderClickListener() {
                @Override
                public void onOrderClick(Order order) {
                    openOrderTracking(order);
                }

                @Override
                public void onConfirmReceived(Order order) {
                    confirmReceived(order);
                }

                @Override
                public void onReviewOrder(Order order) {
                    openReviewDialog(order);
                }
            });
            binding.rvOrders.setAdapter(orderAdapter);
        }
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

    private void confirmReceived(Order order) {
        new androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Xác nhận đã nhận hàng")
            .setMessage("Bạn chắc chắn đã nhận được hàng và hài lòng với sản phẩm?")
            .setPositiveButton("Xác nhận", (dialog, which) -> {
                binding.swipeRefresh.setRefreshing(true);
                orderRepository.updateOrderStatusInFirestore(order.getId(), "DELIVERED", "Khách hàng xác nhận đã nhận hàng", (success, error) -> {
                    binding.swipeRefresh.setRefreshing(false);
                    if (success) {
                        android.widget.Toast.makeText(this, "Cảm ơn bạn đã xác nhận!", android.widget.Toast.LENGTH_SHORT).show();
                        loadOrders(); // Tải lại danh sách
                    } else {
                        android.widget.Toast.makeText(this, "Lỗi: " + error, android.widget.Toast.LENGTH_SHORT).show();
                    }
                });
            })
            .setNegativeButton("Đóng", null)
            .show();
    }

    private void openReviewDialog(Order order) {
        // Đơn hàng có thể có nhiều sản phẩm. Mở chi tiết đơn để người dùng chọn sản phẩm đánh giá.
        openOrderTracking(order);
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
