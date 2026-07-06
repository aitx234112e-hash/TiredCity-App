package com.tiredcity.app.ui.shop;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Toast;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.tiredcity.app.R;
import com.tiredcity.app.adapter.ProductAdapter;
import com.tiredcity.app.adapter.SearchAdapter;
import com.tiredcity.app.data.local.CartLocalStore;
import com.tiredcity.app.data.model.ApiListResponse;
import com.tiredcity.app.data.model.CartItem;
import com.tiredcity.app.data.model.Product;
import com.tiredcity.app.data.model.search.EventItem;
import com.tiredcity.app.data.model.search.ProductItem;
import com.tiredcity.app.data.model.search.PromotionItem;
import com.tiredcity.app.data.model.search.SearchItem;
import com.tiredcity.app.data.repository.FirestoreProductRepository;
import com.tiredcity.app.databinding.ActivitySearchBinding;
import com.tiredcity.app.ui.base.BaseActivity;
import com.tiredcity.app.ui.reward.VoucherDetailActivity;
import com.tiredcity.app.utils.Constants;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class SearchActivity extends BaseActivity {

    public static final String EXTRA_QUERY = "extra_query";

    /** Số sản phẩm thật hiển thị trong "Gợi ý cho bạn". */
    private static final int SUGGESTION_PRODUCT_COUNT = 4;

    private ActivitySearchBinding binding;
    private FirestoreProductRepository firestoreRepository;
    private ProductAdapter productAdapter;
    private SearchAdapter suggestionAdapter;

    /** Toàn bộ sản phẩm tải một lần từ Firestore, dùng cho cả gợi ý lẫn lọc tìm kiếm. */
    private List<Product> allProducts = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySearchBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        firestoreRepository = new FirestoreProductRepository();

        productAdapter = new ProductAdapter(null);
        productAdapter.setOnProductClickListener(new ProductAdapter.OnProductClickListener() {
            @Override
            public void onProductClick(Product product) {
                openProductDetail(product);
            }

            @Override
            public void onSaveToggle(Product product, boolean saved) {}

            @Override
            public void onAddToCartClick(Product product) {
                // Bắt buộc chọn size -> Mở màn hình chi tiết
                Intent intent = new Intent(SearchActivity.this, ProductDetailActivity.class);
                intent.putExtra(Constants.EXTRA_PRODUCT_ID, product.getId());
                startActivity(intent);
                Toast.makeText(SearchActivity.this, "Vui lòng chọn Size trước khi mua", Toast.LENGTH_SHORT).show();
            }
        });
        binding.rvResults.setLayoutManager(new GridLayoutManager(this, 2));
        binding.rvResults.setAdapter(productAdapter);

        // Back button
        binding.btnBack.setOnClickListener(v -> finish());

        // Clear button
        binding.etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void afterTextChanged(Editable s) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                binding.btnClear.setVisibility(s.length() > 0 ? View.VISIBLE : View.GONE);
                if (s.length() == 0) showRecentState();
            }
        });

        binding.btnClear.setOnClickListener(v -> {
            binding.etSearch.setText("");
            showRecentState();
        });

        binding.etSearch.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                performSearch(binding.etSearch.getText().toString().trim());
                return true;
            }
            return false;
        });

        binding.swipeRefresh.setOnRefreshListener(() ->
            performSearch(binding.etSearch.getText().toString().trim()));

        // Phần 2 — "Được tìm kiếm nhiều nhất" tags
        addPopularChip(getString(R.string.tag_ao_dai));
        addPopularChip(getString(R.string.tag_nhat_binh));
        addPopularChip(getString(R.string.tag_phu_kien));

        // Phần 3 & 4 — "Gợi ý cho bạn" mixed-type suggestion list
        setupSuggestions();

        // Tải sản phẩm thật từ Firestore (dùng chung cho gợi ý + tìm kiếm)
        loadProducts();

        // Pre-filled query (e.g. tapped tag/product on the Shop tab) → search now
        String initialQuery = getIntent().getStringExtra(EXTRA_QUERY);
        if (initialQuery != null && !initialQuery.trim().isEmpty()) {
            binding.etSearch.setText(initialQuery);
            binding.etSearch.setSelection(initialQuery.length());
            performSearch(initialQuery.trim());
        } else {
            binding.etSearch.requestFocus();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Trạng thái yêu thích có thể đã đổi ở màn hình khác (chi tiết sản phẩm, tủ đồ...)
        productAdapter.notifyDataSetChanged();
    }

    /** Tải toàn bộ sản phẩm một lần rồi bơm vào phần gợi ý. */
    private void loadProducts() {
        firestoreRepository.getProducts(products -> {
            allProducts = products != null ? products : new ArrayList<>();
            refreshSuggestionProducts();
        });
    }

    // ── Gợi ý cho bạn ───────────────────────────────────────────────────────────

    /** Builds the multi-view-type discovery list (promotion + event + product). */
    private void setupSuggestions() {
        suggestionAdapter = new SearchAdapter(buildStaticSuggestions());
        suggestionAdapter.setOnItemClickListener(item -> {
            if (item instanceof PromotionItem) {
                openVoucherDetail((PromotionItem) item);
            } else if (item instanceof ProductItem) {
                // Sản phẩm → mở trang chi tiết sản phẩm thật.
                openProductDetail(((ProductItem) item).getProduct());
                performSearch(getString(((ProductItem) item).getBrandResId()));
            }
        });
        binding.rvSuggestions.setLayoutManager(new LinearLayoutManager(this));
        binding.rvSuggestions.setNestedScrollingEnabled(false);
        binding.rvSuggestions.setAdapter(suggestionAdapter);
    }

    /** Ưu đãi + sự kiện (nội dung tĩnh) — luôn đứng đầu danh sách gợi ý. */
    private List<SearchItem> buildStaticSuggestions() {
        List<SearchItem> items = new ArrayList<>();
        items.add(new PromotionItem(R.string.search_promo_birthday,
                                    R.drawable.banner_1,
                                    R.string.reward_voucher_birthday_title,
                                    R.string.reward_voucher_birthday_subtitle));
        items.add(new EventItem(R.string.search_event_coach_title,
                                R.string.search_event_coach_time, 0));
        return items;
    }

    /** Ghép ưu đãi/sự kiện với vài sản phẩm thật đầu tiên từ Firestore. */
    private void refreshSuggestionProducts() {
        List<SearchItem> combined = buildStaticSuggestions();
        int count = Math.min(SUGGESTION_PRODUCT_COUNT, allProducts.size());
        for (int i = 0; i < count; i++) {
            combined.add(new ProductItem(allProducts.get(i)));
        }
        suggestionAdapter.updateItems(combined);
    }

    private void openVoucherDetail(PromotionItem item) {
        Intent intent = new Intent(this, VoucherDetailActivity.class);
        intent.putExtra(VoucherDetailActivity.EXTRA_TITLE, getString(item.getVoucherTitleResId()));
        intent.putExtra(VoucherDetailActivity.EXTRA_SUBTITLE, getString(item.getVoucherSubtitleResId()));
        intent.putExtra(VoucherDetailActivity.EXTRA_BANNER, item.getBannerRes());
        intent.putExtra(VoucherDetailActivity.EXTRA_CODE, getString(R.string.barcode_code));
        startActivity(intent);
    }

    private void openProductDetail(Product product) {
        if (product == null || product.getId() == null) return;
        Intent intent = new Intent(this, ProductDetailActivity.class);
        intent.putExtra(Constants.EXTRA_PRODUCT_ID, product.getId());
        startActivity(intent);
    }

    // ── Tìm kiếm ────────────────────────────────────────────────────────────────

    private void performSearch(String keyword) {
        if (keyword.isEmpty()) { showRecentState(); return; }

        showResultsState();
        binding.swipeRefresh.setRefreshing(true);
        binding.tvResultCount.setText(getString(R.string.search_searching));

        productRepository.getProducts(1, 40, null, keyword)
            .enqueue(new Callback<ApiListResponse<Product>>() {
                @Override
                public void onResponse(Call<ApiListResponse<Product>> call, Response<ApiListResponse<Product>> response) {
                    binding.swipeRefresh.setRefreshing(false);
                    List<Product> results = null;
                    if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                        results = response.body().getData();
                    }

                    // FALLBACK: Search in Mock Catalog if API returns nothing
                    if (results == null || results.isEmpty()) {
                        results = searchInMockCatalog(keyword);
                    }

                    if (results == null || results.isEmpty()) {
                        showEmptyState(keyword);
                    } else {
                        binding.tvResultCount.setText(getString(
                                R.string.search_result_count, results.size()));
                        productAdapter.updateData(results);
                    }
                }
        binding.tvResultCount.setText(getString(R.string.search_searching));

        // Nếu chưa tải xong danh sách sản phẩm thì tải rồi lọc; ngược lại lọc ngay.
        if (allProducts.isEmpty()) {
            binding.swipeRefresh.setRefreshing(true);
            firestoreRepository.getProducts(products -> {
                allProducts = products != null ? products : new ArrayList<>();
                refreshSuggestionProducts();
                binding.swipeRefresh.setRefreshing(false);
                showFilteredResults(keyword);
            });
        } else {
            showFilteredResults(keyword);
        }
    }

    /** Lọc client-side theo tên / danh mục / chất liệu / màu. */
    private void showFilteredResults(String keyword) {
        String q = keyword.toLowerCase(Locale.getDefault());
        List<Product> results = new ArrayList<>();
        for (Product p : allProducts) {
            if (contains(p.getName(), q)
                    || contains(p.getCategory(), q)
                    || contains(p.getMaterial(), q)
                    || colorsContain(p, q)) {
                results.add(p);
            }
        }

        if (results.isEmpty()) {
            showEmptyState(keyword);
        } else {
            binding.tvResultCount.setText(getString(R.string.search_result_count, results.size()));
            productAdapter.updateData(results);
        }
    }

    private static boolean contains(String value, String lowerQuery) {
        return value != null && value.toLowerCase(Locale.getDefault()).contains(lowerQuery);
    }

                @Override
                public void onFailure(Call<ApiListResponse<Product>> call, Throwable t) {
                    binding.swipeRefresh.setRefreshing(false);
                    List<Product> results = searchInMockCatalog(keyword);
                    if (results.isEmpty()) {
                        showEmptyState(keyword);
                    } else {
                        productAdapter.updateData(results);
                    }
                }
            });
    private static boolean colorsContain(Product p, String lowerQuery) {
        if (p.getColors() == null) return false;
        for (String c : p.getColors()) {
            if (contains(c, lowerQuery)) return true;
        }
        return false;
    }

    private List<Product> searchInMockCatalog(String keyword) {
        List<Product> allMock = com.tiredcity.app.data.mock.MockProductCatalog.getProducts(this, "ALL");
        List<Product> filtered = new java.util.ArrayList<>();
        String query = keyword.toLowerCase().trim();
        for (Product p : allMock) {
            if (p.getName().toLowerCase().contains(query) ||
                (p.getCategory() != null && p.getCategory().toLowerCase().contains(query)) ||
                (p.getId() != null && p.getId().toLowerCase().contains(query))) {
                filtered.add(p);
            }
        }
        return filtered;
    }

    // ── Trạng thái hiển thị ─────────────────────────────────────────────────────

    private void showRecentState() {
        binding.layoutRecent.setVisibility(View.VISIBLE);
        binding.layoutResults.setVisibility(View.GONE);
        binding.layoutEmpty.setVisibility(View.GONE);
    }

    private void showResultsState() {
        binding.layoutRecent.setVisibility(View.GONE);
        binding.layoutResults.setVisibility(View.VISIBLE);
        binding.layoutEmpty.setVisibility(View.GONE);
    }

    private void showEmptyState(String keyword) {
        binding.layoutRecent.setVisibility(View.GONE);
        binding.layoutResults.setVisibility(View.GONE);
        binding.layoutEmpty.setVisibility(View.VISIBLE);
        binding.tvEmptyMessage.setText(getString(R.string.search_not_found, keyword));
    }

    private void addPopularChip(String label) {
        com.google.android.material.chip.Chip chip = new com.google.android.material.chip.Chip(this);
        chip.setText(label);
        chip.setOnClickListener(v -> {
            binding.etSearch.setText(label);
            binding.etSearch.setSelection(label.length());
            performSearch(label);
        });
        binding.chipGroupPopular.addView(chip);
    }
}
