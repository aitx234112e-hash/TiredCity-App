package com.tiredcity.app.ui.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.view.animation.OvershootInterpolator;
import androidx.appcompat.widget.AppCompatImageButton;
import com.tiredcity.app.R;

/**
 * Nút "yêu thích" tái sử dụng ở trang chi tiết sản phẩm và danh sách sản phẩm.
 * Tự quản lý drawable (viền xám / đỏ đặc) và hiệu ứng pop khi đổi trạng thái.
 */
public class FavoriteButton extends AppCompatImageButton {

    private boolean saved = false;

    public FavoriteButton(Context context) {
        super(context);
        init();
    }

    public FavoriteButton(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public FavoriteButton(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        setImageResource(R.drawable.ic_star_outline);
    }

    public boolean isSaved() {
        return saved;
    }

    public void setSaved(boolean saved, boolean animate) {
        this.saved = saved;
        setImageResource(saved ? R.drawable.ic_star_filled : R.drawable.ic_star_outline);
        if (animate) playPopAnimation();
    }

    /** Đảo trạng thái, luôn kèm hiệu ứng pop. Trả về trạng thái mới. */
    public boolean toggle() {
        setSaved(!saved, true);
        return saved;
    }

    private void playPopAnimation() {
        animate().cancel();
        setScaleX(0.7f);
        setScaleY(0.7f);
        animate()
                .scaleX(1.15f)
                .scaleY(1.15f)
                .setDuration(140)
                .setInterpolator(new OvershootInterpolator(3f))
                .withEndAction(() -> animate().scaleX(1f).scaleY(1f).setDuration(120).start())
                .start();
    }
}
