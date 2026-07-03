package com.tiredcity.app.data.model;

import com.google.gson.annotations.SerializedName;
import java.util.List;

/**
 * Phan hoi tu Messages API. Chi lay nhung field can dung.
 */
public class ClaudeResponse {

    public List<ContentBlock> content;
    @SerializedName("stop_reason") public String stopReason;

    /** Ghep tat ca cac block dang text thanh mot chuoi tra ve nguoi dung. */
    public String text() {
        if (content == null) return null;
        StringBuilder sb = new StringBuilder();
        for (ContentBlock b : content) {
            if (b != null && "text".equals(b.type) && b.text != null) {
                if (sb.length() > 0) sb.append("\n\n");
                sb.append(b.text);
            }
        }
        return sb.length() > 0 ? sb.toString() : null;
    }

    public static class ContentBlock {
        public String type;
        public String text;
    }
}
