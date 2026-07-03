package com.tiredcity.admin.data.model;

import android.content.Context;

import com.tiredcity.admin.R;

import java.util.Arrays;
import java.util.List;

/**
 * Kho lay hang cua TiredCity. Danh sach co dinh (2 cua hang), admin chi chon kho
 * dang hoat dong — id cua kho dang chon luu o {@link ShippingSettings#activePickupId}.
 * Kho dau tien la kho mac dinh (main warehouse).
 */
public class PickupPoint {

    public final String id;
    public final String name;
    public final String address;

    public PickupPoint(String id, String name, String address) {
        this.id = id;
        this.name = name;
        this.address = address;
    }

    /** 2 kho co dinh, doc ten/dia chi tu strings de dong bo da ngon ngu. */
    public static List<PickupPoint> defaults(Context c) {
        return Arrays.asList(
                new PickupPoint("hanghanh",
                        c.getString(R.string.scfg_pickup_1_name),
                        c.getString(R.string.scfg_pickup_1_addr)),
                new PickupPoint("hanggai",
                        c.getString(R.string.scfg_pickup_2_name),
                        c.getString(R.string.scfg_pickup_2_addr)));
    }

    /** Id kho mac dinh (kho chinh) — luon la kho dau tien trong danh sach. */
    public static String defaultId() {
        return "hanghanh";
    }
}
