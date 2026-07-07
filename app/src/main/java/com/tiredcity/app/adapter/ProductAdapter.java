package com.tiredcity.app.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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

public class ProductAdapter extends RecyclerView.Adapter<ProductAdapter.ViewHolder> {

    /** Chiều cao ảnh (dp) theo số cột đang hiển thị — thẻ càng hẹp thì ảnh càng thấp. */
    private static final int IMAGE_HEIGHT_DP_1_COL = 300;
    private static final int IMAGE_HEIGHT_DP_2_COL = 200;
    private static final int IMAGE_HEIGHT_DP_3_COL = 150;

    private List<Product>           products;
    private OnProductClickListener  listener;
    private int                     spanCount = 2;

    public interface OnProductClickListener {
        void onProductClick(Product product);
        void onSaveToggle(Product product, boolean saved);
    }

    public ProductAdapter(List<Product> products) {
        this.products = products;
    }

    public void setOnProductClickListener(OnProductClickListener l) {
        this.listener = l;
    }

    /** Số cột đang hiển thị (1/2/3) — quyết định chiều cao ảnh poster full-bleed của thẻ. */
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
    }

    @Override
    public int getItemCount() {
        return products != null ? products.size() : 0;
    }

    private static int imageHeightDp(int spanCount) {
        if (spanCount <= 1) return IMAGE_HEIGHT_DP_1_COL;
        if (spanCount == 2) return IMAGE_HEIGHT_DP_2_COL;
        return IMAGE_HEIGHT_DP_3_COL;
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

        void bind(Product product, OnProductClickListener listener, int spanCount) {
            Context context = itemView.getContext();

            ViewGroup.LayoutParams lp = b.flImage.getLayoutParams();
            lp.height = Math.round(imageHeightDp(spanCount) * context.getResources().getDisplayMetrics().density);
            b.flImage.setLayoutParams(lp);

            b.tvProductName.setText(product.getName());
            b.tvProductMaterial.setText(product.getMaterial() != null ? product.getMaterial() : "");
            b.tvProductPrice.setText(PriceUtils.formatVnd(product.getEffectivePrice()));
            b.rbRating.setRating((float) product.getRating());

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
                Glide.with(b.ivProductImage.getContext())
                        .load(imageUrl)
                        // Ảnh sản phẩm có thể nặng (PNG vài MB) → nới timeout để không bị
                        // huỷ tải giữa chừng (mặc định Glide chỉ 2500ms).
                        .timeout(30000)
                        .centerCrop()
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

            b.getRoot().setOnClickListener(v -> {
                if (listener != null) listener.onProductClick(product);
            });
        }
    }
}
