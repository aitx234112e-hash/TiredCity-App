package com.tiredcity.app.adapter;

import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.VideoView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.tiredcity.app.R;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Carousel media trộn lẫn ảnh/gif (Glide) và video local (VideoView).
 * Video tắt tiếng, lặp vô hạn và CHỈ phát ở trang đang hiển thị — gọi
 * {@link #setActivePosition(int)} từ OnPageChangeCallback của ViewPager2,
 * và pause/resume theo lifecycle của Fragment.
 */
public class MediaBannerAdapter extends RecyclerView.Adapter<MediaBannerAdapter.Holder> {

    /** Một trang carousel: hoặc ảnh drawable (gif/jpg), hoặc video trong res/raw. */
    public static class MediaItem {
        final int imageRes;
        final int videoRes;

        private MediaItem(int imageRes, int videoRes) {
            this.imageRes = imageRes;
            this.videoRes = videoRes;
        }

        public static MediaItem image(int drawableRes) {
            return new MediaItem(drawableRes, 0);
        }

        public static MediaItem video(int rawRes) {
            return new MediaItem(0, rawRes);
        }
    }

    private final List<MediaItem> items;
    private final Set<Holder> attachedHolders = new HashSet<>();
    private int activePosition = 0;
    private boolean pausedForLifecycle = false;

    public MediaBannerAdapter(List<MediaItem> items) {
        this.items = items;
    }

    /** Trang đang hiển thị của ViewPager2 — video ở trang này phát, các trang khác dừng. */
    public void setActivePosition(int position) {
        activePosition = position;
        syncPlayback();
    }

    public void pauseForLifecycle() {
        pausedForLifecycle = true;
        syncPlayback();
    }

    public void resumeForLifecycle() {
        pausedForLifecycle = false;
        syncPlayback();
    }

    private void syncPlayback() {
        for (Holder holder : attachedHolders) {
            holder.syncPlayback();
        }
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_media_banner, parent, false);
        return new Holder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull Holder holder, int position) {
        MediaItem item = items.get(position);
        holder.bind(item);
    }

    @Override
    public void onViewAttachedToWindow(@NonNull Holder holder) {
        attachedHolders.add(holder);
        holder.syncPlayback();
    }

    @Override
    public void onViewDetachedFromWindow(@NonNull Holder holder) {
        attachedHolders.remove(holder);
        if (holder.boundVideoRes != 0) holder.videoView.pause();
    }

    @Override
    public void onViewRecycled(@NonNull Holder holder) {
        if (holder.boundVideoRes != 0) holder.videoView.stopPlayback();
    }

    @Override
    public int getItemCount() {
        return items != null ? items.size() : 0;
    }

    class Holder extends RecyclerView.ViewHolder {
        final VideoView videoView;
        final ImageView imageView;
        int boundVideoRes = 0;

        Holder(@NonNull View itemView) {
            super(itemView);
            videoView = itemView.findViewById(R.id.vv_banner);
            imageView = itemView.findViewById(R.id.iv_banner_media);
            // VideoView tự requestFocus() trong constructor — tắt để không kéo trang
            // chứa carousel cuộn tới vị trí của nó khi mở màn hình.
            videoView.setFocusable(false);
            videoView.setFocusableInTouchMode(false);
            videoView.clearFocus();
        }

        void bind(MediaItem item) {
            if (item.videoRes != 0) {
                boundVideoRes = item.videoRes;
                imageView.setVisibility(View.GONE);
                videoView.setVisibility(View.VISIBLE);
                Uri uri = Uri.parse("android.resource://"
                        + itemView.getContext().getPackageName() + "/" + item.videoRes);
                videoView.setVideoURI(uri);
                // onPrepared chạy lại mỗi lần Surface tạo lại — áp lại loop/mute tại đây.
                videoView.setOnPreparedListener(mp -> {
                    mp.setLooping(true);
                    mp.setVolume(0f, 0f);
                    if (shouldPlay()) {
                        videoView.start();
                    } else {
                        // Hiện khung hình đầu thay vì màn đen khi trang chưa được chọn
                        videoView.seekTo(1);
                        videoView.pause();
                    }
                });
            } else {
                boundVideoRes = 0;
                videoView.setVisibility(View.GONE);
                imageView.setVisibility(View.VISIBLE);
                Glide.with(imageView.getContext())
                        .load(item.imageRes)
                        .centerCrop()
                        .into(imageView);
            }
        }

        private boolean shouldPlay() {
            return boundVideoRes != 0
                    && getBindingAdapterPosition() == activePosition
                    && !pausedForLifecycle;
        }

        void syncPlayback() {
            if (boundVideoRes == 0) return;
            if (shouldPlay()) {
                videoView.start();
            } else if (videoView.isPlaying()) {
                videoView.pause();
            }
        }
    }
}
