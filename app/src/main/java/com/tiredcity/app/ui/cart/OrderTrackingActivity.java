package com.tiredcity.app.ui.cart;

import android.os.Bundle;
import android.widget.Toast;

import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.tiredcity.app.R;
import com.tiredcity.app.adapter.OrderItemAdapter;
import com.tiredcity.app.data.model.Order;
import com.tiredcity.app.data.repository.OrderMapper;
import com.tiredcity.app.databinding.ActivityOrderTrackingBinding;
import com.tiredcity.app.databinding.ItemTrackingStepBinding;
import com.tiredcity.app.ui.base.BaseActivity;
import com.tiredcity.app.utils.Constants;
import com.tiredcity.app.utils.DateUtils;
import com.tiredcity.app.utils.PriceUtils;

/**
 * Chi tiết theo dõi 1 đơn hàng — đọc thẳng document Firestore theo id (đơn được
 * PaymentActivity ghi trực tiếp vào Firestore, không qua REST API), dùng chung cách
 * map dữ liệu với OrderHistoryActivity qua {@link OrderMapper}.
 */
public class OrderTrackingActivity extends BaseActivity {

    private ActivityOrderTrackingBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityOrderTrackingBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        binding.toolbar.setNavigationOnClickListener(v -> finish());

        binding.rvOrderItems.setLayoutManager(new LinearLayoutManager(this));

        String orderId = getIntent().getStringExtra("order_id");
        loadOrder(orderId);
    }

    private void loadOrder(String orderId) {
        if (orderId == null) { finish(); return; }
        FirebaseFirestore.getInstance().collection("orders").document(orderId).get()
                .addOnSuccessListener(this::onOrderLoaded)
                .addOnFailureListener(e -> {
                    Toast.makeText(this, getString(R.string.error_network), Toast.LENGTH_SHORT).show();
                    finish();
                });
    }

    private void onOrderLoaded(DocumentSnapshot doc) {
        if (!doc.exists()) {
            Toast.makeText(this, getString(R.string.error_loading), Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        bindOrder(OrderMapper.mapOrder(doc));
    }

    private void bindOrder(Order order) {
        String displayId = order.getOrderCode() != null ? order.getOrderCode() : "#" + order.getId();
        binding.tvOrderId.setText(displayId);
        binding.tvOrderDate.setText(DateUtils.formatDisplay(order.getCreatedAt()));
        binding.tvOrderTotal.setText(PriceUtils.format(order.getTotalPrice()));
        binding.tvShippingAddress.setText(order.getShippingAddress());
        binding.rvOrderItems.setAdapter(new OrderItemAdapter(order.getPreviewItems()));
        updateTrackingSteps(order.getStatus(), order.getCreatedAt());
    }

    private void updateTrackingSteps(String status, java.util.Date createdAt) {
        // Status progression: PENDING → CONFIRMED → SHIPPING → DELIVERED
        boolean isPending   = true;
        boolean isConfirmed = !status.equalsIgnoreCase("PENDING");
        boolean isShipping  = status.equalsIgnoreCase("SHIPPING") 
                              || status.equalsIgnoreCase("SHIPPED")
                              || status.equalsIgnoreCase("DELIVERED");
        boolean isDelivered = status.equalsIgnoreCase("DELIVERED");

        // Chỉ có mốc thời gian thật cho bước "Đặt hàng" (createdAt) — các bước sau không có
        // timestamp riêng trong Firestore nên không bịa giờ giả cho chúng.
        applyStep(binding.stepPlaced, getString(R.string.track_step_placed), isPending,
                DateUtils.formatDisplayDateTime(createdAt));
        applyStep(binding.stepConfirmed, getString(R.string.track_step_confirmed), isConfirmed, null);
        applyStep(binding.stepShipping, getString(R.string.track_step_shipping), isShipping, null);
        applyStep(binding.stepDelivered, getString(R.string.track_step_delivered), isDelivered, null);
    }

    private void applyStep(ItemTrackingStepBinding step, String label, boolean active, String time) {
        step.tvStepTitle.setText(label);
        step.tvStepTitle.setTextColor(getResources().getColor(
            active ? R.color.text_primary : R.color.text_hint, getTheme()));
        step.vStepDot.setBackgroundResource(
            active ? R.drawable.tc_bg_circle_red : R.drawable.tc_bg_circle_sand);

        if (time != null && !time.isEmpty()) {
            step.tvStepTime.setVisibility(android.view.View.VISIBLE);
            step.tvStepTime.setText(time);
        } else {
            step.tvStepTime.setVisibility(android.view.View.GONE);
        }
    }
}
