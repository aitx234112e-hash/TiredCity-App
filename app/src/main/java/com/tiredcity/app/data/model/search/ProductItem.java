package com.tiredcity.app.data.model.search;

import androidx.annotation.DrawableRes;
import androidx.annotation.StringRes;

/** Loại 3: Sản phẩm — ảnh, thương hiệu (đậm), tên, giá (VNĐ). */
public class ProductItem implements SearchItem {

    @StringRes private final int brandResId;
    @StringRes private final int nameResId;
    private final double price;
    @DrawableRes private final int imageRes;

    public ProductItem(@StringRes int brandResId, @StringRes int nameResId,
                       double price, @DrawableRes int imageRes) {
        this.brandResId = brandResId;
        this.nameResId  = nameResId;
        this.price      = price;
        this.imageRes   = imageRes;
    }

    @StringRes public int getBrandResId() { return brandResId; }
    @StringRes public int getNameResId() { return nameResId; }
    public double getPrice() { return price; }
    @DrawableRes public int getImageRes() { return imageRes; }

    @Override
    public int getType() { return TYPE_PRODUCT; }
}
