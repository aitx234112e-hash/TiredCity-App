package com.tiredcity.app.data.repository;

import com.tiredcity.app.data.model.ApiListResponse;
import com.tiredcity.app.data.model.ApiResponse;
import com.tiredcity.app.data.model.Product;
import com.tiredcity.app.data.model.Review;
import com.tiredcity.app.data.network.ApiService;
import retrofit2.Call;

public class ProductRepository {
    private final ApiService apiService;

    public ProductRepository(ApiService apiService) {
        this.apiService = apiService;
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
