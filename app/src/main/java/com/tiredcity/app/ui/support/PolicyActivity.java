package com.tiredcity.app.ui.support;

import android.os.Bundle;
import android.view.MenuItem;
import androidx.annotation.NonNull;
import com.tiredcity.app.databinding.ActivityPolicyBinding;
import com.tiredcity.app.ui.base.BaseActivity;

public class PolicyActivity extends BaseActivity {

    private ActivityPolicyBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityPolicyBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setupToolbar();
    }

    private void setupToolbar() {
        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Chính sách");
        }
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
