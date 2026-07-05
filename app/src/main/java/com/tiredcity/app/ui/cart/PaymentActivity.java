package com.tiredcity.app.ui.cart;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.tiredcity.app.utils.AddressData;
import com.tiredcity.app.adapter.CheckoutItemAdapter;
import com.tiredcity.app.data.local.CartLocalStore;
import com.tiredcity.app.data.model.CartItem;
import com.tiredcity.app.data.model.ShippingOption;
import com.tiredcity.app.data.network.ApiClient;
import com.tiredcity.app.data.repository.OrderRepository;
import com.tiredcity.app.databinding.ActivityPaymentBinding;
import com.tiredcity.app.databinding.DialogEditRecipientBinding;
import com.tiredcity.app.databinding.DialogVoucherCodeBinding;
import com.tiredcity.app.ui.base.BaseActivity;
import com.tiredcity.app.utils.CheckoutPriceCalculator;
import com.tiredcity.app.utils.PriceUtils;
import java.util.List;

import com.google.firebase.firestore.DocumentSnapshot;
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
    private final CheckoutPriceCalculator priceCalculator = new CheckoutPriceCalculator();

    /** Phí ship mặc định khi chưa tải được cấu hình SPX Express (mất mạng...). */
    private static final double DEFAULT_SHIPPING_FEE = 30_000;
    private static final String SHIPPING_COLLECTION = "shipping_configs";
    private static final String[] SHIPPING_ORDER = {"economy", "standard", "express"};
    private static final String DEFAULT_SHIPPING_ID = "standard";

    private String shippingMethodName = "";
    private final List<ShippingOption> activeShippingOptions = new ArrayList<>();
    private ShippingOption selectedShippingOption;

    /** Phương thức thanh toán đang chọn — mặc định MoMo (đã bỏ COD). */
    private String selectedPaymentMethod = "MOMO";

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
        AddressData.init(this);

        priceCalculator.setShippingFee(DEFAULT_SHIPPING_FEE);

        setupOrderSummary();
        setupOrderItems();
        setupPaymentSelector();
        loadShippingMethods();

        binding.rowVoucher.setOnClickListener(v -> showVoucherDialog());
        binding.tvChangeAddress.setOnClickListener(v -> showEditRecipientDialog());
        binding.rowAddress.setOnClickListener(v -> showEditRecipientDialog());
        binding.rowShippingMethod.setOnClickListener(v -> showShippingMethodSheet());

        binding.btnPlaceOrder.setOnClickListener(v -> {
            UserProfile user = preferenceManager.getUser();
            String address = user != null ? user.getFullAddress() : "";
            if (TextUtils.isEmpty(address)) {
                Toast.makeText(this, com.tiredcity.app.R.string.pay_address_required, Toast.LENGTH_SHORT).show();
                showEditRecipientDialog();
                return;
            }
            showSlideCaptcha(() -> placeOrder(address, selectedPaymentMethod));
        });
    }

    private void setupOrderSummary() {
        bindRecipientCard();
        refreshTotals();
    }

    /** Đổ thông tin người nhận + địa chỉ đầy đủ vào card; hiện trạng thái trống nếu chưa có địa chỉ. */
    private void bindRecipientCard() {
        UserProfile user = preferenceManager.getUser();
        String name = user != null ? user.getName() : "";
        String phone = user != null ? user.getPhone() : "";
        String fullAddress = user != null ? user.getFullAddress() : "";

        binding.tvRecipientName.setText(name);
        binding.tvRecipientPhone.setText(phone);

        boolean hasAddress = !TextUtils.isEmpty(fullAddress);
        binding.tvRecipientAddress.setVisibility(hasAddress ? View.VISIBLE : View.GONE);
        binding.tvAddressEmpty.setVisibility(hasAddress ? View.GONE : View.VISIBLE);
        if (hasAddress) binding.tvRecipientAddress.setText(fullAddress);
    }

    private void setupOrderItems() {
        List<CartItem> items = selectedItems();
        binding.rvOrderItems.setLayoutManager(new LinearLayoutManager(this));
        binding.rvOrderItems.setAdapter(new CheckoutItemAdapter(items));
    }

    /** Chỉ những sản phẩm được tick chọn ở màn Giỏ hàng mới được đưa vào thanh toán. */
    private List<CartItem> selectedItems() {
        List<CartItem> result = new ArrayList<>();
        List<CartItem> items = cartLocalStore.getCartItems();
        if (items != null) for (CartItem item : items) if (item.isSelected()) result.add(item);
        return result;
    }

    private double currentSubtotal() {
        double subtotal = 0;
        for (CartItem item : selectedItems()) subtotal += item.getSubtotal();
        return subtotal;
    }

    private void refreshTotals() {
        priceCalculator.setSubtotal(currentSubtotal());

        binding.tvSubtotal.setText(PriceUtils.format(priceCalculator.getSubtotal()));

        double effectiveShipping = priceCalculator.getEffectiveShippingFee();
        binding.tvShippingFee.setText(effectiveShipping <= 0
                ? getString(com.tiredcity.app.R.string.pay_shipping_free)
                : PriceUtils.format(effectiveShipping));

        double discount = priceCalculator.getDiscountAmount();
        binding.rowDiscount.setVisibility(discount > 0 ? View.VISIBLE : View.GONE);
        if (discount > 0) {
            binding.tvDiscount.setText("-" + PriceUtils.format(discount));
        }

        binding.tvTotal.setText(PriceUtils.format(priceCalculator.getTotal()));
    }

    /** Đọc 3 gói SPX Express (collection "shipping_configs") đang isActive=true và cho khách chọn. */
    private void loadShippingMethods() {
        FirebaseFirestore.getInstance()
                .collection(SHIPPING_COLLECTION)
                .get()
                .addOnSuccessListener(snap -> {
                    Map<String, DocumentSnapshot> byId = new HashMap<>();
                    for (com.google.firebase.firestore.QueryDocumentSnapshot d : snap) byId.put(d.getId(), d);

                    activeShippingOptions.clear();
                    for (String id : SHIPPING_ORDER) {
                        DocumentSnapshot d = byId.get(id);
                        if (d == null || !Boolean.TRUE.equals(d.getBoolean("isActive"))) continue;
                        String name = d.getString("name");
                        String estimate = d.getString("estimate");
                        Double price = d.getDouble("price");
                        activeShippingOptions.add(new ShippingOption(
                                id, name != null ? name : "", price != null ? price : 0,
                                estimate != null ? estimate : ""));
                    }

                    if (activeShippingOptions.isEmpty()) {
                        applyFallbackShipping();
                        return;
                    }

                    binding.tvShippingLoading.setVisibility(View.GONE);
                    binding.tvSpxPackage.setVisibility(View.VISIBLE);

                    ShippingOption initial = findShippingOptionById(DEFAULT_SHIPPING_ID);
                    if (initial == null) initial = activeShippingOptions.get(0);
                    selectShippingOption(initial);
                })
                .addOnFailureListener(e -> applyFallbackShipping());
    }

    /** Không tải được cấu hình từ Firestore → dùng gói "SPX Cơ bản" mặc định. */
    private void applyFallbackShipping() {
        activeShippingOptions.clear();
        ShippingOption fallback = new ShippingOption(DEFAULT_SHIPPING_ID, "SPX Cơ bản", DEFAULT_SHIPPING_FEE, "");
        activeShippingOptions.add(fallback);
        binding.tvShippingLoading.setVisibility(View.GONE);
        binding.tvSpxPackage.setVisibility(View.VISIBLE);
        selectShippingOption(fallback);
    }

    private ShippingOption findShippingOptionById(String id) {
        for (ShippingOption o : activeShippingOptions) if (o.id.equals(id)) return o;
        return null;
    }

    private void selectShippingOption(ShippingOption option) {
        selectedShippingOption = option;
        shippingMethodName = option.name;
        priceCalculator.setShippingFee(option.price);

        String priceText = option.price <= 0
                ? getString(com.tiredcity.app.R.string.pay_shipping_free) : PriceUtils.format(option.price);
        binding.tvSpxPackage.setText(option.name + "  •  " + priceText);

        if (option.estimate != null && !option.estimate.isEmpty()) {
            binding.tvShippingEta.setVisibility(View.VISIBLE);
            binding.tvShippingEta.setText(
                    getString(com.tiredcity.app.R.string.pay_shipping_eta, deliveryEtaText(option.estimate)));
        } else {
            binding.tvShippingEta.setVisibility(View.GONE);
        }
        refreshTotals();
    }

    /**
     * Chuyển "estimate" của gói thành ngày dự kiến nhận hàng.
     * Nếu tính theo ngày (vd "3-5 ngày") thì cộng số ngày lớn nhất vào hôm nay và trả về dd/MM/yyyy;
     * nếu theo giờ (vd "2 giờ") thì hiển thị "hôm nay". Không đọc được thì giữ nguyên chuỗi gốc.
     */
    private String deliveryEtaText(String estimate) {
        String lower = estimate.toLowerCase(Locale.ROOT);
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("(\\d+)").matcher(lower);
        int lastNumber = -1;
        while (m.find()) {
            try { lastNumber = Integer.parseInt(m.group(1)); } catch (NumberFormatException ignored) {}
        }
        if (lower.contains("giờ") || lower.contains("gio")) {
            return "hôm nay (" + estimate + ")";
        }
        if (lastNumber >= 0 && (lower.contains("ngày") || lower.contains("ngay"))) {
            java.util.Calendar cal = java.util.Calendar.getInstance();
            cal.add(java.util.Calendar.DAY_OF_YEAR, lastNumber);
            return new SimpleDateFormat("dd/MM/yyyy", Locale.US).format(cal.getTime());
        }
        return estimate;
    }

    /** Mở bottom sheet cho khách chọn lại 1 trong các gói SPX Express đang bật. */
    private void showShippingMethodSheet() {
        if (activeShippingOptions.isEmpty()) return;
        String selectedId = selectedShippingOption != null ? selectedShippingOption.id : DEFAULT_SHIPPING_ID;
        ShippingMethodBottomSheet sheet = ShippingMethodBottomSheet.newInstance(
                new ArrayList<>(activeShippingOptions), selectedId);
        sheet.setOnShippingSelectedListener(this::selectShippingOption);
        sheet.show(getSupportFragmentManager(), "shipping_method");
    }

    /** Hiện Slider Captcha (kéo mảnh ghép); chỉ khi khớp mới cho đặt hàng. */
    private void showSlideCaptcha(Runnable onVerified) {
        SlideCaptchaBottomSheet sheet = new SlideCaptchaBottomSheet();
        sheet.setOnVerifiedListener(onVerified::run);
        sheet.show(getSupportFragmentManager(), "slide_captcha");
    }

    /** Hai lựa chọn thanh toán dạng card có hiệu ứng viền + dấu tick khi được chọn. */
    private void setupPaymentSelector() {
        binding.optMomo.setOnClickListener(v -> setPaymentMethod("MOMO"));
        binding.optBank.setOnClickListener(v -> setPaymentMethod("BANK_TRANSFER"));
        setPaymentMethod(selectedPaymentMethod);
    }

    private void setPaymentMethod(String method) {
        selectedPaymentMethod = method;
        boolean momo = "MOMO".equals(method);
        binding.optMomo.setSelected(momo);
        binding.optBank.setSelected(!momo);
        binding.checkMomo.setVisibility(momo ? View.VISIBLE : View.INVISIBLE);
        binding.checkBank.setVisibility(momo ? View.INVISIBLE : View.VISIBLE);
    }

    private void updateVoucherStatusLabel() {
        String code = priceCalculator.getAppliedVoucherCode();
        if (code != null) {
            binding.tvVoucherStatus.setText(getString(com.tiredcity.app.R.string.pay_voucher_applied, code));
        } else {
            binding.tvVoucherStatus.setText(com.tiredcity.app.R.string.pay_voucher_hint);
        }
    }

    private void showVoucherDialog() {
        DialogVoucherCodeBinding dialogBinding = DialogVoucherCodeBinding.inflate(getLayoutInflater());
        String currentCode = priceCalculator.getAppliedVoucherCode();
        if (currentCode != null) dialogBinding.etVoucherCode.setText(currentCode);

        AlertDialog.Builder builder = new AlertDialog.Builder(this)
                .setTitle(com.tiredcity.app.R.string.pay_voucher_dialog_title)
                .setView(dialogBinding.getRoot())
                .setPositiveButton(com.tiredcity.app.R.string.btn_save, (dialog, which) -> {
                    String code = dialogBinding.etVoucherCode.getText().toString().trim().toUpperCase();
                    if (code.isEmpty()) {
                        priceCalculator.clearVoucher();
                        updateVoucherStatusLabel();
                        refreshTotals();
                    } else {
                        // Liên kết thật với Firebase Vouchers
                        FirebaseFirestore.getInstance().collection("vouchers").document(code).get()
                            .addOnSuccessListener(doc -> {
                                if (doc.exists() && Boolean.TRUE.equals(doc.getBoolean("isActive"))) {
                                    String type = doc.getString("type"); // PERCENT, FLAT, FREE_SHIP
                                    double value = doc.getDouble("value") != null ? doc.getDouble("value") : 0;
                                    
                                    // Áp dụng vào PriceCalculator
                                    if (priceCalculator.applyVoucher(code, type, value)) {
                                        Toast.makeText(this, "Áp dụng mã thành công!", Toast.LENGTH_SHORT).show();
                                    } else {
                                        Toast.makeText(this, "Mã không hợp lệ cho đơn hàng này", Toast.LENGTH_SHORT).show();
                                    }
                                } else {
                                    Toast.makeText(this, "Mã giảm giá không tồn tại hoặc đã hết hạn", Toast.LENGTH_SHORT).show();
                                    priceCalculator.clearVoucher();
                                }
                                updateVoucherStatusLabel();
                                refreshTotals();
                            });
                    }
                })
                .setNegativeButton(com.tiredcity.app.R.string.btn_cancel, null);

        if (currentCode != null) {
            builder.setNeutralButton(com.tiredcity.app.R.string.btn_remove_short, (dialog, which) -> {
                priceCalculator.clearVoucher();
                updateVoucherStatusLabel();
                refreshTotals();
                Toast.makeText(this, com.tiredcity.app.R.string.pay_voucher_removed, Toast.LENGTH_SHORT).show();
            });
        }
        builder.show();
    }

    private void showEditRecipientDialog() {
        DialogEditRecipientBinding dialogBinding = DialogEditRecipientBinding.inflate(getLayoutInflater());
        UserProfile user = preferenceManager.getUser();

        // Prefill từ hồ sơ đã lưu
        if (user != null) {
            dialogBinding.etRecipientName.setText(user.getName());
            dialogBinding.etRecipientPhone.setText(user.getPhone());
            dialogBinding.etRecipientAddress.setText(user.getStreet());
        }

        // Dropdown địa chỉ 3 cấp: Tỉnh → Quận/Huyện → Phường/Xã (tái dùng AddressData)
        AutoCompleteTextView actProvince = dialogBinding.actProvince;
        AutoCompleteTextView actDistrict = dialogBinding.actDistrict;
        AutoCompleteTextView actWard = dialogBinding.actWard;

        setDropdown(actProvince, new ArrayList<>(java.util.Arrays.asList(
                getResources().getStringArray(com.tiredcity.app.R.array.vn_provinces))));

        actProvince.setOnItemClickListener((parent, v, pos, id) -> {
            String prov = actProvince.getText().toString();
            setDropdown(actDistrict, AddressData.getDistricts(prov));
            actDistrict.setText("", false);
            setDropdown(actWard, new ArrayList<>());
            actWard.setText("", false);
        });
        actDistrict.setOnItemClickListener((parent, v, pos, id) -> {
            String prov = actProvince.getText().toString();
            String dist = actDistrict.getText().toString();
            setDropdown(actWard, AddressData.getWards(prov, dist));
            actWard.setText("", false);
        });

        if (user != null) {
            actProvince.setText(user.getProvince(), false);
            setDropdown(actDistrict, AddressData.getDistricts(user.getProvince()));
            actDistrict.setText(user.getDistrict(), false);
            setDropdown(actWard, AddressData.getWards(user.getProvince(), user.getDistrict()));
            actWard.setText(user.getWard(), false);
        }

        new AlertDialog.Builder(this)
                .setTitle(com.tiredcity.app.R.string.pay_edit_recipient_title)
                .setView(dialogBinding.getRoot())
                .setPositiveButton(com.tiredcity.app.R.string.btn_save, (dialog, which) -> {
                    UserProfile p = preferenceManager.getUser();
                    if (p == null) p = new UserProfile();
                    p.setName(dialogBinding.etRecipientName.getText().toString().trim());
                    p.setPhone(dialogBinding.etRecipientPhone.getText().toString().trim());
                    p.setProvince(actProvince.getText().toString().trim());
                    p.setDistrict(actDistrict.getText().toString().trim());
                    p.setWard(actWard.getText().toString().trim());
                    p.setStreet(dialogBinding.etRecipientAddress.getText().toString().trim());
                    p.setAddress(p.getFullAddress());
                    preferenceManager.saveUser(p);
                    bindRecipientCard();
                })
                .setNegativeButton(com.tiredcity.app.R.string.btn_cancel, null)
                .show();
    }

    private void setDropdown(AutoCompleteTextView view, List<String> items) {
        view.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, items));
    }

    /**
     * Ghi don hang thang vao Cloud Firestore (collection "orders") dung dinh dang
     * ma web-admin Angular doc: status/items/subTotal/shippingFee/totalPrice...
     */
    private void placeOrder(String address, String paymentMethod) {
        binding.btnPlaceOrder.setEnabled(false);
        List<CartItem> items = selectedItems();

        if (items == null || items.isEmpty()) {
            binding.btnPlaceOrder.setEnabled(true);
            Toast.makeText(this, getString(com.tiredcity.app.R.string.error_order_failed), Toast.LENGTH_SHORT).show();
            return;
        }

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        
        // 1. Generate Human-readable Order ID
        String datePrefix = new SimpleDateFormat("yyMMdd", Locale.US).format(new Date());
        String randomSuffix = String.format("%04d", (int)(Math.random() * 10000));
        String orderCode = "TC-" + datePrefix + "-" + randomSuffix;

        // 2. Prepare Items & Subtotal
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

        double shippingFee = priceCalculator.getEffectiveShippingFee();
        double discount = priceCalculator.getDiscountAmount();
        double total = priceCalculator.getTotal();

        String currentUid = preferenceManager.getUserId();
        if (currentUid == null || currentUid.isEmpty()) {
            // Thử lấy từ Firebase Auth trực tiếp
            com.google.firebase.auth.FirebaseUser fUser = com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser();
            if (fUser != null) {
                currentUid = fUser.getUid();
                preferenceManager.saveUserId(currentUid);
            }
        }
        
        if (currentUid == null || currentUid.isEmpty()) {
            binding.btnPlaceOrder.setEnabled(true);
            Toast.makeText(this, "Vui lòng đăng nhập để đặt hàng!", Toast.LENGTH_SHORT).show();
            return;
        }

        UserProfile user = preferenceManager.getUser();

        // 3. Final Order Object
        Map<String, Object> orderData = new HashMap<>();
        orderData.put("orderCode", orderCode);
        orderData.put("userId", currentUid);
        orderData.put("user", currentUid);
        orderData.put("userName", user != null ? user.getName() : "");
        orderData.put("userEmail", user != null ? user.getEmail() : "");
        orderData.put("phone", user != null ? user.getPhone() : "");
        orderData.put("items", itemList);
        orderData.put("orderItems", itemList);
        orderData.put("subTotal", subtotal);
        orderData.put("shippingFee", shippingFee);
        orderData.put("shippingMethod", shippingMethodName);
        orderData.put("discountAmount", discount);
        orderData.put("voucherCode", priceCalculator.getAppliedVoucherCode());
        orderData.put("totalPrice", total);
        orderData.put("status", "PENDING");
        orderData.put("paymentMethod", paymentMethod);
        orderData.put("shippingAddress", address);
        orderData.put("isPaid", false);
        orderData.put("createdAt", com.google.firebase.Timestamp.now()); 
        orderData.put("updatedAt", com.google.firebase.Timestamp.now());

        // 4. Use Transaction to decrement stock
        db.runTransaction(transaction -> {
            // Gom nhóm updates theo từng sản phẩm để tránh ghi đè
            Map<String, Map<String, Object>> productUpdates = new HashMap<>();
            
            for (CartItem item : items) {
                if (item.getProduct() != null) {
                    String rawId = item.getProduct().getId();
                    if (rawId == null || rawId.isEmpty()) {
                        throw new RuntimeException("Sản phẩm " + item.getProduct().getName() + " bị thiếu mã định danh!");
                    }
                    
                    // 1. NORMALIZE ID: "ad1" -> "AD01", "at2" -> "AT02"
                    String pId = rawId.trim().toUpperCase();
                    if (pId.matches("^[A-Z]{2}\\d$")) {
                        pId = pId.substring(0, 2) + "0" + pId.substring(2);
                    }
                    
                    // FALLBACK: If normalization to AD01 didn't find it, maybe the database uses AD1
                    com.google.firebase.firestore.DocumentReference pRef = db.collection("products").document(pId);
                    DocumentSnapshot pSnap = transaction.get(pRef);
                    
                    if (!pSnap.exists()) {
                        // Thử lại với mã gốc viết hoa (AD1)
                        pId = rawId.trim().toUpperCase();
                        pRef = db.collection("products").document(pId);
                        pSnap = transaction.get(pRef);
                    }

                    if (pSnap.exists()) {
                        Map<String, Object> currentProductUpdates = productUpdates.get(pId);
                        if (currentProductUpdates == null) {
                            currentProductUpdates = new HashMap<>();
                            productUpdates.put(pId, currentProductUpdates);
                        }

                        // A. Tính toán stock tổng mới
                        long currentTotalStock = pSnap.getLong("stock") != null ? pSnap.getLong("stock") : 0;
                        // Nếu sản phẩm này đã được trừ ở item trước trong cùng đơn hàng
                        if (currentProductUpdates.containsKey("stock")) {
                            currentTotalStock = (long) currentProductUpdates.get("stock");
                        }
                        
                        if (currentTotalStock < item.getQuantity()) {
                            throw new RuntimeException("Sản phẩm " + item.getProduct().getName() + " không đủ hàng!");
                        }
                        currentProductUpdates.put("stock", currentTotalStock - item.getQuantity());

                        // B. Tìm và trừ đúng Size
                        boolean sizeFound = false;
                        for (int i = 0; i <= 20; i++) {
                            String fieldName = String.valueOf(i);
                            Object obj = pSnap.get(fieldName);
                            if (obj instanceof Map) {
                                Map<String, Object> sizeInfo = new HashMap<>((Map<String, Object>) obj);
                                String sName = (String) sizeInfo.get("size");
                                
                                if (item.getSelectedSize() != null && item.getSelectedSize().equalsIgnoreCase(sName)) {
                                    long sQty = 0;
                                    Object qObj = sizeInfo.get("quantity");
                                    if (qObj instanceof Number) sQty = ((Number) qObj).longValue();

                                    if (sQty < item.getQuantity()) {
                                        throw new RuntimeException("Size " + sName + " của sản phẩm " + item.getProduct().getName() + " đã hết!");
                                    }
                                    
                                    sizeInfo.put("quantity", sQty - item.getQuantity());
                                    currentProductUpdates.put(fieldName, sizeInfo);
                                    sizeFound = true;
                                    break;
                                }
                            }
                        }
                        
                        if (!sizeFound) {
                            // Fallback cho mảng "sizes"
                            List<Map<String, Object>> sizes = (List<Map<String, Object>>) pSnap.get("sizes");
                            if (sizes != null) {
                                List<Map<String, Object>> newSizes = new ArrayList<>();
                                for (Map<String, Object> s : sizes) {
                                    Map<String, Object> sNew = new HashMap<>(s);
                                    String sName = (String) sNew.get("size");
                                    if (item.getSelectedSize() != null && item.getSelectedSize().equalsIgnoreCase(sName)) {
                                        long sQty = ((Number) sNew.get("quantity")).longValue();
                                        sNew.put("quantity", sQty - item.getQuantity());
                                        sizeFound = true;
                                    }
                                    newSizes.add(sNew);
                                }
                                if (sizeFound) currentProductUpdates.put("sizes", newSizes);
                            }
                        }
                        
                        if (!sizeFound) {
                            throw new RuntimeException("Không tìm thấy Size " + item.getSelectedSize() + " cho sản phẩm " + item.getProduct().getName());
                        }
                    } else {
                        throw new RuntimeException("Sản phẩm mã " + pId + " không tồn tại trên hệ thống!");
                    }
                }
            }
            
            // Thực thi toàn bộ updates cho sản phẩm
            for (Map.Entry<String, Map<String, Object>> entry : productUpdates.entrySet()) {
                transaction.update(db.collection("products").document(entry.getKey()), entry.getValue());
            }
            
            // Ghi đơn hàng
            com.google.firebase.firestore.DocumentReference orderRef = db.collection("orders").document();
            transaction.set(orderRef, orderData);
            return orderRef.getId();
            
        }).addOnSuccessListener(docId -> {
            for (CartItem item : items) {
                if (item.getProduct() != null) cartLocalStore.removeItem(item.getProduct().getId());
            }
            Intent intent = new Intent(PaymentActivity.this, OrderSuccessActivity.class);
            intent.putExtra("order_id", docId);
            intent.putExtra("order_code", orderCode);
            startActivity(intent);
            finish();
            
        }).addOnFailureListener(e -> {
            binding.btnPlaceOrder.setEnabled(true);
            String msg = e.getMessage();
            if (msg != null && (msg.contains("tồn kho") || msg.contains("hàng") || msg.contains("hết"))) {
                Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(this, "Đặt hàng thất bại: " + msg, Toast.LENGTH_SHORT).show();
            }
        });
    }
}
