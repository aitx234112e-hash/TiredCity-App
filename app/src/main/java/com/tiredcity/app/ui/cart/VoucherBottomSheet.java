package com.tiredcity.app.ui.cart;

import android.content.res.ColorStateList;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.chip.Chip;
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

        setupTypeChips();
        renderList();

        binding.btnClose.setOnClickListener(v -> dismiss());

        // Cập nhật danh sách ngay khi gõ (để "hiện" mã nếu khớp)
        binding.etCode.addTextChangedListener(new android.text.TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { renderList(); }
            @Override public void afterTextChanged(android.text.Editable s) {}
        });

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

    private void setupTypeChips() {
        String[] labels = getResources().getStringArray(R.array.voucher_type_filters);
        ColorStateList bg = ContextCompat.getColorStateList(requireContext(), R.color.voucher_chip_bg);
        ColorStateList text = ContextCompat.getColorStateList(requireContext(), R.color.voucher_chip_text);
        for (int i = 0; i < labels.length; i++) {
            Chip chip = new Chip(requireContext());
            chip.setText(labels[i]);
            chip.setCheckable(true);
            chip.setCheckedIconVisible(false);
            chip.setChipBackgroundColor(bg);
            chip.setTextColor(text);
            chip.setChipStrokeWidth(0f);
            final int idx = i;
            chip.setOnClickListener(v -> {
                activeFilter = idx < TYPE_FILTERS.length ? TYPE_FILTERS[idx] : null;
                renderList();
            });
            binding.chipGroupType.addView(chip);
            if (i == 0) chip.setChecked(true);
        }
    }

    private void renderList() {
        final String input = binding.etCode.getText() != null
                ? binding.etCode.getText().toString().trim() : "";

        List<Voucher> staticVouchers = CheckoutPriceCalculator.getAvailableVouchers();
        
        com.google.firebase.firestore.FirebaseFirestore.getInstance().collection("vouchers")
                .whereEqualTo("isActive", true)
                .get()
                .addOnSuccessListener(query -> {
                    if (binding == null) return;
                    
                    List<Voucher> all = new ArrayList<>(staticVouchers); // Luôn hiện các mã mặc định
                    
                    for (com.google.firebase.firestore.DocumentSnapshot doc : query.getDocuments()) {
                        String code = doc.getString("code");
                        if (code == null) continue;
                        
                        // 1. Không hiện mã đã hết hạn
                        if (isExpired(doc.getString("expiry"))) continue;

                        // 2. Mã bí mật (Ẩn voucher): Chỉ hiện nếu nhập đúng mã HOẶC đang áp dụng
                        boolean isHidden = Boolean.TRUE.equals(doc.getBoolean("isHidden"));
                        if (isHidden) {
                            if (!code.equalsIgnoreCase(input) && !code.equalsIgnoreCase(currentCode)) {
                                continue; 
                            }
                        }

                        // Tránh trùng mã mặc định
                        boolean exists = false;
                        for (Voucher v : staticVouchers) { if (v.code.equalsIgnoreCase(code)) { exists = true; break; } }
                        if (exists) continue;

                        String title = doc.getString("title");
                        if (title == null) title = code;
                        String desc = doc.getString("description");
                        if (desc == null) desc = "Mã giảm giá từ hệ thống";

                        String typeStr = doc.getString("type");
                        if (typeStr == null) typeStr = doc.contains("discount") ? "PERCENT" : "FLAT";
                        
                        double val = 0;
                        if (doc.contains("value")) {
                            Double dv = doc.getDouble("value");
                            if (dv != null) val = dv;
                        } else if (doc.contains("discount")) {
                            Double dd = doc.getDouble("discount");
                            if (dd != null) val = dd;
                        }
                        
                        double min = 0;
                        if (doc.contains("minOrder")) {
                            Double dm = doc.getDouble("minOrder");
                            if (dm != null) min = dm;
                        } else if (doc.contains("minSpend")) {
                            Double ds = doc.getDouble("minSpend");
                            if (ds != null) min = ds;
                        }

                        try {
                            VoucherType type = VoucherType.valueOf(typeStr.toUpperCase(Locale.ROOT));
                            all.add(new Voucher(code, title, desc, type, val, 0, min));
                        } catch (Exception ignored) {}
                    }
                    
                    displayVouchers(all);
                })
                .addOnFailureListener(e -> {
                    if (binding == null) return;
                    displayVouchers(staticVouchers);
                });
    }

    private boolean isExpired(String dateStr) {
        if (TextUtils.isEmpty(dateStr)) return false;
        try {
            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd", Locale.US);
            java.util.Date date = sdf.parse(dateStr);
            if (date == null) return false;
            
            java.util.Calendar cal = java.util.Calendar.getInstance();
            cal.setTime(date);
            cal.set(java.util.Calendar.HOUR_OF_DAY, 23);
            cal.set(java.util.Calendar.MINUTE, 59);
            cal.set(java.util.Calendar.SECOND, 59);
            
            return new java.util.Date().after(cal.getTime());
        } catch (Exception e) { return false; }
    }

    private void displayVouchers(List<Voucher> all) {
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
