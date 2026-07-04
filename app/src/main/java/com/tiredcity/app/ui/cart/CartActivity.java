package com.tiredcity.app.ui.cart;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.tiredcity.app.adapter.CartAdapter;
import com.tiredcity.app.data.local.CartLocalStore;
import com.tiredcity.app.data.model.CartItem;
import com.tiredcity.app.data.network.ApiClient;
import com.tiredcity.app.data.repository.CartRepository;
import com.tiredcity.app.databinding.ActivityCartBinding;
import com.tiredcity.app.ui.base.BaseActivity;
import com.tiredcity.app.utils.PriceUtils;
import java.util.List;

public class CartActivity extends BaseActivity {

    private ActivityCartBinding binding;
    private CartLocalStore cartLocalStore;
    private CartRepository cartRepository;
    private CartAdapter cartAdapter;
    private List<CartItem> cartItems;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityCartBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(getString(com.tiredcity.app.R.string.cart_title));
        }
        binding.toolbar.setNavigationOnClickListener(v -> finish());

        cartLocalStore = new CartLocalStore(this);
        cartRepository = new CartRepository(ApiClient.getApiService(null), cartLocalStore);

        binding.rvCartItems.setLayoutManager(new LinearLayoutManager(this));
        binding.btnCheckout.setOnClickListener(v -> proceedToPayment());

        // Đồng bộ từ Cloud về trước khi hiện
        cartRepository.fetchCartFromCloud(items -> runOnUiThread(this::loadCart));

        loadCart();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadCart(); // Refresh when returning from product detail
    }

    private void loadCart() {
        cartItems = cartLocalStore.getCartItems();
        cartAdapter = new CartAdapter(cartItems, item -> {
            cartLocalStore.removeItem(item.getProduct().getId());
            cartRepository.syncCartToCloud(); // Sync xóa
            loadCart();
        });
        cartAdapter.setOnCartChangeListener(() -> {
            cartRepository.syncCartToCloud(); // Sync thay đổi số lượng/chọn
            refreshTotal();
        });
        binding.rvCartItems.setAdapter(cartAdapter);

        refreshTotal();
    }

    private void refreshTotal() {
        double total = 0;
        boolean anySelected = false;
        for (CartItem item : cartItems) {
            if (item.isSelected()) {
                total += item.getSubtotal();
                anySelected = true;
            }
        }
        binding.tvTotal.setText(PriceUtils.format(total));
        binding.btnCheckout.setEnabled(!cartItems.isEmpty() && anySelected);
    }

    private void proceedToPayment() {
        boolean anySelected = false;
        for (CartItem item : cartItems) {
            if (item.isSelected()) { anySelected = true; break; }
        }
        if (cartItems.isEmpty() || !anySelected) {
            Toast.makeText(this, getString(com.tiredcity.app.R.string.error_empty_cart), Toast.LENGTH_SHORT).show();
            return;
        }
        cartLocalStore.saveCartItems(cartItems);
        startActivity(new Intent(this, PaymentActivity.class));
    }
}
