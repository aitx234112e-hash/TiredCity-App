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

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvOrderId     = itemView.findViewById(R.id.tv_order_id);
            tvOrderStatus = itemView.findViewById(R.id.tv_order_status);
            tvOrderDate   = itemView.findViewById(R.id.tv_order_date);
            tvOrderTotal  = itemView.findViewById(R.id.tv_order_total);
            tvItemCount   = itemView.findViewById(R.id.tv_order_items);
        }

        void bind(Order order, OnOrderClickListener listener) {
            if (tvOrderId != null)
                tvOrderId.setText("#" + order.getId());
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

            itemView.setOnClickListener(v -> {
                if (listener != null) listener.onOrderClick(order);
            });
        }

        private String getStatusLabel(String status) {
            if (status == null) return "Không rõ";
            switch (status) {
                case Constants.ORDER_PENDING:   return "Chờ xử lý";
                case Constants.ORDER_CONFIRMED: return "Đã xác nhận";
                case Constants.ORDER_SHIPPING:  return "Đang giao";
                case Constants.ORDER_DELIVERED: return "Đã nhận";
                case Constants.ORDER_CANCELLED: return "Đã hủy";
                default: return status;
            }
        }

        private int getStatusBackground(String status) {
            if (status == null) return R.drawable.bg_status_pending;
            switch (status) {
                case Constants.ORDER_DELIVERED: return R.drawable.bg_status_delivered;
                case Constants.ORDER_SHIPPING:  return R.drawable.bg_status_shipping;
                case Constants.ORDER_CANCELLED: return R.drawable.bg_status_cancelled;
                default: return R.drawable.bg_status_pending;
            }
        }
    }
}
