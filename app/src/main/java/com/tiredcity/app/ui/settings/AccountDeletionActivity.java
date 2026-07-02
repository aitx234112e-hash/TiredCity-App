package com.tiredcity.app.ui.settings;

import android.os.Bundle;
import android.widget.Toast;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.tiredcity.app.R;
import com.tiredcity.app.adapter.DeletionReasonAdapter;
import com.tiredcity.app.data.model.DeletionReason;
import com.tiredcity.app.databinding.ActivityAccountDeletionBinding;
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
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.settings_general_delete_title)
                .setMessage(R.string.settings_delete_warning)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(R.string.settings_delete_confirm, (d, w) -> {
                    Toast.makeText(this, R.string.settings_delete_submitted, Toast.LENGTH_LONG).show();
                    finish();
                })
                .show();
    }
}
