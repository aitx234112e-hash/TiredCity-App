package com.tiredcity.app.ui.main;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.content.Intent;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.VideoView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.widget.NestedScrollView;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.viewpager2.widget.ViewPager2;

import com.bumptech.glide.Glide;
import com.tiredcity.app.R;
import com.tiredcity.app.adapter.HotProductCarouselAdapter;
import com.tiredcity.app.adapter.MediaBannerAdapter;
import com.tiredcity.app.adapter.ProductAdapter;
import com.tiredcity.app.adapter.PromoStripAdapter;
import com.tiredcity.app.data.mock.MockProductCatalog;
import com.tiredcity.app.data.model.Article;
import com.tiredcity.app.data.model.Event;
import com.tiredcity.app.data.model.Product;
import com.tiredcity.app.data.model.UserProfile;
import com.tiredcity.app.data.network.ApiClient;
import com.tiredcity.app.data.repository.ArticleRepository;
import com.tiredcity.app.data.repository.AuthRepository;
import com.tiredcity.app.data.repository.EventRepository;
import com.tiredcity.app.data.repository.FavoritesRepository;
import com.tiredcity.app.data.repository.FirestoreProductRepository;
import com.tiredcity.app.data.repository.ProductRepository;
import com.tiredcity.app.databinding.FragmentHomeBinding;
import com.tiredcity.app.ui.styling.AiStylingActivity;
import com.tiredcity.app.ui.styling.ChatBotActivity;
import com.tiredcity.app.utils.EdgeToEdgeUtils;
import com.tiredcity.app.utils.LocaleHelper;
import com.tiredcity.app.utils.MenhCalculator;
import com.tiredcity.app.utils.PreferenceManager;
import com.tiredcity.app.utils.PriceUtils;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class HomeFragment extends Fragment {

    private FragmentHomeBinding binding;
    private PreferenceManager prefs;
    private AuthRepository authRepository;
    private FavoritesRepository favoritesRepository;
    private ProductRepository productRepository;
    private EventRepository eventRepository;
    private ArticleRepository articleRepository;
    private FirestoreProductRepository firestoreRepository;

    private MediaBannerAdapter bannerAdapter;
    private MediaBannerAdapter banner2Adapter;
    private PromoStripAdapter promoAdapter;
    private HotProductCarouselAdapter hotProductsAdapter;

    private String latestEventId;
    private String latestNewsId;

    private Handler autoScrollHandler;
    private Runnable autoScrollRunnable;
    private Handler promoScrollHandler;
    private Runnable promoScrollRunnable;
    private Handler hotScrollHandler;
    private Runnable hotScrollRunnable;

    private List<View> recommendedCards;
    private Handler recommendedSpotlightHandler;
    private Runnable recommendedSpotlightRunnable;
    private int recommendedSpotlightIndex = 0;

    private List<VideoSection> videoSections;
    private List<CategoryFlipCard> categoryFlipCards;
    private Handler categoryFlipHandler;
    private Runnable categoryFlipRunnable;
    private List<ShimmerSweep> shimmerSweeps;
    private List<ObjectAnimator> ctaPulseAnimators;
    private List<View> parallaxBanners;
    private float parallaxMaxOffsetPx;
    private List<ArrowReveal> arrowReveals;
    private List<ScrollReveal> scrollReveals;

    private static final float VIDEO_VISIBLE_THRESHOLD = 0.12f;
    private static final long CATEGORY_FLIP_INTERVAL = 3500L;
    private static final long HOT_PRODUCTS_INTERVAL = 2800L;
    private static final float PROMO_IMAGE_RATIO = 2061f / 763f;
    private static final float MENH_BANNER_RATIO = 1774f / 887f;
    private static final float NEWS_BANNER_RATIO = 1191f / 842f;
    private static final float EVENTS_BANNER_RATIO = 1672f / 941f;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
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
        eventRepository = new com.tiredcity.app.data.repository.EventRepository(ApiClient.getApiService(null));
        articleRepository = new com.tiredcity.app.data.repository.ArticleRepository(ApiClient.getApiService(null));

        EdgeToEdgeUtils.applyStatusBarTopPadding(binding.homeTopSection);
        syncUserProfile();
        setupGreeting();
        setupMenhBanner();
        setupBanner();
        setupBanner2();
        setupPromoStrip();
        setupRecommendedProducts();
        setupHotProducts();
        loadFirestoreHighlights();
        loadLatestEventAndNews();
        setupLanguageButton();
        setupClickListeners();
        setupBrandMedia();
        setupCategoryImages();
        setupDiscoverEffects();

        killVideoFocus(binding.videoWelcome);
        killVideoFocus(binding.videoCraft);

        binding.getRoot().post(() -> {
            if (binding != null) binding.getRoot().scrollTo(0, 0);
        });
    }

    private static void killVideoFocus(View videoView) {
        videoView.setFocusable(false);
        videoView.setFocusableInTouchMode(false);
        videoView.clearFocus();
    }

    @Override
    public void onResume() {
        super.onResume();
        startAutoScroll();
        startPromoScroll();
        startCategoryFlip();
        startHotProductsScroll();
        startRecommendedSpotlight();
        startDiscoverEffects();
        updateBadges();
        if (hotProductsAdapter != null) hotProductsAdapter.notifyDataSetChanged();
        if (videoSections != null) {
            for (VideoSection section : videoSections) section.resumeForLifecycle();
        }
        if (bannerAdapter != null) bannerAdapter.resumeForLifecycle();
        if (banner2Adapter != null) banner2Adapter.resumeForLifecycle();
    }

    @Override
    public void onPause() {
        super.onPause();
        stopAutoScroll();
        stopPromoScroll();
        stopCategoryFlip();
        stopHotProductsScroll();
        stopRecommendedSpotlight();
        stopDiscoverEffects();
        if (videoSections != null) {
            for (VideoSection section : videoSections) section.pauseForLifecycle();
        }
        if (bannerAdapter != null) bannerAdapter.pauseForLifecycle();
        if (banner2Adapter != null) banner2Adapter.pauseForLifecycle();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        stopAutoScroll();
        stopPromoScroll();
        stopCategoryFlip();
        stopHotProductsScroll();
        stopRecommendedSpotlight();
        stopDiscoverEffects();
        if (videoSections != null) {
            for (VideoSection section : videoSections) section.release();
            videoSections = null;
        }
        categoryFlipCards = null;
        recommendedCards = null;
        shimmerSweeps = null;
        ctaPulseAnimators = null;
        parallaxBanners = null;
        arrowReveals = null;
        scrollReveals = null;
        binding = null;
        bannerAdapter = null;
        banner2Adapter = null;
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

    private void setupBrandMedia() {
        String packageName = requireContext().getPackageName();
        videoSections = java.util.Arrays.asList(
                new VideoSection(binding.sectionVideoWelcome, binding.videoWelcome, Uri.parse("android.resource://" + packageName + "/" + R.raw.welcome_chicken), true, true),
                new VideoSection(binding.sectionVideoCraft, binding.videoCraft, Uri.parse("android.resource://" + packageName + "/" + R.raw.main5), true));
        for (VideoSection section : videoSections) section.setup();

        NestedScrollView scrollView = (NestedScrollView) binding.getRoot();
        scrollView.setOnScrollChangeListener((NestedScrollView.OnScrollChangeListener) (v, sx, sy, osx, osy) -> onHomeScrollChanged());
        scrollView.post(this::onHomeScrollChanged);
    }

    private void onHomeScrollChanged() {
        checkVideoVisibility();
        updateBannerParallax();
        updateArrowReveals();
        updateScrollReveals();
    }

    private void updateArrowReveals() {
        if (arrowReveals == null) return;
        for (ArrowReveal reveal : arrowReveals) reveal.update();
    }

    private void updateScrollReveals() {
        if (scrollReveals == null) return;
        for (ScrollReveal reveal : scrollReveals) reveal.update();
    }

    private void checkVideoVisibility() {
        if (videoSections == null) return;
        for (VideoSection section : videoSections) section.update(isVisibleEnough(section.container));
    }

    private static void applyAspectRatio(View container, float widthToHeightRatio) {
        container.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                int width = container.getWidth();
                if (width <= 0) return;
                int desiredHeight = Math.round(width / widthToHeightRatio);
                ViewGroup.LayoutParams params = container.getLayoutParams();
                if (params.height != desiredHeight) {
                    params.height = desiredHeight;
                    container.setLayoutParams(params);
                }
            }
        });
    }

    private static boolean isVisibleEnough(View container) {
        int height = container.getHeight();
        if (height == 0) return false;
        Rect rect = new Rect();
        boolean any = container.getLocalVisibleRect(rect);
        return any && (rect.height() / (float) height) >= VIDEO_VISIBLE_THRESHOLD;
    }

    private static class VideoSection {
        private static final float GROW_INITIAL_SCALE = 0.55f;
        private static final long GROW_DURATION = 550L;
        final View container;
        private final VideoView videoView;
        private final Uri uri;
        private final boolean muted;
        private final boolean growEffect;
        private boolean visible = false;
        private boolean pausedForLifecycle = false;

        VideoSection(View container, VideoView videoView, Uri uri, boolean muted) {
            this(container, videoView, uri, muted, false);
        }

        VideoSection(View container, VideoView videoView, Uri uri, boolean muted, boolean growEffect) {
            this.container = container;
            this.videoView = videoView;
            this.uri = uri;
            this.muted = muted;
            this.growEffect = growEffect;
            if (growEffect) {
                container.setScaleX(GROW_INITIAL_SCALE);
                container.setScaleY(GROW_INITIAL_SCALE);
            }
        }

        void setup() {
            videoView.setVideoURI(uri);
            videoView.setOnPreparedListener(mp -> {
                mp.setLooping(true);
                if (muted) mp.setVolume(0f, 0f);
                else mp.setVolume(1f, 1f);
                resizeContainerToVideo(mp.getVideoWidth(), mp.getVideoHeight());
                if (visible && !pausedForLifecycle) videoView.start();
                else videoView.pause();
            });
        }

        private void resizeContainerToVideo(int videoWidth, int videoHeight) {
            if (videoWidth <= 0 || videoHeight <= 0) return;
            container.post(() -> {
                int width = container.getWidth();
                if (width <= 0) return;
                int desiredHeight = Math.round(width * videoHeight / (float) videoWidth);
                ViewGroup.LayoutParams params = container.getLayoutParams();
                if (params.height != desiredHeight) {
                    params.height = desiredHeight;
                    container.setLayoutParams(params);
                }
            });
        }

        void update(boolean nowVisible) {
            if (nowVisible == visible) return;
            visible = nowVisible;
            if (growEffect) {
                container.animate()
                        .scaleX(visible ? 1f : GROW_INITIAL_SCALE)
                        .scaleY(visible ? 1f : GROW_INITIAL_SCALE)
                        .setDuration(GROW_DURATION)
                        .setInterpolator(visible ? new OvershootInterpolator(0.8f) : new DecelerateInterpolator())
                        .start();
            }
            if (pausedForLifecycle) return;
            if (visible) videoView.start();
            else videoView.pause();
        }

        void pauseForLifecycle() {
            pausedForLifecycle = true;
            videoView.pause();
        }

        void resumeForLifecycle() {
            pausedForLifecycle = false;
            if (visible) videoView.start();
        }

        void release() { videoView.stopPlayback(); }
    }

    private void setupGreeting() {
        int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        String greeting;
        if (hour < 12)      greeting = getString(R.string.greeting_morning);
        else if (hour < 18) greeting = getString(R.string.greeting_afternoon);
        else                greeting = getString(R.string.greeting_evening);
        binding.tvGreeting.setText(greeting);
        String name = "";
        if (prefs.getUser() != null) name = prefs.getUser().getName();
        binding.tvUserName.setText(TextUtils.isEmpty(name) ? getString(R.string.greeting_guest) : name);
    }

    private void setupMenhBanner() {
        applyAspectRatio(binding.cardMenhCta, MENH_BANNER_RATIO);
        applyAspectRatio(binding.cvMenhBanner, MENH_BANNER_RATIO);

        String menh   = prefs.getMenh();
        String zodiac = prefs.getZodiac();
        UserProfile profile = prefs.getUser();

        if (profile != null && profile.getBirthDate() != null && profile.getBirthDate().length() >= 10) {
            try {
                int year = Integer.parseInt(profile.getBirthDate().substring(0, 4));
                int month = Integer.parseInt(profile.getBirthDate().substring(5, 7));
                int day = Integer.parseInt(profile.getBirthDate().substring(8, 10));

                String newMenh = MenhCalculator.tinhMenh(year);
                String newZodiac = MenhCalculator.tinhCungHoangDao(month, day);
                String animal = MenhCalculator.tinhConGiap(year);

                if (!newMenh.equals(menh) || !newZodiac.equals(zodiac)) {
                    menh = newMenh;
                    zodiac = newZodiac;
                    prefs.setMenh(menh);
                    prefs.setZodiac(zodiac);
                    profile.setMenh(menh);
                    profile.setZodiac(zodiac);
                    profile.setAnimal(animal);
                    prefs.saveUser(profile);
                    authRepository.syncUserProfileToFirestore(profile);
                }
            } catch (Exception ignored) {}
        }

        if (menh == null || menh.isEmpty()) {
            binding.layoutMenhHeaderResult.setVisibility(View.GONE);
            binding.cvMenhBanner.setVisibility(View.GONE);
            binding.layoutMenhHeaderCta.setVisibility(View.VISIBLE);
            binding.cardMenhCta.setVisibility(View.VISIBLE);
            binding.cardMenhCta.setOnClickListener(v -> startActivity(new Intent(requireContext(), AiStylingActivity.class)));
            return;
        }

        binding.layoutMenhHeaderCta.setVisibility(View.GONE);
        binding.cardMenhCta.setVisibility(View.GONE);
        binding.layoutMenhHeaderResult.setVisibility(View.VISIBLE);
        binding.cvMenhBanner.setVisibility(View.VISIBLE);
        binding.tvMenhEmoji.setText(MenhCalculator.getEmojiMenh(menh));
        binding.tvMenhElement.setText(getString(R.string.menh_label, MenhCalculator.localizeMenh(requireContext(), menh)));
        String zodiacText = (zodiac != null && !zodiac.isEmpty()) ? getString(R.string.zodiac_label, MenhCalculator.localizeZodiac(requireContext(), zodiac)) : getString(R.string.menh_tap_hint);
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
            android.widget.LinearLayout.LayoutParams lp = new android.widget.LinearLayout.LayoutParams(android.widget.LinearLayout.LayoutParams.WRAP_CONTENT, android.widget.LinearLayout.LayoutParams.WRAP_CONTENT);
            lp.setMarginEnd((int) (6 * getResources().getDisplayMetrics().density));
            chip.setLayoutParams(lp);
            container.addView(chip);
        }
    }

    private void setupBanner() {
        List<MediaBannerAdapter.MediaItem> items = new ArrayList<>();
        items.add(MediaBannerAdapter.MediaItem.image(R.drawable.main8));
        items.add(MediaBannerAdapter.MediaItem.video(R.raw.main1));
        items.add(MediaBannerAdapter.MediaItem.video(R.raw.main3));
        bannerAdapter = new MediaBannerAdapter(items);
        binding.vpBanner.setAdapter(bannerAdapter);
        binding.dotsIndicator.attachTo(binding.vpBanner);
        binding.vpBanner.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override public void onPageSelected(int position) { bannerAdapter.setActivePosition(position); }
        });
        applyAspectRatio(binding.vpBanner, 16f / 9f);
        autoScrollHandler = new Handler(Looper.getMainLooper());
        autoScrollRunnable = () -> {
            if (bannerAdapter.getItemCount() == 0) return;
            int next = (binding.vpBanner.getCurrentItem() + 1) % bannerAdapter.getItemCount();
            binding.vpBanner.setCurrentItem(next, true);
            autoScrollHandler.postDelayed(autoScrollRunnable, 8000L);
        };
    }

    private void setupBanner2() {
        List<MediaBannerAdapter.MediaItem> items = new ArrayList<>();
        items.add(MediaBannerAdapter.MediaItem.image(R.drawable.main12));
        banner2Adapter = new MediaBannerAdapter(items);
        binding.vpBanner2.setAdapter(banner2Adapter);
        binding.dotsIndicator2.setVisibility(View.GONE);
        applyAspectRatio(binding.vpBanner2, 16f / 9f);
    }

    private void startAutoScroll() {
        if (autoScrollHandler != null && autoScrollRunnable != null) autoScrollHandler.postDelayed(autoScrollRunnable, 8000L);
    }

    private void stopAutoScroll() {
        if (autoScrollHandler != null && autoScrollRunnable != null) autoScrollHandler.removeCallbacks(autoScrollRunnable);
    }

    private void setupPromoStrip() {
        List<PromoStripAdapter.PromoItem> items = new ArrayList<>();
        items.add(new PromoStripAdapter.PromoItem(R.drawable.voucher_1));
        items.add(new PromoStripAdapter.PromoItem(R.drawable.voucher_2));
        items.add(new PromoStripAdapter.PromoItem(R.drawable.voucher_3));
        promoAdapter = new PromoStripAdapter(items);
        binding.vpPromoStrip.setAdapter(promoAdapter);
        binding.dotsPromo.attachTo(binding.vpPromoStrip);
        applyAspectRatio(binding.vpPromoStrip, PROMO_IMAGE_RATIO);
        binding.vpPromoStrip.setPageTransformer(new TiltCardPageTransformer());
        promoScrollHandler = new Handler(Looper.getMainLooper());
        promoScrollRunnable = () -> {
            if (promoAdapter.getItemCount() == 0) return;
            int next = (binding.vpPromoStrip.getCurrentItem() + 1) % promoAdapter.getItemCount();
            binding.vpPromoStrip.setCurrentItem(next, true);
            promoScrollHandler.postDelayed(promoScrollRunnable, 3000L);
        };
    }

    private void startPromoScroll() {
        if (promoScrollHandler != null && promoScrollRunnable != null) promoScrollHandler.postDelayed(promoScrollRunnable, 3000L);
    }

    private void stopPromoScroll() {
        if (promoScrollHandler != null && promoScrollRunnable != null) promoScrollHandler.removeCallbacks(promoScrollRunnable);
    }

    private static final float RECOMMENDED_DIM_ALPHA = 0.42f;
    private static final float RECOMMENDED_DIM_SCALE = 0.94f;
    private static final long RECOMMENDED_SPOTLIGHT_INTERVAL = 1000L;

    private void setupRecommendedProducts() {
        List<Product> products = MockProductCatalog.getHomeHighlights(requireContext(), true);
        bindRecommendedCards(products);
        recommendedCards = java.util.Arrays.asList(binding.cardRecommended1, binding.cardRecommended2, binding.cardRecommended3, binding.cardRecommended4);
        for (int i = 0; i < recommendedCards.size(); i++) setRecommendedSpotlight(recommendedCards.get(i), i == 0);
        recommendedSpotlightIndex = 0;
        recommendedSpotlightHandler = new Handler(Looper.getMainLooper());
        recommendedSpotlightRunnable = () -> {
            setRecommendedSpotlight(recommendedCards.get(recommendedSpotlightIndex), false);
            recommendedSpotlightIndex = (recommendedSpotlightIndex + 1) % recommendedCards.size();
            setRecommendedSpotlight(recommendedCards.get(recommendedSpotlightIndex), true);
            recommendedSpotlightHandler.postDelayed(recommendedSpotlightRunnable, RECOMMENDED_SPOTLIGHT_INTERVAL);
        };
    }

    private static void setRecommendedSpotlight(View card, boolean bright) {
        card.animate().alpha(bright ? 1f : RECOMMENDED_DIM_ALPHA).scaleX(bright ? 1f : RECOMMENDED_DIM_SCALE).scaleY(bright ? 1f : RECOMMENDED_DIM_SCALE).setDuration(350L).start();
    }

    private void startRecommendedSpotlight() {
        if (recommendedSpotlightHandler != null && recommendedSpotlightRunnable != null) recommendedSpotlightHandler.postDelayed(recommendedSpotlightRunnable, RECOMMENDED_SPOTLIGHT_INTERVAL);
    }

    private void stopRecommendedSpotlight() {
        if (recommendedSpotlightHandler != null && recommendedSpotlightRunnable != null) recommendedSpotlightHandler.removeCallbacks(recommendedSpotlightRunnable);
    }

    private void bindRecommendedCards(List<Product> products) {
        bindRecommendedCard(binding.cardRecommended1, binding.ivRecommended1, binding.tvRecommended1Name, binding.tvRecommended1Price, products, 0);
        bindRecommendedCard(binding.cardRecommended2, binding.ivRecommended2, binding.tvRecommended2Name, binding.tvRecommended2Price, products, 1);
        bindRecommendedCard(binding.cardRecommended3, binding.ivRecommended3, binding.tvRecommended3Name, binding.tvRecommended3Price, products, 2);
        bindRecommendedCard(binding.cardRecommended4, binding.ivRecommended4, binding.tvRecommended4Name, binding.tvRecommended4Price, products, 3);
    }

    private void bindRecommendedCard(View card, android.widget.ImageView image, android.widget.TextView name, android.widget.TextView price, List<Product> products, int index) {
        if (products == null || index >= products.size()) { card.setVisibility(View.INVISIBLE); return; }
        card.setVisibility(View.VISIBLE);
        Product product = products.get(index);
        name.setText(product.getName());
        price.setText(PriceUtils.format(product.getEffectivePrice()));
        String imageUrl = product.getFirstImage();
        if (!imageUrl.isEmpty()) Glide.with(this).load(imageUrl).timeout(30000).centerCrop().placeholder(R.color.bg_subtle).error(R.color.bg_subtle).into(image);
        else { image.setImageDrawable(null); image.setBackgroundColor(requireContext().getColor(R.color.bg_subtle)); }
        card.setOnClickListener(v -> openProductDetail(product.getId()));
    }

    private void setupHotProducts() {
        List<Product> products = MockProductCatalog.getHomeHighlights(requireContext(), false);
        hotProductsAdapter = new HotProductCarouselAdapter(products);
        hotProductsAdapter.setOnProductClickListener(product -> openProductDetail(product.getId()));
        ViewPager2 pager = binding.vpHotProducts;
        pager.setAdapter(hotProductsAdapter);
        pager.setOffscreenPageLimit(3);
        pager.setPageTransformer(new CenterScalePageTransformer());
        int peekPx = (int) (56 * getResources().getDisplayMetrics().density);
        pager.setPadding(peekPx, 0, peekPx, 0);
        hotScrollHandler = new Handler(Looper.getMainLooper());
        hotScrollRunnable = () -> {
            if (hotProductsAdapter.getItemCount() == 0) return;
            int next = (pager.getCurrentItem() + 1) % hotProductsAdapter.getItemCount();
            pager.setCurrentItem(next, true);
            hotScrollHandler.postDelayed(hotScrollRunnable, HOT_PRODUCTS_INTERVAL);
        };
    }

    private void startHotProductsScroll() {
        if (hotScrollHandler != null && hotScrollRunnable != null) hotScrollHandler.postDelayed(hotScrollRunnable, HOT_PRODUCTS_INTERVAL);
    }

    private void stopHotProductsScroll() {
        if (hotScrollHandler != null && hotScrollRunnable != null) hotScrollHandler.removeCallbacks(hotScrollRunnable);
    }

    private static class CenterScalePageTransformer implements ViewPager2.PageTransformer {
        private static final float MIN_SCALE = 0.78f;
        private static final float MIN_ALPHA = 0.6f;
        @Override
        public void transformPage(@NonNull View page, float position) {
            float distance = Math.min(Math.abs(position), 1f);
            float scale = MIN_SCALE + (1f - MIN_SCALE) * (1f - distance);
            page.setScaleX(scale); page.setScaleY(scale);
            page.setAlpha(MIN_ALPHA + (1f - MIN_ALPHA) * (1f - distance));
        }
    }

    private void loadFirestoreHighlights() {
        if (firestoreRepository == null) firestoreRepository = new FirestoreProductRepository();
        firestoreRepository.getProducts(all -> {
            if (binding == null || all == null || all.isEmpty()) return;
            List<Product> sorted = new ArrayList<>(all);
            java.util.Collections.sort(sorted, (a, b) -> {
                if (a.getDiscount() != b.getDiscount()) return b.getDiscount() - a.getDiscount();
                return Double.compare(b.getRating(), a.getRating());
            });
            List<Product> hot = new ArrayList<>();
            List<Product> recommended = new ArrayList<>();
            for (int i = 0; i < sorted.size(); i++) { if (i % 2 == 0) hot.add(sorted.get(i)); else recommended.add(sorted.get(i)); }
            bindRecommendedCards(recommended);
            if (hotProductsAdapter != null) hotProductsAdapter.updateData(hot);
        });
    }

    private void setupLanguageButton() {
        String lang = prefs.getLanguage();
        binding.btnLanguage.setText("vi".equals(lang) ? "EN" : "VI");
        binding.btnLanguage.setOnClickListener(v -> {
            LocaleHelper.toggleLanguage(requireContext());
            requireActivity().recreate();
        });
    }

    private void setupClickListeners() {
        binding.cvMenhBanner.setOnClickListener(v -> startActivity(new Intent(requireContext(), AiStylingActivity.class)));
        binding.cvAiStrip.setOnClickListener(v -> startActivity(new Intent(requireContext(), ChatBotActivity.class)));
        binding.btnCart.setOnClickListener(v -> startActivity(new Intent(requireContext(), com.tiredcity.app.ui.cart.CartActivity.class)));
        binding.btnNotification.setOnClickListener(v -> startActivity(new Intent(requireContext(), com.tiredcity.app.ui.notification.NotificationActivity.class)));
        binding.tvPromoSeeAll.setOnClickListener(v -> startActivity(new Intent(requireContext(), com.tiredcity.app.ui.reward.RewardActivity.class)));
        binding.tvCategoriesSeeAll.setOnClickListener(v -> Navigation.findNavController(requireView()).navigate(R.id.stylingFragment));
        binding.tvHotSeeAll.setOnClickListener(v -> Navigation.findNavController(requireView()).navigate(R.id.stylingFragment));
        binding.tvRecommendedSeeAll.setOnClickListener(v -> Navigation.findNavController(requireView()).navigate(R.id.stylingFragment));
        binding.cardCatAoDai.setOnClickListener(v -> openStylingCategory("ÁO DÀI"));
        binding.cardCatNhatBinh.setOnClickListener(v -> openStylingCategory("NHẬT BÌNH"));
        binding.cardCatAoTac.setOnClickListener(v -> openStylingCategory("ÁO TẤC"));
        binding.cardCatGiaoLinh.setOnClickListener(v -> openStylingCategory("GIAO LĨNH"));
        binding.cardCatYemDao.setOnClickListener(v -> openStylingCategory("YẾM ĐÀO"));
        binding.cardCatAccessories.setOnClickListener(v -> openStylingCategory("PHỤ KIỆN"));
        binding.cardEvents.setOnClickListener(v -> {
            Intent intent = new Intent(requireContext(), com.tiredcity.app.ui.explore.EventDetailActivity.class);
            if (latestEventId != null) intent.putExtra(com.tiredcity.app.utils.Constants.EXTRA_EVENT_ID, latestEventId);
            startActivity(intent);
        });
        binding.tvEventsSeeAll.setOnClickListener(v -> startActivity(new Intent(requireContext(), com.tiredcity.app.ui.explore.EventActivity.class)));
        applyAspectRatio(binding.cardEvents, EVENTS_BANNER_RATIO);

        binding.cardNews.setOnClickListener(v -> {
            Intent intent = new Intent(requireContext(), com.tiredcity.app.ui.explore.NewsDetailActivity.class);
            if (latestNewsId != null) intent.putExtra(com.tiredcity.app.utils.Constants.EXTRA_ARTICLE_ID, latestNewsId);
            startActivity(intent);
        });
        binding.tvNewsSeeAll.setOnClickListener(v -> startActivity(new Intent(requireContext(), com.tiredcity.app.ui.explore.ArticleActivity.class)));
        applyAspectRatio(binding.cardNews, NEWS_BANNER_RATIO);
    }

    private void loadLatestEventAndNews() {
        eventRepository.getEventsFromFirestore(new com.tiredcity.app.data.repository.EventRepository.OnEventsLoadedListener() {
            @Override
            public void onSuccess(List<com.tiredcity.app.data.model.Event> events) {
                if (binding == null || events == null || events.isEmpty()) return;
                com.tiredcity.app.data.model.Event latest = events.get(0);
                latestEventId = latest.getId();
                // Giữ ảnh (main15) và tiêu đề tĩnh trong layout/strings; chỉ lấy id để mở chi tiết
            }
            @Override public void onError(String message) {}
        });

        articleRepository.getArticlesFromFirestore(new com.tiredcity.app.data.repository.ArticleRepository.OnArticlesLoadedListener() {
            @Override
            public void onSuccess(List<com.tiredcity.app.data.model.Article> articles) {
                if (binding == null || articles == null || articles.isEmpty()) return;
                com.tiredcity.app.data.model.Article latest = articles.get(0);
                latestNewsId = latest.getId();
                // Giữ ảnh (main13) và tiêu đề tĩnh trong layout/strings; chỉ lấy id để mở chi tiết
            }
            @Override public void onError(String message) {}
        });
    }

    private void setupCategoryImages() {
        loadCategoryImage(binding.imgCatAoDaiFront, R.drawable.cate_ao_dai);
        loadCategoryImage(binding.imgCatNhatBinhFront, R.drawable.cate_nhat_binh);
        loadCategoryImage(binding.imgCatAoTacFront, R.drawable.cate_ao_tac);
        loadCategoryImage(binding.imgCatGiaoLinhFront, R.drawable.cate_giao_linh);
        loadCategoryImage(binding.imgCatYemDaoFront, R.drawable.cate_yem_dao);
        loadCategoryImage(binding.imgCatAccessoriesFront, R.drawable.cate_phu_kien);
        loadCategoryImage(binding.imgCatAoDaiBack, R.drawable.costume_ao_dai);
        loadCategoryImage(binding.imgCatNhatBinhBack, R.drawable.costume_nhat_binh);
        loadCategoryImage(binding.imgCatAoTacBack, R.drawable.costume_ao_tac);
        loadCategoryImage(binding.imgCatGiaoLinhBack, R.drawable.costume_giao_linh);
        loadCategoryImage(binding.imgCatYemDaoBack, R.drawable.costume_yem_dao);
        loadCategoryImage(binding.imgCatAccessoriesBack, R.drawable.costume_phu_kien);

        categoryFlipCards = java.util.Arrays.asList(
                new CategoryFlipCard(binding.imgCatAoDaiFront, binding.layoutCatAoDaiBack),
                new CategoryFlipCard(binding.imgCatNhatBinhFront, binding.layoutCatNhatBinhBack),
                new CategoryFlipCard(binding.imgCatAoTacFront, binding.layoutCatAoTacBack),
                new CategoryFlipCard(binding.imgCatGiaoLinhFront, binding.layoutCatGiaoLinhBack),
                new CategoryFlipCard(binding.imgCatYemDaoFront, binding.layoutCatYemDaoBack),
                new CategoryFlipCard(binding.imgCatAccessoriesFront, binding.layoutCatAccessoriesBack));

        categoryFlipHandler = new Handler(Looper.getMainLooper());
        categoryFlipRunnable = () -> {
            for (CategoryFlipCard card : categoryFlipCards) card.flip();
            categoryFlipHandler.postDelayed(categoryFlipRunnable, CATEGORY_FLIP_INTERVAL);
        };
    }

    private void loadCategoryImage(android.widget.ImageView target, int drawableRes) {
        Glide.with(this).load(drawableRes).diskCacheStrategy(com.bumptech.glide.load.engine.DiskCacheStrategy.NONE).override(400, 400).centerCrop().into(target);
    }

    private void startCategoryFlip() {
        if (categoryFlipHandler != null && categoryFlipRunnable != null) categoryFlipHandler.postDelayed(categoryFlipRunnable, CATEGORY_FLIP_INTERVAL);
    }

    private void stopCategoryFlip() {
        if (categoryFlipHandler != null && categoryFlipRunnable != null) categoryFlipHandler.removeCallbacks(categoryFlipRunnable);
    }

    private static class CategoryFlipCard {
        private static final long FLIP_HALF_DURATION = 380L;
        private final View front;
        private final View back;
        private boolean showingFront = true;
        CategoryFlipCard(View front, View back) {
            this.front = front; this.back = back;
            float density = front.getResources().getDisplayMetrics().density;
            front.setCameraDistance(8000 * density); back.setCameraDistance(8000 * density);
        }
        void flip() {
            View outView = showingFront ? front : back;
            View inView = showingFront ? back : front;
            showingFront = !showingFront;
            ObjectAnimator out = ObjectAnimator.ofFloat(outView, View.ROTATION_Y, 0f, 90f);
            out.setDuration(FLIP_HALF_DURATION); out.setInterpolator(new AccelerateInterpolator());
            out.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    outView.setVisibility(View.GONE);
                    inView.setRotationY(-90f); inView.setVisibility(View.VISIBLE);
                    ObjectAnimator in = ObjectAnimator.ofFloat(inView, View.ROTATION_Y, -90f, 0f);
                    in.setDuration(FLIP_HALF_DURATION); in.setInterpolator(new DecelerateInterpolator());
                    in.start();
                }
            });
            out.start();
        }
    }

    private static final long SHIMMER_SWEEP_DURATION = 1100L;
    private static final long SHIMMER_SWEEP_INTERVAL = 3400L;
    private static final long CTA_PULSE_DURATION = 1500L;
    private static final float PARALLAX_MAX_OFFSET_DP = 16f;
    private static final long SCROLL_REVEAL_DURATION = 520L;
    private static final long SCROLL_REVEAL_BANNER_DELAY = 140L;

    private void setupDiscoverEffects() {
        shimmerSweeps = java.util.Arrays.asList(
                new ShimmerSweep(binding.shimmerAiStrip, binding.cvAiStrip),
                new ShimmerSweep(binding.shimmerEvents, binding.cardEvents),
                new ShimmerSweep(binding.shimmerNews, binding.cardNews),
                new ShimmerSweep(binding.shimmerMenhCta, binding.cardMenhCta),
                new ShimmerSweep(binding.shimmerMenhResult, binding.cvMenhBanner));
        ctaPulseAnimators = java.util.Arrays.asList(createCtaPulse(binding.btnAiStripCta), createCtaPulse(binding.btnMenhCta));
        arrowReveals = java.util.Arrays.asList(
                new ArrowReveal(binding.cvAiStrip, binding.arrowAiStripCta),
                new ArrowReveal(binding.cardMenhCta, binding.arrowMenhCta),
                new ArrowReveal(binding.cvMenhBanner, binding.ivMenhArrow));
        parallaxBanners = java.util.Arrays.asList(binding.cvAiStrip, binding.cardEvents, binding.cardNews, binding.vpPromoStrip, binding.cardMenhCta, binding.cvMenhBanner);
        parallaxMaxOffsetPx = PARALLAX_MAX_OFFSET_DP * getResources().getDisplayMetrics().density;
        scrollReveals = java.util.Arrays.asList(
                new ScrollReveal(binding.cvAiStrip, binding.layoutAiStripHeader, 0L),
                new ScrollReveal(binding.cvAiStrip, binding.cvAiStrip, SCROLL_REVEAL_BANNER_DELAY),
                new ScrollReveal(binding.cardEvents, binding.layoutEventsHeader, 0L),
                new ScrollReveal(binding.cardEvents, binding.cardEvents, SCROLL_REVEAL_BANNER_DELAY),
                new ScrollReveal(binding.cardNews, binding.layoutNewsHeader, 0L),
                new ScrollReveal(binding.cardNews, binding.cardNews, SCROLL_REVEAL_BANNER_DELAY),
                new ScrollReveal(binding.vpPromoStrip, binding.layoutPromoHeader, 0L),
                new ScrollReveal(binding.vpPromoStrip, binding.vpPromoStrip, SCROLL_REVEAL_BANNER_DELAY),
                new ScrollReveal(binding.cardMenhCta, binding.layoutMenhHeaderCta, 0L),
                new ScrollReveal(binding.cardMenhCta, binding.cardMenhCta, SCROLL_REVEAL_BANNER_DELAY),
                new ScrollReveal(binding.cvMenhBanner, binding.layoutMenhHeaderResult, 0L),
                new ScrollReveal(binding.cvMenhBanner, binding.cvMenhBanner, SCROLL_REVEAL_BANNER_DELAY));
    }

    private void updateBannerParallax() {
        if (parallaxBanners == null || binding == null) return;
        NestedScrollView scrollView = (NestedScrollView) binding.getRoot();
        int viewportHeight = scrollView.getHeight();
        if (viewportHeight <= 0) return;
        int scrollY = scrollView.getScrollY();
        for (View banner : parallaxBanners) {
            if (banner.getHeight() == 0) continue;
            float top = getRelativeTop(banner, scrollView);
            float centerInViewport = top + banner.getHeight() / 2f - scrollY;
            float normalized = (centerInViewport - viewportHeight / 2f) / (viewportHeight / 2f);
            normalized = Math.max(-1f, Math.min(1f, normalized));
            banner.setTranslationY(-normalized * parallaxMaxOffsetPx);
        }
    }

    private static float getRelativeTop(View descendant, View ancestor) {
        float top = 0f;
        View v = descendant;
        while (v != null && v != ancestor) {
            top += v.getTop();
            Object parent = v.getParent();
            if (!(parent instanceof View)) break;
            v = (View) parent;
        }
        return top;
    }

    private static ObjectAnimator createCtaPulse(View target) {
        ObjectAnimator pulse = ObjectAnimator.ofPropertyValuesHolder(target, android.animation.PropertyValuesHolder.ofFloat(View.SCALE_X, 1f, 1.08f, 1f), android.animation.PropertyValuesHolder.ofFloat(View.SCALE_Y, 1f, 1.08f, 1f));
        pulse.setDuration(CTA_PULSE_DURATION); pulse.setRepeatCount(ObjectAnimator.INFINITE);
        return pulse;
    }

    private void startDiscoverEffects() {
        if (shimmerSweeps != null) {
            long stagger = 0L;
            for (ShimmerSweep sweep : shimmerSweeps) { sweep.start(stagger); stagger += 600L; }
        }
        if (ctaPulseAnimators != null) {
            for (ObjectAnimator anim : ctaPulseAnimators) { if (anim.isStarted()) anim.resume(); else anim.start(); }
        }
    }

    private void stopDiscoverEffects() {
        if (shimmerSweeps != null) for (ShimmerSweep sweep : shimmerSweeps) sweep.stop();
        if (ctaPulseAnimators != null) for (ObjectAnimator anim : ctaPulseAnimators) anim.pause();
    }

    private static class ShimmerSweep {
        private final View shimmer; private final View container;
        private Handler handler; private Runnable runnable;
        ShimmerSweep(View shimmer, View container) { this.shimmer = shimmer; this.container = container; }
        void start(long initialDelay) {
            handler = new Handler(Looper.getMainLooper());
            runnable = () -> { sweepOnce(); handler.postDelayed(runnable, SHIMMER_SWEEP_INTERVAL); };
            handler.postDelayed(runnable, initialDelay);
        }
        private void sweepOnce() {
            int width = container.getWidth();
            if (width <= 0) return;
            float startX = -width * 0.6f; float endX = width * 1.3f;
            shimmer.setTranslationX(startX);
            shimmer.animate().translationX(endX).setDuration(SHIMMER_SWEEP_DURATION).setInterpolator(new android.view.animation.LinearInterpolator()).start();
        }
        void stop() { if (handler != null && runnable != null) handler.removeCallbacks(runnable); shimmer.animate().cancel(); }
    }

    private static class ArrowReveal {
        private final View container; private final View arrow;
        private boolean revealed = false;
        ArrowReveal(View container, View arrow) { this.container = container; this.arrow = arrow; }
        void update() {
            if (revealed || container.getVisibility() != View.VISIBLE) return;
            if (!isVisibleEnough(container)) return;
            revealed = true;
            arrow.animate().alpha(1f).scaleX(1f).scaleY(1f).setStartDelay(150L).setDuration(380L).setInterpolator(new OvershootInterpolator(1.6f)).start();
        }
    }

    private static class ScrollReveal {
        private final View trigger; private final View target; private final long delay;
        private boolean revealed = false;
        ScrollReveal(View trigger, View target, long delay) { this.trigger = trigger; this.target = target; this.delay = delay; }
        void update() {
            if (revealed || trigger.getVisibility() != View.VISIBLE) return;
            if (!isVisibleEnough(trigger)) return;
            revealed = true;
            android.view.ViewPropertyAnimator anim = target.animate().alpha(1f).setStartDelay(delay).setDuration(SCROLL_REVEAL_DURATION).setInterpolator(new DecelerateInterpolator(1.4f));
            if (target == trigger) anim.scaleX(1f).scaleY(1f);
            else anim.translationY(0f);
            anim.start();
        }
    }

    private static class TiltCardPageTransformer implements ViewPager2.PageTransformer {
        private static final float MAX_ROTATION_Y = 16f;
        @Override
        public void transformPage(@NonNull View page, float position) {
            page.setPivotX(position < 0 ? 0f : page.getWidth()); page.setPivotY(page.getHeight() * 0.5f);
            page.setRotationY(MAX_ROTATION_Y * -position);
            page.setAlpha(1f - Math.min(Math.abs(position), 1f) * 0.35f);
        }
    }

    private void openStylingCategory(String categoryId) {
        Bundle args = new Bundle();
        args.putString(StylingFragment.ARG_CATEGORY_ID, categoryId);
        Navigation.findNavController(requireView()).navigate(R.id.stylingFragment, args);
    }

    private void updateBadges() {
        if (binding == null) return;
        int cartCount = new com.tiredcity.app.data.local.CartLocalStore(requireContext()).getCartCount();
        bindBadge(binding.tvCartBadge, cartCount);
        int notifCount = new com.tiredcity.app.data.local.NotificationStore(requireContext()).getUnreadCount();
        bindBadge(binding.tvNotifBadge, notifCount);
    }

    private void bindBadge(android.widget.TextView badge, int count) {
        if (count <= 0) badge.setVisibility(View.GONE);
        else { badge.setVisibility(View.VISIBLE); badge.setText(count > 9 ? "9+" : String.valueOf(count)); }
    }

    private void openProductDetail(String productId) {
        Intent intent = new Intent(requireContext(), com.tiredcity.app.ui.shop.ProductDetailActivity.class);
        intent.putExtra(com.tiredcity.app.utils.Constants.EXTRA_PRODUCT_ID, productId);
        startActivity(intent);
    }
}
