package com.tiredcity.app.data.model;

import com.google.firebase.firestore.DocumentId;
import com.google.firebase.firestore.IgnoreExtraProperties;
import com.google.firebase.firestore.PropertyName;

import java.util.Date;

@IgnoreExtraProperties
public class Event {
    @DocumentId
    private String id;
    
    private String title;
    private String description;
    
    @PropertyName("image")
    private String imageUrl;
    
    private String location;
    
    @PropertyName("date")
    private Object startDate; // String or Timestamp
    
    private boolean isOnline;
    private Object createdAt;
    private Object updatedAt;

    /** Ảnh drawable nội bộ (ưu tiên hơn imageUrl khi > 0). Không lưu Firestore. */
    private transient int localImageRes;

    public Event() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    @PropertyName("image")
    public String getImageUrl() { return imageUrl; }
    
    @PropertyName("image")
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
    
    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    @PropertyName("date")
    public Object getStartDate() { return startDate; }
    
    @PropertyName("date")
    public void setStartDate(Object startDate) { this.startDate = startDate; }

    /** Helper to get start date as Java Date object */
    public Date getEventDate() {
        if (startDate instanceof com.google.firebase.Timestamp) {
            return ((com.google.firebase.Timestamp) startDate).toDate();
        }
        if (startDate instanceof Date) {
            return (Date) startDate;
        }
        if (startDate instanceof String) {
            try {
                return new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).parse((String) startDate);
            } catch (Exception e) {
                return null;
            }
        }
        return null;
    }

    public boolean isOnline() { return isOnline; }
    public void setOnline(boolean online) { isOnline = online; }
    
    public Object getCreatedAt() { return createdAt; }
    public void setCreatedAt(Object createdAt) { this.createdAt = createdAt; }

    public Object getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Object updatedAt) { this.updatedAt = updatedAt; }

    public int getLocalImageRes() { return localImageRes; }
    public void setLocalImageRes(int localImageRes) { this.localImageRes = localImageRes; }
}
