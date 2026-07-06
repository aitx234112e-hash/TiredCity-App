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
        // Tự động xác nhận các đơn hàng quá hạn 2 phút
        autoConfirmOverdueOrders(userId);

        // Query by either 'userId' or 'user' field to cover both old and new data
        com.google.firebase.firestore.Query query1 = db.collection("orders").whereEqualTo("userId", userId);
        com.google.firebase.firestore.Query query2 = db.collection("orders").whereEqualTo("user", userId);

        query1.get().addOnSuccessListener(snap1 -> {
            List<Order> orders = snap1.toObjects(Order.class);
            query2.get().addOnSuccessListener(snap2 -> {
                List<Order> orders2 = snap2.toObjects(Order.class);
                // Merge and deduplicate by ID
                java.util.Map<String, Order> merged = new java.util.HashMap<>();
                for (Order o : orders) if (o.getId() != null) merged.put(o.getId(), o);
                for (Order o : orders2) if (o.getId() != null) merged.put(o.getId(), o);
                
                listener.onSuccess(new java.util.ArrayList<>(merged.values()));
            }).addOnFailureListener(e -> listener.onSuccess(orders)); // Fallback to first query if second fails
        }).addOnFailureListener(e -> listener.onError(e.getMessage()));
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

    /** Hủy đơn hàng và hoàn tồn kho cho khách hàng (Transaction chuẩn: Đọc trước, Ghi sau). */
    public void cancelOrderInFirestore(String orderId, String reason, OnOrderActionFinishedListener listener) {
        db.runTransaction(transaction -> {
            com.google.firebase.firestore.DocumentReference orderRef = db.collection("orders").document(orderId);
            com.google.firebase.firestore.DocumentSnapshot orderSnap = transaction.get(orderRef);

            if (!orderSnap.exists()) throw new RuntimeException("Đơn hàng không tồn tại");

            String oldStatus = orderSnap.getString("status");
            if ("CANCELLED".equalsIgnoreCase(oldStatus)) return null;

            List<java.util.Map<String, Object>> items = (List<java.util.Map<String, Object>>) orderSnap.get("items");
            if (items == null) items = (List<java.util.Map<String, Object>>) orderSnap.get("orderItems");

            if (items != null) {
                // BƯỚC 1: ĐỌC TẤT CẢ (READ ALL)
                java.util.Map<String, com.google.firebase.firestore.DocumentSnapshot> productSnaps = new java.util.HashMap<>();
                for (java.util.Map<String, Object> item : items) {
                    String pId = (String) item.get("productId");
                    if (pId != null && !productSnaps.containsKey(pId)) {
                        productSnaps.put(pId, transaction.get(db.collection("products").document(pId)));
                    }
                }

                // BƯỚC 2 & 3: TÍNH TOÁN VÀ GHI (CALCULATE & WRITE)
                for (java.util.Map<String, Object> item : items) {
                    String pId = (String) item.get("productId");
                    com.google.firebase.firestore.DocumentSnapshot pSnap = productSnaps.get(pId);
                    
                    if (pSnap != null && pSnap.exists()) {
                        long qty = 0;
                        Object qObj = item.get("quantity");
                        if (qObj instanceof Number) qty = ((Number) qObj).longValue();

                        java.util.Map<String, Object> pUpdates = new java.util.HashMap<>();
                        long currentStock = pSnap.getLong("stock") != null ? pSnap.getLong("stock") : 0;
                        pUpdates.put("stock", currentStock + qty);

                        // Hoàn size
                        String selectedSize = (String) item.get("size");
                        if (selectedSize != null) {
                            boolean sizeFound = false;
                            for (int i = 0; i <= 10; i++) {
                                String field = String.valueOf(i);
                                Object sObj = pSnap.get(field);
                                if (sObj instanceof java.util.Map) {
                                    java.util.Map<String, Object> sizeInfo = new java.util.HashMap<>((java.util.Map<String, Object>) sObj);
                                    if (selectedSize.equalsIgnoreCase((String) sizeInfo.get("size"))) {
                                        long sQty = ((Number) sizeInfo.get("quantity")).longValue();
                                        sizeInfo.put("quantity", sQty + qty);
                                        pUpdates.put(field, sizeInfo);
                                        sizeFound = true;
                                        break;
                                    }
                                }
                            }
                            if (!sizeFound && pSnap.get("sizes") instanceof List) {
                                List<java.util.Map<String, Object>> sizes = (List<java.util.Map<String, Object>>) pSnap.get("sizes");
                                List<java.util.Map<String, Object>> newSizes = new java.util.ArrayList<>();
                                for (java.util.Map<String, Object> s : sizes) {
                                    java.util.Map<String, Object> sNew = new java.util.HashMap<>(s);
                                    if (selectedSize.equalsIgnoreCase((String) sNew.get("size"))) {
                                        long sQty = ((Number) sNew.get("quantity")).longValue();
                                        sNew.put("quantity", sQty + qty);
                                    }
                                    newSizes.add(sNew);
                                }
                                pUpdates.put("sizes", newSizes);
                            }
                        }
                        transaction.update(db.collection("products").document(pId), pUpdates);
                    }
                }
            }

            // Cập nhật trạng thái đơn hàng
            java.util.Map<String, Object> orderUpdates = new java.util.HashMap<>();
            orderUpdates.put("status", "CANCELLED");
            orderUpdates.put("updatedAt", com.google.firebase.Timestamp.now());

            List<java.util.Map<String, Object>> history = (List<java.util.Map<String, Object>>) orderSnap.get("history");
            if (history == null) history = new java.util.ArrayList<>();
            java.util.Map<String, Object> log = new java.util.HashMap<>();
            log.put("status", "CANCELLED");
            log.put("time", com.google.firebase.Timestamp.now());
            log.put("actor", "Khách hàng");
            log.put("note", reason);
            history.add(log);
            orderUpdates.put("history", history);

            transaction.update(orderRef, orderUpdates);
            return null;
        }).addOnSuccessListener(v -> listener.onFinished(true, null))
          .addOnFailureListener(e -> listener.onFinished(false, e.getMessage()));
    }

    private void autoConfirmOverdueOrders(String userId) {
        long twoMinsAgo = System.currentTimeMillis() - (2 * 60 * 1000);
        db.collection("orders")
            .whereEqualTo("userId", userId)
            .whereEqualTo("status", "SHIPPED")
            .get()
            .addOnSuccessListener(snap -> {
                for (com.google.firebase.firestore.DocumentSnapshot doc : snap) {
                    com.google.firebase.Timestamp ts = doc.getTimestamp("updatedAt");
                    if (ts != null && ts.toDate().getTime() < twoMinsAgo) {
                        updateOrderStatusInFirestore(doc.getId(), "DELIVERED", "Hệ thống tự động xác nhận sau 2 phút", (success, err) -> {});
                    }
                }
            });
    }

    public void updateOrderStatusInFirestore(String orderId, String newStatus, String note, OnOrderActionFinishedListener listener) {
        com.google.firebase.firestore.DocumentReference orderRef = db.collection("orders").document(orderId);
        
        db.runTransaction(transaction -> {
            com.google.firebase.firestore.DocumentSnapshot orderSnap = transaction.get(orderRef);
            if (!orderSnap.exists()) throw new RuntimeException("Đơn hàng không tồn tại");

            java.util.Map<String, Object> updates = new java.util.HashMap<>();
            updates.put("status", newStatus);
            updates.put("updatedAt", com.google.firebase.Timestamp.now());

            if ("DELIVERED".equalsIgnoreCase(newStatus)) {
                updates.put("isPaid", true); // Đã nhận hàng thì coi như đã thanh toán (nếu là COD)
            }

            List<java.util.Map<String, Object>> history = (List<java.util.Map<String, Object>>) orderSnap.get("history");
            if (history == null) history = new java.util.ArrayList<>();
            
            java.util.Map<String, Object> log = new java.util.HashMap<>();
            log.put("status", newStatus);
            log.put("time", com.google.firebase.Timestamp.now());
            log.put("actor", "Khách hàng");
            log.put("note", note);
            history.add(log);
            updates.put("history", history);

            transaction.update(orderRef, updates);
            return null;
        }).addOnSuccessListener(v -> listener.onFinished(true, null))
          .addOnFailureListener(e -> listener.onFinished(false, e.getMessage()));
    }

    public interface OnOrderActionFinishedListener {
        void onFinished(boolean success, String error);
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
