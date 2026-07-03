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
            .orderBy("id", Query.Direction.ASCENDING)
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

    public interface OnProductsLoadedListener {
        void onSuccess(List<Product> products);
        void onError(String message);
    }

    public void getProductByIdFromFirestore(String id, OnProductLoadedListener listener) {
        db.collection("products").whereEqualTo("id", id).limit(1).get()
            .addOnSuccessListener(queryDocumentSnapshots -> {
                if (!queryDocumentSnapshots.isEmpty()) {
                    Product p = queryDocumentSnapshots.getDocuments().get(0).toObject(Product.class);
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

    public Call<ApiListResponse<Review>> getProductReviews(String productId) {
        return apiService.getProductReviews(productId);
    }

    public Call<ApiResponse<Review>> addReview(String productId, Review review) {
        return apiService.addReview(productId, review);
    }
}
