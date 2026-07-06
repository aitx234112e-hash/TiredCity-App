package com.tiredcity.app.ui.cart;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.tiredcity.app.R;
import com.tiredcity.app.adapter.VoucherAdapter;
import com.tiredcity.app.databinding.BottomSheetVoucherBinding;
import com.tiredcity.app.utils.CheckoutPriceCalculator;
import com.tiredcity.app.utils.CheckoutPriceCalculator.Voucher;
import com.tiredcity.app.utils.CheckoutPriceCalculator.VoucherType;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Bottom sheet chọn mã giảm giá (lọc loại + nhập tay + danh sách chọn 1) kiểu Shopee. */
public class VoucherBottomSheet extends BottomSheetDialogFragment {

    public interface OnVoucherAppliedListener {
        /** code = null/rỗng → bỏ mã; ngược lại áp mã (activity tự validate + báo lỗi nếu sai). */
        void onVoucherApplied(String code);
    }

    private BottomSheetVoucherBinding binding;
    private VoucherAdapter adapter;
    private OnVoucherAppliedListener listener;
    private String currentCode;

    /** Loại lọc theo thứ tự dropdown: null = Tất cả. */
    private static final VoucherType[] TYPE_FILTERS =
            { null, VoucherType.PERCENT, VoucherType.FLAT, VoucherType.FREE_SHIP };
    private VoucherType activeFilter = null;

    public void setOnVoucherAppliedListener(OnVoucherAppliedListener l) { this.listener = l; }
    public void setCurrentCode(String code) { this.currentCode = code; }

    @Override
    public int getTheme() {
        return R.style.Theme_TC_BottomSheetDialog;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                              @Nullable Bundle savedInstanceState) {
        binding = BottomSheetVoucherBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        adapter = new VoucherAdapter(code -> updateApplyLabel());
        binding.rvVouchers.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvVouchers.setAdapter(adapter);

        setupTypeDropdown();
        renderList();

        binding.btnClose.setOnClickListener(v -> dismiss());

        // Nhập tay + Áp dụng: áp ngay mã đã gõ.
        binding.btnApplyCode.setOnClickListener(v -> {
            String code = binding.etCode.getText() != null
                    ? binding.etCode.getText().toString().trim().toUpperCase(Locale.ROOT) : "";
            if (TextUtils.isEmpty(code)) return;
            apply(code);
        });

        // Áp dụng lựa chọn trong danh sách (null = bỏ mã).
        binding.btnApply.setOnClickListener(v -> apply(adapter.getSelectedCode()));

        updateApplyLabel();
    }

    private void setupTypeDropdown() {
        String[] labels = getResources().getStringArray(R.array.voucher_type_filters);
        binding.actVoucherType.setAdapter(new ArrayAdapter<>(
                requireContext(), android.R.layout.simple_list_item_1, labels));
        binding.actVoucherType.setText(labels[0], false);
        binding.actVoucherType.setOnItemClickListener((parent, v, pos, id) -> {
            activeFilter = pos >= 0 && pos < TYPE_FILTERS.length ? TYPE_FILTERS[pos] : null;
            renderList();
        });
    }

    private void renderList() {
        List<Voucher> all = CheckoutPriceCalculator.getAvailableVouchers();
        List<Voucher> filtered = new ArrayList<>();
        for (Voucher v : all) {
            if (activeFilter == null || v.type == activeFilter) filtered.add(v);
        }
        adapter.submit(filtered, currentCode);

        boolean empty = filtered.isEmpty();
        binding.tvEmpty.setVisibility(empty ? View.VISIBLE : View.GONE);
        binding.rvVouchers.setVisibility(empty ? View.GONE : View.VISIBLE);
        binding.tvAvailableCount.setText(getString(R.string.voucher_sheet_available, filtered.size()));
        updateApplyLabel();
    }

    private void updateApplyLabel() {
        if (binding == null) return;
        int n = adapter.getSelectedCode() != null ? 1 : 0;
        binding.btnApply.setText(getString(R.string.voucher_sheet_apply_n, n));
    }

    private void apply(String code) {
        if (listener != null) listener.onVoucherApplied(code);
        dismiss();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
