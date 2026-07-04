package com.tiredcity.app.ui.auth;

import android.content.Intent;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Toast;

import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.content.res.AppCompatResources;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.auth.api.signin.GoogleSignInStatusCodes;
import com.google.android.gms.common.api.ApiException;

import com.google.android.material.datepicker.CalendarConstraints;
import com.google.android.material.datepicker.DateValidatorPointBackward;
import com.google.android.material.datepicker.MaterialDatePicker;

import com.tiredcity.app.R;
import com.tiredcity.app.data.model.ApiResponse;
import com.tiredcity.app.data.model.User;
import com.tiredcity.app.data.model.UserProfile;
import com.tiredcity.app.data.network.ApiClient;
import com.tiredcity.app.data.repository.AuthRepository;
import com.tiredcity.app.databinding.ActivityLoginBinding;
import com.tiredcity.app.ui.base.BaseActivity;
import com.tiredcity.app.ui.main.MainActivity;
import com.tiredcity.app.ui.onboarding.OnboardingActivity;
import com.tiredcity.app.utils.LocaleHelper;
import com.tiredcity.app.utils.MenhCalculator;

import java.util.Calendar;
import java.util.Locale;
import java.util.TimeZone;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.facebook.AccessToken;
import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.login.LoginManager;
import com.facebook.login.LoginResult;
import java.util.Arrays;
import com.google.firebase.auth.FacebookAuthProvider;
import android.util.Base64;
import android.util.Log;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;

public class LoginActivity extends BaseActivity {

    // Dev bypass — chấp nhận đăng nhập offline mà không gọi API.
    // Hữu ích khi backend tiredcity.vn chưa có endpoint /api/auth/login.
    private static final String DEMO_EMAIL    = "demo@demo.com";
    private static final String DEMO_PASSWORD = "123456";
    private static final String DEMO_TOKEN    = "demo-token-local";
    private static final String DEMO_USER_ID  = "demo-user";

    // Package của app Quản trị (app-admin/). Chọn "Quản trị" sẽ mở app này nếu đã cài.
    private static final String ADMIN_PACKAGE = "com.tiredcity.admin";
    // Khoá gửi kèm email/mật khẩu vừa nhập sang app Quản trị (phải khớp với LoginActivity bên app-admin)
    // để app đó vào thẳng Dashboard, không hiện lại form đăng nhập riêng.
    private static final String EXTRA_ADMIN_EMAIL    = "com.tiredcity.admin.extra.EMAIL";
    private static final String EXTRA_ADMIN_PASSWORD = "com.tiredcity.admin.extra.PASSWORD";

    private ActivityLoginBinding binding;
    private AuthRepository authRepository;

    // true sau khi vừa mở app Quản trị: chờ tới khi Activity này HIỆN LẠI (onResume) mới
    // đưa toggle về "Khách hàng" — làm ngay lúc startActivity() sẽ bị người dùng nhìn thấy
    // thanh toggle giật nhảy sang trái trong lúc màn hình còn đang animate chuyển đi.
    private boolean pendingAdminToggleReset = false;

    private GoogleSignInClient googleSignInClient;
    private ActivityResultLauncher<Intent> googleSignInLauncher;
    private CallbackManager callbackManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Kiểm tra đăng nhập: Kết hợp PreferenceManager và FirebaseAuth
        boolean isLogged = preferenceManager.isLoggedIn() || FirebaseAuth.getInstance().getCurrentUser() != null;
        
        if (isLogged) {
            // Nếu đã qua Onboarding thì vào Home, nếu chưa thì vào Onboarding
            if (preferenceManager.isOnboardingShown()) {
                startActivity(new Intent(this, MainActivity.class));
            } else {
                startActivity(new Intent(this, OnboardingActivity.class));
            }
            finish();
            return;
        }

        // Hâm nóng tầng mạng (OkHttp/Retrofit) trên luồng nền trong lúc splash overlay hiển thị.
        new Thread(() -> {
            try { ApiClient.getApiService(null); } catch (Exception ignored) { }
        }, "api-warmup").start();

        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Android 15+ (edge-to-edge): adjustResize KHÔNG tự chừa chỗ cho bàn phím nữa.
        // Đệm ĐÁY cho root (cha) bằng chiều cao bàn phím (IME) → ScrollView (match_parent) co lại
        // phía trên bàn phím, nhờ đó tự cuộn ô mật khẩu / nút đăng nhập vào tầm nhìn, không bị che.
        // Dùng root chứ không phải ScrollView: đệm đáy một view match_parent sẽ nằm ngoài màn hình,
        // chỉ đệm cha mới thực sự thu nhỏ vùng hiển thị.
        ViewCompat.setOnApplyWindowInsetsListener(binding.getRoot(), (v, insets) -> {
            int ime = insets.getInsets(WindowInsetsCompat.Type.ime()).bottom;
            v.setPadding(v.getPaddingLeft(), v.getPaddingTop(), v.getPaddingRight(), ime);
            return insets;
        });

        // AuthRepository/Retrofit tạo lazy trong attemptOnlineLogin() (đăng nhập offline không dùng).

        restoreSavedCredentials();
        setupRoleToggle();
        setupGoogleSignIn();

        callbackManager = CallbackManager.Factory.create();
        setupFacebookSignIn();
        
        logKeyHash();

        binding.btnLogin.setOnClickListener(v -> {
            String email    = binding.etEmail.getText().toString().trim();
            String password = binding.etPassword.getText().toString().trim();

            if (TextUtils.isEmpty(email)) {
                binding.etEmail.setError("Nhập email");
                return;
            }
            if (TextUtils.isEmpty(password)) {
                binding.etPassword.setError("Nhập mật khẩu");
                return;
            }

            // Toggle "Quản trị": mở thẳng app Quản trị (vào Dashboard) — cùng 1 màn login
            // này, chỉ khác hướng đi. Toggle "Khách hàng": đi luồng khách hàng như thường.
            if (binding.toggleRole.getCheckedButtonId() == binding.btnRoleAdmin.getId()) {
                openAdminApp(email, password);
                return;
            }

            persistRememberMe(email, password);
            attemptLogin(email, password);
        });

        // Nhấn "Done" trên bàn phím ở ô mật khẩu = bấm nút Đăng nhập (khỏi cần tìm nút bị bàn phím che).
        binding.etPassword.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                binding.btnLogin.performClick();
                return true;
            }
            return false;
        });

        binding.tvRegister.setOnClickListener(v -> navigateToRegister());
        binding.tvForgotPassword.setOnClickListener(v ->
                startActivity(new Intent(this, ForgotPasswordActivity.class)));

        binding.btnLanguage.setOnClickListener(v -> switchLanguageInstantly());

        // Google: dùng Google Sign-In thật (hiện hộp chọn tài khoản Google của máy).
        binding.btnGoogle.setOnClickListener(v -> launchGoogleSignIn());
        // Facebook
        binding.btnFacebook.setOnClickListener(v -> 
            LoginManager.getInstance().logInWithReadPermissions(this, callbackManager, Arrays.asList("email", "public_profile")));

        setupSplashOverlay();
    }

    /** Toggle chọn vai trò — mặc định "Khách hàng". Chỉ đổi màu chọn, không làm gì thêm. */
    private void setupRoleToggle() {
        binding.toggleRole.check(binding.btnRoleCustomer.getId());
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Màn hình này vừa hiện LẠI (quay lại từ app Quản trị hoặc từ hộp thoại "chưa cài") —
        // giờ mới an toàn để đưa toggle về "Khách hàng" mà không bị thấy nhảy giữa chừng.
        if (pendingAdminToggleReset && binding != null) {
            binding.toggleRole.check(binding.btnRoleCustomer.getId());
            pendingAdminToggleReset = false;
        }
    }

    /**
     * Mở app Quản trị (TiredCity Admin) qua package, kèm email/mật khẩu vừa nhập để app đó
     * vào THẲNG Dashboard (không hiện lại form đăng nhập). Chưa cài thì hiện hộp thoại hướng dẫn.
     */
    private void openAdminApp(String email, String password) {
        Intent launch = getPackageManager().getLaunchIntentForPackage(ADMIN_PACKAGE);
        if (launch != null) {
            launch.putExtra(EXTRA_ADMIN_EMAIL, email);
            launch.putExtra(EXTRA_ADMIN_PASSWORD, password);
            // CLEAR_TOP: nếu app Quản trị đang mở sẵn thì dựng lại với extras mới
            // (mang task cũ lên không thôi sẽ làm rơi mất email/mật khẩu).
            launch.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(launch);
            // Trả toggle về "Khách hàng" để khi quay lại màn login không kẹt ở trạng thái Admin —
            // nhưng CHỜ tới onResume() (xem pendingAdminToggleReset) để không bị thấy nhảy khi
            // màn hình còn đang animate chuyển sang app Quản trị.
            pendingAdminToggleReset = true;
        } else {
            new AlertDialog.Builder(this)
                    .setTitle(R.string.role_admin)
                    .setMessage(R.string.admin_app_not_installed)
                    .setPositiveButton(android.R.string.ok, (d, w) -> d.dismiss())
                    .setOnDismissListener(d -> binding.toggleRole.check(binding.btnRoleCustomer.getId()))
                    .show();
        }
    }

    // ─────────────────────────────── Google Sign-In ───────────────────────────────

    private void setupGoogleSignIn() {
        GoogleSignInOptions.Builder gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .requestProfile();
        if (isWebClientIdConfigured()) {
            // Có Web Client ID thật → lấy idToken để gửi backend xác thực (RBAC/JWT).
            gso.requestIdToken(getString(R.string.default_web_client_id));
        }
        googleSignInClient = GoogleSignIn.getClient(this, gso.build());

        googleSignInLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> handleGoogleResult(result.getData()));
    }

    private boolean isWebClientIdConfigured() {
        String id = getString(R.string.default_web_client_id);
        return id != null && !id.startsWith("YOUR_WEB_CLIENT_ID");
    }

    private void launchGoogleSignIn() {
        // Không còn Toast "chế độ demo" gây hiểu nhầm — chỉ báo khi đăng nhập THẬT SỰ lỗi
        // (xử lý trong handleGoogleResult). Sign-in cơ bản (email + tên) vẫn chạy bình thường.
        // Đăng xuất phiên cũ để LUÔN hiện hộp chọn tài khoản (không tự đăng nhập lại tài khoản trước).
        googleSignInClient.signOut().addOnCompleteListener(t ->
                googleSignInLauncher.launch(googleSignInClient.getSignInIntent()));
    }

    private void handleGoogleResult(Intent data) {
        try {
            GoogleSignInAccount account =
                    GoogleSignIn.getSignedInAccountFromIntent(data).getResult(ApiException.class);
            String idToken = account.getIdToken();
            if (idToken != null) {
                AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
                FirebaseAuth.getInstance().signInWithCredential(credential)
                        .addOnCompleteListener(this, task -> {
                            if (task.isSuccessful()) {
                                FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                                String email = user != null ? user.getEmail() : account.getEmail();
                                String name = user != null ? user.getDisplayName() : account.getDisplayName();
                                completeSocialLogin("Google", email != null ? email : "", name);
                            } else {
                                Toast.makeText(this, "Firebase Google Auth failed: " + task.getException().getMessage(),
                                        Toast.LENGTH_SHORT).show();
                            }
                        });
            } else {
                Toast.makeText(this, "Google ID Token is null. Check Web Client ID config.", Toast.LENGTH_SHORT).show();
            }
        } catch (ApiException e) {
            if (e.getStatusCode() != GoogleSignInStatusCodes.SIGN_IN_CANCELLED) {
                Toast.makeText(this,
                        getString(R.string.google_signin_failed) + " (" + e.getStatusCode() + ")",
                        Toast.LENGTH_SHORT).show();
            }
        }
    }

    /**
     * Splash overlay (thay cho SplashActivity cũ): chỉ hiện khi mở app từ launcher,
     * giữ 1.8s rồi mờ dần để lộ form. Vì cùng 1 Activity nên KHÔNG có khung đen.
     */
    private void setupSplashOverlay() {
        boolean fromLauncher = getIntent() != null
                && Intent.ACTION_MAIN.equals(getIntent().getAction())
                && getIntent().hasCategory(Intent.CATEGORY_LAUNCHER);

        if (!fromLauncher) {
            binding.splashOverlay.setVisibility(View.GONE);
            return;
        }

        binding.splashOverlay.setVisibility(View.VISIBLE);
        binding.splashOverlay.postDelayed(() -> {
            if (isFinishing() || binding == null) return;
            binding.splashOverlay.animate()
                    .alpha(0f)
                    .setDuration(450)
                    .withEndAction(() -> {
                        if (binding != null) binding.splashOverlay.setVisibility(View.GONE);
                    })
                    .start();
        }, 1800);
    }

    /**
     * Đổi ngôn ngữ NGAY trên màn hình đăng nhập — không recreate() Activity nên
     * không còn hiện lại lớp phủ splash đỏ. Cập nhật Resources của chính Activity
     * này tại chỗ rồi gán lại text cho từng view; các màn/dialog mở sau đó từ
     * Activity này (vd hộp thoại "chưa cài app Quản trị") cũng dùng đúng ngôn ngữ mới.
     */
    private void switchLanguageInstantly() {
        String newLang = LocaleHelper.toggleLanguage(this);

        Resources res = getResources();
        Configuration config = new Configuration(res.getConfiguration());
        Locale locale = new Locale(newLang);
        Locale.setDefault(locale);
        config.setLocale(locale);
        //noinspection deprecation
        res.updateConfiguration(config, res.getDisplayMetrics());

        applyLocalizedStrings(res);
    }

    /** Gán lại text/hint cho toàn bộ view hiển thị ngôn ngữ trên màn đăng nhập. */
    private void applyLocalizedStrings(Resources res) {
        binding.btnLanguage.setText(res.getString(R.string.switch_to_other_lang));
        // Icon cờ đổi theo ngôn ngữ (res/drawable-en/ic_flag_switch.xml = cờ Việt Nam khi đang
        // English, res/drawable/ic_flag_switch.xml = cờ Anh khi đang tiếng Việt). Không recreate()
        // nên phải tự nạp lại drawable từ Resources vừa cập nhật locale, nếu không icon đứng yên.
        Drawable flag = AppCompatResources.getDrawable(this, R.drawable.ic_flag_switch);
        binding.btnLanguage.setCompoundDrawablesWithIntrinsicBounds(flag, null, null, null);
        binding.tvBrandTagline.setText(res.getString(R.string.brand_tagline));
        binding.tvLoginSubtitle.setText(res.getString(R.string.login_subtitle));

        binding.btnRoleCustomer.setText(res.getString(R.string.role_customer));
        binding.btnRoleAdmin.setText(res.getString(R.string.role_admin));

        binding.tvLabelEmail.setText(res.getString(R.string.hint_email));
        binding.etEmail.setHint(res.getString(R.string.hint_email));
        binding.tvLabelPassword.setText(res.getString(R.string.hint_password));
        binding.etPassword.setHint(res.getString(R.string.hint_password));

        binding.cbRememberMe.setText(res.getString(R.string.login_remember_me));
        binding.tvForgotPassword.setText(res.getString(R.string.login_forgot_password));

        binding.btnLogin.setText(res.getString(R.string.btn_login));
        binding.tvOrWith.setText(res.getString(R.string.login_or_with));

        binding.tvNoAccount.setText(res.getString(R.string.login_no_account));
        binding.tvRegister.setText(res.getString(R.string.btn_register));
    }

    private void restoreSavedCredentials() {
        if (preferenceManager.isRememberMeEnabled()) {
            binding.etEmail.setText(preferenceManager.getSavedEmail());
            binding.etPassword.setText(preferenceManager.getSavedPassword());
            binding.cbRememberMe.setChecked(true);
        }
    }

    private void persistRememberMe(String email, String password) {
        if (binding.cbRememberMe.isChecked()) {
            preferenceManager.setRememberMe(true);
            preferenceManager.saveCredentials(email, password);
        } else {
            preferenceManager.clearCredentials();
        }
    }

    private void attemptLogin(String email, String password) {
        binding.btnLogin.setEnabled(false);
        FirebaseAuth.getInstance().signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    binding.btnLogin.setEnabled(true);
                    if (task.isSuccessful()) {
                        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                        if (user != null) {
                            user.getIdToken(true).addOnCompleteListener(tokenTask -> {
                                String token = tokenTask.isSuccessful() ? tokenTask.getResult().getToken() : "firebase-token-local";
                                String uid = user.getUid();

                                preferenceManager.saveToken(token);
                                preferenceManager.saveUserId(uid);

                                UserProfile profile = preferenceManager.getUser();
                                if (profile == null) profile = new UserProfile();
                                boolean hasRealName = profile.getName() != null && !profile.getName().trim().isEmpty();
                                if (!hasRealName) {
                                    profile.setEmail(email);
                                    String raw = email.contains("@") ? email.substring(0, email.indexOf('@')) : email;
                                    profile.setName(prettifyName(raw));
                                    preferenceManager.saveUser(profile);
                                }

                                // ĐỒNG BỘ NGAY LÊN CLOUD
                                if (authRepository == null) {
                                    authRepository = new AuthRepository(ApiClient.getApiService(null), preferenceManager);
                                }
                                authRepository.syncUserProfileToFirestore(profile);

                                ApiClient.reset();
                                startActivity(new Intent(LoginActivity.this, OnboardingActivity.class));
                                finishAffinity();
                            });
                        }
                    } else {
                        Toast.makeText(LoginActivity.this, "Đăng nhập thất bại: " + task.getException().getMessage(),
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    /**
     * Đăng nhập mạng xã hội — bản demo offline.
     * Khi có backend: thay bằng Google Sign-In / Facebook Login SDK / Apple, lấy token rồi gọi server.
     */
    private void socialLogin(String provider, String email) {
        completeSocialLogin(provider, email, null);
    }

    /**
     * Hoàn tất đăng nhập mạng xã hội. {@code displayName} là tên thật lấy từ tài khoản
     * (vd Google account); nếu null sẽ suy ra từ email hoặc dùng "{provider} User".
     */
    private void completeSocialLogin(String provider, String email, String displayName) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            user.getIdToken(true).addOnCompleteListener(tokenTask -> {
                String token = tokenTask.isSuccessful() ? tokenTask.getResult().getToken() : DEMO_TOKEN;
                preferenceManager.saveToken(token);
                preferenceManager.saveUserId(user.getUid());

                UserProfile profile = preferenceManager.getUser();
                if (profile == null) profile = new UserProfile();
                boolean hasRealName = profile.getName() != null && !profile.getName().trim().isEmpty();
                if (!hasRealName) {
                    profile.setEmail(email);
                    String name;
                    if (!TextUtils.isEmpty(displayName)) {
                        name = displayName;
                    } else if (email != null && email.contains("@")) {
                        name = prettifyName(email.substring(0, email.indexOf('@')));
                    } else {
                        name = provider + " User";
                    }
                    profile.setName(name);
                }
                preferenceManager.saveUser(profile);

                // ĐỒNG BỘ TỪ CLOUD VỀ TRƯỚC
                if (authRepository == null) {
                    authRepository = new AuthRepository(ApiClient.getApiService(null), preferenceManager);
                }

                authRepository.syncAccount(profile, cloudProfile -> {
                    // Sau khi đã tải dữ liệu từ Cloud (Firestore) về máy thành công:
                    runOnUiThread(() -> {
                        ApiClient.reset();
                        Toast.makeText(this, provider, Toast.LENGTH_SHORT).show();

                        if (cloudProfile != null && !TextUtils.isEmpty(cloudProfile.getBirthDate())) {
                            // Đã có ngày sinh trên Cloud -> Vào thẳng app
                            goToOnboarding();
                        } else {
                            // Chưa có ngày sinh -> Mới hiện bảng chọn
                            promptBirthDateForMenh();
                        }
                    });
                });
            });
        }
    }

    /** Vào màn Onboarding rồi đóng toàn bộ stack đăng nhập. */
    private void goToOnboarding() {
        startActivity(new Intent(LoginActivity.this, OnboardingActivity.class));
        finishAffinity();
    }

    /**
     * Mở lịch chọn ngày sinh (giống màn Đăng ký) sau khi đăng nhập mạng xã hội.
     * Chọn xong → tính Mệnh/Cung hoàng đạo/Con giáp tại máy, lưu hồ sơ, rồi vào Onboarding.
     * Đóng/huỷ vẫn cho vào app (có thể bổ sung ngày sinh sau trong Hồ sơ).
     */
    private void promptBirthDateForMenh() {
        Calendar c = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        c.clear();
        c.set(2000, 0, 1);

        CalendarConstraints constraints = new CalendarConstraints.Builder()
                .setValidator(DateValidatorPointBackward.now())   // chặn chọn ngày tương lai
                .build();

        MaterialDatePicker<Long> picker = MaterialDatePicker.Builder.datePicker()
                .setTitleText(R.string.hint_birthdate)
                .setSelection(c.getTimeInMillis())
                .setCalendarConstraints(constraints)
                .build();

        picker.addOnPositiveButtonClickListener(selection -> {
            Calendar sel = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
            sel.setTimeInMillis(selection);
            int year  = sel.get(Calendar.YEAR);
            int month = sel.get(Calendar.MONTH) + 1;
            int day   = sel.get(Calendar.DAY_OF_MONTH);

            String menh   = MenhCalculator.tinhMenh(year);
            String zodiac = MenhCalculator.tinhCungHoangDao(month, day);
            String animal = MenhCalculator.tinhConGiap(year);

            UserProfile profile = preferenceManager.getUser();
            if (profile == null) profile = new UserProfile();
            profile.setBirthDate(String.format(Locale.US, "%04d-%02d-%02d", year, month, day));
            profile.setMenh(menh);
            profile.setZodiac(zodiac);
            profile.setAnimal(animal);
            preferenceManager.saveUser(profile);

            preferenceManager.setMenh(menh);
            preferenceManager.setZodiac(zodiac);

            goToOnboarding();
        });
        // Người dùng đóng/huỷ không chọn ngày → vẫn vào app, không kẹt lại màn login.
        picker.addOnNegativeButtonClickListener(v -> goToOnboarding());
        picker.addOnCancelListener(d -> goToOnboarding());

        picker.show(getSupportFragmentManager(), "social_birthdate_picker");
    }

    /** Biến phần email thành tên dễ nhìn: "nguyen_van.a" → "Nguyen Van A". */
    private String prettifyName(String raw) {
        if (raw == null || raw.trim().isEmpty()) return getString(com.tiredcity.app.R.string.greeting_guest);
        raw = raw.replace('.', ' ').replace('_', ' ').trim();
        StringBuilder sb = new StringBuilder();
        for (String part : raw.split("\\s+")) {
            if (part.isEmpty()) continue;
            sb.append(Character.toUpperCase(part.charAt(0)));
            if (part.length() > 1) sb.append(part.substring(1));
            sb.append(' ');
        }
        return sb.toString().trim();
    }

    /** Đăng nhập qua API thật. Hiện chưa dùng vì backend chưa có endpoint auth. */
    @SuppressWarnings("unused")
    private void attemptOnlineLogin(String email, String password) {
        if (authRepository == null) {
            authRepository = new AuthRepository(ApiClient.getApiService(null), preferenceManager);
        }
        binding.btnLogin.setEnabled(false);
        authRepository.login(email, password).enqueue(new Callback<ApiResponse<User>>() {
            @Override
            public void onResponse(Call<ApiResponse<User>> call, Response<ApiResponse<User>> response) {
                binding.btnLogin.setEnabled(true);
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    User user = response.body().getData();
                    preferenceManager.saveToken(user.getToken());
                    preferenceManager.saveUserId(user.getId());
                    ApiClient.reset();
                    startActivity(new Intent(LoginActivity.this, OnboardingActivity.class));
                    finishAffinity();
                } else {
                    String msg = (response.body() != null) ? response.body().getMessage() : "Đăng nhập thất bại";
                    Toast.makeText(LoginActivity.this, msg, Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<User>> call, Throwable t) {
                binding.btnLogin.setEnabled(true);
                Toast.makeText(LoginActivity.this, "Lỗi mạng: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setupFacebookSignIn() {
        LoginManager.getInstance().registerCallback(callbackManager,
                new FacebookCallback<LoginResult>() {
                    @Override
                    public void onSuccess(LoginResult loginResult) {
                        Log.d("FacebookLogin", "onSuccess: " + loginResult.getAccessToken().getToken());
                        handleFacebookAccessToken(loginResult.getAccessToken());
                    }

                    @Override
                    public void onCancel() {
                        Log.d("FacebookLogin", "onCancel");
                        Toast.makeText(LoginActivity.this, "Đăng nhập Facebook bị hủy", Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onError(FacebookException exception) {
                        Log.e("FacebookLogin", "onError", exception);
                        Toast.makeText(LoginActivity.this, "Đăng nhập Facebook lỗi: " + exception.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void logKeyHash() {
        try {
            PackageInfo info = getPackageManager().getPackageInfo(
                    getPackageName(),
                    PackageManager.GET_SIGNATURES);
            if (info.signatures != null) {
                for (Signature signature : info.signatures) {
                    MessageDigest md = MessageDigest.getInstance("SHA");
                    md.update(signature.toByteArray());
                    String hash = Base64.encodeToString(md.digest(), Base64.DEFAULT);
                    Log.d("KeyHash:", hash);
                }
            }
        } catch (PackageManager.NameNotFoundException | NoSuchAlgorithmException e) {
            Log.e("KeyHash", "Error getting key hash", e);
        }
    }

    private void handleFacebookAccessToken(AccessToken token) {
        com.google.firebase.auth.AuthCredential credential = FacebookAuthProvider.getCredential(token.getToken());
        FirebaseAuth.getInstance().signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                        String email = user != null ? user.getEmail() : "";
                        String name = user != null ? user.getDisplayName() : "Facebook User";
                        completeSocialLogin("Facebook", email != null ? email : "", name);
                    } else {
                        Toast.makeText(this, "Firebase Facebook Auth failed: " + task.getException().getMessage(),
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (callbackManager != null) {
            callbackManager.onActivityResult(requestCode, resultCode, data);
        }
    }

    private void navigateToRegister() {
        startActivity(new Intent(this, RegisterActivity.class));
    }
}
