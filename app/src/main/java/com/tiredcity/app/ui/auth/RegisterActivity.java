package com.tiredcity.app.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Toast;
import com.google.android.material.datepicker.CalendarConstraints;
import com.google.android.material.datepicker.DateValidatorPointBackward;
import com.google.android.material.datepicker.MaterialDatePicker;
import java.util.TimeZone;
import com.tiredcity.app.data.model.ApiResponse;
import com.tiredcity.app.data.model.User;
import com.tiredcity.app.data.model.UserProfile;
import com.tiredcity.app.data.network.ApiClient;
import com.tiredcity.app.data.repository.AuthRepository;
import com.tiredcity.app.databinding.ActivityRegisterBinding;
import com.tiredcity.app.ui.base.BaseActivity;
import com.tiredcity.app.ui.main.MainActivity;
import com.tiredcity.app.utils.LocaleHelper;
import com.tiredcity.app.utils.MenhCalculator;
import java.util.Calendar;
import java.util.Locale;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class RegisterActivity extends BaseActivity {

    private ActivityRegisterBinding binding;
    private AuthRepository authRepository;

    private int birthYear, birthMonth, birthDay;
    private boolean dateChosen = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityRegisterBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        authRepository = new AuthRepository(
                ApiClient.getApiService(null),
                preferenceManager
        );

        binding.etBirthdate.setOnClickListener(v -> showDatePicker());

        binding.btnRegister.setOnClickListener(v -> {
            String name     = binding.etFullname.getText().toString().trim();
            String email    = binding.etEmail.getText().toString().trim();
            String phone    = binding.etPhone.getText().toString().trim();
            String password = binding.etPassword.getText().toString().trim();
            String confirm  = binding.etConfirmPassword.getText().toString().trim();

            if (TextUtils.isEmpty(name)) {
                binding.etFullname.setError("Nhập họ và tên");
                return;
            }
            if (TextUtils.isEmpty(email)) {
                binding.etEmail.setError("Nhập email");
                return;
            }
            if (!dateChosen) {
                binding.etBirthdate.setError(getString(com.tiredcity.app.R.string.error_birthdate));
                Toast.makeText(this, getString(com.tiredcity.app.R.string.error_birthdate),
                        Toast.LENGTH_SHORT).show();
                return;
            }
            if (TextUtils.isEmpty(password) || password.length() < 6) {
                binding.tilPassword.setError("Mật khẩu ít nhất 6 ký tự");
                return;
            }
            binding.tilPassword.setError(null);
            if (!password.equals(confirm)) {
                binding.tilConfirmPassword.setError(
                        getString(com.tiredcity.app.R.string.error_confirm_password));
                return;
            }
            binding.tilConfirmPassword.setError(null);
            if (!binding.cbTerms.isChecked()) {
                Toast.makeText(this, getString(com.tiredcity.app.R.string.error_agree_terms),
                        Toast.LENGTH_SHORT).show();
                return;
            }
            attemptRegister(email, password, name, phone);
        });

        binding.tvLogin.setOnClickListener(v -> navigateToLogin());

        binding.btnLanguage.setOnClickListener(v -> {
            LocaleHelper.toggleLanguage(this);
            recreate();
        });
    }

    /** Hiện lịch chọn ngày sinh bằng MaterialDatePicker (tối đa là hôm nay). */
    private void showDatePicker() {
        // Ngày mở đầu: ngày đã chọn, hoặc mặc định 01/01/2000 (UTC để khớp với MaterialDatePicker).
        Calendar c = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        c.clear();
        if (dateChosen) c.set(birthYear, birthMonth - 1, birthDay);
        else            c.set(2000, 0, 1);
        long startSelection = c.getTimeInMillis();

        CalendarConstraints constraints = new CalendarConstraints.Builder()
                .setValidator(DateValidatorPointBackward.now())   // chặn chọn ngày tương lai
                .build();

        MaterialDatePicker<Long> picker = MaterialDatePicker.Builder.datePicker()
                .setTitleText(com.tiredcity.app.R.string.hint_birthdate)
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
            binding.etBirthdate.setError(null);
            binding.etBirthdate.setText(
                    String.format(Locale.getDefault(), "%02d/%02d/%04d", birthDay, birthMonth, birthYear));
        });

        picker.show(getSupportFragmentManager(), "birthdate_picker");
    }

    private void attemptRegister(String email, String password, String fullName, String phone) {
        binding.btnRegister.setEnabled(false);

        FirebaseAuth.getInstance().createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    binding.btnRegister.setEnabled(true);
                    if (task.isSuccessful()) {
                        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                        if (user != null) {
                            user.getIdToken(true).addOnCompleteListener(tokenTask -> {
                                String token = tokenTask.isSuccessful() ? tokenTask.getResult().getToken() : "firebase-token-local";
                                String uid = user.getUid();

                                // SỬ DỤNG CÔNG THỨC MỚI NHẤT
                                String menh   = MenhCalculator.tinhMenh(birthYear);
                                String sign   = MenhCalculator.tinhCungHoangDao(birthMonth, birthDay);
                                String animal = MenhCalculator.tinhConGiap(birthYear);

                                preferenceManager.setMenh(menh);
                                preferenceManager.setZodiac(sign);

                                UserProfile profile = new UserProfile();
                                profile.setName(fullName);
                                profile.setEmail(email);
                                profile.setPhone(phone);
                                profile.setBirthDate(String.format(Locale.US, "%04d-%02d-%02d", birthYear, birthMonth, birthDay));
                                profile.setMenh(menh);
                                profile.setZodiac(sign);
                                profile.setAnimal(animal);
                                preferenceManager.saveUser(profile);

                                preferenceManager.saveToken(token);
                                preferenceManager.saveUserId(uid);

                                // ĐỒNG BỘ LÊN CLOUD NGAY
                                authRepository.syncUserProfileToFirestore(profile);

                                // Gửi email xác thực
                                user.sendEmailVerification();

                                ApiClient.reset();
                                Toast.makeText(this, "Đăng ký thành công! Vui lòng kiểm tra email để xác thực.", Toast.LENGTH_LONG).show();
                                startActivity(new Intent(RegisterActivity.this, MainActivity.class));
                                finishAffinity();
                            });
                        }
                    } else {
                        Toast.makeText(this, "Đăng ký thất bại: " + task.getException().getMessage(),
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    /** Đăng ký qua API thật. Hiện chưa dùng vì backend chưa có endpoint auth. */
    @SuppressWarnings("unused")
    private void attemptOnlineRegister(String email, String password, String fullName) {
        binding.btnRegister.setEnabled(false);
        authRepository.register(email, password, fullName).enqueue(new Callback<ApiResponse<User>>() {
            @Override
            public void onResponse(Call<ApiResponse<User>> call, Response<ApiResponse<User>> response) {
                binding.btnRegister.setEnabled(true);
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    User user = response.body().getData();
                    preferenceManager.saveToken(user.getToken());
                    preferenceManager.saveUserId(user.getId());
                    ApiClient.reset();
                    startActivity(new Intent(RegisterActivity.this, MainActivity.class));
                    finishAffinity();
                } else {
                    String msg = (response.body() != null) ? response.body().getMessage() : "Đăng ký thất bại";
                    Toast.makeText(RegisterActivity.this, msg, Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<User>> call, Throwable t) {
                binding.btnRegister.setEnabled(true);
                Toast.makeText(RegisterActivity.this, "Lỗi mạng: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void navigateToLogin() {
        startActivity(new Intent(this, LoginActivity.class));
        finish();
    }
}
