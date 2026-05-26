package com.tiredcity.app.data.model;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class Product {

    @SerializedName("id")
    private String id;

    @SerializedName("name")
    private String name;

    @SerializedName("category")
    private String category;

    @SerializedName("material")
    private String material;

    @SerializedName("origin")
    private String origin;

    @SerializedName("description")
    private String description;

    @SerializedName("price")
    private double price;

    @SerializedName("rating")
    private double rating;

    @SerializedName("stock")
    private int stock;

    @SerializedName("discount")
    private int discount;

    @SerializedName("is_new")
    private boolean isNew;

    @SerializedName("images")
    private List<String> images;

    @SerializedName("colors")
    private List<String> colors;

    @SerializedName("menh")
    private List<String> menh;

    public Product() {}

    // ── Getters & Setters ─────────────────────────────────────────────────────

    public String getId()                   { return id; }
    public void setId(String id)            { this.id = id; }

    public String getName()                 { return name; }
    public void setName(String name)        { this.name = name; }

    public String getCategory()             { return category; }
    public void setCategory(String cat)     { this.category = cat; }

    public String getMaterial()             { return material; }
    public void setMaterial(String mat)     { this.material = mat; }

    public String getOrigin()               { return origin; }
    public void setOrigin(String origin)    { this.origin = origin; }

    public String getDescription()          { return description; }
    public void setDescription(String d)    { this.description = d; }

    public double getPrice()                { return price; }
    public void setPrice(double price)      { this.price = price; }

    public double getRating()               { return rating; }
    public void setRating(double rating)    { this.rating = rating; }

    public int getStock()                   { return stock; }
    public void setStock(int stock)         { this.stock = stock; }

    public int getDiscount()                { return discount; }
    public void setDiscount(int discount)   { this.discount = discount; }

    public boolean isNew()                  { return isNew; }
    public void setNew(boolean isNew)       { this.isNew = isNew; }

    public List<String> getImages()         { return images; }
    public void setImages(List<String> imgs){ this.images = imgs; }

    public List<String> getColors()         { return colors; }
    public void setColors(List<String> c)   { this.colors = c; }

    public List<String> getMenh()           { return menh; }
    public void setMenh(List<String> menh)  { this.menh = menh; }

    /** Convenience: first image URL or empty string. */
    public String getFirstImage() {
        return (images != null && !images.isEmpty()) ? images.get(0) : "";
    }

    /** Effective price after discount. */
    public double getEffectivePrice() {
        if (discount <= 0) return price;
        return price * (1 - discount / 100.0);
    }
}
