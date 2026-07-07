package com.tiredcity.app.ui.main;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.google.android.material.tabs.TabLayout;
import com.tiredcity.app.R;
import com.tiredcity.app.databinding.FragmentExploreBinding;
import com.tiredcity.app.utils.EdgeToEdgeUtils;

public class ExploreFragment extends Fragment {

    private FragmentExploreBinding binding;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentExploreBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        EdgeToEdgeUtils.applyStatusBarTopPadding(binding.exploreHeader);

        // Lần đầu tạo view: nạp tab "Giới thiệu" (vị trí 0).
        if (savedInstanceState == null) {
            showAbout();
        }

        binding.tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                // Cả hai tab hiển thị nội dung ngay trong trang (giữ nguyên header trắng + tab),
                // không mở Activity riêng.
                if (tab.getPosition() == 1) {
                    showStore();
                } else {
                    showAbout();
                }
            }
            @Override public void onTabUnselected(TabLayout.Tab tab) {}
            @Override public void onTabReselected(TabLayout.Tab tab) {}
        });
    }

    private void showAbout() {
        getChildFragmentManager()
                .beginTransaction()
                .replace(R.id.about_container, new AboutFragment())
                .commit();
    }

    private void showStore() {
        getChildFragmentManager()
                .beginTransaction()
                .replace(R.id.about_container, new StoreFragment())
                .commit();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
