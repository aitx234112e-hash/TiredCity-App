package com.tiredcity.app.ui.cart;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.tiredcity.app.adapter.CartAdapter;
import com.tiredcity.app.adapter.RecentlyViewedAdapter;
import com.tiredcity.app.data.local.CartLocalStore;
import com.tiredcity.app.data.local.RecentlyViewedStore;
import com.tiredcity.app.data.model.CartItem;
import com.tiredcity.app.data.model.Product;
import com.tiredcity.app.databinding.ActivityCartBinding;
import com.tiredcity.app.ui.base.BaseActivity;
import com.tiredcity.app.ui.shop.ProductDetailActivity;
import com.tiredcity.app.utils.Constants;
import com.tiredcity.app.utils.PriceUtils;
import java.util.List;

public class CartActivity extends BaseActivity {

    private ActivityCartBinding binding;
    private CartLocalStore cartLocalStore;
    private RecentlyViewedStore recentlyViewedStore;
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
        recentlyViewedStore = new RecentlyViewedStore(this);

        binding.rvCartItems.setLayoutManager(new LinearLayoutManager(this));
        binding.rvRecentlyViewed.setLayoutManager(
                new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        binding.btnCheckout.setOnClickListener(v -> proceedToPayment());

        loadCart();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadCart(); // Refresh when returning from product detail
        loadRecentlyViewed();
    }

    /** Dải "Đã xem gần đây" — chỉ hiện khi khách thật sự đã mở xem sản phẩm nào đó. */
    private void loadRecentlyViewed() {
        List<Product> recent = recentlyViewedStore.getRecentlyViewed();
        if (recent.isEmpty()) {
            binding.llRecentlyViewed.setVisibility(View.GONE);
            return;
        }
        binding.llRecentlyViewed.setVisibility(View.VISIBLE);
        binding.rvRecentlyViewed.setAdapter(new RecentlyViewedAdapter(recent, product -> {
            Intent intent = new Intent(this, ProductDetailActivity.class);
            intent.putExtra(Constants.EXTRA_PRODUCT_ID, product.getId());
            startActivity(intent);
        }));
    }

    private void loadCart() {
        cartItems = cartLocalStore.getCartItems();
        cartAdapter = new CartAdapter(cartItems, item -> {
            cartLocalStore.removeItem(item.getProduct().getId());
            loadCart();
        });
        cartAdapter.setOnCartChangeListener(this::refreshTotal);
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
