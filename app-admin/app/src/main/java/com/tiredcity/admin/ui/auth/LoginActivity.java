package com.tiredcity.admin.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;

import androidx.appcompat.app.AppCompatActivity;

import com.tiredcity.admin.databinding.ActivityLoginBinding;
import com.tiredcity.admin.ui.dashboard.DashboardActivity;

/**
 * Man hinh dang nhap admin.
 * Neu duoc mo tu app khach hang (toggle "Quan tri" sau khi da nhap email/mat khau o do)
 * thi intent se kem san EXTRA_EMAIL/EXTRA_PASSWORD -> vao thang Dashboard, KHONG hien lai form.
 * Neu tu mo app nay truc tiep (icon rieng, khong qua app khach hang) thi van hien form
 * email/mat khau nhu binh thuong de test doc lap.
 * Luong Google Sign-In -> gui idToken sang backend (/auth/google) -> nhan JWT
 * -> luu vao SharedPreferences -> mo DashboardActivity.
 * (Phan tich hop Google Sign-In se hoan thien o Giai doan tiep theo.)
 */
public class LoginActivity extends AppCompatActivity {

    // Phai khop voi EXTRA_ADMIN_EMAIL/PASSWORD ben app khach hang (ui.auth.LoginActivity).
    private static final String EXTRA_EMAIL    = "com.tiredcity.admin.extra.EMAIL";
    private static final String EXTRA_PASSWORD = "com.tiredcity.admin.extra.PASSWORD";

    private ActivityLoginBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        String email = getIntent().getStringExtra(EXTRA_EMAIL);
        String password = getIntent().getStringExtra(EXTRA_PASSWORD);
        if (!TextUtils.isEmpty(email) && !TextUtils.isEmpty(password)) {
            // Da dang nhap san o man hinh chinh (app khach hang) -> vao thang Dashboard.
            startActivity(new Intent(this, DashboardActivity.class));
            finish();
            return;
        }

        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.btnLogin.setOnClickListener(v -> attemptLogin());

        binding.btnGoogleLogin.setOnClickListener(v -> {
            // TODO: khoi dong Google Sign-In, doi idToken lay JWT tu backend
            startActivity(new Intent(this, DashboardActivity.class));
        });
    }

    private void attemptLogin() {
        String email = binding.etEmail.getText() == null ? "" : binding.etEmail.getText().toString().trim();
        String password = binding.etPassword.getText() == null ? "" : binding.etPassword.getText().toString().trim();

        if (TextUtils.isEmpty(email)) {
            binding.etEmail.setError(getString(com.tiredcity.admin.R.string.error_required_email));
            return;
        }
        if (TextUtils.isEmpty(password)) {
            binding.etPassword.setError(getString(com.tiredcity.admin.R.string.error_required_password));
            return;
        }

        startActivity(new Intent(this, DashboardActivity.class));
    }
}
