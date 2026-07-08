package com.tiredcity.app.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.tiredcity.app.R;
import com.tiredcity.app.data.model.Event;
import com.tiredcity.app.utils.DateUtils;
import java.util.List;

public class EventAdapter extends RecyclerView.Adapter<EventAdapter.ViewHolder> {
    private List<Event> events;
    private OnEventClickListener listener;

    public interface OnEventClickListener {
        void onEventClick(Event event);
    }

    public EventAdapter(List<Event> events) {
        this.events = events;
    }

    public void setOnEventClickListener(OnEventClickListener listener) {
        this.listener = listener;
    }

    public void updateEvents(List<Event> newEvents) {
        this.events = newEvents;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_event, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Event event = events.get(position);
        holder.tvTitle.setText(event.getTitle());

        // Sự kiện online dùng icon quả địa cầu, offline dùng ghim địa điểm
        holder.tvLocation.setText(event.isOnline() ? "Online"
                : (event.getLocation() != null ? event.getLocation() : ""));
        holder.ivLocationIcon.setImageResource(
                event.isOnline() ? R.drawable.ic_globe : R.drawable.ic_pin);

        holder.tvDate.setText(event.getEventDate() != null
                ? DateUtils.formatDisplayDate(event.getEventDate()) : "");

        if (event.getLocalImageRes() != 0) {
            Glide.with(holder.ivImage.getContext())
                    .load(event.getLocalImageRes())
                    .fitCenter()
                    .placeholder(R.color.bg_subtle)
                    .into(holder.ivImage);
        } else if (event.getImageUrl() != null && !event.getImageUrl().isEmpty()) {
            Glide.with(holder.ivImage.getContext())
                    .load(event.getImageUrl())
                    .fitCenter()
                    .placeholder(R.color.bg_subtle)
                    .into(holder.ivImage);
        } else {
            holder.ivImage.setImageResource(R.color.bg_subtle);
        }

        View.OnClickListener openDetail = v -> {
            if (listener != null) listener.onEventClick(event);
        };
        holder.itemView.setOnClickListener(openDetail);
        holder.btnDetail.setOnClickListener(openDetail);
    }

    @Override
    public int getItemCount() {
        return events != null ? events.size() : 0;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivImage, ivLocationIcon;
        TextView tvTitle, tvLocation, tvDate;
        Button btnDetail;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivImage        = itemView.findViewById(R.id.iv_event_image);
            ivLocationIcon = itemView.findViewById(R.id.iv_event_location_icon);
            tvTitle        = itemView.findViewById(R.id.tv_event_title);
            tvLocation     = itemView.findViewById(R.id.tv_event_location);
            tvDate         = itemView.findViewById(R.id.tv_event_date);
            btnDetail      = itemView.findViewById(R.id.btn_event_detail);
        }
    }
}
