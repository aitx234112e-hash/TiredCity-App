package com.tiredcity.admin.data.model;

import androidx.annotation.ColorRes;

/**
 * View-model chung cho 1 dong trong danh sach admin (san pham, don hang, user...).
 * Moi man dung chung {@code RowAdapter} nen chi can map document Firestore -> Row.
 */
public class Row {
    public final String title;     // dong dam ben trai
    public final String subtitle;  // dong phu mo ta
    public final String meta;      // gia tri ben phai (gia tien, ngay...)
    public final String badge;     // nhan trang thai (co the null)
    @ColorRes public final int badgeColor;
    public String actionLabel;     // nhan nut bam (vd: "Dừng")
    public Runnable onActionClick; // callback khi bam nut
    public String searchData;      // Du lieu an de tim kiem (vd: ID day du, SĐT, Email...)

    public Row(String title, String subtitle, String meta, String badge, @ColorRes int badgeColor) {
        this.title = title;
        this.subtitle = subtitle;
        this.meta = meta;
        this.badge = badge;
        this.badgeColor = badgeColor;
    }

    public Row(String title, String subtitle, String meta) {
        this(title, subtitle, meta, null, 0);
    }

    public Row withAction(String label, Runnable callback) {
        this.actionLabel = label;
        this.onActionClick = callback;
        return this;
    }

    public Row withSearchData(String data) {
        this.searchData = data;
        return this;
    }
}
