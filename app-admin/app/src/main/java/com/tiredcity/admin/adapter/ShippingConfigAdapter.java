package com.tiredcity.admin.adapter;

import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.tiredcity.admin.R;
import com.tiredcity.admin.data.model.ShippingConfig;
import com.tiredcity.admin.utils.DocUtils;

import java.util.ArrayList;
import java.util.List;

/** Danh sach co dinh 3 goi cuoc SPX Express (economy / standard / express). */
public class ShippingConfigAdapter extends RecyclerView.Adapter<ShippingConfigAdapter.VH> {

    public interface OnConfigClick {
        void onClick(ShippingConfig config);
    }

    private final List<ShippingConfig> items = new ArrayList<>();
    private final OnConfigClick listener;

    public ShippingConfigAdapter(OnConfigClick listener) {
        this.listener = listener;
    }

    public void submit(List<ShippingConfig> data) {
        items.clear();
        if (data != null) items.addAll(data);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_shipping_config, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        ShippingConfig c = items.get(position);
        h.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onClick(c);
        });

        h.name.setText(c.name);
        h.price.setText(DocUtils.money(c.price));
        h.estimate.setText(h.itemView.getContext().getString(R.string.scfg_estimate_label) + ": " + c.estimate);

        int statusColor = ContextCompat.getColor(h.itemView.getContext(),
                c.isActive ? R.color.st_success : R.color.st_danger);
        h.status.setText(c.isActive ? R.string.scfg_active : R.string.scfg_inactive);
        h.status.setTextColor(statusColor);
        if (h.status.getBackground() instanceof GradientDrawable) {
            ((GradientDrawable) h.status.getBackground().mutate()).setColor(withAlpha(statusColor, 0x22));
        }
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    private static int withAlpha(int color, int alpha) {
        return (alpha << 24) | (color & 0x00FFFFFF);
    }

    static class VH extends RecyclerView.ViewHolder {
        final TextView name, price, estimate, status;

        VH(@NonNull View v) {
            super(v);
            name = v.findViewById(R.id.tvName);
            price = v.findViewById(R.id.tvPrice);
            estimate = v.findViewById(R.id.tvEstimate);
            status = v.findViewById(R.id.tvStatus);
        }
    }
}
