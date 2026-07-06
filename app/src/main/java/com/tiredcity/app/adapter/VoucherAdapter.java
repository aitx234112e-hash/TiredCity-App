package com.tiredcity.app.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.tiredcity.app.R;
import com.tiredcity.app.utils.CheckoutPriceCalculator.Voucher;

import java.util.ArrayList;
import java.util.List;

/** Danh sách mã giảm giá trong bottom sheet — chọn 1 (tap để chọn/bỏ chọn). */
public class VoucherAdapter extends RecyclerView.Adapter<VoucherAdapter.ViewHolder> {

    public interface OnSelectionChanged {
        void onSelectionChanged(String selectedCode);
    }

    private final List<Voucher> items = new ArrayList<>();
    private String selectedCode;
    private final OnSelectionChanged listener;

    public VoucherAdapter(OnSelectionChanged listener) {
        this.listener = listener;
    }

    public void submit(List<Voucher> vouchers, String selectedCode) {
        items.clear();
        if (vouchers != null) items.addAll(vouchers);
        this.selectedCode = selectedCode;
        notifyDataSetChanged();
    }

    public String getSelectedCode() {
        return selectedCode;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_voucher, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder h, int position) {
        Voucher v = items.get(position);
        h.title.setText(v.title);
        h.desc.setText(v.description);

        boolean selected = v.code.equals(selectedCode);
        h.card.setSelected(selected);
        h.checkBox.setChecked(selected);

        h.card.setOnClickListener(view -> {
            // Tap lại mã đang chọn → bỏ chọn; ngược lại chọn mã này (bỏ chọn mã cũ).
            selectedCode = v.code.equals(selectedCode) ? null : v.code;
            notifyDataSetChanged();
            if (listener != null) listener.onSelectionChanged(selectedCode);
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final View card;
        final TextView title;
        final TextView desc;
        final CheckBox checkBox;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            card = itemView.findViewById(R.id.voucher_card);
            title = itemView.findViewById(R.id.tv_voucher_title);
            desc = itemView.findViewById(R.id.tv_voucher_desc);
            checkBox = itemView.findViewById(R.id.cb_voucher);
        }
    }
}
