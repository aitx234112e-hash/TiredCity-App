package com.tiredcity.app.ui.main;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.google.android.material.chip.Chip;
import com.tiredcity.app.R;
import com.tiredcity.app.adapter.BannerAdapter;
import com.tiredcity.app.adapter.ProductAdapter;
import com.tiredcity.app.data.model.Product;
import com.tiredcity.app.databinding.FragmentHomeBinding;
import com.tiredcity.app.ui.styling.ChatBotActivity;
import com.tiredcity.app.utils.LocaleHelper;
import com.tiredcity.app.utils.MenhCalculator;
import com.tiredcity.app.utils.PreferenceManager;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;

public class HomeFragment extends Fragment {

    private FragmentHomeBinding binding;
    private PreferenceManager   prefs;
    private Handler             autoScrollHandler;
    private Runnable            autoScrollRunnable;
    private BannerAdapter       bannerAdapter;

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentHomeBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        prefs = new PreferenceManager(requireContext());

        setupGreeting();
        setupMenhBanner();
        setupBanner();
        setupCategories();
        setupRecommendedProducts();
        setupHotProducts();
        setupLanguageButton();
        setupClickListeners();
    }

    @Override
    public void onResume() {
        super.onResume();
        startAutoScroll();
    }

    @Override
    public void onPause() {
        super.onPause();
        stopAutoScroll();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        stopAutoScroll();
        binding = null;
    }

    // ── Greeting ──────────────────────────────────────────────────────────────

    private void setupGreeting() {
        int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        String greeting;
        if (hour < 12)      greeting = getString(R.string.greeting_morning);
        else if (hour < 18) greeting = getString(R.string.greeting_afternoon);
        else                greeting = getString(R.string.greeting_evening);

        binding.tvGreeting.setText(greeting);

        String name = "";
        if (prefs.getUser() != null) name = prefs.getUser().getDisplayName();
        binding.tvUserName.setText(name.isEmpty() ? getString(R.string.app_name) : name);
    }

    // ── Menh banner ───────────────────────────────────────────────────────────

    private void setupMenhBanner() {
        String menh   = prefs.getMenh();
        String zodiac = prefs.getZodiac();

        if (menh == null || menh.isEmpty()) {
            binding.cvMenhBanner.setVisibility(View.GONE);
            return;
        }
        binding.cvMenhBanner.setVisibility(View.VISIBLE);
        binding.tvMenhEmoji.setText(MenhCalculator.getEmojiMenh(menh));
        binding.tvMenhElement.setText(getString(R.string.menh_label, menh));

        String zodiacText = (zodiac != null && !zodiac.isEmpty())
                ? getString(R.string.zodiac_label, zodiac)
                : getString(R.string.menh_tap_hint);
        binding.tvMenhZodiac.setText(zodiacText);
    }

    // ── Banner ViewPager2 ─────────────────────────────────────────────────────

    private void setupBanner() {
        List<BannerAdapter.BannerItem> items = new ArrayList<>();
        items.add(new BannerAdapter.BannerItem("",
                getString(R.string.banner_title_1), Color.parseColor("#2C1810")));
        items.add(new BannerAdapter.BannerItem("",
                getString(R.string.banner_title_2), Color.parseColor("#1A2C10")));
        items.add(new BannerAdapter.BannerItem("",
                getString(R.string.banner_title_3), Color.parseColor("#10182C")));

        bannerAdapter = new BannerAdapter(items);
        binding.vpBanner.setAdapter(bannerAdapter);
        binding.dotsIndicator.attachTo(binding.vpBanner);

        autoScrollHandler  = new Handler(Looper.getMainLooper());
        autoScrollRunnable = () -> {
            if (bannerAdapter.getItemCount() == 0) return;
            int next = (binding.vpBanner.getCurrentItem() + 1) % bannerAdapter.getItemCount();
            binding.vpBanner.setCurrentItem(next, true);
            autoScrollHandler.postDelayed(autoScrollRunnable, 4000L);
        };
    }

    private void startAutoScroll() {
        if (autoScrollHandler != null && autoScrollRunnable != null) {
            autoScrollHandler.postDelayed(autoScrollRunnable, 4000L);
        }
    }

    private void stopAutoScroll() {
        if (autoScrollHandler != null && autoScrollRunnable != null) {
            autoScrollHandler.removeCallbacks(autoScrollRunnable);
        }
    }

    // ── Category chips ────────────────────────────────────────────────────────

    private void setupCategories() {
        String[] labels = {
            getString(R.string.category_all),
            getString(R.string.category_ao_dai),
            getString(R.string.category_ao_tac),
            getString(R.string.category_nhat_binh),
            getString(R.string.category_accessories),
            getString(R.string.category_set)
        };

        for (int i = 0; i < labels.length; i++) {
            Chip chip = new Chip(requireContext());
            chip.setText(labels[i]);
            chip.setCheckable(true);
            chip.setChecked(i == 0);
            chip.setChipBackgroundColorResource(R.color.selector_chip_bg);
            chip.setTextColor(requireContext().getColorStateList(R.color.selector_chip_text));
            final String category = i == 0 ? null : labels[i];
            chip.setOnCheckedChangeListener((v, checked) -> {
                if (checked) filterByCategory(category);
            });
            binding.chipGroupCategories.addView(chip);
        }
    }

    private void filterByCategory(String category) {
        // Future: reload products with the given category filter
    }

    // ── Recommended products ──────────────────────────────────────────────────

    private void setupRecommendedProducts() {
        List<Product> products = buildMockProducts(true);
        ProductAdapter adapter = new ProductAdapter(products);
        adapter.setOnProductClickListener(new ProductAdapter.OnProductClickListener() {
            @Override
            public void onProductClick(Product product) {
                openProductDetail(product.getId());
            }
            @Override
            public void onSaveToggle(Product product, boolean saved) { /* handle wishlist */ }
        });

        binding.rvRecommended.setLayoutManager(
                new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false));
        binding.rvRecommended.setAdapter(adapter);
    }

    // ── Hot products ──────────────────────────────────────────────────────────

    private void setupHotProducts() {
        List<Product> products = buildMockProducts(false);
        ProductAdapter adapter = new ProductAdapter(products);
        adapter.setOnProductClickListener(new ProductAdapter.OnProductClickListener() {
            @Override
            public void onProductClick(Product product) {
                openProductDetail(product.getId());
            }
            @Override
            public void onSaveToggle(Product product, boolean saved) { /* handle wishlist */ }
        });

        binding.rvHotProducts.setLayoutManager(
                new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false));
        binding.rvHotProducts.setAdapter(adapter);
    }

    // ── Language button ───────────────────────────────────────────────────────

    private void setupLanguageButton() {
        String lang = prefs.getLanguage();
        binding.btnLanguage.setText("vi".equals(lang) ? "EN" : "VI");

        binding.btnLanguage.setOnClickListener(v -> {
            String newLang = LocaleHelper.toggleLanguage(requireContext());
            binding.btnLanguage.setText("vi".equals(newLang) ? "EN" : "VI");
            requireActivity().recreate();
        });
    }

    // ── Click listeners ───────────────────────────────────────────────────────

    private void setupClickListeners() {
        binding.cvMenhBanner.setOnClickListener(v ->
                Navigation.findNavController(requireView())
                          .navigate(R.id.stylingFragment));

        binding.cvAiStrip.setOnClickListener(v ->
                startActivity(new Intent(requireContext(), ChatBotActivity.class)));
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void openProductDetail(String productId) {
        Intent intent = new Intent(requireContext(),
                com.tiredcity.app.ui.shop.ProductDetailActivity.class);
        intent.putExtra(com.tiredcity.app.utils.Constants.EXTRA_PRODUCT_ID, productId);
        startActivity(intent);
    }

    /** Builds mock Product data for offline / dev preview. */
    private List<Product> buildMockProducts(boolean recommended) {
        String[][] data = {
            {"1", "Áo Dài Cổ Truyền",     "Lụa tơ tằm",  "850000", "10", "4.8"},
            {"2", "Nhật Bình Hoàng Hậu",  "Gấm thêu",    "1200000","15", "4.9"},
            {"3", "Áo Tấc Nam Quan",       "Đũi tơ",      "650000", "0",  "4.5"},
            {"4", "Khăn Đóng Truyền Thống","Lụa mềm",     "250000", "5",  "4.3"},
            {"5", "Bộ Áo Dài Cưới",        "Lụa cao cấp", "2500000","20", "5.0"},
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
            p.setNew(recommended);
            list.add(p);
        }
        return list;
    }
}
