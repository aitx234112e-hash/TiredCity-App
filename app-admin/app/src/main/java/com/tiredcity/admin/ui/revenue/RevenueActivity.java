package com.tiredcity.admin.ui.revenue;

import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.tiredcity.admin.R;
import com.tiredcity.admin.adapter.RowAdapter;
import com.tiredcity.admin.data.AdminModule;
import com.tiredcity.admin.databinding.ActivityRevenueBinding;
import com.tiredcity.admin.utils.DocUtils;
import com.tiredcity.admin.utils.RowFormatter;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;

/**
 * Bao cao doanh thu — mirror trang Revenue ben web-admin:
 * 4 chi so tong hop, bieu do cot doanh thu 6 thang gan nhat, top don gia tri cao.
 */
public class RevenueActivity extends AppCompatActivity {

    private static final int MONTHS = 6;
    private static final int TOP_ORDERS = 6;

    private ActivityRevenueBinding binding;
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final RowAdapter topAdapter = new RowAdapter();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityRevenueBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        getWindow().setStatusBarColor(ContextCompat.getColor(this, R.color.m_revenue));

        binding.btnBack.setOnClickListener(v -> finish());
        binding.rvTop.setLayoutManager(new LinearLayoutManager(this));
        binding.rvTop.setNestedScrollingEnabled(false);
        binding.rvTop.setAdapter(topAdapter);

        binding.swipeRefresh.setOnRefreshListener(this::loadData);
        loadData();
    }

    private void loadData() {
        binding.swipeRefresh.setRefreshing(true);
        db.collection("orders").get()
                .addOnSuccessListener(snap -> {
                    compute(snap.getDocuments());
                    binding.swipeRefresh.setRefreshing(false);
                })
                .addOnFailureListener(e -> {
                    binding.swipeRefresh.setRefreshing(false);
                    Toast.makeText(this, getString(R.string.load_error, e.getMessage()),
                            Toast.LENGTH_LONG).show();
                });
    }

    private void compute(List<DocumentSnapshot> orders) {
        double total = 0, paidRevenue = 0;
        int paidCount = 0;
        for (DocumentSnapshot d : orders) {
            double t = orderTotal(d);
            total += t;
            if (isPaid(d)) {
                paidRevenue += t;
                paidCount++;
            }
        }
        double avg = orders.isEmpty() ? 0 : Math.round(total / orders.size());

        binding.tvTotal.setText(DocUtils.money(total));
        binding.tvPaid.setText(DocUtils.money(paidRevenue));
        binding.tvPaidCount.setText(getString(R.string.list_count_fmt, paidCount));
        binding.tvOrders.setText(String.valueOf(orders.size()));
        binding.tvAvg.setText(DocUtils.money(avg));

        renderChart(monthBuckets(orders));
        renderTopOrders(orders);
    }

    private static double orderTotal(DocumentSnapshot d) {
        return DocUtils.num(d, "totalPrice", "total", "amount");
    }

    private static boolean isPaid(DocumentSnapshot d) {
        String status = DocUtils.str(d, "status");
        return Boolean.TRUE.equals(d.getBoolean("isPaid"))
                || Boolean.TRUE.equals(d.getBoolean("paid"))
                || "delivered".equals(status) || "completed".equals(status);
    }

    /** Doanh thu theo 6 thang gan nhat, phan tu cuoi la thang hien tai. */
    private double[] monthBuckets(List<DocumentSnapshot> orders) {
        Calendar now = Calendar.getInstance();
        int curYear = now.get(Calendar.YEAR);
        int curMonth = now.get(Calendar.MONTH);

        double[] values = new double[MONTHS];
        Calendar c = Calendar.getInstance();
        for (DocumentSnapshot d : orders) {
            long ms = DocUtils.millis(d, "createdAt", "created_at", "date", "orderDate");
            if (ms <= 0) continue;
            c.setTimeInMillis(ms);
            int diff = (curYear - c.get(Calendar.YEAR)) * 12 + (curMonth - c.get(Calendar.MONTH));
            if (diff >= 0 && diff < MONTHS) {
                values[MONTHS - 1 - diff] += orderTotal(d);
            }
        }
        return values;
    }

    private void renderChart(double[] values) {
        LinearLayout bars = binding.chartBars;
        bars.removeAllViews();

        double max = 1;
        for (double v : values) max = Math.max(max, v);

        Calendar now = Calendar.getInstance();
        int barArea = dp(110);
        int barColor = ContextCompat.getColor(this, R.color.m_revenue);
        int secondary = ContextCompat.getColor(this, R.color.tc_text_secondary);

        for (int i = 0; i < values.length; i++) {
            LinearLayout col = new LinearLayout(this);
            col.setOrientation(LinearLayout.VERTICAL);
            col.setGravity(Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL);
            col.setLayoutParams(new LinearLayout.LayoutParams(0,
                    LinearLayout.LayoutParams.MATCH_PARENT, 1f));

            TextView tvVal = new TextView(this);
            tvVal.setText(DocUtils.compact(values[i]));
            tvVal.setTextSize(10);
            tvVal.setTextColor(secondary);
            col.addView(tvVal);

            View bar = new View(this);
            GradientDrawable bg = new GradientDrawable();
            bg.setColor(barColor);
            bg.setCornerRadius(dp(4));
            bar.setBackground(bg);
            int h = (int) Math.max(dp(4), barArea * values[i] / max);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(dp(18), h);
            lp.topMargin = dp(4);
            bar.setLayoutParams(lp);
            col.addView(bar);

            // Nhan thang: lui tu thang hien tai ve truoc (Th1..Th12)
            Calendar m = (Calendar) now.clone();
            m.add(Calendar.MONTH, i - (values.length - 1));
            TextView tvMonth = new TextView(this);
            tvMonth.setText(getString(R.string.month_fmt, m.get(Calendar.MONTH) + 1));
            tvMonth.setTextSize(11);
            tvMonth.setTextColor(secondary);
            LinearLayout.LayoutParams mlp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            mlp.topMargin = dp(6);
            tvMonth.setLayoutParams(mlp);
            col.addView(tvMonth);

            bars.addView(col);
        }
    }

    private void renderTopOrders(List<DocumentSnapshot> orders) {
        List<DocumentSnapshot> sorted = new ArrayList<>(orders);
        Collections.sort(sorted, (a, b) -> Double.compare(orderTotal(b), orderTotal(a)));
        if (sorted.size() > TOP_ORDERS) sorted = sorted.subList(0, TOP_ORDERS);
        topAdapter.submit(RowFormatter.format(this, AdminModule.ORDERS, sorted));
    }

    private int dp(int v) {
        return (int) (v * getResources().getDisplayMetrics().density);
    }
}
