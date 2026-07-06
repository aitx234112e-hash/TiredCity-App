package com.tiredcity.app.adapter;

import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.LinearInterpolator;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.card.MaterialCardView;
import com.tiredcity.app.R;
import com.tiredcity.app.data.model.CategoryItem;
import java.util.List;

/** Thẻ "poster" ẢNH TRANG PHỤC THẬT cho lưới 2 cột ở trang "Danh mục" — ảnh phủ kín thẻ, lớp phủ
 *  gradient tối dần ở đáy (xem tc_category_card_scrim + item_styling_category.xml) giữ chữ trắng
 *  luôn đủ tương phản dù hậu cảnh ảnh thật là gì (tường gỗ tối, phông sáng,…) — không thể suy màu
 *  chữ từ placeholderColor như với ảnh mẫu vải phẳng màu. Mỗi lần bind: thẻ mờ + trượt NGANG từ
 *  ngoài vào (cột trái từ trái, cột phải từ phải) rồi vệt sáng chéo "tráng gương" lướt qua — đồng
 *  bộ ngôn ngữ hiệu ứng với các banner ở Trang chủ (HomeFragment.ShimmerSweep/ScrollReveal); đây
 *  cũng chính là cách các thẻ danh mục "lần lượt hiện ra" từng thẻ một khi vào tab. */
public class StylingAdapter extends RecyclerView.Adapter<StylingAdapter.ViewHolder> {

    private static final long ENTRANCE_DURATION = 360L;
    private static final long ENTRANCE_STAGGER_STEP = 45L;
    private static final int  ENTRANCE_STAGGER_MAX_STEPS = 6;
    private static final float SLIDE_DISTANCE_DP = 90f;
    private static final long SHIMMER_DURATION = 950L;
    private static final long SHIMMER_DELAY = 160L;
    private static final float COLUMN_GUTTER_DP = 6f;

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
        } else {
            holder.ivThumb.setImageDrawable(null);
            holder.card.setCardBackgroundColor(item.getPlaceholderColor());
        }

        applyColumnGutter(holder.itemView, position);

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onCategoryClick(item);
        });
        setupPressFeedback(holder.itemView);

        playEntrance(holder, position);
    }

    /** Cột trái (chẵn) chừa lề phải, cột phải (lẻ) chừa lề trái — tạo khe hở đều giữa 2 cột mà
     *  không cần ItemDecoration riêng (lưới chỉ 2 cột, số lượng mục ít). */
    private static void applyColumnGutter(View card, int position) {
        ViewGroup.LayoutParams rawParams = card.getLayoutParams();
        if (!(rawParams instanceof ViewGroup.MarginLayoutParams)) return;
        ViewGroup.MarginLayoutParams lp = (ViewGroup.MarginLayoutParams) rawParams;
        int gutter = Math.round(COLUMN_GUTTER_DP * card.getResources().getDisplayMetrics().density);
        boolean leftColumn = position % 2 == 0;
        lp.leftMargin = leftColumn ? 0 : gutter;
        lp.rightMargin = leftColumn ? gutter : 0;
        card.setLayoutParams(lp);
    }

    /** Thẻ mờ + trượt NGANG từ ngoài vào khi hiện ra — cột trái trượt từ trái, cột phải trượt từ
     *  phải, cộng vệt sáng lướt chéo qua cả thẻ ngay sau đó. Cả 2 chỉ chạy 1 LẦN mỗi lượt bind
     *  (không lặp) để an toàn khi RecyclerView tái sử dụng view. */
    private void playEntrance(ViewHolder holder, int position) {
        View card = holder.itemView;
        card.animate().cancel();
        float density = card.getResources().getDisplayMetrics().density;
        boolean fromLeft = position % 2 == 0;
        float startX = (fromLeft ? -1f : 1f) * SLIDE_DISTANCE_DP * density;
        card.setAlpha(0f);
        card.setTranslationX(startX);
        long delay = Math.min(position, ENTRANCE_STAGGER_MAX_STEPS) * ENTRANCE_STAGGER_STEP;
        card.animate()
                .alpha(1f)
                .translationX(0f)
                .setStartDelay(delay)
                .setDuration(ENTRANCE_DURATION)
                .setInterpolator(new DecelerateInterpolator(1.4f))
                .start();

        View shimmer = holder.shimmerThumb;
        shimmer.animate().cancel();
        card.post(() -> {
            int width = card.getWidth();
            if (width <= 0) return;
            shimmer.setTranslationX(-width * 0.6f);
            shimmer.animate()
                    .translationX(width * 1.3f)
                    .setStartDelay(delay + SHIMMER_DELAY)
                    .setDuration(SHIMMER_DURATION)
                    .setInterpolator(new LinearInterpolator())
                    .start();
        });
    }

    /** "Bóp" nhẹ khi chạm — phản hồi xúc giác giống các nút CTA ở Trang chủ. Trả về false để
     *  click/ripple mặc định của card vẫn hoạt động bình thường. */
    private static void setupPressFeedback(View card) {
        card.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    v.animate().scaleX(0.97f).scaleY(0.97f).setDuration(120L).start();
                    break;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    v.animate().scaleX(1f).scaleY(1f).setDuration(150L).start();
                    break;
                default:
                    break;
            }
            return false;
        });
    }

    @Override
    public int getItemCount() {
        return items != null ? items.size() : 0;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final MaterialCardView card;
        final TextView tvName;
        final TextView tvDescription;
        final ImageView ivThumb;
        final View shimmerThumb;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            card = (MaterialCardView) itemView;
            tvName = itemView.findViewById(R.id.tv_category_name);
            tvDescription = itemView.findViewById(R.id.tv_category_description);
            ivThumb = itemView.findViewById(R.id.iv_category_thumb);
            shimmerThumb = itemView.findViewById(R.id.shimmer_category_thumb);
        }
    }
}
