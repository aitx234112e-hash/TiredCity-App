package com.tiredcity.admin.ui.dashboard;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
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
import com.tiredcity.admin.ui.shipping.ShippingConfigActivity;
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

        setupLightStatusBar();
        applyStatusBarInset();

        String email = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getEmail() : null;
        String name = (email != null && email.contains("@"))
                ? email.substring(0, email.indexOf('@')) : getString(R.string.role_admin);
        binding.tvHello.setText(getString(R.string.dashboard_greeting_fmt, name, currentDateVi()));

        binding.btnLogout.setOnClickListener(v -> logout());

        GridLayoutManager layoutManager = new GridLayoutManager(this, 2);
        ModuleTileAdapter adapter = new ModuleTileAdapter(this::openModule);
        layoutManager.setSpanSizeLookup(adapter.spanSizeLookup(2));
        binding.rvModules.setLayoutManager(layoutManager);
        binding.rvModules.setNestedScrollingEnabled(false);
        binding.rvModules.setAdapter(adapter);

        binding.swipeRefresh.setOnRefreshListener(this::loadOverview);
    }

    /** Header do -> icon status bar mau sang (trang) de nhin ro. */
    private void setupLightStatusBar() {
        WindowInsetsControllerCompat c = WindowCompat.getInsetsController(getWindow(), binding.getRoot());
        c.setAppearanceLightStatusBars(false);
        // API < 35 chua ap dung edge-to-edge -> to mau status bar cung tone do dinh header.
        getWindow().setStatusBarColor(androidx.core.content.ContextCompat.getColor(this, R.color.tc_red_lacquer_top));
    }

    /** targetSdk 35 ve behind status bar -> pad header bang chieu cao status bar. */
    private void applyStatusBarInset() {
        final int hPad = dp(20), topBase = dp(10), botPad = dp(20);
        ViewCompat.setOnApplyWindowInsetsListener(binding.headerBar, (v, insets) -> {
            int top = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top;
            v.setPadding(hPad, top + topBase, hPad, botPad);
            return insets;
        });
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadOverview();
    }

    private void openModule(AdminModule m) {
        Intent i;
        switch (m) {
            case REVENUE:
                i = new Intent(this, RevenueActivity.class);
                break;
            case REPORTS:
                i = new Intent(this, ReportsActivity.class);
                break;
            case CHATBOT:
                i = new Intent(this, ChatbotActivity.class);
                break;
            case SHIPPING:
                i = new Intent(this, ShippingConfigActivity.class);
                break;
            default:
                i = new Intent(this, ModuleListActivity.class);
                i.putExtra(ModuleListActivity.EXTRA_MODULE, m.name());
                break;
        }
        startActivity(i);
    }

    /** Doc nhanh 3 chi so tu Firestore: doanh thu, so don, so khach hang. */
    private void loadOverview() {
        binding.swipeRefresh.setRefreshing(true);

        db.collection("orders").get().addOnSuccessListener(snap -> {
            double totalRev = 0;
            int pending = 0;
            for (QueryDocumentSnapshot d : snap) {
                String status = DocUtils.str(d, "status");
                // Doanh thu chỉ tính trên các đơn hàng đã hoàn tất (DELIVERED)
                if ("DELIVERED".equalsIgnoreCase(status)) {
                    totalRev += DocUtils.num(d, "totalPrice", "total", "amount");
                }
                if ("pending".equalsIgnoreCase(status)) pending++;
            }
            binding.tvRevenue.setText(DocUtils.money(totalRev));
            binding.tvOrders.setText(String.valueOf(snap.size()));
            binding.tvPending.setText(getString(R.string.dashboard_pending_fmt, pending));
            binding.swipeRefresh.setRefreshing(false);
        }).addOnFailureListener(e -> binding.swipeRefresh.setRefreshing(false));

        db.collection("users").get()
                .addOnSuccessListener(snap -> binding.tvCustomers.setText(String.valueOf(snap.size())))
                .addOnFailureListener(e -> {});
    }

    /** VD: "Thứ Sáu, 03/07/2026". */
    private String currentDateVi() {
        String[] days = {"Chủ Nhật", "Thứ Hai", "Thứ Ba", "Thứ Tư", "Thứ Năm", "Thứ Sáu", "Thứ Bảy"};
        java.util.Calendar cal = java.util.Calendar.getInstance();
        String dayName = days[cal.get(java.util.Calendar.DAY_OF_WEEK) - 1];
        return getString(R.string.dashboard_date_fmt, dayName,
                new java.text.SimpleDateFormat("dd/MM/yyyy", new java.util.Locale("vi", "VN")).format(cal.getTime()));
    }

    private void logout() {
        FirebaseAuth.getInstance().signOut();
        Intent i = new Intent(this, LoginActivity.class);
        i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(i);
        finish();
    }
}
