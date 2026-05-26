package com.tiredcity.app.ui.onboarding;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import com.tiredcity.app.databinding.ActivitySplashBinding;
import com.tiredcity.app.ui.base.BaseActivity;
import com.tiredcity.app.ui.auth.LoginActivity;
import com.tiredcity.app.ui.main.MainActivity;

public class SplashActivity extends BaseActivity {

    private static final long SPLASH_DELAY_MS = 2000;
    private ActivitySplashBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySplashBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        new Handler(Looper.getMainLooper()).postDelayed(this::navigateNext, SPLASH_DELAY_MS);
    }

    private void navigateNext() {
        if (!preferenceManager.isOnboardingShown()) {
            startActivity(new Intent(this, OnboardingActivity.class));
        } else if (preferenceManager.isLoggedIn()) {
            startActivity(new Intent(this, MainActivity.class));
        } else {
            startActivity(new Intent(this, LoginActivity.class));
        }
        finish();
    }
}
