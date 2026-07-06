package com.tiredcity.app.adapter;

import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.tiredcity.app.R;
import com.tiredcity.app.utils.CheckoutPriceCalculator.Voucher;
import com.tiredcity.app.utils.CheckoutPriceCalculator.VoucherType;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Danh sách mã giảm giá trong bottom sheet — thẻ kiểu vé, chọn 1 (tap để chọn/bỏ chọn). */
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
        h.code.setText("MÃ " + v.code);

        // Cuống vé: giá trị lớn + nhãn + màu theo loại
        int stubColor;
        switch (v.type) {
            case PERCENT:
                h.value.setText(fmtInt(v.value) + "%");
                h.valueLabel.setText("GIẢM");
                stubColor = R.color.tc_red;
                break;
            case FLAT:
                h.value.setText(fmtShortMoney(v.value));
                h.valueLabel.setText("GIẢM");
                stubColor = R.color.tc_gold;
                break;
            case FREE_SHIP:
            default:
                h.value.setText("FREE");
                h.valueLabel.setText("SHIP");
                stubColor = R.color.tc_ship_green;
                break;
        }
        h.stub.setBackgroundTintList(ColorStateList.valueOf(
                ContextCompat.getColor(h.stub.getContext(), stubColor)));

        boolean selected = v.code.equals(selectedCode);
        h.card.setSelected(selected);   // duplicateParentState → iv_check tự đổi trạng thái

        h.card.setOnClickListener(view -> {
            selectedCode = v.code.equals(selectedCode) ? null : v.code;
            notifyDataSetChanged();
            if (listener != null) listener.onSelectionChanged(selectedCode);
        });
    }

    private static String fmtInt(double d) {
        return String.valueOf((long) d);
    }

    /** 50000 → "50K", 1000000 → "1TR". */
    private static String fmtShortMoney(double d) {
        long n = (long) d;
        if (n >= 1_000_000) {
            double tr = n / 1_000_000.0;
            return (tr == Math.floor(tr) ? String.valueOf((long) tr)
                    : String.format(Locale.US, "%.1f", tr)) + "TR";
        }
        if (n >= 1_000) return (n / 1_000) + "K";
        return String.valueOf(n);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final View card;
        final View stub;
        final TextView value;
        final TextView valueLabel;
        final TextView title;
        final TextView desc;
        final TextView code;
        final ImageView check;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            card = itemView.findViewById(R.id.voucher_card);
            stub = itemView.findViewById(R.id.voucher_stub);
            value = itemView.findViewById(R.id.tv_voucher_value);
            valueLabel = itemView.findViewById(R.id.tv_voucher_value_label);
            title = itemView.findViewById(R.id.tv_voucher_title);
            desc = itemView.findViewById(R.id.tv_voucher_desc);
            code = itemView.findViewById(R.id.tv_voucher_code);
            check = itemView.findViewById(R.id.iv_check);
        }
    }
}
