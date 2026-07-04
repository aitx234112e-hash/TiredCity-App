package com.tiredcity.app.data.repository;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.tiredcity.app.data.model.ApiListResponse;
import com.tiredcity.app.data.model.ApiResponse;
import com.tiredcity.app.data.model.Order;
import com.tiredcity.app.data.network.ApiService;
import java.util.List;
import retrofit2.Call;

public class OrderRepository {
    private final ApiService apiService;
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    public OrderRepository(ApiService apiService) {
        this.apiService = apiService;
    }

    /** Lấy đơn hàng từ Firestore thay vì API để đồng bộ với PaymentActivity. */
    public void getOrdersFromFirestore(String userId, OnOrdersLoadedListener listener) {
        db.collection("orders")
            .whereEqualTo("userId", userId)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener(queryDocumentSnapshots -> {
                List<Order> orders = queryDocumentSnapshots.toObjects(Order.class);
                listener.onSuccess(orders);
            })
            .addOnFailureListener(e -> listener.onError(e.getMessage()));
    }

    public void getOrderByIdFromFirestore(String orderId, OnOrderLoadedListener listener) {
        db.collection("orders").document(orderId).get()
            .addOnSuccessListener(doc -> {
                if (doc.exists()) {
                    Order o = doc.toObject(Order.class);
                    listener.onSuccess(o);
                } else {
                    listener.onError("Order not found");
                }
            })
            .addOnFailureListener(e -> listener.onError(e.getMessage()));
    }

    public interface OnOrdersLoadedListener {
        void onSuccess(List<Order> orders);
        void onError(String message);
    }

    public interface OnOrderLoadedListener {
        void onSuccess(Order order);
        void onError(String message);
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
