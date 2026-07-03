package com.tiredcity.app.data.model;

import com.google.gson.annotations.SerializedName;
import java.util.List;

/**
 * Body cho POST https://api.anthropic.com/v1/messages
 */
public class ClaudeRequest {

    public String model;
    @SerializedName("max_tokens") public int maxTokens;
    public String system;
    public List<Message> messages;

    public ClaudeRequest(String model, int maxTokens, String system, List<Message> messages) {
        this.model    = model;
        this.maxTokens = maxTokens;
        this.system   = system;
        this.messages = messages;
    }

    /** Mot luot hoi thoai: role = "user" hoac "assistant". */
    public static class Message {
        public String role;
        public String content;

        public Message(String role, String content) {
            this.role    = role;
            this.content = content;
        }
    }
}
