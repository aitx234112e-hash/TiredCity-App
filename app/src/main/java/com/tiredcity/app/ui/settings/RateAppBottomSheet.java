package com.tiredcity.app.ui.settings;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.tiredcity.app.R;
import com.tiredcity.app.databinding.BottomSheetRateAppBinding;

/**
 * Đánh giá ứng dụng — bottom sheet với 5 sao vàng kim.
 * Nút "GỬI" chỉ bật khi người dùng chọn ít nhất 1 sao.
 */
public class RateAppBottomSheet extends BottomSheetDialogFragment {

    private BottomSheetRateAppBinding binding;

    @Override
    public int getTheme() {
        return R.style.Theme_TC_BottomSheetDialog;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = BottomSheetRateAppBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        binding.btnClose.setOnClickListener(v -> dismiss());

        binding.ratingBar.setOnRatingBarChangeListener((bar, rating, fromUser) ->
                binding.btnSend.setEnabled(rating >= 1f));

        binding.btnSend.setOnClickListener(v -> {
            Toast.makeText(requireContext(), R.string.settings_rate_thanks, Toast.LENGTH_SHORT).show();
            dismiss();
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
