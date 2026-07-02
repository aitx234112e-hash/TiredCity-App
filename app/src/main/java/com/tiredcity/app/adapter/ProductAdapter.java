package com.tiredcity.app.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.tiredcity.app.R;
import com.tiredcity.app.data.model.Product;
import com.tiredcity.app.databinding.ItemProductBinding;
import com.tiredcity.app.utils.ColorTaxonomy;
import com.tiredcity.app.utils.PriceUtils;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ProductAdapter extends RecyclerView.Adapter<ProductAdapter.ViewHolder> {

    private List<Product>           products;
    private final Set<String>       savedIds = new HashSet<>();
    private OnProductClickListener  listener;
    private boolean                 fillWidth = false;

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

    public void setSavedIds(Set<String> ids) {
        savedIds.clear();
        savedIds.addAll(ids);
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
        holder.bind(products.get(position), savedIds, listener);
    }

    @Override
    public int getItemCount() {
        return products != null ? products.size() : 0;
    }

    // ── ViewHolder ────────────────────────────────────────────────────────────

    static class ViewHolder extends RecyclerView.ViewHolder {

        private final ItemProductBinding b;

        ViewHolder(ItemProductBinding binding) {
            super(binding.getRoot());
            this.b = binding;
        }

        void bind(Product product, Set<String> savedIds, OnProductClickListener listener) {
            b.tvProductName.setText(product.getName());
            b.tvProductMaterial.setText(product.getMaterial() != null ? product.getMaterial() : "");
            b.tvProductPrice.setText(PriceUtils.formatVnd(product.getEffectivePrice()));
            b.rbRating.setRating((float) product.getRating());

            // Tag màu (Áo Dài) hoặc loại phụ kiện (Phụ Kiện) — cùng lấy từ product.getColors().
            String colorTag = ColorTaxonomy.primaryTag(product.getColors());
            if (colorTag == null && product.getColors() != null && !product.getColors().isEmpty()) {
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

            // Save / wishlist toggle
            boolean saved = savedIds.contains(product.getId());
            b.ibSave.setImageResource(saved
                    ? android.R.drawable.btn_star_big_on
                    : android.R.drawable.btn_star_big_off);
            b.ibSave.setOnClickListener(v -> {
                if (product.getId() == null) return;
                boolean nowSaved = !savedIds.contains(product.getId());
                if (nowSaved) savedIds.add(product.getId());
                else          savedIds.remove(product.getId());
                b.ibSave.setImageResource(nowSaved
                        ? android.R.drawable.btn_star_big_on
                        : android.R.drawable.btn_star_big_off);
                if (listener != null) listener.onSaveToggle(product, nowSaved);
            });

            b.getRoot().setOnClickListener(v -> {
                if (listener != null) listener.onProductClick(product);
            });
        }
    }
}
