package com.tiredcity.app.ui.main;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import com.google.android.material.chip.Chip;
import com.tiredcity.app.R;
import com.tiredcity.app.adapter.ProductAdapter;
import com.tiredcity.app.data.model.Product;
import com.tiredcity.app.data.network.ApiClient;
import com.tiredcity.app.data.repository.ProductRepository;
import com.tiredcity.app.databinding.FragmentShopBinding;
import com.tiredcity.app.ui.base.BaseActivity;
import com.tiredcity.app.ui.shop.SearchActivity;
import com.tiredcity.app.utils.PreferenceManager;
import java.util.ArrayList;
import java.util.List;

/**
 * Shop tab — nạp dữ liệu Realtime từ Firebase Firestore.
 */
public class ShopFragment extends Fragment {

    private FragmentShopBinding binding;
    private ProductRepository productRepository;
    private ProductAdapter productAdapter;

    // Các thẻ tìm kiếm phổ biến.
    private static final int[] POPULAR_TAGS = {
        R.string.tag_ao_thun,
        R.string.tag_ao_croptop,
        R.string.tag_chan_vay
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
        PreferenceManager preferenceManager = new PreferenceManager(requireContext());
        productRepository = new ProductRepository(ApiClient.getApiService(preferenceManager.getToken()));
        
        setupSearchBar();
        setupPopularTags();
        setupProductGrid();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    private void setupSearchBar() {
        View.OnClickListener openSearch = v -> openSearch(null);
        binding.etSearch.setOnClickListener(openSearch);
        binding.btnFilter.setOnClickListener(openSearch);
    }

    private void setupPopularTags() {
        for (int tagRes : POPULAR_TAGS) {
            String label = getString(tagRes);
            Chip chip = new Chip(requireContext());
            chip.setText(label);
            chip.setOnClickListener(v -> openSearch(label));
            binding.chipGroupPopular.addView(chip);
        }
    }

    private void setupProductGrid() {
        productAdapter = new ProductAdapter(new ArrayList<>());
        productAdapter.setOnProductClickListener(new ProductAdapter.OnProductClickListener() {
            @Override
            public void onProductClick(Product product) {
                Intent intent = new Intent(requireContext(), com.tiredcity.app.ui.shop.ProductDetailActivity.class);
                intent.putExtra(com.tiredcity.app.utils.Constants.EXTRA_PRODUCT_ID, product.getId());
                startSmoothActivity(intent);
            }
            @Override
            public void onSaveToggle(Product product, boolean saved) {}

            @Override
            public void onAddToCartClick(Product product) {
                com.tiredcity.app.data.local.CartLocalStore cartStore = new com.tiredcity.app.data.local.CartLocalStore(requireContext());
                cartStore.addItem(new com.tiredcity.app.data.model.CartItem(product, 1));
                Toast.makeText(requireContext(), getString(R.string.success_add_cart) + " 🛒", Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(requireContext(), com.tiredcity.app.ui.cart.CartActivity.class);
                startSmoothActivity(intent);
            }
        });

        binding.rvSuggestions.setLayoutManager(new GridLayoutManager(requireContext(), 2));
        binding.rvSuggestions.setHasFixedSize(true);
        binding.rvSuggestions.setAdapter(productAdapter);

        // Nạp dữ liệu Realtime từ Firestore
        productRepository.getProductsFromFirestore(new ProductRepository.OnProductsLoadedListener() {
            @Override
            public void onSuccess(List<Product> products) {
                if (products != null && !products.isEmpty()) {
                    productAdapter.updateData(products);
                } else {
                    productAdapter.updateData(buildMockProducts());
                }
            }

            @Override
            public void onError(String message) {
                productAdapter.updateData(buildMockProducts());
            }
        });
    }

    private List<Product> buildMockProducts() {
        String[][] data = {
            {"1", "Khói Trắng Kết Duyên", "Trắng", "2890000", "0", "4.8", "onboarding_1"},
            {"2", "Lam Lụa Cố Trạch", "Xanh lam", "1590000", "0", "4.9", "onboarding_2"},
            {"3", "Kim Vũ Phong Hoa", "Vàng", "1750000", "0", "4.5", "onboarding_3"},
            {"4", "Hồng Trần Mộc Dược", "Hồng", "1290000", "0", "4.3", "onboarding_4"}
        };
        List<Product> list = new ArrayList<>();
        for (String[] row : data) {
            Product p = new Product();
            p.setId(row[0]);
            p.setName(row[1]);
            p.setMaterial(row[2]);
            p.setPrice(Double.parseDouble(row[3]));
            p.setDiscount(Integer.parseInt(row[4]));
            p.setRating(Double.parseDouble(row[5]));
            p.setImage(row[6]);
            list.add(p);
        }
        return list;
    }

    private void openSearch(@Nullable String query) {
        Intent intent = new Intent(requireContext(), SearchActivity.class);
        if (query != null) {
            intent.putExtra(SearchActivity.EXTRA_QUERY, query);
        }
        startSmoothActivity(intent);
    }

    private void startSmoothActivity(Intent intent) {
        if (getActivity() instanceof BaseActivity) {
            ((BaseActivity) getActivity()).startSmoothActivity(intent);
        } else {
            startActivity(intent);
        }
    }
}
