package com.tiredcity.app.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.tiredcity.app.R;
import java.util.List;

public class ShopCategoryAdapter extends RecyclerView.Adapter<ShopCategoryAdapter.ViewHolder> {

    public static final class CategoryItem {
        public final String displayTitle;
        public final String subtitle;
        /** String sent to the API as the category filter. */
        public final String apiFilter;
        @Nullable public final String imageUrl;
        @ColorInt public final int placeholderColor;

        public CategoryItem(String displayTitle, String subtitle, String apiFilter,
                            @Nullable String imageUrl, @ColorInt int placeholderColor) {
            this.displayTitle    = displayTitle;
            this.subtitle        = subtitle;
            this.apiFilter       = apiFilter;
            this.imageUrl        = imageUrl;
            this.placeholderColor = placeholderColor;
        }
    }

    public interface OnCategoryClickListener {
        void onCategoryClick(CategoryItem item);
    }

    private List<CategoryItem> items;
    private OnCategoryClickListener listener;

    public ShopCategoryAdapter(List<CategoryItem> items) {
        this.items = items;
    }

    public void setOnCategoryClickListener(OnCategoryClickListener l) {
        this.listener = l;
    }

    public void updateData(List<CategoryItem> newItems) {
        this.items = newItems;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_shop_category, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        CategoryItem item = items.get(position);
        holder.tvTitle.setText(item.displayTitle);
        holder.tvSubtitle.setText(item.subtitle);

        // Always set placeholder color first; image loads on top if URL is present
        holder.ivThumb.setBackgroundColor(item.placeholderColor);
        if (item.imageUrl != null && !item.imageUrl.isEmpty()) {
            Glide.with(holder.ivThumb.getContext())
                    .load(item.imageUrl)
                    .centerCrop()
                    .into(holder.ivThumb);
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
        final TextView  tvTitle;
        final TextView  tvSubtitle;
        final ImageView ivThumb;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle   = itemView.findViewById(R.id.tv_category_name);
            tvSubtitle = itemView.findViewById(R.id.tv_category_description);
            ivThumb   = itemView.findViewById(R.id.iv_category_thumb);
        }
    }
}
