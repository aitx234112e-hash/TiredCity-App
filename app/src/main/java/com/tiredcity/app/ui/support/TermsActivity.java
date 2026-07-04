package com.tiredcity.app.ui.support;

import android.os.Build;
import android.os.Bundle;
import android.text.Html;
import com.tiredcity.app.R;
import com.tiredcity.app.databinding.ActivityTermsBinding;
import com.tiredcity.app.ui.base.BaseActivity;

public class TermsActivity extends BaseActivity {

    private ActivityTermsBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityTermsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        binding.toolbar.setNavigationOnClickListener(v -> finish());

        String content = getString(R.string.terms_content);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            binding.tvTermsContent.setText(Html.fromHtml(content, Html.FROM_HTML_MODE_COMPACT));
        } else {
            binding.tvTermsContent.setText(Html.fromHtml(content));
        }
    }
}
