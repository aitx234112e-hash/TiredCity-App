package com.tiredcity.app.data.repository;

import com.tiredcity.app.data.local.CartLocalStore;
import com.tiredcity.app.data.model.ApiListResponse;
import com.tiredcity.app.data.model.ApiResponse;
import com.tiredcity.app.data.model.CartItem;
import com.tiredcity.app.data.network.ApiService;
import retrofit2.Call;

public class CartRepository {
    private final ApiService apiService;
    private final CartLocalStore localStore;

    public CartRepository(ApiService apiService, CartLocalStore localStore) {
        this.apiService = apiService;
        this.localStore = localStore;
    }

    public Call<ApiListResponse<CartItem>> getCartItems() {
        return apiService.getCartItems();
    }

    public Call<ApiResponse<CartItem>> addToCart(CartItem item) {
        return apiService.addToCart(item);
    }

    public Call<ApiResponse<CartItem>> updateCartItem(String id, CartItem item) {
        return apiService.updateCartItem(id, item);
    }

    public Call<ApiResponse<Void>> removeCartItem(String id) {
        return apiService.removeCartItem(id);
    }

    public CartLocalStore getLocalStore() {
        return localStore;
    }
}
