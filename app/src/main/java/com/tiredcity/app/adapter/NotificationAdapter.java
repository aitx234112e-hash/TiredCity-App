package com.tiredcity.app.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.tiredcity.app.R;
import com.tiredcity.app.data.model.NotificationItem;
import java.util.List;

public class NotificationAdapter
        extends RecyclerView.Adapter<NotificationAdapter.ViewHolder> {

    public interface OnNotificationClickListener {
        void onNotificationClick(NotificationItem item, int position);
    }

    private final List<NotificationItem> items;
    private final OnNotificationClickListener listener;

    public NotificationAdapter(List<NotificationItem> items,
                               OnNotificationClickListener listener) {
        this.items    = items;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_notification, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(items.get(position), listener, position);
    }

    @Override
    public int getItemCount() {
        return items != null ? items.size() : 0;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {

        private final ImageView ivIcon;
        private final TextView  tvTitle;
        private final TextView  tvMessage;
        private final TextView  tvTime;
        private final View      dotUnread;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivIcon    = itemView.findViewById(R.id.iv_notif_icon);
            tvTitle   = itemView.findViewById(R.id.tv_notif_title);
            tvMessage = itemView.findViewById(R.id.tv_notif_message);
            tvTime    = itemView.findViewById(R.id.tv_notif_time);
            dotUnread = itemView.findViewById(R.id.dot_unread);
        }

        void bind(NotificationItem item, OnNotificationClickListener listener, int position) {
            tvTitle.setText(item.getTitle());
            tvMessage.setText(item.getMessage());
            tvTime.setText(item.getTime());
            ivIcon.setImageResource(iconFor(item.getType()));
            dotUnread.setVisibility(item.isUnread() ? View.VISIBLE : View.GONE);

            itemView.setOnClickListener(v -> {
                if (listener != null) listener.onNotificationClick(item, position);
            });
        }

        private int iconFor(NotificationItem.Type type) {
            switch (type) {
                case ORDER: return R.drawable.ic_orders;
                case PROMO: return R.drawable.ic_ticket;
                default:    return R.drawable.ic_bell;
            }
        }
    }
}
