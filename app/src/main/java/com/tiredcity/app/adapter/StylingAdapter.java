package com.tiredcity.app.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.imageview.ShapeableImageView;
import com.tiredcity.app.R;
import com.tiredcity.app.data.model.CategoryItem;
import java.util.List;

public class StylingAdapter extends RecyclerView.Adapter<StylingAdapter.ViewHolder> {

    public interface OnCategoryClickListener {
        void onCategoryClick(CategoryItem item);
    }

    private List<CategoryItem> items;
    private OnCategoryClickListener listener;

    public StylingAdapter(List<CategoryItem> items) {
        this.items = items;
    }

    public void setOnCategoryClickListener(OnCategoryClickListener l) {
        this.listener = l;
    }

    /** Replace the displayed categories (used when switching tabs). */
    public void setItems(List<CategoryItem> newItems) {
        this.items = newItems;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_styling_category, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        CategoryItem item = items.get(position);

        if (item.getNameText() != null) {
            holder.tvName.setText(item.getNameText());
        } else {
            holder.tvName.setText(item.getNameResId());
        }
        if (item.getDescriptionText() != null) {
            holder.tvDescription.setText(item.getDescriptionText());
        } else {
            holder.tvDescription.setText(item.getDescriptionResId());
        }

        if (item.getImageRes() != 0) {
            holder.ivThumb.setImageResource(item.getImageRes());
            holder.ivThumb.setScaleType(ShapeableImageView.ScaleType.CENTER_CROP);
        } else {
            holder.ivThumb.setImageDrawable(null);
            holder.ivThumb.setBackgroundColor(item.getPlaceholderColor());
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onCategoryClick(item);
        });
    }

    @Override
    public int getItemCount() {
        return items != null ? items.size() : 0;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final TextView tvName;
        final TextView tvDescription;
        final ShapeableImageView ivThumb;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tv_category_name);
            tvDescription = itemView.findViewById(R.id.tv_category_description);
            ivThumb = itemView.findViewById(R.id.iv_category_thumb);
        }
    }
}
