package com.tiredcity.app.data.repository;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.HashMap;
import java.util.Map;

/**
 * Đồng bộ hồ sơ user Firebase Auth (Google Sign-In, email/password...) vào Firestore
 * collection {@code users} — id document = Firebase Auth UID. Dùng {@link SetOptions#merge()}
 * để không ghi đè các field khác (địa chỉ, mệnh, style preferences...) mà web-admin/app đã
 * cập nhật sau đó.
 */
public class FirestoreUserRepository {

    private static final String COLLECTION = "users";

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    /** Tạo/cập nhật document users/{uid} với thông tin lấy được từ tài khoản đăng nhập. */
    public void syncUser(@NonNull String uid, @Nullable String email, @Nullable String name,
                          @Nullable String avatarUrl, @Nullable String provider) {
        Map<String, Object> data = new HashMap<>();
        data.put("uid", uid);
        if (email != null)      data.put("email", email);
        if (name != null)       data.put("name", name);
        if (avatarUrl != null)  data.put("avatarUrl", avatarUrl);
        if (provider != null)   data.put("provider", provider);
        data.put("lastLoginAt", System.currentTimeMillis());

        db.collection(COLLECTION).document(uid).set(data, SetOptions.merge());
    }
}
