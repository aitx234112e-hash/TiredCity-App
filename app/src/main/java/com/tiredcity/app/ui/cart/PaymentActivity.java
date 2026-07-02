package com.tiredcity.app.ui.cart;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.tiredcity.app.data.local.CartLocalStore;
import com.tiredcity.app.data.model.ApiResponse;
import com.tiredcity.app.data.model.CartItem;
import com.tiredcity.app.data.model.Order;
import com.tiredcity.app.data.network.ApiClient;
import com.tiredcity.app.data.repository.OrderRepository;
import com.tiredcity.app.databinding.ActivityPaymentBinding;
import com.tiredcity.app.ui.base.BaseActivity;
import com.tiredcity.app.utils.PriceUtils;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import java.util.List;

import com.google.firebase.firestore.FirebaseFirestore;
import com.tiredcity.app.data.model.Product;
import com.tiredcity.app.data.model.UserProfile;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

public class PaymentActivity extends BaseActivity {

    private ActivityPaymentBinding binding;
    private OrderRepository orderRepository;
    private CartLocalStore cartLocalStore;

    /** Phí ship mặc định khi admin chưa cấu hình đơn vị vận chuyển nào. */
    private static final double DEFAULT_SHIPPING_FEE = 30_000;
    private double shippingFee = DEFAULT_SHIPPING_FEE;
    private String shippingMethodName = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityPaymentBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        binding.toolbar.setNavigationOnClickListener(v -> finish());

        orderRepository = new OrderRepository(ApiClient.getApiService(preferenceManager.getToken()));
        cartLocalStore  = new CartLocalStore(this);

        setupOrderSummary();
        loadShippingMethods();

        binding.btnPlaceOrder.setOnClickListener(v -> {
            String address = binding.tvRecipientAddress.getText().toString();
            String method  = getSelectedPaymentMethod();
            placeOrder(address, method);
        });
    }

    private void setupOrderSummary() {
        refreshTotals();

        // Prefill recipient info from saved profile
        com.tiredcity.app.data.model.UserProfile user = preferenceManager.getUser();
        if (user != null) {
            binding.tvRecipientName.setText(user.getName());
            binding.tvRecipientPhone.setText(user.getPhone());
            binding.tvRecipientAddress.setText(user.getAddress());
        }
    }

    private double currentSubtotal() {
        double subtotal = 0;
        List<CartItem> items = cartLocalStore.getCartItems();
        if (items != null) for (CartItem item : items) subtotal += item.getSubtotal();
        return subtotal;
    }

    private void refreshTotals() {
        double subtotal = currentSubtotal();
        binding.tvSubtotal.setText(PriceUtils.format(subtotal));
        binding.tvShippingFee.setText(shippingFee <= 0 ? getString(com.tiredcity.app.R.string.pay_shipping_free)
                : PriceUtils.format(shippingFee));
        binding.tvTotal.setText(PriceUtils.format(subtotal + shippingFee));
    }

    /** Đọc các phương thức vận chuyển admin cấu hình (collection "shipping") và cho khách chọn. */
    private void loadShippingMethods() {
        FirebaseFirestore.getInstance()
                .collection("shipping")
                .get()
                .addOnSuccessListener(snap -> {
                    binding.rgShippingMethod.removeAllViews();
                    List<Map<String, Object>> methods = new ArrayList<>();
                    for (com.google.firebase.firestore.QueryDocumentSnapshot d : snap) {
                        methods.add(d.getData());
                    }
                    if (methods.isEmpty()) {
                        // Không có cấu hình → giữ mức mặc định, ẩn danh sách
                        binding.tvShippingLoading.setVisibility(View.GONE);
                        binding.rgShippingMethod.setVisibility(View.GONE);
                        shippingFee = DEFAULT_SHIPPING_FEE;
                        shippingMethodName = "";
                        refreshTotals();
                        return;
                    }
                    // Sắp xếp theo phí tăng dần cho khớp trang admin
                    methods.sort((a, b) -> Double.compare(feeOf(a), feeOf(b)));

                    binding.tvShippingLoading.setVisibility(View.GONE);
                    binding.rgShippingMethod.setVisibility(View.VISIBLE);
                    for (int i = 0; i < methods.size(); i++) {
                        Map<String, Object> m = methods.get(i);
                        double fee = feeOf(m);
                        String name = m.get("name") != null ? String.valueOf(m.get("name")) : "";
                        String eta = m.get("estimatedTime") != null ? String.valueOf(m.get("estimatedTime")) : "";

                        RadioButton rb = new RadioButton(this);
                        rb.setId(View.generateViewId());
                        StringBuilder label = new StringBuilder(name);
                        label.append("  •  ").append(fee <= 0
                                ? getString(com.tiredcity.app.R.string.pay_shipping_free) : PriceUtils.format(fee));
                        if (!eta.isEmpty()) label.append("  •  ").append(eta);
                        rb.setText(label.toString());
                        rb.setTextColor(getColor(com.tiredcity.app.R.color.text_primary));
                        rb.setTextSize(14);
                        rb.setButtonTintList(android.content.res.ColorStateList.valueOf(
                                getColor(com.tiredcity.app.R.color.tc_red)));
                        rb.setTag(fee);
                        rb.setTag(com.tiredcity.app.R.id.rg_shipping_method, name);
                        binding.rgShippingMethod.addView(rb);
                    }

                    binding.rgShippingMethod.setOnCheckedChangeListener((group, checkedId) -> {
                        RadioButton rb = group.findViewById(checkedId);
                        if (rb == null) return;
                        shippingFee = rb.getTag() instanceof Double ? (Double) rb.getTag() : DEFAULT_SHIPPING_FEE;
                        Object nameTag = rb.getTag(com.tiredcity.app.R.id.rg_shipping_method);
                        shippingMethodName = nameTag != null ? String.valueOf(nameTag) : "";
                        refreshTotals();
                    });

                    // Chọn phương thức rẻ nhất mặc định
                    ((RadioButton) binding.rgShippingMethod.getChildAt(0)).setChecked(true);
                })
                .addOnFailureListener(e -> {
                    binding.tvShippingLoading.setVisibility(View.GONE);
                    binding.rgShippingMethod.setVisibility(View.GONE);
                    shippingFee = DEFAULT_SHIPPING_FEE;
                    shippingMethodName = "";
                    refreshTotals();
                });
    }

    private double feeOf(Map<String, Object> m) {
        Object f = m.get("fee");
        if (f instanceof Number) return ((Number) f).doubleValue();
        try { return f != null ? Double.parseDouble(String.valueOf(f)) : 0; }
        catch (NumberFormatException ex) { return 0; }
    }

    private String getSelectedPaymentMethod() {
        int id = binding.rgPaymentMethod.getCheckedRadioButtonId();
        if (id == binding.rbBankTransfer.getId()) return "BANK_TRANSFER";
        if (id == binding.rbMomo.getId())         return "MOMO";
        return "COD";
    }

    /**
     * Ghi don hang thang vao Cloud Firestore (collection "orders") dung dinh dang
     * ma web-admin Angular doc: status/items/subTotal/shippingFee/totalPrice...
     */
    private void placeOrder(String address, String paymentMethod) {
        binding.btnPlaceOrder.setEnabled(false);
        List<CartItem> items = cartLocalStore.getCartItems();

        if (items == null || items.isEmpty()) {
            binding.btnPlaceOrder.setEnabled(true);
            Toast.makeText(this, getString(com.tiredcity.app.R.string.error_order_failed), Toast.LENGTH_SHORT).show();
            return;
        }

        // Dung item + subtotal
        double subtotal = 0;
        List<Map<String, Object>> itemList = new ArrayList<>();
        for (CartItem item : items) {
            Product p = item.getProduct();
            double lineTotal = item.getSubtotal();
            subtotal += lineTotal;

            Map<String, Object> m = new HashMap<>();
            m.put("product_name", p != null ? p.getName() : "");
            m.put("productId", p != null ? p.getId() : null);
            m.put("price", p != null ? p.getEffectivePrice() : 0);
            m.put("quantity", item.getQuantity());
            m.put("size", item.getSelectedSize());
            m.put("color", item.getSelectedColor());
            m.put("image", p != null ? p.getFirstImage() : null);
            m.put("lineTotal", lineTotal);
            itemList.add(m);
        }
        double total = subtotal + shippingFee;

        UserProfile user = preferenceManager.getUser();

        // Thoi gian tao dang ISO-8601 (UTC) cho khop web
        SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US);
        fmt.setTimeZone(TimeZone.getTimeZone("UTC"));

        Map<String, Object> order = new HashMap<>();
        order.put("userId", user != null ? user.getId() : null);
        order.put("user", user != null ? user.getId() : null);
        order.put("userName", user != null ? user.getName() : "");
        order.put("userEmail", user != null ? user.getEmail() : "");
        order.put("phone", user != null ? user.getPhone() : "");
        order.put("items", itemList);
        order.put("orderItems", itemList);
        order.put("subTotal", subtotal);
        order.put("shippingFee", shippingFee);
        order.put("shippingMethod", shippingMethodName);
        order.put("totalPrice", total);
        order.put("status", "pending");
        order.put("paymentMethod", paymentMethod);
        order.put("shippingAddress", address);
        order.put("isPaid", false);
        order.put("createdAt", fmt.format(new Date()));

        FirebaseFirestore.getInstance()
                .collection("orders")
                .add(order)
                .addOnSuccessListener(ref -> {
                    cartLocalStore.clearCart();
                    Intent intent = new Intent(PaymentActivity.this, OrderSuccessActivity.class);
                    intent.putExtra("order_id", ref.getId());
                    startActivity(intent);
                    finish();
                })
                .addOnFailureListener(e -> {
                    binding.btnPlaceOrder.setEnabled(true);
                    Toast.makeText(PaymentActivity.this,
                            getString(com.tiredcity.app.R.string.error_order_failed) + ": " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                });
    }
}
