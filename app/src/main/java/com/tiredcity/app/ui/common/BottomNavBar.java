package com.tiredcity.app.ui.common;

import android.app.Activity;
import android.content.Intent;
import android.widget.ImageView;
import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import com.tiredcity.app.R;
import com.tiredcity.app.databinding.ViewBottomNavBinding;
import com.tiredcity.app.ui.main.MainActivity;
import com.tiredcity.app.ui.profile.ProfileActivity;
import com.tiredcity.app.ui.shop.SearchActivity;
import com.tiredcity.app.utils.EdgeToEdgeUtils;

/**
 * Gắn sự kiện + trạng thái chọn cho thanh bottom-nav lơ lửng ({@code view_bottom_nav.xml}).
 *
 * <p>Ba tab HOME/STYLING/EXPLORE là destination trong nav_graph nên chỉ MainActivity điều hướng
 * được — các màn khác gửi {@link MainActivity#EXTRA_OPEN_TAB} về MainActivity. Còn SHOP và PROFILE
 * là Activity riêng, ai cũng mở thẳng được.
 *
 * <p>Mỗi lần rời màn hiện tại (trừ MainActivity) đều {@code finish()} nó, để back-stack luôn phẳng:
 * người dùng nhảy giữa các mục của thanh nav bao nhiêu lần thì bấm Back vẫn về đúng MainActivity,
 * không phải lùi qua từng màn đã xem.
 */
public final class BottomNavBar {

    /** Mục đang được chọn trên thanh nav; {@link #NONE} khi màn hình không ứng với mục nào. */
    public enum Tab { SHOP, STYLING, HOME, EXPLORE, PROFILE, NONE }

    /** Xử lý 3 tab nằm trong nav_graph — chỉ MainActivity cung cấp, nơi khác truyền {@code null}. */
    public interface OnGraphTabSelected {
        void onSelected(@NonNull Tab tab);
    }

    private BottomNavBar() { }

    /**
     * @param host           Activity chứa thanh nav.
     * @param nav            binding của {@code <include layout="@layout/view_bottom_nav">}.
     * @param active         mục cần tô đậm (icon đỏ + nền pill).
     * @param graphTabHandler cách điều hướng 3 tab trong nav_graph; {@code null} = gửi Intent về MainActivity.
     */
    public static void bind(@NonNull Activity host,
                            @NonNull ViewBottomNavBinding nav,
                            @NonNull Tab active,
                            @Nullable OnGraphTabSelected graphTabHandler) {

        EdgeToEdgeUtils.applyNavBarBottomMargin(nav.getRoot());

        nav.navShop.setOnClickListener(v -> {
            if (active == Tab.SHOP) return;
            leaveFor(host, new Intent(host, SearchActivity.class));
        });
        nav.navProfile.setOnClickListener(v -> {
            if (active == Tab.PROFILE) return;
            leaveFor(host, new Intent(host, ProfileActivity.class));
        });

        bindGraphTab(host, nav.navStyling, Tab.STYLING, graphTabHandler);
        bindGraphTab(host, nav.navHome, Tab.HOME, graphTabHandler);
        bindGraphTab(host, nav.navExplore, Tab.EXPLORE, graphTabHandler);

        setSelected(host, nav, active);
    }

    /** Tô đậm lại mục đang chọn — MainActivity gọi mỗi khi NavController đổi destination. */
    public static void setSelected(@NonNull Activity host,
                                   @NonNull ViewBottomNavBinding nav,
                                   @NonNull Tab active) {
        setIcon(host, nav.navShopIcon, active == Tab.SHOP);
        setIcon(host, nav.navStylingIcon, active == Tab.STYLING);
        setIcon(host, nav.navHomeIcon, active == Tab.HOME);
        setIcon(host, nav.navExploreIcon, active == Tab.EXPLORE);
        setIcon(host, nav.navProfileIcon, active == Tab.PROFILE);
    }

    /**
     * Không chặn cú bấm vào tab đang mở ở đây: tab đang chọn đổi liên tục trong MainActivity, mà
     * listener thì chỉ bind một lần. Việc bấm lại tab hiện tại đã được {@code navigateTab()} bỏ qua.
     */
    private static void bindGraphTab(Activity host,
                                     android.view.View target,
                                     Tab tab,
                                     @Nullable OnGraphTabSelected handler) {
        target.setOnClickListener(v -> {
            if (handler != null) {
                handler.onSelected(tab);
            } else {
                Intent intent = new Intent(host, MainActivity.class);
                intent.putExtra(MainActivity.EXTRA_OPEN_TAB, tab.name());
                leaveFor(host, intent);
            }
        });
    }

    /**
     * Mở màn mới rồi đóng màn hiện tại — nhưng KHÔNG đóng MainActivity, vì nó là gốc back-stack
     * (đóng đi thì bấm Back từ Tìm kiếm/Tôi sẽ thoát hẳn app).
     */
    private static void leaveFor(Activity host, Intent intent) {
        if (host instanceof MainActivity) {
            host.startActivity(intent);
            return;
        }
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        host.startActivity(intent);
        host.finish();
        host.overridePendingTransition(R.anim.tc_fade_in, R.anim.tc_fade_out);
    }

    /** Mục đang chọn: icon đỏ đậm + nền pill sáng; còn lại đỏ nhạt. */
    private static void setIcon(Activity host, ImageView icon, boolean isActive) {
        icon.setBackgroundResource(isActive ? R.drawable.tc_bg_nav_pill_active : 0);
        @ColorInt int color = ContextCompat.getColor(host,
                isActive ? R.color.tc_red : R.color.tc_nav_icon_inactive);
        icon.setColorFilter(color);
    }
}
