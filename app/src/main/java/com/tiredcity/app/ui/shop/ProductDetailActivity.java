package com.tiredcity.app.ui.shop;

import android.animation.Keyframe;
import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.animation.ValueAnimator;
import android.content.Intent;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.transition.AutoTransition;
import android.transition.TransitionManager;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.tiredcity.app.R;
import com.tiredcity.app.adapter.ReviewAdapter;
import com.tiredcity.app.data.local.CartLocalStore;
import com.tiredcity.app.data.local.FavoritesLocalStore;
import com.tiredcity.app.data.local.RecentlyViewedStore;
import com.tiredcity.app.data.mock.MockProductCatalog;
import com.tiredcity.app.data.mock.MockReviewCatalog;
import com.tiredcity.app.data.model.ApiListResponse;
import com.tiredcity.app.data.model.ApiResponse;
import com.tiredcity.app.data.model.CartItem;
import com.tiredcity.app.data.model.Product;
import com.tiredcity.app.data.model.Review;
import com.tiredcity.app.data.network.ApiClient;
import com.tiredcity.app.data.repository.FirestoreProductRepository;
import com.tiredcity.app.data.repository.FavoritesRepository;
import com.tiredcity.app.data.repository.ProductRepository;
import androidx.annotation.NonNull;
import androidx.viewpager2.widget.ViewPager2;
import com.tiredcity.app.databinding.ActivityProductDetailBinding;
import com.tiredcity.app.databinding.ItemHeroCarouselPhotoBinding;
import com.tiredcity.app.databinding.ItemRatingBarRowBinding;
import com.tiredcity.app.ui.base.BaseActivity;
import com.tiredcity.app.ui.cart.CartActivity;
import com.tiredcity.app.ui.support.PolicyActivity;
import com.tiredcity.app.utils.ColorTaxonomy;
import com.tiredcity.app.utils.Constants;
import com.tiredcity.app.utils.PriceUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * ProductDetailActivity - Tối ưu bởi Senior Architect (Pure Java).
 * Kiến trúc MVVM, Xử lý dữ liệu tập trung, Giao diện mượt mà & Chống Crash.
 */
public class ProductDetailActivity extends BaseActivity {

    private ActivityProductDetailBinding binding;
    private ProductRepository productRepository;
    private FirestoreProductRepository firestoreRepository;
    private CartLocalStore cartLocalStore;
    private ProductDetailViewModel viewModel;
    private CartLocalStore cartStore;
    private FavoritesLocalStore favoritesStore;
    private RecentlyViewedStore recentlyViewedStore;
    private FavoritesRepository favoritesRepository;
    private AlertDialog reviewDialog;

    private Product currentProduct;
    private String selectedSize = "";
    private String selectedColor = null;
    private int quantity = 1;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityProductDetailBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        initDependencies();
        initUI();
        observeData();

        String productId = getIntent().getStringExtra(Constants.EXTRA_PRODUCT_ID);
        if (productId != null) {
            viewModel.loadProduct(productId);
        } else {
            Toast.makeText(this, R.string.error_product_load_failed, Toast.LENGTH_SHORT).show();
            finish();
        }
    }

        String productId = getIntent().getStringExtra(EXTRA_PRODUCT_ID);
        productRepository = new ProductRepository(ApiClient.getApiService(preferenceManager.getToken()));
        cartLocalStore     = new CartLocalStore(this);
        favoritesStore     = new FavoritesLocalStore(this);
        recentlyViewedStore = new RecentlyViewedStore(this);
    private void initDependencies() {
        ProductRepository repository = new ProductRepository(ApiClient.getApiService(preferenceManager.getToken()));
        cartStore = new CartLocalStore(this);
        favoritesStore = new FavoritesLocalStore(this);
        favoritesRepository = new FavoritesRepository(favoritesStore);

        // Sử dụng Factory chuyên biệt để khởi tạo ViewModel
        ProductDetailViewModelFactory factory = new ProductDetailViewModelFactory(repository);
        viewModel = new ViewModelProvider(this, factory).get(ProductDetailViewModel.class);
    }

    private void initUI() {
        // Back navigation
        binding.btnBack.setOnClickListener(v -> finishSmoothly());

        // Add to cart actions
        binding.btnAddToCart.setOnClickListener(v -> addToCart());
        binding.btnBuyNow.setOnClickListener(v -> addToCartAndBuyNow());

        // Review actions
        binding.btnWriteReview.setOnClickListener(v -> showAddReviewDialog());

        // Setup static UI components
        setupSizeSelector();
        setupInfoRows();
        setupCardStackScroll();
        setupSizeGuideImages();
        setupAccordions();
        setupPolicyIcons();
        setupPolicyCards();

        // Setup Related Products RecyclerView
        binding.rvRelated.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
    }

    private void setupPolicyIcons() {
        binding.ivShippingIcon.setImageResource(R.drawable.ic_policy_shipping);
        binding.ivReturnsIcon.setImageResource(R.drawable.ic_policy_return);
        binding.ivPaymentIcon.setImageResource(R.drawable.ic_policy_payment);

        binding.tvShippingText.setText(R.string.label_free_shipping);
        binding.tvReturnsText.setText(R.string.label_easy_returns);
        binding.tvPaymentText.setText(R.string.label_secure_payment);
    }

    private void observeData() {
        // Lắng nghe dữ liệu sản phẩm từ ViewModel
        viewModel.getProduct().observe(this, product -> {
            if (product != null) {
                currentProduct = product;
                bindProductToUI(product);
            }
        });

        // Lắng nghe trạng thái loading
        viewModel.getIsLoading().observe(this, isLoading -> {
            // Hiển thị Shimmer hoặc Progress nếu cần
        });

        // Lắng nghe lỗi - Thêm fallback cho dữ liệu mẫu offline
        viewModel.getErrorMessage().observe(this, (String error) -> {
            if (error != null && !error.isEmpty()) {
                Toast.makeText(ProductDetailActivity.this, error, Toast.LENGTH_SHORT).show();
            }
        });

        // Lắng nghe trạng thái gửi feedback
        viewModel.getIsSubmitting().observe(this, isSubmitting -> {
            if (reviewDialog != null && reviewDialog.isShowing()) {
                reviewDialog.findViewById(R.id.btn_submit).setEnabled(!isSubmitting);
                reviewDialog.findViewById(R.id.btn_cancel).setEnabled(!isSubmitting);
                if (isSubmitting) {
                    ((android.widget.Button)reviewDialog.findViewById(R.id.btn_submit)).setText("Đang gửi...");
                } else {
                    ((android.widget.Button)reviewDialog.findViewById(R.id.btn_submit)).setText("Gửi đánh giá");
                }
            }
        });

        viewModel.getIsSubmitSuccess().observe(this, success -> {
            if (success != null && success) {
                if (reviewDialog != null && reviewDialog.isShowing()) {
                    reviewDialog.dismiss();
                }
                Toast.makeText(this, "Cảm ơn bạn đã gửi đánh giá!", Toast.LENGTH_SHORT).show();
            }
        });

        viewModel.getCanUserReview().observe(this, canReview -> {
            binding.btnWriteReview.setVisibility(canReview ? View.VISIBLE : View.GONE);

            // Tự động mở dialog đánh giá nếu được yêu cầu từ màn hình Đơn hàng
            if (canReview && getIntent().getBooleanExtra(Constants.EXTRA_ACTION_REVIEW, false)) {
                getIntent().removeExtra(Constants.EXTRA_ACTION_REVIEW); // Chỉ mở một lần
                showAddReviewDialog();
            }
        });

    private void fallbackToMockOrFinish(String productId) {
        // Backend REST không nhận id → thử Firestore (cùng nguồn với admin, có ảnh)
        // trước, chỉ dùng dữ liệu mẫu khi Firestore cũng không có.
        if (firestoreRepository == null) firestoreRepository = new FirestoreProductRepository();
        firestoreRepository.getProductById(productId, product -> {
            if (product != null) {
                currentProduct = product;
                bindProduct(product);
            } else {
                fallbackToMock(productId);
            }
        });
    }

    private void fallbackToMock(String productId) {
        Product mock = MockProductCatalog.findById(this, productId);
        if (mock != null) {
            currentProduct = mock;
            bindProduct(mock);
        } else {
            Toast.makeText(this, R.string.error_product_load_failed, Toast.LENGTH_SHORT).show();
            finish();
        }
        // Lắng nghe đánh giá
        viewModel.getReviews().observe(this, reviews -> {
            if (reviews != null && !reviews.isEmpty()) {
                bindReviews(reviews);
            } else {
                // Fallback: Nếu Firestore chưa có đánh giá cho mã này, hiện đánh giá mẫu
                bindReviews(buildMockReviews());
            }
        });

        // Lắng nghe sản phẩm liên quan
        viewModel.getRelatedProducts().observe(this, list -> {
            if (list != null) {
                ProductAdapter adapter = new ProductAdapter(list);
                adapter.setOnProductClickListener(new ProductAdapter.OnProductClickListener() {
                    @Override public void onProductClick(Product p) {
                        Intent intent = new Intent(ProductDetailActivity.this, ProductDetailActivity.class);
                        intent.putExtra(Constants.EXTRA_PRODUCT_ID, p.getId());
                        startSmoothActivity(intent);
                    }
                    @Override public void onSaveToggle(Product p, boolean saved) {}
                    @Override public void onAddToCartClick(Product p) {
                        // Bắt buộc chọn size -> Mở màn hình chi tiết cho sản phẩm liên quan đó
                        Intent intent = new Intent(ProductDetailActivity.this, ProductDetailActivity.class);
                        intent.putExtra(Constants.EXTRA_PRODUCT_ID, p.getId());
                        startSmoothActivity(intent);
                        Toast.makeText(ProductDetailActivity.this, "Vui lòng chọn Size trước khi thêm vào giỏ", Toast.LENGTH_SHORT).show();
                    }
                });
                binding.rvRelated.setAdapter(adapter);
            }
        });
    }

    private void bindProduct(Product product) {
        // Ghi nhận đây là sản phẩm khách thật sự đã xem → hiển thị lại ở "Đã xem gần đây".
        if (recentlyViewedStore != null) recentlyViewedStore.addProduct(product);

        // Điền mô tả/câu chuyện/thông số còn thiếu hoặc còn sơ sài (ví dụ sản phẩm lấy từ
        // Firestore/REST chưa được admin viết nội dung riêng) — áp dụng cho MỌI nguồn dữ liệu,
        // không riêng dữ liệu mẫu, để mục 01/02/03 luôn đầy đủ và khác nhau theo từng sản phẩm.
        MockProductCatalog.applyGenericDetail(this, product);

        binding.tvProductName.setText(product.getName());
        binding.tvPrice.setText(PriceUtils.format(product.getEffectivePrice()));
        binding.tvMaterial.setText(product.getMaterial() != null ? product.getMaterial() : "");
        binding.tvOrigin.setText(product.getOrigin() != null ? product.getOrigin() : getString(R.string.default_origin));
        binding.tvDescription.setText(product.getDescription() != null ? product.getDescription() : "");
        binding.tvStory.setText(product.getStory() != null ? product.getStory() : "");
        binding.rbRating.setRating((float) product.getRating());
    private void bindProductToUI(Product p) {
        binding.tvProductName.setText(p.getName());
        binding.tvPrice.setText(PriceUtils.format(p.getEffectivePrice()));
        binding.tvMaterial.setText(p.getMaterial() != null ? p.getMaterial() : "");
        binding.tvOrigin.setText(p.getOrigin() != null ? p.getOrigin() : getString(R.string.default_origin));
        binding.tvDescription.setText(p.getDescription());
        binding.tvStory.setText(p.getStory());
        binding.rbRating.setRating((float) p.getRating());

        updateAvailabilityUI(p);
        updateImageCarousel(p);
        updateColorSwatches(p);
        updateSpecifications(p);
        updateCareInstructions(p);
        updateQuantityStepper(p);
        updateFavoriteButton(p);
    }

    // ── UI Update Methods ───────────────────────────────────────────────────

    private void updateAvailabilityUI(Product p) {
        int stock = p.getStock();
        boolean out = stock <= 0;
        int statusColor = ContextCompat.getColor(this, out ? R.color.tc_red : (stock <= 5 ? R.color.tc_gold_deep : R.color.tc_success));
        String statusText = getString(out ? R.string.status_out_of_stock : (stock <= 5 ? R.string.status_low_stock : R.string.status_in_stock));

        binding.tvAvailability.setText("●  " + statusText);
        binding.tvAvailability.setTextColor(statusColor);

        GradientDrawable pill = new GradientDrawable();
        pill.setCornerRadius(dp(20));
        pill.setColor((statusColor & 0x00FFFFFF) | 0x22000000);
        binding.tvAvailability.setBackground(pill);
    // ── Availability ─────────────────────────────────────────────────────────

    private void bindAvailability(Product product) {
        int stock = product.getStock();
        boolean outOfStock = stock <= 0;
        int dotColor;
        String statusText;
        if (outOfStock) {
            dotColor = getColor(R.color.tc_stroke);
            statusText = getString(R.string.status_out_of_stock);
        } else if (stock <= 5) {
            dotColor = getColor(R.color.tc_gold_deep);
            statusText = getString(R.string.status_low_stock);
        } else {
            dotColor = getColor(R.color.tc_red);
            statusText = getString(R.string.status_in_stock);
        }
        // Viên xám trung tính (đặt cố định trong XML) + chấm màu theo trạng thái + chữ luôn tối
        // màu — thay cho kiểu tô cả viên theo màu trạng thái cũ.
        binding.tvAvailability.setText(statusText);
        binding.dotAvailability.setBackgroundTintList(android.content.res.ColorStateList.valueOf(dotColor));

        binding.btnAddToCart.setEnabled(!out);
        binding.btnBuyNow.setEnabled(!out);
        binding.btnAddToCart.setAlpha(out ? 0.5f : 1.0f);
        binding.btnAddToCart.setEnabled(!outOfStock);
        binding.btnBuyNow.setEnabled(!outOfStock);
        binding.btnAddToCart.setAlpha(outOfStock ? 0.5f : 1f);
        binding.btnBuyNow.setAlpha(outOfStock ? 0.5f : 1f);
    }

    // ── Specifications (Chi tiết) ────────────────────────────────────────────

    private void bindSpecifications(Product product) {
        binding.llSpecifications.removeAllViews();
        Map<String, String> specs = product.getSpecifications();
        if (specs == null) return;
        String materialLabel = getString(R.string.label_material);
        String originLabel = getString(R.string.label_origin);
        for (Map.Entry<String, String> entry : specs.entrySet()) {
            // Chất liệu/Xuất xứ đã hiển thị ở 2 dòng cố định phía trên — bỏ qua để tránh lặp.
            if (entry.getKey().equalsIgnoreCase(materialLabel) || entry.getKey().equalsIgnoreCase(originLabel)) {
                continue;
            }
            binding.llSpecifications.addView(buildSpecRow(entry.getKey(), entry.getValue()));
        }
    }

    private LinearLayout buildSpecRow(String label, String value) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        row.setPadding(0, 0, 0, dp(6));

        TextView tvLabel = new TextView(this);
        tvLabel.setLayoutParams(new LinearLayout.LayoutParams(dp(100), LinearLayout.LayoutParams.WRAP_CONTENT));
        tvLabel.setText(label);
        tvLabel.setTextColor(getColor(R.color.text_secondary));
        tvLabel.setTextSize(13);

        TextView tvValue = new TextView(this);
        LinearLayout.LayoutParams valueParams = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        tvValue.setLayoutParams(valueParams);
        tvValue.setText(value);
        tvValue.setTextColor(getColor(R.color.text_primary));
        tvValue.setTextSize(13);
        tvValue.setTypeface(tvValue.getTypeface(), android.graphics.Typeface.BOLD);

        row.addView(tvLabel);
        row.addView(tvValue);
        return row;
    }

    // ── Care instructions (Bảo quản) — hàng ngang icon + chú thích, nền trắng (thẻ 05) ─────

    private static final int[] CARE_ICONS = {
            R.drawable.ic_care_handwash, R.drawable.ic_care_nobleach,
            R.drawable.ic_care_dryshade, R.drawable.ic_care_iron
    };

    private void bindCareInstructions(Product product) {
        binding.contentCare.removeAllViews();
        List<String> care = product.getCareInstructions();
        if (care == null) return;
        for (int i = 0; i < care.size(); i++) {
            binding.contentCare.addView(buildCareItem(CARE_ICONS[i % CARE_ICONS.length], care.get(i)));
        }
    }

    private LinearLayout buildCareItem(int iconRes, String caption) {
        LinearLayout col = new LinearLayout(this);
        LinearLayout.LayoutParams colParams = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        colParams.setMarginEnd(dp(6));
        col.setLayoutParams(colParams);
        col.setOrientation(LinearLayout.VERTICAL);
        col.setGravity(android.view.Gravity.CENTER_HORIZONTAL);

        ImageView icon = new ImageView(this);
        icon.setLayoutParams(new LinearLayout.LayoutParams(dp(28), dp(28)));
        icon.setImageResource(iconRes);

        TextView tv = new TextView(this);
        LinearLayout.LayoutParams tvParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        tvParams.topMargin = dp(6);
        tv.setLayoutParams(tvParams);
        tv.setText(caption);
        tv.setTextColor(getColor(R.color.text_secondary));
        tv.setTextSize(10.5f);
        tv.setGravity(android.view.Gravity.CENTER);
        tv.setLineSpacing(dp(1), 1f);

        col.addView(icon);
        col.addView(tv);
        return col;
    }

    private void updateImageCarousel(Product p) {
        List<String> images = p.getImages();
        if (images == null || images.isEmpty()) {
            images = new ArrayList<>();
            images.add(p.getFirstImage());
        }
        binding.vpProductImages.setAdapter(new ImagePagerAdapter(images));
        binding.dotsImages.attachTo(binding.vpProductImages);
    }
    // ── Hero images (banner ngang full-bleed, liền mạch với mục thông tin bên dưới — không
    //    còn kiểu "thẻ nổi" bo góc/xoay như trước) ─────────────────────────────────────────────

    private void bindImages(Product product) {
        final List<String> images = product.getImages();
        final boolean hasImage = images != null && !images.isEmpty();

        ViewPager2 pager = binding.vpProductImages;
        pager.setAdapter(new androidx.recyclerview.widget.RecyclerView.Adapter<HeroPhotoViewHolder>() {
            @NonNull
            @Override
            public HeroPhotoViewHolder onCreateViewHolder(@NonNull android.view.ViewGroup parent, int viewType) {
                ItemHeroCarouselPhotoBinding b = ItemHeroCarouselPhotoBinding.inflate(
                        getLayoutInflater(), parent, false);
                return new HeroPhotoViewHolder(b);
            }

            @Override
            public void onBindViewHolder(@NonNull HeroPhotoViewHolder holder, int position) {
                ImageView photo = holder.b.ivPhoto;
                if (hasImage) {
                    // Load ĐÚNG ảnh theo vị trí (trước đây luôn load ảnh đầu → các trang bị lặp).
                    Glide.with(photo.getContext())
                        .load(images.get(position))
                        .timeout(30000)
                        .centerCrop()
                        .placeholder(R.color.tc_red_deep)
                        .into(photo);
                } else {
                    // Sản phẩm mẫu/offline chưa có ảnh thật — hiển thị hình minh hoạ thay vì khung trống.
                    photo.setBackgroundColor(getColor(R.color.tc_red_deep));
                    photo.setImageResource(R.drawable.ic_wardrobe);
                    photo.setColorFilter(getColor(R.color.tc_on_red));
                    int inset = dp(72);
                    photo.setPadding(inset, inset, inset, inset);
                }
                startShine(holder);
            }

    private void updateColorSwatches(Product p) {
            @Override
            public int getItemCount() {
                return hasImage ? images.size() : 1;
            }
        });

        pager.setOffscreenPageLimit(3);
        // Hiệu ứng "card swipe": ảnh đang vuốt đi thu nhỏ + mờ dần, ảnh mới trượt vào phóng to +
        // hiện rõ dần — vẫn full-bleed khi đứng yên (scale/alpha = 1 tại position 0).
        pager.setPageTransformer((page, position) -> {
            if (position < -1f || position > 1f) {
                page.setAlpha(0f);
            } else {
                float scale = 1f - (0.15f * Math.abs(position));
                page.setScaleX(scale);
                page.setScaleY(scale);
                page.setAlpha(1f - Math.abs(position) * 0.5f);
            }
        });
        binding.dotsImages.attachTo(pager);
    }

    static class HeroPhotoViewHolder extends androidx.recyclerview.widget.RecyclerView.ViewHolder {
        final ItemHeroCarouselPhotoBinding b;
        ObjectAnimator shineAnimator;
        HeroPhotoViewHolder(ItemHeroCarouselPhotoBinding binding) {
            super(binding.getRoot());
            this.b = binding;
        }
    }

    // Hiệu ứng "tráng gương": dải sáng chéo (v_shine) quét ngang qua ảnh rồi dừng ở ngoài khung
    // hình, lặp lại theo chu kỳ — mô phỏng bề mặt bóng như tráng gương thay vì ảnh tĩnh.
    private void startShine(HeroPhotoViewHolder holder) {
        if (holder.shineAnimator != null) {
            holder.shineAnimator.cancel();
        }
        View shine = holder.b.vShine;
        float travel = getResources().getDisplayMetrics().widthPixels * 0.9f;

        Keyframe hold1 = Keyframe.ofFloat(0f, -travel);
        Keyframe holdUntilSweep = Keyframe.ofFloat(0.15f, -travel);
        Keyframe sweepEnd = Keyframe.ofFloat(0.45f, travel);
        Keyframe holdAfterSweep = Keyframe.ofFloat(1f, travel);
        PropertyValuesHolder pvh = PropertyValuesHolder.ofKeyframe(
                "translationX", hold1, holdUntilSweep, sweepEnd, holdAfterSweep);

        ObjectAnimator anim = ObjectAnimator.ofPropertyValuesHolder(shine, pvh);
        anim.setDuration(3200);
        anim.setRepeatCount(ValueAnimator.INFINITE);
        anim.start();
        holder.shineAnimator = anim;
    }

    // ── Colour swatches ──────────────────────────────────────────────────────

    private void bindColors(Product product) {
        binding.llColors.removeAllViews();
        List<String> raw = p.getColors();
        if (raw == null || raw.isEmpty()) {
            binding.llColors.setVisibility(View.GONE);
            return;
        }

        List<String> buckets = new ArrayList<>();
        for (String c : raw) {
            for (String b : ColorTaxonomy.normalize(c)) {
                if (!buckets.contains(b)) buckets.add(b);
            }
        }

        if (buckets.isEmpty()) {
            binding.llColors.setVisibility(View.GONE);
            return;
        }

        binding.llColors.setVisibility(View.VISIBLE);
        selectedColor = buckets.get(0);
        List<View> rings = new ArrayList<>();

        for (String bucket : buckets) {
            FrameLayout frame = new FrameLayout(this);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(dp(36), dp(36));
            lp.setMargins(0, 0, dp(10), 0);
            frame.setLayoutParams(lp);

            View ring = new View(this);
            ring.setLayoutParams(new FrameLayout.LayoutParams(dp(36), dp(36)));
            ring.setBackgroundResource(R.drawable.tc_bg_circle_ring_selected);
            ring.setVisibility(bucket.equals(selectedColor) ? View.VISIBLE : View.INVISIBLE);

            View dot = new View(this);
            FrameLayout.LayoutParams dlp = new FrameLayout.LayoutParams(dp(26), dp(26));
            dlp.gravity = Gravity.CENTER;
            dot.setLayoutParams(dlp);

            GradientDrawable shape = new GradientDrawable();
            shape.setShape(GradientDrawable.OVAL);
            shape.setColor(ContextCompat.getColor(this, getColorResForBucket(bucket)));
            // Dùng màu Espresso đậm để làm viền cho rõ nét
            shape.setStroke(dp(1), ContextCompat.getColor(this, R.color.tc_espresso));
            dot.setBackground(shape);

            frame.addView(dot);
            frame.addView(ring);
            rings.add(ring);

            frame.setOnClickListener(v -> {
                selectedColor = bucket;
                for (int i = 0; i < rings.size(); i++) {
                    rings.get(i).setVisibility(buckets.get(i).equals(bucket) ? View.VISIBLE : View.INVISIBLE);
                }
            });
            binding.llColors.addView(frame);
        }
    }

    private int getColorResForBucket(String bucket) {
        switch (bucket) {
            case ColorTaxonomy.DO:      return R.color.tc_swatch_do;
            case ColorTaxonomy.XANH:    return R.color.tc_swatch_xanh;
            case ColorTaxonomy.VANG:    return R.color.tc_swatch_vang;
            case ColorTaxonomy.TRANG:   return R.color.tc_swatch_trang;
            case ColorTaxonomy.DEN:     return R.color.tc_swatch_den;
            case ColorTaxonomy.HONG:    return R.color.tc_swatch_hong;
            case ColorTaxonomy.TIM:     return R.color.tc_swatch_tim;
            case ColorTaxonomy.XANH_LA: return R.color.tc_swatch_xanh_la;
            case ColorTaxonomy.CAM:     return R.color.tc_swatch_cam;
            default:                    return R.color.tc_bg_subtle;
        }
    }

    private void updateSpecifications(Product p) {
        binding.llSpecifications.removeAllViews();
        Map<String, String> specs = p.getSpecifications();
        if (specs == null) return;
        for (Map.Entry<String, String> entry : specs.entrySet()) {
            if (entry.getKey().equalsIgnoreCase(getString(R.string.label_material)) ||
                entry.getKey().equalsIgnoreCase(getString(R.string.label_origin))) continue;

            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setPadding(0, 0, 0, dp(6));

            TextView label = new TextView(this);
            label.setLayoutParams(new LinearLayout.LayoutParams(dp(100), -2));
            label.setText(entry.getKey());
            label.setTextColor(ContextCompat.getColor(this, R.color.text_secondary));
            label.setTextSize(13);

            TextView val = new TextView(this);
            val.setLayoutParams(new LinearLayout.LayoutParams(0, -2, 1f));
            val.setText(entry.getValue());
            val.setTextColor(ContextCompat.getColor(this, R.color.text_primary));
            val.setTypeface(null, Typeface.BOLD);
            val.setTextSize(13);

            row.addView(label); row.addView(val);
            binding.llSpecifications.addView(row);
        }
    }

    private void selectSize(String size, TextView[] sizeViews) {
        selectedSize = size;
        for (TextView v : sizeViews) {
            boolean selected = v.getText().toString().equalsIgnoreCase(size);
            v.setBackgroundResource(selected ? R.drawable.tc_bg_variant_selected : R.drawable.tc_bg_variant);
            v.setTextColor(getColor(selected ? R.color.white : R.color.tc_red));
    private void updateCareInstructions(Product p) {
        binding.contentCare.removeAllViews();
        List<String> care = p.getCareInstructions();
        if (care == null) return;
        for (String line : care) {
            TextView tv = new TextView(this);
            tv.setText("• " + line);
            tv.setTextColor(ContextCompat.getColor(this, R.color.text_secondary));
            tv.setTextSize(13);
            tv.setPadding(0, 0, 0, dp(6));
            binding.contentCare.addView(tv);
        }
    }

    private void updateQuantityStepper(Product p) {
        quantity = 1;
        binding.tvQuantity.setText(String.valueOf(quantity));
        binding.btnQtyMinus.setOnClickListener(v -> {
            if (quantity > 1) { quantity--; binding.tvQuantity.setText(String.valueOf(quantity)); }
        });
        binding.btnQtyPlus.setOnClickListener(v -> {
            if (quantity < p.getStock()) { quantity++; binding.tvQuantity.setText(String.valueOf(quantity)); }
            else Toast.makeText(this, R.string.out_of_stock, Toast.LENGTH_SHORT).show();
        });
    }

    // ── Vertical Card Stack scroll animation ────────────────────────────────

    /** 6 thẻ số thứ tự (01..06) "ghim" lại đúng đỉnh khung nhìn khi cuộn tới, rồi thu nhỏ + mờ
     *  dần khi thẻ kế tiếp trượt trùm lên — hiệu ứng "xếp chồng" kiểu trang sản phẩm Apple.
     *  Toạ độ gốc của mỗi thẻ chỉ đo được sau khi layout xong nên chờ 1 lượt post(). */
    private void setupCardStackScroll() {
        View[] cards = {
                binding.cardSection1, binding.cardSection2, binding.cardSection3,
                binding.cardSection4, binding.cardSection5, binding.cardSection6
        };
        View scrollContent = binding.cardSection1;
        scrollContent.post(() -> {
            int[] naturalTop = new int[cards.length];
            for (int i = 0; i < cards.length; i++) {
                naturalTop[i] = cards[i].getTop();
            }
            binding.scrollProductDetail.setOnScrollChangeListener(
                    (androidx.core.widget.NestedScrollView.OnScrollChangeListener) (v, scrollX, scrollY, oldX, oldY) ->
                            applyStackTransforms(cards, naturalTop, scrollY));
            applyStackTransforms(cards, naturalTop, binding.scrollProductDetail.getScrollY());
        });
    }

    private static final float STACK_SHRINK_MIN_SCALE = 0.94f;
    private static final float STACK_FADE_MIN_ALPHA = 0.55f;
    private static final int STACK_SHRINK_WINDOW_DP = 140;

    private void applyStackTransforms(View[] cards, int[] naturalTop, int scrollY) {
        int shrinkWindow = dp(STACK_SHRINK_WINDOW_DP);
        // Thẻ CUỐI (06) không ghim — không có thẻ nào trượt trùm lên nó, nên cứ để nó cuộn qua
        // bình thường như nội dung phẳng, không giữ lại ở đỉnh khung nhìn khi qua đến "Có thể
        // bạn cũng thích"/"Cam kết dịch vụ" phía dưới.
        int pinnableCount = cards.length - 1;
        for (int i = 0; i < cards.length; i++) {
            View card = cards[i];

            if (i >= pinnableCount) {
                card.setTranslationY(0f);
                card.setScaleX(1f);
                card.setScaleY(1f);
                card.setAlpha(1f);
                continue;
            }

            // QUAN TRỌNG: chặn (cap) localScroll tại đúng lúc thẻ kế tiếp bắt đầu ghim
            // (naturalTop[i+1]) — nếu không, translationY cứ tăng theo scrollY KHÔNG GIỚI HẠN,
            // giữ thẻ này đứng yên ở đỉnh khung nhìn MÃI MÃI (chỉ mờ+nhỏ đi chứ không biến mất),
            // gây "bóng mờ" đè lên nội dung phía dưới khi cuộn qua khỏi cả thẻ cuối (xem ảnh lỗi:
            // mục 04/05 vẫn hiện mờ mờ dù đã cuộn qua mục 06). Sau khi bị che hẳn, thẻ phải cùng
            // "trôi" lên khỏi màn hình với tốc độ cuộn bình thường, không dừng lại giữa chừng.
            int cappedScrollY = Math.min(scrollY, naturalTop[i + 1]);
            int localScroll = cappedScrollY - naturalTop[i];

            // Chưa tới điểm ghim: cuộn bình thường, không biến đổi.
            if (localScroll <= 0) {
                card.setTranslationY(0f);
                card.setScaleX(1f);
                card.setScaleY(1f);
                card.setAlpha(1f);
                continue;
            }

            // Ghim thẻ tại đỉnh khung nhìn cho tới khi thẻ kế tiếp cuộn trùm hẳn lên, sau đó
            // translationY bị "đóng băng" ở giá trị đã cap nên thẻ trôi lên cùng tốc độ cuộn.
            card.setTranslationY(localScroll);

            // Thẻ kế tiếp càng gần điểm ghim của nó thì thẻ này càng thu nhỏ + mờ dần — bắt đầu
            // co lại từ trước SHRINK_WINDOW dp, co xong (progress=1) đúng lúc thẻ sau ghim.
            int distanceToNext = naturalTop[i + 1] - scrollY;
            float progress = 1f - Math.max(0f, Math.min(1f, (float) distanceToNext / shrinkWindow));
            float scale = 1f - (1f - STACK_SHRINK_MIN_SCALE) * progress;
            card.setPivotX(card.getWidth() / 2f);
            card.setPivotY(0f);
            card.setScaleX(scale);
            card.setScaleY(scale);
            card.setAlpha(1f - (1f - STACK_FADE_MIN_ALPHA) * progress);
        }
    }

    // ── Reviews summary ──────────────────────────────────────────────────────

    private void loadReviews(String productId) {
        productRepository.getProductReviews(productId).enqueue(new Callback<ApiListResponse<Review>>() {
            @Override
            public void onResponse(Call<ApiListResponse<Review>> call, Response<ApiListResponse<Review>> response) {
                List<Review> reviews = (response.isSuccessful() && response.body() != null && response.body().isSuccess())
                        ? response.body().getData() : null;
                // Backend chưa có đánh giá thật (offline/demo) → dùng bộ đánh giá mẫu thay vì
                // để trống, để mục "Đánh giá khách hàng" luôn có nội dung xem được.
                if (reviews == null || reviews.isEmpty()) {
                    reviews = MockReviewCatalog.getReviewsForProduct(currentProduct);
                }
                applyReviews(reviews);
            }

            @Override
            public void onFailure(Call<ApiListResponse<Review>> call, Throwable t) {
                applyReviews(MockReviewCatalog.getReviewsForProduct(currentProduct));
            }
        });
    }

    private void applyReviews(List<Review> reviews) {
        bindReviewsSummary(reviews);
        binding.rvReviews.setLayoutManager(new LinearLayoutManager(ProductDetailActivity.this));
        binding.rvReviews.setAdapter(new ReviewAdapter(reviews));
        binding.btnViewAllReviews.setOnClickListener(v -> {
            binding.rvReviews.setVisibility(View.VISIBLE);
            binding.btnViewAllReviews.setVisibility(View.GONE);
        });
    }

    private void updateFavoriteButton(Product p) {
        binding.ibSave.setSaved(favoritesStore.isFavorite(p.getId()), false);
        binding.ibSave.setOnClickListener(v -> {
            boolean nowSaved = favoritesStore.toggleFavorite(p);
            binding.ibSave.setSaved(nowSaved, true);
            favoritesRepository.syncFavoritesToCloud(); // Đồng bộ lên Cloud ngay khi tim/bỏ tim
        });
    }

    private void bindReviews(List<Review> list) {
        if (list == null) return;
        int total = list.size();
    private void bindReviewsSummary(List<Review> reviews) {
        int total = reviews.size();
        binding.tvReviewsTitle.setText(getString(R.string.reviews_count_format, total));
        binding.tvRatingCount.setText(getString(R.string.reviews_count_short, total));

        int[] stars = new int[6];
        double sum = 0;
        for (Review r : list) {
            int s = Math.round(r.getRating());
            if (s >= 1 && s <= 5) stars[s]++;
            sum += r.getRating();
        }
        double avg = total > 0 ? sum / total : 0;
        binding.tvAvgRating.setText(String.format(Locale.getDefault(), "%.1f", avg));
        binding.rbSummaryRating.setRating((float) avg);

        bindStarBar(binding.rowStar5, 5, stars[5], total);
        bindStarBar(binding.rowStar4, 4, stars[4], total);
        bindStarBar(binding.rowStar3, 3, stars[3], total);
        bindStarBar(binding.rowStar2, 2, stars[2], total);
        bindStarBar(binding.rowStar1, 1, stars[1], total);

        binding.rvReviews.setLayoutManager(new LinearLayoutManager(this));
        binding.rvReviews.setAdapter(new ReviewAdapter(list));
        binding.btnViewAllReviews.setOnClickListener(v -> {
            binding.rvReviews.setVisibility(View.VISIBLE);
            binding.btnViewAllReviews.setVisibility(View.GONE);
        });
    }

    private void bindStarBar(ItemRatingBarRowBinding row, int star, int count, int total) {
        row.tvStarLabel.setText(String.valueOf(star));
        row.tvStarCount.setText(String.valueOf(count));
        int pct = total > 0 ? (count * 100 / total) : 0;
        ((LinearLayout.LayoutParams) row.barFill.getLayoutParams()).weight = pct;
        ((LinearLayout.LayoutParams) row.barSpacer.getLayoutParams()).weight = 100 - pct;
    }

    // ── You may also like — thẻ ảnh phủ kín + chữ đè, cùng khuôn với danh mục (không viền) ──

    private void loadRelatedProducts(Product product) {
        binding.rvRelated.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        RelatedProductAdapter relatedAdapter = new RelatedProductAdapter(new ArrayList<>(), related -> {
            Intent intent = new Intent(ProductDetailActivity.this, ProductDetailActivity.class);
            intent.putExtra(Constants.EXTRA_PRODUCT_ID, related.getId());
            startActivity(intent);
        });
        binding.rvRelated.setAdapter(relatedAdapter);

        productRepository.getProducts(1, 10, product.getCategory(), null).enqueue(new Callback<ApiListResponse<Product>>() {
            @Override
            public void onResponse(Call<ApiListResponse<Product>> call, Response<ApiListResponse<Product>> response) {
                List<Product> source = (response.isSuccessful() && response.body() != null && response.body().isSuccess())
                        ? response.body().getData() : null;
                if (source == null || source.isEmpty()) {
                    source = MockProductCatalog.getProducts(ProductDetailActivity.this, product.getCategory());
                }
                relatedAdapter.updateData(excludeCurrent(source, product.getId()));
            }

    // ── Interaction Logic ───────────────────────────────────────────────────

    private boolean isSizeRequired() {
        return currentProduct != null && currentProduct.getSizes() != null && !currentProduct.getSizes().isEmpty();
    private interface OnRelatedProductClickListener {
        void onClick(Product product);
    }

    private static class RelatedProductAdapter extends androidx.recyclerview.widget.RecyclerView.Adapter<RelatedProductAdapter.ViewHolder> {
        private List<Product> products;
        private final OnRelatedProductClickListener listener;

        RelatedProductAdapter(List<Product> products, OnRelatedProductClickListener listener) {
            this.products = products;
            this.listener = listener;
        }

        void updateData(List<Product> newProducts) {
            this.products = newProducts;
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull android.view.ViewGroup parent, int viewType) {
            return new ViewHolder(com.tiredcity.app.databinding.ItemRelatedProductBinding.inflate(
                    LayoutInflater.from(parent.getContext()), parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            Product product = products.get(position);
            holder.b.tvProductName.setText(product.getName());
            holder.b.tvProductPrice.setText(PriceUtils.format(product.getEffectivePrice()));
            Glide.with(holder.b.ivProductImage.getContext())
                    .load(product.getFirstImage())
                    .timeout(30000)
                    .centerCrop()
                    .placeholder(R.color.bg_subtle)
                    .error(R.color.bg_subtle)
                    .into(holder.b.ivProductImage);
            holder.b.getRoot().setOnClickListener(v -> listener.onClick(product));
        }

        @Override
        public int getItemCount() {
            return products != null ? products.size() : 0;
        }

        static class ViewHolder extends androidx.recyclerview.widget.RecyclerView.ViewHolder {
            final com.tiredcity.app.databinding.ItemRelatedProductBinding b;
            ViewHolder(com.tiredcity.app.databinding.ItemRelatedProductBinding binding) {
                super(binding.getRoot());
                this.b = binding;
            }
        }
    }

    private List<Product> excludeCurrent(List<Product> products, String currentId) {
        List<Product> filtered = new ArrayList<>();
        for (Product p : products) {
            if (!p.getId().equals(currentId)) filtered.add(p);
        }
        return filtered;
    }

    // ── Info rows: shipping / returns / payment ─────────────────────────────

    private void setupInfoRows() {
        bindInfoCard(binding.rowReturns, binding.ivReturnsIcon, binding.tvReturnsText, binding.tvReturnsSubtitle,
                R.drawable.ic_policy_exchange, getString(R.string.label_easy_returns), getString(R.string.label_easy_returns_subtitle));
        bindInfoCard(binding.rowPremium, binding.ivPremiumIcon, binding.tvPremiumText, binding.tvPremiumSubtitle,
                R.drawable.ic_policy_leaf, getString(R.string.label_premium_material), getString(R.string.label_premium_material_subtitle));
        bindInfoCard(binding.rowPayment, binding.ivPaymentIcon, binding.tvPaymentText, binding.tvPaymentSubtitle,
                R.drawable.ic_policy_shield_check, getString(R.string.label_secure_payment), getString(R.string.label_secure_payment_subtitle));
        bindInfoCard(binding.rowShipping, binding.ivShippingIcon, binding.tvShippingText, binding.tvShippingSubtitle,
                R.drawable.ic_policy_fast_delivery, getString(R.string.label_free_shipping), getString(R.string.label_free_shipping_subtitle));
    }

    private void bindInfoCard(View card, ImageView icon, TextView text, TextView subtitle, int iconRes, String label, String sublabel) {
        icon.setImageResource(iconRes);
        text.setText(label);
        subtitle.setText(sublabel);
        card.setOnClickListener(v -> startActivity(new Intent(this, PolicyActivity.class)));
    }

    // ── Bảng size (mục 04) — ảnh tĩnh, giống nhau cho mọi sản phẩm; ảnh động (GIF) cần
    // Glide để phát hoạt hình, setImageResource() chỉ hiển thị khung hình đầu tiên. ──────────

    private void setupSizeGuideImages() {
        Glide.with(this).load(R.drawable.dm_size_guide_chart).into(binding.ivSizeGuideChart);
        Glide.with(this).load(R.raw.dm_size_guide_demo).into(binding.ivSizeGuideDemo);
    }

    // ── Cart ──────────────────────────────────────────────────────────────────

    private void addToCart() {
        if (currentProduct == null) return;

        if (isSizeRequired() && (selectedSize == null || selectedSize.isEmpty())) {
            Toast.makeText(this, "BẮT BUỘC CHỌN SIZE!", Toast.LENGTH_SHORT).show();
            // Scroll to size selector if needed
            binding.nsvRoot.scrollTo(0, binding.llSizes.getTop());
            // Highlight sizes
            binding.llSizes.setBackgroundColor(ContextCompat.getColor(this, R.color.tc_red_pale));
            binding.llSizes.postDelayed(() -> binding.llSizes.setBackgroundColor(0), 1000);
            return;
        }

        cartStore.addItem(new CartItem(currentProduct, quantity, selectedSize, selectedColor));
        Toast.makeText(this, getString(R.string.success_add_cart) + " 🛒", Toast.LENGTH_SHORT).show();
    }

    private void addToCartAndBuyNow() {
        if (currentProduct == null) return;

        if (isSizeRequired() && (selectedSize == null || selectedSize.isEmpty())) {
            Toast.makeText(this, "Vui lòng chọn Size để mua ngay!", Toast.LENGTH_SHORT).show();
            return;
        }

        CartItem item = new CartItem(currentProduct, quantity, selectedSize, selectedColor);
        cartLocalStore.addItem(item);
        item.setSelected(true); // Auto select for checkout
        cartStore.addItem(item);
        startSmoothActivity(new Intent(this, CartActivity.class));
    }

    private void setupSizeSelector() {
        TextView[] views = {binding.tvSizeS, binding.tvSizeM, binding.tvSizeL, binding.tvSizeXl};
        String[] labels = {getString(R.string.size_label_s), getString(R.string.size_label_m), getString(R.string.size_label_l), getString(R.string.size_label_xl)};

        selectedSize = ""; // Bắt buộc chọn, không mặc định S nữa

        for (int i = 0; i < views.length; i++) {
            final int idx = i;
            views[i].setOnClickListener(v -> {
                selectedSize = labels[idx];
                for (TextView tv : views) {
                    boolean isSel = tv.getText().toString().equalsIgnoreCase(selectedSize);
                    tv.setBackgroundResource(isSel ? R.drawable.tc_bg_variant_selected : R.drawable.tc_bg_variant);
                    tv.setTextColor(ContextCompat.getColor(this, isSel ? R.color.white : R.color.text_primary));
                }
            });

            // Reset background for all at start
            views[i].setBackgroundResource(R.drawable.tc_bg_variant);
            views[i].setTextColor(ContextCompat.getColor(this, R.color.text_primary));
        }
    }

    private void setupAccordions() {
        binding.headerDescription.setOnClickListener(v -> toggleAccordion(binding.dividerDescription, binding.contentDescription, binding.ivExpandDescription));
        binding.headerStory.setOnClickListener(v -> toggleAccordion(binding.dividerStory, binding.contentStory, binding.ivExpandStory));
        binding.headerDetails.setOnClickListener(v -> toggleAccordion(binding.dividerDetails, binding.contentDetails, binding.ivExpandDetails));
        binding.headerSizeGuide.setOnClickListener(v -> toggleAccordion(binding.dividerSizeGuide, binding.contentSizeGuide, binding.ivExpandSizeGuide));
        binding.headerCare.setOnClickListener(v -> toggleAccordion(binding.dividerCare, binding.contentCare, binding.ivExpandCare));
    }

    private void toggleAccordion(View div, View cont, View chev) {
        boolean expanded = cont.getVisibility() == View.VISIBLE;
        TransitionManager.beginDelayedTransition(binding.llCardContent, new AutoTransition());
        cont.setVisibility(expanded ? View.GONE : View.VISIBLE);
        div.setVisibility(expanded ? View.VISIBLE : View.GONE);
        chev.animate().rotation(expanded ? 0f : 180f).setDuration(250).start();
    }

    private void setupPolicyCards() {
        binding.rowShipping.setOnClickListener(v -> startSmoothActivity(new Intent(this, PolicyActivity.class)));
        binding.rowReturns.setOnClickListener(v -> startSmoothActivity(new Intent(this, PolicyActivity.class)));
        binding.rowPayment.setOnClickListener(v -> startSmoothActivity(new Intent(v.getContext(), PolicyActivity.class)));
    }

    private void showAddReviewDialog() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_add_review, null);
        android.widget.RatingBar rbInput = dialogView.findViewById(R.id.rb_input);
        com.google.android.material.textfield.TextInputEditText etComment = dialogView.findViewById(R.id.et_comment);
        android.widget.Button btnSubmit = dialogView.findViewById(R.id.btn_submit);
        android.widget.Button btnCancel = dialogView.findViewById(R.id.btn_cancel);

        reviewDialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .setCancelable(false)
                .create();

        btnCancel.setOnClickListener(v -> reviewDialog.dismiss());
        btnSubmit.setOnClickListener(v -> {
            float rating = rbInput.getRating();
            String comment = etComment.getText() != null ? etComment.getText().toString() : "";
            viewModel.submitReview(rating, comment);
        });

        reviewDialog.show();
    }

    private List<Review> buildMockReviews() {
        List<Review> list = new ArrayList<>();
        Review r1 = new Review();
        r1.setUserName("Nguyễn Minh Anh");
        r1.setComment("Sản phẩm rất đẹp, vải lụa mặc rất mát và sang trọng. Giao hàng nhanh!");
        r1.setRating(5);
        r1.setCreatedAt(new java.util.Date());

        Review r2 = new Review();
        r2.setUserName("Trần Thu Hà");
        r2.setComment("Đóng gói cẩn thận, form áo chuẩn như hình. Rất hài lòng.");
        r2.setRating(4);
        r2.setCreatedAt(new java.util.Date());

        list.add(r1); list.add(r2);
        return list;
    }

    private int dp(int val) { return (int) (val * getResources().getDisplayMetrics().density); }

    // ── Inner Adapter for ViewPager2 ────────────────────────────────────────

    private class ImagePagerAdapter extends RecyclerView.Adapter<ImagePagerAdapter.ViewHolder> {
        private final List<String> list;
        ImagePagerAdapter(List<String> list) { this.list = list; }
        @NonNull @Override public ViewHolder onCreateViewHolder(@NonNull ViewGroup p, int vt) {
            ImageView iv = new ImageView(p.getContext());
            iv.setLayoutParams(new ViewGroup.LayoutParams(-1, -1));
            iv.setScaleType(ImageView.ScaleType.CENTER_CROP);
            return new ViewHolder(iv);
        }
        @Override public void onBindViewHolder(@NonNull ViewHolder h, int pos) {
            String imageUrl = list.get(pos);
            Object loadTarget = imageUrl;

            // Hỗ trợ nạp ảnh từ drawable resource (mẫu offline)
            if (imageUrl != null && !imageUrl.startsWith("http") && !imageUrl.startsWith("content")) {
                int resId = getResources().getIdentifier(imageUrl, "drawable", getPackageName());
                if (resId != 0) loadTarget = resId;
            }

            Glide.with(ProductDetailActivity.this)
                    .load(loadTarget)
                    .placeholder(R.color.tc_red_pale)
                    .error(R.color.tc_red_pale)
                    .into((ImageView) h.itemView);
        }
        @Override public int getItemCount() { return list.size(); }
        class ViewHolder extends RecyclerView.ViewHolder { ViewHolder(View v) { super(v); } }
    }
}
