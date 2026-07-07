package com.tiredcity.app.ui.profile;

import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import com.google.android.material.datepicker.CalendarConstraints;
import com.google.android.material.datepicker.DateValidatorPointBackward;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.tiredcity.app.R;
import com.tiredcity.app.data.model.UserProfile;
import com.tiredcity.app.databinding.ActivityEditProfileBinding;
import com.tiredcity.app.ui.base.BaseActivity;
import com.tiredcity.app.utils.AddressData;
import com.tiredcity.app.utils.AvatarUtils;
import com.tiredcity.app.utils.MenhCalculator;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

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
        setupBirthDateFormatter();
        binding.ibPickBirthDate.setOnClickListener(v -> showBirthDatePicker());

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

    /** Tự chèn dấu "/" khi gõ 6 chữ số ngày sinh (DD/MM/YY) — người dùng chỉ cần gõ số, không cần
     *  tự gõ dấu "/". Giữ đúng vị trí con trỏ khi sửa giữa chuỗi (đếm số chữ số trước con trỏ rồi
     *  quy về vị trí tương ứng sau khi định dạng lại), và khi bấm xoá đúng 1 lần trúng dấu "/" tự
     *  chèn (số chữ số không đổi dù chuỗi ngắn lại) thì xoá luôn chữ số liền trước — để không bị
     *  cảm giác "kẹt", phải bấm xoá 2 lần mới mất 1 số. */
    private void setupBirthDateFormatter() {
        binding.etBirthDate.addTextChangedListener(new TextWatcher() {
            private boolean isFormatting = false;
            private int previousLength = 0;
            private int previousDigitCount = 0;

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                if (isFormatting) return;
                previousLength = s.length();
                previousDigitCount = s.toString().replaceAll("[^0-9]", "").length();
            }

            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                if (isFormatting) return;

                String raw = s.toString();
                boolean isDeleting = raw.length() < previousLength;
                int cursor = binding.etBirthDate.getSelectionStart();
                if (cursor < 0) cursor = raw.length();

                int digitsBeforeCursor = 0;
                for (int i = 0; i < cursor && i < raw.length(); i++) {
                    if (Character.isDigit(raw.charAt(i))) digitsBeforeCursor++;
                }

                String digits = raw.replaceAll("[^0-9]", "");
                if (isDeleting && digits.length() == previousDigitCount && digitsBeforeCursor > 0) {
                    digits = digits.substring(0, digitsBeforeCursor - 1) + digits.substring(digitsBeforeCursor);
                    digitsBeforeCursor--;
                }
                if (digits.length() > 6) digits = digits.substring(0, 6);

                StringBuilder sb = new StringBuilder();
                int newCursor = digitsBeforeCursor <= 0 ? 0 : -1;
                for (int i = 0; i < digits.length(); i++) {
                    if (i == 2 || i == 4) sb.append('/');
                    sb.append(digits.charAt(i));
                    if (i + 1 == digitsBeforeCursor) newCursor = sb.length();
                }
                if (digitsBeforeCursor >= digits.length()) newCursor = sb.length();

                String formatted = sb.toString();
                isFormatting = true;
                s.replace(0, s.length(), formatted);
                binding.etBirthDate.setSelection(Math.max(0, Math.min(newCursor, formatted.length())));
                isFormatting = false;
            }
        });
    }

    /** Lịch chọn ngày sinh (MaterialDatePicker, không cho chọn ngày tương lai) — cách nhập thứ 2
     *  ngoài gõ tay, điền lại vào ô theo đúng định dạng DD/MM/YY. */
    private void showBirthDatePicker() {
        Calendar c = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        c.clear();
        String currentIso = displayToIsoBirthDate(binding.etBirthDate.getText().toString().trim());
        if (currentIso != null) {
            c.set(Integer.parseInt(currentIso.substring(0, 4)),
                  Integer.parseInt(currentIso.substring(5, 7)) - 1,
                  Integer.parseInt(currentIso.substring(8, 10)));
        } else {
            c.set(2000, 0, 1);
        }

        CalendarConstraints constraints = new CalendarConstraints.Builder()
                .setValidator(DateValidatorPointBackward.now())
                .build();

        MaterialDatePicker<Long> picker = MaterialDatePicker.Builder.datePicker()
                .setTitleText(R.string.hint_birthdate)
                .setSelection(c.getTimeInMillis())
                .setCalendarConstraints(constraints)
                .build();

        picker.addOnPositiveButtonClickListener(selection -> {
            Calendar sel = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
            sel.setTimeInMillis(selection);
            int year = sel.get(Calendar.YEAR);
            int month = sel.get(Calendar.MONTH) + 1;
            int day = sel.get(Calendar.DAY_OF_MONTH);
            binding.etBirthDate.setError(null);
            binding.etBirthDate.setText(
                    String.format(Locale.US, "%02d/%02d/%02d", day, month, year % 100));
        });

        picker.show(getSupportFragmentManager(), "birthdate_picker");
    }

    /** yyyy-MM-dd (lưu trữ) → dd/MM/yy (hiển thị/chỉnh sửa). Rỗng nếu chưa có/không đúng định dạng. */
    private String isoToDisplayBirthDate(String iso) {
        if (iso == null || iso.length() < 10) return "";
        try {
            String dd = iso.substring(8, 10);
            String mm = iso.substring(5, 7);
            String yy = iso.substring(2, 4);
            return dd + "/" + mm + "/" + yy;
        } catch (Exception e) {
            return "";
        }
    }

    /** dd/MM/yy (nhập tay, 2 số cuối năm) → yyyy-MM-dd (lưu trữ, để {@link UserProfile#getBirthYear()}
     *  và MenhCalculator đọc đúng). Năm 2 số suy ra thế kỷ theo nguyên tắc "không sinh trong tương
     *  lai": YY lớn hơn 2 số cuối năm hiện tại → 19YY, ngược lại → 20YY. Trả về null nếu chưa nhập
     *  đủ 6 số hoặc ngày/tháng không hợp lệ. */
    private String displayToIsoBirthDate(String display) {
        String digits = display.replaceAll("[^0-9]", "");
        if (digits.length() != 6) return null;

        int day = Integer.parseInt(digits.substring(0, 2));
        int month = Integer.parseInt(digits.substring(2, 4));
        int yy = Integer.parseInt(digits.substring(4, 6));
        if (month < 1 || month > 12 || day < 1 || day > 31) return null;

        int currentYY = Calendar.getInstance().get(Calendar.YEAR) % 100;
        int year = (yy > currentYY ? 1900 : 2000) + yy;

        return String.format(Locale.US, "%04d-%02d-%02d", year, month, day);
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
        binding.etBirthDate.setText(isoToDisplayBirthDate(profile.getBirthDate()));

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

        String birthInput = binding.etBirthDate.getText().toString().trim();
        if (!birthInput.isEmpty()) {
            String isoBirthDate = displayToIsoBirthDate(birthInput);
            if (isoBirthDate == null) {
                binding.etBirthDate.setError(getString(R.string.error_birthdate));
                return;
            }
            p.setBirthDate(isoBirthDate);

            // Ngày sinh đổi → tính lại Mệnh/Cung hoàng đạo/Con giáp ngay (giống lúc đăng ký) và
            // lưu lại, để Trang chủ/AI Styling hiển thị đúng mệnh mới ngay khi quay lại.
            int birthYear = p.getBirthYear();
            int birthMonth = Integer.parseInt(isoBirthDate.substring(5, 7));
            int birthDay = Integer.parseInt(isoBirthDate.substring(8, 10));
            String menh = MenhCalculator.tinhMenh(birthYear);
            String sign = MenhCalculator.tinhCungHoangDao(birthMonth, birthDay);
            String animal = MenhCalculator.tinhConGiap(birthYear);
            preferenceManager.setMenh(menh);
            preferenceManager.setZodiac(sign);
            p.setMenh(menh);
            p.setZodiac(sign);
            p.setAnimal(animal);
        }

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
