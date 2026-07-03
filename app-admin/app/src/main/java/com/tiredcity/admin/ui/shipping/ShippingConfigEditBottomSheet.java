package com.tiredcity.admin.ui.shipping;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import com.tiredcity.admin.R;
import com.tiredcity.admin.data.model.ShippingConfig;
import com.tiredcity.admin.databinding.BottomSheetShippingConfigEditBinding;
import com.tiredcity.admin.utils.AuditLogger;

/** Form sua 1 goi cuoc SPX Express: gia, thoi gian du kien, bat/tat. */
public class ShippingConfigEditBottomSheet extends BottomSheetDialogFragment {

    public interface OnSavedListener {
        void onSaved();
    }

    private static final String ARG_ID = "arg_id";
    private static final String ARG_NAME = "arg_name";
    private static final String ARG_PRICE = "arg_price";
    private static final String ARG_ESTIMATE = "arg_estimate";
    private static final String ARG_ACTIVE = "arg_active";

    private BottomSheetShippingConfigEditBinding binding;
    private OnSavedListener listener;

    public static ShippingConfigEditBottomSheet newInstance(ShippingConfig config) {
        ShippingConfigEditBottomSheet sheet = new ShippingConfigEditBottomSheet();
        Bundle args = new Bundle();
        args.putString(ARG_ID, config.id);
        args.putString(ARG_NAME, config.name);
        args.putDouble(ARG_PRICE, config.price);
        args.putString(ARG_ESTIMATE, config.estimate);
        args.putBoolean(ARG_ACTIVE, config.isActive);
        sheet.setArguments(args);
        return sheet;
    }

    public void setOnSavedListener(OnSavedListener listener) {
        this.listener = listener;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                              @Nullable Bundle savedInstanceState) {
        binding = BottomSheetShippingConfigEditBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Bundle args = getArguments();
        if (args == null) return;

        String id = args.getString(ARG_ID);
        binding.tvSheetTitle.setText(args.getString(ARG_NAME));
        binding.etPrice.setText(String.valueOf((long) args.getDouble(ARG_PRICE)));
        binding.etEstimate.setText(args.getString(ARG_ESTIMATE));
        binding.switchActive.setChecked(args.getBoolean(ARG_ACTIVE));

        binding.btnSave.setOnClickListener(v -> save(id, args.getString(ARG_NAME)));
    }

    private void save(String id, String name) {
        String priceStr = binding.etPrice.getText() != null ? binding.etPrice.getText().toString().trim() : "";
        String estimate = binding.etEstimate.getText() != null ? binding.etEstimate.getText().toString().trim() : "";

        double price;
        try {
            price = Double.parseDouble(priceStr);
        } catch (NumberFormatException e) {
            binding.etPrice.setError(getString(R.string.scfg_price_invalid));
            return;
        }

        ShippingConfig updated = new ShippingConfig(id, name, price, estimate, binding.switchActive.isChecked());

        binding.btnSave.setEnabled(false);
        FirebaseFirestore.getInstance()
                .collection("shipping_configs")
                .document(id)
                .set(updated.toMap(), SetOptions.merge())
                .addOnSuccessListener(unused -> {
                    AuditLogger.log("update_shipping_config", id, "");
                    Toast.makeText(requireContext(), R.string.scfg_saved, Toast.LENGTH_SHORT).show();
                    if (listener != null) listener.onSaved();
                    dismiss();
                })
                .addOnFailureListener(e -> {
                    binding.btnSave.setEnabled(true);
                    Toast.makeText(requireContext(),
                            getString(R.string.scfg_save_error, e.getMessage()), Toast.LENGTH_LONG).show();
                });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
