package com.tiredcity.app.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Toast;
import com.tiredcity.app.data.model.ApiResponse;
import com.tiredcity.app.data.model.User;
import com.tiredcity.app.data.network.ApiClient;
import com.tiredcity.app.data.repository.AuthRepository;
import com.tiredcity.app.databinding.ActivityRegisterBinding;
import com.tiredcity.app.ui.base.BaseActivity;
import com.tiredcity.app.ui.main.MainActivity;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class RegisterActivity extends BaseActivity {

    private ActivityRegisterBinding binding;
    private AuthRepository authRepository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityRegisterBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        authRepository = new AuthRepository(
                ApiClient.getApiService(null),
                preferenceManager
        );

        binding.btnRegister.setOnClickListener(v -> {
            String name     = binding.etFullname.getText().toString().trim();
            String email    = binding.etEmail.getText().toString().trim();
            String password = binding.etPassword.getText().toString().trim();

            if (TextUtils.isEmpty(name)) {
                binding.etFullname.setError("Nhập họ và tên");
                return;
            }
            if (TextUtils.isEmpty(email)) {
                binding.etEmail.setError("Nhập email");
                return;
            }
            if (TextUtils.isEmpty(password) || password.length() < 6) {
                binding.etPassword.setError("Mật khẩu ít nhất 6 ký tự");
                return;
            }
            attemptRegister(email, password, name);
        });

        binding.tvLogin.setOnClickListener(v -> navigateToLogin());
    }

    private void attemptRegister(String email, String password, String fullName) {
        binding.btnRegister.setEnabled(false);
        authRepository.register(email, password, fullName).enqueue(new Callback<ApiResponse<User>>() {
            @Override
            public void onResponse(Call<ApiResponse<User>> call, Response<ApiResponse<User>> response) {
                binding.btnRegister.setEnabled(true);
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    User user = response.body().getData();
                    preferenceManager.saveToken(user.getToken());
                    preferenceManager.saveUserId(user.getId());
                    ApiClient.reset();
                    startActivity(new Intent(RegisterActivity.this, MainActivity.class));
                    finishAffinity();
                } else {
                    String msg = (response.body() != null) ? response.body().getMessage() : "Đăng ký thất bại";
                    Toast.makeText(RegisterActivity.this, msg, Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<User>> call, Throwable t) {
                binding.btnRegister.setEnabled(true);
                Toast.makeText(RegisterActivity.this, "Lỗi mạng: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void navigateToLogin() {
        startActivity(new Intent(this, LoginActivity.class));
        finish();
    }
}
