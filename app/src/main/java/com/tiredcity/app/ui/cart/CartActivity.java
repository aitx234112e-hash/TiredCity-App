package com.tiredcity.app.ui.cart;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.tiredcity.app.adapter.CartAdapter;
import com.tiredcity.app.data.local.CartLocalStore;
import com.tiredcity.app.data.model.CartItem;
import com.tiredcity.app.databinding.ActivityCartBinding;
import com.tiredcity.app.ui.base.BaseActivity;
import com.tiredcity.app.utils.PriceUtils;
import java.util.List;

public class CartActivity extends BaseActivity {

    private ActivityCartBinding binding;
    private CartLocalStore cartLocalStore;
    private CartAdapter cartAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityCartBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Giỏ hàng");
        }
        binding.toolbar.setNavigationOnClickListener(v -> finish());

        cartLocalStore = new CartLocalStore(this);

        binding.rvCartItems.setLayoutManager(new LinearLayoutManager(this));
        binding.btnCheckout.setOnClickListener(v -> proceedToPayment());

        loadCart();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadCart(); // Refresh when returning from product detail
    }

    private void loadCart() {
        List<CartItem> items = cartLocalStore.getCartItems();
        cartAdapter = new CartAdapter(items, item -> {
            cartLocalStore.removeItem(item.getProduct().getId());
            loadCart();
        });
        binding.rvCartItems.setAdapter(cartAdapter);

        // Update total
        double total = 0;
        for (CartItem item : items) total += item.getSubtotal();
        binding.tvTotal.setText(PriceUtils.format(total));
        binding.btnCheckout.setEnabled(!items.isEmpty());
    }

    private void proceedToPayment() {
        List<CartItem> items = cartLocalStore.getCartItems();
        if (items.isEmpty()) {
            Toast.makeText(this, "Giỏ hàng trống", Toast.LENGTH_SHORT).show();
            return;
        }
        startActivity(new Intent(this, PaymentActivity.class));
    }
}
