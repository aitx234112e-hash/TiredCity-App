package com.tiredcity.app.data.repository;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.tiredcity.app.data.model.ApiListResponse;
import com.tiredcity.app.data.model.ApiResponse;
import com.tiredcity.app.data.model.Product;
import com.tiredcity.app.data.model.Review;
import com.tiredcity.app.data.network.ApiService;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import retrofit2.Call;

public class ProductRepository {
    private final ApiService apiService;
    private final FirebaseFirestore db;

    public ProductRepository(ApiService apiService) {
        this.apiService = apiService;
        this.db = FirebaseFirestore.getInstance();
    }

    /**
     * Lấy sản phẩm từ Firebase Firestore (Realtime).
     * Ưu tiên dùng Firebase theo yêu cầu dự án.
     */
    public void getProductsFromFirestore(OnProductsLoadedListener listener) {
        db.collection("products")
            .addSnapshotListener((value, error) -> {
                if (error != null) {
                    android.util.Log.e("ProductRepository", "Firestore Error: " + error.getMessage());
                    listener.onError(error.getMessage());
                    return;
                }
                if (value != null) {
                    List<Product> list = value.toObjects(Product.class);
                    android.util.Log.d("ProductRepository", "Loaded " + list.size() + " products");
                    listener.onSuccess(list);
                }
            });
    }

    public interface OnProductsLoadedListener {
        void onSuccess(List<Product> products);
        void onError(String message);
    }

    public void getProductByIdFromFirestore(String id, OnProductLoadedListener listener) {
        db.collection("products").document(id).get()
            .addOnSuccessListener(documentSnapshot -> {
                if (documentSnapshot.exists()) {
                    Product p = documentSnapshot.toObject(Product.class);
                    listener.onSuccess(p);
                } else {
                    listener.onError("Không tìm thấy sản phẩm");
                }
            })
            .addOnFailureListener(e -> listener.onError(e.getMessage()));
    }

    public void getProductsByCategoryFromFirestore(String category, OnProductsLoadedListener listener) {
        db.collection("products").whereEqualTo("product_dept", category)
            .addSnapshotListener((value, error) -> {
                if (error != null) {
                    listener.onError(error.getMessage());
                    return;
                }
                if (value != null) {
                    List<Product> list = value.toObjects(Product.class);
                    listener.onSuccess(list);
                }
            });
    }

    public interface OnProductLoadedListener {
        void onSuccess(Product product);
        void onError(String message);
    }

    public Call<ApiListResponse<Product>> getProducts(int page, int size, String categoryId, String keyword) {
        return apiService.getProducts(page, size, categoryId, keyword);
    }

    public Call<ApiResponse<Product>> getProductById(String id) {
        return apiService.getProductById(id);
    }

    public Call<ApiListResponse<Product>> getFeaturedProducts() {
        return apiService.getFeaturedProducts();
    }

    public void getProductReviewsFromFirestore(String productId, OnReviewsLoadedListener listener) {
        db.collection("reviews")
            .whereEqualTo("productId", productId)
            .addSnapshotListener((value, error) -> {
                if (error != null) {
                    listener.onError(error.getMessage());
                    return;
                }
                if (value != null) {
                    List<Review> list = value.toObjects(Review.class);
                    // Sắp xếp client-side để không yêu cầu Index
                    java.util.Collections.sort(list, (r1, r2) -> {
                        if (r1.getCreatedAt() == null || r2.getCreatedAt() == null) return 0;
                        return r2.getCreatedAt().compareTo(r1.getCreatedAt());
                    });
                    listener.onSuccess(list);
                }
            });
    }

    public interface OnReviewsLoadedListener {
        void onSuccess(List<Review> reviews);
        void onError(String message);
    }

    public interface OnActionCompleteListener {
        void onSuccess();
        void onError(String message);
    }

    public interface OnCheckPurchaseListener {
        void onResult(boolean hasPurchased);
        void onError(String message);
    }

    public void checkUserPurchasedProduct(String userId, String productId, OnCheckPurchaseListener listener) {
        // Kiểm tra trong collection "orders" xem có đơn hàng nào của user này chứa productId không
        // Giả sử cấu trúc Order có List<OrderItem> chứa productId
        db.collection("orders")
            .whereEqualTo("userId", userId)
            .whereEqualTo("status", "DELIVERED")
            .get()
            .addOnSuccessListener(queryDocumentSnapshots -> {
                boolean found = false;
                if (queryDocumentSnapshots != null) {
                    for (com.google.firebase.firestore.DocumentSnapshot doc : queryDocumentSnapshots) {
                        List<Map<String, Object>> items = (List<Map<String, Object>>) doc.get("items");
                        if (items != null) {
                            for (Map<String, Object> item : items) {
                                if (productId.equals(item.get("productId"))) {
                                    found = true;
                                    break;
                                }
                            }
                        }
                        if (found) break;
                    }
                }
                listener.onResult(found);
            })
            .addOnFailureListener(e -> listener.onError(e.getMessage()));
    }

    public void addReviewToFirestore(Review review, OnActionCompleteListener listener) {
        db.collection("reviews").add(review)
            .addOnSuccessListener(documentReference -> {
                review.setId(documentReference.getId());
                documentReference.update("id", review.getId());
                listener.onSuccess();
            })
            .addOnFailureListener(e -> listener.onError(e.getMessage()));
    }

    public Call<ApiListResponse<Review>> getProductReviews(String productId) {
        return apiService.getProductReviews(productId);
    }

    public Call<ApiResponse<Review>> addReview(String productId, Review review) {
        return apiService.addReview(productId, review);
    }
}
