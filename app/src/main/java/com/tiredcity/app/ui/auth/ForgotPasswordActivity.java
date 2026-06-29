package com.tiredcity.app.ui.auth;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.View;
import com.tiredcity.app.databinding.ActivityForgotPasswordBinding;
import com.tiredcity.app.ui.base.BaseActivity;

public class ForgotPasswordActivity extends BaseActivity {

    private ActivityForgotPasswordBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityForgotPasswordBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.btnBack.setOnClickListener(v -> finish());
        binding.tvBackLogin.setOnClickListener(v -> finish());
        binding.btnBackToLogin.setOnClickListener(v -> finish());

        binding.btnSend.setOnClickListener(v -> {
            String email = binding.etEmail.getText().toString().trim();
            if (TextUtils.isEmpty(email) || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                binding.etEmail.setError(getString(
                        com.tiredcity.app.R.string.forgot_pw_error_email));
                return;
            }
            showSuccess(email);
        });
    }

    /**
     * Hiển thị trạng thái "đã gửi liên kết". Offline — không gọi backend.
     * Khi có backend thật, gọi API gửi email đặt lại trước khi hiện màn này.
     */
    private void showSuccess(String email) {
        binding.inputGroup.setVisibility(View.GONE);
        binding.tvBackLogin.setVisibility(View.GONE);
        binding.successGroup.setVisibility(View.VISIBLE);
        binding.tvSuccessDesc.setText(getString(
                com.tiredcity.app.R.string.forgot_pw_success_desc, email));
    }
}
