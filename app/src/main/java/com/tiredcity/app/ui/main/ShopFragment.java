package com.tiredcity.app.ui.main;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import com.google.android.material.chip.Chip;
import com.tiredcity.app.R;
import com.tiredcity.app.adapter.ProductAdapter;
import com.tiredcity.app.data.model.ApiListResponse;
import com.tiredcity.app.data.model.Product;
import com.tiredcity.app.data.network.ApiClient;
import com.tiredcity.app.data.repository.ProductRepository;
import com.tiredcity.app.databinding.FragmentShopBinding;
import com.tiredcity.app.ui.shop.ProductDetailActivity;
import com.tiredcity.app.ui.shop.SearchActivity;
import com.tiredcity.app.utils.Constants;
import com.tiredcity.app.utils.PreferenceManager;
import java.util.List;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ShopFragment extends Fragment {

    private FragmentShopBinding binding;
    private ProductRepository productRepository;
    private ProductAdapter productAdapter;
    private String selectedCategory = null;
    private int currentPage = 1;

    private static final String[] CATEGORIES = {
        "Tất cả", "Áo Dài", "Áo Tấc", "Nhật Bình", "Phụ Kiện", "Bộ Trọn"
    };

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentShopBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        PreferenceManager prefs = new PreferenceManager(requireContext());
        productRepository = new ProductRepository(ApiClient.getApiService(prefs.getToken()));

        setupCategoryChips();
        setupProductList();
        setupSearch();

        binding.swipeRefresh.setColorSchemeColors(
            requireContext().getColor(R.color.brand_gold));
        binding.swipeRefresh.setOnRefreshListener(() -> {
            currentPage = 1;
            loadProducts();
        });

        loadProducts();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    private void setupCategoryChips() {
        for (int i = 0; i < CATEGORIES.length; i++) {
            Chip chip = new Chip(requireContext());
            chip.setText(CATEGORIES[i]);
            chip.setCheckable(true);
            chip.setChecked(i == 0);
            final String cat = (i == 0) ? null : CATEGORIES[i];
            chip.setOnCheckedChangeListener((v, checked) -> {
                if (checked) {
                    selectedCategory = cat;
                    currentPage = 1;
                    loadProducts();
                }
            });
            binding.chipGroupFilter.addView(chip);
        }
    }

    private void setupProductList() {
        productAdapter = new ProductAdapter(null);
        productAdapter.setOnProductClickListener(new ProductAdapter.OnProductClickListener() {
            @Override
            public void onProductClick(Product product) {
                Intent intent = new Intent(requireContext(), ProductDetailActivity.class);
                intent.putExtra(Constants.EXTRA_PRODUCT_ID, product.getId());
                startActivity(intent);
            }

            @Override
            public void onSaveToggle(Product product, boolean saved) {}
        });

        binding.rvProducts.setLayoutManager(new GridLayoutManager(requireContext(), 2));
        binding.rvProducts.setAdapter(productAdapter);
    }

    private void setupSearch() {
        binding.etSearch.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                String query = binding.etSearch.getText().toString().trim();
                if (!query.isEmpty()) {
                    Intent intent = new Intent(requireContext(), SearchActivity.class);
                    startActivity(intent);
                }
                return true;
            }
            return false;
        });

        // Clicking the search bar also opens SearchActivity
        binding.etSearch.setOnClickListener(v ->
            startActivity(new Intent(requireContext(), SearchActivity.class)));
    }

    private void loadProducts() {
        binding.swipeRefresh.setRefreshing(true);
        productRepository.getProducts(currentPage, 20, selectedCategory, null)
            .enqueue(new Callback<ApiListResponse<Product>>() {
                @Override
                public void onResponse(Call<ApiListResponse<Product>> call,
                                       Response<ApiListResponse<Product>> response) {
                    if (binding == null) return;
                    binding.swipeRefresh.setRefreshing(false);
                    if (response.isSuccessful() && response.body() != null
                        && response.body().isSuccess()) {
                        List<Product> products = response.body().getData();
                        productAdapter.updateData(products);
                    }
                }

                @Override
                public void onFailure(Call<ApiListResponse<Product>> call, Throwable t) {
                    if (binding != null) binding.swipeRefresh.setRefreshing(false);
                }
            });
    }
}
