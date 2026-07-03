package com.tiredcity.admin.data.model;

/** 1 tin nhan trong khung chat cua Tro ly Admin. */
public class ChatMessage {

    public static final int FROM_BOT = 0;
    public static final int FROM_USER = 1;

    public final int from;
    public final String text;
    public final long time;

    public ChatMessage(int from, String text) {
        this.from = from;
        this.text = text;
        this.time = System.currentTimeMillis();
    }
}
