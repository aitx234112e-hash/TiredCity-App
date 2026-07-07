package com.tiredcity.app.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;

import com.facebook.AccessToken;
import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.login.LoginManager;
import com.facebook.login.LoginResult;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.android.material.datepicker.CalendarConstraints;
import com.google.android.material.datepicker.DateValidatorPointBackward;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.firebase.FirebaseNetworkException;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthInvalidUserException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;

import com.tiredcity.app.R;
import com.tiredcity.app.data.model.UserProfile;
import com.tiredcity.app.data.network.ApiClient;
import com.tiredcity.app.data.repository.AuthRepository;
import com.tiredcity.app.data.repository.FirestoreUserRepository;
import com.tiredcity.app.databinding.ActivityLoginBinding;
import com.tiredcity.app.ui.base.BaseActivity;
import com.tiredcity.app.ui.onboarding.OnboardingActivity;
import com.tiredcity.app.utils.LocaleHelper;
import com.tiredcity.app.utils.MenhCalculator;

import java.util.Arrays;
import java.util.Calendar;
import java.util.Locale;
import java.util.TimeZone;

public class LoginActivity extends BaseActivity {

    private static final String DEMO_EMAIL = "demo@demo.com";
    private static final String DEMO_PASSWORD = "123456";
    private static final String DEMO_TOKEN = "demo-token-local";
    private static final String DEMO_USER_ID = "demo-user";

    private static final String ADMIN_PACKAGE = "com.tiredcity.admin";
    private static final String EXTRA_ADMIN_EMAIL = "com.tiredcity.admin.extra.EMAIL";
    private static final String EXTRA_ADMIN_PASSWORD = "com.tiredcity.admin.extra.PASSWORD";

    private ActivityLoginBinding binding;
    private AuthRepository authRepository;
    private boolean pendingAdminToggleReset = false;

    private GoogleSignInClient googleSignInClient;
    private ActivityResultLauncher<Intent> googleSignInLauncher;
    private final FirestoreUserRepository userRepository = new FirestoreUserRepository();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        authRepository = new AuthRepository(ApiClient.getApiService(null), preferenceManager);

        setupSplashOverlay();
        setupRoleToggle();
        setupGoogleSignIn();
        setupFacebookSignIn();
        restoreSavedCredentials();

        binding.btnLogin.setOnClickListener(v -> {
            String email = binding.etEmail.getText().toString().trim();
            String pass = binding.etPassword.getText().toString().trim();

            if (TextUtils.isEmpty(email)) {
                binding.etEmail.setError(getString(R.string.error_invalid_email));
                return;
            }
            if (TextUtils.isEmpty(pass)) {
                binding.etPassword.setError(getString(R.string.error_login_failed));
                return;
            }

            persistRememberMe(email, pass);

            // Kiểm tra toggle vai trò qua ID của button được chọn
            int checkedId = binding.toggleRole.getCheckedButtonId();
            if (checkedId == R.id.btn_role_admin) {
                openAdminApp(email, pass);
            } else {
                attemptLogin(email, pass);
            }
        });

        binding.btnGoogle.setOnClickListener(v -> launchGoogleSignIn());
        binding.btnFacebook.setOnClickListener(v -> LoginManager.getInstance().logInWithReadPermissions(this, Arrays.asList("public_profile", "email")));

        binding.tvRegister.setOnClickListener(v -> navigateToRegister());
        binding.tvForgotPassword.setOnClickListener(v -> startActivity(new Intent(this, ForgotPasswordActivity.class)));

        binding.btnLanguage.setOnClickListener(v -> {
            LocaleHelper.toggleLanguage(this);
            recreate();
        });
    }

    private void setupRoleToggle() {
        binding.toggleRole.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (isChecked) {
                binding.btnLogin.setText(checkedId == R.id.btn_role_admin ? "LOGIN AS ADMIN" : getString(R.string.btn_login));
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (pendingAdminToggleReset) {
            binding.toggleRole.check(R.id.btn_role_customer);
            pendingAdminToggleReset = false;
        }
    }

    private void openAdminApp(String email, String pass) {
        Intent launchIntent = getPackageManager().getLaunchIntentForPackage(ADMIN_PACKAGE);
        if (launchIntent != null) {
            launchIntent.putExtra(EXTRA_ADMIN_EMAIL, email);
            launchIntent.putExtra(EXTRA_ADMIN_PASSWORD, pass);
            pendingAdminToggleReset = true;
            startActivity(launchIntent);
        } else {
            Toast.makeText(this, R.string.admin_app_not_installed, Toast.LENGTH_LONG).show();
            binding.toggleRole.check(R.id.btn_role_customer);
        }
    }

    private void setupGoogleSignIn() {
        String webClientId = getString(R.string.default_web_client_id);
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(webClientId)
                .requestEmail()
                .build();
        googleSignInClient = GoogleSignIn.getClient(this, gso);

        googleSignInLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                handleGoogleResult(result.getData());
            }
        });
    }

    private void launchGoogleSignIn() {
        googleSignInClient.signOut().addOnCompleteListener(t -> googleSignInLauncher.launch(googleSignInClient.getSignInIntent()));
    }

    private void handleGoogleResult(Intent data) {
        Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
        try {
            GoogleSignInAccount account =
                    GoogleSignIn.getSignedInAccountFromIntent(data).getResult(ApiException.class);
            String email    = account.getEmail();
            String name     = account.getDisplayName();
            String idToken  = account.getIdToken();
            String avatar   = account.getPhotoUrl() != null ? account.getPhotoUrl().toString() : null;

            if (idToken == null) {
                // Chưa cấu hình Web Client ID thật → không có idToken để xác thực qua Firebase,
                // đành đăng nhập cục bộ (không có FirebaseUser/uid) như trước đây.
                completeSocialLogin("Google", email != null ? email : "", name, null);
                return;
            }

            // Xác thực THẬT qua Firebase Auth bằng idToken của Google → có FirebaseUser.uid
            // dùng chung cho Firestore (thay vì chỉ lấy email/tên cục bộ như trước).
            AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
            binding.btnGoogle.setEnabled(false);
            FirebaseAuth.getInstance().signInWithCredential(credential)
                    .addOnSuccessListener(this, result -> {
                        binding.btnGoogle.setEnabled(true);
                        FirebaseUser firebaseUser = result.getUser();
                        String uid = firebaseUser != null ? firebaseUser.getUid() : null;
                        if (uid != null) {
                            userRepository.syncUser(uid, email, name, avatar, "google");
                        }
                        completeSocialLogin("Google", email != null ? email : "", name, uid);
                    })
                    .addOnFailureListener(this, e -> {
                        binding.btnGoogle.setEnabled(true);
                        // Log chi tiết thật của lỗi (vd "CONFIGURATION_NOT_FOUND" khi provider Google
                        // chưa được bật trong Firebase Console → Authentication → Sign-in method) để
                        // chẩn đoán qua Logcat thay vì chỉ thấy thông báo chung chung.
                        android.util.Log.e("LoginActivity", "Firebase signInWithCredential(Google) thất bại", e);
                        String detail = e.getMessage();
                        String msg = getString(R.string.google_signin_failed)
                                + (detail != null ? " (" + detail + ")" : "");
                        Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
                    });
        } catch (ApiException e) {
            if (e.getStatusCode() != GoogleSignInStatusCodes.SIGN_IN_CANCELLED) {
                Toast.makeText(this,
                        getString(R.string.google_signin_failed) + " (" + e.getStatusCode() + ")",
                        Toast.LENGTH_SHORT).show();
            }
        } catch (ApiException e) {
            Log.e("Login", "Google sign in failed", e);
            Toast.makeText(this, "Google Sign-in failed: " + e.getStatusCode(), Toast.LENGTH_SHORT).show();
        }
    }

    private void setupSplashOverlay() {
        new Handler().postDelayed(() -> {
            binding.splashOverlay.animate().alpha(0f).setDuration(400).withEndAction(() -> binding.splashOverlay.setVisibility(View.GONE)).start();
        }, 1200);
    }

    private void restoreSavedCredentials() {
        if (preferenceManager.isRememberMeEnabled()) {
            binding.etEmail.setText(preferenceManager.getSavedEmail());
            binding.etPassword.setText(preferenceManager.getSavedPassword());
            binding.cbRememberMe.setChecked(true);
        }
    }

    private void persistRememberMe(String email, String pass) {
        boolean remember = binding.cbRememberMe.isChecked();
        preferenceManager.setRememberMe(remember);
        if (remember) {
            preferenceManager.saveCredentials(email, pass);
        } else {
            preferenceManager.clearCredentials();
        }
    }

    private void attemptLogin(String email, String password) {
        if (DEMO_EMAIL.equals(email) && DEMO_PASSWORD.equals(password)) {
            binding.btnLogin.setEnabled(false);
            FirebaseAuth.getInstance().signInAnonymously()
                    .addOnCompleteListener(this, t -> {
                        binding.btnLogin.setEnabled(true);
                        completeLogin(email);
                    });
            return;
        }

        binding.btnLogin.setEnabled(false);
        FirebaseAuth.getInstance().signInWithEmailAndPassword(email, password)
                .addOnSuccessListener(this, result -> {
                    binding.btnLogin.setEnabled(true);
                    completeLogin(email);
                })
                .addOnFailureListener(this, e -> {
                    binding.btnLogin.setEnabled(true);
                    binding.etPassword.setError(authErrorMessage(e));
                    binding.etPassword.requestFocus();
                });
    }

    private String authErrorMessage(Exception e) {
        if (e instanceof FirebaseAuthInvalidUserException || e instanceof FirebaseAuthInvalidCredentialsException) {
            return getString(R.string.error_login_failed);
        }
        if (e instanceof FirebaseNetworkException) {
            return getString(R.string.error_network_auth);
        }
        return e.getLocalizedMessage() != null ? e.getLocalizedMessage() : getString(R.string.error_login_failed);
    }

    private void completeLogin(String email) {
        preferenceManager.saveToken(DEMO_TOKEN);
        preferenceManager.saveUserId(TextUtils.isEmpty(email) ? DEMO_USER_ID : email);

        // Hiển thị TÊN khách ở Home:
        //  - Nếu đã ĐĂNG KÝ (hồ sơ có sẵn tên đầy đủ) → GIỮ NGUYÊN tên đó.
        //  - Nếu chưa từng đăng ký → tạm lấy phần trước dấu @ làm tên. Không còn "Guest".
        UserProfile profile = preferenceManager.getUser();
        boolean hasRealName = profile != null
                && profile.getName() != null && !profile.getName().trim().isEmpty();
        if (!hasRealName) {
            if (profile == null) profile = new UserProfile();
            profile.setEmail(email);
            String raw = email.contains("@") ? email.substring(0, email.indexOf('@')) : email;
            profile.setName(prettifyName(raw));
            preferenceManager.saveUser(profile);
        }

        ApiClient.reset();
        startActivity(new Intent(LoginActivity.this, OnboardingActivity.class));
        finishAffinity();
    }

    /**
     * Đăng nhập mạng xã hội — bản demo offline.
     * Khi có backend: thay bằng Google Sign-In / Facebook Login SDK / Apple, lấy token rồi gọi server.
     */
    private void socialLogin(String provider, String email) {
        completeSocialLogin(provider, email, null, null);
    }

    /**
     * Hoàn tất đăng nhập mạng xã hội. {@code displayName} là tên thật lấy từ tài khoản
     * (vd Google account); nếu null sẽ suy ra từ email hoặc dùng "{provider} User".
     * {@code uid} là Firebase Auth UID thật (chỉ có khi đã xác thực qua signInWithCredential,
     * vd Google); null nếu đăng nhập cục bộ không qua Firebase (vd Facebook demo).
     */
    private void completeSocialLogin(String provider, String email, String displayName, String uid) {
        preferenceManager.saveToken(DEMO_TOKEN);
        preferenceManager.saveUserId(!TextUtils.isEmpty(uid) ? uid
                : (TextUtils.isEmpty(email) ? provider : email));

        UserProfile profile = preferenceManager.getUser();
        if (profile == null) profile = new UserProfile();
        if (!TextUtils.isEmpty(uid)) {
            profile.setUid(uid);
            // profile.id chưa từng được gán ở nơi khác trong luồng đăng nhập cục bộ này —
            // gán bằng uid thật để đơn hàng (PaymentActivity) gắn đúng chủ tài khoản Firebase.
            profile.setId(uid);
        }
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

        ApiClient.reset();
        Toast.makeText(this, provider, Toast.LENGTH_SHORT).show();

        // Đăng nhập mạng xã hội (Google/Facebook/Apple) KHÔNG trả về ngày sinh/SĐT/địa chỉ,
        // và avatar vẫn giữ logo con gà TiredCity mặc định (không lấy ảnh từ tài khoản Google).
        // → Nếu hồ sơ chưa có ngày sinh, mở lịch nhập ngày sinh để tính Ngũ Hành mệnh NGAY.
        if (TextUtils.isEmpty(profile.getBirthDate())) {
            promptBirthDateForMenh();
        } else {
            goToOnboarding();
            return;
        }

        user.getIdToken(true).addOnCompleteListener(task -> {
            String token = task.isSuccessful() ? task.getResult().getToken() : DEMO_TOKEN;
            String uid = user.getUid();
            preferenceManager.saveToken(token);
            preferenceManager.saveUserId(uid);

            UserProfile profile = preferenceManager.getUser();
            if (profile == null) profile = new UserProfile();
            profile.setId(uid);

            if (profile.getName() == null || profile.getName().isEmpty()) {
                profile.setEmail(email);
                String raw = (email != null && email.contains("@")) ? email.substring(0, email.indexOf('@')) : (email != null ? email : "User");
                profile.setName(prettifyName(raw));
                preferenceManager.saveUser(profile);
            }

            if (authRepository == null) {
                authRepository = new AuthRepository(ApiClient.getApiService(null), preferenceManager);
            }
            authRepository.syncUserProfileToFirestore(profile);
            ApiClient.reset();
            goToOnboarding();
        });
    }

    private void completeSocialLogin(String provider, String email, String displayName) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            user.getIdToken(true).addOnCompleteListener(tokenTask -> {
                String token = tokenTask.isSuccessful() ? tokenTask.getResult().getToken() : DEMO_TOKEN;
                preferenceManager.saveToken(token);
                preferenceManager.saveUserId(user.getUid());

                UserProfile profile = preferenceManager.getUser();
                if (profile == null) profile = new UserProfile();
                profile.setId(user.getUid());

                if (TextUtils.isEmpty(profile.getName())) {
                    profile.setEmail(email);
                    String name = !TextUtils.isEmpty(displayName) ? displayName : (email != null && email.contains("@") ? prettifyName(email.substring(0, email.indexOf('@'))) : provider + " User");
                    profile.setName(name);
                }
                preferenceManager.saveUser(profile);

                if (authRepository == null) {
                    authRepository = new AuthRepository(ApiClient.getApiService(null), preferenceManager);
                }

                authRepository.syncAccount(profile, cloudProfile -> {
                    runOnUiThread(() -> {
                        ApiClient.reset();
                        if (cloudProfile != null && !TextUtils.isEmpty(cloudProfile.getBirthDate())) {
                            goToOnboarding();
                        } else {
                            promptBirthDateForMenh();
                        }
                    });
                });
            });
        }
    }

    private void goToOnboarding() {
        startActivity(new Intent(LoginActivity.this, OnboardingActivity.class));
        finishAffinity();
    }

    private void promptBirthDateForMenh() {
        Calendar c = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        c.clear();
        c.set(2000, 0, 1);
        long startSelection = c.getTimeInMillis();

        CalendarConstraints constraints = new CalendarConstraints.Builder().setValidator(DateValidatorPointBackward.now()).build();
        MaterialDatePicker<Long> picker = MaterialDatePicker.Builder.datePicker().setTitleText(R.string.hint_birthdate).setSelection(startSelection).setCalendarConstraints(constraints).build();

        picker.addOnPositiveButtonClickListener(selection -> {
            Calendar sel = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
            sel.setTimeInMillis(selection);
            int year = sel.get(Calendar.YEAR), month = sel.get(Calendar.MONTH) + 1, day = sel.get(Calendar.DAY_OF_MONTH);
            String menh = MenhCalculator.tinhMenh(year), zodiac = MenhCalculator.tinhCungHoangDao(month, day), animal = MenhCalculator.tinhConGiap(year);

            UserProfile profile = preferenceManager.getUser();
            if (profile == null) profile = new UserProfile();
            profile.setBirthDate(String.format(Locale.US, "%04d-%02d-%02d", year, month, day));
            profile.setMenh(menh); profile.setZodiac(zodiac); profile.setAnimal(animal);
            preferenceManager.saveUser(profile);
            preferenceManager.setMenh(menh); preferenceManager.setZodiac(zodiac);
            goToOnboarding();
        });

        picker.addOnNegativeButtonClickListener(v -> goToOnboarding());
        picker.addOnCancelListener(d -> goToOnboarding());
        picker.show(getSupportFragmentManager(), "social_birthdate_picker");
    }

    private String prettifyName(String raw) {
        if (TextUtils.isEmpty(raw)) return "User";
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

    private void setupFacebookSignIn() {
        callbackManager = CallbackManager.Factory.create();
        LoginManager.getInstance().registerCallback(callbackManager, new FacebookCallback<LoginResult>() {
            @Override public void onSuccess(LoginResult loginResult) { handleFacebookAccessToken(loginResult.getAccessToken()); }
            @Override public void onCancel() { Toast.makeText(LoginActivity.this, "Đăng nhập Facebook bị hủy", Toast.LENGTH_SHORT).show(); }
            @Override public void onError(FacebookException exception) { Toast.makeText(LoginActivity.this, "Đăng nhập Facebook lỗi: " + exception.getMessage(), Toast.LENGTH_SHORT).show(); }
        });
    }

    private void handleFacebookAccessToken(AccessToken token) {
        AuthCredential credential = FacebookAuthProvider.getCredential(token.getToken());
        FirebaseAuth.getInstance().signInWithCredential(credential)
                .addOnSuccessListener(authResult -> completeSocialLogin("Facebook", null, null))
                .addOnFailureListener(e -> Toast.makeText(this, "Facebook Auth Error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (callbackManager != null) callbackManager.onActivityResult(requestCode, resultCode, data);
    }

    private void navigateToRegister() {
        startActivity(new Intent(this, RegisterActivity.class));
    }
}
