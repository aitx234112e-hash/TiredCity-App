package com.tiredcity.admin.adapter;

import android.graphics.PorterDuff;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.tiredcity.admin.R;
import com.tiredcity.admin.data.model.Row;

import java.util.ArrayList;
import java.util.List;

/** Adapter dung chung cho moi danh sach admin. Binh du lieu tu {@link Row}. */
public class RowAdapter extends RecyclerView.Adapter<RowAdapter.VH> {

    public interface OnRowClick {
        void onClick(int position);
    }

    private final List<Row> items = new ArrayList<>();
    private final OnRowClick listener;

    public RowAdapter() {
        this(null);
    }

    public RowAdapter(OnRowClick listener) {
        this.listener = listener;
    }

    public void submit(List<Row> data) {
        items.clear();
        if (data != null) items.addAll(data);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_admin_row, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        Row r = items.get(position);
        if (listener != null) {
            h.itemView.setOnClickListener(v -> {
                int pos = h.getBindingAdapterPosition();
                if (pos != RecyclerView.NO_POSITION) listener.onClick(pos);
            });
        }
        h.title.setText(r.title);

        h.subtitle.setText(r.subtitle);
        h.subtitle.setVisibility(isEmpty(r.subtitle) ? View.GONE : View.VISIBLE);

        h.meta.setText(r.meta);
        h.meta.setVisibility(isEmpty(r.meta) ? View.GONE : View.VISIBLE);

        if (isEmpty(r.badge)) {
            h.badge.setVisibility(View.GONE);
        } else {
            h.badge.setVisibility(View.VISIBLE);
            h.badge.setText(r.badge);
            int color = ContextCompat.getColor(h.badge.getContext(),
                    r.badgeColor != 0 ? r.badgeColor : R.color.st_neutral);
            if (h.badge.getBackground() instanceof GradientDrawable) {
                ((GradientDrawable) h.badge.getBackground().mutate())
                        .setColor(withAlpha(color, 0x22));
            }
            h.badge.setTextColor(color);
        }

        if (!isEmpty(r.actionLabel) && r.onActionClick != null) {
            h.btnAction.setVisibility(View.VISIBLE);
            h.btnAction.setText(r.actionLabel);
            h.btnAction.setOnClickListener(v -> r.onActionClick.run());
        } else {
            h.btnAction.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    private static boolean isEmpty(String s) {
        return s == null || s.trim().isEmpty();
    }

    private static int withAlpha(int color, int alpha) {
        return (alpha << 24) | (color & 0x00FFFFFF);
    }

    static class VH extends RecyclerView.ViewHolder {
        final TextView title, subtitle, meta, badge, btnAction;

        VH(@NonNull View v) {
            super(v);
            title = v.findViewById(R.id.tvTitle);
            subtitle = v.findViewById(R.id.tvSubtitle);
            meta = v.findViewById(R.id.tvMeta);
            badge = v.findViewById(R.id.tvBadge);
            btnAction = v.findViewById(R.id.btnAction);
        }
    }
}
