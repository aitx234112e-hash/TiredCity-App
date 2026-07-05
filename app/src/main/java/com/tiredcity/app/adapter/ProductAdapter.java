package com.tiredcity.app.adapter;

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

    private List<Product>           products;
    private OnProductClickListener  listener;
    private boolean                 fillWidth = false;

    public interface OnProductClickListener {
        void onProductClick(Product product);
        void onSaveToggle(Product product, boolean saved);
        void onAddToCartClick(Product product);
    }

    public ProductAdapter(List<Product> products) {
        this.products = products;
    }

    public void setOnProductClickListener(OnProductClickListener l) {
        this.listener = l;
    }

    /**
     * Khi bật, thẻ sản phẩm sẽ giãn đầy ô lưới (MATCH_PARENT) thay vì giữ bề rộng
     * cố định trong XML — dùng cho lưới đổi 1/2/3 cột (CategoryActivity).
     */
    public void setFillWidth(boolean fill) {
        this.fillWidth = fill;
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
        if (fillWidth) {
            android.view.ViewGroup.LayoutParams lp = b.getRoot().getLayoutParams();
            if (lp != null) {
                lp.width = android.view.ViewGroup.LayoutParams.MATCH_PARENT;
                b.getRoot().setLayoutParams(lp);
            }
        }
        return new ViewHolder(b);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(products.get(position), listener);
    }

    @Override
    public int getItemCount() {
        return products != null ? products.size() : 0;
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

        void bind(Product product, OnProductClickListener listener) {
            b.tvProductName.setText(product.getName());
            b.tvProductMaterial.setText(product.getMaterial() != null ? product.getMaterial() : "");
            b.tvProductPrice.setText(PriceUtils.formatVnd(product.getEffectivePrice()));
            b.rbRating.setRating((float) product.getRating());

            // Tag màu (Áo Dài) hoặc loại phụ kiện (Phụ Kiện) — cùng lấy từ product.getColors().
            String colorTag = ColorTaxonomy.primaryTag(product.getColors());
            if (colorTag != null) {
                colorTag = ColorTaxonomy.displayName(itemView.getContext(), colorTag);
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
                Object loadTarget = imageUrl;
                // Nếu là tên resource (không phải URL), chuyển thành resource ID để Glide nạp offline
                if (!imageUrl.startsWith("http") && !imageUrl.startsWith("content")) {
                    int resId = b.ivProductImage.getContext().getResources().getIdentifier(
                            imageUrl, "drawable", b.ivProductImage.getContext().getPackageName());
                    if (resId != 0) loadTarget = resId;
                }

                Glide.with(b.ivProductImage.getContext())
                        .load(loadTarget)
                        .centerCrop()
                        .override(400, 500) // Optimize size for grid
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
                if (listener != null) listener.onAddToCartClick(product);
            });

            b.getRoot().setOnClickListener(v -> {
                if (listener != null) listener.onProductClick(product);
            });
        }
    }
}
