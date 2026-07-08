package com.tiredcity.app.utils;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.app.Activity;
import android.graphics.Outline;
import android.graphics.Path;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewOutlineProvider;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;

import androidx.annotation.NonNull;

/**
 * Hiệu ứng "bay vào giỏ": tạo một bản sao thu nhỏ hình tròn của ảnh sản phẩm, cho nó bay
 * theo đường cong parabol từ vị trí bắt đầu (ảnh sản phẩm hoặc nút Thêm vào giỏ) tới icon
 * giỏ hàng, đồng thời thu nhỏ + mờ dần; khi chạm giỏ thì icon giỏ nảy nhẹ để phản hồi.
 *
 * Dùng chung cho mọi màn có icon giỏ hàng: chỉ cần truyền view nguồn, icon giỏ và một
 * {@link ImageBinder} để nạp ảnh (thường là Glide) vào ảnh bay.
 */
public final class CartFlyAnimation {

    /** Nạp ảnh vào view bay (ví dụ Glide.with(...).load(url).into(target)). */
    public interface ImageBinder {
        void bind(@NonNull ImageView target);
    }

    private static final int   FLY_SIZE_DP   = 52;
    private static final long  FLY_DURATION  = 650L;
    private static final float ARC_LIFT_DP   = 90f;
    private static final float END_SCALE     = 0.32f;

    private CartFlyAnimation() {}

    /**
     * Chạy hiệu ứng bay vào giỏ.
     *
     * @param activity  màn hình đang hiển thị (dùng android.R.id.content làm lớp phủ)
     * @param sourceView view nguồn để lấy vị trí bắt đầu (ảnh sản phẩm hoặc nút thêm giỏ)
     * @param cartIcon  icon giỏ hàng — đích đến của ảnh bay và là view sẽ nảy khi chạm
     * @param binder    nạp ảnh vào view bay
     */
    public static void fly(@NonNull Activity activity,
                           @NonNull View sourceView,
                           @NonNull View cartIcon,
                           @NonNull ImageBinder binder) {
        final ViewGroup overlay = activity.findViewById(android.R.id.content);
        if (overlay == null || sourceView.getWidth() == 0 || cartIcon.getWidth() == 0) {
            bumpCart(cartIcon);
            return;
        }

        final int sizePx = dp(activity, FLY_SIZE_DP);

        final ImageView fly = new ImageView(activity);
        fly.setLayoutParams(new FrameLayout.LayoutParams(sizePx, sizePx));
        fly.setScaleType(ImageView.ScaleType.CENTER_CROP);
        fly.setElevation(dp(activity, 8));
        fly.setClipToOutline(true);
        fly.setOutlineProvider(new ViewOutlineProvider() {
            @Override public void getOutline(View v, Outline outline) {
                outline.setOval(0, 0, v.getWidth(), v.getHeight());
            }
        });
        binder.bind(fly);
        overlay.addView(fly);

        // Toạ độ tương đối trong lớp phủ.
        final int[] parentLoc = new int[2];
        final int[] srcLoc = new int[2];
        final int[] dstLoc = new int[2];
        overlay.getLocationOnScreen(parentLoc);
        sourceView.getLocationOnScreen(srcLoc);
        cartIcon.getLocationOnScreen(dstLoc);

        final float startX = srcLoc[0] - parentLoc[0] + (sourceView.getWidth() - sizePx) / 2f;
        final float startY = srcLoc[1] - parentLoc[1] + (sourceView.getHeight() - sizePx) / 2f;
        final float endX = dstLoc[0] - parentLoc[0] + (cartIcon.getWidth() - sizePx) / 2f;
        final float endY = dstLoc[1] - parentLoc[1] + (cartIcon.getHeight() - sizePx) / 2f;

        fly.setX(startX);
        fly.setY(startY);

        // Đường cong: nhấc lên cao hơn cả điểm đầu và điểm cuối rồi rơi vào giỏ.
        final Path path = new Path();
        path.moveTo(startX, startY);
        final float ctrlX = endX;
        final float ctrlY = Math.min(startY, endY) - dp(activity, (int) ARC_LIFT_DP);
        path.quadTo(ctrlX, ctrlY, endX, endY);

        final ObjectAnimator move = ObjectAnimator.ofFloat(fly, View.X, View.Y, path);
        move.setInterpolator(new AccelerateInterpolator(1.1f));

        final ObjectAnimator scaleX = ObjectAnimator.ofFloat(fly, View.SCALE_X, 1f, END_SCALE);
        final ObjectAnimator scaleY = ObjectAnimator.ofFloat(fly, View.SCALE_Y, 1f, END_SCALE);
        final ObjectAnimator fade = ObjectAnimator.ofFloat(fly, View.ALPHA, 1f, 0.6f);

        final AnimatorSet set = new AnimatorSet();
        set.playTogether(move, scaleX, scaleY, fade);
        set.setDuration(FLY_DURATION);
        set.addListener(new AnimatorListenerAdapter() {
            @Override public void onAnimationEnd(Animator animation) {
                overlay.removeView(fly);
                bumpCart(cartIcon);
            }
        });
        set.start();
    }

    /** Cú nảy nhỏ ở icon giỏ khi ảnh bay chạm tới. */
    private static void bumpCart(@NonNull View cartIcon) {
        cartIcon.animate().cancel();
        cartIcon.setScaleX(1f);
        cartIcon.setScaleY(1f);
        cartIcon.animate()
                .scaleX(1.35f).scaleY(1.35f)
                .setDuration(130)
                .withEndAction(() -> cartIcon.animate()
                        .scaleX(1f).scaleY(1f)
                        .setInterpolator(new OvershootInterpolator(3f))
                        .setDuration(220)
                        .start())
                .start();
    }

    private static int dp(@NonNull Activity activity, int value) {
        return Math.round(TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, value, activity.getResources().getDisplayMetrics()));
    }
}
