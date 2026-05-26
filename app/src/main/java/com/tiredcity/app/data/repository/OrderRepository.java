package com.tiredcity.app.data.repository;

import com.tiredcity.app.data.model.ApiListResponse;
import com.tiredcity.app.data.model.ApiResponse;
import com.tiredcity.app.data.model.Order;
import com.tiredcity.app.data.network.ApiService;
import retrofit2.Call;

public class OrderRepository {
    private final ApiService apiService;

    public OrderRepository(ApiService apiService) {
        this.apiService = apiService;
    }

    public Call<ApiListResponse<Order>> getOrders() {
        return apiService.getMyOrders();
    }

    public Call<ApiResponse<Order>> getOrderById(String id) {
        return apiService.getOrderById(id);
    }

    public Call<ApiResponse<Order>> createOrder(Order order) {
        return apiService.createOrder(order);
    }
}
