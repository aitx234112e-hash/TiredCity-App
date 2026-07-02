package com.tiredcity.app.ui.settings;

import android.os.Bundle;
import com.tiredcity.app.databinding.ActivityAppInfoBinding;
import com.tiredcity.app.ui.base.BaseActivity;

/**
 * Thông tin ứng dụng — phiên bản, release notes, ngày cập nhật.
 */
public class AppInfoActivity extends BaseActivity {

    private ActivityAppInfoBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityAppInfoBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.btnBack.setOnClickListener(v -> finish());
    }
}
