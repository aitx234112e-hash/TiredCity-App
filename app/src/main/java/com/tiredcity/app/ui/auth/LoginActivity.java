package com.tiredcity.app.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Toast;
import com.tiredcity.app.data.model.ApiResponse;
import com.tiredcity.app.data.model.User;
import com.tiredcity.app.data.network.ApiClient;
import com.tiredcity.app.data.repository.AuthRepository;
import com.tiredcity.app.databinding.ActivityLoginBinding;
import com.tiredcity.app.ui.base.BaseActivity;
import com.tiredcity.app.ui.main.MainActivity;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LoginActivity extends BaseActivity {

    private ActivityLoginBinding binding;
    private AuthRepository authRepository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        authRepository = new AuthRepository(
                ApiClient.getApiService(null),
                preferenceManager
        );

        binding.btnLogin.setOnClickListener(v -> {
            String email    = binding.etEmail.getText().toString().trim();
            String password = binding.etPassword.getText().toString().trim();

            if (TextUtils.isEmpty(email)) {
                binding.etEmail.setError("Nhập email");
                return;
            }
            if (TextUtils.isEmpty(password)) {
                binding.etPassword.setError("Nhập mật khẩu");
                return;
            }
            attemptLogin(email, password);
        });

        binding.tvRegister.setOnClickListener(v -> navigateToRegister());
    }

    private void attemptLogin(String email, String password) {
        binding.btnLogin.setEnabled(false);
        authRepository.login(email, password).enqueue(new Callback<ApiResponse<User>>() {
            @Override
            public void onResponse(Call<ApiResponse<User>> call, Response<ApiResponse<User>> response) {
                binding.btnLogin.setEnabled(true);
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    User user = response.body().getData();
                    preferenceManager.saveToken(user.getToken());
                    preferenceManager.saveUserId(user.getId());
                    ApiClient.reset();
                    startActivity(new Intent(LoginActivity.this, MainActivity.class));
                    finishAffinity();
                } else {
                    String msg = (response.body() != null) ? response.body().getMessage() : "Đăng nhập thất bại";
                    Toast.makeText(LoginActivity.this, msg, Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<User>> call, Throwable t) {
                binding.btnLogin.setEnabled(true);
                Toast.makeText(LoginActivity.this, "Lỗi mạng: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void navigateToRegister() {
        startActivity(new Intent(this, RegisterActivity.class));
    }
}
