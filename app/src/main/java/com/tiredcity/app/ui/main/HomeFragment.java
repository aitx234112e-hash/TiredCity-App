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
import com.tiredcity.app.R;
import com.tiredcity.app.adapter.BannerAdapter;
import com.tiredcity.app.adapter.ProductAdapter;
import com.tiredcity.app.adapter.PromoStripAdapter;
import com.tiredcity.app.data.mock.MockProductCatalog;
import com.tiredcity.app.data.model.Product;
import com.tiredcity.app.data.model.UserProfile;
import com.tiredcity.app.data.network.ApiClient;
import com.tiredcity.app.data.repository.AuthRepository;
import com.tiredcity.app.data.repository.FavoritesRepository;
import com.tiredcity.app.databinding.FragmentHomeBinding;
import com.tiredcity.app.ui.styling.AiStylingActivity;
import com.tiredcity.app.ui.styling.ChatBotActivity;
import com.tiredcity.app.utils.LocaleHelper;
import com.tiredcity.app.utils.MenhCalculator;
import com.tiredcity.app.utils.PreferenceManager;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class HomeFragment extends Fragment {

    private FragmentHomeBinding binding;
    private PreferenceManager   prefs;
    private AuthRepository      authRepository;
    private FavoritesRepository favoritesRepository;
    private Handler             autoScrollHandler;
    private Runnable            autoScrollRunnable;
    private BannerAdapter       bannerAdapter;
    private Handler             promoScrollHandler;
    private Runnable            promoScrollRunnable;
    private PromoStripAdapter   promoAdapter;
    private com.tiredcity.app.data.repository.ProductRepository productRepository;
    private ProductAdapter      recommendedAdapter;
    private ProductAdapter      hotProductsAdapter;

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
        productRepository = new com.tiredcity.app.data.repository.ProductRepository(ApiClient.getApiService(null));
        authRepository = new AuthRepository(ApiClient.getApiService(null), prefs);
        favoritesRepository = new FavoritesRepository(new com.tiredcity.app.data.local.FavoritesLocalStore(requireContext()));

        syncUserProfile();
        setupGreeting();
        setupMenhBanner();
        setupBanner();
        setupPromoStrip();
        setupRecommendedProducts();
        setupHotProducts();
        setupLanguageButton();
        setupClickListeners();
    }

    @Override
    public void onResume() {
        super.onResume();
        startAutoScroll();
        startPromoScroll();
        updateBadges();
        // Trạng thái yêu thích có thể đã đổi ở màn hình khác (chi tiết sản phẩm, tủ đồ...)
        if (recommendedAdapter != null) recommendedAdapter.notifyDataSetChanged();
        if (hotProductsAdapter != null) hotProductsAdapter.notifyDataSetChanged();
    }

    @Override
    public void onPause() {
        super.onPause();
        stopAutoScroll();
        stopPromoScroll();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        stopAutoScroll();
        stopPromoScroll();
        binding = null;
        recommendedAdapter = null;
        hotProductsAdapter = null;
    }

    private void syncUserProfile() {
        authRepository.syncAccount(prefs.getUser(), cloudProfile -> {
            if (isAdded() && cloudProfile != null) {
                requireActivity().runOnUiThread(() -> {
                    setupGreeting();
                    setupMenhBanner();
                });
            }
        });
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
        binding.tvUserName.setText(name.isEmpty() ? getString(R.string.greeting_guest) : name);
    }

    // ── Menh banner ───────────────────────────────────────────────────────────

    private void setupMenhBanner() {
        UserProfile profile = prefs.getUser();
        String menh = null;
        String zodiac = null;

        if (profile != null && profile.getBirthDate() != null && profile.getBirthDate().length() >= 10) {
            try {
                int year = Integer.parseInt(profile.getBirthDate().substring(0, 4));
                int month = Integer.parseInt(profile.getBirthDate().substring(5, 7));
                int day = Integer.parseInt(profile.getBirthDate().substring(8, 10));

                // LUÔN TÍNH LẠI ĐỂ ĐẢM BẢO CHÍNH XÁC THEO CÔNG THỨC MỚI
                menh = MenhCalculator.tinhMenh(year);
                zodiac = MenhCalculator.tinhCungHoangDao(month, day);
                String animal = MenhCalculator.tinhConGiap(year);

                // Cập nhật lại cache và Cloud nếu có sự thay đổi (Self-Healing)
                if (!menh.equals(prefs.getMenh()) || !zodiac.equals(prefs.getZodiac())) {
                    prefs.setMenh(menh);
                    prefs.setZodiac(zodiac);
                    
                    profile.setMenh(menh);
                    profile.setZodiac(zodiac);
                    profile.setAnimal(animal);
                    prefs.saveUser(profile);
                    
                    // Đồng bộ ngay lên Cloud để các máy khác cũng nhận dữ liệu đúng
                    authRepository.syncUserProfileToFirestore(profile);
                }
            } catch (Exception ignored) {
                menh = prefs.getMenh();
                zodiac = prefs.getZodiac();
            }
        } else {
            menh = prefs.getMenh();
            zodiac = prefs.getZodiac();
        }

        if (menh == null || menh.isEmpty()) {
            binding.cvMenhBanner.setVisibility(View.GONE);
            binding.cardMenhCta.setVisibility(View.VISIBLE);
            binding.cardMenhCta.setOnClickListener(v ->
                    startActivity(new Intent(requireContext(), AiStylingActivity.class)));
            return;
        }

        binding.cardMenhCta.setVisibility(View.GONE);
        binding.cvMenhBanner.setVisibility(View.VISIBLE);
        binding.tvMenhEmoji.setText(MenhCalculator.getEmojiMenh(menh));
        binding.tvMenhElement.setText(getString(R.string.menh_label,
                MenhCalculator.localizeMenh(requireContext(), menh)));

        String zodiacText = (zodiac != null && !zodiac.isEmpty())
                ? getString(R.string.zodiac_label,
                        MenhCalculator.localizeZodiac(requireContext(), zodiac))
                : getString(R.string.menh_tap_hint);
        binding.tvMenhZodiac.setText(zodiacText);

        buildColorChips(menh);
    }

    private void buildColorChips(String menh) {
        android.widget.LinearLayout container = binding.layoutMenhColors;
        container.removeAllViews();

        String[] colors = MenhCalculator.getMauHopMenh(menh);
        for (String colorName : colors) {
            android.widget.TextView chip = new android.widget.TextView(requireContext());
            chip.setText(MenhCalculator.localizeColor(requireContext(), colorName));
            chip.setTextColor(android.graphics.Color.WHITE);
            chip.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 11);
            chip.setTypeface(null, android.graphics.Typeface.BOLD);
            chip.setBackgroundResource(R.drawable.tc_bg_color_chip);
            chip.setGravity(android.view.Gravity.CENTER);

            int hPad = (int) (12 * getResources().getDisplayMetrics().density);
            int vPad = (int) (5 * getResources().getDisplayMetrics().density);
            chip.setPadding(hPad, vPad, hPad, vPad);

            android.widget.LinearLayout.LayoutParams lp =
                    new android.widget.LinearLayout.LayoutParams(
                            android.widget.LinearLayout.LayoutParams.WRAP_CONTENT,
                            android.widget.LinearLayout.LayoutParams.WRAP_CONTENT);
            lp.setMarginEnd((int) (6 * getResources().getDisplayMetrics().density));
            chip.setLayoutParams(lp);

            container.addView(chip);
        }
    }

    // ── Banner ViewPager2 ─────────────────────────────────────────────────────

    private void setupBanner() {
        List<BannerAdapter.BannerItem> items = new ArrayList<>();
        items.add(new BannerAdapter.BannerItem(R.drawable.banner_1, getString(R.string.banner_title_1)));
        items.add(new BannerAdapter.BannerItem(R.drawable.banner_2, getString(R.string.banner_title_2)));
        items.add(new BannerAdapter.BannerItem(R.drawable.banner_3, getString(R.string.banner_title_3)));

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

    // ── Promo strip (mini banner giữa Events và News) ─────────────────────────

    private void setupPromoStrip() {
        List<PromoStripAdapter.PromoItem> items = new ArrayList<>();
        items.add(new PromoStripAdapter.PromoItem(
                R.drawable.banner_1,
                getString(R.string.promo_strip_eyebrow),
                getString(R.string.promo_strip_1_title),
                getString(R.string.promo_strip_1_sub)));
        items.add(new PromoStripAdapter.PromoItem(
                R.drawable.banner_2,
                getString(R.string.promo_strip_2_eyebrow),
                getString(R.string.promo_strip_2_title),
                getString(R.string.promo_strip_2_sub)));
        items.add(new PromoStripAdapter.PromoItem(
                R.drawable.banner_3,
                getString(R.string.promo_strip_3_eyebrow),
                getString(R.string.promo_strip_3_title),
                getString(R.string.promo_strip_3_sub)));

        promoAdapter = new PromoStripAdapter(items);
        binding.vpPromoStrip.setAdapter(promoAdapter);
        binding.dotsPromo.attachTo(binding.vpPromoStrip);

        promoScrollHandler  = new Handler(Looper.getMainLooper());
        promoScrollRunnable = () -> {
            if (promoAdapter.getItemCount() == 0) return;
            int next = (binding.vpPromoStrip.getCurrentItem() + 1) % promoAdapter.getItemCount();
            binding.vpPromoStrip.setCurrentItem(next, true);
            promoScrollHandler.postDelayed(promoScrollRunnable, 3000L);
        };
    }

    private void startPromoScroll() {
        if (promoScrollHandler != null && promoScrollRunnable != null) {
            promoScrollHandler.postDelayed(promoScrollRunnable, 3000L);
        }
    }

    private void stopPromoScroll() {
        if (promoScrollHandler != null && promoScrollRunnable != null) {
            promoScrollHandler.removeCallbacks(promoScrollRunnable);
        }
    }

    // ── Recommended products ──────────────────────────────────────────────────

    private void setupRecommendedProducts() {
        binding.rvRecommended.setLayoutManager(
                new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false));
        
        // CHUYỂN SANG DÙNG FIREBASE THẬT
        productRepository.getProductsFromFirestore(new com.tiredcity.app.data.repository.ProductRepository.OnProductsLoadedListener() {
            @Override
            public void onSuccess(List<Product> products) {
                if (!isAdded() || products == null) return;
                
                // Lọc lấy 5 sản phẩm đầu tiên làm gợi ý
                List<Product> recommended = products.size() > 5 ? products.subList(0, 5) : products;
                recommendedAdapter = new ProductAdapter(recommended);
                setupAdapterListeners(recommendedAdapter);
                binding.rvRecommended.setAdapter(recommendedAdapter);
            }

            @Override
            public void onError(String message) {
                // Fallback chỉ khi mất mạng
                recommendedAdapter = new ProductAdapter(MockProductCatalog.getHomeHighlights(requireContext(), true));
                setupAdapterListeners(recommendedAdapter);
                binding.rvRecommended.setAdapter(recommendedAdapter);
            }
        });
    }

    private void setupHotProducts() {
        binding.rvHotProducts.setLayoutManager(
                new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false));

        productRepository.getProductsFromFirestore(new com.tiredcity.app.data.repository.ProductRepository.OnProductsLoadedListener() {
            @Override
            public void onSuccess(List<Product> products) {
                if (!isAdded() || products == null) return;
                
                // Lấy các sản phẩm tiếp theo làm sản phẩm nổi bật
                List<Product> hot = products.size() > 10 ? products.subList(5, 10) : products;
                hotProductsAdapter = new ProductAdapter(hot);
                setupAdapterListeners(hotProductsAdapter);
                binding.rvHotProducts.setAdapter(hotProductsAdapter);
            }

            @Override
            public void onError(String message) {
                hotProductsAdapter = new ProductAdapter(MockProductCatalog.getHomeHighlights(requireContext(), false));
                setupAdapterListeners(hotProductsAdapter);
                binding.rvHotProducts.setAdapter(hotProductsAdapter);
            }
        });
    }

    private void setupAdapterListeners(ProductAdapter adapter) {
        adapter.setOnProductClickListener(new ProductAdapter.OnProductClickListener() {
            @Override
            public void onProductClick(Product product) {
                openProductDetail(product.getId());
            }
            @Override
            public void onSaveToggle(Product product, boolean saved) {
                favoritesRepository.syncFavoritesToCloud();
            }
            @Override
            public void onAddToCartClick(Product product) {
                openProductDetail(product.getId());
                android.widget.Toast.makeText(requireContext(), "Vui lòng chọn Size trước khi thêm vào giỏ", android.widget.Toast.LENGTH_SHORT).show();
            }
        });
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
                startActivity(new Intent(requireContext(), AiStylingActivity.class)));

        binding.cvAiStrip.setOnClickListener(v ->
                startActivity(new Intent(requireContext(), ChatBotActivity.class)));

        binding.btnCart.setOnClickListener(v ->
                startActivity(new Intent(requireContext(),
                        com.tiredcity.app.ui.cart.CartActivity.class)));

        binding.btnNotification.setOnClickListener(v ->
                startActivity(new Intent(requireContext(),
                        com.tiredcity.app.ui.notification.NotificationActivity.class)));

        // "Xem tất cả" và "Bộ Trọn" (không có tab riêng ở trang Danh mục) → ShopFragment
        View.OnClickListener catClick = v ->
                Navigation.findNavController(requireView()).navigate(R.id.shopFragment);
        binding.tvCategoriesSeeAll.setOnClickListener(catClick);
        binding.cardCatSet.setOnClickListener(catClick);

        // 4 thẻ còn lại → mở đúng tab tương ứng ở trang Danh mục (tab biểu tượng móc áo)
        binding.cardCatAoDai.setOnClickListener(v -> openStylingCategory("ÁO DÀI"));
        binding.cardCatAoTac.setOnClickListener(v -> openStylingCategory("ÁO TẤC"));
        binding.cardCatNhatBinh.setOnClickListener(v -> openStylingCategory("NHẬT BÌNH"));
        binding.cardCatAccessories.setOnClickListener(v -> openStylingCategory("PHỤ KIỆN"));

        // Sự kiện → trang Sự kiện riêng (EventActivity)
        View.OnClickListener eventsClick = v ->
                startActivity(new Intent(requireContext(),
                        com.tiredcity.app.ui.explore.EventActivity.class));
        binding.cardEvents.setOnClickListener(eventsClick);
        binding.tvEventsSeeAll.setOnClickListener(eventsClick);

        // Tin tức → trang Tin tức riêng (ArticleActivity)
        View.OnClickListener newsClick = v ->
                startActivity(new Intent(requireContext(),
                        com.tiredcity.app.ui.explore.ArticleActivity.class));
        binding.cardNews.setOnClickListener(newsClick);
        binding.tvNewsSeeAll.setOnClickListener(newsClick);
    }

    /** Mở trang Danh mục (tab móc áo) và chọn sẵn đúng nhóm trang phục đã bấm ở Trang chủ. */
    private void openStylingCategory(String categoryId) {
        Bundle args = new Bundle();
        args.putString(StylingFragment.ARG_CATEGORY_ID, categoryId);
        Navigation.findNavController(requireView()).navigate(R.id.stylingFragment, args);
    }

    // ── Badges (giỏ hàng + thông báo) ─────────────────────────────────────────

    /** Cập nhật số trên icon giỏ hàng & thông báo; gọi lại mỗi khi quay về màn hình. */
    private void updateBadges() {
        if (binding == null) return;

        int cartCount = 0;
        for (com.tiredcity.app.data.model.CartItem item :
                new com.tiredcity.app.data.local.CartLocalStore(requireContext()).getCartItems()) {
            cartCount += item.getQuantity();
        }
        bindBadge(binding.tvCartBadge, cartCount);

        int notifCount = new com.tiredcity.app.data.local.NotificationStore(requireContext())
                .getUnreadCount();
        bindBadge(binding.tvNotifBadge, notifCount);
    }

    /** Hiện badge nếu count > 0, tối đa hiển thị "9+". */
    private void bindBadge(android.widget.TextView badge, int count) {
        if (count <= 0) {
            badge.setVisibility(View.GONE);
        } else {
            badge.setVisibility(View.VISIBLE);
            badge.setText(count > 9 ? "9+" : String.valueOf(count));
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void openProductDetail(String productId) {
        Intent intent = new Intent(requireContext(),
                com.tiredcity.app.ui.shop.ProductDetailActivity.class);
        intent.putExtra(com.tiredcity.app.utils.Constants.EXTRA_PRODUCT_ID, productId);
        startActivity(intent);
    }

}
