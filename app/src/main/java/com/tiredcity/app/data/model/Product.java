package com.tiredcity.app.data.model;

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

    public String getStory()                { return story; }
    public void setStory(String story)      { this.story = story; }

    public List<String> getCareInstructions() { return careInstructions; }
    public void setCareInstructions(List<String> care) { this.careInstructions = care; }

    public Map<String, String> getSpecifications() { return specifications; }
    public void setSpecifications(Map<String, String> spec) { this.specifications = spec; }

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

    public String getImage()                { return image; }
    public void setImage(String image)      { this.image = image; }

    public List<String> getColors()         { return colors; }
    public void setColors(List<String> c)   { this.colors = c; }

    public List<String> getMenh()           { return menh; }
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
