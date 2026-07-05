package com.tiredcity.app.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.tiredcity.app.R;
import java.util.List;

/** Carousel ưu đãi — mỗi trang là 1 ảnh voucher đã có sẵn chữ/thiết kế, không đè thêm text. */
public class PromoStripAdapter extends RecyclerView.Adapter<PromoStripAdapter.VH> {

    public static class PromoItem {
        public final @DrawableRes int imageRes;

        public PromoItem(@DrawableRes int imageRes) {
            this.imageRes = imageRes;
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
        Glide.with(h.bg.getContext())
                .load(item.imageRes)
                .fitCenter()
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

        VH(@NonNull View v) {
            super(v);
            bg = v.findViewById(R.id.iv_promo_bg);
        }
    }
}
