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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
    }

    private void loadOrders() {
        db.collection("orders").get().addOnSuccessListener(snap -> {
            List<Map<String, Object>> orders = new ArrayList<>();
            for (QueryDocumentSnapshot d : snap) {
                Map<String, Object> o = d.getData();
                o.put("docId", d.getId());
                orders.add(o);
            }
            displayOrders(orders);
        });
    }

    private void displayOrders(List<Map<String, Object>> orders) {
        binding.rvOrders.setAdapter(new androidx.recyclerview.widget.RecyclerView.Adapter<androidx.recyclerview.widget.RecyclerView.ViewHolder>() {
            @NonNull
            @Override
            public androidx.recyclerview.widget.RecyclerView.ViewHolder onCreateViewHolder(@NonNull android.view.ViewGroup parent, int viewType) {
                android.widget.TextView tv = new android.widget.TextView(parent.getContext());
                tv.setPadding(32, 32, 32, 32);
                tv.setTextSize(16);
                return new androidx.recyclerview.widget.RecyclerView.ViewHolder(tv) {};
            }

            @Override
            public void onBindViewHolder(@NonNull androidx.recyclerview.widget.RecyclerView.ViewHolder holder, int position) {
                Map<String, Object> o = orders.get(position);
                android.widget.TextView tv = (android.widget.TextView) holder.itemView;
                tv.setText("Order #" + o.get("orderCode") + "\nStatus: " + o.get("status") + "\nTotal: " + o.get("total"));
                
                tv.setOnClickListener(v -> {
                    String current = (String) o.get("status");
                    String next = "PENDING";
                    if ("PENDING".equals(current)) next = "CONFIRMED";
                    else if ("CONFIRMED".equals(current)) next = "SHIPPING";
                    else if ("SHIPPING".equals(current)) next = "DELIVERED";
                    
                    if (!next.equals(current)) {
                        db.collection("orders").document((String) o.get("docId")).update("status", next)
                            .addOnSuccessListener(aVoid -> {
                                Toast.makeText(AdminOrderActivity.this, "Đã cập nhật: " + next, Toast.LENGTH_SHORT).show();
                                loadOrders();
                            });
                    }
                });
            }

            @Override
            public int getItemCount() { return orders.size(); }
        });
    }
}
