package com.tiredcity.app.data.model;

/**
 * Body gui len n8n webhook cua AI Agent.
 * n8n doc: body.message, body.sessionId, body.orderId (xem docs/n8n-ai-agent-setup.md).
 */
public class N8nRequest {

    public String message;
    public String sessionId;
    public String orderId;

    public N8nRequest(String message, String sessionId, String orderId) {
        this.message   = message;
        this.sessionId = sessionId;
        this.orderId   = orderId == null ? "" : orderId;
    }
}
