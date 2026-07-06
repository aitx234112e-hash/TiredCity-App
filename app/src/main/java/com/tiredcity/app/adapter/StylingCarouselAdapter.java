package com.tiredcity.app.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.tiredcity.app.R;
import java.util.List;

/**
 * Carousel ảnh trang phục THẬT ở đầu trang "Danh mục" (Horizontal Scroll Section) — mỗi trang là
 * 1 ảnh của đúng danh mục đang chọn, dùng chung ViewPager2 + PageTransformer với carousel "Sản
 * phẩm nổi bật" ở Trang chủ (xem StylingFragment.CenterScalePageTransformer).
 */
public class StylingCarouselAdapter extends RecyclerView.Adapter<StylingCarouselAdapter.ViewHolder> {

    private List<Integer> photoResIds;

    public StylingCarouselAdapter(List<Integer> photoResIds) {
        this.photoResIds = photoResIds;
    }

    public void updateData(List<Integer> newPhotoResIds) {
        this.photoResIds = newPhotoResIds;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_styling_carousel_photo, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.ivPhoto.setImageResource(photoResIds.get(position));
    }

    @Override
    public int getItemCount() {
        return photoResIds != null ? photoResIds.size() : 0;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final ImageView ivPhoto;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivPhoto = itemView.findViewById(R.id.iv_carousel_photo);
        }
    }
}
