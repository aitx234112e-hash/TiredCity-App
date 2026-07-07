package com.tiredcity.app.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.LinearInterpolator;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.tiredcity.app.R;
import com.tiredcity.app.data.model.Product;
import com.tiredcity.app.databinding.ItemProductPhotoCardBinding;
import java.util.List;

/** Thẻ "gợi ý trang phục" cho màn AI Styling (gợi ý theo Ngũ Hành) — ảnh phủ kín cả thẻ, lớp phủ
 *  tối dần đáy, chỉ hiện tên sản phẩm (chữ trắng in hoa) + nút mũi tên tròn góc dưới-phải, giống
 *  kiểu thẻ "poster" dùng chung với item_related_product.xml/item_styling_category.xml. Không tag
 *  màu/giá/sao đánh giá. Mỗi thẻ có vệt sáng chéo "tráng gương" lướt 1 lần khi hiện ra. */
public class ProductPhotoCardAdapter extends RecyclerView.Adapter<ProductPhotoCardAdapter.ViewHolder> {

    private static final long SHIMMER_DURATION = 950L;
    private static final long SHIMMER_DELAY = 160L;
    private static final long SHIMMER_STAGGER_STEP = 45L;
    private static final int SHIMMER_STAGGER_MAX_STEPS = 6;

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
        playShine(holder, position);
    }

    /** Vệt sáng chéo lướt 1 lần từ ngoài-trái sang ngoài-phải ảnh, so le theo vị trí thẻ. */
    private void playShine(ViewHolder holder, int position) {
        View shine = holder.b.shineProductImage;
        shine.animate().cancel();
        long delay = Math.min(position, SHIMMER_STAGGER_MAX_STEPS) * SHIMMER_STAGGER_STEP;
        shine.post(() -> {
            int width = holder.b.getRoot().getWidth();
            if (width <= 0) return;
            shine.setTranslationX(-width * 0.6f);
            shine.animate()
                    .translationX(width * 1.3f)
                    .setStartDelay(delay + SHIMMER_DELAY)
                    .setDuration(SHIMMER_DURATION)
                    .setInterpolator(new LinearInterpolator())
                    .start();
        });
    }

    @Override
    public int getItemCount() {
        return products != null ? products.size() : 0;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private final ItemProductPhotoCardBinding b;

        ViewHolder(ItemProductPhotoCardBinding binding) {
            super(binding.getRoot());
            this.b = binding;
        }

        void bind(Product product, OnProductClickListener listener) {
            b.tvProductName.setText(product.getName());

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

            b.getRoot().setOnClickListener(v -> {
                if (listener != null) listener.onProductClick(product);
            });
        }
    }
}
