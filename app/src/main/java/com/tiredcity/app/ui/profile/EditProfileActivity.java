package com.tiredcity.app.ui.profile;

import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.bumptech.glide.Glide;
import com.google.android.material.datepicker.CalendarConstraints;
import com.google.android.material.datepicker.DateValidatorPointBackward;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.tiredcity.app.R;
import com.tiredcity.app.data.model.UserProfile;
import com.tiredcity.app.data.network.ApiClient;
import com.tiredcity.app.data.repository.AuthRepository;
import com.tiredcity.app.databinding.ActivityEditProfileBinding;
import com.tiredcity.app.ui.base.BaseActivity;
import com.tiredcity.app.utils.AddressData;
import com.tiredcity.app.utils.AvatarUtils;
import com.tiredcity.app.utils.MenhCalculator;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

public class EditProfileActivity extends BaseActivity {

    private ActivityEditProfileBinding binding;
    private AuthRepository authRepository;

    private int birthYear, birthMonth, birthDay;
    private boolean dateChosen = false;

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

        // Xử lý bàn phím che khuất các ô nhập liệu (đặc biệt trên Android 15+)
        ViewCompat.setOnApplyWindowInsetsListener(binding.getRoot(), (v, insets) -> {
            int ime = insets.getInsets(WindowInsetsCompat.Type.ime()).bottom;
            v.setPadding(v.getPaddingLeft(), v.getPaddingTop(), v.getPaddingRight(), ime);
            return insets;
        });

        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        binding.toolbar.setNavigationOnClickListener(v -> finish());

        // Đổi ảnh đại diện
        binding.ivAvatar.setOnClickListener(v -> pickAvatar.launch("image/*"));
        binding.ibChangeAvatar.setOnClickListener(v -> pickAvatar.launch("image/*"));

        authRepository = new AuthRepository(ApiClient.getApiService(null), preferenceManager);

        binding.etBirthDate.setOnClickListener(v -> showDatePicker());
        binding.etBirthDate.setFocusable(false);
        binding.etBirthDate.setCursorVisible(false);

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
            // Hiển thị cục bộ ngay
            Glide.with(this).load(uri).circleCrop().into(binding.ivAvatar);
            
            binding.btnSave.setEnabled(false);
            Toast.makeText(this, "Đang tải ảnh lên...", Toast.LENGTH_SHORT).show();

            // Tải lên Cloud
            AvatarUtils.uploadToCloud(this, uri, new AvatarUtils.OnUploadListener() {
                @Override
                public void onSuccess(String url) {
                    UserProfile p = preferenceManager.getUser();
                    if (p == null) p = new UserProfile();
                    p.setAvatar(url);
                    preferenceManager.saveUser(p);
                    authRepository.syncUserProfileToFirestore(p);
                    
                    runOnUiThread(() -> {
                        binding.btnSave.setEnabled(true);
                        Toast.makeText(EditProfileActivity.this, "Cập nhật ảnh đại diện thành công", Toast.LENGTH_SHORT).show();
                    });
                }

                @Override
                public void onError(String message) {
                    runOnUiThread(() -> {
                        binding.btnSave.setEnabled(true);
                        Toast.makeText(EditProfileActivity.this, "Lỗi tải ảnh: " + message, Toast.LENGTH_SHORT).show();
                    });
                }
            });
            
            String path = AvatarUtils.saveFromUri(this, uri);
            preferenceManager.setAvatarPath(path);
        } catch (Exception e) {
            Toast.makeText(this, getString(R.string.error_update_failed), Toast.LENGTH_SHORT).show();
        }
    }

    private void bindProfileToForm(UserProfile profile) {
        binding.etName.setText(profile.getName());
        binding.etEmail.setText(profile.getEmail());
        binding.etPhone.setText(profile.getPhone());
        
        if (profile.getBirthDate() != null && !profile.getBirthDate().isEmpty()) {
            try {
                // profile.getBirthDate() thường là yyyy-MM-dd
                String[] parts = profile.getBirthDate().split("-");
                if (parts.length == 3) {
                    birthYear = Integer.parseInt(parts[0]);
                    birthMonth = Integer.parseInt(parts[1]);
                    birthDay = Integer.parseInt(parts[2]);
                    dateChosen = true;
                    binding.etBirthDate.setText(String.format(Locale.getDefault(), "%02d/%02d/%04d", birthDay, birthMonth, birthYear));
                }
            } catch (Exception ignored) {}
        }

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
        
        if (dateChosen) {
            String birthDateStr = String.format(Locale.US, "%04d-%02d-%02d", birthYear, birthMonth, birthDay);
            p.setBirthDate(birthDateStr);
            
            // TÍNH LẠI MỆNH VÀ CUNG HOÀNG ĐẠO THEO CÔNG THỨC MỚI
            String menh = MenhCalculator.tinhMenh(birthYear);
            String zodiac = MenhCalculator.tinhCungHoangDao(birthMonth, birthDay);
            String animal = MenhCalculator.tinhConGiap(birthYear);
            
            p.setMenh(menh);
            p.setZodiac(zodiac);
            p.setAnimal(animal);
            
            // Cập nhật preference để hiện ngay ở ProfileActivity
            preferenceManager.setMenh(menh);
            preferenceManager.setZodiac(zodiac);
        }

        // Địa chỉ tách phần + gộp lại thành address đầy đủ để hiển thị/giao hàng
        p.setProvince(binding.actProvince.getText().toString().trim());
        p.setDistrict(binding.actDistrict.getText().toString().trim());
        p.setWard(binding.actWard.getText().toString().trim());
        p.setStreet(binding.etStreet.getText().toString().trim());
        p.setAddress(p.getFullAddress());

        preferenceManager.saveUser(p);

        // ĐỒNG BỘ LÊN CLOUD
        authRepository.syncUserProfileToFirestore(p);

        Toast.makeText(this, getString(R.string.success_profile_update), Toast.LENGTH_SHORT).show();
        finish();
    }

    private void showDatePicker() {
        Calendar c = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        c.clear();
        if (dateChosen) c.set(birthYear, birthMonth - 1, birthDay);
        else            c.set(2000, 0, 1);
        long startSelection = c.getTimeInMillis();

        CalendarConstraints constraints = new CalendarConstraints.Builder()
                .setValidator(DateValidatorPointBackward.now())
                .build();

        MaterialDatePicker<Long> picker = MaterialDatePicker.Builder.datePicker()
                .setTitleText(R.string.hint_birthdate)
                .setSelection(startSelection)
                .setCalendarConstraints(constraints)
                .build();

        picker.addOnPositiveButtonClickListener(selection -> {
            Calendar sel = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
            sel.setTimeInMillis(selection);
            birthYear  = sel.get(Calendar.YEAR);
            birthMonth = sel.get(Calendar.MONTH) + 1;
            birthDay   = sel.get(Calendar.DAY_OF_MONTH);
            dateChosen = true;
            binding.etBirthDate.setText(
                    String.format(Locale.getDefault(), "%02d/%02d/%04d", birthDay, birthMonth, birthYear));
        });

        picker.show(getSupportFragmentManager(), "birthdate_picker");
    }
}
