package com.tiredcity.admin.data.model;

import java.util.HashMap;
import java.util.Map;

/** 1 goi cuoc SPX Express. Id co dinh: economy / standard / express. */
public class ShippingConfig {

    public final String id;
    public String name;
    public double price;
    public String estimate;
    public boolean isActive;

    public ShippingConfig(String id, String name, double price, String estimate, boolean isActive) {
        this.id = id;
        this.name = name;
        this.price = price;
        this.estimate = estimate;
        this.isActive = isActive;
    }

    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("name", name);
        map.put("price", price);
        map.put("estimate", estimate);
        map.put("isActive", isActive);
        return map;
    }

    public static ShippingConfig defaultFor(String id) {
        switch (id) {
            case "economy":  return new ShippingConfig(id, "SPX Tiết kiệm", 15_000, "3-5 ngày", true);
            case "express":  return new ShippingConfig(id, "SPX Hỏa tốc",   50_000, "2 giờ",     true);
            case "standard":
            default:         return new ShippingConfig(id, "SPX Cơ bản",    30_000, "1-2 ngày",  true);
        }
    }
}
