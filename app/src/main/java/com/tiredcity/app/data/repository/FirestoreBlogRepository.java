package com.tiredcity.app.data.repository;

import androidx.annotation.NonNull;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.tiredcity.app.data.model.Article;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Đọc bài Tin tức trực tiếp từ Firestore collection {@code blogs} — CÙNG nguồn với
 * web-admin / app-admin (mục Blog). Dùng thay REST ({@code api/articles}) chưa có, để
 * trang Tin tức app khách hiển thị đúng bài admin đăng thay vì dữ liệu mẫu cứng.
 *
 * Field khớp schema admin (ModuleForm.BLOGS): title, excerpt, content, thumbnail,
 * authorName, status, publishedAt. Chỉ lấy bài {@code status == "published"}.
 */
public class FirestoreBlogRepository {

    public interface Callback { void onResult(List<Article> articles); }

    private static final String COLLECTION = "blogs";

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    /** Tải bài đã xuất bản, mới nhất lên đầu; trả về {@code null} nếu lỗi (nơi gọi fallback mock). */
    public void getArticles(@NonNull Callback cb) {
        db.collection(COLLECTION).get()
                .addOnSuccessListener(snap -> {
                    List<Article> list = new ArrayList<>();
                    for (QueryDocumentSnapshot d : snap) {
                        String status = str(d, "status");
                        // Chỉ hiện bài đã xuất bản; bài cũ không có field status coi như published.
                        if (status != null && !status.isEmpty() && !"published".equalsIgnoreCase(status)) continue;
                        list.add(map(d));
                    }
                    Collections.sort(list, (a, b) -> {
                        long ta = a.getPublishedAt() != null ? a.getPublishedAt().getTime() : 0;
                        long tb = b.getPublishedAt() != null ? b.getPublishedAt().getTime() : 0;
                        return Long.compare(tb, ta); // giảm dần: mới nhất trước
                    });
                    cb.onResult(list);
                })
                .addOnFailureListener(e -> cb.onResult(null));
    }

    private static Article map(DocumentSnapshot d) {
        Article a = new Article();
        a.setId(d.getId());
        String title = str(d, "title");
        a.setTitle(title);
        a.setTitleVi(title);
        a.setSummary(str(d, "excerpt"));
        a.setContent(str(d, "content"));
        a.setImageUrl(str(d, "thumbnail"));
        a.setAuthor(str(d, "authorName"));
        a.setPublishedAt(parseDate(str(d, "publishedAt")));
        return a;
    }

    /** publishedAt admin ghi là chuỗi ISO; hỗ trợ thêm dạng "yyyy-MM-dd" cho chắc. */
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
