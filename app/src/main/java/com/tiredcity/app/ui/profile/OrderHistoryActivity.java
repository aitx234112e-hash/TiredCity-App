package com.tiredcity.app.ui.profile;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.tiredcity.app.adapter.OrderAdapter;
import com.tiredcity.app.data.model.ApiListResponse;
import com.tiredcity.app.data.model.Order;
import com.tiredcity.app.data.network.ApiClient;
import com.tiredcity.app.data.repository.OrderRepository;
import com.tiredcity.app.databinding.ActivityOrderHistoryBinding;
import com.tiredcity.app.ui.base.BaseActivity;
import com.tiredcity.app.ui.cart.OrderTrackingActivity;
import com.tiredcity.app.ui.main.MainActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.android.material.tabs.TabLayout;
import com.tiredcity.app.utils.Constants;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class OrderHistoryActivity extends BaseActivity {

    private ActivityOrderHistoryBinding binding;
    private OrderRepository orderRepository;
    private OrderAdapter orderAdapter;
    private List<Order> allOrders = new ArrayList<>();
    private String currentStatusFilter = null; // null means "All"

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityOrderHistoryBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        binding.toolbar.setNavigationOnClickListener(v -> finish());

        orderRepository = new OrderRepository(ApiClient.getApiService(preferenceManager.getToken()));

        binding.rvOrders.setLayoutManager(new LinearLayoutManager(this));

        binding.swipeRefresh.setOnRefreshListener(this::loadOrders);
        binding.swipeRefresh.setColorSchemeColors(
            getResources().getColor(com.tiredcity.app.R.color.tc_red, getTheme()));

        setupTabs();
        
        binding.btnShopNow.setOnClickListener(v -> {
            loadOrders(); // Thử tải lại thay vì đi shopping nếu đang lỗi
        });

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
        
        // Ưu tiên dùng UID từ Firebase Auth trực tiếp
        String userId = null;
        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        }
        
        if (userId == null) {
            userId = preferenceManager.getUserId();
        }
        
        android.util.Log.d("OrderHistory", "Loading orders for userId: " + userId);
        
        if (userId == null) {
            binding.swipeRefresh.setRefreshing(false);
            displayOrders(new ArrayList<>());
            return;
        }

        final String finalUserId = userId;
        orderRepository.getOrdersFromFirestore(userId, new OrderRepository.OnOrdersLoadedListener() {
            @Override
            public void onSuccess(List<Order> orders) {
                binding.swipeRefresh.setRefreshing(false);
                if (orders != null && !orders.isEmpty()) {
                    Collections.sort(orders, (o1, o2) -> {
                        if (o1.getCreatedAt() == null || o2.getCreatedAt() == null) return 0;
                        return o2.getCreatedAt().compareTo(o1.getCreatedAt());
                    });
                    allOrders = orders;
                } else {
                    allOrders = new ArrayList<>();
                }
                android.util.Log.d("OrderHistory", "Loaded " + allOrders.size() + " orders for " + finalUserId);
                filterAndDisplay();
            }

            @Override
            public void onError(String message) {
                binding.swipeRefresh.setRefreshing(false);
                android.util.Log.e("OrderHistory", "Firestore Error: " + message);
                // Fallback to API if Firestore fails
                loadOrdersFromApi();
            }
        });
    }

    private void filterAndDisplay() {
        List<Order> filtered;
        if (currentStatusFilter == null) {
            filtered = allOrders;
        } else {
            filtered = new ArrayList<>();
            for (Order o : allOrders) {
                if (currentStatusFilter.equalsIgnoreCase(o.getStatus())) {
                    filtered.add(o);
                }
            }
        }
        android.util.Log.d("OrderHistory", "Filtering status: " + currentStatusFilter + " -> " + filtered.size() + " items");
        displayOrders(filtered);
    }

    private void loadOrdersFromApi() {
        orderRepository.getOrders().enqueue(new Callback<ApiListResponse<Order>>() {
            @Override
            public void onResponse(Call<ApiListResponse<Order>> call, Response<ApiListResponse<Order>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    displayOrders(response.body().getData());
                }
            }
            @Override public void onFailure(Call<ApiListResponse<Order>> call, Throwable t) { }
        });
    }

    private void displayOrders(List<Order> orders) {
        if (orders == null || orders.isEmpty()) {
            binding.layoutEmpty.setVisibility(View.VISIBLE);
            binding.swipeRefresh.setVisibility(View.GONE);
        } else {
            binding.layoutEmpty.setVisibility(View.GONE);
            binding.swipeRefresh.setVisibility(View.VISIBLE);
            orderAdapter = new OrderAdapter(orders, this::openOrderTracking);
            binding.rvOrders.setAdapter(orderAdapter);
        }
    }

    private void openOrderTracking(Order order) {
        Intent intent = new Intent(this, OrderTrackingActivity.class);
        intent.putExtra("order_id", order.getId());
        startActivity(intent);
    }
}
