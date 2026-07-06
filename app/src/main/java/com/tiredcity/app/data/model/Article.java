package com.tiredcity.app.data.model;

import com.google.firebase.firestore.DocumentId;
import com.google.firebase.firestore.IgnoreExtraProperties;
import com.google.firebase.firestore.PropertyName;

import java.io.Serializable;
import java.util.Date;

@IgnoreExtraProperties
public class Article implements Serializable {
    @DocumentId
    private String id;
    
    private String title;
    
    @PropertyName("titleVi")
    private String titleVi;
    
    private String excerpt;
    private String content;
    
    @PropertyName("thumbnail")
    private String imageUrl;
    
    @PropertyName("authorName")
    private String author;
    
    private String status; // draft, published
    private String slug;
    private long views;
    private String category;
    private Object publishedAt; // Can be String (ISO) or Timestamp
    private Object createdAt;

    public Article() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    @PropertyName("titleVi")
    public String getTitleVi() { 
        if (titleVi == null || titleVi.isEmpty()) return title;
        return titleVi; 
    }
    
    @PropertyName("titleVi")
    public void setTitleVi(String titleVi) { this.titleVi = titleVi; }

    public String getExcerpt() { return excerpt; }
    public void setExcerpt(String excerpt) { this.excerpt = excerpt; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    @PropertyName("thumbnail")
    public String getImageUrl() { return imageUrl; }
    
    @PropertyName("thumbnail")
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    @PropertyName("authorName")
    public String getAuthor() { return author; }
    
    @PropertyName("authorName")
    public void setAuthor(String author) { this.author = author; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getSlug() { return slug; }
    public void setSlug(String slug) { this.slug = slug; }

    public long getViews() { return views; }
    public void setViews(long views) { this.views = views; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public Object getPublishedAt() { return publishedAt; }
    public void setPublishedAt(Object publishedAt) { this.publishedAt = publishedAt; }

    /** Helper to get published date as Java Date object */
    public Date getPublishedDate() {
        if (publishedAt instanceof com.google.firebase.Timestamp) {
            return ((com.google.firebase.Timestamp) publishedAt).toDate();
        }
        if (publishedAt instanceof Date) {
            return (Date) publishedAt;
        }
        if (publishedAt instanceof String) {
            // ISO format "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"
            try {
                java.text.SimpleDateFormat iso = new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", java.util.Locale.US);
                iso.setTimeZone(java.util.TimeZone.getTimeZone("UTC"));
                return iso.parse((String) publishedAt);
            } catch (Exception e) {
                try {
                    // Try simpler format "yyyy-MM-dd"
                    return new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).parse((String) publishedAt);
                } catch (Exception e2) {
                    return null;
                }
            }
        }
        return null;
    }

    public Object getCreatedAt() { return createdAt; }
    public void setCreatedAt(Object createdAt) { this.createdAt = createdAt; }
}
