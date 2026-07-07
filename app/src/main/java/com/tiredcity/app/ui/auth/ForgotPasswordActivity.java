package com.tiredcity.app.ui.auth;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.core.text.HtmlCompat;

import com.google.firebase.auth.FirebaseAuth;
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

        String prefillEmail = getIntent().getStringExtra("EMAIL");
        if (prefillEmail != null) {
            binding.etEmail.setText(prefillEmail);
        }

        setupEmailHistory();

        binding.btnSend.setOnClickListener(v -> {
            String email = binding.etEmail.getText().toString().trim();
            if (TextUtils.isEmpty(email) || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                binding.etEmail.setError(getString(
                        com.tiredcity.app.R.string.forgot_pw_error_email));
                return;
            }
            
            sendResetEmail(email);
        });
    }

    private void sendResetEmail(String email) {
        hideKeyboard();
        binding.btnSend.setEnabled(false);
        binding.pbLoading.setVisibility(View.VISIBLE);

        FirebaseAuth.getInstance().sendPasswordResetEmail(email)
                .addOnCompleteListener(task -> {
                    binding.btnSend.setEnabled(true);
                    binding.pbLoading.setVisibility(View.GONE);
                    if (task.isSuccessful()) {
                        // Lưu email vào lịch sử
                        preferenceManager.saveEmailToHistory(email);

                        // Nếu đang đăng nhập, tự động logout để buộc login lại với pass mới
                        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
                            FirebaseAuth.getInstance().signOut();
                            preferenceManager.clearToken();
                        }
                        showSuccess(email);
                    } else {
                        String error = task.getException() != null ? task.getException().getMessage() : "Unknown error";
                        Toast.makeText(this, "Lỗi: " + error, Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void hideKeyboard() {
        View view = this.getCurrentFocus();
        if (view != null) {
            InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }

    private void setupEmailHistory() {
        java.util.List<String> history = preferenceManager.getEmailHistory();
        if (!history.isEmpty()) {
            ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                    android.R.layout.simple_dropdown_item_1line, history);
            binding.etEmail.setAdapter(adapter);
            
            // Hiện dropdown ngay khi focus nếu có lịch sử
            binding.etEmail.setOnFocusChangeListener((v, hasFocus) -> {
                if (hasFocus && binding.etEmail.getText().toString().isEmpty()) {
                    binding.etEmail.showDropDown();
                }
            });
            binding.etEmail.setOnClickListener(v -> {
                if (binding.etEmail.getText().toString().isEmpty()) {
                    binding.etEmail.showDropDown();
                }
            });
        }
    }

    /**
     * Hiển thị trạng thái "đã gửi liên kết". Offline — không gọi backend.
     * Khi có backend thật, gọi API gửi email đặt lại trước khi hiện màn này.
     */
    private void showSuccess(String email) {
        binding.inputGroup.setVisibility(View.GONE);
        binding.tvBackLogin.setVisibility(View.GONE);
        binding.successGroup.setVisibility(View.VISIBLE);
        
        String instruction = "Chúng tôi đã gửi một <b>liên kết đặt lại mật khẩu</b> đến email: <b>" + email + "</b>" + 
                "<br/><br/>1. Vui lòng kiểm tra hộp thư (bao gồm cả thư rác).<br/>" +
                "2. Nhấn vào liên kết trong email để tạo mật khẩu mới.<br/>" +
                "3. Sau khi hoàn tất, hãy quay lại đây để đăng nhập.";
        
        binding.tvSuccessDesc.setText(HtmlCompat.fromHtml(instruction, HtmlCompat.FROM_HTML_MODE_LEGACY));
    }
}
