package com.tiredcity.app.ui.settings;

import android.os.Bundle;
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

        // Mặc định TẮT.
        binding.switchPin.setChecked(preferenceManager.getToggle(Constants.KEY_PIN_UNLOCK, false));
        binding.rowPin.setOnClickListener(v -> binding.switchPin.toggle());
        binding.switchPin.setOnCheckedChangeListener((btn, isChecked) ->
                preferenceManager.setToggle(Constants.KEY_PIN_UNLOCK, isChecked));
    }
}
