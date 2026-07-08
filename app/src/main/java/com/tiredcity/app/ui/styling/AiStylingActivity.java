package com.tiredcity.app.ui.styling;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.GridLayoutManager;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.FitCenter;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.tiredcity.app.R;
import com.tiredcity.app.adapter.ProductPhotoCardAdapter;
import com.tiredcity.app.data.model.Product;
import com.tiredcity.app.data.model.UserProfile;
import com.tiredcity.app.data.network.ApiClient;
import com.tiredcity.app.data.network.ApiService;
import com.tiredcity.app.data.repository.FirestoreProductRepository;
import com.tiredcity.app.databinding.ActivityAiStylingBinding;
import com.tiredcity.app.ui.base.BaseActivity;
import com.tiredcity.app.ui.shop.ProductDetailActivity;
import com.tiredcity.app.utils.ColorTaxonomy;
import com.tiredcity.app.utils.Constants;
import com.tiredcity.app.utils.GeminiStylist;
import com.tiredcity.app.utils.MenhCalculator;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AiStylingActivity extends BaseActivity {

    /** Chặn trên kích thước decode ảnh (px) cho 3 dải màu/phụ kiện/hoạ tiết hợp mệnh — ảnh nguồn
     *  trong các thư mục "banner cung mệnh" có kích thước rất khác nhau (có file lên tới vài nghìn
     *  px hoặc GIF nhiều khung hình); nếu không giới hạn, Glide có thể giải mã ở kích thước gốc và
     *  gây OutOfMemoryError khi vào trang (đã gặp thực tế với 1 GIF 2917×2917px ở mệnh Mộc). Việc
     *  ép width lẫn height về mốc này (dùng cùng FitCenter) buộc Glide luôn downsample bất kể ảnh
     *  nguồn to cỡ nào. */
    private static final int MAX_IMAGE_DECODE_PX = 1440;

    /** Cỡ decode thực tế cho 2 dải màu/phụ kiện — không bao giờ vượt bề rộng màn hình (ảnh full-width
     *  chỉ cần tới đó là đủ nét) và vẫn ≤ {@link #MAX_IMAGE_DECODE_PX} để chặn OOM. Giảm số pixel phải
     *  bo góc (RoundedCorners) nên render lần đầu nhẹ và mượt hơn. Gán 1 lần trong {@link #onCreate}. */
    private int imageDecodeCap = MAX_IMAGE_DECODE_PX;

    private ActivityAiStylingBinding binding;
    private ApiService apiService;
    private final FirestoreProductRepository firestoreRepository = new FirestoreProductRepository();
    private ProductPhotoCardAdapter recommendedAdapter;

    /** Handler tự trượt dải "Phụ kiện hợp mệnh" trái ⇄ phải (xem
     *  {@link #startAccessoriesAutoScroll()}) — dừng ở onPause để tránh rò rỉ. */
    private final Handler accessoriesScrollHandler = new Handler(Looper.getMainLooper());
    private Runnable accessoriesScrollRunnable;
    private int accessoriesScrollDir = 1;
    /** true khi mệnh hiện tại có ≥2 ảnh phụ kiện → mới đáng bật auto-trượt lại sau khi người dùng
     *  ngừng cuộn trang (xem {@link #onCreate} listener của scrollRoot). */
    private boolean accessoriesAutoScrollEligible = false;
    /** Bật lại auto-trượt sau khi người dùng buông tay cuộn trang một lúc — tránh để hai animation
     *  (auto-trượt + cuộn tay) tranh luồng chính gây giật. */
    private final Runnable resumeAutoScrollRunnable = this::startAccessoriesAutoScroll;

    /** Mệnh thật của người dùng, tính từ ngày sinh (xem {@link MenhCalculator#tinhMenh}) — quyết
     *  định toàn bộ nội dung trang (banner/màu/phụ kiện/hoạ tiết/lời khuyên/gợi ý). */
    private String realMenh = "Kim";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityAiStylingBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        binding.toolbar.setNavigationOnClickListener(v -> finish());

        apiService = ApiClient.getApiService(preferenceManager.getToken());

        imageDecodeCap = Math.min(MAX_IMAGE_DECODE_PX, getResources().getDisplayMetrics().widthPixels);

        applyHeroAspectRatio();

        recommendedAdapter = new ProductPhotoCardAdapter(null);
        binding.rvSuggestions.setLayoutManager(new GridLayoutManager(this, 2));
        binding.rvSuggestions.setAdapter(recommendedAdapter);
        binding.rvSuggestions.setNestedScrollingEnabled(false);
        recommendedAdapter.setOnProductClickListener(product -> {
            if (product.getId() == null) return;
            Intent i = new Intent(AiStylingActivity.this, com.tiredcity.app.ui.shop.ProductDetailActivity.class);
            i.putExtra(com.tiredcity.app.utils.Constants.EXTRA_PRODUCT_ID, product.getId());
            startActivity(i);
        });

        binding.btnRefreshSuggestions.setOnClickListener(v -> openSeeMoreSuggestions());

        // Trong lúc người dùng cuộn trang, tạm ngưng vòng auto-trượt dải phụ kiện (nó tick 33 lần/s
        // trên luồng chính) để cú vuốt mượt; chạy lại 900ms sau khi trang đứng yên.
        binding.scrollRoot.setOnScrollChangeListener(
                (androidx.core.widget.NestedScrollView.OnScrollChangeListener)
                        (v, x, y, oldX, oldY) -> {
                            stopAccessoriesAutoScroll();
                            if (accessoriesAutoScrollEligible) {
                                accessoriesScrollHandler.postDelayed(resumeAutoScrollRunnable, 900);
                            }
                        });
    }

    /** "Xem thêm" — mở lưới sản phẩm đầy đủ, lọc theo màu hợp mệnh đang xem hiện tại (không giới
     *  hạn danh mục, không giới hạn số lượng như 6 gợi ý ở đây). */
    private void openSeeMoreSuggestions() {
        String[] buckets = menhColorBuckets(realMenh);
        Intent intent = new Intent(this, com.tiredcity.app.ui.shop.CategoryActivity.class);
        intent.putExtra(com.tiredcity.app.ui.shop.CategoryActivity.EXTRA_CATEGORY_NAME,
                getString(R.string.aistyle_suggestions).replace("✨", "").trim());
        if (buckets.length > 0) {
            intent.putExtra(com.tiredcity.app.ui.shop.CategoryActivity.EXTRA_TAG_FILTER, buckets[0]);
        }
        startActivity(intent);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Tải lại mỗi lần màn hình được hiện ra (kể cả lần đầu, vì onResume luôn chạy sau
        // onCreate) — để nếu người dùng vừa đổi ngày sinh ở Hồ sơ rồi quay lại đây, mệnh/gợi ý
        // đổi theo ngay, không cần thoát vào lại app.
        loadUserProfileAndRecommend();
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Màn hình không còn hiển thị — dừng hẳn animation/auto-scroll, tránh chạy ngầm vô ích.
        // onResume sẽ tự dựng lại qua loadUserProfileAndRecommend() -> updateMenhAccessories/Patterns.
        stopAccessoriesAutoScroll();
    }

    private void loadUserProfileAndRecommend() {
        // Luôn hiển thị một mệnh ngay lập tức để giao diện không trống:
        // ưu tiên mệnh đã lưu → tính từ hồ sơ cache → mặc định "Kim".
        realMenh = resolveMenh();
        showMenhScreen(realMenh);

        if (preferenceManager.getMenh() != null) return;

        apiService.getProfile().enqueue(new Callback<com.tiredcity.app.data.model.ApiResponse<UserProfile>>() {
            @Override
            public void onResponse(Call<com.tiredcity.app.data.model.ApiResponse<UserProfile>> call, Response<com.tiredcity.app.data.model.ApiResponse<UserProfile>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess() && response.body().getData() != null) {
                    int birthYear = response.body().getData().getBirthYear();
                    if (birthYear <= 0) return;
                    String refined = MenhCalculator.tinhMenh(birthYear);
                    preferenceManager.setMenh(refined);
                    realMenh = refined;
                    showMenhScreen(refined);
                }
            }
            @Override public void onFailure(Call<com.tiredcity.app.data.model.ApiResponse<UserProfile>> call, Throwable t) {}
        });
    }

    /** Dựng lại TOÀN BỘ nội dung trang theo đúng mệnh thật của người dùng: banner, nhãn/phụ đề,
     *  màu/phụ kiện/hoạ tiết hợp mệnh, gợi ý trang phục + lời khuyên phong cách — gọi mỗi khi mệnh
     *  được tính lại (mở màn hình, hoặc vừa đổi ngày sinh ở Hồ sơ). */
    private void showMenhScreen(String menh) {
        loadHeroBanner(menh);
        updateMenhHeaderUi(menh);
        updateMenhColors(menh);
        updateMenhAccessories(menh);
        updateTipsCardTheme(menh);
        loadRecommendedProducts(menh);
    }

    /** Tô nền thẻ + tiêu đề "Lời khuyên phong cách" theo đúng mệnh — tách riêng khỏi
     *  {@link #loadAiStylingTip} (chạy sau khi Firestore trả dữ liệu, có độ trễ mạng) để card đổi
     *  màu ngay lập tức khi chuyển mệnh, không đợi gợi ý trang phục tải xong. */
    private void updateTipsCardTheme(String menh) {
        int accent = ContextCompat.getColor(this, MenhCalculator.getMenhTitleColorRes(menh));
        int panelBg = ContextCompat.getColor(this, MenhCalculator.getMenhPanelBgColorRes(menh));
        binding.cardTips.setCardBackgroundColor(panelBg);
        binding.cardTips.setStrokeColor(accent);
        binding.tvTipsTitle.setTextColor(accent);
    }

    /** Ưu tiên tính lại từ năm sinh (nạp âm đúng, chữa lành cache cũ) → mệnh đã lưu → "Kim". */
    private String resolveMenh() {
        UserProfile cached = preferenceManager.getUser();
        if (cached != null && cached.getBirthYear() > 0) {
            String menh = MenhCalculator.tinhMenh(cached.getBirthYear());
            if (!menh.equals(preferenceManager.getMenh())) {
                preferenceManager.setMenh(menh);
                cached.setMenh(menh);
                preferenceManager.saveUser(cached);
            }
            return menh;
        }
        String prefMenh = preferenceManager.getMenh();
        return (prefMenh != null) ? prefMenh : "Kim";
    }

    /** Tải banner thương hiệu đúng mệnh thật của người dùng (xem {@link MenhCalculator#getMenhBanner}). */
    private void loadHeroBanner(String menh) {
        Glide.with(this)
                .load(MenhCalculator.getMenhBanner(menh))
                .into(binding.ivMenhHero);
    }

    /** Tô lại tiêu đề lớn "Mệnh {tên}" (tô màu nhấn), câu mô tả và nhãn "mệnh của bạn" theo đúng
     *  mệnh thật của người dùng. Dùng chung màu nhấn với 3 mục màu/phụ kiện/hoạ tiết bên dưới để
     *  đồng bộ toàn màn hình. */
    private void updateMenhHeaderUi(String menh) {
        int accent = ContextCompat.getColor(this, MenhCalculator.getMenhTitleColorRes(menh));

        binding.tvMenhTitle.setText(MenhCalculator.getMenhTitleText(this, menh));
        binding.tvMenhTitle.setTextColor(accent);
        binding.tvMenhSubtitle.setText(MenhCalculator.getMenhDescText(this, menh));

        binding.tvYourMenhTag.setVisibility(View.VISIBLE);
        binding.tvYourMenhTag.getBackground().mutate().setTint(accent);
    }

    /** "Phụ kiện hợp mệnh" — dải ảnh CAO BẰNG NHAU (rộng tự do theo đúng tỉ lệ gốc mỗi ảnh), tự
     *  trượt trái ⇄ phải liên tục + đôi lời riêng theo mệnh đang xem. Mệnh chưa có ảnh (xem
     *  {@link MenhCalculator#getMenhAccessoryPhotos}) thì ẩn cả mục. */
    private void updateMenhAccessories(String menh) {
        int[] photos = MenhCalculator.getMenhAccessoryPhotos(menh);
        stopAccessoriesAutoScroll();
        accessoriesAutoScrollEligible = photos.length > 1;
        if (photos.length == 0) {
            binding.sectionMenhAccessories.setVisibility(View.GONE);
            return;
        }
        binding.sectionMenhAccessories.setVisibility(View.VISIBLE);
        int accent = ContextCompat.getColor(this, MenhCalculator.getMenhTitleColorRes(menh));
        int panelBg = ContextCompat.getColor(this, MenhCalculator.getMenhPanelBgColorRes(menh));
        binding.sectionMenhAccessories.getBackground().mutate().setTint(panelBg);
        binding.tvAccessoriesTitle.setTextColor(accent);
        setHighlightedText(binding.tvAccessoriesText, MenhCalculator.getMenhAccessoryText(this, menh),
                MenhCalculator.getMenhAccessoryKeywords(this, menh), accent);

        binding.layoutMenhAccessories.removeAllViews();
        float density = getResources().getDisplayMetrics().density;
        int cardHeight = (int) (260 * density);
        int radius = (int) (16 * density);
        int marginEnd = (int) (12 * density);
        for (int photo : photos) {
            ImageView image = new ImageView(this);
            // Chiều cao cố định + chiều rộng WRAP_CONTENT + adjustViewBounds → mọi ảnh cao bằng
            // nhau, rộng khác nhau tuỳ tỉ lệ gốc, không ảnh nào bị cắt xén.
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT, cardHeight);
            lp.setMarginEnd(marginEnd);
            image.setLayoutParams(lp);
            image.setAdjustViewBounds(true);
            image.setScaleType(ImageView.ScaleType.FIT_CENTER);
            Glide.with(this)
                    .load(photo)
                    .override(imageDecodeCap, imageDecodeCap)
                    .transform(new FitCenter(), new RoundedCorners(radius))
                    .into(image);
            binding.layoutMenhAccessories.addView(image);
        }
        // Chỉ đáng tự trượt khi có từ 2 ảnh trở lên (1 ảnh thì không có gì để trượt qua lại).
        if (photos.length > 1) {
            binding.scrollMenhAccessories.post(this::startAccessoriesAutoScroll);
        }
    }

    /** Tự trượt {@link com.tiredcity.app.databinding.ActivityAiStylingBinding#scrollMenhAccessories}
     *  qua lại kiểu "ping-pong" — trượt phải đến hết dải ảnh rồi quay lại, lặp vô hạn cho đến khi
     *  {@link #stopAccessoriesAutoScroll()} được gọi (onPause / dựng lại dải ảnh mới). */
    private void startAccessoriesAutoScroll() {
        stopAccessoriesAutoScroll();
        if (binding == null) return;
        HorizontalScrollView scrollView = binding.scrollMenhAccessories;
        View content = binding.layoutMenhAccessories;
        accessoriesScrollRunnable = new Runnable() {
            @Override
            public void run() {
                if (binding == null) return;
                int maxScroll = content.getWidth() - scrollView.getWidth();
                if (maxScroll <= 0) {
                    accessoriesScrollHandler.postDelayed(this, 800);
                    return;
                }
                int next = scrollView.getScrollX() + accessoriesScrollDir * ACCESSORIES_SCROLL_STEP_PX;
                if (next >= maxScroll) {
                    next = maxScroll;
                    accessoriesScrollDir = -1;
                } else if (next <= 0) {
                    next = 0;
                    accessoriesScrollDir = 1;
                }
                scrollView.scrollTo(next, 0);
                accessoriesScrollHandler.postDelayed(this, ACCESSORIES_SCROLL_INTERVAL_MS);
            }
        };
        accessoriesScrollHandler.postDelayed(accessoriesScrollRunnable, 1200);
    }

    private void stopAccessoriesAutoScroll() {
        accessoriesScrollHandler.removeCallbacks(resumeAutoScrollRunnable);
        if (accessoriesScrollRunnable != null) {
            accessoriesScrollHandler.removeCallbacks(accessoriesScrollRunnable);
        }
    }

    private static final int ACCESSORIES_SCROLL_STEP_PX = 2;
    private static final long ACCESSORIES_SCROLL_INTERVAL_MS = 30;

    /** "Màu hợp mệnh" — ảnh minh hoạ nguyên khổ (không cắt xén, giữ đúng tỉ lệ gốc) + bài viết
     *  riêng theo mệnh đang xem. Mệnh chưa có ảnh thì ẩn cả mục. */
    private void updateMenhColors(String menh) {
        int[] photos = MenhCalculator.getMenhColorPhotos(menh);
        if (photos.length == 0) {
            binding.sectionMenhColors.setVisibility(View.GONE);
            return;
        }
        binding.sectionMenhColors.setVisibility(View.VISIBLE);
        int accent = ContextCompat.getColor(this, MenhCalculator.getMenhTitleColorRes(menh));
        int panelBg = ContextCompat.getColor(this, MenhCalculator.getMenhPanelBgColorRes(menh));
        binding.sectionMenhColors.getBackground().mutate().setTint(panelBg);
        binding.tvColorsTitle.setTextColor(accent);
        setHighlightedText(binding.tvColorsText, MenhCalculator.getMenhColorText(this, menh),
                MenhCalculator.getMenhColorKeywords(this, menh), accent);

        binding.layoutMenhColors.removeAllViews();
        float density = getResources().getDisplayMetrics().density;
        int radius = (int) (16 * density);
        int marginBottom = (int) (10 * density);
        for (int i = 0; i < photos.length; i++) {
            ImageView image = new ImageView(this);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            if (i < photos.length - 1) lp.bottomMargin = marginBottom;
            image.setLayoutParams(lp);
            image.setAdjustViewBounds(true);
            image.setScaleType(ImageView.ScaleType.FIT_CENTER);
            Glide.with(this)
                    .load(photos[i])
                    .override(imageDecodeCap, imageDecodeCap)
                    .transform(new FitCenter(), new RoundedCorners(radius))
                    .into(image);
            binding.layoutMenhColors.addView(image);
        }
    }

    /** Gán {@code fullText} lên {@code tv}, in đậm + tô màu {@code accent} lên MỌI lần xuất hiện
     *  của từng từ khoá trong {@code keywords} — dùng chung cho 3 mục màu/phụ kiện/hoạ tiết hợp
     *  mệnh, mỗi mục chỉ đổi màu nhấn + bộ từ khoá theo đúng mệnh đang xem. */
    private void setHighlightedText(TextView tv, String fullText, String[] keywords, int accent) {
        if (keywords == null || keywords.length == 0) {
            tv.setText(fullText);
            return;
        }
        SpannableString spannable = new SpannableString(fullText);
        for (String keyword : keywords) {
            if (keyword == null || keyword.isEmpty()) continue;
            int start = fullText.indexOf(keyword);
            while (start >= 0) {
                int end = start + keyword.length();
                spannable.setSpan(new ForegroundColorSpan(accent), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                spannable.setSpan(new StyleSpan(Typeface.BOLD), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                start = fullText.indexOf(keyword, end);
            }
        }
        tv.setText(spannable);
    }

    /** Tỉ lệ thật (px) của banner mệnh — dùng để khoá chiều cao khung banner, đảm bảo ảnh
     *  không bao giờ bị cắt (xem {@link #applyHeroAspectRatio()}). */
    private static final int HERO_BANNER_W = 1774;
    private static final int HERO_BANNER_H = 887;

    /** Khoá chiều cao khung banner đúng theo tỉ lệ ảnh gốc (gọi 1 lần trong onCreate). */
    private void applyHeroAspectRatio() {
        int screenWidth = getResources().getDisplayMetrics().widthPixels;
        int heroHeight = Math.round(screenWidth * (HERO_BANNER_H / (float) HERO_BANNER_W));
        ViewGroup.LayoutParams lp = binding.heroMenh.getLayoutParams();
        lp.height = heroHeight;
        binding.heroMenh.setLayoutParams(lp);
    }

    private static final int SUGGESTION_COUNT = 6;
    private static final int RELEVANT_POOL = 12;

    private void loadRecommendedProducts(String menh) {
        firestoreRepository.getProducts(all -> {
            if (binding == null) return;
            if (all == null || all.isEmpty()) {
                recommendedAdapter.updateData(buildMockSuggestions());
                return;
            }

            List<Product> matched = filterByMenh(all, menh);
            List<Product> pool = matched.isEmpty() ? sortByRelevance(new ArrayList<>(all)) : matched;

            List<Product> topPool = pool.subList(0, Math.min(pool.size(), RELEVANT_POOL));
            List<Product> picks = new ArrayList<>(topPool);
            Collections.shuffle(picks);
            List<Product> shown = new ArrayList<>(picks.subList(0, Math.min(picks.size(), SUGGESTION_COUNT)));
            recommendedAdapter.updateData(shown);
            loadAiStylingTip(menh, shown);
        });
    }

    private void loadAiStylingTip(String menh, List<Product> products) {
        if (binding == null) return;

        // Chưa cấu hình khoá Gemini → giữ nguyên lời khuyên tĩnh theo mệnh, không hiện "đang tạo".
        if (!GeminiStylist.isConfigured()) {
            binding.tvStylingTip.setText(MenhCalculator.getMenhTipFallback(this, menh));
            return;
        }

        binding.tvStylingTip.setText(getString(R.string.aistyle_tip_loading));
        List<String> productNames = new ArrayList<>();
        if (products != null) {
            for (Product p : products) if (p.getName() != null && !p.getName().trim().isEmpty()) productNames.add(p.getName());
        }
        List<String> colors = new ArrayList<>(Arrays.asList(MenhCalculator.getMauHopMenh(menh)));

        GeminiStylist.suggest(menh, colors, productNames, new GeminiStylist.Callback() {
            @Override
            public void onAdvice(String advice) {
                runOnUiThread(() -> {
                    if (binding != null) binding.tvStylingTip.setText(advice);
                });
            }

            @Override
            public void onError() {
                runOnUiThread(() -> {
                    if (binding != null) binding.tvStylingTip.setText(MenhCalculator.getMenhTipFallback(AiStylingActivity.this, menh));
                });
            }
        });
    }

    private List<Product> filterByMenh(List<Product> all, String menh) {
        String[] buckets = menhColorBuckets(menh);
        List<Product> out = new ArrayList<>();
        for (Product p : all) {
            for (String bucket : buckets) {
                if (ColorTaxonomy.matchesBucket(p.getColors(), bucket)) {
                    out.add(p);
                    break;
                }
            }
        }
        return sortByRelevance(out);
    }

    /** Nhóm màu chuẩn (ColorTaxonomy) tương ứng với màu hợp từng mệnh — đúng 1-1 với tag màu hiển
     *  thị trên thẻ sản phẩm (xem color_bucket_* trong strings.xml): Kim ↔ "Trắng Bạch Ngọc", Mộc
     *  ↔ "Xanh Lục Bảo", Thủy ↔ "Xanh Lam Ngọc Bích", Hỏa ↔ "Đỏ Cẩm Thạch", Thổ ↔ "Vàng Hổ Phách"
     *  / "Cam Hổ Phách". */
    private static String[] menhColorBuckets(String menh) {
        if (menh == null) return new String[0];
        switch (menh) {
            case "Kim":  return new String[]{ColorTaxonomy.TRANG};
            case "Mộc":  return new String[]{ColorTaxonomy.XANH_LA};
            case "Thủy": return new String[]{ColorTaxonomy.XANH};
            case "Hỏa":  return new String[]{ColorTaxonomy.DO};
            case "Thổ":  return new String[]{ColorTaxonomy.VANG, ColorTaxonomy.CAM};
            default:     return new String[0];
        }
    }

    private List<Product> sortByRelevance(List<Product> list) {
        Collections.sort(list, (a, b) -> {
            int byRating = Double.compare(b.getRating(), a.getRating());
            if (byRating != 0) return byRating;
            return b.getDiscount() - a.getDiscount();
        });
        return list;
    }

    private List<Product> buildMockSuggestions() {
        String[][] data = {
            {"1", "Áo Dài Lụa Trắng",   "Lụa tơ tằm",  "850000",  "10", "4.8"},
            {"2", "Nhật Bình Vàng Đồng", "Gấm thêu kim", "1450000", "15", "4.9"},
            {"5", "Áo Tấc Trắng Ngà",    "Đũi tơ cao cấp", "990000", "0",  "4.7"},
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
            list.add(p);
        }
        return list;
    }
}
