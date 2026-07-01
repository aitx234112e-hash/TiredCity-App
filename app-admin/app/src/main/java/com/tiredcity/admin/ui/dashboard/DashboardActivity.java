package com.tiredcity.admin.ui.dashboard;

import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.tiredcity.admin.data.model.DashboardOverview;
import com.tiredcity.admin.data.remote.ApiClient;
import com.tiredcity.admin.databinding.ActivityDashboardBinding;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/** Hien thi cac chi so tong quan (A2) lay tu backend. */
public class DashboardActivity extends AppCompatActivity {

    private ActivityDashboardBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityDashboardBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        loadOverview();
    }

    private void loadOverview() {
        // TODO: thay bang JWT thuc te tu SharedPreferences
        String bearer = "Bearer <token>";
        ApiClient.api().getOverview(bearer).enqueue(new Callback<DashboardOverview>() {
            @Override
            public void onResponse(@NonNull Call<DashboardOverview> call, @NonNull Response<DashboardOverview> res) {
                if (res.isSuccessful() && res.body() != null) {
                    DashboardOverview o = res.body();
                    binding.tvRevenue.setText(String.valueOf(o.revenue));
                    binding.tvOrders.setText(String.valueOf(o.totalOrders));
                    binding.tvCustomers.setText(String.valueOf(o.totalCustomers));
                }
            }

            @Override
            public void onFailure(@NonNull Call<DashboardOverview> call, @NonNull Throwable t) {
                Toast.makeText(DashboardActivity.this, "Loi tai du lieu: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}
