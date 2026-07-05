package com.tiredcity.app.data.model;

import java.io.Serializable;

/** 1 goi cuoc SPX Express dang bat (isActive = true) doc tu Firestore. */
public class ShippingOption implements Serializable {

    public final String id;
    public final String name;
    public final double price;
    /** Giá gốc trước khi miễn phí (để gạch ngang cạnh "Miễn Phí"); 0 = không hiện. */
    public final double originalPrice;
    public final String estimate;

    public ShippingOption(String id, String name, double price, String estimate) {
        this(id, name, price, 0, estimate);
    }

    public ShippingOption(String id, String name, double price, double originalPrice, String estimate) {
        this.id = id;
        this.name = name;
        this.price = price;
        this.originalPrice = originalPrice;
        this.estimate = estimate;
    }
}
