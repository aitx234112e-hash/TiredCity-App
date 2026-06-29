package com.tiredcity.app.ui.reward;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

/** Bốn tab của trang "Ưu đãi": Tất cả · Sinh nhật · Last chance · Nổi bật. */
public class RewardPagerAdapter extends FragmentStateAdapter {

    private static final int TAB_COUNT = 4;

    public RewardPagerAdapter(@NonNull FragmentActivity activity) {
        super(activity);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        switch (position) {
            case 1:  return RewardListFragment.newInstance(RewardListFragment.CATEGORY_BIRTHDAY);
            case 2:  return RewardListFragment.newInstance(RewardListFragment.CATEGORY_LAST_CHANCE);
            case 3:  return RewardListFragment.newInstance(RewardListFragment.CATEGORY_FEATURED);
            default: return RewardListFragment.newInstance(RewardListFragment.CATEGORY_ALL);
        }
    }

    @Override
    public int getItemCount() {
        return TAB_COUNT;
    }
}
