package com.tiredcity.app.ui.cart;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.tiredcity.app.R;
import com.tiredcity.app.adapter.CartAdapter;
import com.tiredcity.app.adapter.RecentlyViewedAdapter;
import com.tiredcity.app.data.local.CartLocalStore;
import com.tiredcity.app.data.local.RecentlyViewedStore;
import com.tiredcity.app.data.model.CartItem;
import com.tiredcity.app.data.model.Product;
import com.tiredcity.app.data.network.ApiClient;
import com.tiredcity.app.data.repository.CartRepository;
import com.tiredcity.app.databinding.ActivityCartBinding;
import com.tiredcity.app.ui.base.BaseActivity;
import com.tiredcity.app.ui.reward.RewardActivity;
import com.tiredcity.app.ui.shop.ProductDetailActivity;
import com.tiredcity.app.ui.styling.ChatBotActivity;
import com.tiredcity.app.utils.Constants;
import com.tiredcity.app.utils.PriceUtils;
import java.util.List;

public class CartActivity extends BaseActivity {

    /** Số sản phẩm "Đã xem gần đây" hiện khi dải còn thu gọn. */
    private static final int RECENT_COLLAPSED_COUNT = 4;

    private ActivityCartBinding binding;
    private CartLocalStore cartLocalStore;
    private RecentlyViewedStore recentlyViewedStore;
    private CartRepository cartRepository;
    private CartAdapter cartAdapter;
    private List<CartItem> cartItems;

    private boolean recentExpanded;
    /** Chặn vòng lặp khi "Tất cả" tự đổi trạng thái theo các dòng bên trong. */
    private boolean suppressSelectAllCallback;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityCartBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowTitleEnabled(false); // tiêu đề dùng tv_cart_title
        }
        binding.toolbar.setNavigationOnClickListener(v -> finish());
        applySystemBarInsets();

        cartLocalStore = new CartLocalStore(this);
        recentlyViewedStore = new RecentlyViewedStore(this);
        cartRepository = new CartRepository(ApiClient.getApiService(null), cartLocalStore);

        binding.rvCartItems.setLayoutManager(new LinearLayoutManager(this));
        binding.rvRecentlyViewed.setLayoutManager(
                new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));

        binding.btnCheckout.setOnClickListener(v -> proceedToPayment());
        binding.btnEdit.setOnClickListener(v -> toggleEditMode());
        binding.btnChat.setOnClickListener(v -> {
            binding.dotChatUnread.setVisibility(View.GONE);
            startActivity(new Intent(this, ChatBotActivity.class));
        });
        binding.rowVoucher.setOnClickListener(v -> startActivity(new Intent(this, RewardActivity.class)));
        binding.btnRecentSeeAll.setOnClickListener(v -> {
            recentExpanded = !recentExpanded;
            loadRecentlyViewed();
        });
        binding.cbSelectAll.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (suppressSelectAllCallback) return;
            for (CartItem item : cartItems) item.setSelected(isChecked);
            cartAdapter.notifyDataSetChanged();
            cartRepository.syncCartToCloud();
            refreshTotal();
        });

        loadCart();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadCart(); // Refresh when returning from product detail
        loadRecentlyViewed();
    }

    /**
     * Edge-to-edge (targetSdk 35): status bar đè lên header, nav bar đè lên dòng cuối trang.
     * Đẩy header xuống bằng padding trên (dải status bar vẫn đỏ vì là nền của Toolbar) và
     * chừa padding dưới cho thanh điều hướng.
     */
    private void applySystemBarInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.getRoot(), (v, windowInsets) -> {
            Insets bars = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());
            binding.toolbar.setPadding(0, bars.top, 0, 0);
            v.setPadding(bars.left, 0, bars.right, bars.bottom);
            return windowInsets;
        });
    }

    /** "Sửa" ⇄ "Xong": bật/tắt nút thùng rác trên từng dòng sản phẩm. */
    private void toggleEditMode() {
        boolean editing = !cartAdapter.isEditMode();
        cartAdapter.setEditMode(editing);
        binding.btnEdit.setText(editing ? R.string.cart_edit_done : R.string.cart_edit);
    }

    /**
     * Dải "Đã xem gần đây" — chỉ hiện khi khách thật sự đã mở xem sản phẩm nào đó.
     * Thu gọn còn {@link #RECENT_COLLAPSED_COUNT} sản phẩm; "Xem tất cả" mở hết danh sách.
     */
    private void loadRecentlyViewed() {
        List<Product> recent = recentlyViewedStore.getRecentlyViewed();
        if (recent.isEmpty()) {
            binding.llRecentlyViewed.setVisibility(View.GONE);
            return;
        }
        binding.llRecentlyViewed.setVisibility(View.VISIBLE);

        boolean hasMore = recent.size() > RECENT_COLLAPSED_COUNT;
        binding.btnRecentSeeAll.setVisibility(hasMore ? View.VISIBLE : View.GONE);
        binding.tvRecentSeeAll.setText(recentExpanded ? R.string.cart_recent_collapse : R.string.see_all);

        List<Product> shown = (hasMore && !recentExpanded)
                ? recent.subList(0, RECENT_COLLAPSED_COUNT)
                : recent;

        binding.rvRecentlyViewed.setAdapter(new RecentlyViewedAdapter(shown, product -> {
            Intent intent = new Intent(this, ProductDetailActivity.class);
            intent.putExtra(Constants.EXTRA_PRODUCT_ID, product.getId());
            startActivity(intent);
        }));
    }

    private void loadCart() {
        boolean editing = cartAdapter != null && cartAdapter.isEditMode();
        cartItems = cartLocalStore.getCartItems();
        cartAdapter = new CartAdapter(cartItems, item -> {
            cartLocalStore.removeItem(item.getProduct().getId());
            cartRepository.syncCartToCloud(); // Sync xóa
            loadCart();
        });
        cartAdapter.setEditMode(editing);
        cartAdapter.setOnCartChangeListener(() -> {
            cartRepository.syncCartToCloud(); // Sync thay đổi số lượng/chọn
            refreshTotal();
        });
        binding.rvCartItems.setAdapter(cartAdapter);
        binding.cardCartItems.setVisibility(cartItems.isEmpty() ? View.GONE : View.VISIBLE);

        refreshTotal();
    }

    private void refreshTotal() {
        double total = 0;
        int selectedCount = 0;
        for (CartItem item : cartItems) {
            if (item.isSelected()) {
                total += item.getSubtotal();
                selectedCount++;
            }
        }

        binding.tvCartTitle.setText(getString(R.string.cart_title_count, cartItems.size()));
        binding.tvSelectAll.setText(getString(R.string.cart_select_all, cartItems.size()));
        binding.tvTotal.setText(PriceUtils.format(total));
        binding.btnCheckout.setText(getString(R.string.cart_checkout_count, selectedCount));
        binding.btnCheckout.setEnabled(selectedCount > 0);
        binding.btnCheckout.setAlpha(selectedCount > 0 ? 1f : 0.5f);

        suppressSelectAllCallback = true;
        binding.cbSelectAll.setChecked(!cartItems.isEmpty() && selectedCount == cartItems.size());
        suppressSelectAllCallback = false;
    }

    private void proceedToPayment() {
        boolean anySelected = false;
        for (CartItem item : cartItems) {
            if (item.isSelected()) { anySelected = true; break; }
        }
        if (cartItems.isEmpty() || !anySelected) {
            Toast.makeText(this, getString(R.string.error_empty_cart), Toast.LENGTH_SHORT).show();
            return;
        }
        cartLocalStore.saveCartItems(cartItems);
        Intent intent = new Intent(this, PaymentActivity.class);
        startActivity(intent);
    }
}
