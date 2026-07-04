package com.tiredcity.admin.ui.order;

import android.os.Bundle;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.tiredcity.admin.databinding.ActivityAdminOrderBinding;
import java.util.ArrayList;
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
            public void onBindViewHolder(@NonNull OrderViewHolder holder, int position) {
                Map<String, Object> o = orders.get(position);
                
                String orderCode = o.get("orderCode") != null ? String.valueOf(o.get("orderCode")) : "N/A";
                String statusKey = o.get("status") != null ? String.valueOf(o.get("status")) : "PENDING";
                String total = o.get("total") != null ? String.valueOf(o.get("total")) : "0";
                
                String statusLabel = getStatusLabel(statusKey);
                
                holder.binding.tvTitle.setText(getString(R.string.order_item_title_fmt, getString(R.string.order_code), orderCode));
                holder.binding.tvSubtitle.setText(getString(R.string.order_item_subtitle_fmt, getString(R.string.order_total), total));
                holder.binding.tvBadge.setText(statusLabel);
                
                holder.itemView.setOnClickListener(v -> {
                    String next;
                    switch (statusKey) {
                        case "PENDING": next = "CONFIRMED"; break;
                        case "CONFIRMED": next = "SHIPPING"; break;
                        case "SHIPPING": next = "DELIVERED"; break;
                        default: next = "PENDING"; break;
                    }
                    
                    String docId = (String) o.get("docId");
                    if (docId != null) {
                        final String finalNext = next;
                        db.collection("orders").document(docId).update("status", finalNext)
                            .addOnSuccessListener(aVoid -> {
                                Toast.makeText(AdminOrderActivity.this, 
                                    getString(R.string.status_updated) + ": " + getStatusLabel(finalNext), 
                                    Toast.LENGTH_SHORT).show();
                                loadOrders();
                            })
                            .addOnFailureListener(e -> Toast.makeText(AdminOrderActivity.this, 
                                    getString(R.string.update_error, e.getMessage()), 
                                    Toast.LENGTH_SHORT).show());
                    }
                });
            }

            @Override
            public int getItemCount() { return orders.size(); }
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
        switch (status) {
            case "PENDING": return getString(R.string.status_pending);
            case "CONFIRMED": return getString(R.string.status_confirmed);
            case "SHIPPING": return getString(R.string.status_shipped);
            case "DELIVERED": return getString(R.string.status_delivered);
            case "CANCELLED": return getString(R.string.status_cancelled);
            default: return status;
        }
    }
}
