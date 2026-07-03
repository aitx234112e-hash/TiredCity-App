package com.tiredcity.admin.ui.dashboard;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.tiredcity.admin.R;
import com.tiredcity.admin.adapter.ModuleTileAdapter;
import com.tiredcity.admin.data.AdminModule;
import com.tiredcity.admin.databinding.ActivityDashboardBinding;
import com.tiredcity.admin.ui.auth.LoginActivity;
import com.tiredcity.admin.ui.chatbot.ChatbotActivity;
import com.tiredcity.admin.ui.list.ModuleListActivity;
import com.tiredcity.admin.ui.reports.ReportsActivity;
import com.tiredcity.admin.ui.revenue.RevenueActivity;
import com.tiredcity.admin.utils.DocUtils;

/** Trang chu admin: cac chi so tong quan + luoi chuc nang (mirror nav web-admin). */
public class DashboardActivity extends AppCompatActivity {

    private ActivityDashboardBinding binding;
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityDashboardBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        String email = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getEmail() : null;
        binding.tvHello.setText(email != null ? email : getString(R.string.dashboard_subtitle));

        binding.btnLogout.setOnClickListener(v -> logout());

        binding.rvModules.setLayoutManager(new GridLayoutManager(this, 2));
        binding.rvModules.setNestedScrollingEnabled(false);
        binding.rvModules.setAdapter(new ModuleTileAdapter(this::openModule));

        binding.swipeRefresh.setOnRefreshListener(this::loadOverview);
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadOverview();
    }

    private void openModule(AdminModule m) {
        Intent i;
        if (m == AdminModule.REVENUE) {
            i = new Intent(this, RevenueActivity.class);
        } else if (m == AdminModule.REPORTS) {
            i = new Intent(this, ReportsActivity.class);
        } else if (m == AdminModule.CHATBOT) {
            i = new Intent(this, ChatbotActivity.class);
        } else {
            i = new Intent(this, ModuleListActivity.class);
            i.putExtra(ModuleListActivity.EXTRA_MODULE, m.name());
        }
        startActivity(i);
    }

    /** Doc nhanh 3 chi so tu Firestore: doanh thu, so don, so khach hang. */
    private void loadOverview() {
        binding.swipeRefresh.setRefreshing(true);

        db.collection("orders").get().addOnSuccessListener(snap -> {
            double revenue = 0;
            int pending = 0;
            for (QueryDocumentSnapshot d : snap) {
                revenue += DocUtils.num(d, "totalPrice", "total", "amount");
                if ("pending".equalsIgnoreCase(DocUtils.str(d, "status"))) pending++;
            }
            binding.tvRevenue.setText(DocUtils.money(revenue));
            binding.tvOrders.setText(String.valueOf(snap.size()));
            binding.tvPending.setText(getString(R.string.dashboard_pending_fmt, pending));
            binding.swipeRefresh.setRefreshing(false);
        }).addOnFailureListener(e -> binding.swipeRefresh.setRefreshing(false));

        db.collection("users").get()
                .addOnSuccessListener(snap -> binding.tvCustomers.setText(String.valueOf(snap.size())));
    }

    private void logout() {
        FirebaseAuth.getInstance().signOut();
        Intent i = new Intent(this, LoginActivity.class);
        i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(i);
        finish();
    }
}
