package com.tiredcity.app.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.google.android.material.imageview.ShapeableImageView;
import com.tiredcity.app.R;
import com.tiredcity.app.data.model.Product;
import com.tiredcity.app.data.model.search.EventItem;
import com.tiredcity.app.data.model.search.ProductItem;
import com.tiredcity.app.data.model.search.PromotionItem;
import com.tiredcity.app.data.model.search.SearchItem;
import com.tiredcity.app.utils.PriceUtils;
import java.util.ArrayList;
import java.util.List;

/**
 * Multi-view-type adapter for the search discovery list. Renders three mixed
 * row types — promotion, event and product — from one {@code List<SearchItem>}.
 */
public class SearchAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    public interface OnItemClickListener {
        void onItemClick(SearchItem item);
    }

    private final List<SearchItem> items;
    private OnItemClickListener listener;

    public SearchAdapter(List<SearchItem> items) {
        this.items = items != null ? new ArrayList<>(items) : new ArrayList<>();
    }

    public void setOnItemClickListener(OnItemClickListener l) {
        this.listener = l;
    }

    /** Thay toàn bộ danh sách gợi ý (ví dụ sau khi tải sản phẩm thật từ Firestore). */
    public void updateItems(List<SearchItem> newItems) {
        items.clear();
        if (newItems != null) items.addAll(newItems);
        notifyDataSetChanged();
    }

    @Override
    public int getItemViewType(int position) {
        return items.get(position).getType();
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        switch (viewType) {
            case SearchItem.TYPE_PROMOTION:
                return new PromotionViewHolder(
                        inflater.inflate(R.layout.item_search_promotion, parent, false));
            case SearchItem.TYPE_EVENT:
                return new EventViewHolder(
                        inflater.inflate(R.layout.item_search_event, parent, false));
            case SearchItem.TYPE_PRODUCT:
            default:
                return new ProductViewHolder(
                        inflater.inflate(R.layout.item_search_product, parent, false));
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        SearchItem item = items.get(position);
        switch (item.getType()) {
            case SearchItem.TYPE_PROMOTION:
                ((PromotionViewHolder) holder).bind((PromotionItem) item);
                break;
            case SearchItem.TYPE_EVENT:
                ((EventViewHolder) holder).bind((EventItem) item);
                break;
            case SearchItem.TYPE_PRODUCT:
                ((ProductViewHolder) holder).bind((ProductItem) item);
                break;
        }
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onItemClick(item);
        });
    }

    @Override
    public int getItemCount() {
        return items != null ? items.size() : 0;
    }

    // ─── Loại 1: Ưu đãi ───────────────────────────────────────────────
    static class PromotionViewHolder extends RecyclerView.ViewHolder {
        final ShapeableImageView ivImage;
        final TextView tvContent;
        PromotionViewHolder(@NonNull View v) {
            super(v);
            ivImage   = v.findViewById(R.id.iv_promo_image);
            tvContent = v.findViewById(R.id.tv_promo_content);
        }
        void bind(PromotionItem item) {
            tvContent.setText(item.getContentResId());
            if (item.getBannerRes() != 0) {
                ivImage.setImageResource(item.getBannerRes());
            } else {
                ivImage.setImageDrawable(null);
            }
        }
    }

    // ─── Loại 2: Sự kiện ──────────────────────────────────────────────
    static class EventViewHolder extends RecyclerView.ViewHolder {
        final ShapeableImageView ivBanner;
        final TextView tvTitle;
        final TextView tvTime;
        EventViewHolder(@NonNull View v) {
            super(v);
            ivBanner = v.findViewById(R.id.iv_event_banner);
            tvTitle  = v.findViewById(R.id.tv_event_title);
            tvTime   = v.findViewById(R.id.tv_event_time);
        }
        void bind(EventItem item) {
            tvTitle.setText(item.getTitleResId());
            tvTime.setText(item.getTimeResId());
            if (item.getBannerRes() != 0) {
                ivBanner.setImageResource(item.getBannerRes());
            } else {
                ivBanner.setImageDrawable(null);
            }
        }
    }

    // ─── Loại 3: Sản phẩm ─────────────────────────────────────────────
    static class ProductViewHolder extends RecyclerView.ViewHolder {
        final ShapeableImageView ivImage;
        final TextView tvBrand;
        final TextView tvName;
        final TextView tvPrice;
        ProductViewHolder(@NonNull View v) {
            super(v);
            ivImage = v.findViewById(R.id.iv_product_image);
            tvBrand = v.findViewById(R.id.tv_brand_name);
            tvName  = v.findViewById(R.id.tv_product_name);
            tvPrice = v.findViewById(R.id.tv_product_price);
        }
        void bind(ProductItem item) {
            Product p = item.getProduct();
            // Dòng đậm phía trên: dùng nhãn danh mục (ÁO DÀI, PHỤ KIỆN…) làm "thương hiệu".
            tvBrand.setText(p.getCategory() != null ? p.getCategory() : "");
            tvName.setText(p.getName() != null ? p.getName() : "");
            tvPrice.setText(PriceUtils.formatVnd(p.getEffectivePrice()));

            String imageUrl = p.getFirstImage();
            if (!imageUrl.isEmpty()) {
                Glide.with(ivImage.getContext())
                        .load(imageUrl)
                        .timeout(30000)
                        .centerCrop()
                        .transition(DrawableTransitionOptions.withCrossFade())
                        .placeholder(R.color.bg_subtle)
                        .error(R.color.bg_subtle)
                        .into(ivImage);
            } else {
                ivImage.setBackgroundColor(ivImage.getContext().getColor(R.color.bg_subtle));
                ivImage.setImageDrawable(null);
            }
        }
    }
}
