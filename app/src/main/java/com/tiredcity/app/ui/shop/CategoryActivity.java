package com.tiredcity.app.ui.shop;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.Toast;
import androidx.annotation.ColorInt;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import com.tiredcity.app.R;
import com.tiredcity.app.adapter.ProductAdapter;
import com.tiredcity.app.data.local.CartLocalStore;
import com.tiredcity.app.data.mock.MockProductCatalog;
import com.tiredcity.app.data.model.ApiListResponse;
import com.tiredcity.app.data.model.CartItem;
import com.tiredcity.app.data.model.Product;
import com.tiredcity.app.data.network.ApiClient;
import com.tiredcity.app.data.repository.FirestoreProductRepository;
import com.tiredcity.app.data.repository.ProductRepository;
import com.tiredcity.app.databinding.ActivityCategoryBinding;
import com.tiredcity.app.ui.base.BaseActivity;
import com.tiredcity.app.ui.cart.CartActivity;
import com.tiredcity.app.utils.ColorTaxonomy;
import com.tiredcity.app.utils.Constants;
import java.util.ArrayList;
import java.util.List;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CategoryActivity extends BaseActivity {

    public static final String EXTRA_CATEGORY_ID   = "category_id";
    public static final String EXTRA_CATEGORY_NAME = "category_name";
    /** Nhóm màu (5 danh mục trang phục) hoặc phân loại phụ kiện (Phụ Kiện) — lọc client-side theo Product.getColors(). */
    public static final String EXTRA_TAG_FILTER    = "tag_filter";

    private ActivityCategoryBinding binding;
    private ProductRepository productRepository;
    private FirestoreProductRepository firestoreRepository;
    private ProductAdapter productAdapter;
    private GridLayoutManager gridManager;
    private String categoryId;
    private String tagFilter;
    private int spanCount = 2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityCategoryBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        categoryId = getIntent().getStringExtra(EXTRA_CATEGORY_ID);
        tagFilter = getIntent().getStringExtra(EXTRA_TAG_FILTER);
        String categoryName = getIntent().getStringExtra(EXTRA_CATEGORY_NAME);
        binding.tvTitle.setText(categoryName != null ? categoryName
                : getString(R.string.category_default_title));

        binding.btnBack.setOnClickListener(v -> finish());
        binding.btnCart.setOnClickListener(v ->
                startActivity(new Intent(this, CartActivity.class)));

        setupProductGrid();
        setupViewModeToggle();
        setupFilter();

        loadProducts();
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateCartBadge();
        // Trạng thái yêu thích có thể đã đổi ở màn hình khác (chi tiết sản phẩm, tủ đồ...)
        productAdapter.notifyDataSetChanged();
    }

    // ── Lưới sản phẩm ──────────────────────────────────────────────────────────

    private void setupProductGrid() {
        productAdapter = new ProductAdapter(null);
        productAdapter.setFillWidth(true);
        productAdapter.setOnProductClickListener(new ProductAdapter.OnProductClickListener() {
            @Override
            public void onProductClick(Product product) {
                Intent intent = new Intent(CategoryActivity.this, ProductDetailActivity.class);
                intent.putExtra(Constants.EXTRA_PRODUCT_ID, product.getId());
                startActivity(intent);
            }

            @Override
            public void onSaveToggle(Product product, boolean saved) {}
        });

        gridManager = new GridLayoutManager(this, spanCount);
        binding.rvProducts.setLayoutManager(gridManager);
        binding.rvProducts.setAdapter(productAdapter);

        binding.swipeRefresh.setColorSchemeColors(
                ContextCompat.getColor(this, R.color.tc_red));
        binding.swipeRefresh.setOnRefreshListener(this::loadProducts);
    }

    // ── Chế độ xem 1 / 2 / 3 cột ──────────────────────────────────────────────

    private void setupViewModeToggle() {
        binding.btnView1.setOnClickListener(v -> setSpanCount(1));
        binding.btnView2.setOnClickListener(v -> setSpanCount(2));
        binding.btnView3.setOnClickListener(v -> setSpanCount(3));
        highlightViewMode();
    }

    private void setSpanCount(int span) {
        if (span == spanCount) return;
        spanCount = span;
        gridManager.setSpanCount(span);
        productAdapter.notifyDataSetChanged();
        highlightViewMode();
    }

    private void highlightViewMode() {
        @ColorInt int active   = ContextCompat.getColor(this, R.color.tc_red);
        @ColorInt int inactive = ContextCompat.getColor(this, R.color.tc_text_secondary);
        tint(binding.btnView1, spanCount == 1 ? active : inactive);
        tint(binding.btnView2, spanCount == 2 ? active : inactive);
        tint(binding.btnView3, spanCount == 3 ? active : inactive);
    }

    private void tint(ImageButton button, @ColorInt int color) {
        button.setColorFilter(color);
    }

    // ── Bộ lọc ─────────────────────────────────────────────────────────────────

    private void setupFilter() {
        binding.btnFilter.setOnClickListener(v ->
                Toast.makeText(this, R.string.filter_coming_soon, Toast.LENGTH_SHORT).show());
    }

    // ── Giỏ hàng badge ─────────────────────────────────────────────────────────

    private void updateCartBadge() {
        int count = 0;
        for (CartItem item : new CartLocalStore(this).getCartItems()) {
            count += item.getQuantity();
        }
        if (count <= 0) {
            binding.tvCartBadge.setVisibility(android.view.View.GONE);
        } else {
            binding.tvCartBadge.setVisibility(android.view.View.VISIBLE);
            binding.tvCartBadge.setText(count > 9 ? getString(R.string.cart_badge_overflow) : String.valueOf(count));
        }
    }

    // ── Tải sản phẩm ───────────────────────────────────────────────────────────

    private void loadProducts() {
        binding.swipeRefresh.setRefreshing(true);
        if (firestoreRepository == null) firestoreRepository = new FirestoreProductRepository();
        // Nguồn chính: Firestore (cùng dữ liệu + ảnh với admin). REST backend
        // (tiredcity.vn/api) hiện chưa hoạt động nên không dùng nữa; nếu Firestore
        // không có sản phẩm cho danh mục này thì mới hiển thị dữ liệu mẫu offline.
        firestoreRepository.getProducts(all -> {
            binding.swipeRefresh.setRefreshing(false);
            List<Product> inCategory = filterByCategory(all);
            List<Product> source = (inCategory != null && !inCategory.isEmpty())
                    ? inCategory : MockProductCatalog.getProducts(CategoryActivity.this, categoryId);
            productAdapter.updateData(applyTagFilter(source));
        });
    }

    /** Giữ lại các sản phẩm thuộc đúng danh mục đang mở (so theo nhãn danh mục đã ánh xạ). */
    private List<Product> filterByCategory(List<Product> all) {
        if (all == null) return null;
        if (categoryId == null) return all;
        List<Product> out = new ArrayList<>();
        for (Product p : all) {
            if (categoryId.equals(p.getCategory())) out.add(p);
        }
        return out;
    }

    /**
     * Lọc theo nhóm màu (5 danh mục trang phục, qua {@link ColorTaxonomy}) hoặc theo phân loại
     * phụ kiện (Phụ Kiện, so khớp trực tiếp) — cả hai đều dựa trên {@link Product#getColors()}.
     */
    private List<Product> applyTagFilter(List<Product> products) {
        if (tagFilter == null || products == null) return products;
        boolean matchByColorBucket = !"PHỤ KIỆN".equals(categoryId);

        List<Product> filtered = new ArrayList<>();
        for (Product p : products) {
            List<String> colors = p.getColors();
            boolean matches = matchByColorBucket
                    ? ColorTaxonomy.matchesBucket(colors, tagFilter)
                    : (colors != null && colors.contains(tagFilter));
            if (matches) filtered.add(p);
        }
        return filtered;
    }

}
