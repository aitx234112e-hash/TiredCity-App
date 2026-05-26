package com.tiredcity.app.ui.shop;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import androidx.recyclerview.widget.GridLayoutManager;
import com.tiredcity.app.adapter.ProductAdapter;
import com.tiredcity.app.data.model.ApiListResponse;
import com.tiredcity.app.data.model.Product;
import com.tiredcity.app.data.network.ApiClient;
import com.tiredcity.app.data.repository.ProductRepository;
import com.tiredcity.app.databinding.ActivitySearchBinding;
import com.tiredcity.app.ui.base.BaseActivity;
import com.tiredcity.app.utils.Constants;
import java.util.List;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SearchActivity extends BaseActivity {

    private ActivitySearchBinding binding;
    private ProductRepository productRepository;
    private ProductAdapter productAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySearchBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        productRepository = new ProductRepository(ApiClient.getApiService(preferenceManager.getToken()));

        // Setup results RecyclerView
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

        // Pre-populate popular searches
        addPopularChip("Áo dài");
        addPopularChip("Áo Tấc");
        addPopularChip("Nhật Bình");
        addPopularChip("Phụ kiện");
        addPopularChip("Lụa tơ tằm");

        // Focus search field
        binding.etSearch.requestFocus();
    }

    private void performSearch(String keyword) {
        if (keyword.isEmpty()) { showRecentState(); return; }

        showResultsState();
        binding.swipeRefresh.setRefreshing(true);
        binding.tvResultCount.setText("Đang tìm…");

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
                            binding.tvResultCount.setText("Tìm thấy " + results.size() + " kết quả");
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
        binding.tvEmptyMessage.setText("Không tìm thấy \"" + keyword + "\"");
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
