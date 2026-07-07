package com.tiredcity.app.ui.main;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageView;
import androidx.annotation.ColorInt;
import androidx.core.content.ContextCompat;
import androidx.navigation.NavController;
import androidx.navigation.NavOptions;
import androidx.navigation.fragment.NavHostFragment;
import com.tiredcity.app.R;
import com.tiredcity.app.databinding.ActivityMainBinding;
import com.tiredcity.app.ui.base.BaseActivity;

public class MainActivity extends BaseActivity {

    /** Extra: id danh mục cần mở sẵn ở tab "Danh mục" — dùng khi một Activity khác (vd. chip
     *  tên danh mục ở màn Tìm kiếm) muốn quay lại đây và nhảy thẳng vào đúng nhóm trang phục. */
    public static final String EXTRA_OPEN_CATEGORY_ID = "open_category_id";

    private ActivityMainBinding binding;
    private NavController navController;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager()
                .findFragmentById(R.id.nav_host_fragment);
        navController = navHostFragment.getNavController();

        // Icon "tìm kiếm" ở bottom nav mở thẳng màn Tìm kiếm đầy đủ (SearchActivity) thay vì
        // chuyển tab — giống cách navProfile mở ProfileActivity riêng, không phải nav destination.
        binding.navShop.setOnClickListener(v -> startActivity(
                new Intent(this, com.tiredcity.app.ui.shop.SearchActivity.class)));
        binding.navStyling.setOnClickListener(v -> navigateTab(R.id.stylingFragment));
        binding.navHome.setOnClickListener(v -> navigateTab(R.id.homeFragment));
        binding.navExplore.setOnClickListener(v -> navigateTab(R.id.exploreFragment));
        binding.navProfile.setOnClickListener(v ->
                startActivity(new Intent(this, com.tiredcity.app.ui.profile.ProfileActivity.class)));

        navController.addOnDestinationChangedListener((controller, destination, args) ->
                updateSelected(destination.getId()));

        handleOpenCategoryIntent(getIntent());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleOpenCategoryIntent(intent);
    }

    /** Nếu Intent mang theo {@link #EXTRA_OPEN_CATEGORY_ID} thì mở thẳng tab Danh mục và chọn
     *  sẵn đúng nhóm trang phục đó (vd. bấm chip tên danh mục ở "gợi ý từ khóa" trong Tìm kiếm). */
    private void handleOpenCategoryIntent(Intent intent) {
        String categoryId = intent.getStringExtra(EXTRA_OPEN_CATEGORY_ID);
        if (categoryId == null) return;

        Bundle args = new Bundle();
        args.putString(StylingFragment.ARG_CATEGORY_ID, categoryId);
        // Không setRestoreState(true): thao tác này phải luôn hiện đúng danh mục vừa bấm, không
        // được khôi phục lại instance StylingFragment đã lưu trước đó (đang đứng ở tab khác).
        NavOptions options = new NavOptions.Builder()
                .setLaunchSingleTop(true)
                .setPopUpTo(navController.getGraph().getStartDestinationId(), false, false)
                .build();
        navController.navigate(R.id.stylingFragment, args, options);
    }

    /** Điều hướng tab theo kiểu bottom-nav: single-top + lưu/khôi phục trạng thái. */
    private void navigateTab(int destId) {
        if (navController.getCurrentDestination() != null
                && navController.getCurrentDestination().getId() == destId) {
            return;
        }
        NavOptions options = new NavOptions.Builder()
                .setLaunchSingleTop(true)
                .setRestoreState(true)
                .setPopUpTo(navController.getGraph().getStartDestinationId(), false, true)
                .build();
        navController.navigate(destId, null, options);
    }

    /** Tab đang chọn: icon đỏ đậm + nền pill sáng; còn lại đỏ nhạt. */
    private void updateSelected(int destId) {
        setIcon(binding.navStylingIcon, destId == R.id.stylingFragment);
        setIcon(binding.navHomeIcon, destId == R.id.homeFragment);
        setIcon(binding.navExploreIcon, destId == R.id.exploreFragment);
        // navShop (mở SearchActivity) và navProfile là Activity riêng, không có trạng thái chọn.
    }

    private void setIcon(ImageView icon, boolean active) {
        icon.setBackgroundResource(active ? R.drawable.tc_bg_nav_pill_active : 0);
        @ColorInt int color = ContextCompat.getColor(this,
                active ? R.color.tc_red : R.color.tc_nav_icon_inactive);
        icon.setColorFilter(color);
    }
}
