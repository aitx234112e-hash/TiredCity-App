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

    /** Khi thu gọn, chỉ hiện tối đa 2 sản phẩm; phần còn lại ẩn sau nút "Xem tất cả". */
    private static final int COLLAPSED_LIMIT = 2;

    public interface OnItemReviewListener {
        void onReviewItem(CartItem item);
    }

    private final List<CartItem> items;
    private boolean collapsed = true;
    private OnItemReviewListener reviewListener;
    private boolean showReviewButton = false;

    public CheckoutItemAdapter(List<CartItem> items) {
        this.items = items;
    }

    /** Tổng số sản phẩm thật (bỏ qua trạng thái thu gọn) — dùng cho nhãn "Xem tất cả (n)". */
    public int getTotalCount() {
        return items != null ? items.size() : 0;
    }

    /** Có nhiều hơn ngưỡng thu gọn hay không → quyết định hiện nút toggle. */
    public boolean isCollapsible() {
        return getTotalCount() > COLLAPSED_LIMIT;
    }

    public boolean isCollapsed() {
        return collapsed;
    }

    /** Bật/tắt thu gọn danh sách và cập nhật lại RecyclerView. */
    public void setCollapsed(boolean collapsed) {
        if (this.collapsed == collapsed) return;
        this.collapsed = collapsed;
        notifyDataSetChanged();
    }

    public CheckoutItemAdapter(List<CartItem> items, boolean showReviewButton, OnItemReviewListener listener) {
        this.items = items;
        this.showReviewButton = showReviewButton;
        this.reviewListener = listener;
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
        holder.bind(items.get(position), showReviewButton, reviewListener);
    }

    @Override
    public int getItemCount() {
        int total = getTotalCount();
        return collapsed ? Math.min(total, COLLAPSED_LIMIT) : total;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {

        final ImageView ivImage;
        final TextView tvName, tvVariant, tvQuantity, tvSubtotal, btnReview;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivImage    = itemView.findViewById(R.id.iv_product_image);
            tvName     = itemView.findViewById(R.id.tv_product_name);
            tvVariant  = itemView.findViewById(R.id.tv_product_variant);
            tvQuantity = itemView.findViewById(R.id.tv_quantity);
            tvSubtotal = itemView.findViewById(R.id.tv_subtotal);
            btnReview  = itemView.findViewById(R.id.btn_item_review);
        }

        void bind(CartItem item, boolean showReviewButton, OnItemReviewListener listener) {
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

            if (btnReview != null) {
                btnReview.setVisibility(showReviewButton ? View.VISIBLE : View.GONE);
                btnReview.setOnClickListener(v -> {
                    if (listener != null) listener.onReviewItem(item);
                });
            }

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
