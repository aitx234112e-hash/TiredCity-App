package com.tiredcity.app.ui.cart;

import android.content.Intent;
import android.os.Bundle;
import com.tiredcity.app.databinding.ActivityOrderSuccessBinding;
import com.tiredcity.app.ui.base.BaseActivity;
import com.tiredcity.app.ui.main.MainActivity;

public class OrderSuccessActivity extends BaseActivity {

    private ActivityOrderSuccessBinding binding;
    private String orderId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityOrderSuccessBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        orderId = getIntent().getStringExtra("order_id");
        if (orderId != null) {
            binding.tvOrderId.setText("Mã đơn: " + orderId);
        }

        // Start Lottie animation if available
        try {
            binding.lottieSuccess.setAnimation("lottie_success.json");
            binding.lottieSuccess.playAnimation();
        } catch (Exception ignored) {}

        binding.btnViewOrder.setOnClickListener(v -> viewOrder());
        binding.btnContinue.setOnClickListener(v -> continueShopping());
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
