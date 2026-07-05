package com.tiredcity.app.data.model.search;

import com.tiredcity.app.data.model.Product;

/** Loại 3: Sản phẩm gợi ý — bọc một {@link Product} thật lấy từ Firestore. */
public class ProductItem implements SearchItem {

    private final Product product;

    public ProductItem(Product product) {
        this.product = product;
    }

    public Product getProduct() { return product; }

    @Override
    public int getType() { return TYPE_PRODUCT; }
}
