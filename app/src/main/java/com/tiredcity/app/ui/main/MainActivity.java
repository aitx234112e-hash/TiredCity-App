package com.tiredcity.app.ui.main;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import androidx.annotation.ColorInt;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.navigation.NavController;
import androidx.navigation.NavOptions;
import androidx.navigation.fragment.NavHostFragment;
import com.tiredcity.app.R;
import com.tiredcity.app.databinding.ActivityMainBinding;
import com.tiredcity.app.ui.base.BaseActivity;
import com.tiredcity.app.ui.settings.PinActivity;
import com.tiredcity.app.utils.Constants;

public class MainActivity extends BaseActivity {

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

            binding.navShop.setOnClickListener(v -> navigateTab(R.id.shopFragment));
            binding.navStyling.setOnClickListener(v -> navigateTab(R.id.stylingFragment));
            binding.navHome.setOnClickListener(v -> navigateTab(R.id.homeFragment));
            binding.navExplore.setOnClickListener(v -> navigateTab(R.id.exploreFragment));
            binding.navProfile.setOnClickListener(v ->
                    startActivity(new Intent(this, com.tiredcity.app.ui.profile.ProfileActivity.class)));

            navController.addOnDestinationChangedListener((controller, destination, args) ->
                    updateSelected(destination.getId()));
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
        setIcon(binding.navShopIcon, destId == R.id.shopFragment);
        setIcon(binding.navStylingIcon, destId == R.id.stylingFragment);
        setIcon(binding.navHomeIcon, destId == R.id.homeFragment);
        setIcon(binding.navExploreIcon, destId == R.id.exploreFragment);
        // navProfile là Activity riêng, không có trạng thái chọn.
    }

    private void setIcon(ImageView icon, boolean active) {
        icon.setBackgroundResource(active ? R.drawable.tc_bg_nav_pill_active : 0);
        @ColorInt int color = ContextCompat.getColor(this,
                active ? R.color.tc_red : R.color.tc_nav_icon_inactive);
        icon.setColorFilter(color);
    }
}
