package com.tiredcity.app.ui.profile;

import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Toast;
import com.bumptech.glide.Glide;
import com.tiredcity.app.data.model.ApiResponse;
import com.tiredcity.app.data.model.UserProfile;
import com.tiredcity.app.data.network.ApiClient;
import com.tiredcity.app.data.network.ApiService;
import com.tiredcity.app.databinding.ActivityEditProfileBinding;
import com.tiredcity.app.ui.base.BaseActivity;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class EditProfileActivity extends BaseActivity {

    private ActivityEditProfileBinding binding;
    private ApiService apiService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityEditProfileBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        binding.toolbar.setNavigationOnClickListener(v -> finish());

        apiService = ApiClient.getApiService(preferenceManager.getToken());

        binding.btnSave.setOnClickListener(v -> {
            String name    = binding.etName.getText().toString().trim();
            String phone   = binding.etPhone.getText().toString().trim();
            String address = binding.etAddress.getText().toString().trim();
            String birth   = binding.etBirthDate.getText().toString().trim();

            if (TextUtils.isEmpty(name)) {
                binding.etName.setError("Nhập họ và tên");
                return;
            }
            UserProfile updated = new UserProfile();
            updated.setName(name);
            updated.setPhone(phone);
            updated.setAddress(address);
            updated.setBirthDate(birth);
            saveProfile(updated);
        });

        // Pre-fill from cached profile
        UserProfile cached = preferenceManager.getUser();
        if (cached != null) bindProfileToForm(cached);
        else loadCurrentProfile();
    }

    private void loadCurrentProfile() {
        apiService.getProfile().enqueue(new Callback<ApiResponse<UserProfile>>() {
            @Override
            public void onResponse(Call<ApiResponse<UserProfile>> call, Response<ApiResponse<UserProfile>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    bindProfileToForm(response.body().getData());
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<UserProfile>> call, Throwable t) {}
        });
    }

    private void bindProfileToForm(UserProfile profile) {
        binding.etName.setText(profile.getName());
        binding.etEmail.setText(profile.getEmail());
        binding.etPhone.setText(profile.getPhone());
        binding.etAddress.setText(profile.getAddress());
        binding.etBirthDate.setText(profile.getBirthDate());

        if (profile.getAvatar() != null && !profile.getAvatar().isEmpty()) {
            Glide.with(this)
                .load(profile.getAvatar())
                .placeholder(com.tiredcity.app.R.drawable.ic_person_placeholder)
                .circleCrop()
                .into(binding.ivAvatar);
        }
    }

    private void saveProfile(UserProfile profile) {
        binding.btnSave.setEnabled(false);
        apiService.updateProfile(profile).enqueue(new Callback<ApiResponse<UserProfile>>() {
            @Override
            public void onResponse(Call<ApiResponse<UserProfile>> call, Response<ApiResponse<UserProfile>> response) {
                binding.btnSave.setEnabled(true);
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    preferenceManager.saveUser(response.body().getData());
                    Toast.makeText(EditProfileActivity.this, "Cập nhật thành công ✓", Toast.LENGTH_SHORT).show();
                    finish();
                } else {
                    Toast.makeText(EditProfileActivity.this, "Cập nhật thất bại", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<UserProfile>> call, Throwable t) {
                binding.btnSave.setEnabled(true);
                Toast.makeText(EditProfileActivity.this, "Lỗi mạng", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
