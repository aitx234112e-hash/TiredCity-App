package com.tiredcity.app.ui.settings;

import android.content.Intent;
import android.os.Bundle;
import com.tiredcity.app.R;
import com.tiredcity.app.databinding.ActivityGeneralSettingsBinding;
import com.tiredcity.app.ui.base.BaseActivity;
import com.tiredcity.app.utils.ThemeManager;

/**
 * Cài đặt chung (CHUNG) — menu: Bảo mật · Thông tin ứng dụng ·
 * Đánh giá ứng dụng · Yêu cầu xóa tài khoản.
 */
public class GeneralSettingsActivity extends BaseActivity {

    private ActivityGeneralSettingsBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityGeneralSettingsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.btnBack.setOnClickListener(v -> finish());

        // Chế độ màn hình tối — công tắc bật/tắt, lưu và áp dụng ngay.
        binding.rowDarkMode.tvLabel.setText(R.string.settings_general_dark_mode_title);
        binding.rowDarkMode.switchToggle.setChecked(ThemeManager.isDarkMode(this));
        binding.rowDarkMode.switchToggle.setOnClickListener(v -> {
            ThemeManager.toggleTheme(this);
            recreate();
        });

        binding.rowSecurity.tvLabel.setText(R.string.settings_general_security_title);
        binding.rowAppInfo.tvLabel.setText(R.string.settings_general_app_info_title);
        binding.rowRate.tvLabel.setText(R.string.settings_general_rate_title);
        binding.rowDelete.tvLabel.setText(R.string.settings_general_delete_title);

        binding.rowSecurity.getRoot().setOnClickListener(v ->
                startActivity(new Intent(this, SecuritySettingsActivity.class)));
        binding.rowAppInfo.getRoot().setOnClickListener(v ->
                startActivity(new Intent(this, AppInfoActivity.class)));
        binding.rowRate.getRoot().setOnClickListener(v ->
                new RateAppBottomSheet().show(getSupportFragmentManager(), "rate_app"));
        binding.rowDelete.getRoot().setOnClickListener(v ->
                startActivity(new Intent(this, AccountDeletionActivity.class)));
    }
}
