package com.tiredcity.app.ui.profile;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import com.bumptech.glide.Glide;
import com.tiredcity.app.data.model.ApiResponse;
import com.tiredcity.app.data.model.UserProfile;
import com.tiredcity.app.data.network.ApiClient;
import com.tiredcity.app.data.network.ApiService;
import com.tiredcity.app.databinding.ActivityProfileBinding;
import com.tiredcity.app.ui.auth.LoginActivity;
import com.tiredcity.app.ui.base.BaseActivity;
import com.tiredcity.app.utils.MenhCalculator;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ProfileActivity extends BaseActivity {

    private ActivityProfileBinding binding;
    private ApiService apiService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityProfileBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        binding.toolbar.setNavigationOnClickListener(v -> finish());

        apiService = ApiClient.getApiService(preferenceManager.getToken());

        binding.layoutEditProfile.setOnClickListener(v -> openEditProfile());
        binding.layoutOrderHistory.setOnClickListener(v -> openOrderHistory());
        binding.layoutWardrobe.setOnClickListener(v -> openWardrobe());
        binding.btnLogout.setOnClickListener(v -> logout());

        loadProfile();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadProfile(); // Refresh after edit
    }

    private void loadProfile() {
        // Try cached profile first
        UserProfile cached = preferenceManager.getUser();
        if (cached != null) bindProfile(cached);

        // Then fetch fresh from server
        apiService.getProfile().enqueue(new Callback<ApiResponse<UserProfile>>() {
            @Override
            public void onResponse(Call<ApiResponse<UserProfile>> call, Response<ApiResponse<UserProfile>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    UserProfile profile = response.body().getData();
                    preferenceManager.saveUser(profile);
                    bindProfile(profile);
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<UserProfile>> call, Throwable t) {
                // Silently fail — cached data already displayed
            }
        });
    }

    private void bindProfile(UserProfile profile) {
        binding.tvUserName.setText(profile.getDisplayName());
        binding.tvUserEmail.setText(profile.getEmail());

        // Menh badge
        String menh = preferenceManager.getMenh();
        if (menh != null) {
            String emoji = MenhCalculator.getEmojiMenh(menh);
            binding.tvMenhBadge.setText(emoji + " Mệnh " + menh);
            binding.tvMenhBadge.setVisibility(View.VISIBLE);
        }

        // Avatar
        if (profile.getAvatar() != null && !profile.getAvatar().isEmpty()) {
            Glide.with(this)
                .load(profile.getAvatar())
                .placeholder(com.tiredcity.app.R.drawable.ic_person_placeholder)
                .circleCrop()
                .into(binding.ivAvatar);
        }
    }

    private void openEditProfile() {
        startActivity(new Intent(this, EditProfileActivity.class));
    }

    private void openOrderHistory() {
        startActivity(new Intent(this, OrderHistoryActivity.class));
    }

    private void openWardrobe() {
        startActivity(new Intent(this, WardrobeActivity.class));
    }

    private void logout() {
        preferenceManager.clearToken();
        startActivity(new Intent(this, LoginActivity.class));
        finishAffinity();
    }
}
