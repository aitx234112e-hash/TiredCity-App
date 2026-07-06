package com.tiredcity.admin.ui.list;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;

import com.tiredcity.admin.R;
import com.tiredcity.admin.data.model.DetailField;

import java.util.List;

/** AlertDialog chi tiet 1 ban ghi — hien danh sach nhan/gia tri, co the them 1 nut hanh dong. */
public final class DetailDialog {
    private DetailDialog() {}

    public interface OnAction {
        void run();
    }

    public static void show(Context ctx, String title, List<DetailField> fields,
                             String actionLabel, OnAction action) {
        show(ctx, title, fields, actionLabel, action, null, null);
    }

    /** Ban day du: them nut trung lap (neutral) — dung cho hanh dong phu nhu "Huy don". */
    public static void show(Context ctx, String title, List<DetailField> fields,
                             String actionLabel, OnAction action,
                             String neutralLabel, OnAction neutralAction) {
        View content = LayoutInflater.from(ctx).inflate(R.layout.dialog_detail, null, false);
        LinearLayout container = content.findViewById(R.id.containerFields);

        for (DetailField f : fields) {
            if (f.value == null || f.value.trim().isEmpty()) continue;
            View row = LayoutInflater.from(ctx).inflate(R.layout.item_detail_field, container, false);
            ((TextView) row.findViewById(R.id.tvFieldLabel)).setText(f.label);
            ((TextView) row.findViewById(R.id.tvFieldValue)).setText(f.value);
            container.addView(row);
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(ctx)
                .setTitle(title)
                .setView(content)
                .setNegativeButton(R.string.btn_close, null);

        if (actionLabel != null && action != null) {
            builder.setPositiveButton(actionLabel, (dialog, which) -> action.run());
        }
        if (neutralLabel != null && neutralAction != null) {
            builder.setNeutralButton(neutralLabel, (dialog, which) -> neutralAction.run());
        }

        builder.show();
    }

    /** Bản mở rộng: hỗ trợ 3 nút hành động (ví dụ: Duyệt, Ẩn, Xoá). */
    public static void show(Context ctx, String title, List<DetailField> fields,
                             String posLabel, OnAction posAction,
                             String neuLabel, OnAction neuAction,
                             String negLabel, OnAction negAction) {
        View content = LayoutInflater.from(ctx).inflate(R.layout.dialog_detail, null, false);
        LinearLayout container = content.findViewById(R.id.containerFields);

        for (DetailField f : fields) {
            if (f.value == null || f.value.trim().isEmpty()) continue;
            View row = LayoutInflater.from(ctx).inflate(R.layout.item_detail_field, container, false);
            ((TextView) row.findViewById(R.id.tvFieldLabel)).setText(f.label);
            ((TextView) row.findViewById(R.id.tvFieldValue)).setText(f.value);
            container.addView(row);
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(ctx)
                .setTitle(title)
                .setView(content);

        if (posLabel != null && posAction != null) {
            builder.setPositiveButton(posLabel, (dialog, which) -> posAction.run());
        }
        if (neuLabel != null && neuAction != null) {
            builder.setNeutralButton(neuLabel, (dialog, which) -> neuAction.run());
        }
        if (negLabel != null && negAction != null) {
            builder.setNegativeButton(negLabel, (dialog, which) -> negAction.run());
        } else {
            builder.setNegativeButton(R.string.btn_close, null);
        }

        builder.show();
    }
}
