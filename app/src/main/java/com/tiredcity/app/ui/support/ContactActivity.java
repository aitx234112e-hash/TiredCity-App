package com.tiredcity.app.ui.support;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.MenuItem;
import androidx.annotation.NonNull;
import com.tiredcity.app.databinding.ActivityContactBinding;
import com.tiredcity.app.ui.base.BaseActivity;

public class ContactActivity extends BaseActivity {

    private ActivityContactBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityContactBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setupToolbar();
        setupClickListeners();
    }

    private void setupToolbar() {
        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Liên hệ");
        }
    }

    private void setupClickListeners() {
        binding.btnBack.setOnClickListener(v -> onBackPressed());

        binding.btnSendMessage.setOnClickListener(v -> {
            // Handle send message
        });

        // Map and Store clicks
        binding.mapCard.setOnClickListener(v -> openMaps("TiredCity Hanoi"));

        binding.layoutStore1.setOnClickListener(v -> openMaps("TiredCity 37 Hàng Hành"));
        binding.layoutStore2.setOnClickListener(v -> openMaps("TiredCity 97 Hàng Gai"));
        binding.layoutStore3.setOnClickListener(v -> openMaps("TiredCity 19 Nhà Thờ"));
        binding.layoutStore4.setOnClickListener(v -> openMaps("TiredCity 100 Hàng Đào"));
        binding.layoutStore5.setOnClickListener(v -> openMaps("TiredCity 8 Lương Ngọc Quyến"));
    }

    private void openMaps(String query) {
        Uri gmmIntentUri = Uri.parse("geo:0,0?q=" + Uri.encode(query));
        Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
        mapIntent.setPackage("com.google.android.apps.maps");
        if (mapIntent.resolveActivity(getPackageManager()) != null) {
            startActivity(mapIntent);
        } else {
            // Fallback to browser
            Intent browserIntent = new Intent(Intent.ACTION_VIEW, 
                Uri.parse("https://www.google.com/maps/search/?api=1&query=" + Uri.encode(query)));
            startActivity(browserIntent);
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
