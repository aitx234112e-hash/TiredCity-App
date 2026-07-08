package com.tiredcity.app.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Toast;

import com.google.android.material.datepicker.CalendarConstraints;
import com.google.android.material.datepicker.DateValidatorPointBackward;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.firebase.FirebaseNetworkException;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseAuthWeakPasswordException;
import com.google.firebase.auth.FirebaseUser;
import com.tiredcity.app.R;
import com.tiredcity.app.data.model.UserProfile;
import com.tiredcity.app.data.network.ApiClient;
import com.tiredcity.app.data.repository.AuthRepository;
import com.tiredcity.app.databinding.ActivityRegisterBinding;
import com.tiredcity.app.ui.base.BaseActivity;
import com.tiredcity.app.ui.main.MainActivity;
import com.tiredcity.app.utils.LocaleHelper;
import com.tiredcity.app.utils.MenhCalculator;
import com.tiredcity.app.utils.PhoneUtils;

import java.util.Calendar;
import java.util.Locale;
import java.util.TimeZone;

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

        authRepository = new AuthRepository(ApiClient.getApiService(null), preferenceManager);

        binding.etBirthdate.setOnClickListener(v -> showDatePicker());
        PhoneUtils.attach(binding.etPhone);   // tự cách 4-3-3, chặn quá 10 số

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
            if (PhoneUtils.isEmpty(phone)) {
                binding.etPhone.setError(getString(R.string.error_phone_empty));
                return;
            }
            if (!PhoneUtils.isValid(phone)) {
                binding.etPhone.setError(getString(R.string.error_phone_invalid));
                return;
            }
            binding.etPhone.setError(null);
            if (!dateChosen) {
                binding.etBirthdate.setError(getString(R.string.error_birthdate));
                Toast.makeText(this, getString(R.string.error_birthdate), Toast.LENGTH_SHORT).show();
                return;
            }
            if (TextUtils.isEmpty(password) || password.length() < 6) {
                binding.tilPassword.setError("Mật khẩu ít nhất 6 ký tự");
                return;
            }
            binding.tilPassword.setError(null);
            if (!password.equals(confirm)) {
                binding.tilConfirmPassword.setError(getString(R.string.error_confirm_password));
                return;
            }
            binding.tilConfirmPassword.setError(null);
            if (!binding.cbTerms.isChecked()) {
                Toast.makeText(this, getString(R.string.error_agree_terms), Toast.LENGTH_SHORT).show();
                return;
            }
            attemptRegister(email, password, name, PhoneUtils.digits(phone));   // lưu chỉ chữ số
        });

        binding.tvLogin.setOnClickListener(v -> navigateToLogin());

        binding.btnLanguage.setOnClickListener(v -> {
            LocaleHelper.toggleLanguage(this);
            recreate();
        });
    }

    private void showDatePicker() {
        Calendar c = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        c.clear();
        if (dateChosen) c.set(birthYear, birthMonth - 1, birthDay);
        else            c.set(2000, 0, 1);
        long startSelection = c.getTimeInMillis();

        // Bó dải lịch [1920 → hôm nay] và mở đúng tại ngày đang chọn: mặc định MaterialDatePicker
        // nạp cả Jan 1900 → Dec 2100 (~2400 tháng) vào RecyclerView nên mở CHẬM và GIẬT.
        Calendar startBound = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        startBound.clear();
        startBound.set(1920, 0, 1);
        long now = MaterialDatePicker.todayInUtcMilliseconds();
        long openAt = Math.min(Math.max(startSelection, startBound.getTimeInMillis()), now);

        CalendarConstraints constraints = new CalendarConstraints.Builder()
                .setStart(startBound.getTimeInMillis())
                .setEnd(now)
                .setOpenAt(openAt)
                .setValidator(DateValidatorPointBackward.now())
                .build();

        MaterialDatePicker<Long> picker = MaterialDatePicker.Builder.datePicker()
                .setTheme(R.style.Theme_TC_DatePicker)   // bỏ animation mở → hết giật giựt
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
            binding.etBirthdate.setError(null);
            binding.etBirthdate.setText(String.format(Locale.getDefault(), "%02d/%02d/%04d", birthDay, birthMonth, birthYear));
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

                                String menh   = MenhCalculator.tinhMenh(birthYear);
                                String sign   = MenhCalculator.tinhCungHoangDao(birthMonth, birthDay);
                                String animal = MenhCalculator.tinhConGiap(birthYear);

                                preferenceManager.setMenh(menh);
                                preferenceManager.setZodiac(sign);

                                UserProfile profile = new UserProfile();
                                profile.setId(uid);
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

                                authRepository.syncUserProfileToFirestore(profile);
                                user.sendEmailVerification();

                                ApiClient.reset();
                                Toast.makeText(this, "Đăng ký thành công! Vui lòng kiểm tra email để xác thực.", Toast.LENGTH_LONG).show();
                                startActivity(new Intent(RegisterActivity.this, MainActivity.class));
                                finishAffinity();
                            });
                        }
                    } else {
                        String msg = registerErrorMessage(task.getException());
                        binding.etEmail.setError(msg);
                        Toast.makeText(this, "Đăng ký thất bại: " + msg, Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private String registerErrorMessage(Exception e) {
        if (e instanceof FirebaseAuthUserCollisionException) {
            return getString(R.string.error_email_in_use);
        }
        if (e instanceof FirebaseAuthWeakPasswordException) {
            return getString(R.string.error_weak_password);
        }
        if (e instanceof FirebaseAuthInvalidCredentialsException) {
            return getString(R.string.error_invalid_email);
        }
        if (e instanceof FirebaseNetworkException) {
            return getString(R.string.error_network_auth);
        }
        return e.getLocalizedMessage() != null ? e.getLocalizedMessage() : getString(R.string.error_login_failed);
    }

    private void navigateToLogin() {
        startActivity(new Intent(this, LoginActivity.class));
        finish();
    }
}
