package com.tiredcity.app.ui.settings;

import android.content.Intent;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserInfo;
import com.google.firebase.firestore.FirebaseFirestore;
import com.tiredcity.app.R;
import com.tiredcity.app.adapter.DeletionReasonAdapter;
import com.tiredcity.app.data.model.DeletionReason;
import com.tiredcity.app.databinding.ActivityAccountDeletionBinding;
import com.tiredcity.app.ui.auth.LoginActivity;
import com.tiredcity.app.ui.base.BaseActivity;
import java.util.Arrays;
import java.util.List;

/**
 * Yêu cầu xóa tài khoản — chọn lý do (RecyclerView), nút gửi chỉ bật khi đã chọn.
 */
public class AccountDeletionActivity extends BaseActivity {

    private ActivityAccountDeletionBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityAccountDeletionBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.btnBack.setOnClickListener(v -> finish());

        List<DeletionReason> reasons = Arrays.asList(
                new DeletionReason(R.string.settings_delete_reason1_title, R.string.settings_delete_reason1_desc),
                new DeletionReason(R.string.settings_delete_reason2_title, R.string.settings_delete_reason2_desc),
                new DeletionReason(R.string.settings_delete_reason3_title, R.string.settings_delete_reason3_desc),
                new DeletionReason(R.string.settings_delete_reason4_title, R.string.settings_delete_reason4_desc),
                new DeletionReason(R.string.settings_delete_reason5_title, R.string.settings_delete_reason5_desc));

        DeletionReasonAdapter adapter = new DeletionReasonAdapter(reasons,
                position -> binding.btnConfirmDelete.setEnabled(true));
        binding.rvReasons.setLayoutManager(new LinearLayoutManager(this));
        binding.rvReasons.setAdapter(adapter);

        binding.btnConfirmDelete.setOnClickListener(v -> confirmDeletion());
    }

    private void confirmDeletion() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        boolean isEmailUser = false;
        for (UserInfo profile : user.getProviderData()) {
            if (EmailAuthProvider.PROVIDER_ID.equals(profile.getProviderId())) {
                isEmailUser = true;
                break;
            }
        }

        if (isEmailUser && user.getEmail() != null) {
            showPasswordConfirmationDialog(user);
        } else {
            // Social users don't have passwords, but still need recent login
            showSocialDeletionDialog(user);
        }
    }

    private void showPasswordConfirmationDialog(FirebaseUser user) {
        EditText etPassword = new EditText(this);
        etPassword.setHint("Nhập mật khẩu để xác nhận");
        etPassword.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);
        
        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        params.setMargins(60, 20, 60, 0);
        etPassword.setLayoutParams(params);
        container.addView(etPassword);

        new MaterialAlertDialogBuilder(this)
                .setTitle("Xác nhận xóa tài khoản")
                .setMessage("Dữ liệu sẽ không thể khôi phục. Vui lòng nhập mật khẩu để tiếp tục.")
                .setView(container)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton("XÓA VĨNH VIỄN", (d, w) -> {
                    String password = etPassword.getText().toString();
                    if (password.isEmpty()) {
                        Toast.makeText(this, "Vui lòng nhập mật khẩu", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    reauthenticateAndDelete(user, password);
                })
                .show();
    }

    private void showSocialDeletionDialog(FirebaseUser user) {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Xác nhận xóa tài khoản")
                .setMessage("Bạn đang đăng nhập bằng mạng xã hội. Việc này sẽ xóa toàn bộ dữ liệu mua hàng và thông tin cá nhân. Bạn có chắc chắn muốn xóa?")
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton("XÓA TÀI KHOẢN", (d, w) -> {
                    performAccountDeletion(user);
                })
                .show();
    }

    private void reauthenticateAndDelete(FirebaseUser user, String password) {
        binding.btnConfirmDelete.setEnabled(false);
        AuthCredential credential = EmailAuthProvider.getCredential(user.getEmail(), password);
        
        user.reauthenticate(credential).addOnCompleteListener(reauthTask -> {
            if (reauthTask.isSuccessful()) {
                performAccountDeletion(user);
            } else {
                binding.btnConfirmDelete.setEnabled(true);
                Toast.makeText(this, "Mật khẩu không chính xác hoặc phiên đã hết hạn", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void performAccountDeletion(FirebaseUser user) {
        binding.btnConfirmDelete.setEnabled(false);
        String uid = user.getUid();

        // 1. Xóa dữ liệu Firestore (Thông tin cá nhân)
        FirebaseFirestore.getInstance().collection("users").document(uid).delete()
                .addOnCompleteListener(task -> {
                    // 2. Xóa tài khoản Firebase Auth
                    user.delete().addOnCompleteListener(deleteTask -> {
                        if (deleteTask.isSuccessful()) {
                            Toast.makeText(this, R.string.settings_delete_submitted, Toast.LENGTH_LONG).show();
                            preferenceManager.clearAll();
                            
                            Intent intent = new Intent(this, LoginActivity.class);
                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                            startActivity(intent);
                            finish();
                        } else {
                            binding.btnConfirmDelete.setEnabled(true);
                            String error = deleteTask.getException() != null ? deleteTask.getException().getMessage() : "Unknown error";
                            Toast.makeText(this, "Lỗi khi xóa tài khoản: " + error, Toast.LENGTH_LONG).show();
                        }
                    });
                });
    }
}
