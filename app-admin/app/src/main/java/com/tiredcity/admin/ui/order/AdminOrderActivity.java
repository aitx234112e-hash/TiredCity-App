package com.tiredcity.admin.ui.order;

import android.os.Bundle;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.DocumentSnapshot;
import com.tiredcity.admin.databinding.ActivityAdminOrderBinding;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.tiredcity.admin.R;
import com.tiredcity.admin.databinding.ItemAdminRowBinding;

public class AdminOrderActivity extends AppCompatActivity {

    private ActivityAdminOrderBinding binding;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityAdminOrderBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        db = FirebaseFirestore.getInstance();

        binding.rvOrders.setLayoutManager(new LinearLayoutManager(this));
        loadOrders();

        binding.btnBack.setOnClickListener(v -> finish());
    }

    private void loadOrders() {
        db.collection("orders").get()
            .addOnSuccessListener(snap -> {
                List<Map<String, Object>> orders = new ArrayList<>();
                for (QueryDocumentSnapshot d : snap) {
                    Map<String, Object> o = d.getData();
                    o.put("docId", d.getId());
                    orders.add(o);
                }
                displayOrders(orders);
            })
            .addOnFailureListener(e -> {
                Toast.makeText(this, getString(R.string.error_load_orders), Toast.LENGTH_SHORT).show();
            });
    }

    private void displayOrders(List<Map<String, Object>> orders) {
        binding.rvOrders.setAdapter(new androidx.recyclerview.widget.RecyclerView.Adapter<OrderViewHolder>() {
            @NonNull
            @Override
            public OrderViewHolder onCreateViewHolder(@NonNull android.view.ViewGroup parent, int viewType) {
                ItemAdminRowBinding itemBinding = ItemAdminRowBinding.inflate(getLayoutInflater(), parent, false);
                return new OrderViewHolder(itemBinding);
            }

            @Override
            @SuppressWarnings("unchecked")
            public void onBindViewHolder(@NonNull OrderViewHolder holder, int position) {
                Map<String, Object> o = orders.get(position);
                
                String orderCode = o.get("orderCode") != null ? String.valueOf(o.get("orderCode")) : "N/A";
                String customer = o.get("userName") != null ? String.valueOf(o.get("userName")) : "Khách vãng lai";
                String statusKey = o.get("status") != null ? String.valueOf(o.get("status")) : "PENDING";
                String total = o.get("totalPrice") != null ? String.valueOf(o.get("totalPrice")) : 
                              (o.get("total") != null ? String.valueOf(o.get("total")) : "0");
                
                holder.binding.tvTitle.setText(customer);
                holder.binding.tvSubtitle.setText(String.format("%s - %,.0f đ", orderCode, Double.parseDouble(total)));
                holder.binding.tvBadge.setText(getStatusLabel(statusKey));
                
                // Set color for badge
                int badgeColor = getResources().getColor(android.R.color.darker_gray);
                if ("PENDING".equals(statusKey)) badgeColor = getResources().getColor(android.R.color.holo_orange_dark);
                else if ("SHIPPING".equals(statusKey)) badgeColor = getResources().getColor(android.R.color.holo_blue_dark);
                else if ("DELIVERED".equals(statusKey)) badgeColor = getResources().getColor(android.R.color.holo_green_dark);
                else if ("CANCELLED".equals(statusKey)) badgeColor = getResources().getColor(android.R.color.holo_red_dark);
                holder.binding.tvBadge.setTextColor(badgeColor);

                holder.itemView.setOnClickListener(v -> showStatusUpdateDialog(o));
            }

            @Override
            public int getItemCount() { return orders.size(); }
        });
    }

    private void showStatusUpdateDialog(Map<String, Object> order) {
        String currentStatus = (String) order.get("status");
        String docId = (String) order.get("docId");
        
        String[] options = {"Xác nhận đơn (CONFIRMED)", "Giao hàng (SHIPPING)", "Hoàn tất (DELIVERED)", "Hủy đơn (CANCELLED)"};
        String[] keys = {"CONFIRMED", "SHIPPING", "DELIVERED", "CANCELLED"};

        new androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Cập nhật trạng thái đơn hàng")
            .setItems(options, (dialog, which) -> {
                updateOrderWithTransaction(docId, keys[which]);
            })
            .show();
    }

    @SuppressWarnings("unchecked")
    private void updateOrderWithTransaction(String docId, String newStatus) {
        db.runTransaction(transaction -> {
            com.google.firebase.firestore.DocumentReference orderRef = db.collection("orders").document(docId);
            DocumentSnapshot orderSnap = transaction.get(orderRef);
            
            if (!orderSnap.exists()) return null;
            
            String oldStatus = orderSnap.getString("status");
            if (newStatus.equals(oldStatus)) return null;

            Map<String, Object> updates = new HashMap<>();
            updates.put("status", newStatus);
            updates.put("updatedAt", new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", java.util.Locale.US).format(new java.util.Date()));

            // 1. Logic hoàn tồn kho nếu Hủy
            if ("CANCELLED".equals(newStatus) && !"CANCELLED".equals(oldStatus)) {
                List<Map<String, Object>> items = (List<Map<String, Object>>) orderSnap.get("orderItems");
                if (items == null) items = (List<Map<String, Object>>) orderSnap.get("items");
                
                if (items != null) {
                    for (Map<String, Object> item : items) {
                        String pId = (String) item.get("productId");
                        if (pId != null) {
                            com.google.firebase.firestore.DocumentReference pRef = db.collection("products").document(pId);
                            DocumentSnapshot pSnap = transaction.get(pRef);
                            if (pSnap.exists()) {
                                long qty = 0;
                                Object qObj = item.get("quantity");
                                if (qObj == null) qObj = item.get("qty");
                                if (qObj instanceof Long) qty = (Long) qObj;
                                else if (qObj instanceof Double) qty = ((Double) qObj).longValue();
                                else if (qObj instanceof Integer) qty = ((Integer) qObj).longValue();

                                Map<String, Object> pUpdates = new HashMap<>();
                                long currentTotalStock = pSnap.getLong("stock") != null ? pSnap.getLong("stock") : 0;
                                pUpdates.put("stock", currentTotalStock + qty);

                                // Hoàn kho theo Size
                                String selectedSize = (String) item.get("size");
                                if (selectedSize == null) selectedSize = (String) item.get("selected_size");

                                if (selectedSize != null) {
                                    boolean sizeFound = false;
                                    // Kiểm tra field 0, 1, 2...
                                    for (int i = 0; i <= 10; i++) {
                                        String field = String.valueOf(i);
                                        Object sObj = pSnap.get(field);
                                        if (sObj instanceof Map) {
                                            Map<String, Object> sizeInfo = new HashMap<>((Map<String, Object>) sObj);
                                            String sName = (String) sizeInfo.get("size");
                                            if (selectedSize.equalsIgnoreCase(sName)) {
                                                long sQty = 0;
                                                Object sq = sizeInfo.get("quantity");
                                                if (sq instanceof Number) sQty = ((Number) sq).longValue();
                                                sizeInfo.put("quantity", sQty + qty);
                                                pUpdates.put(field, sizeInfo);
                                                sizeFound = true;
                                                break;
                                            }
                                        }
                                    }

                                    // Kiểm tra mảng sizes
                                    if (!sizeFound) {
                                        List<Map<String, Object>> sizes = (List<Map<String, Object>>) pSnap.get("sizes");
                                        if (sizes != null) {
                                            List<Map<String, Object>> newSizes = new ArrayList<>();
                                            for (Map<String, Object> s : sizes) {
                                                Map<String, Object> sNew = new HashMap<>(s);
                                                if (selectedSize.equalsIgnoreCase((String) sNew.get("size"))) {
                                                    long sQty = ((Number) sNew.get("quantity")).longValue();
                                                    sNew.put("quantity", sQty + qty);
                                                }
                                                newSizes.add(sNew);
                                            }
                                            pUpdates.put("sizes", newSizes);
                                        }
                                    }
                                }
                                transaction.update(pRef, pUpdates);
                            }
                        }
                    }
                }
            }

            // 2. Logic thanh toán nếu Giao xong
            if ("DELIVERED".equals(newStatus)) {
                updates.put("isPaid", true);
                updates.put("paidAt", new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", java.util.Locale.US).format(new java.util.Date()));
            }

            // 3. Ghi log lịch sử
            List<Map<String, Object>> history = (List<Map<String, Object>>) orderSnap.get("history");
            if (history == null) history = new ArrayList<>();
            
            Map<String, Object> log = new HashMap<>();
            log.put("status", newStatus);
            log.put("time", updates.get("updatedAt"));
            log.put("actor", "Admin App"); // Có thể lấy tên user login nếu có module auth
            log.put("note", "Cập nhật trạng thái từ Android Admin");
            history.add(log);
            updates.put("history", history);

            transaction.update(orderRef, updates);
            return null;
        }).addOnSuccessListener(aVoid -> {
            Toast.makeText(this, "Cập nhật thành công!", Toast.LENGTH_SHORT).show();
            loadOrders();
        }).addOnFailureListener(e -> {
            Toast.makeText(this, "Lỗi: " + e.getMessage(), Toast.LENGTH_LONG).show();
        });
    }

    private static class OrderViewHolder extends androidx.recyclerview.widget.RecyclerView.ViewHolder {
        final ItemAdminRowBinding binding;
        OrderViewHolder(ItemAdminRowBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }

    private String getStatusLabel(String status) {
        if (status == null) return "N/A";
        String s = status.toUpperCase();
        switch (s) {
            case "PENDING": return getString(R.string.status_pending);
            case "CONFIRMED": 
            case "PROCESSING": return getString(R.string.status_confirmed);
            case "SHIPPING": 
            case "SHIPPED": return getString(R.string.status_shipped);
            case "DELIVERED": return getString(R.string.status_delivered);
            case "CANCELLED": return getString(R.string.status_cancelled);
            default: return status;
        }
    }
}
