package com.tiredcity.app.data.model;

import com.google.gson.annotations.SerializedName;

public class CartItem {

    @SerializedName("id")
    private String id;

    @SerializedName("product")
    private Product product;

    @SerializedName("quantity")
    private int quantity;

    @SerializedName("selected_size")
    private String selectedSize;

    @SerializedName("selected_color")
    private String selectedColor;

    public CartItem() {}

    public CartItem(Product product, int quantity) {
        this.product = product;
        this.quantity = quantity;
    }

    public CartItem(Product product, int quantity, String selectedSize, String selectedColor) {
        this.product = product;
        this.quantity = quantity;
        this.selectedSize = selectedSize;
        this.selectedColor = selectedColor;
    }

    /** Total price for this line item after any product discount. */
    public double getTotalPrice() {
        if (product == null) return 0;
        return product.getEffectivePrice() * quantity;
    }

    /** Alias for getTotalPrice() — used by adapters that call getSubtotal(). */
    public double getSubtotal() {
        return getTotalPrice();
    }

    // ── Getters & Setters ─────────────────────────────────────────────────────

    public String getId()                           { return id; }
    public void setId(String id)                    { this.id = id; }

    public Product getProduct()                     { return product; }
    public void setProduct(Product product)         { this.product = product; }

    public int getQuantity()                        { return quantity; }
    public void setQuantity(int quantity)           { this.quantity = quantity; }

    public String getSelectedSize()                 { return selectedSize; }
    public void setSelectedSize(String size)        { this.selectedSize = size; }

    public String getSelectedColor()                { return selectedColor; }
    public void setSelectedColor(String color)      { this.selectedColor = color; }
}
