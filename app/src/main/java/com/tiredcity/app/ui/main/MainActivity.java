package com.tiredcity.app.ui.main;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import androidx.annotation.Nullable;
import androidx.navigation.NavController;
import androidx.navigation.NavOptions;
import androidx.navigation.fragment.NavHostFragment;
import com.tiredcity.app.R;
import com.tiredcity.app.databinding.ActivityMainBinding;
import com.tiredcity.app.ui.base.BaseActivity;
import com.tiredcity.app.ui.common.BottomNavBar;
import com.tiredcity.app.ui.settings.PinActivity;
import com.tiredcity.app.utils.Constants;

public class MainActivity extends BaseActivity {

    /** Extra: id danh mục cần mở sẵn ở tab "Danh mục" — dùng khi một Activity khác (vd. chip
     *  tên danh mục ở màn Tìm kiếm) muốn quay lại đây và nhảy thẳng vào đúng nhóm trang phục. */
    public static final String EXTRA_OPEN_CATEGORY_ID = "open_category_id";

    /** Extra: tên {@link BottomNavBar.Tab} cần mở — dùng khi bấm tab trên thanh nav của
     *  SearchActivity/ProfileActivity, vì 3 tab đó chỉ điều hướng được từ NavController ở đây. */
    public static final String EXTRA_OPEN_TAB = "open_tab";

    private ActivityMainBinding binding;
    private NavController navController;
    private static final int RC_PIN_VERIFY = 1001;
    private boolean pinVerified = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Kiểm tra PIN nếu đã bật
        if (preferenceManager.getToggle(Constants.KEY_PIN_UNLOCK, false) && !pinVerified) {
            Intent intent = new Intent(this, PinActivity.class);
            intent.putExtra("MODE", "VERIFY");
            startActivityForResult(intent, RC_PIN_VERIFY);
            // Ẩn nội dung chính trong lúc chờ PIN
            binding.getRoot().setVisibility(View.INVISIBLE);
        } else {
            initUI();
        }
    }

    private void initUI() {
        binding.getRoot().setVisibility(View.VISIBLE);

        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager()
                .findFragmentById(R.id.nav_host_fragment);
        if (navHostFragment != null) {
            navController = navHostFragment.getNavController();

            // Icon "Tìm kiếm"/"Tôi" mở Activity riêng, 3 icon còn lại đổi destination trong nav_graph.
            BottomNavBar.bind(this, binding.bottomNavBar, tabOf(currentDestinationId()),
                    tab -> navigateTab(destinationOf(tab)));

            navController.addOnDestinationChangedListener((controller, destination, args) ->
                    BottomNavBar.setSelected(this, binding.bottomNavBar, tabOf(destination.getId())));

            handleOpenTabIntent(getIntent());
            handleOpenCategoryIntent(getIntent());
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == RC_PIN_VERIFY) {
            if (resultCode == RESULT_OK) {
                pinVerified = true;
                initUI();
            } else {
                finish(); // Không nhập đúng PIN thì thoát app
            }
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleOpenTabIntent(intent);
        handleOpenCategoryIntent(intent);
    }

    /** Bấm tab Trang chủ/Phối đồ/Khám phá trên thanh nav của SearchActivity/ProfileActivity. */
    private void handleOpenTabIntent(Intent intent) {
        String tabName = intent.getStringExtra(EXTRA_OPEN_TAB);
        // navController còn null khi onNewIntent chạy lúc màn PIN đang chặn (initUI chưa gọi).
        if (tabName == null || navController == null) return;
        intent.removeExtra(EXTRA_OPEN_TAB); // tránh mở lại tab này ở lần onNewIntent kế tiếp
        navigateTab(destinationOf(BottomNavBar.Tab.valueOf(tabName)));
    }

    /** Nếu Intent mang theo {@link #EXTRA_OPEN_CATEGORY_ID} thì mở thẳng tab Danh mục và chọn
     *  sẵn đúng nhóm trang phục đó (vd. bấm chip tên danh mục ở "gợi ý từ khóa" trong Tìm kiếm). */
    private void handleOpenCategoryIntent(Intent intent) {
        String categoryId = intent.getStringExtra(EXTRA_OPEN_CATEGORY_ID);
        if (categoryId == null || navController == null) return;

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

    private int currentDestinationId() {
        return navController.getCurrentDestination() != null
                ? navController.getCurrentDestination().getId()
                : navController.getGraph().getStartDestinationId();
    }

    /** Destination đang mở → mục cần tô đậm trên thanh nav. */
    private static BottomNavBar.Tab tabOf(int destId) {
        if (destId == R.id.stylingFragment) return BottomNavBar.Tab.STYLING;
        if (destId == R.id.homeFragment) return BottomNavBar.Tab.HOME;
        if (destId == R.id.exploreFragment) return BottomNavBar.Tab.EXPLORE;
        return BottomNavBar.Tab.NONE; // vd. aboutFragment — không ứng với mục nào
    }

    /** Mục trên thanh nav → destination trong nav_graph (chỉ 3 tab nằm trong graph). */
    private static int destinationOf(BottomNavBar.Tab tab) {
        switch (tab) {
            case STYLING: return R.id.stylingFragment;
            case EXPLORE: return R.id.exploreFragment;
            case HOME:
            default: return R.id.homeFragment;
        }
    }
}
