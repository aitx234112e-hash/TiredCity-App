package com.tiredcity.app.adapter;

import android.content.Context;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.LinearInterpolator;
import android.widget.LinearLayout;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.tiredcity.app.R;
import com.tiredcity.app.data.local.FavoritesLocalStore;
import com.tiredcity.app.data.model.Product;
import com.tiredcity.app.databinding.ItemProductBinding;
import com.tiredcity.app.utils.ColorTaxonomy;
import com.tiredcity.app.utils.PriceUtils;
import java.util.List;

/**
 * Lưới sản phẩm poster: mỗi thẻ là một tấm ảnh bo góc phủ kín, có hiệu ứng "tráng gương"
 * (men bóng tĩnh + vệt sáng chéo quét qua) và khối thông tin chữ trắng đè lên đáy ảnh.
 *
 * Cùng một layout item_product.xml phục vụ cả 3 chế độ 1/2/3 cột — mọi khác biệt về kích thước
 * (chiều cao ảnh, lề thẻ, bán kính góc, cỡ chữ, chi tiết ẩn/hiện) được tính ở đây theo spanCount
 * để thẻ hẹp không bị chữ tràn và thẻ rộng không bị trống trải.
 */
public class ProductAdapter extends RecyclerView.Adapter<ProductAdapter.ViewHolder> {

    /** Tỉ lệ cao/rộng của thẻ theo số cột — cột càng nhiều, ảnh càng cao tương đối để giữ dáng poster. */
    private static final float ASPECT_1_COL = 0.95f;
    private static final float ASPECT_2_COL = 1.30f;
    private static final float ASPECT_3_COL = 1.40f;

    /** Vệt sáng "tráng gương" quét qua ảnh một lần khi thẻ hiện ra. */
    private static final long SHINE_DURATION      = 900L;
    private static final long SHINE_START_DELAY   = 180L;
    private static final long SHINE_STAGGER_STEP  = 90L;
    private static final int  SHINE_STAGGER_MAX   = 6;

    private List<Product>           products;
    private OnProductClickListener  listener;
    private int                     spanCount = 2;

    public interface OnProductClickListener {
        void onProductClick(Product product);
        void onSaveToggle(Product product, boolean saved);
        /** @param sourceView ảnh sản phẩm được bấm — điểm bắt đầu cho hiệu ứng bay vào giỏ. */
        void onAddToCartClick(Product product, View sourceView);
    }

    public ProductAdapter(List<Product> products) {
        this.products = products;
    }

    public void setOnProductClickListener(OnProductClickListener l) {
        this.listener = l;
    }

    /** Số cột đang hiển thị (1/2/3) — quyết định toàn bộ kích thước của thẻ poster. */
    public void setSpanCount(int spanCount) {
        if (this.spanCount == spanCount) return;
        this.spanCount = spanCount;
        notifyDataSetChanged();
    }

    public void updateProducts(List<Product> newProducts) {
        this.products = newProducts;
        notifyDataSetChanged();
    }

    /** Alias used by activities that call adapter.updateData(). */
    public void updateData(List<Product> newProducts) {
        this.products = newProducts;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemProductBinding b = ItemProductBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new ViewHolder(b);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(products.get(position), listener, spanCount);
        holder.playShine(position);
    }

    @Override
    public void onViewRecycled(@NonNull ViewHolder holder) {
        holder.b.shineProductImage.animate().cancel();
        super.onViewRecycled(holder);
    }

    @Override
    public int getItemCount() {
        return products != null ? products.size() : 0;
    }

    // ── Kích thước theo số cột ────────────────────────────────────────────────

    /** Lề quanh thẻ (dp): 1 cột thoáng như trang bìa, 3 cột sát nhau như bảng contact sheet. */
    private static int cardMarginDp(int spanCount) {
        if (spanCount <= 1) return 12;
        if (spanCount == 2) return 8;
        return 5;
    }

    private static int cardRadiusDp(int spanCount) {
        if (spanCount <= 1) return 24;
        if (spanCount == 2) return 20;
        return 14;
    }

    private static float aspect(int spanCount) {
        if (spanCount <= 1) return ASPECT_1_COL;
        if (spanCount == 2) return ASPECT_2_COL;
        return ASPECT_3_COL;
    }

    /** Cỡ chữ tên sản phẩm (sp). */
    private static float nameSizeSp(int spanCount) {
        if (spanCount <= 1) return 19f;
        if (spanCount == 2) return 15f;
        return 11f;
    }

    /** Cỡ chữ giá (sp). */
    private static float priceSizeSp(int spanCount) {
        if (spanCount <= 1) return 20f;
        if (spanCount == 2) return 16f;
        return 11f;
    }

    private static int infoPaddingDp(int spanCount) {
        if (spanCount <= 1) return 16;
        if (spanCount == 2) return 12;
        return 8;
    }

    private static int dp(Context c, float value) {
        return Math.round(TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, value, c.getResources().getDisplayMetrics()));
    }

    // ── ViewHolder ────────────────────────────────────────────────────────────

    static class ViewHolder extends RecyclerView.ViewHolder {

        private final ItemProductBinding b;
        private final FavoritesLocalStore favoritesStore;

        ViewHolder(ItemProductBinding binding) {
            super(binding.getRoot());
            this.b = binding;
            this.favoritesStore = new FavoritesLocalStore(binding.getRoot().getContext());
        }

        /**
         * Vệt sáng chéo lướt từ ngoài-trái sang ngoài-phải thẻ, so le theo vị trí để cả lưới
         * sáng dần như lật một tập ảnh in bóng, thay vì loé lên cùng lúc.
         */
        void playShine(int position) {
            View shine = b.shineProductImage;
            shine.animate().cancel();
            long delay = SHINE_START_DELAY + Math.min(position, SHINE_STAGGER_MAX) * SHINE_STAGGER_STEP;
            shine.post(() -> {
                int width = b.getRoot().getWidth();
                if (width <= 0) return;
                shine.setTranslationX(-width * 0.6f);
                shine.animate()
                        .translationX(width * 1.3f)
                        .setStartDelay(delay)
                        .setDuration(SHINE_DURATION)
                        .setInterpolator(new LinearInterpolator())
                        .start();
            });
        }

        void bind(Product product, OnProductClickListener listener, int spanCount) {
            Context context = itemView.getContext();

            applyLayoutForSpan(context, spanCount);

            b.tvProductName.setText(product.getName());
            b.tvProductPrice.setText(PriceUtils.formatVnd(product.getEffectivePrice()));

            // Chất liệu là chi tiết phụ — thẻ 3 cột quá hẹp để đọc được nên ẩn hẳn.
            String material = product.getMaterial();
            boolean showMaterial = spanCount <= 2 && material != null && !material.isEmpty();
            b.tvProductMaterial.setVisibility(showMaterial ? View.VISIBLE : View.GONE);
            if (showMaterial) b.tvProductMaterial.setText(material);

            // Tag màu (Áo Dài) hoặc loại phụ kiện (Phụ Kiện) — cùng lấy từ product.getColors().
            String colorTag = ColorTaxonomy.primaryTag(product.getColors());
            if (colorTag != null) {
                colorTag = ColorTaxonomy.displayName(context, colorTag);
            } else if (product.getColors() != null && !product.getColors().isEmpty()) {
                colorTag = product.getColors().get(0);
            }
            if (colorTag != null) {
                b.tvColorTag.setVisibility(View.VISIBLE);
                b.tvColorTag.setText(colorTag);
            } else {
                b.tvColorTag.setVisibility(View.GONE);
            }
            // Hàng tag rỗng thì bỏ luôn để không chừa khoảng trắng dưới tên sản phẩm.
            b.llTags.setVisibility(colorTag != null || showMaterial ? View.VISIBLE : View.GONE);

            // Discount badge
            if (product.getDiscount() > 0) {
                b.tvDiscount.setVisibility(View.VISIBLE);
                b.tvDiscount.setText("-" + product.getDiscount() + "%");
            } else {
                b.tvDiscount.setVisibility(View.GONE);
            }

            // Product image via Glide
            String imageUrl = product.getFirstImage();
            if (!imageUrl.isEmpty()) {
                Object loadTarget = imageUrl;
                // Nếu là tên resource (không phải URL), chuyển thành resource ID để Glide nạp offline
                if (!imageUrl.startsWith("http") && !imageUrl.startsWith("content")) {
                    int resId = b.ivProductImage.getContext().getResources().getIdentifier(
                            imageUrl, "drawable", b.ivProductImage.getContext().getPackageName());
                    if (resId != 0) loadTarget = resId;
                }

                Glide.with(b.ivProductImage.getContext())
                        // Ảnh sản phẩm có thể nặng (PNG vài MB) → nới timeout để không bị
                        // huỷ tải giữa chừng (mặc định Glide chỉ 2500ms).
                        .load(loadTarget)
                        .timeout(30000)
                        .centerCrop()
                        .override(600, 750) // Optimize size for grid
                        .thumbnail(0.1f)    // Load small thumbnail first
                        .transition(DrawableTransitionOptions.withCrossFade())
                        .placeholder(R.color.bg_subtle)
                        .error(R.color.bg_subtle)
                        .into(b.ivProductImage);
            } else {
                b.ivProductImage.setBackgroundColor(
                        b.ivProductImage.getContext().getColor(R.color.bg_subtle));
                b.ivProductImage.setImageDrawable(null);
            }

            // Save / wishlist toggle — trạng thái đọc từ FavoritesLocalStore để đồng bộ mọi màn hình
            b.ibSave.setSaved(favoritesStore.isFavorite(product.getId()), false);
            b.ibSave.setOnClickListener(v -> {
                if (product.getId() == null) return;
                boolean nowSaved = favoritesStore.toggleFavorite(product);
                b.ibSave.setSaved(nowSaved, true);
                if (listener != null) listener.onSaveToggle(product, nowSaved);
            });

            // Quick add to cart
            b.btnQuickAdd.setOnClickListener(v -> {
                if (listener != null) listener.onAddToCartClick(product, b.ivProductImage);
            });

            b.getRoot().setOnClickListener(v -> {
                if (listener != null) listener.onProductClick(product);
            });
        }

        /** Ép mọi kích thước của thẻ theo số cột: lề, bo góc, chiều cao ảnh, cỡ chữ, nút phụ. */
        private void applyLayoutForSpan(Context context, int spanCount) {
            int marginPx = dp(context, cardMarginDp(spanCount));

            ViewGroup.LayoutParams rootLp = b.getRoot().getLayoutParams();
            if (rootLp instanceof ViewGroup.MarginLayoutParams) {
                ViewGroup.MarginLayoutParams mlp = (ViewGroup.MarginLayoutParams) rootLp;
                mlp.setMargins(marginPx, marginPx, marginPx, marginPx);
                b.getRoot().setLayoutParams(mlp);
            }
            b.cvProduct.setRadius(dp(context, cardRadiusDp(spanCount)));

            // Chiều cao ảnh suy ra từ bề rộng thực của một cột nên thẻ giữ đúng tỉ lệ trên mọi máy.
            int screenWidthPx = context.getResources().getDisplayMetrics().widthPixels;
            int columnWidthPx = (screenWidthPx - marginPx * 2 * spanCount) / spanCount;
            ViewGroup.LayoutParams imageLp = b.flImage.getLayoutParams();
            imageLp.height = Math.round(columnWidthPx * aspect(spanCount));
            b.flImage.setLayoutParams(imageLp);

            int infoPadPx = dp(context, infoPaddingDp(spanCount));
            b.llInfo.setPadding(infoPadPx, infoPadPx, infoPadPx, infoPadPx);

            b.tvProductName.setTextSize(TypedValue.COMPLEX_UNIT_SP, nameSizeSp(spanCount));
            b.tvProductPrice.setTextSize(TypedValue.COMPLEX_UNIT_SP, priceSizeSp(spanCount));

            // Thẻ 3 cột: bỏ nút thêm-giỏ và gạch chân, chỉ còn tên + tag màu + giá cho đủ chỗ thở.
            boolean compact = spanCount >= 3;
            b.btnQuickAdd.setVisibility(compact ? View.GONE : View.VISIBLE);
            b.vInfoDivider.setVisibility(compact ? View.GONE : View.VISIBLE);
            b.tvDiscount.setTextSize(TypedValue.COMPLEX_UNIT_SP, compact ? 9f : 11f);
            b.tvColorTag.setTextSize(TypedValue.COMPLEX_UNIT_SP, compact ? 9f : 10f);

            // Tên màu có thể rất dài ("Trắng Bạch Ngọc") — ở thẻ 3 cột phải ép chip co theo bề
            // ngang thẻ (weight) để tự cắt bớt, thay vì tràn ra ngoài như khi wrap_content.
            LinearLayout.LayoutParams tagLp = (LinearLayout.LayoutParams) b.tvColorTag.getLayoutParams();
            tagLp.width = compact ? 0 : ViewGroup.LayoutParams.WRAP_CONTENT;
            tagLp.weight = compact ? 1f : 0f;
            b.tvColorTag.setLayoutParams(tagLp);

            int saveSizePx = dp(context, compact ? 26 : 40);
            ViewGroup.LayoutParams saveLp = b.ibSave.getLayoutParams();
            saveLp.width = saveSizePx;
            saveLp.height = saveSizePx;
            b.ibSave.setLayoutParams(saveLp);
        }
    }
}
