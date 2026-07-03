package com.tiredcity.admin.ui.reports;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.tiredcity.admin.R;
import com.tiredcity.admin.databinding.ActivityReportsBinding;
import com.tiredcity.admin.utils.AuditLogger;
import com.tiredcity.admin.utils.DocUtils;

import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Bao cao & xuat du lieu — mirror trang Reports ben web-admin.
 * Moi dataset (don hang / san pham / tai khoan) co so luong ban ghi + nut xuat CSV
 * (luu qua he thong chon tep, BOM UTF-8 de Excel doc dung tieng Viet).
 */
public class ReportsActivity extends AppCompatActivity {

    private ActivityReportsBinding binding;
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    private final Map<String, List<DocumentSnapshot>> cache = new HashMap<>();
    private final Map<String, TextView> countViews = new HashMap<>();
    private final Map<String, Button> exportButtons = new HashMap<>();
    private TextView tvRevenueCount;

    /** Du lieu CSV dang cho ghi sau khi nguoi dung chon vi tri luu. */
    private String pendingCsv;
    private String pendingKey;
    private int pendingRows;

    private final ActivityResultLauncher<Intent> saveLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() != RESULT_OK || result.getData() == null
                        || result.getData().getData() == null || pendingCsv == null) {
                    return;
                }
                writeCsv(result.getData().getData());
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityReportsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        getWindow().setStatusBarColor(ContextCompat.getColor(this, R.color.m_reports));
        binding.btnBack.setOnClickListener(v -> finish());

        addCard("orders", R.string.report_orders_title, R.string.report_orders_desc, true);
        addCard("products", R.string.report_products_title, R.string.report_products_desc, true);
        addCard("users", R.string.report_users_title, R.string.report_users_desc, true);
        addCard("revenue", R.string.report_revenue_title, R.string.report_revenue_desc, false);

        loadCollection("orders");
        loadCollection("products");
        loadCollection("users");
    }

    private void addCard(String key, int titleRes, int descRes, boolean exportable) {
        View card = LayoutInflater.from(this)
                .inflate(R.layout.item_report_card, binding.containerCards, false);
        ((TextView) card.findViewById(R.id.tvCardTitle)).setText(titleRes);
        ((TextView) card.findViewById(R.id.tvCardDesc)).setText(descRes);

        TextView tvCount = card.findViewById(R.id.tvCardCount);
        Button btnExport = card.findViewById(R.id.btnExport);

        if (exportable) {
            countViews.put(key, tvCount);
            exportButtons.put(key, btnExport);
            btnExport.setEnabled(false);
            btnExport.setOnClickListener(v -> export(key));
        } else {
            tvRevenueCount = tvCount;
            btnExport.setVisibility(View.GONE);
        }
        binding.containerCards.addView(card);
    }

    private void loadCollection(String key) {
        db.collection(key).get()
                .addOnSuccessListener(snap -> {
                    cache.put(key, snap.getDocuments());
                    TextView tv = countViews.get(key);
                    if (tv != null) tv.setText(String.valueOf(snap.size()));
                    Button btn = exportButtons.get(key);
                    if (btn != null) btn.setEnabled(true);
                    if ("orders".equals(key)) updateRevenueCard(snap.getDocuments());
                })
                .addOnFailureListener(e -> {
                    TextView tv = countViews.get(key);
                    if (tv != null) tv.setText("0");
                });
    }

    private void updateRevenueCard(List<DocumentSnapshot> orders) {
        double total = 0;
        for (DocumentSnapshot d : orders) {
            total += DocUtils.num(d, "totalPrice", "total", "amount");
        }
        if (tvRevenueCount != null) tvRevenueCount.setText(DocUtils.money(total));
    }

    // ---------------------------------------------------------------- export

    /** Mo hop thoai luu tep; CSV duoc ghi trong callback saveLauncher. */
    private void export(String key) {
        List<String[]> rows = toRows(key);
        if (rows.isEmpty()) return;

        StringBuilder sb = new StringBuilder("\uFEFF"); // BOM de Excel doc dung UTF-8
        for (int i = 0; i < rows.size(); i++) {
            if (i > 0) sb.append("\r\n");
            String[] r = rows.get(i);
            for (int j = 0; j < r.length; j++) {
                if (j > 0) sb.append(',');
                sb.append(csvEscape(r[j]));
            }
        }
        pendingCsv = sb.toString();
        pendingKey = key;
        pendingRows = rows.size() - 1; // tru dong header

        String date = new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date());
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT)
                .addCategory(Intent.CATEGORY_OPENABLE)
                .setType("text/csv")
                .putExtra(Intent.EXTRA_TITLE, "tiredcity_" + key + "_" + date + ".csv");
        saveLauncher.launch(intent);
    }

    private void writeCsv(Uri uri) {
        try (OutputStream os = getContentResolver().openOutputStream(uri)) {
            os.write(pendingCsv.getBytes(StandardCharsets.UTF_8));
            os.flush();
            AuditLogger.log("export.csv", pendingKey, "Xuất " + pendingRows + " dòng");
            Toast.makeText(this, getString(R.string.export_done_fmt, pendingRows),
                    Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(this, R.string.export_error, Toast.LENGTH_LONG).show();
        } finally {
            pendingCsv = null;
        }
    }

    /** Header + du lieu tung dataset, giu dung cot nhu ham toRows ben web. */
    private List<String[]> toRows(String key) {
        List<DocumentSnapshot> data = cache.get(key);
        List<String[]> rows = new ArrayList<>();
        if (data == null) return rows;

        if ("orders".equals(key)) {
            rows.add(new String[]{"Mã đơn", "Khách hàng", "Tổng tiền", "Trạng thái", "Thanh toán", "Ngày đặt"});
            for (DocumentSnapshot d : data) {
                String id = DocUtils.str(d, "orderID", "id");
                rows.add(new String[]{
                        id.isEmpty() ? d.getId() : id,
                        DocUtils.str(d, "userName", "customer"),
                        String.valueOf((long) DocUtils.num(d, "totalPrice", "total", "amount")),
                        DocUtils.str(d, "status"),
                        Boolean.TRUE.equals(d.getBoolean("isPaid")) ? "Đã thanh toán" : "Chưa",
                        DocUtils.date(d, "createdAt"),
                });
            }
        } else if ("products".equals(key)) {
            rows.add(new String[]{"Tên sản phẩm", "Giá", "Tồn kho", "Danh mục"});
            for (DocumentSnapshot d : data) {
                rows.add(new String[]{
                        DocUtils.str(d, "product_name", "name", "title"),
                        String.valueOf((long) DocUtils.num(d, "unit_price", "price")),
                        String.valueOf((long) DocUtils.num(d, "stocked_quantity", "stock")),
                        DocUtils.str(d, "product_dept", "category"),
                });
            }
        } else {
            rows.add(new String[]{"Tên", "Email", "SĐT", "Vai trò", "Trạng thái"});
            for (DocumentSnapshot d : data) {
                String role = DocUtils.str(d, "role");
                rows.add(new String[]{
                        DocUtils.str(d, "profileName", "fullName"),
                        DocUtils.str(d, "email"),
                        DocUtils.str(d, "phone"),
                        role.isEmpty() ? "user" : role,
                        Boolean.TRUE.equals(d.getBoolean("disabled")) ? "Vô hiệu hoá" : "Hoạt động",
                });
            }
        }
        return rows;
    }

    private static String csvEscape(String val) {
        String s = val == null ? "" : val;
        if (s.contains("\"") || s.contains(",") || s.contains("\n")) {
            return '"' + s.replace("\"", "\"\"") + '"';
        }
        return s;
    }
}
