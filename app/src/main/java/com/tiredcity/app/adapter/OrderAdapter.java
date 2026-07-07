package com.tiredcity.app.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.tiredcity.app.R;
import com.tiredcity.app.data.model.Order;
import com.tiredcity.app.data.model.OrderItemPreview;
import com.tiredcity.app.utils.Constants;
import com.tiredcity.app.utils.DateUtils;
import com.tiredcity.app.utils.PriceUtils;

import java.util.List;

public class OrderAdapter extends RecyclerView.Adapter<OrderAdapter.ViewHolder> {

    /** Số dòng sản phẩm hiển thị tối đa trong 1 thẻ; phần dư gộp vào "+N sản phẩm khác". */
    private static final int MAX_VISIBLE_ITEMS = 3;

    public interface OnOrderClickListener {
        void onOrderClick(Order order);
        default void onConfirmReceived(Order order) {}
        default void onReviewOrder(Order order) {}
    }

    private final List<Order> orders;
    private final OnOrderClickListener listener;

    public OrderAdapter(List<Order> orders, OnOrderClickListener listener) {
        this.orders   = orders;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_order, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(orders.get(position), listener);
    }

    @Override
    public int getItemCount() {
        return orders != null ? orders.size() : 0;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {

        private final TextView tvOrderId;
        private final TextView tvOrderStatus;
        private final TextView tvOrderDate;
        private final TextView tvOrderTotal;
        private final TextView tvMoreItems;
        private final LinearLayout llProducts;
        private final View layoutActions;
        private final View dividerActions;
        private final android.widget.Button btnConfirmReceived;
        private final android.widget.Button btnReview;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvOrderId     = itemView.findViewById(R.id.tv_order_id);
            tvOrderStatus = itemView.findViewById(R.id.tv_order_status);
            tvOrderDate   = itemView.findViewById(R.id.tv_order_date);
            tvOrderTotal  = itemView.findViewById(R.id.tv_order_total);
            tvMoreItems   = itemView.findViewById(R.id.tv_more_items);
            llProducts    = itemView.findViewById(R.id.ll_products);
            layoutActions = itemView.findViewById(R.id.layout_actions);
            dividerActions = itemView.findViewById(R.id.divider_actions);
            btnConfirmReceived = itemView.findViewById(R.id.btn_confirm_received);
            btnReview     = itemView.findViewById(R.id.btn_review);
        }

        void bind(Order order, OnOrderClickListener listener) {
            String displayId = order.getOrderCode() != null ? order.getOrderCode() : "#" + order.getId();
            tvOrderId.setText(displayId);
            tvOrderDate.setText(DateUtils.formatDisplay(order.getCreatedAt()));
            tvOrderTotal.setText(PriceUtils.format(order.getTotalPrice()));

            tvOrderStatus.setText(getStatusLabel(order.getStatus()));
            tvOrderStatus.setBackgroundResource(getStatusBackground(order.getStatus()));
            tvOrderStatus.setTextColor(itemView.getResources().getColor(
                    getStatusColor(order.getStatus()), itemView.getContext().getTheme()));

            bindProducts(order.getPreviewItems());

            // Logic hien thi nut hanh dong
            String status = order.getStatus() != null ? order.getStatus().toUpperCase() : "";
            boolean canConfirm = status.equals("SHIPPING") || status.equals("SHIPPED") || status.equals("PROCESSING") || status.equals("CONFIRMED");
            boolean canReview = status.equals("DELIVERED");

            if (canConfirm || canReview) {
                layoutActions.setVisibility(View.VISIBLE);
                dividerActions.setVisibility(View.VISIBLE);
                btnConfirmReceived.setVisibility(canConfirm ? View.VISIBLE : View.GONE);
                btnReview.setVisibility(canReview ? View.VISIBLE : View.GONE);
            } else {
                layoutActions.setVisibility(View.GONE);
                dividerActions.setVisibility(View.GONE);
            }

            btnConfirmReceived.setOnClickListener(v -> {
                if (listener != null) listener.onConfirmReceived(order);
            });

            btnReview.setOnClickListener(v -> {
                if (listener != null) listener.onReviewOrder(order);
            });

            itemView.setOnClickListener(v -> {
                if (listener != null) listener.onOrderClick(order);
            });
        }

        /** Đổ tối đa MAX_VISIBLE_ITEMS dòng sản phẩm vào container; phần dư → "+N sản phẩm khác". */
        private void bindProducts(List<OrderItemPreview> items) {
            llProducts.removeAllViews();
            if (items == null || items.isEmpty()) {
                tvMoreItems.setVisibility(View.GONE);
                return;
            }

            int shown = Math.min(items.size(), MAX_VISIBLE_ITEMS);
            LayoutInflater inflater = LayoutInflater.from(llProducts.getContext());
            for (int i = 0; i < shown; i++) {
                View row = inflater.inflate(R.layout.item_order_product, llProducts, false);
                bindProductRow(row, items.get(i));
                llProducts.addView(row);
            }

            int remaining = items.size() - shown;
            if (remaining > 0) {
                tvMoreItems.setVisibility(View.VISIBLE);
                tvMoreItems.setText(itemView.getContext()
                        .getString(R.string.order_more_items, remaining));
            } else {
                tvMoreItems.setVisibility(View.GONE);
            }
        }

        private void bindProductRow(View row, OrderItemPreview item) {
            ImageView ivImage  = row.findViewById(R.id.iv_product_image);
            TextView tvName     = row.findViewById(R.id.tv_product_name);
            TextView tvVariant  = row.findViewById(R.id.tv_product_variant);
            TextView tvLine     = row.findViewById(R.id.tv_line_total);
            TextView tvQuantity = row.findViewById(R.id.tv_quantity);

            tvName.setText(item.getName() != null ? item.getName() : "");

            String variant = item.variantLabel();
            if (variant.isEmpty()) {
                tvVariant.setVisibility(View.GONE);
            } else {
                tvVariant.setVisibility(View.VISIBLE);
                tvVariant.setText(variant);
            }

            tvLine.setText(PriceUtils.format(item.getLineTotal()));
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

        private String getStatusLabel(String status) {
            if (status == null) return "Không rõ";
            String s = status.toUpperCase();
            if (s.equals("SHIPPING") || s.equals("SHIPPED")) return "Đang giao";
            switch (s) {
                case "PENDING":   return "Chờ xử lý";
                case "CONFIRMED":
                case "PROCESSING": return "Đã xác nhận";
                case "DELIVERED": return "Đã nhận";
                case "CANCELLED": return "Đã hủy";
                default: return status;
            }
        }

        private int getStatusBackground(String status) {
            if (status == null) return R.drawable.bg_status_pending;
            String s = status.toUpperCase();
            if (s.equals("DELIVERED")) return R.drawable.bg_status_delivered;
            if (s.equals("SHIPPING") || s.equals("SHIPPED")) return R.drawable.bg_status_shipping;
            if (s.equals("CANCELLED")) return R.drawable.bg_status_cancelled;
            return R.drawable.bg_status_pending;
        }

        private int getStatusColor(String status) {
            if (status == null) return R.color.tc_warning;
            switch (status) {
                case Constants.ORDER_DELIVERED: return R.color.tc_success;
                case Constants.ORDER_SHIPPING:  return R.color.tc_info;
                case Constants.ORDER_CANCELLED: return R.color.tc_error;
                default: return R.color.tc_warning;
            }
        }
    }
}
