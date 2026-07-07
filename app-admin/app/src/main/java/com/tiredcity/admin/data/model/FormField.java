package com.tiredcity.admin.data.model;

/**
 * Mo ta 1 truong nhap lieu tren form them/sua ban ghi (mirror cac form ben web-admin).
 * Loai {@link Type} quyet dinh widget hien thi trong {@code EntityFormActivity}.
 */
public class FormField {

    public enum Type {
        TEXT,      // 1 dong
        TEXTAREA,  // nhieu dong
        NUMBER,    // so nguyen
        DECIMAL,   // so thap phan
        MONEY,     // so co dinh dang tien VND
        DATE,      // yyyy-MM-dd (mo DatePicker)
        TIME,      // HH:mm (mo TimePicker)
        SELECT,    // chon 1 trong danh sach (options / optionValues song song)
        SWITCH     // boolean
    }

    public final String key;
    public final String label;
    public final Type type;
    public final boolean required;
    /** Nhan hien thi cho SELECT. */
    public final String[] options;
    /** Gia tri luu tuong ung voi {@link #options} (song song 1-1). */
    public final String[] optionValues;
    /** Gia tri hien tai / gia tri mac dinh (prefill khi sua). */
    public String value;
    /** Goi y nhap (hint), co the null. */
    public String hint;
    /** Cho phep nhap lieu hay chi xem (dung cho logic han che sua). */
    public boolean enabled = true;

    public FormField(String key, String label, Type type, boolean required) {
        this(key, label, type, required, null, null);
    }

    public FormField(String key, String label, Type type, boolean required,
                     String[] options, String[] optionValues) {
        this.key = key;
        this.label = label;
        this.type = type;
        this.required = required;
        this.options = options;
        this.optionValues = optionValues;
        this.value = "";
    }

    public FormField value(String v) {
        this.value = v == null ? "" : v;
        return this;
    }

    public FormField hint(String h) {
        this.hint = h;
        return this;
    }
}
