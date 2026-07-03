package com.tiredcity.admin.data.model;

import com.google.firebase.firestore.DocumentSnapshot;

import java.util.HashMap;
import java.util.Map;

/**
 * Thiet lap chung cho van chuyen — luu o doc co dinh 'shipping_settings/general',
 * dong bo giua app-admin va web-admin.
 */
public class ShippingSettings {

    public static final String COLLECTION = "shipping_settings";
    public static final String DOC_ID = "general";

    /** Id kho dang lay hang. */
    public String activePickupId;
    /** Mua tu muc nay tro len se duoc mien phi ship. 0 = khong ap dung. */
    public double freeshipThreshold;
    /** Ghi chu mac dinh gui shipper. */
    public String shipperNote;

    public ShippingSettings(String activePickupId, double freeshipThreshold, String shipperNote) {
        this.activePickupId = activePickupId;
        this.freeshipThreshold = freeshipThreshold;
        this.shipperNote = shipperNote;
    }

    public static ShippingSettings defaults() {
        return new ShippingSettings(PickupPoint.defaultId(), 0, "");
    }

    public static ShippingSettings from(DocumentSnapshot d) {
        if (d == null || !d.exists()) return defaults();
        String pickup = d.getString("activePickupId");
        Double threshold = d.getDouble("freeshipThreshold");
        String note = d.getString("shipperNote");
        return new ShippingSettings(
                pickup != null ? pickup : PickupPoint.defaultId(),
                threshold != null ? threshold : 0,
                note != null ? note : "");
    }

    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("activePickupId", activePickupId);
        map.put("freeshipThreshold", freeshipThreshold);
        map.put("shipperNote", shipperNote);
        return map;
    }
}
