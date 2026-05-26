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
        holder.tvLocation.setText(event.isOnline() ? "🌐 Online"
                : (event.getLocation() != null ? event.getLocation() : ""));
        holder.tvDate.setText(event.getStartDate() != null
                ? DateUtils.formatDisplayDate(event.getStartDate()) : "");

        if (event.getImageUrl() != null && !event.getImageUrl().isEmpty()) {
            Glide.with(holder.ivImage.getContext())
                    .load(event.getImageUrl())
                    .centerCrop()
                    .placeholder(R.color.bg_subtle)
                    .into(holder.ivImage);
        } else {
            holder.ivImage.setImageResource(R.color.bg_subtle);
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onEventClick(event);
        });
    }

    @Override
    public int getItemCount() {
        return events != null ? events.size() : 0;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivImage;
        TextView tvTitle, tvLocation, tvDate;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivImage    = itemView.findViewById(R.id.iv_event_image);
            tvTitle    = itemView.findViewById(R.id.tv_event_title);
            tvLocation = itemView.findViewById(R.id.tv_event_location);
            tvDate     = itemView.findViewById(R.id.tv_event_date);
        }
    }
}
