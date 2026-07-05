package com.tiredcity.app.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.google.android.material.imageview.ShapeableImageView;
import com.tiredcity.app.R;
import com.tiredcity.app.data.model.Product;
import com.tiredcity.app.utils.PriceUtils;
import java.util.List;

/**
 * Carousel "Sản phẩm nổi bật": mỗi trang là 1 sản phẩm dạng poster full-bleed. Dùng chung với
 * ViewPager2 + PageTransformer (xem HomeFragment.CenterScalePageTransformer) để phóng to thẻ
 * đang ở giữa và thu nhỏ 2 thẻ bên cạnh.
 */
public class HotProductCarouselAdapter
        extends RecyclerView.Adapter<HotProductCarouselAdapter.ViewHolder> {

    public interface OnProductClickListener {
        void onProductClick(Product product);
    }

    private List<Product> products;
    private OnProductClickListener listener;

    public HotProductCarouselAdapter(List<Product> products) {
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
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_hot_product_carousel, parent, false);
        return new ViewHolder(view);
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
        final ShapeableImageView ivImage;
        final TextView tvName;
        final TextView tvPrice;
        final TextView tvDiscount;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivImage = itemView.findViewById(R.id.iv_hot_product_image);
            tvName = itemView.findViewById(R.id.tv_hot_product_name);
            tvPrice = itemView.findViewById(R.id.tv_hot_product_price);
            tvDiscount = itemView.findViewById(R.id.tv_hot_product_discount);
        }

        void bind(Product product, OnProductClickListener listener) {
            tvName.setText(product.getName());
            tvPrice.setText(PriceUtils.formatVnd(product.getEffectivePrice()));

            if (product.getDiscount() > 0) {
                tvDiscount.setVisibility(View.VISIBLE);
                tvDiscount.setText("-" + product.getDiscount() + "%");
            } else {
                tvDiscount.setVisibility(View.GONE);
            }

            String imageUrl = product.getFirstImage();
            if (!imageUrl.isEmpty()) {
                Glide.with(ivImage.getContext())
                        .load(imageUrl)
                        // Ảnh sản phẩm có thể nặng — nới timeout để không bị huỷ tải giữa chừng.
                        .timeout(30000)
                        .centerCrop()
                        .transition(DrawableTransitionOptions.withCrossFade())
                        .placeholder(R.color.bg_subtle)
                        .error(R.color.bg_subtle)
                        .into(ivImage);
            } else {
                ivImage.setImageDrawable(null);
                ivImage.setBackgroundColor(ivImage.getContext().getColor(R.color.bg_subtle));
            }

            itemView.setOnClickListener(v -> {
                if (listener != null) listener.onProductClick(product);
            });
        }
    }
}
