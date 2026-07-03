package com.tiredcity.app.ui.cart;

import android.os.Bundle;
import android.text.SpannableStringBuilder;
import android.text.style.ForegroundColorSpan;
import android.text.style.RelativeSizeSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RadioButton;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.tiredcity.app.R;
import com.tiredcity.app.data.model.ShippingOption;
import com.tiredcity.app.databinding.BottomSheetShippingMethodBinding;
import com.tiredcity.app.utils.PriceUtils;

import java.util.ArrayList;

/** Bottom sheet chon 1 trong cac goi SPX Express dang bat (isActive = true). */
public class ShippingMethodBottomSheet extends BottomSheetDialogFragment {

    public interface OnShippingSelectedListener {
        void onShippingSelected(ShippingOption option);
    }

    private static final String ARG_OPTIONS = "arg_options";
    private static final String ARG_SELECTED_ID = "arg_selected_id";

    private BottomSheetShippingMethodBinding binding;
    private OnShippingSelectedListener listener;

    public static ShippingMethodBottomSheet newInstance(ArrayList<ShippingOption> options, String selectedId) {
        ShippingMethodBottomSheet sheet = new ShippingMethodBottomSheet();
        Bundle args = new Bundle();
        args.putSerializable(ARG_OPTIONS, options);
        args.putString(ARG_SELECTED_ID, selectedId);
        sheet.setArguments(args);
        return sheet;
    }

    public void setOnShippingSelectedListener(OnShippingSelectedListener listener) {
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
        binding = BottomSheetShippingMethodBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @SuppressWarnings("unchecked")
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        ArrayList<ShippingOption> options = getArguments() != null
                ? (ArrayList<ShippingOption>) getArguments().getSerializable(ARG_OPTIONS)
                : new ArrayList<>();
        String selectedId = getArguments() != null ? getArguments().getString(ARG_SELECTED_ID) : null;

        binding.rgShippingOptions.removeAllViews();
        for (ShippingOption option : options) {
            RadioButton rb = new RadioButton(requireContext());
            rb.setId(View.generateViewId());
            rb.setPadding(0, dp(12), 0, dp(12));
            rb.setButtonTintList(android.content.res.ColorStateList.valueOf(
                    ContextCompat.getColor(requireContext(), R.color.tc_spx_orange)));
            rb.setText(buildLabel(option));
            rb.setChecked(option.id.equals(selectedId));
            rb.setOnClickListener(v -> {
                if (listener != null) listener.onShippingSelected(option);
                dismiss();
            });
            binding.rgShippingOptions.addView(rb);
        }
    }

    private CharSequence buildLabel(ShippingOption option) {
        String priceText = option.price <= 0
                ? getString(R.string.pay_shipping_free) : PriceUtils.format(option.price);
        String line1 = option.name + "  •  " + priceText;
        String line2 = getString(R.string.pay_shipping_eta, option.estimate);
        SpannableStringBuilder sb = new SpannableStringBuilder(line1 + "\n" + line2);
        int start = line1.length() + 1;
        sb.setSpan(new ForegroundColorSpan(ContextCompat.getColor(requireContext(), R.color.text_secondary)),
                start, sb.length(), 0);
        sb.setSpan(new RelativeSizeSpan(0.85f), start, sb.length(), 0);
        return sb;
    }

    private int dp(int v) {
        return (int) (v * getResources().getDisplayMetrics().density);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
