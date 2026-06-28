package com.tiredcity.app.ui.main;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.google.android.material.tabs.TabLayout;
import com.tiredcity.app.databinding.FragmentExploreBinding;
import com.tiredcity.app.ui.support.ContactActivity;

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

        binding.tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                if (tab.getPosition() == 1) {
                    // Tab "Store Location" → mở ContactActivity, revert về tab About
                    startActivity(new Intent(requireContext(), ContactActivity.class));
                    TabLayout.Tab aboutTab = binding.tabLayout.getTabAt(0);
                    if (aboutTab != null) aboutTab.select();
                }
                // Tab "About" (vị trí 0) hiển thị AboutFragment đã nhúng trong layout.
            }
            @Override public void onTabUnselected(TabLayout.Tab tab) {}
            @Override public void onTabReselected(TabLayout.Tab tab) {}
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
