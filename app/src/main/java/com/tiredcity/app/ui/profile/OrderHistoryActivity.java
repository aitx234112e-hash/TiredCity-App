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
import java.util.List;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class OrderHistoryActivity extends BaseActivity {

    private ActivityOrderHistoryBinding binding;
    private OrderRepository orderRepository;
    private OrderAdapter orderAdapter;

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
            getResources().getColor(com.tiredcity.app.R.color.brand_gold, getTheme()));

        loadOrders();
    }

    private void loadOrders() {
        binding.swipeRefresh.setRefreshing(true);
        orderRepository.getOrders().enqueue(new Callback<ApiListResponse<Order>>() {
            @Override
            public void onResponse(Call<ApiListResponse<Order>> call, Response<ApiListResponse<Order>> response) {
                binding.swipeRefresh.setRefreshing(false);
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    displayOrders(response.body().getData());
                }
            }

            @Override
            public void onFailure(Call<ApiListResponse<Order>> call, Throwable t) {
                binding.swipeRefresh.setRefreshing(false);
            }
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
