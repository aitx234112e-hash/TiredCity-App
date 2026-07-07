package com.tiredcity.app.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.tiredcity.app.R;
import com.tiredcity.app.data.model.OrderItemPreview;
import com.tiredcity.app.utils.PriceUtils;

import java.util.List;

/** Danh sách đầy đủ sản phẩm đã đặt ở màn Theo dõi đơn hàng — chỉ hiển thị, không cho sửa. */
public class OrderItemAdapter extends RecyclerView.Adapter<OrderItemAdapter.ViewHolder> {

    private final List<OrderItemPreview> items;

    public OrderItemAdapter(List<OrderItemPreview> items) {
        this.items = items;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_order_product, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(items.get(position));
    }

    @Override
    public int getItemCount() {
        return items != null ? items.size() : 0;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {

        private final ImageView ivImage;
        private final TextView tvName;
        private final TextView tvVariant;
        private final TextView tvLineTotal;
        private final TextView tvQuantity;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivImage     = itemView.findViewById(R.id.iv_product_image);
            tvName      = itemView.findViewById(R.id.tv_product_name);
            tvVariant   = itemView.findViewById(R.id.tv_product_variant);
            tvLineTotal = itemView.findViewById(R.id.tv_line_total);
            tvQuantity  = itemView.findViewById(R.id.tv_quantity);
        }

        void bind(OrderItemPreview item) {
            tvName.setText(item.getName() != null ? item.getName() : "");

            String variant = item.variantLabel();
            if (variant.isEmpty()) {
                tvVariant.setVisibility(View.GONE);
            } else {
                tvVariant.setVisibility(View.VISIBLE);
                tvVariant.setText(variant);
            }

            tvLineTotal.setText(PriceUtils.format(item.getLineTotal()));
            tvQuantity.setText("x" + item.getQuantity());

            String url = item.getImage();
            if (url != null && !url.isEmpty()) {
                Glide.with(ivImage.getContext())
                        .load(url)
                        .centerCrop()
                        .placeholder(R.color.bg_subtle)
                        .error(R.color.bg_subtle)
                        .into(ivImage);
            } else {
                ivImage.setImageResource(R.color.bg_subtle);
            }
        }
    }
}
