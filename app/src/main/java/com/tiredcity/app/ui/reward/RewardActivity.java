package com.tiredcity.app.ui.reward;

import android.os.Bundle;
import com.google.android.material.tabs.TabLayoutMediator;
import com.tiredcity.app.R;
import com.tiredcity.app.databinding.ActivityRewardBinding;
import com.tiredcity.app.ui.base.BaseActivity;

/** Trang "Ưu đãi" — 4 tab voucher (Tất cả · Sinh nhật · Last chance · Nổi bật). */
public class RewardActivity extends BaseActivity {

    private ActivityRewardBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityRewardBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        initView();
        initData();
    }

    private void initView() {
        binding.btnBack.setOnClickListener(v -> finish());
    }

    private void initData() {
        binding.viewPager.setAdapter(new RewardPagerAdapter(this));

        final int[] tabTitles = {
                R.string.reward_tab_all,
                R.string.reward_tab_birthday,
                R.string.reward_tab_last_chance,
                R.string.reward_tab_featured
        };

        new TabLayoutMediator(binding.tabLayout, binding.viewPager,
                (tab, position) -> tab.setText(getString(tabTitles[position])))
                .attach();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }
}
