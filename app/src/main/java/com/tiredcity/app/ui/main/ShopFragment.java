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
import com.tiredcity.app.data.model.Product;
import com.tiredcity.app.data.model.search.EventItem;
import com.tiredcity.app.data.model.search.ProductItem;
import com.tiredcity.app.data.model.search.PromotionItem;
import com.tiredcity.app.data.model.search.SearchItem;
import com.tiredcity.app.data.repository.FirestoreProductRepository;
import com.tiredcity.app.databinding.FragmentShopBinding;
import com.tiredcity.app.ui.reward.VoucherDetailActivity;
import com.tiredcity.app.ui.shop.ProductDetailActivity;
import com.tiredcity.app.ui.shop.SearchActivity;
import com.tiredcity.app.utils.Constants;
import com.tiredcity.app.utils.EdgeToEdgeUtils;
import java.util.ArrayList;
import java.util.List;

/**
 * Shop tab — search discovery page: a tappable search bar, the most-searched
 * tags, and a mixed "Gợi ý cho bạn" list (promotion / event / product).
 */
public class ShopFragment extends Fragment {

    private FragmentShopBinding binding;
    private SearchAdapter suggestionAdapter;

    /** Số sản phẩm thật hiển thị trong "Gợi ý cho bạn". */
    private static final int SUGGESTION_PRODUCT_COUNT = 4;

    // Localized tags shown under "Được tìm kiếm nhiều nhất".
    private static final int[] POPULAR_TAGS = {
        R.string.tag_ao_dai,
        R.string.tag_nhat_binh,
        R.string.tag_phu_kien
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
        EdgeToEdgeUtils.applyStatusBarTopPadding(binding.shopHeader);
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
        suggestionAdapter = new SearchAdapter(buildStaticSuggestions());
        suggestionAdapter.setOnItemClickListener(item -> {
            if (item instanceof PromotionItem) {
                openVoucherDetail((PromotionItem) item);
            } else if (item instanceof ProductItem) {
                openProductDetail(((ProductItem) item).getProduct());
            } else {
                openSearch(null);
            }
        });
        binding.rvSuggestions.setLayoutManager(new LinearLayoutManager(requireContext()));
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

    /** Tải vài sản phẩm thật từ Firestore rồi bơm vào cuối danh sách gợi ý. */
    private void loadSuggestedProducts() {
        new FirestoreProductRepository().getProducts(products -> {
            if (binding == null || products == null) return;   // fragment đã bị huỷ
            List<SearchItem> combined = buildStaticSuggestions();
            int count = Math.min(SUGGESTION_PRODUCT_COUNT, products.size());
            for (int i = 0; i < count; i++) {
                combined.add(new ProductItem(products.get(i)));
            }
            suggestionAdapter.updateItems(combined);
        });
    }

    /** Mở trang chi tiết voucher của ưu đãi được bấm. */
    private void openVoucherDetail(PromotionItem item) {
        Intent intent = new Intent(requireContext(), VoucherDetailActivity.class);
        intent.putExtra(VoucherDetailActivity.EXTRA_TITLE, getString(item.getVoucherTitleResId()));
        intent.putExtra(VoucherDetailActivity.EXTRA_SUBTITLE, getString(item.getVoucherSubtitleResId()));
        intent.putExtra(VoucherDetailActivity.EXTRA_BANNER, item.getBannerRes());
        intent.putExtra(VoucherDetailActivity.EXTRA_CODE, getString(R.string.barcode_code));
        startActivity(intent);
    }

    private void openProductDetail(Product product) {
        if (product == null || product.getId() == null) return;
        Intent intent = new Intent(requireContext(), ProductDetailActivity.class);
        intent.putExtra(Constants.EXTRA_PRODUCT_ID, product.getId());
        startActivity(intent);
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
