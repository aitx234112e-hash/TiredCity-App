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
        
        orderRepository.getOrderByIdFromFirestore(orderId, new OrderRepository.OnOrderLoadedListener() {
            @Override
            public void onSuccess(Order order) {
                bindOrder(order);
            }

            @Override
            public void onError(String message) {
                // Fallback to API
                loadOrderFromApi(orderId);
            }
        });
    }

    private void loadOrderFromApi(String orderId) {
        orderRepository.getOrderById(orderId).enqueue(new Callback<ApiResponse<Order>>() {
            @Override
            public void onResponse(Call<ApiResponse<Order>> call, Response<ApiResponse<Order>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    bindOrder(response.body().getData());
                } else {
                    Toast.makeText(OrderTrackingActivity.this, getString(R.string.error_loading), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<Order>> call, Throwable t) {
                Toast.makeText(OrderTrackingActivity.this, getString(R.string.error_network), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void bindOrder(Order order) {
        String displayId = order.getOrderCode() != null ? order.getOrderCode() : "#" + order.getId();
        binding.tvOrderId.setText(displayId);
        binding.tvOrderDate.setText(DateUtils.formatDisplay(order.getCreatedAt()));
        binding.tvOrderTotal.setText(PriceUtils.format(order.getTotalPrice()));
        binding.tvShippingAddress.setText(order.getShippingAddress());
        
        // Setup Cancel Button - Only allow cancel if PENDING
        if (Constants.ORDER_PENDING.equalsIgnoreCase(order.getStatus())) {
            binding.btnCancelOrder.setVisibility(android.view.View.VISIBLE);
            binding.btnCancelOrder.setOnClickListener(v -> confirmCancelOrder(order.getId()));
        } else {
            binding.btnCancelOrder.setVisibility(android.view.View.GONE);
        }

        // Setup Confirm Received Button
        String status = order.getStatus() != null ? order.getStatus().toUpperCase() : "";
        if (status.equals("SHIPPING") || status.equals("SHIPPED") || status.equals("PROCESSING")) {
            binding.btnConfirmReceived.setVisibility(android.view.View.VISIBLE);
            binding.btnConfirmReceived.setOnClickListener(v -> confirmReceived(order));
        } else {
            binding.btnConfirmReceived.setVisibility(android.view.View.GONE);
        }

        // Bind items to RecyclerView
        java.util.List<com.tiredcity.app.data.model.CartItem> items = order.getItems();
        if (items != null) {
            binding.rvOrderItems.setLayoutManager(new androidx.recyclerview.widget.LinearLayoutManager(this));
            
            boolean canReview = Constants.ORDER_DELIVERED.equalsIgnoreCase(order.getStatus());
            binding.rvOrderItems.setAdapter(new com.tiredcity.app.adapter.CheckoutItemAdapter(items, canReview, item -> {
                if (item.getProduct() != null) {
                    android.content.Intent intent = new android.content.Intent(this, com.tiredcity.app.ui.shop.ProductDetailActivity.class);
                    intent.putExtra(Constants.EXTRA_PRODUCT_ID, item.getProduct().getId());
                    startActivity(intent);
                }
            }));
        }

        if (Constants.ORDER_CANCELLED.equals(order.getStatus())) {
            binding.stepDelivered.tvStepTitle.setText("ĐÃ HỦY");
            binding.stepDelivered.tvStepTitle.setTextColor(getResources().getColor(android.R.color.holo_red_dark, getTheme()));
            binding.stepDelivered.vStepDot.setBackgroundResource(R.drawable.tc_bg_circle_red);
        } else {
            updateTrackingSteps(order.getStatus());
        }
    }

    private void confirmCancelOrder(String orderId) {
        new androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Xác nhận hủy đơn")
            .setMessage("Bạn có chắc chắn muốn hủy đơn hàng này không? Hành động này không thể hoàn tác.")
            .setPositiveButton("Hủy đơn", (dialog, which) -> {
                binding.btnCancelOrder.setEnabled(false);
                binding.btnCancelOrder.setText("Đang xử lý...");
                
                orderRepository.cancelOrderInFirestore(orderId, "Khách hàng tự hủy", (success, error) -> {
                    if (success) {
                        Toast.makeText(this, "Đã hủy đơn hàng thành công", Toast.LENGTH_SHORT).show();
                        loadOrder(orderId); // Reload UI
                    } else {
                        binding.btnCancelOrder.setEnabled(true);
                        binding.btnCancelOrder.setText("Hủy đơn hàng");
                        Toast.makeText(this, "Lỗi: " + error, Toast.LENGTH_LONG).show();
                    }
                });
            })
            .setNegativeButton("Quay lại", null)
            .show();
    }

    private void confirmReceived(Order order) {
        new androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Xác nhận đã nhận hàng")
            .setMessage("Bạn chắc chắn đã nhận được hàng và hài lòng với sản phẩm?")
            .setPositiveButton("Xác nhận", (dialog, which) -> {
                binding.btnConfirmReceived.setEnabled(false);
                binding.btnConfirmReceived.setText("Đang xử lý...");
                
                orderRepository.updateOrderStatusInFirestore(order.getId(), "DELIVERED", "Khách hàng xác nhận đã nhận hàng", (success, error) -> {
                    if (success) {
                        Toast.makeText(this, "Cảm ơn bạn đã xác nhận!", Toast.LENGTH_SHORT).show();
                        loadOrder(order.getId()); // Reload UI
                    } else {
                        binding.btnConfirmReceived.setEnabled(true);
                        binding.btnConfirmReceived.setText("Đã nhận được hàng");
                        Toast.makeText(this, "Lỗi: " + error, Toast.LENGTH_LONG).show();
                    }
                });
            })
            .setNegativeButton("Đóng", null)
            .show();
    }

    private void updateTrackingSteps(String status) {
        // Status progression: PENDING → CONFIRMED → SHIPPING → DELIVERED
        boolean isPending   = true;
        boolean isConfirmed = !Constants.ORDER_PENDING.equals(status);
        boolean isShipping  = Constants.ORDER_SHIPPING.equals(status)
                              || Constants.ORDER_DELIVERED.equals(status);
        boolean isDelivered = Constants.ORDER_DELIVERED.equals(status);

        applyStep(binding.stepPlaced,    getString(R.string.track_step_placed),    isPending);
        applyStep(binding.stepConfirmed, getString(R.string.track_step_confirmed), isConfirmed);
        applyStep(binding.stepShipping,  getString(R.string.track_step_shipping),  isShipping);
        applyStep(binding.stepDelivered, getString(R.string.track_step_delivered), isDelivered);
    }

    private void applyStep(ItemTrackingStepBinding step, String label, boolean active) {
        step.tvStepTitle.setText(label);
        step.tvStepTitle.setTextColor(getResources().getColor(
            active ? R.color.text_primary : R.color.text_hint, getTheme()));
        step.vStepDot.setBackgroundResource(
            active ? R.drawable.tc_bg_circle_red : R.drawable.tc_bg_circle_sand);
    }
}
