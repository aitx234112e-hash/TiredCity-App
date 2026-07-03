package com.tiredcity.app.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.tiredcity.app.R;
import com.tiredcity.app.data.model.CartItem;
import com.tiredcity.app.utils.PriceUtils;
import java.util.List;

public class CartAdapter extends RecyclerView.Adapter<CartAdapter.ViewHolder> {

    public interface OnRemoveListener {
        void onRemove(CartItem item);
    }

    public interface OnCartItemActionListener {
        void onQuantityChanged(CartItem item, int newQuantity);
        void onRemoveClick(CartItem item);
    }

    private List<CartItem> cartItems;
    private OnRemoveListener removeListener;
    private OnCartItemActionListener actionListener;
    private Runnable onCartChangeListener;

    /** Constructor used by CartActivity (lambda remove). */
    public CartAdapter(List<CartItem> cartItems, OnRemoveListener removeListener) {
        this.cartItems      = cartItems;
        this.removeListener = removeListener;
    }

    /** Constructor with full action listener. */
    public CartAdapter(List<CartItem> cartItems) {
        this.cartItems = cartItems;
    }

    public void setOnCartItemActionListener(OnCartItemActionListener listener) {
        this.actionListener = listener;
    }

    /** Gọi lại mỗi khi giỏ hàng đổi (chọn/bỏ chọn, đổi số lượng) để màn hình cập nhật tổng tiền. */
    public void setOnCartChangeListener(Runnable listener) {
        this.onCartChangeListener = listener;
    }

    public void updateItems(List<CartItem> newItems) {
        this.cartItems = newItems;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_cart, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        CartItem item = cartItems.get(position);
        holder.bind(item, removeListener, actionListener, onCartChangeListener);
    }

    @Override
    public int getItemCount() {
        return cartItems != null ? cartItems.size() : 0;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {

        final ImageView  ivImage;
        final TextView   tvName, tvVariant, tvQuantity, tvSubtotal;
        final TextView   btnDecrease, btnIncrease;
        final ImageButton btnRemove;
        final CheckBox   cbSelect;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivImage     = itemView.findViewById(R.id.iv_product_image);
            tvName      = itemView.findViewById(R.id.tv_product_name);
            tvVariant   = itemView.findViewById(R.id.tv_product_variant);
            tvQuantity  = itemView.findViewById(R.id.tv_quantity);
            tvSubtotal  = itemView.findViewById(R.id.tv_subtotal);
            btnDecrease = itemView.findViewById(R.id.btn_decrease);
            btnIncrease = itemView.findViewById(R.id.btn_increase);
            btnRemove   = itemView.findViewById(R.id.btn_remove);
            cbSelect    = itemView.findViewById(R.id.cb_select);
        }

        void bind(CartItem item, OnRemoveListener removeListener, OnCartItemActionListener actionListener,
                  Runnable onCartChangeListener) {
            if (item.getProduct() == null) return;

            tvName.setText(item.getProduct().getName());

            cbSelect.setOnCheckedChangeListener(null);
            cbSelect.setChecked(item.isSelected());
            cbSelect.setOnCheckedChangeListener((buttonView, isChecked) -> {
                item.setSelected(isChecked);
                if (onCartChangeListener != null) onCartChangeListener.run();
            });

            String variant = "";
            if (item.getSelectedSize() != null) variant += item.getSelectedSize();
            if (item.getSelectedColor() != null) {
                if (!variant.isEmpty()) variant += " • ";
                variant += item.getSelectedColor();
            }
            tvVariant.setText(variant);
            tvQuantity.setText(String.valueOf(item.getQuantity()));
            tvSubtotal.setText(PriceUtils.format(item.getSubtotal()));

            // Load image
            String imageUrl = item.getProduct().getFirstImage();
            if (imageUrl != null && !imageUrl.isEmpty()) {
                Glide.with(ivImage.getContext())
                    .load(imageUrl)
                    .centerCrop()
                    .placeholder(R.color.bg_subtle)
                    .into(ivImage);
            }

            // Quantity controls
            btnDecrease.setOnClickListener(v -> {
                if (item.getQuantity() > 1) {
                    item.setQuantity(item.getQuantity() - 1);
                    tvQuantity.setText(String.valueOf(item.getQuantity()));
                    tvSubtotal.setText(PriceUtils.format(item.getSubtotal()));
                    if (actionListener != null) actionListener.onQuantityChanged(item, item.getQuantity());
                    if (onCartChangeListener != null) onCartChangeListener.run();
                }
            });

            btnIncrease.setOnClickListener(v -> {
                item.setQuantity(item.getQuantity() + 1);
                tvQuantity.setText(String.valueOf(item.getQuantity()));
                tvSubtotal.setText(PriceUtils.format(item.getSubtotal()));
                if (actionListener != null) actionListener.onQuantityChanged(item, item.getQuantity());
                if (onCartChangeListener != null) onCartChangeListener.run();
            });

            btnRemove.setOnClickListener(v -> {
                if (removeListener != null) removeListener.onRemove(item);
                if (actionListener != null) actionListener.onRemoveClick(item);
            });
        }
    }
}
