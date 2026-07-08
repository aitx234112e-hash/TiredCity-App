package com.tiredcity.app.ui.shop;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.google.android.material.chip.Chip;
import com.tiredcity.app.R;
import com.tiredcity.app.adapter.BannerAdapter;
import com.tiredcity.app.adapter.ProductAdapter;
import com.tiredcity.app.adapter.SearchAdapter;
import com.tiredcity.app.data.local.FavoritesLocalStore;
import com.tiredcity.app.data.local.RecentSearchStore;
import com.tiredcity.app.data.mock.MockProductCatalog;
import com.tiredcity.app.data.model.Product;
import com.tiredcity.app.data.model.search.EventItem;
import com.tiredcity.app.data.model.search.ProductItem;
import com.tiredcity.app.data.model.search.PromotionItem;
import com.tiredcity.app.data.model.search.SearchItem;
import com.tiredcity.app.data.repository.FirestoreProductRepository;
import com.tiredcity.app.databinding.ActivitySearchBinding;
import com.tiredcity.app.ui.base.BaseActivity;
import com.tiredcity.app.ui.cart.CartActivity;
import com.tiredcity.app.ui.explore.EventDetailActivity;
import com.tiredcity.app.ui.main.MainActivity;
import com.tiredcity.app.ui.reward.VoucherDetailActivity;
import com.tiredcity.app.utils.Constants;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class SearchActivity extends BaseActivity {

    public static final String EXTRA_QUERY = "extra_query";

    /** Số sản phẩm thật hiển thị trong "Gợi ý cho bạn" (sau ưu đãi + sự kiện, dạng hàng ngang). */
    private static final int SUGGESTION_PRODUCT_COUNT = 4;

    private static final long BANNER_AUTO_SCROLL_INTERVAL_MS = 4000L;

    private ActivitySearchBinding binding;
    private FirestoreProductRepository firestoreRepository;
    private RecentSearchStore recentSearchStore;
    private ProductAdapter productAdapter;
    private SearchAdapter suggestionAdapter;
    private BannerAdapter searchBannerAdapter;
    private final Handler bannerAutoScrollHandler = new Handler(Looper.getMainLooper());
    private final Runnable bannerAutoScrollRunnable = this::advanceBanner;

    /** Toàn bộ sản phẩm tải một lần từ Firestore, dùng cho cả gợi ý lẫn lọc tìm kiếm. */
    private List<Product> allProducts = new ArrayList<>();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySearchBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        firestoreRepository = new FirestoreProductRepository();
        recentSearchStore = new RecentSearchStore(this);
        recentSearchStore.seedDefaultsIfNeeded(
                Arrays.asList(getResources().getStringArray(R.array.search_recent_seed_keywords)));

        binding.btnBack.setOnClickListener(v -> finishSmoothly());
        binding.btnClear.setOnClickListener(v -> binding.etSearch.setText(""));

        setupSuggestions();

        productAdapter = new ProductAdapter(new ArrayList<>());
        productAdapter.setOnProductClickListener(new ProductAdapter.OnProductClickListener() {
            @Override public void onProductClick(Product p) { openProductDetail(p); }
            @Override public void onSaveToggle(Product p, boolean saved) { new FavoritesLocalStore(SearchActivity.this).toggleFavorite(p); }
            @Override public void onAddToCartClick(Product p) { openProductDetail(p); }
        });
        binding.rvResults.setLayoutManager(new GridLayoutManager(this, 2));
        binding.rvResults.setAdapter(productAdapter);

        binding.etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                binding.btnClear.setVisibility(s.length() > 0 ? View.VISIBLE : View.GONE);
                if (s.length() == 0) showRecentState();
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        binding.btnCartShortcut.setOnClickListener(v ->
                startActivity(new Intent(this, CartActivity.class)));

        binding.etSearch.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                performSearch(binding.etSearch.getText().toString().trim());
                return true;
            }
            return false;
        });

        binding.btnClearRecent.setOnClickListener(v -> {
            recentSearchStore.clearAll();
            refreshRecentSearches();
        });

        binding.btnViewAllResults.setOnClickListener(v -> showAllProductsAsResults());

        setupSearchBannerCarousel();
        refreshSuggestedKeywords();
        setupSuggestions();
        refreshRecentSearches();

        // Tải sản phẩm thật từ Firestore (dùng chung cho gợi ý + tìm kiếm)
        loadProducts();

        // Pre-filled query (e.g. tapped tag/product on the Shop tab) → search now
        String initialQuery = getIntent().getStringExtra(EXTRA_QUERY);
        if (initialQuery != null) {
            binding.etSearch.setText(initialQuery);
            performSearch(initialQuery);
        } else {
            showRecentState();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Trạng thái yêu thích có thể đã đổi ở màn hình khác (chi tiết sản phẩm, tủ đồ...)
        if (productAdapter != null) productAdapter.notifyDataSetChanged();
        bannerAutoScrollHandler.postDelayed(bannerAutoScrollRunnable, BANNER_AUTO_SCROLL_INTERVAL_MS);
    }

    @Override
    protected void onPause() {
        super.onPause();
        bannerAutoScrollHandler.removeCallbacks(bannerAutoScrollRunnable);
    }

    private void loadProducts() {
        firestoreRepository.getProducts(products -> {
            allProducts = products != null ? products : new ArrayList<>();
            refreshSuggestionProducts();
            refreshSuggestedKeywords();
        });
    }

    // ── Carousel banner danh mục ─────────────────────────────────────────────────

    /** 5 ảnh danh mục đẹp nhất trong bộ tư liệu, mỗi ảnh bấm vào sẽ mở đúng danh mục đó. */
    private void setupSearchBannerCarousel() {
        List<BannerAdapter.BannerItem> items = new ArrayList<>();
        items.add(new BannerAdapter.BannerItem(
                R.drawable.search_banner_ao_dai_trang_bach_ngoc, getString(R.string.cat_ao_dai)));
        items.add(new BannerAdapter.BannerItem(
                R.drawable.search_banner_nhat_binh_xanh_lam_ngoc_bich, getString(R.string.cat_nhat_binh)));
        items.add(new BannerAdapter.BannerItem(
                R.drawable.search_banner_giao_linh_xanh_luc_bao, getString(R.string.cat_tab_giao_linh)));
        items.add(new BannerAdapter.BannerItem(
                R.drawable.search_banner_yem_dao_hong_thach_anh, getString(R.string.cat_tab_yem_dao)));
        items.add(new BannerAdapter.BannerItem(
                R.drawable.search_banner_ao_tac_xanh_lam_ngoc_bich, getString(R.string.cat_ao_tac)));

        searchBannerAdapter = new BannerAdapter(items);
        searchBannerAdapter.setOnBannerClickListener((position, item) -> openCategory(item.title, item.title));
        binding.vpSearchBanner.setAdapter(searchBannerAdapter);
        binding.dotsSearchBanner.attachTo(binding.vpSearchBanner);
    }

    /** Tự động lướt sang banner kế tiếp mỗi vài giây, quay lại đầu khi hết danh sách. */
    private void advanceBanner() {
        if (searchBannerAdapter == null || searchBannerAdapter.getItemCount() == 0) return;
        int next = (binding.vpSearchBanner.getCurrentItem() + 1) % searchBannerAdapter.getItemCount();
        binding.vpSearchBanner.setCurrentItem(next, true);
        bannerAutoScrollHandler.postDelayed(bannerAutoScrollRunnable, BANNER_AUTO_SCROLL_INTERVAL_MS);
    }

    // ── Tìm kiếm gần đây ────────────────────────────────────────────────────────

    /** Đọc lại lịch sử tìm kiếm và vẽ lại các chip; ẩn cả khối nếu chưa có lịch sử. */
    private void refreshRecentSearches() {
        List<String> recent = recentSearchStore.getAll();
        binding.layoutRecentSection.setVisibility(recent.isEmpty() ? View.GONE : View.VISIBLE);

        binding.chipGroupRecent.removeAllViews();
        for (String query : recent) {
            Chip chip = createRedOutlineChip(query);
            chip.setCloseIconVisible(true);
            chip.setCloseIconTint(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.tc_red)));
            chip.setOnClickListener(v -> {
                binding.etSearch.setText(query);
                binding.etSearch.setSelection(query.length());
                performSearch(query);
            });
            chip.setOnCloseIconClickListener(v -> {
                recentSearchStore.remove(query);
                refreshRecentSearches();
            });
            binding.chipGroupRecent.addView(chip);
        }
    }

    // ── Gợi ý từ khóa ───────────────────────────────────────────────────────────

    /** 6 danh mục trang phục — khớp với các tab bên "Danh mục" (StylingFragment). */
    private static final int[] SUGGESTED_CATEGORY_LABELS = {
            R.string.cat_tab_ao_dai,
            R.string.cat_tab_nhat_binh,
            R.string.cat_tab_ao_tac,
            R.string.cat_tab_giao_linh,
            R.string.cat_tab_yem_dao,
            R.string.cat_tab_phu_kien
    };
    private static final String[] SUGGESTED_CATEGORY_IDS = {
            "ÁO DÀI", "NHẬT BÌNH", "ÁO TẤC", "GIAO LĨNH", "YẾM ĐÀO", "PHỤ KIỆN"
    };

    /** Số tên sản phẩm thật bơm thêm vào cuối danh sách gợi ý, sau khi Firestore tải xong. */
    private static final int SUGGESTED_PRODUCT_KEYWORD_COUNT = 4;

    /**
     * Vẽ lại toàn bộ chip "gợi ý từ khóa": 6 tên danh mục trước, rồi vài tên sản phẩm thật
     * (nếu đã tải xong) — bấm vào tên danh mục sẽ mở đúng danh mục đó, bấm vào tên sản phẩm
     * sẽ mở đúng trang chi tiết sản phẩm đó.
     */
    private void refreshSuggestedKeywords() {
        binding.chipGroupKeywords.removeAllViews();
        for (int i = 0; i < SUGGESTED_CATEGORY_IDS.length; i++) {
            String categoryId = SUGGESTED_CATEGORY_IDS[i];
            Chip chip = createRedOutlineChip(getString(SUGGESTED_CATEGORY_LABELS[i]));
            chip.setOnClickListener(v -> openStylingCategoryTab(categoryId));
            binding.chipGroupKeywords.addView(chip);
        }

        for (Product product : pickDiverseProducts(SUGGESTED_PRODUCT_KEYWORD_COUNT)) {
            if (product.getName() == null) continue;
            Chip chip = createRedOutlineChip(product.getName());
            chip.setOnClickListener(v -> openProductDetail(product));
            binding.chipGroupKeywords.addView(chip);
        }
    }

    /** Mở lưới sản phẩm của một danh mục (khớp với {@code Product.getCategory()}) — dùng cho banner carousel. */
    private void openCategory(String categoryId, String categoryName) {
        Intent intent = new Intent(this, CategoryActivity.class);
        intent.putExtra(CategoryActivity.EXTRA_CATEGORY_ID, categoryId);
        intent.putExtra(CategoryActivity.EXTRA_CATEGORY_NAME, categoryName);
        startActivity(intent);
    }

    /**
     * Mở tab "Danh mục" (StylingFragment) và chọn sẵn đúng nhóm trang phục — dùng cho chip tên
     * danh mục trong "gợi ý từ khóa", thay vì mở thẳng lưới sản phẩm như {@link #openCategory}.
     */
    private void openStylingCategoryTab(String categoryId) {
        Intent intent = new Intent(this, MainActivity.class);
        intent.putExtra(MainActivity.EXTRA_OPEN_CATEGORY_ID, categoryId);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(intent);
    }

    /** Chip nền trắng, viền đỏ, chữ đen in hoa — dùng cho cả chip "gần đây" lẫn "gợi ý từ khóa". */
    private Chip createRedOutlineChip(String text) {
        Chip chip = new Chip(this);
        chip.setText(text);
        chip.setAllCaps(true);
        chip.setChipBackgroundColor(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.white)));
        chip.setTextColor(ContextCompat.getColor(this, R.color.black));
        chip.setChipStrokeColor(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.tc_red)));
        chip.setChipStrokeWidth(getResources().getDimension(R.dimen.tc_stroke_w));
        return chip;
    }

    // ── Gợi ý cho bạn ───────────────────────────────────────────────────────────

    /** Danh sách hàng ngang gộp ưu đãi + sự kiện + sản phẩm (multiple view types). */
    private void setupSuggestions() {
        suggestionAdapter = new SearchAdapter(buildStaticSuggestions());
        suggestionAdapter.setOnItemClickListener(item -> {
            if (item instanceof PromotionItem) {
                openVoucherDetail((PromotionItem) item);
            } else if (item instanceof EventItem) {
                openEventDetail();
            } else if (item instanceof ProductItem) {
                openProductDetail(((ProductItem) item).getProduct());
            }
        });
        binding.rvSuggestedProducts.setLayoutManager(new LinearLayoutManager(this));
        binding.rvSuggestedProducts.setNestedScrollingEnabled(false);
        binding.rvSuggestedProducts.setAdapter(suggestionAdapter);
    }

    /** Ưu đãi + sự kiện (nội dung tĩnh) — luôn đứng đầu danh sách "Gợi ý cho bạn". */
    private List<SearchItem> buildStaticSuggestions() {
        List<SearchItem> items = new ArrayList<>();
        items.add(new PromotionItem(R.string.search_promo_ao_dai_lua,
                                    R.drawable.search_suggestion_promo_banner,
                                    R.string.search_promo_ao_dai_lua_title,
                                    R.string.search_promo_ao_dai_lua_subtitle));
        items.add(new EventItem(R.string.search_event_dan_toc_tu_hao_title,
                                R.string.search_event_dan_toc_tu_hao_time,
                                R.drawable.search_suggestion_event_banner));
        return items;
    }

    private void refreshSuggestionProducts() {
        List<SearchItem> items = buildStaticSuggestions();
        for (Product product : pickDiverseProducts(SUGGESTION_PRODUCT_COUNT)) {
            items.add(new ProductItem(product));
        }
        suggestionAdapter.updateItems(items);
    }

    /**
     * Chọn tối đa {@code max} sản phẩm nhưng ưu tiên trải đều các danh mục (Áo dài, Nhật bình,
     * Áo tấc...) thay vì lấy nguyên cụm sản phẩm đầu tiên — để phần gợi ý đa dạng, không toàn Áo dài.
     * Duyệt vòng: mỗi lượt lấy 1 sản phẩm của mỗi danh mục theo thứ tự xuất hiện, lặp tới khi đủ.
     */
    private List<Product> pickDiverseProducts(int max) {
        java.util.LinkedHashMap<String, List<Product>> byCategory = new java.util.LinkedHashMap<>();
        for (Product p : allProducts) {
            String key = p.getCategory() != null ? p.getCategory() : "";
            byCategory.computeIfAbsent(key, k -> new ArrayList<>()).add(p);
        }

        List<Product> picked = new ArrayList<>();
        int index = 0;
        boolean tookAny = true;
        while (picked.size() < max && tookAny) {
            tookAny = false;
            for (List<Product> group : byCategory.values()) {
                if (index < group.size()) {
                    picked.add(group.get(index));
                    tookAny = true;
                    if (picked.size() >= max) break;
                }
            }
            index++;
        }
        return picked;
    }

    private void openVoucherDetail(PromotionItem item) {
        startActivity(new Intent(this, VoucherDetailActivity.class));
    }

    /** Mở trang chi tiết sự kiện được bấm. */
    private void openEventDetail() {
        startActivity(new Intent(this, EventDetailActivity.class));
    }

    /** Hiển thị toàn bộ sản phẩm ở trạng thái kết quả, không gắn với một từ khóa cụ thể. */
    private void showAllProductsAsResults() {
        showResultsState();
        binding.tvResultCount.setText(getString(R.string.search_result_count, allProducts.size()));
        productAdapter.updateData(allProducts);
    }

    private void openProductDetail(Product product) {
        Intent intent = new Intent(this, ProductDetailActivity.class);
        intent.putExtra(Constants.EXTRA_PRODUCT_ID, product.getId());
        startSmoothActivity(intent);
    }

    private void performSearch(String keyword) {
        if (keyword.isEmpty()) { showRecentState(); return; }

        recentSearchStore.add(keyword);
        showResultsState();
        binding.swipeRefresh.setRefreshing(true);
        binding.tvResultCount.setText(getString(R.string.search_searching));

        if (allProducts.isEmpty()) {
            firestoreRepository.getProducts(products -> {
                allProducts = products != null ? products : new ArrayList<>();
                refreshSuggestionProducts();
                refreshSuggestedKeywords();
                binding.swipeRefresh.setRefreshing(false);
                showFilteredResults(keyword);
            });
        } else {
            binding.swipeRefresh.setRefreshing(false);
            showFilteredResults(keyword);
        }
    }

    private void showFilteredResults(String keyword) {
        String q = keyword.toLowerCase(Locale.getDefault());
        List<Product> results = new ArrayList<>();
        for (Product p : allProducts) {
            if (contains(p.getName(), q) || contains(p.getCategory(), q) || contains(p.getMaterial(), q) || colorsContain(p, q)) {
                results.add(p);
            }
        }

        if (results.isEmpty()) {
            results = searchInMockCatalog(keyword);
        }

        if (results.isEmpty()) {
            showEmptyState(keyword);
        } else {
            binding.tvResultCount.setText(getString(R.string.search_result_count, results.size()));
            productAdapter.updateData(results);
        }
    }

    private boolean contains(String value, String lowerQuery) {
        return value != null && value.toLowerCase(Locale.getDefault()).contains(lowerQuery);
    }

    private boolean colorsContain(Product p, String query) {
        if (p.getColors() == null) return false;
        for (String c : p.getColors()) {
            if (c.toLowerCase(Locale.getDefault()).contains(query)) return true;
        }
        return false;
    }

    private List<Product> searchInMockCatalog(String keyword) {
        List<Product> allMock = MockProductCatalog.getProducts(this, "ALL");
        List<Product> results = new ArrayList<>();
        String q = keyword.toLowerCase(Locale.getDefault());
        for (Product p : allMock) {
            if (contains(p.getName(), q) || contains(p.getCategory(), q)) results.add(p);
        }
        return results;
    }

    private void showRecentState() {
        refreshRecentSearches();
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
}
