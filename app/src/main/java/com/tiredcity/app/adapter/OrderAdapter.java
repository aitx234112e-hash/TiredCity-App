package com.tiredcity.app.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.tiredcity.app.R;
import com.tiredcity.app.data.model.Order;
import com.tiredcity.app.utils.Constants;
import com.tiredcity.app.utils.DateUtils;
import com.tiredcity.app.utils.PriceUtils;
import java.util.List;

public class OrderAdapter extends RecyclerView.Adapter<OrderAdapter.ViewHolder> {

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
        private final TextView tvItemCount;
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
            tvItemCount   = itemView.findViewById(R.id.tv_order_items);
            layoutActions = itemView.findViewById(R.id.layout_actions);
            dividerActions = itemView.findViewById(R.id.divider_actions);
            btnConfirmReceived = itemView.findViewById(R.id.btn_confirm_received);
            btnReview     = itemView.findViewById(R.id.btn_review);
        }

        void bind(Order order, OnOrderClickListener listener) {
            if (tvOrderId != null) {
                String displayId = order.getOrderCode() != null ? order.getOrderCode() : "#" + order.getId();
                tvOrderId.setText(displayId);
            }
            if (tvOrderDate != null)
                tvOrderDate.setText(DateUtils.formatDisplay(order.getCreatedAt()));
            if (tvOrderTotal != null)
                tvOrderTotal.setText(PriceUtils.format(order.getTotalPrice()));
            if (tvItemCount != null && order.getItems() != null)
                tvItemCount.setText(order.getItems().size() + " sản phẩm");

            if (tvOrderStatus != null) {
                tvOrderStatus.setText(getStatusLabel(order.getStatus()));
                int bgRes = getStatusBackground(order.getStatus());
                tvOrderStatus.setBackgroundResource(bgRes);
            }

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
    }
}
