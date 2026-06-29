package com.tiredcity.app.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Toast;
import com.tiredcity.app.data.model.ApiResponse;
import com.tiredcity.app.data.model.User;
import com.tiredcity.app.data.model.UserProfile;
import com.tiredcity.app.data.network.ApiClient;
import com.tiredcity.app.data.repository.AuthRepository;
import com.tiredcity.app.databinding.ActivityLoginBinding;
import com.tiredcity.app.ui.base.BaseActivity;
import com.tiredcity.app.ui.main.MainActivity;
import com.tiredcity.app.ui.onboarding.OnboardingActivity;
import com.tiredcity.app.utils.LocaleHelper;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LoginActivity extends BaseActivity {

    // Dev bypass — chấp nhận đăng nhập offline mà không gọi API.
    // Hữu ích khi backend tiredcity.vn chưa có endpoint /api/auth/login.
    private static final String DEMO_EMAIL    = "demo@demo.com";
    private static final String DEMO_PASSWORD = "123456";
    private static final String DEMO_TOKEN    = "demo-token-local";
    private static final String DEMO_USER_ID  = "demo-user";

    private ActivityLoginBinding binding;
    private AuthRepository authRepository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Đã đăng nhập (bản release) → vào thẳng Main, không hiện form đăng nhập.
        if (preferenceManager.isLoggedIn()) {
            startActivity(new Intent(this, MainActivity.class));
            finish();
            return;
        }

        // Hâm nóng tầng mạng (OkHttp/Retrofit) trên luồng nền trong lúc splash overlay hiển thị.
        new Thread(() -> {
            try { ApiClient.getApiService(null); } catch (Exception ignored) { }
        }, "api-warmup").start();

        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // AuthRepository/Retrofit tạo lazy trong attemptOnlineLogin() (đăng nhập offline không dùng).

        restoreSavedCredentials();

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

            persistRememberMe(email, password);
            attemptLogin(email, password);
        });

        binding.tvRegister.setOnClickListener(v -> navigateToRegister());
        binding.tvForgotPassword.setOnClickListener(v ->
                startActivity(new Intent(this, ForgotPasswordActivity.class)));

        binding.btnLanguage.setOnClickListener(v -> {
            LocaleHelper.toggleLanguage(this);
            recreate();
        });

        // Đăng nhập mạng xã hội (demo — backend thật cần SDK Google/Facebook/Apple).
        binding.btnGoogle.setOnClickListener(v -> socialLogin("Google", "user@gmail.com"));
        binding.btnFacebook.setOnClickListener(v -> socialLogin("Facebook", "user@facebook.com"));
        binding.btnApple.setOnClickListener(v -> socialLogin("Apple", "user@icloud.com"));

        setupSplashOverlay();
    }

    /**
     * Splash overlay (thay cho SplashActivity cũ): chỉ hiện khi mở app từ launcher,
     * giữ 1.8s rồi mờ dần để lộ form. Vì cùng 1 Activity nên KHÔNG có khung đen.
     */
    private void setupSplashOverlay() {
        boolean fromLauncher = getIntent() != null
                && Intent.ACTION_MAIN.equals(getIntent().getAction())
                && getIntent().hasCategory(Intent.CATEGORY_LAUNCHER);

        if (!fromLauncher) {
            binding.splashOverlay.setVisibility(View.GONE);
            return;
        }

        binding.splashOverlay.setVisibility(View.VISIBLE);
        binding.splashOverlay.postDelayed(() -> {
            if (isFinishing() || binding == null) return;
            binding.splashOverlay.animate()
                    .alpha(0f)
                    .setDuration(450)
                    .withEndAction(() -> {
                        if (binding != null) binding.splashOverlay.setVisibility(View.GONE);
                    })
                    .start();
        }, 1800);
    }

    private void restoreSavedCredentials() {
        if (preferenceManager.isRememberMeEnabled()) {
            binding.etEmail.setText(preferenceManager.getSavedEmail());
            binding.etPassword.setText(preferenceManager.getSavedPassword());
            binding.cbRememberMe.setChecked(true);
        }
    }

    private void persistRememberMe(String email, String password) {
        if (binding.cbRememberMe.isChecked()) {
            preferenceManager.setRememberMe(true);
            preferenceManager.saveCredentials(email, password);
        } else {
            preferenceManager.clearCredentials();
        }
    }

    private void attemptLogin(String email, String password) {
        // ⚠️ Backend tiredcity.vn KHÔNG có API auth → luôn đăng nhập offline cho MỌI tài khoản.
        // Khi có backend thật, thay dòng dưới bằng: attemptOnlineLogin(email, password);
        loginOffline(email);
    }

    /** Đăng nhập cục bộ, không cần mạng — dùng cho bản demo giao diện. */
    private void loginOffline(String email) {
        preferenceManager.saveToken(DEMO_TOKEN);
        preferenceManager.saveUserId(TextUtils.isEmpty(email) ? DEMO_USER_ID : email);

        // Hiển thị TÊN khách ở Home:
        //  - Nếu đã ĐĂNG KÝ (hồ sơ có sẵn tên đầy đủ) → GIỮ NGUYÊN tên đó.
        //  - Nếu chưa từng đăng ký → tạm lấy phần trước dấu @ làm tên. Không còn "Guest".
        UserProfile profile = preferenceManager.getUser();
        boolean hasRealName = profile != null
                && profile.getName() != null && !profile.getName().trim().isEmpty();
        if (!hasRealName) {
            if (profile == null) profile = new UserProfile();
            profile.setEmail(email);
            String raw = email.contains("@") ? email.substring(0, email.indexOf('@')) : email;
            profile.setName(prettifyName(raw));
            preferenceManager.saveUser(profile);
        }

        ApiClient.reset();
        startActivity(new Intent(LoginActivity.this, OnboardingActivity.class));
        finishAffinity();
    }

    /**
     * Đăng nhập mạng xã hội — bản demo offline.
     * Khi có backend: thay bằng Google Sign-In / Facebook Login SDK / Apple, lấy token rồi gọi server.
     */
    private void socialLogin(String provider, String email) {
        preferenceManager.saveToken(DEMO_TOKEN);
        preferenceManager.saveUserId(email);

        UserProfile profile = preferenceManager.getUser();
        boolean hasRealName = profile != null
                && profile.getName() != null && !profile.getName().trim().isEmpty();
        if (!hasRealName) {
            if (profile == null) profile = new UserProfile();
            profile.setEmail(email);
            profile.setName(provider + " User");
            preferenceManager.saveUser(profile);
        }

        ApiClient.reset();
        Toast.makeText(this, provider, Toast.LENGTH_SHORT).show();
        startActivity(new Intent(LoginActivity.this, OnboardingActivity.class));
        finishAffinity();
    }

    /** Biến phần email thành tên dễ nhìn: "nguyen_van.a" → "Nguyen Van A". */
    private String prettifyName(String raw) {
        if (raw == null || raw.trim().isEmpty()) return getString(com.tiredcity.app.R.string.greeting_guest);
        raw = raw.replace('.', ' ').replace('_', ' ').trim();
        StringBuilder sb = new StringBuilder();
        for (String part : raw.split("\\s+")) {
            if (part.isEmpty()) continue;
            sb.append(Character.toUpperCase(part.charAt(0)));
            if (part.length() > 1) sb.append(part.substring(1));
            sb.append(' ');
        }
        return sb.toString().trim();
    }

    /** Đăng nhập qua API thật. Hiện chưa dùng vì backend chưa có endpoint auth. */
    @SuppressWarnings("unused")
    private void attemptOnlineLogin(String email, String password) {
        if (authRepository == null) {
            authRepository = new AuthRepository(ApiClient.getApiService(null), preferenceManager);
        }
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
                    startActivity(new Intent(LoginActivity.this, OnboardingActivity.class));
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
