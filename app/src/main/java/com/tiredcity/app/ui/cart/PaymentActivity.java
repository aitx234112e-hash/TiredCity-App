package com.tiredcity.app.ui.cart;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.TextView;
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
    /** Các gói THỰC SỰ hiển thị sau khi lọc theo địa chỉ (Hỏa Tốc chỉ giao nội thành Hà Nội). */
    private final List<ShippingOption> displayedShippingOptions = new ArrayList<>();
    private ShippingOption selectedShippingOption;
    /** Card vận chuyển đang mở rộng (hiện mọi gói) hay thu gọn (chỉ gói đang chọn). */
    private boolean shippingExpanded = true;

    /** Phương thức thanh toán đang chọn — mặc định MoMo. */
    private String selectedPaymentMethod = "MOMO";

    /** Adapter danh sách sản phẩm — giữ tham chiếu để bật/tắt "Xem tất cả". */
    private CheckoutItemAdapter checkoutItemAdapter;

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

        binding.rowVoucher.setOnClickListener(v -> showVoucherSheet());
        binding.tvChangeAddress.setOnClickListener(v -> showEditRecipientDialog());
        binding.rowAddress.setOnClickListener(v -> showEditRecipientDialog());
        binding.boxAddressAlert.setOnClickListener(v -> showEditRecipientDialog());

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

    /**
     * Đổ thông tin người nhận + địa chỉ đầy đủ vào card.
     * Chưa có địa chỉ → hiện hộp cảnh báo đỏ nhạt, ẩn card người nhận và ngược lại.
     */
    private void bindRecipientCard() {
        UserProfile user = preferenceManager.getUser();
        String name = user != null ? user.getName() : "";
        String phone = user != null ? user.getPhone() : "";
        String fullAddress = user != null ? user.getFullAddress() : "";

        boolean hasAddress = !TextUtils.isEmpty(fullAddress);
        binding.boxAddressAlert.setVisibility(hasAddress ? View.GONE : View.VISIBLE);
        binding.cardAddress.setVisibility(hasAddress ? View.VISIBLE : View.GONE);

        binding.tvRecipientName.setText(name);
        binding.tvRecipientPhone.setText(phone);
        if (hasAddress) binding.tvRecipientAddress.setText(fullAddress);
    }

    private void setupOrderItems() {
        List<CartItem> items = selectedItems();
        checkoutItemAdapter = new CheckoutItemAdapter(items);
        binding.rvOrderItems.setLayoutManager(new LinearLayoutManager(this));
        binding.rvOrderItems.setAdapter(checkoutItemAdapter);

        boolean collapsible = checkoutItemAdapter.isCollapsible();
        binding.tvSeeAll.setVisibility(collapsible ? View.VISIBLE : View.GONE);
        if (collapsible) {
            updateSeeAllLabel();
            binding.tvSeeAll.setOnClickListener(v -> {
                checkoutItemAdapter.setCollapsed(!checkoutItemAdapter.isCollapsed());
                updateSeeAllLabel();
            });
        }
    }

    /** Nhãn nút toggle: "Xem tất cả (n)" khi đang thu gọn, "Thu gọn" khi đã mở. */
    private void updateSeeAllLabel() {
        if (checkoutItemAdapter.isCollapsed()) {
            binding.tvSeeAll.setText(getString(
                    com.tiredcity.app.R.string.pay_see_all, checkoutItemAdapter.getTotalCount()));
        } else {
            binding.tvSeeAll.setText(com.tiredcity.app.R.string.pay_collapse);
        }
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

    /**
     * Tải cấu hình vận chuyển: đọc ngưỡng freeship (shipping_settings/general) trước,
     * sau đó mới tải các gói để giá hiển thị đúng trạng thái "Miễn Phí".
     */
    private void loadShippingMethods() {
        FirebaseFirestore.getInstance()
                .collection("shipping_settings").document("general")
                .get()
                .addOnSuccessListener(d -> {
                    Double threshold = d != null ? d.getDouble("freeshipThreshold") : null;
                    priceCalculator.setFreeshipThreshold(threshold != null ? threshold : 0);
                    loadShippingConfigs();
                })
                .addOnFailureListener(e -> loadShippingConfigs());
    }

    /** Đọc 3 gói vận chuyển (collection "shipping_configs") đang isActive=true và cho khách chọn. */
    private void loadShippingConfigs() {
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
                        Double original = d.getDouble("originalPrice");
                        activeShippingOptions.add(new ShippingOption(
                                id, name != null ? name : "", price != null ? price : 0,
                                original != null ? original : 0,
                                estimate != null ? estimate : ""));
                    }

                    if (activeShippingOptions.isEmpty()) {
                        applyFallbackShipping();
                        return;
                    }

                    applyShippingForAddress();
                })
                .addOnFailureListener(e -> applyFallbackShipping());
    }

    /**
     * Chưa cấu hình gói trong Firestore (shipping_configs rỗng / mất mạng) → dùng bộ gói
     * mặc định kiểu Shopee: "Nhanh" MIỄN PHÍ (gạch giá gốc 16.500đ) + "Hỏa Tốc" 92.100đ (pill xanh).
     * Admin cấu hình shipping_configs sẽ ghi đè bộ này.
     */
    private void applyFallbackShipping() {
        activeShippingOptions.clear();
        activeShippingOptions.add(new ShippingOption("standard", "", 0, 16_500, "1-2 ngày"));
        activeShippingOptions.add(new ShippingOption("express", "", 92_100, 0, "2 giờ"));
        applyShippingForAddress();
    }

    /**
     * Lọc & hiển thị gói vận chuyển theo địa chỉ khách:
     * - Chưa có địa chỉ → hiện lời nhắc, ẩn danh sách gói (chưa biết giao ở đâu).
     * - Hà Nội → đủ gói (gồm Hỏa Tốc). Tỉnh/thành khác → bỏ Hỏa Tốc, chỉ có Nhanh.
     * Gọi lại mỗi khi khách cập nhật địa chỉ (không cần tải lại Firestore).
     */
    private void applyShippingForAddress() {
        if (!hasShippingAddress()) {
            binding.tvShippingLoading.setVisibility(View.VISIBLE);
            binding.tvShippingLoading.setText(com.tiredcity.app.R.string.pay_shipping_need_address);
            binding.containerShippingOptions.setVisibility(View.GONE);
            binding.tvShippingInspection.setVisibility(View.GONE);
            binding.tvShippingSeeAll.setVisibility(View.GONE);
            selectedShippingOption = null;
            priceCalculator.setShippingFee(0);
            refreshTotals();
            return;
        }

        boolean hanoi = isHanoiProvince();
        displayedShippingOptions.clear();
        for (ShippingOption o : activeShippingOptions) {
            if ("express".equals(o.id) && !hanoi) continue;   // Hỏa Tốc chỉ giao nội thành Hà Nội
            displayedShippingOptions.add(o);
        }

        binding.containerShippingOptions.setVisibility(View.VISIBLE);
        renderShippingOptions();

        ShippingOption initial = findShippingOptionById(DEFAULT_SHIPPING_ID);
        if (initial == null && !displayedShippingOptions.isEmpty()) initial = displayedShippingOptions.get(0);
        if (initial != null) selectShippingOption(initial);
    }

    private boolean hasShippingAddress() {
        UserProfile user = preferenceManager.getUser();
        return user != null && !TextUtils.isEmpty(user.getFullAddress());
    }

    /** Địa chỉ khách thuộc Hà Nội? Chuẩn hoá bỏ dấu để khớp "Hà Nội", "TP. Hà Nội"... */
    private boolean isHanoiProvince() {
        UserProfile user = preferenceManager.getUser();
        String province = user != null ? user.getProvince() : "";
        return noAccent(province).contains("ha noi");
    }

    /** Tìm gói theo id TRONG danh sách đang hiển thị (đã lọc theo địa chỉ). */
    private ShippingOption findShippingOptionById(String id) {
        for (ShippingOption o : displayedShippingOptions) if (o.id.equals(id)) return o;
        return null;
    }

    /** Bỏ dấu tiếng Việt + thường hoá để so khớp tên tỉnh/thành. */
    private static String noAccent(String s) {
        if (s == null) return "";
        String n = java.text.Normalizer.normalize(s, java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "")
                .replace('đ', 'd').replace('Đ', 'D');
        return n.toLowerCase(Locale.ROOT).trim();
    }

    /**
     * Tên gói hiển thị cho khách: chỉ nói tốc độ (Tiết kiệm / Nhanh / Hỏa Tốc),
     * không lộ đơn vị vận chuyển. Gói lạ thì dùng tên admin đặt, bỏ tiền tố "SPX".
     */
    private String shippingDisplayName(ShippingOption option) {
        switch (option.id) {
            case "economy":  return getString(com.tiredcity.app.R.string.pay_ship_economy);
            case "express":  return getString(com.tiredcity.app.R.string.pay_ship_express);
            case "standard": return getString(com.tiredcity.app.R.string.pay_ship_standard);
            default:
                return option.name != null ? option.name.replaceFirst("(?i)^SPX\\s*", "") : "";
        }
    }

    /** Vẽ các gói vận chuyển inline trong card — bấm ô nào chọn ô đó, không cần bottom sheet. */
    private void renderShippingOptions() {
        binding.tvShippingLoading.setVisibility(View.GONE);
        binding.containerShippingOptions.removeAllViews();
        boolean freeByThreshold = priceCalculator.isFreeshipByThreshold();

        for (ShippingOption option : displayedShippingOptions) {
            View root = getLayoutInflater().inflate(com.tiredcity.app.R.layout.item_shipping_option,
                    binding.containerShippingOptions, false);

            TextView tvName = root.findViewById(com.tiredcity.app.R.id.tv_option_name);
            TextView tvPrice = root.findViewById(com.tiredcity.app.R.id.tv_option_price);
            TextView tvOriginal = root.findViewById(com.tiredcity.app.R.id.tv_option_price_original);
            View ivVoucher = root.findViewById(com.tiredcity.app.R.id.iv_freeship_voucher);
            TextView tvEta = root.findViewById(com.tiredcity.app.R.id.tv_option_eta);
            View badge = root.findViewById(com.tiredcity.app.R.id.badge_eta);
            TextView tvBadge = root.findViewById(com.tiredcity.app.R.id.tv_option_eta_badge);

            tvName.setText(shippingDisplayName(option));

            // Giá: gói miễn phí (giá 0 hoặc đạt ngưỡng freeship) → gạch giá gốc + "Miễn Phí"
            // xanh ngọc + tem voucher. Giá gốc lấy từ ngưỡng freeship hoặc field originalPrice.
            if (option.price <= 0 || freeByThreshold) {
                tvPrice.setText(com.tiredcity.app.R.string.pay_ship_free);
                tvPrice.setTextColor(androidx.core.content.ContextCompat.getColor(
                        this, com.tiredcity.app.R.color.tc_ship_green));
                ivVoucher.setVisibility(View.VISIBLE);
                double original = freeByThreshold && option.price > 0 ? option.price : option.originalPrice;
                if (original > 0) {
                    tvOriginal.setVisibility(View.VISIBLE);
                    tvOriginal.setText(PriceUtils.format(original));
                    tvOriginal.setPaintFlags(
                            tvOriginal.getPaintFlags() | android.graphics.Paint.STRIKE_THRU_TEXT_FLAG);
                }
            } else {
                tvPrice.setText(PriceUtils.format(option.price));
                tvPrice.setTextColor(androidx.core.content.ContextCompat.getColor(
                        this, com.tiredcity.app.R.color.text_primary));
                ivVoucher.setVisibility(View.GONE);
            }

            // Thời gian nhận: theo ngày → "Nhận từ 7 Th07 - 8 Th07"; theo giờ → badge xanh
            String estimate = option.estimate != null ? option.estimate : "";
            if (estimate.isEmpty()) {
                tvEta.setVisibility(View.GONE);
            } else {
                String range = etaRangeText(estimate);
                if (range == null) {
                    tvEta.setText(com.tiredcity.app.R.string.pay_ship_eta_in);
                    badge.setVisibility(View.VISIBLE);
                    tvBadge.setText(estimate);
                } else {
                    tvEta.setText(range);
                }
            }

            root.setTag(option.id);
            root.findViewById(com.tiredcity.app.R.id.row_shipping_option)
                    .setOnClickListener(v -> selectShippingOption(option));
            binding.containerShippingOptions.addView(root);
        }

        binding.tvShippingInspection.setVisibility(View.VISIBLE);
        binding.tvShippingSeeAll.setVisibility(
                displayedShippingOptions.size() > 1 ? View.VISIBLE : View.GONE);
        binding.tvShippingSeeAll.setOnClickListener(v -> {
            shippingExpanded = !shippingExpanded;
            applyShippingCollapse();
        });
    }

    /** Tô trạng thái chọn: viền đỏ + ribbon tích góc cho ô đang chọn, các ô khác về mặc định. */
    private void updateShippingSelectionUi() {
        String selectedId = selectedShippingOption != null ? selectedShippingOption.id : null;
        for (int i = 0; i < binding.containerShippingOptions.getChildCount(); i++) {
            View root = binding.containerShippingOptions.getChildAt(i);
            boolean selected = root.getTag() != null && root.getTag().equals(selectedId);
            root.findViewById(com.tiredcity.app.R.id.row_shipping_option).setSelected(selected);
            root.findViewById(com.tiredcity.app.R.id.iv_option_ribbon)
                    .setVisibility(selected ? View.VISIBLE : View.GONE);
        }
        applyShippingCollapse();
    }

    /** Thu gọn: chỉ hiện gói đang chọn (kiểu Shopee); "Xem tất cả" mở toàn bộ để đổi gói. */
    private void applyShippingCollapse() {
        String selectedId = selectedShippingOption != null ? selectedShippingOption.id : null;
        for (int i = 0; i < binding.containerShippingOptions.getChildCount(); i++) {
            View root = binding.containerShippingOptions.getChildAt(i);
            boolean selected = root.getTag() != null && root.getTag().equals(selectedId);
            root.setVisibility(shippingExpanded || selected ? View.VISIBLE : View.GONE);
        }
        // Giữ nhãn "Xem tất cả >" cố định như thiết kế (mặc định đã mở đủ các gói).
        binding.tvShippingSeeAll.setText(com.tiredcity.app.R.string.pay_ship_see_all);
    }

    private void selectShippingOption(ShippingOption option) {
        selectedShippingOption = option;
        shippingMethodName = shippingDisplayName(option);
        priceCalculator.setShippingFee(option.price);
        updateShippingSelectionUi();
        refreshTotals();
    }

    /**
     * Chuyển "estimate" của gói thành khoảng ngày nhận kiểu Shopee.
     * "1-2 ngày" → "Nhận từ 6 Th07 - 7 Th07"; "3 ngày" → "Nhận vào 8 Th07".
     * Theo giờ (vd "2 giờ") → trả null để hiển thị badge xanh thay vì text.
     * Không đọc được thì giữ nguyên chuỗi gốc.
     */
    private String etaRangeText(String estimate) {
        String lower = estimate.toLowerCase(Locale.ROOT);
        if (lower.contains("giờ") || lower.contains("gio")) return null;

        java.util.regex.Matcher m = java.util.regex.Pattern.compile("(\\d+)").matcher(lower);
        int min = -1, max = -1;
        while (m.find()) {
            try {
                int n = Integer.parseInt(m.group(1));
                if (min < 0) min = n;
                max = n;
            } catch (NumberFormatException ignored) {}
        }
        if (min < 0 || !(lower.contains("ngày") || lower.contains("ngay"))) return estimate;

        SimpleDateFormat fmt = new SimpleDateFormat("d 'Th'MM", Locale.US);
        java.util.Calendar cal = java.util.Calendar.getInstance();
        cal.add(java.util.Calendar.DAY_OF_YEAR, min);
        String from = fmt.format(cal.getTime());
        if (max == min) {
            return getString(com.tiredcity.app.R.string.pay_ship_eta_on, from);
        }
        cal = java.util.Calendar.getInstance();
        cal.add(java.util.Calendar.DAY_OF_YEAR, max);
        return getString(com.tiredcity.app.R.string.pay_ship_eta_range, from, fmt.format(cal.getTime()));
    }

    /** Hiện Slider Captcha (kéo mảnh ghép); chỉ khi khớp mới cho đặt hàng. */
    private void showSlideCaptcha(Runnable onVerified) {
        SlideCaptchaBottomSheet sheet = new SlideCaptchaBottomSheet();
        sheet.setOnVerifiedListener(onVerified::run);
        sheet.show(getSupportFragmentManager(), "slide_captcha");
    }

    /** Lưới 2 cột 4 phương thức: ô được chọn có viền đỏ son + dấu tích ở góc. */
    private void setupPaymentSelector() {
        binding.optCod.setOnClickListener(v -> setPaymentMethod("COD"));
        binding.optBank.setOnClickListener(v -> setPaymentMethod("BANK_TRANSFER"));
        binding.optMomo.setOnClickListener(v -> setPaymentMethod("MOMO"));
        binding.optCard.setOnClickListener(v -> setPaymentMethod("CARD"));
        setPaymentMethod(selectedPaymentMethod);
    }

    private void setPaymentMethod(String method) {
        selectedPaymentMethod = method;
        applyPaymentCellState(binding.optCod, binding.checkCod, "COD".equals(method));
        applyPaymentCellState(binding.optBank, binding.checkBank, "BANK_TRANSFER".equals(method));
        applyPaymentCellState(binding.optMomo, binding.checkMomo, "MOMO".equals(method));
        applyPaymentCellState(binding.optCard, binding.checkCard, "CARD".equals(method));
    }

    private void applyPaymentCellState(View cell, View check, boolean selected) {
        cell.setSelected(selected);
        check.setVisibility(selected ? View.VISIBLE : View.GONE);
    }

    private void updateVoucherStatusLabel() {
        String code = priceCalculator.getAppliedVoucherCode();
        if (code != null) {
            binding.tvVoucherStatus.setText(getString(com.tiredcity.app.R.string.pay_voucher_applied, code));
        } else {
            binding.tvVoucherStatus.setText(com.tiredcity.app.R.string.pay_voucher_hint);
        }
    }

    /** Mở bottom sheet chọn mã giảm giá (kiểu Shopee) — chọn từ danh sách hoặc nhập tay. */
    private void showVoucherSheet() {
        VoucherBottomSheet sheet = new VoucherBottomSheet();
        sheet.setCurrentCode(priceCalculator.getAppliedVoucherCode());
        sheet.setOnVoucherAppliedListener(code -> {
            if (code == null || code.trim().isEmpty()) {
                boolean had = priceCalculator.getAppliedVoucherCode() != null;
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
                if (had) Toast.makeText(this,
                        com.tiredcity.app.R.string.pay_voucher_removed, Toast.LENGTH_SHORT).show();
            } else if (priceCalculator.applyVoucher(code)) {
                Toast.makeText(this, getString(
                        com.tiredcity.app.R.string.pay_voucher_applied_toast, code), Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this,
                        com.tiredcity.app.R.string.pay_voucher_invalid, Toast.LENGTH_SHORT).show();
            }
            updateVoucherStatusLabel();
            refreshTotals();
        });
        sheet.show(getSupportFragmentManager(), "voucher_sheet");
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

        // Header + nút Lưu/Hủy nằm trong layout → dựng dialog trần, nền trong suốt để lộ bo góc
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dialogBinding.getRoot())
                .create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        dialogBinding.btnRecipientCancel.setOnClickListener(v -> dialog.dismiss());
        dialogBinding.btnRecipientSave.setOnClickListener(v -> {
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
            applyShippingForAddress();   // địa chỉ đổi → lọc lại gói (Hỏa Tốc theo Hà Nội)
            dialog.dismiss();
        });

        dialog.show();
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
        order.put("discountAmount", discount);
        order.put("voucherCode", priceCalculator.getAppliedVoucherCode());
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
                    for (CartItem item : items) {
                        if (item.getProduct() != null) cartLocalStore.removeItem(item.getProduct().getId());
                    }
                    Intent intent = new Intent(PaymentActivity.this, OrderSuccessActivity.class);
                    intent.putExtra("order_id", ref.getId());
                    intent.putExtra("order_total", total);
                    intent.putExtra("order_payment", paymentMethod);
                    intent.putExtra("order_item_count", items.size());
                    startActivity(intent);
                    finish();
                })
                .addOnFailureListener(e -> {
                    binding.btnPlaceOrder.setEnabled(true);
                    Toast.makeText(PaymentActivity.this,
                            getString(com.tiredcity.app.R.string.error_order_failed) + ": " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                });
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
