package com.tiredcity.app.ui.main;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.google.android.material.chip.Chip;
import com.tiredcity.app.R;
import com.tiredcity.app.adapter.SearchAdapter;
import com.tiredcity.app.data.model.search.EventItem;
import com.tiredcity.app.data.model.search.ProductItem;
import com.tiredcity.app.data.model.search.PromotionItem;
import com.tiredcity.app.data.model.search.SearchItem;
import com.tiredcity.app.databinding.FragmentShopBinding;
import com.tiredcity.app.ui.shop.SearchActivity;
import java.util.Arrays;
import java.util.List;

/**
 * Shop tab — search discovery page: a tappable search bar, the most-searched
 * tags, and a mixed "Gợi ý cho bạn" list (promotion / event / product).
 */
public class ShopFragment extends Fragment {

    private FragmentShopBinding binding;

    // Localized tags shown under "Được tìm kiếm nhiều nhất".
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
        setupSearchBar();
        setupPopularTags();
        setupSuggestions();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    // ─── Search bar — opens the full search screen for typing ──────────
    private void setupSearchBar() {
        View.OnClickListener openSearch = v -> openSearch(null);
        binding.etSearch.setOnClickListener(openSearch);
        binding.btnFilter.setOnClickListener(openSearch);
    }

    // ─── "Được tìm kiếm nhiều nhất" tag chips ──────────────────────────
    private void setupPopularTags() {
        for (int tagRes : POPULAR_TAGS) {
            String label = getString(tagRes);
            Chip chip = new Chip(requireContext());
            chip.setText(label);
            chip.setOnClickListener(v -> openSearch(label));
            binding.chipGroupPopular.addView(chip);
        }
    }

    // ─── "Gợi ý cho bạn" — mixed-type discovery list ───────────────────
    private void setupSuggestions() {
        List<SearchItem> suggestions = Arrays.asList(
            new PromotionItem(R.string.search_promo_birthday),
            new EventItem(R.string.search_event_coach_title,
                          R.string.search_event_coach_time, 0),
            new ProductItem(R.string.search_brand_kangol,
                            R.string.search_product_skirt_pocket, 1_200_000, 0),
            new ProductItem(R.string.search_brand_kangol,
                            R.string.search_product_skirt_slit, 1_000_000, 0)
        );

        SearchAdapter adapter = new SearchAdapter(suggestions);
        adapter.setOnItemClickListener(item -> {
            if (item instanceof ProductItem) {
                openSearch(getString(((ProductItem) item).getBrandResId()));
            } else {
                openSearch(null);
            }
        });
        binding.rvSuggestions.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvSuggestions.setNestedScrollingEnabled(false);
        binding.rvSuggestions.setAdapter(adapter);
    }

    /** Opens SearchActivity, optionally pre-filling a query. */
    private void openSearch(@Nullable String query) {
        Intent intent = new Intent(requireContext(), SearchActivity.class);
        if (query != null) {
            intent.putExtra(SearchActivity.EXTRA_QUERY, query);
        }
        startActivity(intent);
    }
}
