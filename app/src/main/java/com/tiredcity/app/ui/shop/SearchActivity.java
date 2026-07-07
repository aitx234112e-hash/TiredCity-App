package com.tiredcity.app.ui.shop;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.inputmethod.EditorInfo;

import androidx.annotation.Nullable;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.android.material.chip.Chip;
import com.tiredcity.app.R;
import com.tiredcity.app.adapter.ProductAdapter;
import com.tiredcity.app.adapter.SearchAdapter;
import com.tiredcity.app.data.local.FavoritesLocalStore;
import com.tiredcity.app.data.mock.MockProductCatalog;
import com.tiredcity.app.data.model.Product;
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
    private static final int SUGGESTION_PRODUCT_COUNT = 4;

    private ActivitySearchBinding binding;
    private FirestoreProductRepository firestoreRepository;
    private ProductAdapter productAdapter;
    private SearchAdapter suggestionAdapter;
    private List<Product> allProducts = new ArrayList<>();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySearchBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        firestoreRepository = new FirestoreProductRepository();

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

        binding.etSearch.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                performSearch(binding.etSearch.getText().toString().trim());
                return true;
            }
            return false;
        });

        addPopularChip("Áo Dài");
        addPopularChip("Nhật Bình");
        addPopularChip("Phụ Kiện");
        addPopularChip("Lụa");

        String initialQuery = getIntent().getStringExtra(EXTRA_QUERY);
        if (initialQuery != null) {
            binding.etSearch.setText(initialQuery);
            performSearch(initialQuery);
        } else {
            showRecentState();
        }

        loadProducts();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (productAdapter != null) productAdapter.notifyDataSetChanged();
    }

    private void loadProducts() {
        firestoreRepository.getProducts(products -> {
            allProducts = products != null ? products : new ArrayList<>();
            refreshSuggestionProducts();
        });
    }

    private void setupSuggestions() {
        suggestionAdapter = new SearchAdapter(buildStaticSuggestions());
        suggestionAdapter.setOnItemClickListener(item -> {
            if (item instanceof PromotionItem) openVoucherDetail((PromotionItem) item);
            else if (item instanceof ProductItem) openProductDetail(((ProductItem) item).getProduct());
        });
        binding.rvSuggestions.setLayoutManager(new LinearLayoutManager(this));
        binding.rvSuggestions.setAdapter(suggestionAdapter);
    }

    private List<SearchItem> buildStaticSuggestions() {
        List<SearchItem> items = new ArrayList<>();
        // Static items can be added here
        return items;
    }

    private void refreshSuggestionProducts() {
        List<SearchItem> items = buildStaticSuggestions();
        int count = Math.min(SUGGESTION_PRODUCT_COUNT, allProducts.size());
        for (int i = 0; i < count; i++) {
            items.add(new ProductItem(allProducts.get(i)));
        }
        suggestionAdapter.updateItems(items);
    }

    private void openVoucherDetail(PromotionItem item) {
        startActivity(new Intent(this, VoucherDetailActivity.class));
    }

    private void openProductDetail(Product product) {
        Intent intent = new Intent(this, ProductDetailActivity.class);
        intent.putExtra(Constants.EXTRA_PRODUCT_ID, product.getId());
        startSmoothActivity(intent);
    }

    private void performSearch(String keyword) {
        if (keyword.isEmpty()) { showRecentState(); return; }

        showResultsState();
        binding.swipeRefresh.setRefreshing(true);
        binding.tvResultCount.setText(getString(R.string.search_searching));

        if (allProducts.isEmpty()) {
            firestoreRepository.getProducts(products -> {
                allProducts = products != null ? products : new ArrayList<>();
                refreshSuggestionProducts();
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

    private void addPopularChip(String text) {
        Chip chip = new Chip(this);
        chip.setText(text);
        chip.setOnClickListener(v -> {
            binding.etSearch.setText(text);
            performSearch(text);
        });
        binding.chipGroupPopular.addView(chip);
    }
}
