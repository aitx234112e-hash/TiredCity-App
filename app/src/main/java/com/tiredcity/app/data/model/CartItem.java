package com.tiredcity.app.data.model;

import com.google.gson.annotations.SerializedName;

public class CartItem {

    @SerializedName("id")
    private String id;

    @SerializedName("product")
    private Product product;

    @SerializedName("quantity")
    private int quantity;

    @SerializedName(value = "selected_size", alternate = {"size"})
    private String selectedSize;

    @SerializedName(value = "selected_color", alternate = {"color"})
    private String selectedColor;

    /** Người dùng có chọn sản phẩm này để thanh toán hay không. Mặc định chọn sẵn. */
    @SerializedName("selected")
    private boolean selected = true;

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
        Product p = getProduct();
        if (p == null) return 0;
        return p.getEffectivePrice() * quantity;
    }

    /** Alias for getTotalPrice() — used by adapters that call getSubtotal(). */
    public double getSubtotal() {
        return getTotalPrice();
    }

    // ── Getters & Setters ─────────────────────────────────────────────────────

    public String getId()                           { return id; }
    public void setId(String id)                    { this.id = id; }

    public Product getProduct() {
        if (product == null && productId != null) {
            // Reconstruct from flattened Firestore fields
            Product p = new Product();
            p.setId(productId);
            p.setName(productName);
            p.setPrice(price);
            if (image != null) {
                p.setImage(image);
                java.util.List<String> imgs = new java.util.ArrayList<>();
                imgs.add(image);
                p.setImages(imgs);
            }
            return p;
        }
        return product;
    }

    public void setProduct(Product product)         { this.product = product; }

    // ── Flattened Fields for Firestore compatibility ──────────────────────────

    @com.google.firebase.firestore.PropertyName("productId")
    public String getProductId() { return productId != null ? productId : (product != null ? product.getId() : null); }
    @com.google.firebase.firestore.PropertyName("productId")
    public void setProductId(String id) { this.productId = id; }

    @com.google.firebase.firestore.PropertyName("product_name")
    public String getProductName() { return productName != null ? productName : (product != null ? product.getName() : null); }
    @com.google.firebase.firestore.PropertyName("product_name")
    public void setProductName(String name) { this.productName = name; }

    @com.google.firebase.firestore.PropertyName("image")
    public String getImage() { return image != null ? image : (product != null ? product.getFirstImage() : null); }
    @com.google.firebase.firestore.PropertyName("image")
    public void setImage(String image) { this.image = image; }

    @com.google.firebase.firestore.PropertyName("price")
    public double getPrice() { return price > 0 ? price : (product != null ? product.getEffectivePrice() : 0); }
    @com.google.firebase.firestore.PropertyName("price")
    public void setPrice(double price) { this.price = price; }

    private String productId;
    private String productName;
    private String image;
    private double price;

    // ── End of Flattened Fields ───────────────────────────────────────────────

    public int getQuantity()                        { return quantity; }
    @com.google.firebase.firestore.PropertyName("qty")
    public void setQty(int qty)                     { this.quantity = qty; }
    public void setQuantity(int quantity)           { this.quantity = quantity; }

    public String getSelectedSize()                 { return selectedSize; }
    @com.google.firebase.firestore.PropertyName("size")
    public void setSelectedSize(String size)        { this.selectedSize = size; }

    public String getSelectedColor()                { return selectedColor; }
    @com.google.firebase.firestore.PropertyName("color")
    public void setSelectedColor(String color)      { this.selectedColor = color; }

    public boolean isSelected()                     { return selected; }
    public void setSelected(boolean selected)       { this.selected = selected; }
}
