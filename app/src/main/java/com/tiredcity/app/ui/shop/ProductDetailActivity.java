package com.tiredcity.app.ui.shop;

import android.animation.Keyframe;
import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.animation.ValueAnimator;
import android.content.Intent;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.bumptech.glide.Glide;
import com.google.android.material.textfield.TextInputEditText;
import com.tiredcity.app.R;
import com.tiredcity.app.adapter.ReviewAdapter;
import com.tiredcity.app.data.local.CartLocalStore;
import com.tiredcity.app.data.local.FavoritesLocalStore;
import com.tiredcity.app.data.local.RecentlyViewedStore;
import com.tiredcity.app.data.mock.MockProductCatalog;
import com.tiredcity.app.data.mock.MockReviewCatalog;
import com.tiredcity.app.data.model.ApiListResponse;
import com.tiredcity.app.data.model.CartItem;
import com.tiredcity.app.data.model.Product;
import com.tiredcity.app.data.model.Review;
import com.tiredcity.app.data.network.ApiClient;
import com.tiredcity.app.data.repository.FavoritesRepository;
import com.tiredcity.app.data.repository.FirestoreProductRepository;
import com.tiredcity.app.data.repository.ProductRepository;
import com.tiredcity.app.databinding.ActivityProductDetailBinding;
import com.tiredcity.app.databinding.ItemHeroCarouselPhotoBinding;
import com.tiredcity.app.databinding.ItemRatingBarRowBinding;
import com.tiredcity.app.databinding.ItemRelatedProductBinding;
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

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * ProductDetailActivity - Senior Architect Optimized (Pure Java).
 * Architecture: MVVM, Centralized Data Handling, Smooth UI & Stacked Card Animations.
 */
public class ProductDetailActivity extends BaseActivity {

    private ActivityProductDetailBinding binding;
    private ProductDetailViewModel viewModel;
    
    private ProductRepository productRepository;
    private FirestoreProductRepository firestoreRepository;
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
            loadReviews(productId);
        } else {
            Toast.makeText(this, R.string.error_product_load_failed, Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void initDependencies() {
        productRepository = new ProductRepository(ApiClient.getApiService(preferenceManager.getToken()));
        firestoreRepository = new FirestoreProductRepository();
        cartStore = new CartLocalStore(this);
        favoritesStore = new FavoritesLocalStore(this);
        recentlyViewedStore = new RecentlyViewedStore(this);
        favoritesRepository = new FavoritesRepository(favoritesStore);

        ProductDetailViewModelFactory factory = new ProductDetailViewModelFactory(productRepository);
        viewModel = new ViewModelProvider(this, factory).get(ProductDetailViewModel.class);
    }

    private void initUI() {
        binding.btnBack.setOnClickListener(v -> finishSmoothly());
        binding.btnAddToCart.setOnClickListener(v -> addToCart());
        binding.btnBuyNow.setOnClickListener(v -> addToCartAndBuyNow());
        binding.btnWriteReview.setOnClickListener(v -> showAddReviewDialog());

        setupSizeSelector();
        setupInfoRows();
        setupCardStackScroll();
        setupSizeGuideImages();
        setupPolicyCards();

        binding.rvRelated.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
    }

    private void observeData() {
        viewModel.getProduct().observe(this, product -> {
            if (product != null) {
                currentProduct = product;
                bindProductToUI(product);
                loadRelatedProducts(product);
            }
        });

        viewModel.getIsLoading().observe(this, isLoading -> {
            // Optional: Show Shimmer or Progress
        });

        viewModel.getErrorMessage().observe(this, error -> {
            if (error != null && !error.isEmpty()) {
                String productId = getIntent().getStringExtra(Constants.EXTRA_PRODUCT_ID);
                fallbackToMockOrFirestore(productId);
            }
        });

        viewModel.getReviews().observe(this, reviews -> {
            if (reviews != null && !reviews.isEmpty()) {
                applyReviews(reviews);
            }
        });

        viewModel.getIsSubmitting().observe(this, isSubmitting -> {
            if (reviewDialog != null && reviewDialog.isShowing()) {
                Button btnSubmit = reviewDialog.findViewById(R.id.btn_submit);
                Button btnCancel = reviewDialog.findViewById(R.id.btn_cancel);
                if (btnSubmit != null) {
                    btnSubmit.setEnabled(!isSubmitting);
                    btnSubmit.setText(isSubmitting ? "Đang gửi..." : "Gửi đánh giá");
                }
                if (btnCancel != null) btnCancel.setEnabled(!isSubmitting);
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
            if (canReview && getIntent().getBooleanExtra(Constants.EXTRA_ACTION_REVIEW, false)) {
                getIntent().removeExtra(Constants.EXTRA_ACTION_REVIEW);
                showAddReviewDialog();
            }
        });
    }

    private void fallbackToMockOrFirestore(String productId) {
        firestoreRepository.getProductById(productId, product -> {
            if (product != null) {
                currentProduct = product;
                bindProductToUI(product);
            } else {
                Product mock = MockProductCatalog.findById(this, productId);
                if (mock != null) {
                    currentProduct = mock;
                    bindProductToUI(mock);
                } else {
                    Toast.makeText(this, R.string.error_product_load_failed, Toast.LENGTH_SHORT).show();
                    finish();
                }
            }
        });
    }

    private void bindProductToUI(Product p) {
        if (recentlyViewedStore != null) recentlyViewedStore.addProduct(p);
        MockProductCatalog.applyGenericDetail(this, p);

        binding.tvProductName.setText(p.getName());
        binding.tvPrice.setText(PriceUtils.format(p.getEffectivePrice()));
        binding.tvMaterial.setText(p.getMaterial() != null ? p.getMaterial() : "");
        binding.tvOrigin.setText(p.getOrigin() != null ? p.getOrigin() : getString(R.string.default_origin));
        binding.tvDescription.setText(p.getDescription());
        binding.tvStory.setText(p.getStory());
        binding.rbRating.setRating((float) p.getRating());
        binding.tvRatingCount.setText(getString(R.string.reviews_count_short, 0)); // Initial placeholder

        bindAvailability(p);
        bindImages(p);
        bindColors(p);
        bindSpecifications(p);
        bindCareInstructions(p);
        updateQuantityStepper(p);
        updateFavoriteButton(p);
    }

    private void bindAvailability(Product p) {
        int stock = p.getStock();
        boolean outOfStock = stock <= 0;
        int dotColor;
        String statusText;

        if (outOfStock) {
            dotColor = ContextCompat.getColor(this, R.color.tc_stroke);
            statusText = getString(R.string.status_out_of_stock);
        } else if (stock <= 5) {
            dotColor = ContextCompat.getColor(this, R.color.tc_gold_deep);
            statusText = getString(R.string.status_low_stock);
        } else {
            dotColor = ContextCompat.getColor(this, R.color.tc_red);
            statusText = getString(R.string.status_in_stock);
        }

        binding.tvAvailability.setText(statusText);
        binding.dotAvailability.setBackgroundTintList(android.content.res.ColorStateList.valueOf(dotColor));

        binding.btnAddToCart.setEnabled(!outOfStock);
        binding.btnBuyNow.setEnabled(!outOfStock);
        binding.btnAddToCart.setAlpha(outOfStock ? 0.5f : 1f);
        binding.btnBuyNow.setAlpha(outOfStock ? 0.5f : 1f);
    }

    private void bindImages(Product product) {
        final List<String> images = product.getImages();
        final boolean hasImage = images != null && !images.isEmpty();

        ViewPager2 pager = binding.vpProductImages;
        pager.setAdapter(new RecyclerView.Adapter<HeroPhotoViewHolder>() {
            @NonNull
            @Override
            public HeroPhotoViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                ItemHeroCarouselPhotoBinding b = ItemHeroCarouselPhotoBinding.inflate(getLayoutInflater(), parent, false);
                return new HeroPhotoViewHolder(b);
            }

            @Override
            public void onBindViewHolder(@NonNull HeroPhotoViewHolder holder, int position) {
                ImageView photo = holder.b.ivPhoto;
                if (hasImage) {
                    Glide.with(photo.getContext())
                            .load(images.get(position))
                            .timeout(30000)
                            .centerCrop()
                            .placeholder(R.color.tc_red_deep)
                            .into(photo);
                } else {
                    photo.setBackgroundColor(ContextCompat.getColor(ProductDetailActivity.this, R.color.tc_red_deep));
                    photo.setImageResource(R.drawable.ic_wardrobe);
                    photo.setColorFilter(ContextCompat.getColor(ProductDetailActivity.this, R.color.tc_on_red));
                    int inset = dp(72);
                    photo.setPadding(inset, inset, inset, inset);
                }
                startShine(holder);
            }

            @Override
            public int getItemCount() {
                return hasImage ? images.size() : 1;
            }
        });

        pager.setOffscreenPageLimit(3);
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

    static class HeroPhotoViewHolder extends RecyclerView.ViewHolder {
        final ItemHeroCarouselPhotoBinding b;
        ObjectAnimator shineAnimator;
        HeroPhotoViewHolder(ItemHeroCarouselPhotoBinding binding) {
            super(binding.getRoot());
            this.b = binding;
        }
    }

    private void startShine(HeroPhotoViewHolder holder) {
        if (holder.shineAnimator != null) holder.shineAnimator.cancel();
        View shine = holder.b.vShine;
        float travel = getResources().getDisplayMetrics().widthPixels * 0.9f;

        Keyframe hold1 = Keyframe.ofFloat(0f, -travel);
        Keyframe holdUntilSweep = Keyframe.ofFloat(0.15f, -travel);
        Keyframe sweepEnd = Keyframe.ofFloat(0.45f, travel);
        Keyframe holdAfterSweep = Keyframe.ofFloat(1f, travel);
        PropertyValuesHolder pvh = PropertyValuesHolder.ofKeyframe("translationX", hold1, holdUntilSweep, sweepEnd, holdAfterSweep);

        ObjectAnimator anim = ObjectAnimator.ofPropertyValuesHolder(shine, pvh);
        anim.setDuration(3200);
        anim.setRepeatCount(ValueAnimator.INFINITE);
        anim.start();
        holder.shineAnimator = anim;
    }

    private void bindColors(Product p) {
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

    private void bindSpecifications(Product p) {
        binding.llSpecifications.removeAllViews();
        Map<String, String> specs = p.getSpecifications();
        if (specs == null) return;
        String materialLabel = getString(R.string.label_material);
        String originLabel = getString(R.string.label_origin);
        for (Map.Entry<String, String> entry : specs.entrySet()) {
            if (entry.getKey().equalsIgnoreCase(materialLabel) || entry.getKey().equalsIgnoreCase(originLabel)) continue;
            binding.llSpecifications.addView(buildSpecRow(entry.getKey(), entry.getValue()));
        }
    }

    private LinearLayout buildSpecRow(String label, String value) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(0, 0, 0, dp(6));

        TextView tvLabel = new TextView(this);
        tvLabel.setLayoutParams(new LinearLayout.LayoutParams(dp(100), -2));
        tvLabel.setText(label);
        tvLabel.setTextColor(ContextCompat.getColor(this, R.color.text_secondary));
        tvLabel.setTextSize(13);

        TextView tvValue = new TextView(this);
        tvValue.setLayoutParams(new LinearLayout.LayoutParams(0, -2, 1f));
        tvValue.setText(value);
        tvValue.setTextColor(ContextCompat.getColor(this, R.color.text_primary));
        tvValue.setTypeface(null, Typeface.BOLD);
        tvValue.setTextSize(13);

        row.addView(tvLabel); row.addView(tvValue);
        return row;
    }

    private void bindCareInstructions(Product p) {
        binding.contentCare.removeAllViews();
        List<String> care = p.getCareInstructions();
        if (care == null) return;
        int[] icons = {R.drawable.ic_care_handwash, R.drawable.ic_care_nobleach, R.drawable.ic_care_dryshade, R.drawable.ic_care_iron};
        for (int i = 0; i < care.size(); i++) {
            binding.contentCare.addView(buildCareItem(icons[i % icons.length], care.get(i)));
        }
    }

    private LinearLayout buildCareItem(int iconRes, String caption) {
        LinearLayout col = new LinearLayout(this);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, -2, 1f);
        lp.setMarginEnd(dp(6));
        col.setLayoutParams(lp);
        col.setOrientation(LinearLayout.VERTICAL);
        col.setGravity(Gravity.CENTER_HORIZONTAL);

        ImageView icon = new ImageView(this);
        icon.setLayoutParams(new LinearLayout.LayoutParams(dp(28), dp(28)));
        icon.setImageResource(iconRes);

        TextView tv = new TextView(this);
        tv.setLayoutParams(new LinearLayout.LayoutParams(-1, -2));
        tv.setPadding(0, dp(6), 0, 0);
        tv.setText(caption);
        tv.setTextColor(ContextCompat.getColor(this, R.color.text_secondary));
        tv.setTextSize(10.5f);
        tv.setGravity(Gravity.CENTER);

        col.addView(icon); col.addView(tv);
        return col;
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

    private void setupCardStackScroll() {
        View[] cards = {binding.cardSection1, binding.cardSection2, binding.cardSection3, binding.cardSection4, binding.cardSection5, binding.cardSection6};
        binding.scrollProductDetail.post(() -> {
            int[] naturalTop = new int[cards.length];
            for (int i = 0; i < cards.length; i++) naturalTop[i] = cards[i].getTop();
            binding.scrollProductDetail.setOnScrollChangeListener((View.OnScrollChangeListener) (v, sx, sy, ox, oy) -> applyStackTransforms(cards, naturalTop, sy));
            applyStackTransforms(cards, naturalTop, binding.scrollProductDetail.getScrollY());
        });
    }

    private void applyStackTransforms(View[] cards, int[] naturalTop, int scrollY) {
        int shrinkWindow = dp(140);
        int pinnableCount = cards.length - 1;
        for (int i = 0; i < cards.length; i++) {
            View card = cards[i];
            if (i >= pinnableCount) {
                card.setTranslationY(0f); card.setScaleX(1f); card.setScaleY(1f); card.setAlpha(1f);
                continue;
            }
            int cappedScrollY = Math.min(scrollY, naturalTop[i + 1]);
            int localScroll = cappedScrollY - naturalTop[i];
            if (localScroll <= 0) {
                card.setTranslationY(0f); card.setScaleX(1f); card.setScaleY(1f); card.setAlpha(1f);
                continue;
            }
            card.setTranslationY(localScroll);
            int distanceToNext = naturalTop[i + 1] - scrollY;
            float progress = 1f - Math.max(0f, Math.min(1f, (float) distanceToNext / shrinkWindow));
            float scale = 1f - (1f - 0.94f) * progress;
            card.setPivotX(card.getWidth() / 2f);
            card.setPivotY(0f);
            card.setScaleX(scale);
            card.setScaleY(scale);
            card.setAlpha(1f - (1f - 0.55f) * progress);
        }
    }

    private void loadReviews(String productId) {
        productRepository.getProductReviews(productId).enqueue(new Callback<ApiListResponse<Review>>() {
            @Override
            public void onResponse(Call<ApiListResponse<Review>> call, Response<ApiListResponse<Review>> response) {
                List<Review> reviews = (response.isSuccessful() && response.body() != null && response.body().isSuccess()) ? response.body().getData() : null;
                if (reviews == null || reviews.isEmpty()) reviews = MockReviewCatalog.getReviewsForProduct(currentProduct);
                applyReviews(reviews);
            }
            @Override public void onFailure(Call<ApiListResponse<Review>> call, Throwable t) {
                applyReviews(MockReviewCatalog.getReviewsForProduct(currentProduct));
            }
        });
    }

    private void applyReviews(List<Review> reviews) {
        bindReviewsSummary(reviews);
        binding.rvReviews.setLayoutManager(new LinearLayoutManager(this));
        binding.rvReviews.setAdapter(new ReviewAdapter(reviews));
        binding.btnViewAllReviews.setOnClickListener(v -> {
            binding.rvReviews.setVisibility(View.VISIBLE);
            binding.btnViewAllReviews.setVisibility(View.GONE);
        });
    }

    private void bindReviewsSummary(List<Review> reviews) {
        int total = reviews.size();
        binding.tvReviewsTitle.setText(getString(R.string.reviews_count_format, total));
        binding.tvRatingCount.setText(getString(R.string.reviews_count_short, total));

        int[] stars = new int[6];
        double sum = 0;
        for (Review r : reviews) {
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
    }

    private void bindStarBar(ItemRatingBarRowBinding row, int star, int count, int total) {
        row.tvStarLabel.setText(String.valueOf(star));
        row.tvStarCount.setText(String.valueOf(count));
        int pct = total > 0 ? (count * 100 / total) : 0;
        ((LinearLayout.LayoutParams) row.barFill.getLayoutParams()).weight = pct;
        ((LinearLayout.LayoutParams) row.barSpacer.getLayoutParams()).weight = 100 - pct;
    }

    private void updateFavoriteButton(Product p) {
        binding.ibSave.setSaved(favoritesStore.isFavorite(p.getId()), false);
        binding.ibSave.setOnClickListener(v -> {
            boolean nowSaved = favoritesStore.toggleFavorite(p);
            binding.ibSave.setSaved(nowSaved, true);
            favoritesRepository.syncFavoritesToCloud();
        });
    }

    private void loadRelatedProducts(Product product) {
        productRepository.getProducts(1, 10, product.getCategory(), null).enqueue(new Callback<ApiListResponse<Product>>() {
            @Override
            public void onResponse(Call<ApiListResponse<Product>> call, Response<ApiListResponse<Product>> response) {
                List<Product> source = (response.isSuccessful() && response.body() != null && response.body().isSuccess()) ? response.body().getData() : null;
                if (source == null || source.isEmpty()) source = MockProductCatalog.getProducts(ProductDetailActivity.this, product.getCategory());
                updateRelatedProductsUI(excludeCurrent(source, product.getId()));
            }
            @Override public void onFailure(Call<ApiListResponse<Product>> call, Throwable t) {
                updateRelatedProductsUI(excludeCurrent(MockProductCatalog.getProducts(ProductDetailActivity.this, product.getCategory()), product.getId()));
            }
        });
    }

    private void updateRelatedProductsUI(List<Product> products) {
        RelatedProductAdapter adapter = new RelatedProductAdapter(products, p -> {
            Intent intent = new Intent(this, ProductDetailActivity.class);
            intent.putExtra(Constants.EXTRA_PRODUCT_ID, p.getId());
            startSmoothActivity(intent);
        });
        binding.rvRelated.setAdapter(adapter);
    }

    private List<Product> excludeCurrent(List<Product> products, String currentId) {
        List<Product> filtered = new ArrayList<>();
        for (Product p : products) if (!p.getId().equals(currentId)) filtered.add(p);
        return filtered;
    }

    private void setupInfoRows() {
        bindInfoCard(binding.rowReturns, binding.ivReturnsIcon, binding.tvReturnsText, binding.tvReturnsSubtitle, R.drawable.ic_policy_exchange, getString(R.string.label_easy_returns), getString(R.string.label_easy_returns_subtitle));
        bindInfoCard(binding.rowPremium, binding.ivPremiumIcon, binding.tvPremiumText, binding.tvPremiumSubtitle, R.drawable.ic_policy_leaf, getString(R.string.label_premium_material), getString(R.string.label_premium_material_subtitle));
        bindInfoCard(binding.rowPayment, binding.ivPaymentIcon, binding.tvPaymentText, binding.tvPaymentSubtitle, R.drawable.ic_policy_shield_check, getString(R.string.label_secure_payment), getString(R.string.label_secure_payment_subtitle));
        bindInfoCard(binding.rowShipping, binding.ivShippingIcon, binding.tvShippingText, binding.tvShippingSubtitle, R.drawable.ic_policy_fast_delivery, getString(R.string.label_free_shipping), getString(R.string.label_free_shipping_subtitle));
    }

    private void bindInfoCard(View card, ImageView icon, TextView text, TextView subtitle, int iconRes, String label, String sublabel) {
        icon.setImageResource(iconRes);
        text.setText(label);
        subtitle.setText(sublabel);
        card.setOnClickListener(v -> startSmoothActivity(new Intent(this, PolicyActivity.class)));
    }

    private void setupSizeGuideImages() {
        Glide.with(this).load(R.drawable.dm_size_guide_chart).into(binding.ivSizeGuideChart);
        Glide.with(this).load(R.raw.dm_size_guide_demo).into(binding.ivSizeGuideDemo);
    }

    private void addToCart() {
        if (currentProduct == null) return;
        if (isSizeRequired() && (selectedSize == null || selectedSize.isEmpty())) {
            Toast.makeText(this, "BẮT BUỘC CHỌN SIZE!", Toast.LENGTH_SHORT).show();
            binding.scrollProductDetail.smoothScrollTo(0, binding.llSizes.getTop());
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
        cartStore.addItem(item);
        item.setSelected(true);
        startSmoothActivity(new Intent(this, CartActivity.class));
    }

    private boolean isSizeRequired() {
        return currentProduct != null && currentProduct.getSizes() != null && !currentProduct.getSizes().isEmpty();
    }

    private void setupSizeSelector() {
        TextView[] views = {binding.tvSizeS, binding.tvSizeM, binding.tvSizeL, binding.tvSizeXl};
        String[] labels = {getString(R.string.size_label_s), getString(R.string.size_label_m), getString(R.string.size_label_l), getString(R.string.size_label_xl)};
        for (int i = 0; i < views.length; i++) {
            final int idx = i;
            views[idx].setOnClickListener(v -> {
                selectedSize = labels[idx];
                for (TextView tv : views) {
                    boolean isSel = tv.getText().toString().equalsIgnoreCase(selectedSize);
                    tv.setBackgroundResource(isSel ? R.drawable.tc_bg_variant_selected : R.drawable.tc_bg_variant);
                    tv.setTextColor(ContextCompat.getColor(this, isSel ? R.color.white : R.color.text_primary));
                }
            });
            views[i].setBackgroundResource(R.drawable.tc_bg_variant);
            views[i].setTextColor(ContextCompat.getColor(this, R.color.text_primary));
        }
    }

    private void setupPolicyCards() {
        binding.rowShipping.setOnClickListener(v -> startSmoothActivity(new Intent(this, PolicyActivity.class)));
        binding.rowReturns.setOnClickListener(v -> startSmoothActivity(new Intent(this, PolicyActivity.class)));
        binding.rowPayment.setOnClickListener(v -> startSmoothActivity(new Intent(this, PolicyActivity.class)));
    }

    private void showAddReviewDialog() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_add_review, null);
        RatingBar rbInput = dialogView.findViewById(R.id.rb_input);
        TextInputEditText etComment = dialogView.findViewById(R.id.et_comment);
        Button btnSubmit = dialogView.findViewById(R.id.btn_submit);
        Button btnCancel = dialogView.findViewById(R.id.btn_cancel);

        reviewDialog = new AlertDialog.Builder(this).setView(dialogView).setCancelable(false).create();
        btnCancel.setOnClickListener(v -> reviewDialog.dismiss());
        btnSubmit.setOnClickListener(v -> {
            float rating = rbInput.getRating();
            String comment = etComment.getText() != null ? etComment.getText().toString() : "";
            viewModel.submitReview(rating, comment);
        });
        reviewDialog.show();
    }

    private int dp(int val) { return (int) (val * getResources().getDisplayMetrics().density); }

    private interface OnRelatedProductClickListener { void onClick(Product product); }

    private static class RelatedProductAdapter extends RecyclerView.Adapter<RelatedProductAdapter.ViewHolder> {
        private final List<Product> products;
        private final OnRelatedProductClickListener listener;

        RelatedProductAdapter(List<Product> products, OnRelatedProductClickListener listener) {
            this.products = products;
            this.listener = listener;
        }

        @NonNull @Override public ViewHolder onCreateViewHolder(@NonNull ViewGroup p, int vt) {
            return new ViewHolder(ItemRelatedProductBinding.inflate(LayoutInflater.from(p.getContext()), p, false));
        }

        @Override public void onBindViewHolder(@NonNull ViewHolder h, int pos) {
            Product p = products.get(pos);
            h.b.tvProductName.setText(p.getName());
            h.b.tvProductPrice.setText(PriceUtils.format(p.getEffectivePrice()));
            Glide.with(h.b.ivProductImage.getContext()).load(p.getFirstImage()).timeout(30000).centerCrop().placeholder(R.color.bg_subtle).into(h.b.ivProductImage);
            h.b.getRoot().setOnClickListener(v -> listener.onClick(p));
        }

        @Override public int getItemCount() { return products != null ? products.size() : 0; }

        static class ViewHolder extends RecyclerView.ViewHolder {
            final ItemRelatedProductBinding b;
            ViewHolder(ItemRelatedProductBinding binding) { super(binding.getRoot()); this.b = binding; }
        }
    }
}
