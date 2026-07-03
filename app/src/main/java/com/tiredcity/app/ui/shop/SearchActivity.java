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
import com.tiredcity.app.data.network.ApiClient;
import com.tiredcity.app.data.repository.ProductRepository;
import com.tiredcity.app.databinding.ActivitySearchBinding;
import com.tiredcity.app.ui.base.BaseActivity;
import com.tiredcity.app.ui.reward.VoucherDetailActivity;
import com.tiredcity.app.utils.Constants;
import java.util.Arrays;
import java.util.List;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SearchActivity extends BaseActivity {

    public static final String EXTRA_QUERY = "extra_query";

    private ActivitySearchBinding binding;
    private ProductRepository productRepository;
    private ProductAdapter productAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySearchBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        productRepository = new ProductRepository(ApiClient.getApiService(preferenceManager.getToken()));

        productAdapter = new ProductAdapter(null);
        productAdapter.setOnProductClickListener(new ProductAdapter.OnProductClickListener() {
            @Override
            public void onProductClick(Product product) {
                Intent intent = new Intent(SearchActivity.this, ProductDetailActivity.class);
                intent.putExtra(Constants.EXTRA_PRODUCT_ID, product.getId());
                startActivity(intent);
            }

            @Override
            public void onSaveToggle(Product product, boolean saved) {}

            @Override
            public void onAddToCartClick(Product product) {
                new CartLocalStore(SearchActivity.this).addItem(new CartItem(product, 1));
                Toast.makeText(SearchActivity.this, getString(R.string.success_add_cart) + " 🛒", Toast.LENGTH_SHORT).show();
            }
        });
        binding.rvResults.setLayoutManager(new GridLayoutManager(this, 2));
        binding.rvResults.setAdapter(productAdapter);

        binding.btnBack.setOnClickListener(v -> finish());

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

        addPopularChip(getString(R.string.tag_ao_thun));
        addPopularChip(getString(R.string.tag_ao_croptop));
        addPopularChip(getString(R.string.tag_chan_vay));

        setupSuggestions();

        String initialQuery = getIntent().getStringExtra(EXTRA_QUERY);
        if (initialQuery != null && !initialQuery.trim().isEmpty()) {
            binding.etSearch.setText(initialQuery);
            binding.etSearch.setSelection(initialQuery.length());
            performSearch(initialQuery.trim());
        } else {
            binding.etSearch.requestFocus();
        }
    }

    private void setupSuggestions() {
        List<SearchItem> suggestions = Arrays.asList(
            new PromotionItem(R.string.search_promo_birthday,
                              R.drawable.banner_1,
                              R.string.reward_voucher_birthday_title,
                              R.string.reward_voucher_birthday_subtitle),
            new EventItem(R.string.search_event_coach_title,
                          R.string.search_event_coach_time, 0),
            new ProductItem(R.string.search_brand_kangol,
                            R.string.search_product_skirt_pocket, 1_200_000, 0),
            new ProductItem(R.string.search_brand_kangol,
                            R.string.search_product_skirt_slit, 1_000_000, 0)
        );

        SearchAdapter adapter = new SearchAdapter(suggestions);
        adapter.setOnItemClickListener(item -> {
            if (item instanceof PromotionItem) {
                openVoucherDetail((PromotionItem) item);
            } else if (item instanceof ProductItem) {
                performSearch(getString(((ProductItem) item).getBrandResId()));
            }
        });
        binding.rvSuggestions.setLayoutManager(new LinearLayoutManager(this));
        binding.rvSuggestions.setNestedScrollingEnabled(false);
        binding.rvSuggestions.setAdapter(adapter);
    }

    private void openVoucherDetail(PromotionItem item) {
        Intent intent = new Intent(this, VoucherDetailActivity.class);
        intent.putExtra(VoucherDetailActivity.EXTRA_TITLE, getString(item.getVoucherTitleResId()));
        intent.putExtra(VoucherDetailActivity.EXTRA_SUBTITLE, getString(item.getVoucherSubtitleResId()));
        intent.putExtra(VoucherDetailActivity.EXTRA_BANNER, item.getBannerRes());
        intent.putExtra(VoucherDetailActivity.EXTRA_CODE, getString(R.string.barcode_code));
        startActivity(intent);
    }

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
                    if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                        List<Product> results = response.body().getData();
                        if (results == null || results.isEmpty()) {
                            showEmptyState(keyword);
                        } else {
                            binding.tvResultCount.setText(getString(
                                    R.string.search_result_count, results.size()));
                            productAdapter.updateData(results);
                        }
                    } else {
                        showEmptyState(keyword);
                    }
                }

                @Override
                public void onFailure(Call<ApiListResponse<Product>> call, Throwable t) {
                    binding.swipeRefresh.setRefreshing(false);
                    showEmptyState(keyword);
                }
            });
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
