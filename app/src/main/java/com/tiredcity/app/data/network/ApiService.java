package com.tiredcity.app.data.network;

import com.tiredcity.app.data.model.ApiListResponse;
import com.tiredcity.app.data.model.ApiResponse;
import com.tiredcity.app.data.model.Article;
import com.tiredcity.app.data.model.CartItem;
import com.tiredcity.app.data.model.Category;
import com.tiredcity.app.data.model.Event;
import com.tiredcity.app.data.model.Order;
import com.tiredcity.app.data.model.Product;
import com.tiredcity.app.data.model.Review;
import com.tiredcity.app.data.model.User;
import com.tiredcity.app.data.model.UserProfile;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Path;
import retrofit2.http.Query;
import java.util.Map;

public interface ApiService {

    // ── Auth ──────────────────────────────────────────────────────────────────

    @POST("api/auth/login")
    Call<ApiResponse<User>> login(@Body Map<String, String> body);

    @POST("api/auth/register")
    Call<ApiResponse<User>> register(@Body Map<String, String> body);

    // ── Products ──────────────────────────────────────────────────────────────

    @GET("api/products")
    Call<ApiListResponse<Product>> getProducts(
            @Query("page")     int page,
            @Query("limit")    int limit,
            @Query("category") String category,
            @Query("q")        String query
    );

    @GET("api/products/featured")
    Call<ApiListResponse<Product>> getFeaturedProducts();

    @GET("api/products/{id}")
    Call<ApiResponse<Product>> getProductById(@Path("id") String id);

    @GET("api/products/recommend")
    Call<ApiListResponse<Product>> getRecommendedProducts(@Query("menh") String menh);

    @GET("api/products/search")
    Call<ApiListResponse<Product>> searchProducts(@Query("q") String query);

    // ── Categories ────────────────────────────────────────────────────────────

    @GET("api/categories")
    Call<ApiListResponse<Category>> getCategories();

    // ── Orders ────────────────────────────────────────────────────────────────

    @POST("api/orders")
    Call<ApiResponse<Order>> createOrder(@Body Order order);

    @GET("api/orders/my")
    Call<ApiListResponse<Order>> getMyOrders();

    @GET("api/orders/{id}")
    Call<ApiResponse<Order>> getOrderById(@Path("id") String id);

    // ── User profile ──────────────────────────────────────────────────────────

    @GET("api/users/profile")
    Call<ApiResponse<UserProfile>> getProfile();

    @PUT("api/users/profile")
    Call<ApiResponse<UserProfile>> updateProfile(@Body UserProfile profile);

    // ── Cart ──────────────────────────────────────────────────────────────────

    @GET("api/cart")
    Call<ApiListResponse<CartItem>> getCartItems();

    @POST("api/cart")
    Call<ApiResponse<CartItem>> addToCart(@Body CartItem item);

    @PUT("api/cart/{id}")
    Call<ApiResponse<CartItem>> updateCartItem(@Path("id") String id, @Body CartItem item);

    @DELETE("api/cart/{id}")
    Call<ApiResponse<Void>> removeCartItem(@Path("id") String id);

    // ── Reviews ───────────────────────────────────────────────────────────────

    @GET("api/products/{id}/reviews")
    Call<ApiListResponse<Review>> getProductReviews(@Path("id") String productId);

    @POST("api/products/{id}/reviews")
    Call<ApiResponse<Review>> addReview(@Path("id") String productId, @Body Review review);

    // ── Articles & Events ─────────────────────────────────────────────────────

    @GET("api/articles")
    Call<ApiListResponse<Article>> getArticles(
            @Query("page") int page,
            @Query("limit") int limit);

    @GET("api/events")
    Call<ApiListResponse<Event>> getEvents(
            @Query("page") int page,
            @Query("limit") int limit);
}
