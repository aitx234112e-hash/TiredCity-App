package com.tiredcity.app.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.google.android.material.imageview.ShapeableImageView;
import com.tiredcity.app.R;
import com.tiredcity.app.data.model.Product;
import com.tiredcity.app.utils.PriceUtils;
import java.util.List;

/** Dải ngang gọn hiển thị các sản phẩm khách đã xem gần đây (dùng ở giỏ hàng). */
public class RecentlyViewedAdapter extends RecyclerView.Adapter<RecentlyViewedAdapter.ViewHolder> {

    public interface OnItemClickListener {
        void onClick(Product product);
    }

    private final List<Product> products;
    private final OnItemClickListener listener;

    public RecentlyViewedAdapter(List<Product> products, OnItemClickListener listener) {
        this.products = products;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_recent_product, parent, false);
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
        final TextView tvName, tvPrice;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivImage = itemView.findViewById(R.id.iv_recent_image);
            tvName  = itemView.findViewById(R.id.tv_recent_name);
            tvPrice = itemView.findViewById(R.id.tv_recent_price);
        }

        void bind(Product product, OnItemClickListener listener) {
            tvName.setText(product.getName());
            tvPrice.setText(PriceUtils.format(product.getEffectivePrice()));

            String imageUrl = product.getFirstImage();
            if (imageUrl != null && !imageUrl.isEmpty()) {
                Glide.with(ivImage.getContext())
                        .load(imageUrl)
                        .centerCrop()
                        .placeholder(R.color.bg_subtle)
                        .into(ivImage);
            } else {
                ivImage.setImageDrawable(null);
            }

            itemView.setOnClickListener(v -> {
                if (listener != null) listener.onClick(product);
            });
        }
    }
}
