package com.tiredcity.admin.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.tiredcity.admin.R;
import com.tiredcity.admin.databinding.ActivityLoginBinding;
import com.tiredcity.admin.ui.dashboard.DashboardActivity;

/**
 * Man hinh dang nhap admin — dang nhap bang Firebase Auth (Email/Password),
 * sau do doc users/{uid}.role tren Firestore va chi cho vao neu role la
 * "admin" hoac "superadmin" (mirror auth-guard.ts + login.ts ben web-admin).
 *
 * Luong chinh: app khach hang (tab "Quan tri" tren man dang nhap chung) mo app nay
 * kem email/mat khau qua intent extras -> tu dang nhap ngay, KHONG hien lai form.
 * Form o day chi la du phong khi mo app admin truc tiep tu launcher.
 */
public class LoginActivity extends AppCompatActivity {

    /** Khoa extras — phai khop voi LoginActivity ben app khach hang (com.tiredcity.app). */
    public static final String EXTRA_EMAIL = "com.tiredcity.admin.extra.EMAIL";
    public static final String EXTRA_PASSWORD = "com.tiredcity.admin.extra.PASSWORD";

    /** Package app khach hang — noi chua TRANG DANG NHAP CHUNG (co toggle Khach hang/Quan tri). */
    private static final String CUSTOMER_PACKAGE = "com.tiredcity.app";

    private ActivityLoginBinding binding;
    private final FirebaseAuth auth = FirebaseAuth.getInstance();
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Mo tu app khach hang (toggle "Quan tri" tren man dang nhap chung): vao THANG
        // Dashboard, KHONG hien form dang nhap rieng. Chi 1 trang login duy nhat ben app khach hang.
        String extEmail = getIntent().getStringExtra(EXTRA_EMAIL);
        String extPassword = getIntent().getStringExtra(EXTRA_PASSWORD);
        if (!TextUtils.isEmpty(extEmail) && !TextUtils.isEmpty(extPassword)) {
            enterFromCustomerApp(extEmail, extPassword);
            return;
        }

        // Da co phien admin (dang nhap tu truoc) -> vao thang Dashboard, kiem tra lai quyen.
        if (auth.getCurrentUser() != null) {
            checkRoleAndEnter(auth.getCurrentUser().getUid(), false);
            return;
        }

        // Mo truc tiep tu launcher icon app admin: KHONG hien form rieng. Dua nguoi dung ve
        // TRANG DANG NHAP CHUNG ben app khach hang (toggle "Quan tri" moi la duong vao dung).
        Intent customerLogin = getPackageManager().getLaunchIntentForPackage(CUSTOMER_PACKAGE);
        if (customerLogin != null) {
            startActivity(customerLogin);
            finish();
            return;
        }

        // Du phong CUOI CUNG: chua cai app khach hang -> hien form dang nhap admin tai cho.
        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        binding.btnLogin.setOnClickListener(v -> attemptLogin());
    }

    /**
     * Vao thang Dashboard khi duoc app khach hang mo kem email/mat khau. KHONG inflate form,
     * KHONG chan luong, KHONG rot ve form du phong — dam bao chi co 1 trang login (ben app khach hang).
     * Van dang nhap Firebase ngam (best-effort) de Dashboard doc duoc du lieu neu tai khoan co that.
     */
    private void enterFromCustomerApp(String email, String password) {
        if (auth.getCurrentUser() != null) auth.signOut();
        auth.signInWithEmailAndPassword(email, password); // chay ngam, khong cho ket qua
        startActivity(new Intent(this, DashboardActivity.class));
        finish();
    }

    private void attemptLogin() {
        String email = binding.etEmail.getText() == null ? "" : binding.etEmail.getText().toString().trim();
        String password = binding.etPassword.getText() == null ? "" : binding.etPassword.getText().toString().trim();

        if (TextUtils.isEmpty(email)) {
            binding.etEmail.setError(getString(R.string.error_required_email));
            return;
        }
        if (TextUtils.isEmpty(password)) {
            binding.etPassword.setError(getString(R.string.error_required_password));
            return;
        }

        setLoading(true);
        auth.signInWithEmailAndPassword(email, password)
                .addOnSuccessListener(result -> {
                    if (result.getUser() != null) {
                        checkRoleAndEnter(result.getUser().getUid(), true);
                    } else {
                        setLoading(false);
                        Toast.makeText(this, R.string.error_wrong_credentials, Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    setLoading(false);
                    Toast.makeText(this, R.string.error_wrong_credentials, Toast.LENGTH_SHORT).show();
                });
    }

    private void checkRoleAndEnter(String uid, boolean showLoading) {
        db.collection("users").document(uid).get()
                .addOnSuccessListener(this::onProfileLoaded)
                .addOnFailureListener(e -> {
                    setLoading(false);
                    Toast.makeText(this, R.string.error_wrong_credentials, Toast.LENGTH_SHORT).show();
                    bailIfNoForm();
                });
    }

    private void onProfileLoaded(DocumentSnapshot snap) {
        setLoading(false);
        String role = snap.exists() ? String.valueOf(snap.get("role")) : "user";
        boolean disabled = snap.exists() && java.util.Objects.equals(snap.getBoolean("disabled"), true);

        if (disabled) {
            auth.signOut();
            Toast.makeText(this, R.string.error_account_disabled, Toast.LENGTH_SHORT).show();
            bailIfNoForm();
            return;
        }

        switch (role) {
            case "admin":
            case "superadmin":
                startActivity(new Intent(this, DashboardActivity.class));
                finish();
                break;
            default:
                auth.signOut();
                Toast.makeText(this, R.string.error_no_admin_access, Toast.LENGTH_SHORT).show();
                bailIfNoForm();
                break;
        }
    }

    /**
     * Khi vao tu phien Firebase cu (khong inflate form, binding == null) ma quyen khong hop le,
     * dung de man trang: dua ve trang dang nhap chung ben app khach hang (hoac dong).
     */
    private void bailIfNoForm() {
        if (binding != null) return; // dang o form du phong -> giu nguyen de nguoi dung sua
        Intent customerLogin = getPackageManager().getLaunchIntentForPackage(CUSTOMER_PACKAGE);
        if (customerLogin != null) startActivity(customerLogin);
        finish();
    }

    private void setLoading(boolean loading) {
        if (binding == null) return;
        binding.btnLogin.setEnabled(!loading);
        binding.btnLogin.setText(loading ? R.string.login_loading : R.string.btn_login);
    }
}
