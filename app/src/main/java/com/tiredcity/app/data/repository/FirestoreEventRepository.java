package com.tiredcity.app.data.repository;

import androidx.annotation.NonNull;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.tiredcity.app.data.model.Event;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Đọc sự kiện trực tiếp từ Firestore collection {@code events} — CÙNG nguồn với
 * web-admin / app-admin (project {@code tiredcity-daf1e}). Dùng thay cho REST backend
 * ({@code api/events}) vốn chưa có, để trang Sự kiện app khách hiển thị đúng dữ liệu
 * admin quản lý thay vì danh sách mẫu cứng.
 *
 * Field khớp schema admin (ModuleForm.EVENTS): title, date (yyyy-MM-dd), location,
 * description, image.
 */
public class FirestoreEventRepository {

    public interface Callback { void onResult(List<Event> events); }

    private static final String COLLECTION = "events";

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    /** Tải toàn bộ sự kiện, sắp theo ngày tăng dần; trả về {@code null} nếu lỗi (nơi gọi fallback mock). */
    public void getEvents(@NonNull Callback cb) {
        db.collection(COLLECTION).get()
                .addOnSuccessListener(snap -> {
                    List<Event> list = new ArrayList<>();
                    for (QueryDocumentSnapshot d : snap) list.add(map(d));
                    Collections.sort(list, (a, b) -> {
                        long ta = a.getStartDate() != null ? a.getStartDate().getTime() : Long.MAX_VALUE;
                        long tb = b.getStartDate() != null ? b.getStartDate().getTime() : Long.MAX_VALUE;
                        return Long.compare(ta, tb);
                    });
                    cb.onResult(list);
                })
                .addOnFailureListener(e -> cb.onResult(null));
    }

    private static Event map(DocumentSnapshot d) {
        Event e = new Event();
        e.setId(d.getId());
        e.setTitle(str(d, "title"));
        String location = str(d, "location");
        e.setLocation(location);
        e.setDescription(str(d, "description"));
        e.setImageUrl(str(d, "image"));
        e.setStartDate(parseDate(str(d, "date")));
        e.setOnline(location != null && location.toLowerCase(Locale.ROOT).contains("online"));
        return e;
    }

    /** Ngày admin nhập có thể là "yyyy-MM-dd" (DATE) hoặc chuỗi ISO đầy đủ. */
    private static Date parseDate(String s) {
        if (s == null || s.isEmpty()) return null;
        String[] patterns = {"yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", "yyyy-MM-dd'T'HH:mm:ss'Z'", "yyyy-MM-dd"};
        for (String p : patterns) {
            try {
                return new SimpleDateFormat(p, Locale.US).parse(s);
            } catch (Exception ignored) { }
        }
        return null;
    }

    private static String str(DocumentSnapshot d, String key) {
        Object v = d.get(key);
        return v != null ? v.toString() : null;
    }
}
