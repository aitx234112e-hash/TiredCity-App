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
import com.tiredcity.app.data.model.CartItem;
import com.tiredcity.app.utils.PriceUtils;
import java.util.List;

/** Danh sách rút gọn sản phẩm trong đơn hàng ở màn Thanh toán — chỉ hiển thị, không cho sửa. */
public class CheckoutItemAdapter extends RecyclerView.Adapter<CheckoutItemAdapter.ViewHolder> {

    private final List<CartItem> items;

    public CheckoutItemAdapter(List<CartItem> items) {
        this.items = items;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_checkout_product, parent, false);
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

        final ImageView ivImage;
        final TextView tvName, tvVariant, tvQuantity, tvSubtotal;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivImage    = itemView.findViewById(R.id.iv_product_image);
            tvName     = itemView.findViewById(R.id.tv_product_name);
            tvVariant  = itemView.findViewById(R.id.tv_product_variant);
            tvQuantity = itemView.findViewById(R.id.tv_quantity);
            tvSubtotal = itemView.findViewById(R.id.tv_subtotal);
        }

        void bind(CartItem item) {
            if (item.getProduct() == null) return;

            tvName.setText(item.getProduct().getName());

            String variant = "";
            if (item.getSelectedSize() != null) variant += item.getSelectedSize();
            if (item.getSelectedColor() != null) {
                if (!variant.isEmpty()) variant += " • ";
                variant += item.getSelectedColor();
            }
            tvVariant.setText(variant);
            tvQuantity.setText("x" + item.getQuantity());
            tvSubtotal.setText(PriceUtils.format(item.getSubtotal()));

            String imageUrl = item.getProduct().getFirstImage();
            if (imageUrl != null && !imageUrl.isEmpty()) {
                Glide.with(ivImage.getContext())
                        .load(imageUrl)
                        .centerCrop()
                        .placeholder(R.color.bg_subtle)
                        .into(ivImage);
            }
        }
    }
}
