package com.tiredcity.app.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.imageview.ShapeableImageView;
import com.tiredcity.app.R;
import com.tiredcity.app.data.model.search.EventItem;
import com.tiredcity.app.data.model.search.ProductItem;
import com.tiredcity.app.data.model.search.PromotionItem;
import com.tiredcity.app.data.model.search.SearchItem;
import com.tiredcity.app.utils.PriceUtils;
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
        this.items = items;
    }

    public void setOnItemClickListener(OnItemClickListener l) {
        this.listener = l;
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
        final TextView tvContent;
        PromotionViewHolder(@NonNull View v) {
            super(v);
            tvContent = v.findViewById(R.id.tv_promo_content);
        }
        void bind(PromotionItem item) {
            tvContent.setText(item.getContentResId());
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
            tvBrand.setText(item.getBrandResId());
            tvName.setText(item.getNameResId());
            tvPrice.setText(PriceUtils.formatVnd(item.getPrice()));
            if (item.getImageRes() != 0) {
                ivImage.setImageResource(item.getImageRes());
            } else {
                ivImage.setImageDrawable(null);
            }
        }
    }
}
