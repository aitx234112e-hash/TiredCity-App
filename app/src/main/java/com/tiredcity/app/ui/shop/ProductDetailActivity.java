package com.tiredcity.app.ui.shop;

import android.content.Intent;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
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
import com.tiredcity.app.adapter.ProductAdapter;
import com.tiredcity.app.adapter.ReviewAdapter;
import com.tiredcity.app.data.local.CartLocalStore;
import com.tiredcity.app.data.local.FavoritesLocalStore;
import com.tiredcity.app.data.model.CartItem;
import com.tiredcity.app.data.model.Product;
import com.tiredcity.app.data.model.Review;
import com.tiredcity.app.data.network.ApiClient;
import com.tiredcity.app.data.repository.FavoritesRepository;
import com.tiredcity.app.data.repository.ProductRepository;
import com.tiredcity.app.databinding.ActivityProductDetailBinding;
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
    private ProductDetailViewModel viewModel;
    private CartLocalStore cartStore;
    private FavoritesLocalStore favoritesStore;
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
        });

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

        binding.btnAddToCart.setEnabled(!out);
        binding.btnBuyNow.setEnabled(!out);
        binding.btnAddToCart.setAlpha(out ? 0.5f : 1.0f);
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

    private void updateColorSwatches(Product p) {
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

    // ── Interaction Logic ───────────────────────────────────────────────────

    private boolean isSizeRequired() {
        return currentProduct != null && currentProduct.getSizes() != null && !currentProduct.getSizes().isEmpty();
    }

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
