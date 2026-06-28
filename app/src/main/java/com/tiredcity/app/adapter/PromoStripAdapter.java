package com.tiredcity.app.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.tiredcity.app.R;
import java.util.List;

public class PromoStripAdapter extends RecyclerView.Adapter<PromoStripAdapter.VH> {

    public static class PromoItem {
        public final @DrawableRes int imageRes;
        public final String eyebrow;
        public final String title;
        public final String subtitle;

        public PromoItem(@DrawableRes int imageRes, String eyebrow, String title, String subtitle) {
            this.imageRes = imageRes;
            this.eyebrow  = eyebrow;
            this.title    = title;
            this.subtitle = subtitle;
        }
    }

    private final List<PromoItem> items;
    private OnPromoClickListener  listener;

    public interface OnPromoClickListener {
        void onPromoClick(int position);
    }

    public PromoStripAdapter(List<PromoItem> items) {
        this.items = items;
    }

    public void setOnPromoClickListener(OnPromoClickListener l) {
        this.listener = l;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_promo_strip, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        PromoItem item = items.get(position);
        h.eyebrow.setText(item.eyebrow);
        h.title.setText(item.title);
        h.subtitle.setText(item.subtitle);
        Glide.with(h.bg.getContext())
                .load(item.imageRes)
                .centerCrop()
                .into(h.bg);
        h.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onPromoClick(h.getAdapterPosition());
        });
    }

    @Override
    public int getItemCount() {
        return items != null ? items.size() : 0;
    }

    static class VH extends RecyclerView.ViewHolder {
        final ImageView bg;
        final TextView eyebrow, title, subtitle;

        VH(@NonNull View v) {
            super(v);
            bg       = v.findViewById(R.id.iv_promo_bg);
            eyebrow  = v.findViewById(R.id.tv_promo_eyebrow);
            title    = v.findViewById(R.id.tv_promo_title);
            subtitle = v.findViewById(R.id.tv_promo_sub);
        }
    }
}
