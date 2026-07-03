package com.tiredcity.admin.utils;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

/**
 * Ghi nhat ky thao tac admin vao collection 'auditLogs' — mirror AuditService (web-admin).
 * Khong chan luong chinh: loi ghi log bi bo qua.
 */
public final class AuditLogger {
    private AuditLogger() {}

    public static void log(String action, String target, String detail) {
        FirebaseUser u = FirebaseAuth.getInstance().getCurrentUser();
        String actor = "admin";
        if (u != null) {
            actor = u.getEmail() != null ? u.getEmail() : u.getUid();
        }

        // ISO-8601 UTC giong new Date().toISOString() ben web
        SimpleDateFormat iso = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US);
        iso.setTimeZone(TimeZone.getTimeZone("UTC"));

        Map<String, Object> entry = new HashMap<>();
        entry.put("action", action);
        entry.put("target", target == null ? "" : target);
        entry.put("detail", detail == null ? "" : detail);
        entry.put("actor", actor);
        entry.put("timestamp", iso.format(new Date()));

        FirebaseFirestore.getInstance().collection("auditLogs").add(entry);
    }
}
