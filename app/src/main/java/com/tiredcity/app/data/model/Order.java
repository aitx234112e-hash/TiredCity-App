package com.tiredcity.app.data.model;

import com.google.firebase.firestore.DocumentId;
import java.util.Date;
import java.util.List;
import java.util.Map;

public class Order {
    @DocumentId
    private String id;
    private String orderCode;
    private String userId;
    private List<CartItem> items;
    private double totalPrice;
    private double shippingFee;
    private String status;
    private String shippingAddress;
    private String paymentMethod;
    private String trackingNumber;
    private List<Map<String, Object>> history;
    private Date createdAt;
    private Date updatedAt;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getOrderCode() { return orderCode; }
    public void setOrderCode(String orderCode) { this.orderCode = orderCode; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public List<CartItem> getItems() { return items; }
    public void setItems(List<CartItem> items) { this.items = items; }

    @com.google.firebase.firestore.PropertyName("orderItems")
    public void setOrderItems(List<CartItem> items) { this.items = items; }
    public double getTotalPrice() { return totalPrice; }
    public void setTotalPrice(double totalPrice) { this.totalPrice = totalPrice; }
    public double getShippingFee() { return shippingFee; }
    public void setShippingFee(double shippingFee) { this.shippingFee = shippingFee; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getShippingAddress() { return shippingAddress; }
    public void setShippingAddress(String shippingAddress) { this.shippingAddress = shippingAddress; }
    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }
    public String getTrackingNumber() { return trackingNumber; }
    public void setTrackingNumber(String trackingNumber) { this.trackingNumber = trackingNumber; }
    public List<Map<String, Object>> getHistory() { return history; }
    public void setHistory(List<Map<String, Object>> history) { this.history = history; }

    @com.google.firebase.firestore.PropertyName("createdAt")
    public void setCreatedAt(Object value) {
        if (value instanceof com.google.firebase.Timestamp) {
            this.createdAt = ((com.google.firebase.Timestamp) value).toDate();
        } else if (value instanceof String) {
            this.createdAt = com.tiredcity.app.utils.DateUtils.parseApiDate((String) value);
        } else if (value instanceof Date) {
            this.createdAt = (Date) value;
        } else if (value instanceof Long) {
            this.createdAt = new Date((Long) value);
        }
    }

    @com.google.firebase.firestore.PropertyName("createdAt")
    public Date getCreatedAt() { return createdAt; }

    @com.google.firebase.firestore.PropertyName("updatedAt")
    public void setUpdatedAt(Object value) {
        if (value instanceof com.google.firebase.Timestamp) {
            this.updatedAt = ((com.google.firebase.Timestamp) value).toDate();
        } else if (value instanceof String) {
            this.updatedAt = com.tiredcity.app.utils.DateUtils.parseApiDate((String) value);
        } else if (value instanceof Date) {
            this.updatedAt = (Date) value;
        }
    }

    @com.google.firebase.firestore.PropertyName("updatedAt")
    public Date getUpdatedAt() { return updatedAt; }
}
