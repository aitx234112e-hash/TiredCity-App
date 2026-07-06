package com.tiredcity.app.ui.shop;

import android.content.Intent;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.bumptech.glide.Glide;
import com.tiredcity.app.R;
import com.tiredcity.app.adapter.ProductAdapter;
import com.tiredcity.app.adapter.ReviewAdapter;
import com.tiredcity.app.data.local.CartLocalStore;
import com.tiredcity.app.data.local.FavoritesLocalStore;
import com.tiredcity.app.data.local.RecentlyViewedStore;
import com.tiredcity.app.data.mock.MockProductCatalog;
import com.tiredcity.app.data.model.ApiListResponse;
import com.tiredcity.app.data.model.ApiResponse;
import com.tiredcity.app.data.model.CartItem;
import com.tiredcity.app.data.model.Product;
import com.tiredcity.app.data.model.Review;
import com.tiredcity.app.data.network.ApiClient;
import com.tiredcity.app.data.repository.FirestoreProductRepository;
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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ProductDetailActivity extends BaseActivity {

    public static final String EXTRA_PRODUCT_ID = Constants.EXTRA_PRODUCT_ID;

    private String[] sizeLabels;

    private ActivityProductDetailBinding binding;
    private ProductRepository productRepository;
    private FirestoreProductRepository firestoreRepository;
    private CartLocalStore cartLocalStore;
    private FavoritesLocalStore favoritesStore;
    private RecentlyViewedStore recentlyViewedStore;
    private Product currentProduct;

    private String selectedColor;
    private String selectedSize;
    private int quantity = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityProductDetailBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        sizeLabels = new String[]{
                getString(R.string.size_label_s),
                getString(R.string.size_label_m),
                getString(R.string.size_label_l),
                getString(R.string.size_label_xl)
        };
        selectedSize = sizeLabels[0];

        // Back button (layout uses ImageButton, not Toolbar)
        binding.btnBack.setOnClickListener(v -> finish());

        String productId = getIntent().getStringExtra(EXTRA_PRODUCT_ID);
        productRepository = new ProductRepository(ApiClient.getApiService(preferenceManager.getToken()));
        cartLocalStore     = new CartLocalStore(this);
        favoritesStore     = new FavoritesLocalStore(this);
        recentlyViewedStore = new RecentlyViewedStore(this);

        binding.btnAddToCart.setOnClickListener(v -> addToCart());
        binding.btnBuyNow.setOnClickListener(v -> {
            addToCart();
            openCart();
        });

        setupSizeSelector();
        setupInfoRows();
        setupCardStackScroll();

        loadProduct(productId);
    }

    private void loadProduct(String productId) {
        if (productId == null) { finish(); return; }
        productRepository.getProductById(productId).enqueue(new Callback<ApiResponse<Product>>() {
            @Override
            public void onResponse(Call<ApiResponse<Product>> call, Response<ApiResponse<Product>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    currentProduct = response.body().getData();
                    bindProduct(currentProduct);
                } else {
                    // Backend không nhận diện được id (ví dụ id của dữ liệu mẫu khi offline) —
                    // thử tra trong catalogue mẫu trước khi bỏ cuộc, tránh đóng màn hình đột ngột.
                    fallbackToMockOrFinish(productId);
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<Product>> call, Throwable t) {
                fallbackToMockOrFinish(productId);
            }
        });
    }

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
    }

    private void bindProduct(Product product) {
        // Ghi nhận đây là sản phẩm khách thật sự đã xem → hiển thị lại ở "Đã xem gần đây".
        if (recentlyViewedStore != null) recentlyViewedStore.addProduct(product);

        binding.tvProductName.setText(product.getName());
        binding.tvPrice.setText(PriceUtils.format(product.getEffectivePrice()));
        binding.tvMaterial.setText(product.getMaterial() != null ? product.getMaterial() : "");
        binding.tvOrigin.setText(product.getOrigin() != null ? product.getOrigin() : getString(R.string.default_origin));
        binding.tvDescription.setText(product.getDescription() != null ? product.getDescription() : "");
        binding.tvStory.setText(product.getStory() != null ? product.getStory() : "");
        binding.rbRating.setRating((float) product.getRating());

        bindAvailability(product);
        bindImages(product);
        bindColors(product);
        bindSpecifications(product);
        bindCareInstructions(product);
        bindQuantityStepper(product);
        bindFavoriteButton(product);
        loadReviews(product.getId());
        loadRelatedProducts(product);
    }

    // ── Favorite (yêu thích) ─────────────────────────────────────────────────

    private void bindFavoriteButton(Product product) {
        binding.ibSave.setSaved(favoritesStore.isFavorite(product.getId()), false);
        binding.ibSave.setOnClickListener(v -> {
            boolean nowSaved = favoritesStore.toggleFavorite(product);
            binding.ibSave.setSaved(nowSaved, true);
        });
    }

    // ── Availability ─────────────────────────────────────────────────────────

    private void bindAvailability(Product product) {
        int stock = product.getStock();
        boolean outOfStock = stock <= 0;
        int statusColor;
        String statusText;
        if (outOfStock) {
            statusColor = getColor(R.color.tc_red);
            statusText = getString(R.string.status_out_of_stock);
        } else if (stock <= 5) {
            statusColor = getColor(R.color.tc_gold_deep);
            statusText = getString(R.string.status_low_stock);
        } else {
            statusColor = getColor(R.color.tc_success);
            statusText = getString(R.string.status_in_stock);
        }
        binding.tvAvailability.setText("●  " + statusText);
        binding.tvAvailability.setTextColor(statusColor);

        GradientDrawable pillBg = new GradientDrawable();
        pillBg.setShape(GradientDrawable.RECTANGLE);
        pillBg.setCornerRadius(dp(20));
        pillBg.setColor((statusColor & 0x00FFFFFF) | 0x22000000);
        binding.tvAvailability.setBackground(pillBg);

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

    // ── Hero images ──────────────────────────────────────────────────────────

    private void bindImages(Product product) {
        final List<String> images = product.getImages();
        final boolean hasImage = images != null && !images.isEmpty();

        binding.vpProductImages.setAdapter(new androidx.recyclerview.widget.RecyclerView.Adapter<androidx.recyclerview.widget.RecyclerView.ViewHolder>() {
            @androidx.annotation.NonNull
            @Override
            public androidx.recyclerview.widget.RecyclerView.ViewHolder onCreateViewHolder(
                    @androidx.annotation.NonNull android.view.ViewGroup parent, int viewType) {
                android.widget.ImageView iv = new android.widget.ImageView(parent.getContext());
                iv.setLayoutParams(new android.view.ViewGroup.LayoutParams(
                    android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                    android.view.ViewGroup.LayoutParams.MATCH_PARENT));
                iv.setScaleType(hasImage
                        ? android.widget.ImageView.ScaleType.CENTER_CROP
                        : android.widget.ImageView.ScaleType.CENTER_INSIDE);
                return new androidx.recyclerview.widget.RecyclerView.ViewHolder(iv) {};
            }

            @Override
            public void onBindViewHolder(@androidx.annotation.NonNull androidx.recyclerview.widget.RecyclerView.ViewHolder holder, int position) {
                android.widget.ImageView iv = (android.widget.ImageView) holder.itemView;
                if (hasImage) {
                    // Load ĐÚNG ảnh theo vị trí (trước đây luôn load ảnh đầu → các trang bị lặp).
                    Glide.with(iv.getContext())
                        .load(images.get(position))
                        .timeout(30000)
                        .centerCrop()
                        .placeholder(R.color.tc_red_deep)
                        .into(iv);
                } else {
                    // Sản phẩm mẫu/offline chưa có ảnh thật — hiển thị hình minh hoạ thay vì khung trống.
                    iv.setBackgroundColor(getColor(R.color.tc_red_deep));
                    iv.setImageResource(R.drawable.ic_wardrobe);
                    iv.setColorFilter(getColor(R.color.tc_on_red));
                    int inset = dp(72);
                    iv.setPadding(inset, inset, inset, inset);
                }
            }

            @Override
            public int getItemCount() {
                return hasImage ? images.size() : 1;
            }
        });
    }

    // ── Colour swatches ──────────────────────────────────────────────────────

    private void bindColors(Product product) {
        binding.llColors.removeAllViews();

        LinkedHashSet<String> buckets = new LinkedHashSet<>();
        if (product.getColors() != null) {
            for (String raw : product.getColors()) {
                buckets.addAll(ColorTaxonomy.normalize(raw));
            }
        }
        if (buckets.isEmpty()) {
            binding.llColors.setVisibility(View.GONE);
            return;
        }
        binding.llColors.setVisibility(View.VISIBLE);

        int dotSize = dp(26);
        int frameSize = dp(36);
        int margin = dp(10);
        List<View> rings = new ArrayList<>();
        List<String> bucketList = new ArrayList<>(buckets);
        selectedColor = bucketList.get(0);

        android.util.TypedValue rippleAttr = new android.util.TypedValue();
        getTheme().resolveAttribute(android.R.attr.selectableItemBackgroundBorderless, rippleAttr, true);

        for (String bucket : bucketList) {
            FrameLayout frame = new FrameLayout(this);
            LinearLayout.LayoutParams frameParams = new LinearLayout.LayoutParams(frameSize, frameSize);
            frameParams.setMarginEnd(margin);
            frame.setLayoutParams(frameParams);
            frame.setForeground(androidx.core.content.ContextCompat.getDrawable(this, rippleAttr.resourceId));

            View ring = new View(this);
            ring.setLayoutParams(new FrameLayout.LayoutParams(frameSize, frameSize));
            ring.setBackgroundResource(R.drawable.tc_bg_circle_ring_selected);
            ring.setVisibility(bucket.equals(selectedColor) ? View.VISIBLE : View.INVISIBLE);

            View dot = new View(this);
            FrameLayout.LayoutParams dotParams = new FrameLayout.LayoutParams(dotSize, dotSize);
            dotParams.gravity = android.view.Gravity.CENTER;
            dot.setLayoutParams(dotParams);
            GradientDrawable dotBg = new GradientDrawable();
            dotBg.setShape(GradientDrawable.OVAL);
            dotBg.setColor(getColor(swatchColorRes(bucket)));
            dotBg.setStroke(dp(1), getColor(R.color.tc_stroke));
            dot.setBackground(dotBg);

            frame.addView(dot);
            frame.addView(ring);
            rings.add(ring);

            frame.setOnClickListener(v -> {
                selectedColor = bucket;
                for (int i = 0; i < rings.size(); i++) {
                    View r = rings.get(i);
                    boolean isSelected = bucketList.get(i).equals(bucket);
                    r.animate().cancel();
                    if (isSelected) {
                        r.setAlpha(0f);
                        r.setVisibility(View.VISIBLE);
                        r.animate().alpha(1f).setDuration(160).start();
                    } else if (r.getVisibility() == View.VISIBLE) {
                        r.animate().alpha(0f).setDuration(160)
                                .withEndAction(() -> r.setVisibility(View.INVISIBLE)).start();
                    }
                }
            });

            binding.llColors.addView(frame);
        }
    }

    @androidx.annotation.ColorRes
    private int swatchColorRes(String bucket) {
        if (ColorTaxonomy.DO.equals(bucket))      return R.color.tc_swatch_do;
        if (ColorTaxonomy.XANH.equals(bucket))    return R.color.tc_swatch_xanh;
        if (ColorTaxonomy.VANG.equals(bucket))    return R.color.tc_swatch_vang;
        if (ColorTaxonomy.TRANG.equals(bucket))   return R.color.tc_swatch_trang;
        if (ColorTaxonomy.DEN.equals(bucket))     return R.color.tc_swatch_den;
        if (ColorTaxonomy.HONG.equals(bucket))    return R.color.tc_swatch_hong;
        if (ColorTaxonomy.TIM.equals(bucket))     return R.color.tc_swatch_tim;
        if (ColorTaxonomy.XANH_LA.equals(bucket)) return R.color.tc_swatch_xanh_la;
        if (ColorTaxonomy.CAM.equals(bucket))     return R.color.tc_swatch_cam;
        return R.color.tc_bg_subtle;
    }

    // ── Size selector ────────────────────────────────────────────────────────

    private void setupSizeSelector() {
        TextView[] sizeViews = {binding.tvSizeS, binding.tvSizeM, binding.tvSizeL, binding.tvSizeXl};
        for (int i = 0; i < sizeViews.length; i++) {
            String label = sizeLabels[i];
            sizeViews[i].setOnClickListener(v -> selectSize(label, sizeViews));
        }
        selectSize(selectedSize, sizeViews);
    }

    private void selectSize(String size, TextView[] sizeViews) {
        selectedSize = size;
        for (TextView v : sizeViews) {
            boolean selected = v.getText().toString().equalsIgnoreCase(size);
            v.setBackgroundResource(selected ? R.drawable.tc_bg_variant_selected : R.drawable.tc_bg_variant);
            v.setTextColor(getColor(selected ? R.color.white : R.color.text_primary));
        }
    }

    // ── Quantity stepper ─────────────────────────────────────────────────────

    private void bindQuantityStepper(Product product) {
        quantity = 1;
        binding.tvQuantity.setText(String.valueOf(quantity));
        int maxQty = product.getStock() > 0 ? product.getStock() : Integer.MAX_VALUE;

        binding.btnQtyMinus.setOnClickListener(v -> {
            if (quantity > 1) {
                quantity--;
                binding.tvQuantity.setText(String.valueOf(quantity));
            }
        });
        binding.btnQtyPlus.setOnClickListener(v -> {
            if (quantity < maxQty) {
                quantity++;
                binding.tvQuantity.setText(String.valueOf(quantity));
            } else {
                Toast.makeText(this, R.string.out_of_stock, Toast.LENGTH_SHORT).show();
            }
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

            int localScroll = scrollY - naturalTop[i];

            // Chưa tới điểm ghim: cuộn bình thường, không biến đổi.
            if (localScroll <= 0) {
                card.setTranslationY(0f);
                card.setScaleX(1f);
                card.setScaleY(1f);
                card.setAlpha(1f);
                continue;
            }

            // Ghim thẻ tại đỉnh khung nhìn cho tới khi thẻ kế tiếp cuộn trùm hẳn lên.
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
                        ? response.body().getData() : new ArrayList<>();
                bindReviewsSummary(reviews);

                binding.rvReviews.setLayoutManager(new LinearLayoutManager(ProductDetailActivity.this));
                binding.rvReviews.setAdapter(new ReviewAdapter(reviews));
                binding.btnViewAllReviews.setOnClickListener(v -> {
                    binding.rvReviews.setVisibility(View.VISIBLE);
                    binding.btnViewAllReviews.setVisibility(View.GONE);
                });
            }

            @Override
            public void onFailure(Call<ApiListResponse<Review>> call, Throwable t) {
                bindReviewsSummary(new ArrayList<>());
            }
        });
    }

    private void bindReviewsSummary(List<Review> reviews) {
        int total = reviews.size();
        binding.tvReviewsTitle.setText(getString(R.string.reviews_count_format, total));
        binding.tvRatingCount.setText(getString(R.string.reviews_count_short, total));

        int[] starCounts = new int[6];
        double sum = 0;
        for (Review r : reviews) {
            int star = Math.round(r.getRating());
            if (star >= 1 && star <= 5) starCounts[star]++;
            sum += r.getRating();
        }
        double avg = total > 0 ? sum / total : 0;
        binding.tvAvgRating.setText(String.format(Locale.getDefault(), "%.1f", avg));
        binding.rbSummaryRating.setRating((float) avg);

        bindStarRow(binding.rowStar5, 5, starCounts[5], total);
        bindStarRow(binding.rowStar4, 4, starCounts[4], total);
        bindStarRow(binding.rowStar3, 3, starCounts[3], total);
        bindStarRow(binding.rowStar2, 2, starCounts[2], total);
        bindStarRow(binding.rowStar1, 1, starCounts[1], total);
    }

    private void bindStarRow(ItemRatingBarRowBinding row, int star, int count, int total) {
        row.tvStarLabel.setText(String.valueOf(star));
        row.tvStarCount.setText(String.valueOf(count));

        int percent = total > 0 ? Math.round(count * 100f / total) : 0;
        LinearLayout.LayoutParams fillParams = (LinearLayout.LayoutParams) row.barFill.getLayoutParams();
        fillParams.weight = percent;
        row.barFill.setLayoutParams(fillParams);

        LinearLayout.LayoutParams spacerParams = (LinearLayout.LayoutParams) row.barSpacer.getLayoutParams();
        spacerParams.weight = 100 - percent;
        row.barSpacer.setLayoutParams(spacerParams);
    }

    // ── You may also like ────────────────────────────────────────────────────

    private void loadRelatedProducts(Product product) {
        binding.rvRelated.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        ProductAdapter relatedAdapter = new ProductAdapter(new ArrayList<>());
        relatedAdapter.setOnProductClickListener(new ProductAdapter.OnProductClickListener() {
            @Override
            public void onProductClick(Product related) {
                Intent intent = new Intent(ProductDetailActivity.this, ProductDetailActivity.class);
                intent.putExtra(Constants.EXTRA_PRODUCT_ID, related.getId());
                startActivity(intent);
            }

            @Override
            public void onSaveToggle(Product related, boolean saved) { /* handle wishlist */ }
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

            @Override
            public void onFailure(Call<ApiListResponse<Product>> call, Throwable t) {
                relatedAdapter.updateData(excludeCurrent(
                        MockProductCatalog.getProducts(ProductDetailActivity.this, product.getCategory()), product.getId()));
            }
        });
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
        bindInfoCard(binding.rowShipping, binding.ivShippingIcon, binding.tvShippingText,
                R.drawable.ic_policy_shipping, getString(R.string.label_free_shipping));
        bindInfoCard(binding.rowReturns, binding.ivReturnsIcon, binding.tvReturnsText,
                R.drawable.ic_policy_return, getString(R.string.label_easy_returns));
        bindInfoCard(binding.rowPayment, binding.ivPaymentIcon, binding.tvPaymentText,
                R.drawable.ic_policy_payment, getString(R.string.label_secure_payment));
    }

    private void bindInfoCard(View card, ImageView icon, TextView text, int iconRes, String label) {
        icon.setImageResource(iconRes);
        text.setText(label);
        card.setOnClickListener(v -> startActivity(new Intent(this, PolicyActivity.class)));
    }

    // ── Cart ──────────────────────────────────────────────────────────────────

    private void addToCart() {
        if (currentProduct == null) return;
        CartItem item = new CartItem(currentProduct, quantity, selectedSize, selectedColor);
        cartLocalStore.addItem(item);
    }

    private void openCart() {
        startActivity(new Intent(this, CartActivity.class));
    }

    private int dp(int value) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round(value * density);
    }
}
