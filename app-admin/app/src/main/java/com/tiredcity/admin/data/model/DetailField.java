package com.tiredcity.admin.data.model;

/** Cap nhan/gia tri hien thi trong dialog chi tiet (mirror cac dong trong modal web-admin). */
public class DetailField {
    public final String label;
    public final String value;

    public DetailField(String label, String value) {
        this.label = label;
        this.value = value;
    }
}
