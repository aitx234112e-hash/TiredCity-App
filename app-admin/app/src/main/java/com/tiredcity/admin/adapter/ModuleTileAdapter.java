package com.tiredcity.admin.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.tiredcity.admin.R;
import com.tiredcity.admin.data.AdminModule;

/** Luoi cac the chuc nang tren man Dashboard. */
public class ModuleTileAdapter extends RecyclerView.Adapter<ModuleTileAdapter.VH> {

    public interface OnTileClick {
        void onClick(AdminModule module);
    }

    private final AdminModule[] modules = AdminModule.values();
    private final OnTileClick listener;

    public ModuleTileAdapter(OnTileClick listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_dashboard_tile, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        AdminModule m = modules[position];
        h.title.setText(m.title);
        h.desc.setText(m.desc);
        h.icon.setImageResource(m.icon);
        int color = ContextCompat.getColor(h.icon.getContext(), m.colorRes);
        h.icon.setColorFilter(color);
        h.iconBg.setBackgroundTintList(android.content.res.ColorStateList.valueOf(withAlpha(color, 0x22)));
        h.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onClick(m);
        });
    }

    @Override
    public int getItemCount() {
        return modules.length;
    }

    private static int withAlpha(int color, int alpha) {
        return (alpha << 24) | (color & 0x00FFFFFF);
    }

    static class VH extends RecyclerView.ViewHolder {
        final ImageView icon;
        final View iconBg;
        final TextView title, desc;

        VH(@NonNull View v) {
            super(v);
            icon = v.findViewById(R.id.ivTileIcon);
            iconBg = v.findViewById(R.id.iconBg);
            title = v.findViewById(R.id.tvTileTitle);
            desc = v.findViewById(R.id.tvTileDesc);
        }
    }
}
