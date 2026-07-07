package com.tiredcity.app.data.repository;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.tiredcity.app.data.model.Article;
import com.tiredcity.app.data.network.ApiService;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ArticleRepository {
    private final ApiService apiService;
    private final FirebaseFirestore db;

    public ArticleRepository(ApiService apiService) {
        this.apiService = apiService;
        this.db = FirebaseFirestore.getInstance();
    }

    public void getArticlesFromFirestore(OnArticlesLoadedListener listener) {
        // Bỏ where và orderBy trong query để tránh yêu cầu Index
        db.collection("blogs")
            .addSnapshotListener((value, error) -> {
                if (error != null) {
                    listener.onError(error.getMessage());
                    return;
                }
                if (value != null) {
                    List<Article> list = new ArrayList<>();
                    for (DocumentSnapshot doc : value.getDocuments()) {
                        Article a = doc.toObject(Article.class);
                        if (a != null && "published".equals(a.getStatus())) {
                            list.add(a);
                        }
                    }
                    
                    // Sắp xếp client-side theo thời gian (nếu có)
                    Collections.sort(list, (a, b) -> {
                        if (a.getPublishedDate() == null || b.getPublishedDate() == null) return 0;
                        return b.getPublishedDate().compareTo(a.getPublishedDate());
                    });

                    listener.onSuccess(list);
                }
            });
    }

    /** Tăng lượt xem bài viết bằng FieldValue.increment để đảm bảo tính nguyên tử */
    public void incrementArticleViews(String articleId) {
        if (articleId == null || articleId.isEmpty()) return;
        db.collection("blogs").document(articleId)
                .update("views", com.google.firebase.firestore.FieldValue.increment(1));
    }

    public void getArticleById(String id, OnArticleLoadedListener listener) {
        db.collection("blogs").document(id).get()
            .addOnSuccessListener(d -> {
                if (d.exists()) {
                    listener.onSuccess(d.toObject(Article.class));
                } else {
                    listener.onError("Article not found");
                }
            })
            .addOnFailureListener(e -> listener.onError(e.getMessage()));
    }

    public interface OnArticleLoadedListener {
        void onSuccess(Article article);
        void onError(String message);
    }

    public interface OnArticlesLoadedListener {
        void onSuccess(List<Article> articles);
        void onError(String message);
    }
}
