package com.tiredcity.app.ui.cart;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.tiredcity.app.R;
import com.tiredcity.app.databinding.BottomSheetSlideCaptchaBinding;

/** Slider Captcha (kéo mảnh ghép) trước khi lưu đơn hàng vào Firestore. */
public class SlideCaptchaBottomSheet extends BottomSheetDialogFragment {

    public interface OnVerifiedListener {
        void onVerified();
    }

    private BottomSheetSlideCaptchaBinding binding;
    private OnVerifiedListener listener;

    public void setOnVerifiedListener(OnVerifiedListener listener) {
        this.listener = listener;
    }

    @Override
    public int getTheme() {
        return R.style.Theme_TC_BottomSheetDialog;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                              @Nullable Bundle savedInstanceState) {
        binding = BottomSheetSlideCaptchaBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setCancelable(true);

        binding.puzzleSlider.setOnSlideCompleteListener(() -> {
            binding.tvCaptchaStatus.setText(R.string.captcha_verified);
            binding.tvCaptchaStatus.setTextColor(ContextCompat.getColor(requireContext(), R.color.tc_success));
            view.postDelayed(() -> {
                if (listener != null) listener.onVerified();
                dismissAllowingStateLoss();
            }, 350);
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
