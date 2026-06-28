package com.tiredcity.app.ui.profile;

import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import com.tiredcity.app.R;
import com.tiredcity.app.data.model.UserProfile;
import com.tiredcity.app.databinding.ActivityEditProfileBinding;
import com.tiredcity.app.ui.base.BaseActivity;
import com.tiredcity.app.utils.AddressData;
import com.tiredcity.app.utils.AvatarUtils;
import java.util.ArrayList;
import java.util.List;

public class EditProfileActivity extends BaseActivity {

    private ActivityEditProfileBinding binding;

    // Bộ chọn ảnh từ thư viện (không cần xin quyền — dùng system picker).
    private final ActivityResultLauncher<String> pickAvatar =
        registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
            if (uri != null) onAvatarPicked(uri);
        });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityEditProfileBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        binding.toolbar.setNavigationOnClickListener(v -> finish());

        // Đổi ảnh đại diện
        binding.ivAvatar.setOnClickListener(v -> pickAvatar.launch("image/*"));
        binding.ibChangeAvatar.setOnClickListener(v -> pickAvatar.launch("image/*"));

        binding.btnSave.setOnClickListener(v -> saveProfileLocal());

        // Dropdown địa chỉ 3 cấp: Tỉnh → Quận/Huyện → Phường/Xã
        AddressData.init(this);
        setDropdown(binding.actProvince,
                new ArrayList<>(java.util.Arrays.asList(getResources().getStringArray(R.array.vn_provinces))));

        binding.actProvince.setOnItemClickListener((parent, v, pos, id) -> {
            String prov = binding.actProvince.getText().toString();
            setDropdown(binding.actDistrict, AddressData.getDistricts(prov));
            binding.actDistrict.setText("", false);
            setDropdown(binding.actWard, new ArrayList<>());
            binding.actWard.setText("", false);
        });
        binding.actDistrict.setOnItemClickListener((parent, v, pos, id) -> {
            String prov = binding.actProvince.getText().toString();
            String dist = binding.actDistrict.getText().toString();
            setDropdown(binding.actWard, AddressData.getWards(prov, dist));
            binding.actWard.setText("", false);
        });

        UserProfile cached = preferenceManager.getUser();
        if (cached != null) bindProfileToForm(cached);
        AvatarUtils.load(this, binding.ivAvatar);
    }

    private void setDropdown(AutoCompleteTextView view, List<String> items) {
        view.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, items));
    }

    private void onAvatarPicked(Uri uri) {
        try {
            String path = AvatarUtils.saveFromUri(this, uri);
            preferenceManager.setAvatarPath(path);
            AvatarUtils.load(this, binding.ivAvatar);
        } catch (Exception e) {
            Toast.makeText(this, getString(R.string.error_update_failed), Toast.LENGTH_SHORT).show();
        }
    }

    private void bindProfileToForm(UserProfile profile) {
        binding.etName.setText(profile.getName());
        binding.etEmail.setText(profile.getEmail());
        binding.etPhone.setText(profile.getPhone());
        binding.etBirthDate.setText(profile.getBirthDate());

        // Địa chỉ tách phần — nạp sẵn dropdown theo dữ liệu đã lưu
        binding.actProvince.setText(profile.getProvince(), false);
        setDropdown(binding.actDistrict, AddressData.getDistricts(profile.getProvince()));
        binding.actDistrict.setText(profile.getDistrict(), false);
        setDropdown(binding.actWard, AddressData.getWards(profile.getProvince(), profile.getDistrict()));
        binding.actWard.setText(profile.getWard(), false);
        binding.etStreet.setText(profile.getStreet());
    }

    /** Lưu hồ sơ cục bộ (offline, không phụ thuộc backend). */
    private void saveProfileLocal() {
        String name = binding.etName.getText().toString().trim();
        if (TextUtils.isEmpty(name)) {
            binding.etName.setError(getString(R.string.hint_fullname));
            return;
        }
        UserProfile p = preferenceManager.getUser();
        if (p == null) p = new UserProfile();
        p.setName(name);
        p.setPhone(binding.etPhone.getText().toString().trim());
        p.setBirthDate(binding.etBirthDate.getText().toString().trim());

        // Địa chỉ tách phần + gộp lại thành address đầy đủ để hiển thị/giao hàng
        p.setProvince(binding.actProvince.getText().toString().trim());
        p.setDistrict(binding.actDistrict.getText().toString().trim());
        p.setWard(binding.actWard.getText().toString().trim());
        p.setStreet(binding.etStreet.getText().toString().trim());
        p.setAddress(p.getFullAddress());

        preferenceManager.saveUser(p);
        Toast.makeText(this, getString(R.string.success_profile_update), Toast.LENGTH_SHORT).show();
        finish();
    }
}
