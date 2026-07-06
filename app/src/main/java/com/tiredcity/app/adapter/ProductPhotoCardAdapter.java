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
import com.tiredcity.app.databinding.ItemProductPhotoCardBinding;
import com.tiredcity.app.utils.ColorTaxonomy;
import com.tiredcity.app.utils.PriceUtils;
import java.util.List;

/** Thẻ "gợi ý trang phục" cho màn AI Styling (gợi ý theo Ngũ Hành) — ảnh không bị cắt (fitCenter)
 *  ở trên, thông tin đầy đủ (tên, chất liệu, màu, giá) trên nền kem bên dưới, cộng huy hiệu lưu
 *  yêu thích hình ngôi sao nền đỏ đè góc ảnh. Tách riêng khỏi ProductAdapter/item_product.xml vì
 *  layout ảnh khác (không cắt cứng 180dp) và không cần rating/2 dòng chữ mô tả như thẻ thường. */
public class ProductPhotoCardAdapter extends RecyclerView.Adapter<ProductPhotoCardAdapter.ViewHolder> {

    public interface OnProductClickListener {
        void onProductClick(Product product);
    }

    private List<Product> products;
    private OnProductClickListener listener;

    public ProductPhotoCardAdapter(List<Product> products) {
        this.products = products;
    }

    public void setOnProductClickListener(OnProductClickListener l) {
        this.listener = l;
    }

    public void updateData(List<Product> newProducts) {
        this.products = newProducts;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemProductPhotoCardBinding b = ItemProductPhotoCardBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
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

    static class ViewHolder extends RecyclerView.ViewHolder {
        private final ItemProductPhotoCardBinding b;
        private final FavoritesLocalStore favoritesStore;

        ViewHolder(ItemProductPhotoCardBinding binding) {
            super(binding.getRoot());
            this.b = binding;
            this.favoritesStore = new FavoritesLocalStore(binding.getRoot().getContext());
        }

        void bind(Product product, OnProductClickListener listener) {
            b.tvProductName.setText(product.getName());
            b.tvProductMaterial.setText(product.getMaterial() != null ? product.getMaterial() : "");
            b.tvProductPrice.setText(PriceUtils.formatVnd(product.getEffectivePrice()));

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

            if (product.getDiscount() > 0) {
                b.tvDiscount.setVisibility(View.VISIBLE);
                b.tvDiscount.setText("-" + product.getDiscount() + "%");
            } else {
                b.tvDiscount.setVisibility(View.GONE);
            }

            String imageUrl = product.getFirstImage();
            if (!imageUrl.isEmpty()) {
                Glide.with(b.ivProductImage.getContext())
                        .load(imageUrl)
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

            b.ibSave.setSaved(favoritesStore.isFavorite(product.getId()), false);
            b.ibSave.setOnClickListener(v -> {
                if (product.getId() == null) return;
                boolean nowSaved = favoritesStore.toggleFavorite(product);
                b.ibSave.setSaved(nowSaved, true);
            });

            b.getRoot().setOnClickListener(v -> {
                if (listener != null) listener.onProductClick(product);
            });
        }
    }
}
