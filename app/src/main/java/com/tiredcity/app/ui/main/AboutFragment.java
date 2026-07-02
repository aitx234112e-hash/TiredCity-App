package com.tiredcity.app.ui.main;

import android.content.Intent;
import android.graphics.Rect;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.animation.DecelerateInterpolator;
import android.widget.VideoView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.widget.NestedScrollView;
import androidx.fragment.app.Fragment;
import com.bumptech.glide.Glide;
import com.tiredcity.app.R;
import com.tiredcity.app.databinding.FragmentAboutBinding;
import java.util.Arrays;
import java.util.List;

public class AboutFragment extends Fragment {

    // Chỉ cần lướt vào khung một chút là kích hoạt hiệu ứng/video ngay
    private static final float VISIBLE_THRESHOLD = 0.12f;
    private static final float SLIDE_OFFSET_DP = 32f;
    private static final long ANIM_DURATION = 260L;

    private FragmentAboutBinding binding;
    private List<RevealSection> revealSections;
    private List<VideoSection> videoSections;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentAboutBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        binding.rowContactEmail.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_SENDTO,
                    Uri.fromParts("mailto", binding.tvEmail.getText().toString(), null));
            startActivity(intent);
        });

        binding.rowContactHotline.setOnClickListener(v -> {
            String phone = binding.tvHotline.getText().toString().replace(" ", "");
            Intent intent = new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + phone));
            startActivity(intent);
        });

        binding.rowContactWebsite.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_VIEW,
                    Uri.parse("https://" + binding.tvWebsite.getText().toString()));
            startActivity(intent);
        });

        float density = getResources().getDisplayMetrics().density;
        float offsetPx = SLIDE_OFFSET_DP * density;

        revealSections = Arrays.asList(
                new RevealSection(binding.sectionAbout, offsetPx),
                new RevealSection(binding.sectionVision, offsetPx),
                new RevealSection(binding.sectionMission, offsetPx)
        );

        String packageName = requireContext().getPackageName();
        videoSections = Arrays.asList(
                new VideoSection(binding.sectionVideoBehance, binding.videoBehance,
                        Uri.parse("android.resource://" + packageName + "/" + R.raw.about_video_behance)),
                new VideoSection(binding.sectionVideoMission, binding.videoLogoTransform,
                        Uri.parse("android.resource://" + packageName + "/" + R.raw.about_video_logo_transform))
        );
        for (VideoSection section : videoSections) {
            section.setup();
        }
        applyAspectRatio(binding.sectionVideoBehance, 16f / 9f);
        applyAspectRatio(binding.sectionVideoMission, 16f / 9f);

        Glide.with(this)
                .load(R.drawable.about_gif_logo_transform)
                .override(1024, 576)
                .into(binding.ivVisionGif);

        NestedScrollView scrollView = binding.getRoot();
        scrollView.setOnScrollChangeListener(
                (NestedScrollView.OnScrollChangeListener) (v, scrollX, scrollY, oldScrollX, oldScrollY) -> checkVisibility());
        scrollView.post(this::checkVisibility);
        scrollView.postDelayed(() -> {
            if (binding == null) return;
            android.util.Log.d("TC_DEBUG", "AboutFragment settled(500ms): heroHeight="
                    + binding.sectionHero.getHeight() + " heroWidth=" + binding.sectionHero.getWidth()
                    + " heroVisibility=" + binding.sectionHero.getVisibility());
        }, 500);
    }

    /** Đưa nội dung về đầu trang; gọi mỗi lần tab/màn hình chứa fragment này được vào lại,
     * vì NestedScrollView giữ nguyên vị trí cuộn khi ẩn/hiện qua lại giữa các tab. */
    public void scrollToTop() {
        if (binding != null) {
            android.util.Log.d("TC_DEBUG", "AboutFragment.scrollToTop: scrollY(before)="
                    + binding.getRoot().getScrollY()
                    + " heroHeight=" + binding.sectionHero.getHeight()
                    + " heroWidth=" + binding.sectionHero.getWidth()
                    + " heroVisibility=" + binding.sectionHero.getVisibility()
                    + " rootHeight=" + binding.getRoot().getHeight());
            binding.getRoot().scrollTo(0, 0);
            checkVisibility();
        }
    }

    private void checkVisibility() {
        if (revealSections != null) {
            for (RevealSection section : revealSections) {
                section.update(isVisibleEnough(section.container));
            }
        }
        if (videoSections != null) {
            for (VideoSection section : videoSections) {
                section.update(isVisibleEnough(section.container));
            }
        }
    }

    /**
     * Forces {@code container} to a fixed width/height ratio using its actual measured width,
     * since VideoView ignores a fixed container height and re-measures to its own aspect ratio
     * (causing letterboxing), and app:layout_constraintDimensionRatio does not reliably resolve
     * for a ConstraintLayout nested inside a scrolling view hierarchy.
     */
    private static void applyAspectRatio(View container, float widthToHeightRatio) {
        container.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                int width = container.getWidth();
                String idName;
                try {
                    idName = container.getResources().getResourceEntryName(container.getId());
                } catch (Exception e) {
                    idName = String.valueOf(container.getId());
                }
                android.util.Log.d("TC_DEBUG", "applyAspectRatio id=" + idName
                        + " width=" + width + " curHeight=" + container.getLayoutParams().height
                        + " actualHeight=" + container.getHeight()
                        + " parentHeight=" + (container.getParent() instanceof View ? ((View) container.getParent()).getHeight() : -1));
                if (width <= 0) return;
                int desiredHeight = Math.round(width / widthToHeightRatio);
                ViewGroup.LayoutParams params = container.getLayoutParams();
                if (params.height != desiredHeight) {
                    params.height = desiredHeight;
                    container.setLayoutParams(params);
                }
            }
        });
    }

    private static boolean isVisibleEnough(View container) {
        int height = container.getHeight();
        if (height == 0) return false;
        Rect rect = new Rect();
        boolean any = container.getLocalVisibleRect(rect);
        return any && (rect.height() / (float) height) >= VISIBLE_THRESHOLD;
    }

    @Override
    public void onPause() {
        super.onPause();
        if (videoSections != null) {
            for (VideoSection section : videoSections) {
                section.pauseForLifecycle();
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (videoSections != null) {
            for (VideoSection section : videoSections) {
                section.resumeForLifecycle();
            }
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (videoSections != null) {
            for (VideoSection section : videoSections) {
                section.release();
            }
        }
        revealSections = null;
        videoSections = null;
        binding = null;
    }

    /** Fade + slide-in reveal for a section as it scrolls into view; replays each time it enters or leaves. */
    private static class RevealSection {
        private final View container;
        private final float offsetPx;
        private boolean revealed = false;

        RevealSection(View container, float offsetPx) {
            this.container = container;
            this.offsetPx = offsetPx;
            container.setTranslationY(offsetPx);
            container.setAlpha(0f);
        }

        void update(boolean visible) {
            if (visible && !revealed) {
                revealed = true;
                container.animate()
                        .translationY(0f)
                        .alpha(1f)
                        .setDuration(ANIM_DURATION)
                        .setInterpolator(new DecelerateInterpolator(1.4f))
                        .start();
            } else if (!visible && revealed) {
                revealed = false;
                container.animate()
                        .translationY(offsetPx)
                        .alpha(0f)
                        .setDuration(ANIM_DURATION)
                        .setInterpolator(new DecelerateInterpolator(1.2f))
                        .start();
            }
        }
    }

    /** Mutes, loops, and lazily plays a local raw video only while its container is scrolled into view. */
    private static class VideoSection {
        private final View container;
        private final VideoView videoView;
        private final Uri uri;
        private MediaPlayer player;
        private boolean prepared = false;
        private boolean visible = false;
        private boolean pausedForLifecycle = false;

        VideoSection(View container, VideoView videoView, Uri uri) {
            this.container = container;
            this.videoView = videoView;
            this.uri = uri;
        }

        void setup() {
            videoView.setVideoURI(uri);
            videoView.setOnPreparedListener(mp -> {
                player = mp;
                mp.setLooping(true);
                mp.setVolume(0f, 0f);
                prepared = true;
                if (visible && !pausedForLifecycle) {
                    mp.start();
                }
            });
        }

        void update(boolean nowVisible) {
            if (nowVisible == visible) return;
            visible = nowVisible;
            if (!prepared || pausedForLifecycle) return;
            if (visible) {
                player.start();
            } else {
                player.pause();
            }
        }

        void pauseForLifecycle() {
            pausedForLifecycle = true;
            if (prepared) {
                player.pause();
            }
        }

        void resumeForLifecycle() {
            pausedForLifecycle = false;
            if (prepared && visible) {
                player.start();
            }
        }

        void release() {
            videoView.stopPlayback();
            player = null;
            prepared = false;
        }
    }
}
