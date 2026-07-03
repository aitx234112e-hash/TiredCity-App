package com.tiredcity.admin.ui.list;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.tiredcity.admin.R;
import com.tiredcity.admin.adapter.RowAdapter;
import com.tiredcity.admin.data.AdminModule;
import com.tiredcity.admin.data.ModuleForm;
import com.tiredcity.admin.data.model.DetailField;
import com.tiredcity.admin.data.model.Row;
import com.tiredcity.admin.databinding.ActivityModuleListBinding;
import com.tiredcity.admin.ui.form.EntityFormActivity;
import com.tiredcity.admin.utils.AuditLogger;
import com.tiredcity.admin.utils.DetailFormatter;
import com.tiredcity.admin.utils.DocUtils;
import com.tiredcity.admin.utils.RowFormatter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Man hinh danh sach dung chung cho moi module co collection Firestore
 * (mirror cac trang quan ly ben web-admin: products, orders, users, vouchers...).
 * Ho tro tim kiem client-side, keo de lam moi, cham vao 1 dong de xem chi tiet;
 * rieng ORDERS co hanh dong cap nhat trang thai, FEEDBACK co danh dau da phan hoi.
 */
public class ModuleListActivity extends AppCompatActivity {

    public static final String EXTRA_MODULE = "extra_module";

    private ActivityModuleListBinding binding;
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    private AdminModule module;
    private RowAdapter adapter;

    private final List<DocumentSnapshot> allDocs = new ArrayList<>();
    /** Danh sach doc dang hien thi, song song 1-1 voi rows cua adapter. */
    private final List<DocumentSnapshot> shownDocs = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityModuleListBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        String name = getIntent().getStringExtra(EXTRA_MODULE);
        try {
            module = AdminModule.valueOf(name);
        } catch (Exception e) {
            finish();
            return;
        }
        if (module.collection == null) {
            finish();
            return;
        }

        int color = ContextCompat.getColor(this, module.colorRes);
        binding.headerBar.setBackgroundColor(color);
        getWindow().setStatusBarColor(color);
        binding.tvTitle.setText(module.title);
        binding.tvCount.setText(module.desc);

        binding.btnBack.setOnClickListener(v -> finish());

        adapter = new RowAdapter(this::openDetail);
        binding.rvRows.setLayoutManager(new LinearLayoutManager(this));
        binding.rvRows.addItemDecoration(new DividerItemDecoration(this, DividerItemDecoration.VERTICAL));
        binding.rvRows.setAdapter(adapter);

        binding.swipeRefresh.setOnRefreshListener(this::loadData);

        binding.etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) {}
            @Override public void onTextChanged(CharSequence s, int a, int b, int c) {}
            @Override public void afterTextChanged(Editable s) { applyFilter(); }
        });

        if (ModuleForm.canCreate(module)) {
            binding.fabAdd.setVisibility(View.VISIBLE);
            binding.fabAdd.setBackgroundTintList(ColorStateList.valueOf(color));
            binding.fabAdd.setOnClickListener(v -> openForm(null));
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Tai lai sau khi them / sua / xoa o man hinh form
        loadData();
    }

    private void loadData() {
        binding.swipeRefresh.setRefreshing(true);
        db.collection(module.collection).get()
                .addOnSuccessListener(snap -> {
                    allDocs.clear();
                    allDocs.addAll(snap.getDocuments());
                    // Moi nhat len dau (giong sap xep mac dinh ben web)
                    Collections.sort(allDocs, (a, b) -> Long.compare(
                            DocUtils.millis(b, "createdAt", "timestamp", "updatedAt", "date"),
                            DocUtils.millis(a, "createdAt", "timestamp", "updatedAt", "date")));
                    applyFilter();
                    binding.swipeRefresh.setRefreshing(false);
                })
                .addOnFailureListener(e -> {
                    binding.swipeRefresh.setRefreshing(false);
                    Toast.makeText(this, getString(R.string.load_error, e.getMessage()),
                            Toast.LENGTH_LONG).show();
                });
    }

    private void applyFilter() {
        String q = binding.etSearch.getText().toString().trim().toLowerCase(Locale.ROOT);
        shownDocs.clear();
        List<Row> rows = new ArrayList<>();
        for (DocumentSnapshot d : allDocs) {
            Row r = RowFormatter.formatOne(this, module, d);
            if (q.isEmpty() || matches(r, q)) {
                shownDocs.add(d);
                rows.add(r);
            }
        }
        adapter.submit(rows);
        binding.tvEmpty.setVisibility(rows.isEmpty() ? View.VISIBLE : View.GONE);
        binding.tvCount.setText(getString(R.string.list_count_fmt, rows.size()));
    }

    private static boolean matches(Row r, String q) {
        return contains(r.title, q) || contains(r.subtitle, q)
                || contains(r.meta, q) || contains(r.badge, q);
    }

    private static boolean contains(String s, String q) {
        return s != null && s.toLowerCase(Locale.ROOT).contains(q);
    }

    // ------------------------------------------------------------------ detail

    private void openDetail(int position) {
        if (position < 0 || position >= shownDocs.size()) return;
        DocumentSnapshot d = shownDocs.get(position);
        switch (module) {
            case ORDERS:
                showOrderDetail(d);
                break;
            case FEEDBACK:
                showFeedbackDetail(d);
                break;
            default:
                if (ModuleForm.canEdit(module) || ModuleForm.canDelete(module)) {
                    showEntityDetail(d);
                } else {
                    DetailDialog.show(this, getString(module.title),
                            DetailFormatter.format(this, module, d), null, null);
                }
        }
    }

    /**
     * Chi tiet ban ghi co CRUD: nut Sua (mo form) + nut Xoa (mirror edit/delete ben web-admin).
     */
    private void showEntityDetail(DocumentSnapshot d) {
        String posLabel = null, neuLabel = null;
        DetailDialog.OnAction posAction = null, neuAction = null;

        if (ModuleForm.canEdit(module)) {
            posLabel = getString(R.string.btn_edit);
            posAction = () -> openForm(d.getId());
        }
        if (ModuleForm.canDelete(module)) {
            neuLabel = getString(R.string.btn_delete);
            neuAction = () -> confirmDelete(d);
        }

        DetailDialog.show(this, getString(module.title),
                DetailFormatter.format(this, module, d),
                posLabel, posAction, neuLabel, neuAction);
    }

    /** Mo man hinh them (docId == null) hoac sua ban ghi. */
    private void openForm(String docId) {
        Intent i = new Intent(this, EntityFormActivity.class);
        i.putExtra(EntityFormActivity.EXTRA_MODULE, module.name());
        if (docId != null) i.putExtra(EntityFormActivity.EXTRA_DOC_ID, docId);
        startActivity(i);
    }

    /** Xac nhan roi xoa document; ghi audit + tai lai danh sach. */
    private void confirmDelete(DocumentSnapshot d) {
        String label = ModuleForm.targetOf(module, d);
        String message = label.isEmpty()
                ? getString(R.string.confirm_delete)
                : getString(R.string.confirm_delete_fmt, label);
        new AlertDialog.Builder(this)
                .setMessage(message)
                .setPositiveButton(R.string.btn_delete, (dialog, which) ->
                        d.getReference().delete()
                                .addOnSuccessListener(x -> {
                                    AuditLogger.log(ModuleForm.auditDelete(module), label, "");
                                    Toast.makeText(this, R.string.deleted_ok, Toast.LENGTH_SHORT).show();
                                    loadData();
                                })
                                .addOnFailureListener(e -> Toast.makeText(this,
                                        getString(R.string.delete_error, e.getMessage()),
                                        Toast.LENGTH_LONG).show()))
                .setNegativeButton(R.string.btn_close, null)
                .show();
    }

    /** Lien he: xem chi tiet + danh dau da phan hoi (mirror markReplied) + xoa. */
    private void showFeedbackDetail(DocumentSnapshot d) {
        boolean replied = Boolean.TRUE.equals(d.getBoolean("replied"));
        DetailDialog.show(this, getString(module.title),
                DetailFormatter.format(this, module, d),
                replied ? null : getString(R.string.btn_mark_replied),
                replied ? null : () -> d.getReference().update("replied", true)
                        .addOnSuccessListener(x -> {
                            Toast.makeText(this, R.string.feedback_marked, Toast.LENGTH_SHORT).show();
                            loadData();
                        })
                        .addOnFailureListener(e -> Toast.makeText(this,
                                getString(R.string.update_error, e.getMessage()),
                                Toast.LENGTH_LONG).show()),
                getString(R.string.btn_delete), () -> confirmDelete(d));
    }

    /**
     * Don hang: chi tiet + hanh dong theo trang thai (mirror modal order-management web):
     * pending -> xac nhan / huy; processing -> gui hang (nhap ma van don) / huy;
     * shipped -> xac nhan da giao; delivered/cancelled -> chi xem.
     */
    private void showOrderDetail(DocumentSnapshot d) {
        String orderId = DocUtils.str(d, "orderID", "id");
        if (orderId.isEmpty()) orderId = d.getId();
        String status = DocUtils.str(d, "status");

        List<DetailField> f = new ArrayList<>();
        f.add(new DetailField(getString(R.string.order_code), orderId));
        f.add(new DetailField(getString(R.string.order_customer), DocUtils.str(d, "userName", "customer")));
        f.add(new DetailField(getString(R.string.order_address),
                DocUtils.str(d, "address", "shippingAddress", "deliveryAddress")));
        f.add(new DetailField(getString(R.string.order_items), itemsSummary(d)));
        f.add(new DetailField(getString(R.string.order_total),
                DocUtils.money(DocUtils.num(d, "totalPrice", "total", "amount"))));
        f.add(new DetailField(getString(R.string.order_status), RowFormatter.orderStatusLabel(this, status)));
        f.add(new DetailField(getString(R.string.order_paid), isPaid(d)
                ? getString(R.string.paid_yes) : getString(R.string.paid_no)));
        f.add(new DetailField(getString(R.string.order_created), DocUtils.date(d, "createdAt")));
        f.add(new DetailField(getString(R.string.order_tracking),
                DocUtils.str(d, "trackingCode", "shipping.trackingCode")));

        final String id = orderId;
        String posLabel = null, neuLabel = null;
        DetailDialog.OnAction posAction = null, neuAction = null;

        switch (status) {
            case "pending":
                posLabel = getString(R.string.btn_confirm_order);
                posAction = () -> updateStatus(d, id, "processing");
                neuLabel = getString(R.string.btn_cancel_order);
                neuAction = () -> confirmCancel(d, id);
                break;
            case "processing":
                posLabel = getString(R.string.btn_ship_order);
                posAction = () -> promptTracking(d, id);
                neuLabel = getString(R.string.btn_cancel_order);
                neuAction = () -> confirmCancel(d, id);
                break;
            case "shipped":
                posLabel = getString(R.string.btn_mark_delivered);
                posAction = () -> updateStatus(d, id, "delivered");
                break;
            default:
                break;
        }

        DetailDialog.show(this, getString(R.string.order_detail_title), f,
                posLabel, posAction, neuLabel, neuAction);
    }

    /** Gop danh sach san pham trong don thanh chuoi "• Ten ×SL" moi dong. */
    private String itemsSummary(DocumentSnapshot d) {
        Object items = d.get("orderItems");
        if (!(items instanceof List)) items = d.get("items");
        if (!(items instanceof List)) return "";
        StringBuilder sb = new StringBuilder();
        for (Object o : (List<?>) items) {
            if (!(o instanceof Map)) continue;
            Map<?, ?> m = (Map<?, ?>) o;
            Object name = first(m, "product_name", "name", "title");
            Object qty = first(m, "qty", "quantity", "qtyOrdered");
            if (sb.length() > 0) sb.append('\n');
            sb.append("• ").append(name == null ? "?" : name);
            if (qty != null) sb.append(" ×").append(qty);
        }
        return sb.toString();
    }

    private static Object first(Map<?, ?> m, String... keys) {
        for (String k : keys) {
            Object v = m.get(k);
            if (v != null && !String.valueOf(v).trim().isEmpty()) return v;
        }
        return null;
    }

    private static boolean isPaid(DocumentSnapshot d) {
        String status = DocUtils.str(d, "status");
        return Boolean.TRUE.equals(d.getBoolean("isPaid"))
                || Boolean.TRUE.equals(d.getBoolean("paid"))
                || "delivered".equals(status) || "completed".equals(status);
    }

    // ------------------------------------------------------------ order actions

    private void updateStatus(DocumentSnapshot d, String orderId, String newStatus) {
        String old = DocUtils.str(d, "status");
        d.getReference().update("status", newStatus)
                .addOnSuccessListener(x -> {
                    AuditLogger.log("order.status", orderId, old + " → " + newStatus);
                    Toast.makeText(this, R.string.status_updated, Toast.LENGTH_SHORT).show();
                    loadData();
                })
                .addOnFailureListener(e -> Toast.makeText(this,
                        getString(R.string.update_error, e.getMessage()), Toast.LENGTH_LONG).show());
    }

    private void confirmCancel(DocumentSnapshot d, String orderId) {
        new AlertDialog.Builder(this)
                .setMessage(R.string.confirm_cancel_order)
                .setPositiveButton(R.string.btn_cancel_order, (dialog, which) ->
                        d.getReference().update("status", "cancelled")
                                .addOnSuccessListener(x -> {
                                    AuditLogger.log("order.cancel", orderId, "");
                                    Toast.makeText(this, R.string.status_updated, Toast.LENGTH_SHORT).show();
                                    loadData();
                                })
                                .addOnFailureListener(e -> Toast.makeText(this,
                                        getString(R.string.update_error, e.getMessage()),
                                        Toast.LENGTH_LONG).show()))
                .setNegativeButton(R.string.btn_close, null)
                .show();
    }

    /** processing -> shipped: nhap ma van don roi cap nhat (mirror shipOrder web). */
    private void promptTracking(DocumentSnapshot d, String orderId) {
        EditText input = new EditText(this);
        input.setHint(R.string.hint_tracking_code);
        input.setSingleLine(true);

        FrameLayout wrap = new FrameLayout(this);
        int pad = (int) (20 * getResources().getDisplayMetrics().density);
        wrap.setPadding(pad, pad / 2, pad, 0);
        wrap.addView(input);

        new AlertDialog.Builder(this)
                .setTitle(R.string.btn_ship_order)
                .setView(wrap)
                .setPositiveButton(R.string.btn_ship_order, (dialog, which) -> {
                    String code = input.getText().toString().trim();
                    if (code.isEmpty()) return;
                    Map<String, Object> patch = new java.util.HashMap<>();
                    patch.put("status", "shipped");
                    patch.put("trackingCode", code);
                    d.getReference().update(patch)
                            .addOnSuccessListener(x -> {
                                AuditLogger.log("order.status", orderId, "Gửi hàng — " + code);
                                Toast.makeText(this, R.string.status_updated, Toast.LENGTH_SHORT).show();
                                loadData();
                            })
                            .addOnFailureListener(e -> Toast.makeText(this,
                                    getString(R.string.update_error, e.getMessage()),
                                    Toast.LENGTH_LONG).show());
                })
                .setNegativeButton(R.string.btn_close, null)
                .show();
    }
}
