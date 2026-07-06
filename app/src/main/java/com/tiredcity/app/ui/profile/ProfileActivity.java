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
import com.tiredcity.app.ui.explore.ArticleActivity;
import com.tiredcity.app.ui.explore.EventActivity;
import com.tiredcity.app.ui.reward.RewardActivity;
import com.tiredcity.app.ui.settings.GeneralSettingsActivity;
import com.tiredcity.app.ui.settings.NotificationSettingsActivity;
import com.tiredcity.app.ui.styling.AiStylingActivity;
import com.tiredcity.app.ui.styling.ChatBotActivity;
import com.tiredcity.app.ui.support.ContactActivity;
import com.tiredcity.app.ui.support.PolicyActivity;
import com.tiredcity.app.ui.support.TermsActivity;
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

        // Header (avatar + tên) → chỉnh sửa hồ sơ
        binding.layoutAccountHeader.setOnClickListener(v -> openEditProfile());

        // Lưới truy cập nhanh — mục đã code
        binding.cardStyling.setOnClickListener(v ->
                startActivity(new Intent(this, AiStylingActivity.class)));
        binding.cardOrders.setOnClickListener(v -> openOrderHistory());
        binding.cardFindStore.setOnClickListener(v ->
                startActivity(new Intent(this, ContactActivity.class)));

        // Sự kiện → trang Sự kiện (EventActivity, dùng item_event.xml)
        binding.itemEvents.setOnClickListener(v ->
                startActivity(new Intent(this, EventActivity.class)));
        // Ưu đãi → trang Ưu đãi (RewardActivity)
        binding.itemPromotions.setOnClickListener(v ->
                startActivity(new Intent(this, RewardActivity.class)));
        // Blogs → trang danh sách bài viết (ArticleActivity)
        binding.itemBlogs.setOnClickListener(v ->
                startActivity(new Intent(this, ArticleActivity.class)));

        // Các mục đã code → mở trang tương ứng
        binding.layoutOrderHistory.setOnClickListener(v -> openOrderHistory());
        binding.layoutWardrobe.setOnClickListener(v -> openWardrobe());
        binding.layoutChat.setOnClickListener(v ->
                startActivity(new Intent(this, ChatBotActivity.class)));
        binding.layoutContact.setOnClickListener(v ->
                startActivity(new Intent(this, ContactActivity.class)));
        binding.layoutPolicy.setOnClickListener(v ->
                startActivity(new Intent(this, PolicyActivity.class)));
        binding.layoutTerms.setOnClickListener(v ->
                startActivity(new Intent(this, TermsActivity.class)));

        // Cài đặt thông báo & Cài đặt chung → các module mới
        binding.layoutNotifications.setOnClickListener(v ->
                startActivity(new Intent(this, NotificationSettingsActivity.class)));
        binding.layoutSettings.setOnClickListener(v ->
                startActivity(new Intent(this, GeneralSettingsActivity.class)));

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
            binding.tvMenhBadge.setText(emoji + " " + getString(
                    com.tiredcity.app.R.string.menh_label, menh));
            binding.tvMenhBadge.setVisibility(View.VISIBLE);
        }

        // Avatar: ảnh đã chọn nếu có, ngược lại logo gà TiredCity
        com.tiredcity.app.utils.AvatarUtils.load(this, binding.ivAvatar);
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
