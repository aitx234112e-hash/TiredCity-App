package com.tiredcity.app.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatRadioButton;
import androidx.recyclerview.widget.RecyclerView;
import com.tiredcity.app.R;
import com.tiredcity.app.data.model.DeletionReason;
import java.util.List;

/**
 * Danh sách lý do xóa tài khoản — chọn đơn (single-select, kiểu radio).
 * Báo vị trí đã chọn ra ngoài để bật/tắt nút "Gửi yêu cầu".
 */
public class DeletionReasonAdapter
        extends RecyclerView.Adapter<DeletionReasonAdapter.ViewHolder> {

    public interface OnReasonSelectedListener {
        void onReasonSelected(int position);
    }

    private final List<DeletionReason> reasons;
    private final OnReasonSelectedListener listener;
    private int selectedPosition = RecyclerView.NO_POSITION;

    public DeletionReasonAdapter(List<DeletionReason> reasons, OnReasonSelectedListener listener) {
        this.reasons  = reasons;
        this.listener = listener;
    }

    public int getSelectedPosition() {
        return selectedPosition;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_deletion_reason, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        DeletionReason reason = reasons.get(position);
        holder.tvTitle.setText(reason.titleRes);
        holder.tvDesc.setText(reason.descRes);
        holder.rbSelect.setChecked(position == selectedPosition);

        holder.itemView.setOnClickListener(v -> {
            int prev = selectedPosition;
            selectedPosition = holder.getAdapterPosition();
            if (prev != RecyclerView.NO_POSITION) notifyItemChanged(prev);
            notifyItemChanged(selectedPosition);
            if (listener != null) listener.onReasonSelected(selectedPosition);
        });
    }

    @Override
    public int getItemCount() {
        return reasons != null ? reasons.size() : 0;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final TextView tvTitle;
        final TextView tvDesc;
        final AppCompatRadioButton rbSelect;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle  = itemView.findViewById(R.id.tv_reason_title);
            tvDesc   = itemView.findViewById(R.id.tv_reason_desc);
            rbSelect = itemView.findViewById(R.id.rb_select);
        }
    }
}
