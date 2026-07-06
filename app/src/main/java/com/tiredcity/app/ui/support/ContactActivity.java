package com.tiredcity.app.ui.support;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.MenuItem;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import com.tiredcity.app.R;
import com.tiredcity.app.databinding.ActivityContactBinding;
import com.tiredcity.app.ui.base.BaseActivity;

public class ContactActivity extends BaseActivity {

    private ActivityContactBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityContactBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Header trắng — status bar cũng trắng + icon tối cho đồng bộ (thay header đỏ cũ)
        getWindow().setStatusBarColor(ContextCompat.getColor(this, R.color.card_white));
        new WindowInsetsControllerCompat(getWindow(), binding.getRoot())
                .setAppearanceLightStatusBars(true);

        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }

        binding.layoutStore1.setOnClickListener(v ->
                openMaps("TiredCity 37 Hàng Hành, Hà Nội"));
        binding.layoutStore2.setOnClickListener(v ->
                openMaps("TiredCity 97 Hàng Gai, Hà Nội"));

        binding.btnSendMessage.setOnClickListener(v -> {
            // TODO: gửi form liên hệ
        });
    }

    private void openMaps(String query) {
        Uri gmmIntentUri = Uri.parse("geo:0,0?q=" + Uri.encode(query));
        Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
        mapIntent.setPackage("com.google.android.apps.maps");
        if (mapIntent.resolveActivity(getPackageManager()) != null) {
            startActivity(mapIntent);
        } else {
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
