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
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.android.material.chip.Chip;
import com.tiredcity.app.R;
import com.tiredcity.app.adapter.ProductAdapter;
import com.tiredcity.app.adapter.SearchAdapter;
import com.tiredcity.app.data.model.Product;
import com.tiredcity.app.data.model.search.EventItem;
import com.tiredcity.app.data.model.search.ProductItem;
import com.tiredcity.app.data.model.search.PromotionItem;
import com.tiredcity.app.data.model.search.SearchItem;
import com.tiredcity.app.data.network.ApiClient;
import com.tiredcity.app.data.repository.FirestoreProductRepository;
import com.tiredcity.app.data.repository.ProductRepository;
import com.tiredcity.app.databinding.FragmentShopBinding;
import com.tiredcity.app.ui.base.BaseActivity;
import com.tiredcity.app.ui.shop.ProductDetailActivity;
import com.tiredcity.app.ui.shop.SearchActivity;
import com.tiredcity.app.utils.Constants;
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
    private SearchAdapter suggestionAdapter;

    private static final int SUGGESTION_PRODUCT_COUNT = 4;
    private static final int[] POPULAR_TAGS = {
        R.string.tag_ao_dai,
        R.string.tag_nhat_binh,
        R.string.tag_phu_kien
    };

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
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
        setupSuggestions();
        loadSuggestedProducts();
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

    private void setupSuggestions() {
        suggestionAdapter = new SearchAdapter(buildStaticSuggestions());
        suggestionAdapter.setOnItemClickListener(item -> {
            if (item instanceof ProductItem) {
                openProductDetail(((ProductItem) item).getProduct());
            } else {
                openSearch(null);
            }
        });
        binding.rvSuggestions.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvSuggestions.setNestedScrollingEnabled(false);
        binding.rvSuggestions.setAdapter(suggestionAdapter);
    }

    private List<SearchItem> buildStaticSuggestions() {
        List<SearchItem> items = new ArrayList<>();
        items.add(new PromotionItem(R.string.search_promo_birthday, R.drawable.banner_1, R.string.reward_voucher_birthday_title, R.string.reward_voucher_birthday_subtitle));
        items.add(new EventItem(R.string.search_event_coach_title, R.string.search_event_coach_time, 0));
        return items;
    }

    private void loadSuggestedProducts() {
        new FirestoreProductRepository().getProducts(products -> {
            if (binding == null || products == null) return;
            List<SearchItem> combined = buildStaticSuggestions();
            int count = Math.min(SUGGESTION_PRODUCT_COUNT, products.size());
            for (int i = 0; i < count; i++) {
                combined.add(new ProductItem(products.get(i)));
            }
            if (suggestionAdapter != null) {
                suggestionAdapter.updateItems(combined);
            }
        });
    }

    private void openProductDetail(Product product) {
        if (product == null || product.getId() == null) return;
        Intent intent = new Intent(requireContext(), ProductDetailActivity.class);
        intent.putExtra(Constants.EXTRA_PRODUCT_ID, product.getId());
        startActivity(intent);
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
