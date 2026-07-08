package com.tiredcity.app.ui.shop;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.recyclerview.widget.GridLayoutManager;

import com.bumptech.glide.Glide;
import com.tiredcity.app.R;
import com.tiredcity.app.adapter.ProductAdapter;
import com.tiredcity.app.data.local.CartLocalStore;
import com.tiredcity.app.data.local.FavoritesLocalStore;
import com.tiredcity.app.data.mock.MockProductCatalog;
import com.tiredcity.app.data.model.CartItem;
import com.tiredcity.app.data.model.Product;
import com.tiredcity.app.data.repository.FirestoreProductRepository;
import com.tiredcity.app.databinding.ActivityCategoryBinding;
import com.tiredcity.app.ui.base.BaseActivity;
import com.tiredcity.app.ui.cart.CartActivity;
import com.tiredcity.app.utils.CartFlyAnimation;
import com.tiredcity.app.utils.ColorTaxonomy;
import com.tiredcity.app.utils.Constants;

import java.util.ArrayList;
import java.util.List;

public class CategoryActivity extends BaseActivity {

    public static final String EXTRA_CATEGORY_ID = "category_id";
    public static final String EXTRA_CATEGORY_NAME = "category_name";
    public static final String EXTRA_TAG_FILTER = "tag_filter";

    private ActivityCategoryBinding binding;
    private FirestoreProductRepository firestoreRepository;
    private ProductAdapter productAdapter;
    private GridLayoutManager gridManager;
    private String categoryId;
    private String tagFilter;
    private int spanCount = 2;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityCategoryBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Thanh tiêu đề nền đỏ chạy liền lên status bar (icon pin/wifi màu sáng cho đủ tương phản);
        // root layout fitsSystemWindows nên nội dung vẫn nằm hẳn dưới status bar.
        getWindow().setStatusBarColor(ContextCompat.getColor(this, R.color.tc_red));
        WindowInsetsControllerCompat insets =
                WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
        insets.setAppearanceLightStatusBars(false);

        categoryId = getIntent().getStringExtra(EXTRA_CATEGORY_ID);
        String categoryName = getIntent().getStringExtra(EXTRA_CATEGORY_NAME);
        tagFilter = getIntent().getStringExtra(EXTRA_TAG_FILTER);

        binding.tvTitle.setText(categoryName != null ? categoryName : categoryId);
        binding.btnBack.setOnClickListener(v -> finishSmoothly());
        binding.btnCart.setOnClickListener(v -> startSmoothActivity(new Intent(this, CartActivity.class)));

        firestoreRepository = new FirestoreProductRepository();
        setupProductGrid();
        setupViewModeToggle();
        setupFilter();

        binding.swipeRefresh.setOnRefreshListener(this::loadProducts);
        loadProducts();
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateCartBadge();
        if (productAdapter != null) {
            productAdapter.notifyDataSetChanged();
        }
    }

    private void setupProductGrid() {
        productAdapter = new ProductAdapter(new ArrayList<>());
        productAdapter.setSpanCount(spanCount);
        productAdapter.setOnProductClickListener(new ProductAdapter.OnProductClickListener() {
            @Override
            public void onProductClick(Product product) {
                Intent intent = new Intent(CategoryActivity.this, ProductDetailActivity.class);
                intent.putExtra(Constants.EXTRA_PRODUCT_ID, product.getId());
                startSmoothActivity(intent);
            }

            @Override
            public void onSaveToggle(Product product, boolean saved) {
                new FavoritesLocalStore(CategoryActivity.this).toggleFavorite(product);
            }

            @Override
            public void onAddToCartClick(Product product, View sourceView) {
                quickAddToCart(product, sourceView);
            }
        });

        gridManager = new GridLayoutManager(this, spanCount);
        binding.rvProducts.setLayoutManager(gridManager);
        binding.rvProducts.setAdapter(productAdapter);
    }

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
        productAdapter.setSpanCount(span);
        highlightViewMode();
    }

    private void highlightViewMode() {
        int active = ContextCompat.getColor(this, R.color.tc_red);
        // Nền trang là trắng nên tc_stroke (kem) gần như tàng hình — dùng xám chữ phụ.
        int inactive = ContextCompat.getColor(this, R.color.tc_text_secondary);
        tint(binding.btnView1, spanCount == 1 ? active : inactive);
        tint(binding.btnView2, spanCount == 2 ? active : inactive);
        tint(binding.btnView3, spanCount == 3 ? active : inactive);
    }

    private void tint(ImageButton btn, int color) {
        btn.setColorFilter(color);
    }

    private void setupFilter() {
        binding.btnFilter.setOnClickListener(v -> {
            // Future implementation
        });
    }

    /** Thêm nhanh vào giỏ từ lưới sản phẩm kèm hiệu ứng ảnh bay vào icon giỏ. */
    private void quickAddToCart(Product product, View sourceView) {
        // Sản phẩm bắt buộc chọn size (áo dài) thì mở chi tiết để chọn, không thêm mù.
        if (product.getSizes() != null && !product.getSizes().isEmpty()) {
            Intent intent = new Intent(this, ProductDetailActivity.class);
            intent.putExtra(Constants.EXTRA_PRODUCT_ID, product.getId());
            startSmoothActivity(intent);
            Toast.makeText(this, "Vui lòng chọn Size trước khi thêm vào giỏ", Toast.LENGTH_SHORT).show();
            return;
        }
        new CartLocalStore(this).addItem(new CartItem(product, 1));
        updateCartBadge();

        final String image = product.getFirstImage();
        CartFlyAnimation.fly(this, sourceView, binding.btnCart, target -> {
            Object loadTarget = image;
            if (image != null && !image.isEmpty()
                    && !image.startsWith("http") && !image.startsWith("content")) {
                int resId = getResources().getIdentifier(image, "drawable", getPackageName());
                if (resId != 0) loadTarget = resId;
            }
            Glide.with(this)
                    .load(loadTarget)
                    .centerCrop()
                    .placeholder(R.color.bg_subtle)
                    .error(R.drawable.ic_wardrobe)
                    .into(target);
        });
        Toast.makeText(this, getString(R.string.success_add_cart) + " 🛒", Toast.LENGTH_SHORT).show();
    }

    private void updateCartBadge() {
        int count = new CartLocalStore(this).getCartCount();
        if (count > 0) {
            binding.tvCartBadge.setVisibility(View.VISIBLE);
            binding.tvCartBadge.setText(String.valueOf(count));
        } else {
            binding.tvCartBadge.setVisibility(View.GONE);
        }
    }

    private void loadProducts() {
        binding.swipeRefresh.setRefreshing(true);
        firestoreRepository.getProducts(all -> {
            binding.swipeRefresh.setRefreshing(false);
            List<Product> inCategory = filterByCategory(all);
            List<Product> source = (inCategory != null && !inCategory.isEmpty())
                    ? inCategory : MockProductCatalog.getProducts(CategoryActivity.this, categoryId);
            productAdapter.updateData(applyTagFilter(source));
        });
    }

    private List<Product> filterByCategory(List<Product> all) {
        if (all == null) return null;
        if (categoryId == null || "ALL".equalsIgnoreCase(categoryId) || "Tất cả".equalsIgnoreCase(categoryId)) return all;
        
        List<Product> out = new ArrayList<>();
        for (Product p : all) {
            String pCat = p.getCategory();
            if (pCat == null) continue;
            
            boolean match = categoryId.equalsIgnoreCase(pCat);
            if (!match) {
                if (categoryId.equalsIgnoreCase("Áo Dài")) match = pCat.contains("ao-dai") || pCat.equalsIgnoreCase("AO DAI");
                else if (categoryId.equalsIgnoreCase("Nhật Bình")) match = pCat.contains("nhat-binh") || pCat.equalsIgnoreCase("NHAT BINH");
                else if (categoryId.equalsIgnoreCase("Áo Tấc")) match = pCat.contains("ao-tac") || pCat.equalsIgnoreCase("AO TAC");
                else if (categoryId.equalsIgnoreCase("Phụ Kiện")) match = pCat.contains("phu-kien") || pCat.equalsIgnoreCase("PHU KIEN");
                else if (categoryId.equalsIgnoreCase("Giao Lĩnh")) match = pCat.contains("giao-linh") || pCat.equalsIgnoreCase("GIAO LINH");
                else if (categoryId.equalsIgnoreCase("Yếm Đào")) match = pCat.contains("yem-dao") || pCat.equalsIgnoreCase("YEM DAO");
            }
            if (match) out.add(p);
        }
        return out;
    }

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
