package com.tiredcity.app.ui.main;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
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
import androidx.viewpager2.widget.ViewPager2;
import com.bumptech.glide.Glide;
import com.tiredcity.app.R;
import com.tiredcity.app.adapter.HotProductCarouselAdapter;
import com.tiredcity.app.adapter.MediaBannerAdapter;
import com.tiredcity.app.adapter.PromoStripAdapter;
import com.tiredcity.app.data.mock.MockProductCatalog;
import com.tiredcity.app.data.model.Product;
import com.tiredcity.app.data.repository.FirestoreProductRepository;
import com.tiredcity.app.data.model.UserProfile;
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
    private Handler             autoScrollHandler;
    private Runnable            autoScrollRunnable;
    private MediaBannerAdapter  bannerAdapter;
    private Handler             banner2ScrollHandler;
    private Runnable            banner2ScrollRunnable;
    private MediaBannerAdapter  banner2Adapter;
    private Handler             promoScrollHandler;
    private Runnable            promoScrollRunnable;
    private PromoStripAdapter   promoAdapter;
    private List<View>          recommendedCards;
    private Handler             recommendedSpotlightHandler;
    private Runnable            recommendedSpotlightRunnable;
    private int                 recommendedSpotlightIndex = 0;
    private HotProductCarouselAdapter hotProductsAdapter;
    private Handler             hotScrollHandler;
    private Runnable            hotScrollRunnable;
    private FirestoreProductRepository firestoreRepository;
    private List<VideoSection>  videoSections;
    private List<CategoryFlipCard> categoryFlipCards;
    private Handler             categoryFlipHandler;
    private Runnable            categoryFlipRunnable;

    // Video chỉ phát khi khung của nó lộ ra ít nhất chừng này (giống AboutFragment)
    private static final float VIDEO_VISIBLE_THRESHOLD = 0.12f;
    // Khoảng nghỉ giữa 2 lần lật thẻ danh mục
    private static final long CATEGORY_FLIP_INTERVAL = 3500L;
    // Khoảng nghỉ giữa 2 lần tự động chuyển trang carousel "Sản phẩm nổi bật"
    private static final long HOT_PRODUCTS_INTERVAL = 2800L;

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
        setupBanner2();
        setupPromoStrip();
        setupRecommendedProducts();
        setupHotProducts();
        loadFirestoreHighlights();
        setupLanguageButton();
        setupClickListeners();
        setupBrandMedia();
        setupCategoryImages();

        // VideoView tự ép focusable=true và requestFocus() trong constructor (ghi đè XML),
        // khiến NestedScrollView cuộn tới video khi mở trang — tắt hẳn focus tại đây.
        killVideoFocus(binding.videoWelcome);
        killVideoFocus(binding.videoCraft);

        // Luôn mở trang từ đầu: đặt lại scroll SAU lượt layout đầu tiên, vì NestedScrollView
        // chỉ áp cuộn (do focus/khôi phục trạng thái) trong onLayout — đặt sớm hơn sẽ bị đè.
        binding.getRoot().post(() -> {
            if (binding != null) binding.getRoot().scrollTo(0, 0);
        });
    }

    /** Tắt triệt để focus của VideoView (constructor của nó luôn bật lại bất chấp XML). */
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
        updateBadges();
        // Trạng thái yêu thích có thể đã đổi ở màn hình khác (chi tiết sản phẩm, tủ đồ...)
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
        if (videoSections != null) {
            for (VideoSection section : videoSections) section.release();
            videoSections = null;
        }
        categoryFlipCards = null;
        recommendedCards = null;
        binding = null;
        bannerAdapter = null;
        banner2Adapter = null;
        hotProductsAdapter = null;
    }

    // ── Media thương hiệu (video Behance + gif) ───────────────────────────────

    /** Gắn 2 video khối màu (welcome, xưởng sáng tạo main5) và các gif Behance. */
    private void setupBrandMedia() {
        String packageName = requireContext().getPackageName();
        // Khung mỗi video tự co theo tỷ lệ THẬT của video (đọc từ MediaPlayer khi prepared)
        // nên không còn dải đen thừa hay bị cắt hình.
        // Video "welcome" có growEffect=true: hiện scale nhỏ rồi phóng to lấp đầy khi cuộn tới.
        videoSections = java.util.Arrays.asList(
                new VideoSection(binding.sectionVideoWelcome, binding.videoWelcome,
                        Uri.parse("android.resource://" + packageName + "/" + R.raw.welcome_chicken),
                        true, true),
                new VideoSection(binding.sectionVideoCraft, binding.videoCraft,
                        Uri.parse("android.resource://" + packageName + "/" + R.raw.main5), true));
        for (VideoSection section : videoSections) {
            section.setup();
        }

        // Dải gif khối màu footer (main12) — giữ nguyên, 16:9 nên ép khung đúng tỷ lệ.
        applyAspectRatio(binding.ivGifFooter, 16f / 9f);
        Glide.with(this).load(R.drawable.main12)
                .override(1024, 576).centerCrop().into(binding.ivGifFooter);

        NestedScrollView scrollView = binding.getRoot();
        scrollView.setOnScrollChangeListener(
                (NestedScrollView.OnScrollChangeListener) (v, sx, sy, osx, osy) -> checkVideoVisibility());
        scrollView.post(this::checkVideoVisibility);
    }

    private void checkVideoVisibility() {
        if (videoSections == null) return;
        for (VideoSection section : videoSections) {
            section.update(isVisibleEnough(section.container));
        }
    }

    /**
     * Ép container về đúng tỷ lệ rộng/cao theo bề rộng đo được thực tế — VideoView bỏ qua
     * chiều cao cố định và tự đo lại theo tỷ lệ video (gây viền đen), giống AboutFragment.
     */
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

    /** Video local: lặp vô hạn, chỉ phát khi khối của nó đang cuộn vào tầm nhìn;
     *  khung chứa tự co về đúng tỷ lệ video để không lộ nền đen thừa. */
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

        /** growEffect=true: khung hiện ở scale nhỏ, phóng to (kèm nảy nhẹ) khi cuộn vào tầm nhìn,
         *  thu nhỏ lại khi cuộn ra — thuần render transform nên không làm layout xung quanh giật. */
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
            // Không giữ tham chiếu MediaPlayer — VideoView tự release khi mất Surface;
            // onPrepared có thể chạy lại mỗi lần Surface tạo lại nên áp lại loop/mute ở đây.
            videoView.setOnPreparedListener(mp -> {
                mp.setLooping(true);
                if (muted) {
                    mp.setVolume(0f, 0f);
                } else {
                    mp.setVolume(1f, 1f);
                }
                resizeContainerToVideo(mp.getVideoWidth(), mp.getVideoHeight());
                if (visible && !pausedForLifecycle) {
                    videoView.start();
                } else {
                    videoView.pause();
                }
            });
        }

        /** Co chiều cao khung về đúng tỷ lệ khung hình thật của video (theo bề rộng hiện có). */
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
                        .setInterpolator(visible ? new OvershootInterpolator(0.8f)
                                : new DecelerateInterpolator())
                        .start();
            }
            if (pausedForLifecycle) return;
            if (visible) {
                videoView.start();
            } else {
                videoView.pause();
            }
        }

        void pauseForLifecycle() {
            pausedForLifecycle = true;
            videoView.pause();
        }

        void resumeForLifecycle() {
            pausedForLifecycle = false;
            if (visible) {
                videoView.start();
            }
        }

        void release() {
            videoView.stopPlayback();
        }
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
        String menh   = prefs.getMenh();
        String zodiac = prefs.getZodiac();

        // Tự tính mệnh từ ngày sinh trong profile nếu chưa có
        if (menh == null || menh.isEmpty()) {
            UserProfile profile = prefs.getUser();
            if (profile != null && profile.getBirthDate() != null
                    && profile.getBirthDate().length() >= 10) {
                try {
                    int year  = Integer.parseInt(profile.getBirthDate().substring(0, 4));
                    int month = Integer.parseInt(profile.getBirthDate().substring(5, 7));
                    int day   = Integer.parseInt(profile.getBirthDate().substring(8, 10));

                    menh   = MenhCalculator.tinhMenh(year);
                    zodiac = MenhCalculator.tinhCungHoangDao(month, day);
                    String animal = MenhCalculator.tinhConGiap(year);

                    prefs.setMenh(menh);
                    prefs.setZodiac(zodiac);

                    UserProfile updated = prefs.getUser();
                    if (updated != null) {
                        updated.setMenh(menh);
                        updated.setZodiac(zodiac);
                        updated.setAnimal(animal);
                        prefs.saveUser(updated);
                    }
                } catch (NumberFormatException ignored) { }
            }
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
        // Carousel mở màn: gif main8 → video main1 → video main2 (đều 16:9, full-bleed).
        // Video tắt tiếng, lặp, chỉ phát ở trang đang hiển thị.
        List<MediaBannerAdapter.MediaItem> items = new ArrayList<>();
        items.add(MediaBannerAdapter.MediaItem.image(R.drawable.main8));
        items.add(MediaBannerAdapter.MediaItem.video(R.raw.main1));
        items.add(MediaBannerAdapter.MediaItem.video(R.raw.main2));

        bannerAdapter = new MediaBannerAdapter(items);
        binding.vpBanner.setAdapter(bannerAdapter);
        binding.dotsIndicator.attachTo(binding.vpBanner);
        binding.vpBanner.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                bannerAdapter.setActivePosition(position);
            }
        });
        applyAspectRatio(binding.vpBanner, 16f / 9f);

        // 8s mỗi trang — đủ để video kịp "khoe" trước khi chuyển
        autoScrollHandler  = new Handler(Looper.getMainLooper());
        autoScrollRunnable = () -> {
            if (bannerAdapter.getItemCount() == 0) return;
            int next = (binding.vpBanner.getCurrentItem() + 1) % bannerAdapter.getItemCount();
            binding.vpBanner.setCurrentItem(next, true);
            autoScrollHandler.postDelayed(autoScrollRunnable, 8000L);
        };
    }

    private void setupBanner2() {
        // Carousel thứ hai (dưới "Đằng sau mỗi thiết kế"): main11 → main12 (gif) → main15
        List<MediaBannerAdapter.MediaItem> items = new ArrayList<>();
        items.add(MediaBannerAdapter.MediaItem.image(R.drawable.main11));
        items.add(MediaBannerAdapter.MediaItem.image(R.drawable.main12));
        items.add(MediaBannerAdapter.MediaItem.image(R.drawable.main15));

        banner2Adapter = new MediaBannerAdapter(items);
        binding.vpBanner2.setAdapter(banner2Adapter);
        binding.dotsIndicator2.attachTo(binding.vpBanner2);
        applyAspectRatio(binding.vpBanner2, 16f / 9f);

        banner2ScrollHandler  = new Handler(Looper.getMainLooper());
        banner2ScrollRunnable = () -> {
            if (banner2Adapter.getItemCount() == 0) return;
            int next = (binding.vpBanner2.getCurrentItem() + 1) % banner2Adapter.getItemCount();
            binding.vpBanner2.setCurrentItem(next, true);
            banner2ScrollHandler.postDelayed(banner2ScrollRunnable, 4000L);
        };
    }

    private void startAutoScroll() {
        if (autoScrollHandler != null && autoScrollRunnable != null) {
            autoScrollHandler.postDelayed(autoScrollRunnable, 8000L);
        }
        if (banner2ScrollHandler != null && banner2ScrollRunnable != null) {
            banner2ScrollHandler.postDelayed(banner2ScrollRunnable, 4000L);
        }
    }

    private void stopAutoScroll() {
        if (autoScrollHandler != null && autoScrollRunnable != null) {
            autoScrollHandler.removeCallbacks(autoScrollRunnable);
        }
        if (banner2ScrollHandler != null && banner2ScrollRunnable != null) {
            banner2ScrollHandler.removeCallbacks(banner2ScrollRunnable);
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

    // ── Recommended products (4 thẻ xen kẽ, spotlight xoay vòng) ──────────────

    private static final float RECOMMENDED_DIM_ALPHA = 0.42f;
    private static final float RECOMMENDED_DIM_SCALE = 0.94f;
    private static final long RECOMMENDED_SPOTLIGHT_INTERVAL = 1000L;

    /** Nạp 4 sản phẩm đầu vào 4 thẻ xen kẽ, rồi bật hiệu ứng "đèn sân khấu": mỗi giây một thẻ
     *  sáng rõ lên, 3 thẻ còn lại mờ đi — xoay vòng liên tục qua cả 4 thẻ. */
    private void setupRecommendedProducts() {
        List<Product> products = MockProductCatalog.getHomeHighlights(requireContext(), true);
        bindRecommendedCards(products);

        recommendedCards = java.util.Arrays.asList(
                binding.cardRecommended1, binding.cardRecommended2,
                binding.cardRecommended3, binding.cardRecommended4);
        for (int i = 0; i < recommendedCards.size(); i++) {
            setRecommendedSpotlight(recommendedCards.get(i), i == 0);
        }
        recommendedSpotlightIndex = 0;

        recommendedSpotlightHandler  = new Handler(Looper.getMainLooper());
        recommendedSpotlightRunnable = () -> {
            setRecommendedSpotlight(recommendedCards.get(recommendedSpotlightIndex), false);
            recommendedSpotlightIndex = (recommendedSpotlightIndex + 1) % recommendedCards.size();
            setRecommendedSpotlight(recommendedCards.get(recommendedSpotlightIndex), true);
            recommendedSpotlightHandler.postDelayed(recommendedSpotlightRunnable,
                    RECOMMENDED_SPOTLIGHT_INTERVAL);
        };
    }

    private static void setRecommendedSpotlight(View card, boolean bright) {
        card.animate()
                .alpha(bright ? 1f : RECOMMENDED_DIM_ALPHA)
                .scaleX(bright ? 1f : RECOMMENDED_DIM_SCALE)
                .scaleY(bright ? 1f : RECOMMENDED_DIM_SCALE)
                .setDuration(350L)
                .start();
    }

    private void startRecommendedSpotlight() {
        if (recommendedSpotlightHandler != null && recommendedSpotlightRunnable != null) {
            recommendedSpotlightHandler.postDelayed(recommendedSpotlightRunnable,
                    RECOMMENDED_SPOTLIGHT_INTERVAL);
        }
    }

    private void stopRecommendedSpotlight() {
        if (recommendedSpotlightHandler != null && recommendedSpotlightRunnable != null) {
            recommendedSpotlightHandler.removeCallbacks(recommendedSpotlightRunnable);
        }
    }

    private void bindRecommendedCards(List<Product> products) {
        bindRecommendedCard(binding.cardRecommended1, binding.ivRecommended1,
                binding.tvRecommended1Name, binding.tvRecommended1Price, products, 0);
        bindRecommendedCard(binding.cardRecommended2, binding.ivRecommended2,
                binding.tvRecommended2Name, binding.tvRecommended2Price, products, 1);
        bindRecommendedCard(binding.cardRecommended3, binding.ivRecommended3,
                binding.tvRecommended3Name, binding.tvRecommended3Price, products, 2);
        bindRecommendedCard(binding.cardRecommended4, binding.ivRecommended4,
                binding.tvRecommended4Name, binding.tvRecommended4Price, products, 3);
    }

    private void bindRecommendedCard(View card,
                                      com.google.android.material.imageview.ShapeableImageView image,
                                      android.widget.TextView name, android.widget.TextView price,
                                      List<Product> products, int index) {
        if (products == null || index >= products.size()) {
            card.setVisibility(View.INVISIBLE);
            return;
        }
        card.setVisibility(View.VISIBLE);
        Product product = products.get(index);
        name.setText(product.getName());
        price.setText(com.tiredcity.app.utils.PriceUtils.formatVnd(product.getEffectivePrice()));

        String imageUrl = product.getFirstImage();
        if (!imageUrl.isEmpty()) {
            Glide.with(this).load(imageUrl)
                    .timeout(30000)
                    .centerCrop()
                    .placeholder(R.color.bg_subtle)
                    .error(R.color.bg_subtle)
                    .into(image);
        } else {
            image.setImageDrawable(null);
            image.setBackgroundColor(requireContext().getColor(R.color.bg_subtle));
        }

        card.setOnClickListener(v -> openProductDetail(product.getId()));
    }

    // ── Hot products ──────────────────────────────────────────────────────────

    /** Carousel 3 thẻ (ViewPager2 + padding ngang để 2 thẻ bên lộ ra); thẻ đang ở giữa được
     *  CenterScalePageTransformer phóng to, tự động chuyển sang thẻ kế tiếp mỗi
     *  {@link #HOT_PRODUCTS_INTERVAL}ms — thẻ đang to co lại đúng lúc thẻ mới phóng to lên. */
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

        hotScrollHandler  = new Handler(Looper.getMainLooper());
        hotScrollRunnable = () -> {
            if (hotProductsAdapter.getItemCount() == 0) return;
            int next = (pager.getCurrentItem() + 1) % hotProductsAdapter.getItemCount();
            pager.setCurrentItem(next, true);
            hotScrollHandler.postDelayed(hotScrollRunnable, HOT_PRODUCTS_INTERVAL);
        };
    }

    private void startHotProductsScroll() {
        if (hotScrollHandler != null && hotScrollRunnable != null) {
            hotScrollHandler.postDelayed(hotScrollRunnable, HOT_PRODUCTS_INTERVAL);
        }
    }

    private void stopHotProductsScroll() {
        if (hotScrollHandler != null && hotScrollRunnable != null) {
            hotScrollHandler.removeCallbacks(hotScrollRunnable);
        }
    }

    /** Thu nhỏ + làm mờ nhẹ các trang lệch khỏi tâm, giữ trang đang ở giữa ở kích thước đầy đủ —
     *  tạo hiệu ứng "phồng lên ở giữa" cho carousel Sản phẩm nổi bật. */
    private static class CenterScalePageTransformer implements ViewPager2.PageTransformer {
        private static final float MIN_SCALE = 0.78f;
        private static final float MIN_ALPHA = 0.6f;

        @Override
        public void transformPage(@NonNull View page, float position) {
            float distance = Math.min(Math.abs(position), 1f);
            float scale = MIN_SCALE + (1f - MIN_SCALE) * (1f - distance);
            page.setScaleX(scale);
            page.setScaleY(scale);
            page.setAlpha(MIN_ALPHA + (1f - MIN_ALPHA) * (1f - distance));
        }
    }

    /**
     * Thay dữ liệu mẫu (không ảnh) bằng sản phẩm thật từ Firestore — cùng nguồn với admin,
     * có ảnh. "Gợi ý cho bạn" = sản phẩm đánh giá cao; "Đang thịnh hành" = đang giảm giá.
     * Nếu Firestore lỗi/rỗng thì giữ nguyên dữ liệu mẫu đã hiển thị.
     */
    private void loadFirestoreHighlights() {
        if (firestoreRepository == null) firestoreRepository = new FirestoreProductRepository();
        firestoreRepository.getProducts(all -> {
            if (binding == null || all == null || all.isEmpty()) return;

            // "Sản phẩm nổi bật" ưu tiên hàng đang giảm giá; "Gợi ý cho bạn" ưu tiên đánh giá cao.
            // Chia thành HAI nhóm KHÔNG trùng sản phẩm: xếp theo độ ưu tiên rồi phân xen kẽ.
            List<Product> sorted = new ArrayList<>(all);
            java.util.Collections.sort(sorted, (a, b) -> {
                if (a.getDiscount() != b.getDiscount()) return b.getDiscount() - a.getDiscount();
                return Double.compare(b.getRating(), a.getRating());
            });

            List<Product> hot = new ArrayList<>();          // ưu tiên giảm giá
            List<Product> recommended = new ArrayList<>();   // phần còn lại
            for (int i = 0; i < sorted.size(); i++) {
                if (i % 2 == 0) hot.add(sorted.get(i));
                else recommended.add(sorted.get(i));
            }

            bindRecommendedCards(recommended);
            if (hotProductsAdapter != null) hotProductsAdapter.updateData(hot);
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

        // "Xem tất cả" → trang chính của Danh mục (tab móc áo, không chọn sẵn nhóm nào)
        binding.tvCategoriesSeeAll.setOnClickListener(v ->
                Navigation.findNavController(requireView()).navigate(R.id.stylingFragment));

        // 6 thẻ danh mục → mở đúng tab tương ứng ở trang Danh mục (khớp TAB_CATEGORY_IDS)
        binding.cardCatAoDai.setOnClickListener(v -> openStylingCategory("ÁO DÀI"));
        binding.cardCatNhatBinh.setOnClickListener(v -> openStylingCategory("NHẬT BÌNH"));
        binding.cardCatAoTac.setOnClickListener(v -> openStylingCategory("ÁO TẤC"));
        binding.cardCatGiaoLinh.setOnClickListener(v -> openStylingCategory("GIAO LĨNH"));
        binding.cardCatYemDao.setOnClickListener(v -> openStylingCategory("YẾM ĐÀO"));
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

    /** Ảnh bìa (front) của mỗi danh mục. Mặt sau (layout_cat_*_back trong XML) đã có sẵn tên
     *  danh mục + logo TiredCity; ảnh nền của mặt sau (img_cat_*_back) tạm lấy gradient
     *  tc_bg_cat_6, sẽ được thay bằng ảnh thật sau. 6 thẻ tự động lật qua lại. */
    private void setupCategoryImages() {
        // Ảnh nguồn khá lớn (1254×1254) so với thẻ vuông nhỏ ~100dp — nạp qua Glide với
        // override để không giải mã 6 bitmap full-size cùng lúc (tốn ~6MB RAM/ảnh nếu nạp thẳng).
        loadCategoryImage(binding.imgCatAoDaiFront, R.drawable.cate_ao_dai);
        loadCategoryImage(binding.imgCatNhatBinhFront, R.drawable.cate_nhat_binh);
        loadCategoryImage(binding.imgCatAoTacFront, R.drawable.cate_ao_tac);
        loadCategoryImage(binding.imgCatGiaoLinhFront, R.drawable.cate_giao_linh);
        loadCategoryImage(binding.imgCatYemDaoFront, R.drawable.cate_yem_dao);
        loadCategoryImage(binding.imgCatAccessoriesFront, R.drawable.cate_phu_kien);

        categoryFlipCards = java.util.Arrays.asList(
                new CategoryFlipCard(binding.imgCatAoDaiFront, binding.layoutCatAoDaiBack),
                new CategoryFlipCard(binding.imgCatNhatBinhFront, binding.layoutCatNhatBinhBack),
                new CategoryFlipCard(binding.imgCatAoTacFront, binding.layoutCatAoTacBack),
                new CategoryFlipCard(binding.imgCatGiaoLinhFront, binding.layoutCatGiaoLinhBack),
                new CategoryFlipCard(binding.imgCatYemDaoFront, binding.layoutCatYemDaoBack),
                new CategoryFlipCard(binding.imgCatAccessoriesFront, binding.layoutCatAccessoriesBack));

        categoryFlipHandler  = new Handler(Looper.getMainLooper());
        categoryFlipRunnable = () -> {
            for (CategoryFlipCard card : categoryFlipCards) card.flip();
            categoryFlipHandler.postDelayed(categoryFlipRunnable, CATEGORY_FLIP_INTERVAL);
        };
    }

    private void loadCategoryImage(android.widget.ImageView target, int drawableRes) {
        Glide.with(this).load(drawableRes).override(400, 400).centerCrop().into(target);
    }

    private void startCategoryFlip() {
        if (categoryFlipHandler != null && categoryFlipRunnable != null) {
            categoryFlipHandler.postDelayed(categoryFlipRunnable, CATEGORY_FLIP_INTERVAL);
        }
    }

    private void stopCategoryFlip() {
        if (categoryFlipHandler != null && categoryFlipRunnable != null) {
            categoryFlipHandler.removeCallbacks(categoryFlipRunnable);
        }
    }

    /** Lật 3D quanh trục Y giữa ảnh bìa (front) và mặt sau (back — ảnh thứ hai + tên danh mục
     *  + logo TiredCity) của một thẻ danh mục. Mặt sau là một View bất kỳ (không chỉ ImageView)
     *  vì nó gồm nhiều lớp chồng (ảnh, lớp phủ tối, chữ, logo). */
    private static class CategoryFlipCard {
        private static final long FLIP_HALF_DURATION = 380L;

        private final View front;
        private final View back;
        private boolean showingFront = true;

        CategoryFlipCard(View front, View back) {
            this.front = front;
            this.back = back;
            float cameraDistance = 8000 * front.getResources().getDisplayMetrics().density;
            front.setCameraDistance(cameraDistance);
            back.setCameraDistance(cameraDistance);
        }

        void flip() {
            View outView = showingFront ? front : back;
            View inView = showingFront ? back : front;
            showingFront = !showingFront;

            ObjectAnimator out = ObjectAnimator.ofFloat(outView, View.ROTATION_Y, 0f, 90f);
            out.setDuration(FLIP_HALF_DURATION);
            out.setInterpolator(new AccelerateInterpolator());
            out.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    outView.setVisibility(View.GONE);
                    inView.setRotationY(-90f);
                    inView.setVisibility(View.VISIBLE);
                    ObjectAnimator in = ObjectAnimator.ofFloat(inView, View.ROTATION_Y, -90f, 0f);
                    in.setDuration(FLIP_HALF_DURATION);
                    in.setInterpolator(new DecelerateInterpolator());
                    in.start();
                }
            });
            out.start();
        }
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
