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
import com.tiredcity.app.data.repository.OrderMapper;
import com.tiredcity.app.databinding.ActivityOrderHistoryBinding;
import com.tiredcity.app.ui.base.BaseActivity;
import com.tiredcity.app.ui.cart.OrderTrackingActivity;
import com.tiredcity.app.ui.main.MainActivity;
import com.tiredcity.app.utils.Constants;
import com.tiredcity.app.utils.EdgeToEdgeUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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

        // targetSdk 35 vẽ edge-to-edge: đệm header đỏ xuống dưới thanh trạng thái (pin/wifi)
        // để không bị dính/che chữ, xem EdgeToEdgeUtils.
        EdgeToEdgeUtils.applyStatusBarTopPadding(binding.headerRed);

        binding.btnBack.setOnClickListener(v -> finish());
        binding.btnNotification.setOnClickListener(v -> startActivity(new Intent(this,
                com.tiredcity.app.ui.notification.NotificationActivity.class)));
        bindNotifBadge();

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
                        allOrders.add(OrderMapper.mapOrder(doc));
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

    private void openReviewDialog(Order order) {
        // Đơn hàng có thể có nhiều sản phẩm. Mở chi tiết đơn để người dùng chọn sản phẩm đánh giá.
        openOrderTracking(order);
    }

    /** Hiện badge số thông báo chưa đọc trên chuông thông báo, tối đa "9+". */
    private void bindNotifBadge() {
        int count = new com.tiredcity.app.data.local.NotificationStore(this).getUnreadCount();
        if (count <= 0) {
            binding.tvNotifBadge.setVisibility(View.GONE);
        } else {
            binding.tvNotifBadge.setVisibility(View.VISIBLE);
            binding.tvNotifBadge.setText(count > 9 ? "9+" : String.valueOf(count));
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
