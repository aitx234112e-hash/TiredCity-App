package com.tiredcity.app.ui.cart;

import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;
import com.tiredcity.app.R;
import com.tiredcity.app.data.model.ApiResponse;
import com.tiredcity.app.data.model.Order;
import com.tiredcity.app.data.network.ApiClient;
import com.tiredcity.app.data.repository.OrderRepository;
import com.tiredcity.app.databinding.ActivityOrderTrackingBinding;
import com.tiredcity.app.databinding.ItemTrackingStepBinding;
import com.tiredcity.app.ui.base.BaseActivity;
import com.tiredcity.app.utils.Constants;
import com.tiredcity.app.utils.DateUtils;
import com.tiredcity.app.utils.PriceUtils;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class OrderTrackingActivity extends BaseActivity {

    private ActivityOrderTrackingBinding binding;
    private OrderRepository orderRepository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityOrderTrackingBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        binding.toolbar.setNavigationOnClickListener(v -> finish());

        orderRepository = new OrderRepository(ApiClient.getApiService(preferenceManager.getToken()));

        String orderId = getIntent().getStringExtra("order_id");
        loadOrder(orderId);
    }

    private void loadOrder(String orderId) {
        if (orderId == null) { finish(); return; }
        orderRepository.getOrderById(orderId).enqueue(new Callback<ApiResponse<Order>>() {
            @Override
            public void onResponse(Call<ApiResponse<Order>> call, Response<ApiResponse<Order>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    bindOrder(response.body().getData());
                } else {
                    Toast.makeText(OrderTrackingActivity.this, "Không tải được đơn hàng", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<Order>> call, Throwable t) {
                Toast.makeText(OrderTrackingActivity.this, "Lỗi mạng", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void bindOrder(Order order) {
        binding.tvOrderId.setText("#" + order.getId());
        binding.tvOrderDate.setText(DateUtils.formatDisplay(order.getCreatedAt()));
        binding.tvOrderTotal.setText(PriceUtils.format(order.getTotalPrice()));
        binding.tvShippingAddress.setText(order.getShippingAddress());
        updateTrackingSteps(order.getStatus());
    }

    private void updateTrackingSteps(String status) {
        // Status progression: PENDING → CONFIRMED → SHIPPING → DELIVERED
        boolean isPending   = true;
        boolean isConfirmed = !Constants.ORDER_PENDING.equals(status);
        boolean isShipping  = Constants.ORDER_SHIPPING.equals(status)
                              || Constants.ORDER_DELIVERED.equals(status);
        boolean isDelivered = Constants.ORDER_DELIVERED.equals(status);

        applyStep(binding.stepPlaced,    "Đã đặt hàng",    isPending);
        applyStep(binding.stepConfirmed, "Đã xác nhận",    isConfirmed);
        applyStep(binding.stepShipping,  "Đang giao hàng", isShipping);
        applyStep(binding.stepDelivered, "Đã nhận hàng",   isDelivered);
    }

    private void applyStep(ItemTrackingStepBinding step, String label, boolean active) {
        step.tvStepTitle.setText(label);
        step.tvStepTitle.setTextColor(getResources().getColor(
            active ? R.color.text_primary : R.color.text_hint, getTheme()));
        step.vStepDot.setBackgroundResource(
            active ? R.drawable.bg_circle_gold : R.drawable.bg_rounded_surface);
    }
}
