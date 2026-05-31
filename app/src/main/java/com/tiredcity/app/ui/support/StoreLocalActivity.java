package com.tiredcity.app.ui.support;

import android.os.Bundle;
import android.view.MenuItem;
import androidx.annotation.NonNull;
import com.tiredcity.app.databinding.ActivityStoreLocationBinding;
import com.tiredcity.app.ui.base.BaseActivity;

public class StoreLocalActivity extends BaseActivity {

    private ActivityStoreLocationBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityStoreLocationBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Hệ thống cửa hàng");
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
