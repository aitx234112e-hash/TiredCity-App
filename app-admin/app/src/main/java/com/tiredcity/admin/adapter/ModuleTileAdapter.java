package com.tiredcity.admin.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.tiredcity.admin.R;
import com.tiredcity.admin.data.AdminModule;

import java.util.ArrayList;
import java.util.List;

/** Luoi cac the chuc nang tren man Dashboard, nhom theo Section (mirror sidebar web-admin). */
public class ModuleTileAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int TYPE_HEADER = 0;
    private static final int TYPE_TILE = 1;

    public interface OnTileClick {
        void onClick(AdminModule module);
    }

    /** Danh sach phang: AdminModule.Section (header) xen ke AdminModule (tile). */
    private final List<Object> items = new ArrayList<>();
    private final OnTileClick listener;

    public ModuleTileAdapter(OnTileClick listener) {
        this.listener = listener;
        for (AdminModule.Section section : AdminModule.Section.values()) {
            items.add(section);
            for (AdminModule m : AdminModule.values()) {
                if (m.section == section) items.add(m);
            }
        }
    }

    /** Header chiem tron 2 cot, the chuc nang chiem 1 cot. */
    public GridLayoutManager.SpanSizeLookup spanSizeLookup(int spanCount) {
        return new GridLayoutManager.SpanSizeLookup() {
            @Override
            public int getSpanSize(int position) {
                return items.get(position) instanceof AdminModule.Section ? spanCount : 1;
            }
        };
    }

    @Override
    public int getItemViewType(int position) {
        return items.get(position) instanceof AdminModule.Section ? TYPE_HEADER : TYPE_TILE;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        if (viewType == TYPE_HEADER) {
            return new HeaderVH(inflater.inflate(R.layout.item_dashboard_section_header, parent, false));
        }
        return new TileVH(inflater.inflate(R.layout.item_dashboard_tile, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        Object item = items.get(position);
        if (holder instanceof HeaderVH) {
            ((HeaderVH) holder).title.setText(((AdminModule.Section) item).title);
            return;
        }
        AdminModule m = (AdminModule) item;
        TileVH h = (TileVH) holder;
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
        return items.size();
    }

    private static int withAlpha(int color, int alpha) {
        return (alpha << 24) | (color & 0x00FFFFFF);
    }

    static class HeaderVH extends RecyclerView.ViewHolder {
        final TextView title;

        HeaderVH(@NonNull View v) {
            super(v);
            title = v.findViewById(R.id.tvSectionTitle);
        }
    }

    static class TileVH extends RecyclerView.ViewHolder {
        final ImageView icon;
        final View iconBg;
        final TextView title, desc;

        TileVH(@NonNull View v) {
            super(v);
            icon = v.findViewById(R.id.ivTileIcon);
            iconBg = v.findViewById(R.id.iconBg);
            title = v.findViewById(R.id.tvTileTitle);
            desc = v.findViewById(R.id.tvTileDesc);
        }
    }
}
