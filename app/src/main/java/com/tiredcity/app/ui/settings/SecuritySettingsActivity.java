package com.tiredcity.app.ui.settings;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;
import com.tiredcity.app.databinding.ActivitySecuritySettingsBinding;
import com.tiredcity.app.ui.base.BaseActivity;
import com.tiredcity.app.utils.Constants;

/**
 * Bảo mật (BẢO MẬT) — công tắc "Mở khóa bằng mã PIN" + ghi chú Face ID/vân tay.
 */
public class SecuritySettingsActivity extends BaseActivity {

    private ActivitySecuritySettingsBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySecuritySettingsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.btnBack.setOnClickListener(v -> finish());

        refreshSwitch();

        // Chặn người dùng gạt trực tiếp switch, buộc phải qua logic hàng (row)
        binding.switchPin.setClickable(false);
        binding.switchPin.setFocusable(false);

        binding.rowPin.setOnClickListener(v -> {
            boolean isEnabled = preferenceManager.getToggle(Constants.KEY_PIN_UNLOCK, false);
            if (!isEnabled) {
                // Mở màn hình thiết lập PIN mới
                Intent intent = new Intent(this, PinActivity.class);
                intent.putExtra("MODE", "SETUP");
                startActivity(intent);
            } else {
                // Yêu cầu nhập lại PIN cũ để xác nhận TẮT
                Intent intent = new Intent(this, PinActivity.class);
                intent.putExtra("MODE", "VERIFY");
                intent.putExtra("PURPOSE", "TURN_OFF");
                startActivity(intent);
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshSwitch();
    }

    private void refreshSwitch() {
        binding.switchPin.setChecked(preferenceManager.getToggle(Constants.KEY_PIN_UNLOCK, false));
    }
}
