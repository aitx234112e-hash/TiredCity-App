package com.tiredcity.app.data.model;

import com.google.firebase.firestore.PropertyName;
import com.google.gson.annotations.SerializedName;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class Product {

    @SerializedName("_id")
    private String id;

    @SerializedName("product_name")
    private String name;

    @SerializedName("product_dept")
    private String category;

    @SerializedName("material")
    private String material;

    @SerializedName("origin")
    private String origin;

    @SerializedName("description")
    private String description;

    @SerializedName("story")
    private String story;

    @SerializedName("care_instructions")
    private List<String> careInstructions;

    @SerializedName("specifications")
    private Map<String, String> specifications = new LinkedHashMap<>();

    @SerializedName("unit_price")
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

    // Field for single image URL if needed by some fragments
    private String image;

    @SerializedName("colors")
    private List<String> colors;

    @SerializedName("menh")
    private List<String> menh;

    @SerializedName("sizes")
    private List<SizeVariant> sizes;

    public Product() {}

    public static class SizeVariant {
        @PropertyName("size")
        public String size;
        @PropertyName("quantity")
        public int quantity;

        public SizeVariant() {} // Required for Firebase
        public SizeVariant(String size, int quantity) {
            this.size = size;
            this.quantity = quantity;
        }
    }

    // ── Getters & Setters ─────────────────────────────────────────────────────

    @PropertyName("sizes")
    public List<SizeVariant> getSizes() { return sizes; }
    @PropertyName("sizes")
    public void setSizes(List<SizeVariant> sizes) { this.sizes = sizes; }

    @PropertyName("_id")
    public String getId()                   { return id; }
    @PropertyName("_id")
    public void setId(String id)            { this.id = id; }

    @PropertyName("product_name")
    public String getName()                 { return name; }
    @PropertyName("product_name")
    public void setName(String name)        { this.name = name; }

    @PropertyName("product_dept")
    public String getCategory()             { return category; }
    @PropertyName("product_dept")
    public void setCategory(String cat)     { this.category = cat; }

    @PropertyName("material")
    public String getMaterial()             { return material; }
    @PropertyName("material")
    public void setMaterial(String mat)     { this.material = mat; }

    @PropertyName("origin")
    public String getOrigin()               { return origin; }
    @PropertyName("origin")
    public void setOrigin(String origin)    { this.origin = origin; }

    @PropertyName("description")
    public String getDescription()          { return description; }
    @PropertyName("description")
    public void setDescription(String d)    { this.description = d; }

    @PropertyName("story")
    public String getStory()                { return story; }
    @PropertyName("story")
    public void setStory(String story)      { this.story = story; }

    @PropertyName("care_instructions")
    public List<String> getCareInstructions() { return careInstructions; }
    @PropertyName("care_instructions")
    public void setCareInstructions(List<String> care) { this.careInstructions = care; }

    @PropertyName("specifications")
    public Map<String, String> getSpecifications() { return specifications; }
    @PropertyName("specifications")
    public void setSpecifications(Map<String, String> spec) { this.specifications = spec; }

    @PropertyName("unit_price")
    public double getPrice()                { return price; }
    @PropertyName("unit_price")
    public void setPrice(double price)      { this.price = price; }

    @PropertyName("rating")
    public double getRating()               { return rating; }
    @PropertyName("rating")
    public void setRating(double rating)    { this.rating = rating; }

    @PropertyName("stock")
    public int getStock()                   { return stock; }
    @PropertyName("stock")
    public void setStock(int stock)         { this.stock = stock; }

    @PropertyName("discount")
    public int getDiscount()                { return discount; }
    @PropertyName("discount")
    public void setDiscount(int discount)   { this.discount = discount; }

    @PropertyName("is_new")
    public boolean isNew()                  { return isNew; }
    @PropertyName("is_new")
    public void setNew(boolean isNew)       { this.isNew = isNew; }

    @PropertyName("images")
    public List<String> getImages()         { return images; }
    @PropertyName("images")
    public void setImages(List<String> imgs){ this.images = imgs; }

    @PropertyName("image")
    public String getImage()                { return image; }
    @PropertyName("image")
    public void setImage(String image)      { this.image = image; }

    @PropertyName("colors")
    public List<String> getColors()         { return colors; }
    @PropertyName("colors")
    public void setColors(List<String> c)   { this.colors = c; }

    @PropertyName("menh")
    public List<String> getMenh()           { return menh; }
    @PropertyName("menh")
    public void setMenh(List<String> menh)  { this.menh = menh; }

    /** Convenience: first image URL or empty string. */
    public String getFirstImage() {
        if (images != null && !images.isEmpty()) return images.get(0);
        return image != null ? image : "";
    }

    /** Effective price after discount. */
    public double getEffectivePrice() {
        if (discount <= 0) return price;
        return price * (1 - discount / 100.0);
    }
}
