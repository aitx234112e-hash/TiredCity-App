package com.tiredcity.app.data.model;

/**
 * Phan hoi tu n8n: node "Respond to Webhook" tra { "reply": "..." }.
 */
public class N8nResponse {

    public String reply;

    /** Cau tra loi cua Agent, hoac null neu khong co. */
    public String text() {
        return reply != null && !reply.trim().isEmpty() ? reply.trim() : null;
    }
}
