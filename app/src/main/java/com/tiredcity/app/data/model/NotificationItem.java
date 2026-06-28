package com.tiredcity.app.data.model;

/** Mục thông báo hiển thị trong trang Thông báo. */
public class NotificationItem {

    /** Loại thông báo – quyết định biểu tượng hiển thị. */
    public enum Type { ORDER, PROMO, SYSTEM }

    private final String title;
    private final String message;
    private final String time;
    private final Type   type;
    private boolean      unread;

    public NotificationItem(String title, String message, String time, Type type, boolean unread) {
        this.title   = title;
        this.message = message;
        this.time    = time;
        this.type    = type;
        this.unread  = unread;
    }

    public String getTitle()   { return title; }
    public String getMessage() { return message; }
    public String getTime()    { return time; }
    public Type   getType()    { return type; }
    public boolean isUnread()  { return unread; }
    public void setUnread(boolean unread) { this.unread = unread; }
}
