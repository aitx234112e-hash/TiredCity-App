package com.tiredcity.app.ui.main;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.tiredcity.app.R;
import com.tiredcity.app.adapter.StylingAdapter;
import com.tiredcity.app.data.model.CategoryItem;
import com.tiredcity.app.databinding.FragmentStylingBinding;
import com.tiredcity.app.ui.shop.CategoryActivity;
import java.util.ArrayList;
import java.util.List;

public class StylingFragment extends Fragment {

    private FragmentStylingBinding binding;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentStylingBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setupCategoryList();
        binding.tvViewAll.setOnClickListener(v -> {
            Intent intent = new Intent(requireContext(), CategoryActivity.class);
            intent.putExtra(CategoryActivity.EXTRA_CATEGORY_NAME,
                getString(R.string.title_category));
            startActivity(intent);
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    private void setupCategoryList() {
        List<CategoryItem> items = buildCategories();
        StylingAdapter adapter = new StylingAdapter(items);
        adapter.setOnCategoryClickListener(item -> {
            Intent intent = new Intent(requireContext(), CategoryActivity.class);
            intent.putExtra(CategoryActivity.EXTRA_CATEGORY_NAME,
                getString(item.getNameResId()));
            startActivity(intent);
        });
        binding.rvCategories.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvCategories.setAdapter(adapter);
    }

    private List<CategoryItem> buildCategories() {
        @ColorInt int red   = requireContext().getColor(R.color.tc_red);
        @ColorInt int dark  = requireContext().getColor(R.color.tc_espresso);
        @ColorInt int gold  = requireContext().getColor(R.color.tc_gold);
        @ColorInt int subtle = requireContext().getColor(R.color.tc_bg_subtle);

        List<CategoryItem> list = new ArrayList<>();
        list.add(new CategoryItem(1,
            R.string.cat_ao_dai, R.string.desc_ao_dai, 0, red));
        list.add(new CategoryItem(2,
            R.string.cat_ao_tac, R.string.desc_ao_tac, 0, dark));
        list.add(new CategoryItem(3,
            R.string.cat_nhat_binh, R.string.desc_nhat_binh, 0, gold));
        list.add(new CategoryItem(4,
            R.string.cat_phu_kien, R.string.desc_phu_kien, 0, subtle));
        return list;
    }
}
