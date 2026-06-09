package com.tiredcity.app.ui.support;

import android.os.Build;
import android.os.Bundle;
import android.text.Html;
import android.transition.AutoTransition;
import android.transition.TransitionManager;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import com.tiredcity.app.R;
import com.tiredcity.app.databinding.ActivityPolicyBinding;
import com.tiredcity.app.ui.base.BaseActivity;

/**
 * Simplified Policy Activity without RecyclerView/Adapter
 * Using static cards for better performance and easier maintenance
 */
public class PolicyActivity extends BaseActivity {

    private ActivityPolicyBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityPolicyBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setupToolbar();
        initContent();
        setupClickListeners();
    }

    private void setupToolbar() {
        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }
        binding.btnBack.setOnClickListener(v -> finish());
    }

    private void initContent() {
        // Set rich HTML content to TextViews
        setHtmlText(binding.tvContentPurchase, getString(R.string.policy_purchase_content));
        setHtmlText(binding.tvContentPayment, getString(R.string.policy_payment_content));
        setHtmlText(binding.tvContentShipping, getString(R.string.policy_shipping_content));
        setHtmlText(binding.tvContentReturn, getString(R.string.policy_return_title)); // Wait, checking strings...
        // Re-checking strings for return policy content
        setHtmlText(binding.tvContentReturn, getString(R.string.policy_return_content));
        setHtmlText(binding.tvContentPrivacy, getString(R.string.policy_privacy_content));
    }

    private void setHtmlText(TextView textView, String html) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            textView.setText(Html.fromHtml(html, Html.FROM_HTML_MODE_COMPACT));
        } else {
            textView.setText(Html.fromHtml(html));
        }
    }

    private void setupClickListeners() {
        // Card 1: Purchase
        binding.cardPurchase.setOnClickListener(v -> toggleCard(binding.cardPurchase, binding.contentPurchase, binding.ivExpandPurchase));
        
        // Card 2: Payment
        binding.cardPayment.setOnClickListener(v -> toggleCard(binding.cardPayment, binding.contentPayment, binding.ivExpandPayment));
        
        // Card 3: Shipping
        binding.cardShipping.setOnClickListener(v -> toggleCard(binding.cardShipping, binding.contentShipping, binding.ivExpandShipping));
        
        // Card 4: Return
        binding.cardReturn.setOnClickListener(v -> toggleCard(binding.cardReturn, binding.contentReturn, binding.ivExpandReturn));
        
        // Card 5: Privacy
        binding.cardPrivacy.setOnClickListener(v -> toggleCard(binding.cardPrivacy, binding.contentPrivacy, binding.ivExpandPrivacy));
    }

    private void toggleCard(ViewGroup card, View content, ImageView arrow) {
        boolean isExpanded = content.getVisibility() == View.VISIBLE;
        
        // Animation
        TransitionManager.beginDelayedTransition(card, new AutoTransition());
        
        if (isExpanded) {
            content.setVisibility(View.GONE);
            arrow.animate().rotation(0).setDuration(300).start();
        } else {
            content.setVisibility(View.VISIBLE);
            arrow.animate().rotation(180).setDuration(300).start();
        }
    }
}
