package com.tiredcity.app.data.model;

/**
 * Represents a single chat message in the ChatBot conversation.
 */
public class ChatMessage {
    private final String text;
    private final boolean isFromUser;
    private final long timestamp;

    public ChatMessage(String text, boolean isFromUser) {
        this.text       = text;
        this.isFromUser = isFromUser;
        this.timestamp  = System.currentTimeMillis();
    }

    public String getText()       { return text; }
    public boolean isFromUser()   { return isFromUser; }
    public long getTimestamp()    { return timestamp; }
}
