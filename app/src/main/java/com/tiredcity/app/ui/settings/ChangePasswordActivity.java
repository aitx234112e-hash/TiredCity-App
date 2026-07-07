package com.tiredcity.app.ui.settings;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Toast;

import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserInfo;
import com.tiredcity.app.databinding.ActivityChangePasswordBinding;
import com.tiredcity.app.ui.auth.ForgotPasswordActivity;
import com.tiredcity.app.ui.base.BaseActivity;

public class ChangePasswordActivity extends BaseActivity {

    private ActivityChangePasswordBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityChangePasswordBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        binding.toolbar.setNavigationOnClickListener(v -> finish());

        checkProvider();

        binding.tvForgotPassword.setOnClickListener(v -> {
            Intent intent = new Intent(this, ForgotPasswordActivity.class);
            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            if (user != null && user.getEmail() != null) {
                intent.putExtra("EMAIL", user.getEmail());
            }
            startActivity(intent);
        });

        binding.btnUpdate.setOnClickListener(v -> attemptUpdatePassword());
        
        binding.btnCreatePassword.setOnClickListener(v -> {
            Intent intent = new Intent(this, ForgotPasswordActivity.class);
            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            if (user != null && user.getEmail() != null) {
                intent.putExtra("EMAIL", user.getEmail());
            }
            startActivity(intent);
        });
    }

    private void checkProvider() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        boolean isEmailProvider = false;
        for (UserInfo profile : user.getProviderData()) {
            if (EmailAuthProvider.PROVIDER_ID.equals(profile.getProviderId())) {
                isEmailProvider = true;
                break;
            }
        }

        if (!isEmailProvider) {
            // Social login user - show custom layout
            binding.layoutEmailAuth.setVisibility(View.GONE);
            binding.layoutSocialAuth.setVisibility(View.VISIBLE);
        }
    }

    private void attemptUpdatePassword() {
        String currentPw = binding.etCurrentPassword.getText().toString().trim();
        String newPw = binding.etNewPassword.getText().toString().trim();
        String confirmPw = binding.etConfirmPassword.getText().toString().trim();

        if (TextUtils.isEmpty(currentPw)) {
            binding.etCurrentPassword.setError("Nhập mật khẩu hiện tại");
            return;
        }
        if (TextUtils.isEmpty(newPw) || newPw.length() < 6) {
            binding.etNewPassword.setError("Mật khẩu mới ít nhất 6 ký tự");
            return;
        }
        if (!newPw.equals(confirmPw)) {
            binding.etConfirmPassword.setError("Mật khẩu xác nhận không khớp");
            return;
        }

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null || user.getEmail() == null) {
            finish();
            return;
        }

        binding.btnUpdate.setEnabled(false);

        // Re-authenticate user
        AuthCredential credential = EmailAuthProvider.getCredential(user.getEmail(), currentPw);
        user.reauthenticate(credential).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                // Update password
                user.updatePassword(newPw).addOnCompleteListener(updateTask -> {
                    binding.btnUpdate.setEnabled(true);
                    if (updateTask.isSuccessful()) {
                        Toast.makeText(this, "Đổi mật khẩu thành công", Toast.LENGTH_SHORT).show();
                        finish();
                    } else {
                        String error = updateTask.getException() != null ? updateTask.getException().getMessage() : "Unknown error";
                        Toast.makeText(this, "Lỗi: " + error, Toast.LENGTH_LONG).show();
                    }
                });
            } else {
                binding.btnUpdate.setEnabled(true);
                Toast.makeText(this, "Mật khẩu hiện tại không chính xác", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
