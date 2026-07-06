package com.tiredcity.app.ui.cart;

import android.app.Dialog;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.DialogFragment;

import com.tiredcity.app.R;
import com.tiredcity.app.databinding.BottomSheetSlideCaptchaBinding;

/** Captcha ghép hình nổi giữa màn hình, hiện trước khi lưu đơn hàng vào Firestore. */
public class SlideCaptchaBottomSheet extends DialogFragment {

    public interface OnVerifiedListener {
        void onVerified();
    }

    private BottomSheetSlideCaptchaBinding binding;
    private OnVerifiedListener listener;

    public void setOnVerifiedListener(OnVerifiedListener listener) {
        this.listener = listener;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(STYLE_NO_TITLE, 0);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                              @Nullable Bundle savedInstanceState) {
        binding = BottomSheetSlideCaptchaBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onStart() {
        super.onStart();
        Dialog dialog = getDialog();
        if (dialog != null && dialog.getWindow() != null) {
            Window window = dialog.getWindow();
            window.setBackgroundDrawableResource(android.R.color.transparent);
            int width = (int) (getResources().getDisplayMetrics().widthPixels * 0.92f);
            window.setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT);
            window.setGravity(Gravity.CENTER);
        }
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setCancelable(true);

        binding.btnCaptchaRefresh.setOnClickListener(v -> {
            binding.puzzleImage.reload();
            binding.puzzleSlider.reset();
            resetStatus();
        });

        binding.puzzleSlider.setListener(new com.tiredcity.app.ui.widget.PuzzleSliderView.Listener() {
            @Override
            public void onProgress(float progress) {
                binding.puzzleImage.setProgress(progress);
            }

            @Override
            public boolean onReleased(float progress) {
                if (binding.puzzleImage.checkSolved()) {
                    binding.puzzleSlider.lockSolved();
                    binding.tvCaptchaStatus.setText(R.string.captcha_verified);
                    binding.tvCaptchaStatus.setTextColor(
                            ContextCompat.getColor(requireContext(), R.color.tc_success));
                    view.postDelayed(() -> {
                        if (listener != null) listener.onVerified();
                        dismissAllowingStateLoss();
                    }, 400);
                    return true;
                }
                // chưa khớp → báo lỗi và trả mảnh ghép về đầu
                binding.puzzleImage.resetPiece();
                binding.tvCaptchaStatus.setText(R.string.captcha_failed);
                binding.tvCaptchaStatus.setTextColor(
                        ContextCompat.getColor(requireContext(), R.color.tc_error));
                return false;
            }
        });
    }

    private void resetStatus() {
        binding.tvCaptchaStatus.setText(R.string.captcha_instruction);
        binding.tvCaptchaStatus.setTextColor(
                ContextCompat.getColor(requireContext(), R.color.text_secondary));
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
