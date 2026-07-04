package com.tiredcity.app.ui.profile;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;
import com.bumptech.glide.Glide;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.tiredcity.app.R;
import com.tiredcity.app.data.model.ApiResponse;
import com.tiredcity.app.data.model.UserProfile;
import com.tiredcity.app.data.network.ApiClient;
import com.tiredcity.app.data.network.ApiService;
import com.tiredcity.app.data.repository.AuthRepository;
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
    private AuthRepository authRepository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityProfileBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        binding.toolbar.setNavigationOnClickListener(v -> finish());

        apiService = ApiClient.getApiService(preferenceManager.getToken());
        authRepository = new AuthRepository(apiService, preferenceManager);

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
        // 1. Dùng cache cục bộ trước để hiện ngay UI
        UserProfile cached = preferenceManager.getUser();
        if (cached != null) bindProfile(cached);

        // 2. Đồng bộ từ Firestore (Cloud) - tin cậy hơn nếu Backend API chưa sẵn sàng
        authRepository.syncAccount(cached, profile -> {
            if (profile != null) {
                runOnUiThread(() -> bindProfile(profile));
            }
        });

        // 3. Gọi Backend API (nếu có)
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

        // Kiểm tra email đã xác thực chưa
        com.google.firebase.auth.FirebaseUser user = com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser();
        if (user != null && !user.isEmailVerified()) {
            binding.tvUserEmail.append(" (Chưa xác thực)");
            binding.tvUserEmail.setTextColor(getResources().getColor(R.color.tc_red, getTheme()));
            
            // Cho phép nhấn vào email để gửi lại mã xác thực
            binding.tvUserEmail.setOnClickListener(v -> resendVerificationEmail(user));
        } else {
            binding.tvUserEmail.setTextColor(getResources().getColor(R.color.tc_on_red_secondary, getTheme()));
            binding.tvUserEmail.setOnClickListener(null);
        }

        // TỰ ĐỘNG TÍNH LẠI MỆNH ĐỂ SỬA DỮ LIỆU CŨ SAI LỆCH
        int birthYear = profile.getBirthYear();
        String currentMenh = profile.getMenh();
        String menh = currentMenh;
        
        if (birthYear > 0) {
            String corrected = MenhCalculator.tinhMenh(birthYear);
            if (!corrected.equals(currentMenh)) {
                menh = corrected;
                profile.setMenh(menh);
                // Cập nhật lại local và cloud để đảm bảo đồng bộ triệt để
                preferenceManager.setMenh(menh);
                preferenceManager.saveUser(profile);
                authRepository.syncUserProfileToFirestore(profile);
            }
        }

        if (menh != null) {
            String emoji = MenhCalculator.getEmojiMenh(menh);
            binding.tvMenhBadge.setText(emoji + " " + getString(
                    com.tiredcity.app.R.string.menh_label, menh));
            binding.tvMenhBadge.setVisibility(View.VISIBLE);
        }

        // Avatar: ảnh đã chọn nếu có, ngược lại logo gà TiredCity
        com.tiredcity.app.utils.AvatarUtils.load(this, binding.ivAvatar);
    }

    private void resendVerificationEmail(com.google.firebase.auth.FirebaseUser user) {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Xác thực Email")
                .setMessage("Bạn có muốn gửi lại email xác thực đến địa chỉ " + user.getEmail() + " không?")
                .setPositiveButton("Gửi lại", (d, w) -> {
                    user.sendEmailVerification().addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            Toast.makeText(this, "Đã gửi email xác thực thành công!", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(this, "Lỗi: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
                })
                .setNegativeButton("Đóng", null)
                .show();
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
        new MaterialAlertDialogBuilder(this)
                .setTitle("Đăng xuất")
                .setMessage("Bạn có chắc chắn muốn đăng xuất khỏi tài khoản này?")
                .setPositiveButton("Đăng xuất", (d, w) -> {
                    com.google.firebase.auth.FirebaseAuth.getInstance().signOut();
                    preferenceManager.clearToken();
                    preferenceManager.clearCredentials();
                    startActivity(new Intent(this, LoginActivity.class));
                    finishAffinity();
                })
                .setNegativeButton("Hủy", null)
                .show();
    }
}
