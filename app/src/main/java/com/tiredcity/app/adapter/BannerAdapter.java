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
import java.util.List;

public class BannerAdapter extends RecyclerView.Adapter<BannerAdapter.ViewHolder> {

    public static class BannerItem {
        public final String imageUrl;
        public final String title;
        public final int    colorFallback;

        public BannerItem(String imageUrl, String title, int colorFallback) {
            this.imageUrl      = imageUrl;
            this.title         = title;
            this.colorFallback = colorFallback;
        }
    }

    private final List<BannerItem> items;
    private OnBannerClickListener  listener;

    public interface OnBannerClickListener {
        void onBannerClick(int position, BannerItem item);
    }

    public BannerAdapter(List<BannerItem> items) {
        this.items = items;
    }

    public void setOnBannerClickListener(OnBannerClickListener l) {
        this.listener = l;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_banner, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        BannerItem item = items.get(position);
        holder.tvTitle.setText(item.title);

        if (item.imageUrl != null && !item.imageUrl.isEmpty()) {
            Glide.with(holder.ivBanner.getContext())
                    .load(item.imageUrl)
                    .centerCrop()
                    .into(holder.ivBanner);
        } else {
            holder.ivBanner.setBackgroundColor(item.colorFallback);
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onBannerClick(holder.getAdapterPosition(), item);
        });
    }

    @Override
    public int getItemCount() {
        return items != null ? items.size() : 0;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final ImageView ivBanner;
        final TextView  tvTitle;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivBanner = itemView.findViewById(R.id.iv_banner);
            tvTitle  = itemView.findViewById(R.id.tv_banner_title);
        }
    }
}
