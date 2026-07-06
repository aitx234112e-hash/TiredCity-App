package com.tiredcity.app.ui.cart;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import com.tiredcity.app.R;
import com.tiredcity.app.databinding.ActivityOrderSuccessBinding;
import com.tiredcity.app.ui.base.BaseActivity;
import com.tiredcity.app.ui.main.MainActivity;
import com.tiredcity.app.utils.PriceUtils;

public class OrderSuccessActivity extends BaseActivity {

    private ActivityOrderSuccessBinding binding;
    private String orderId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityOrderSuccessBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        orderId = getIntent().getStringExtra("order_id");
        double total = getIntent().getDoubleExtra("order_total", 0);
        String payment = getIntent().getStringExtra("order_payment");
        int itemCount = getIntent().getIntExtra("order_item_count", 0);

        if (orderId != null) {
            binding.tvOrderId.setText(orderId);
        String orderCode = getIntent().getStringExtra("order_code");

        if (orderCode != null) {
            binding.tvOrderId.setText(orderCode);
        } else if (orderId != null) {
            binding.tvOrderId.setText("#" + orderId);
        }
        binding.tvOrderTotal.setText(PriceUtils.formatVnd(total));
        binding.tvOrderItems.setText(getString(R.string.order_success_items_value, itemCount));
        binding.tvOrderPayment.setText(paymentLabel(payment));

        setupSuccessAnimation();

        binding.btnViewOrder.setOnClickListener(v -> viewOrder());
        binding.btnContinue.setOnClickListener(v -> continueShopping());
    }

    /** Chạy Lottie nếu có; nếu lỗi thì giữ ảnh check tĩnh làm dự phòng. */
    private void setupSuccessAnimation() {
        try {
            binding.lottieSuccess.setAnimation("lottie_success.json");
            binding.lottieSuccess.playAnimation();
            binding.imgCheckFallback.setVisibility(View.GONE);
        } catch (Exception e) {
            binding.lottieSuccess.setVisibility(View.GONE);
            binding.imgCheckFallback.setVisibility(View.VISIBLE);
        }
    }

    private String paymentLabel(String method) {
        if (method == null) return "—";
        switch (method) {
            case "COD":           return getString(R.string.pay_method_cod);
            case "BANK_TRANSFER": return getString(R.string.pay_method_bank);
            case "MOMO":          return getString(R.string.pay_method_momo);
            case "CARD":          return getString(R.string.pay_method_card);
            default:              return method;
        }
    }

    private void viewOrder() {
        Intent intent = new Intent(this, OrderTrackingActivity.class);
        intent.putExtra("order_id", orderId);
        startActivity(intent);
        finish();
    }

    private void continueShopping() {
        startActivity(new Intent(this, MainActivity.class));
        finishAffinity();
    }

    @Override
    public void onBackPressed() {
        continueShopping();
    }
}
