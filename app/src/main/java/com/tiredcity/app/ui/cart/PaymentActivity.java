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

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.tiredcity.app.data.model.Product;
import com.tiredcity.app.data.model.UserProfile;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

public class PaymentActivity extends BaseActivity {

    private ActivityPaymentBinding binding;
    private CartLocalStore cartLocalStore;
    private final CheckoutPriceCalculator priceCalculator = new CheckoutPriceCalculator();

    private static final double DEFAULT_SHIPPING_FEE = 30_000;
    private static final String SHIPPING_COLLECTION = "shipping_configs";
    private static final String[] SHIPPING_ORDER = {"economy", "standard", "express"};
    private static final String DEFAULT_SHIPPING_ID = "standard";

    private String shippingMethodName = "";
    private final List<ShippingOption> activeShippingOptions = new ArrayList<>();
    private final List<ShippingOption> displayedShippingOptions = new ArrayList<>();
    private ShippingOption selectedShippingOption;
    private boolean shippingExpanded = true;
    private String selectedPaymentMethod = "MOMO";
    private CheckoutItemAdapter checkoutItemAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityPaymentBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        binding.toolbar.setNavigationOnClickListener(v -> finish());

        cartLocalStore = new CartLocalStore(this);
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
            // Logic for captcha can be added here
            placeOrder(address, selectedPaymentMethod);
        });
    }

    private void setupOrderSummary() {
        bindRecipientCard();
        refreshTotals();
    }

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

    private void updateSeeAllLabel() {
        if (checkoutItemAdapter.isCollapsed()) {
            binding.tvSeeAll.setText(getString(com.tiredcity.app.R.string.pay_see_all, checkoutItemAdapter.getTotalCount()));
        } else {
            binding.tvSeeAll.setText(com.tiredcity.app.R.string.pay_collapse);
        }
    }

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
        binding.tvShippingFee.setText(effectiveShipping <= 0 ? getString(com.tiredcity.app.R.string.pay_shipping_free) : PriceUtils.format(effectiveShipping));

        double discount = priceCalculator.getDiscountAmount();
        binding.rowDiscount.setVisibility(discount > 0 ? View.VISIBLE : View.GONE);
        if (discount > 0) binding.tvDiscount.setText("-" + PriceUtils.format(discount));

        binding.tvTotal.setText(PriceUtils.format(priceCalculator.getTotal()));
    }

    private void loadShippingMethods() {
        FirebaseFirestore.getInstance().collection("shipping_settings").document("general").get().addOnSuccessListener(d -> {
            Double threshold = d != null ? d.getDouble("freeshipThreshold") : null;
            priceCalculator.setFreeshipThreshold(threshold != null ? threshold : 0);
            loadShippingConfigs();
        }).addOnFailureListener(e -> loadShippingConfigs());
    }

    private void loadShippingConfigs() {
        FirebaseFirestore.getInstance().collection(SHIPPING_COLLECTION).get().addOnSuccessListener(snap -> {
            Map<String, DocumentSnapshot> byId = new HashMap<>();
            for (com.google.firebase.firestore.QueryDocumentSnapshot d : snap) byId.put(d.getId(), d);
            activeShippingOptions.clear();
            for (String id : SHIPPING_ORDER) {
                DocumentSnapshot d = byId.get(id);
                if (d == null || !Boolean.TRUE.equals(d.getBoolean("isActive"))) continue;
                activeShippingOptions.add(new ShippingOption(id, d.getString("name"), d.getDouble("price") != null ? d.getDouble("price") : 0, d.getDouble("originalPrice") != null ? d.getDouble("originalPrice") : 0, d.getString("estimate")));
            }
            if (activeShippingOptions.isEmpty()) applyFallbackShipping();
            else applyShippingForAddress();
        }).addOnFailureListener(e -> applyFallbackShipping());
    }

    private void applyFallbackShipping() {
        activeShippingOptions.clear();
        activeShippingOptions.add(new ShippingOption("standard", "Giao hàng nhanh", 0, 16_500, "1-2 ngày"));
        activeShippingOptions.add(new ShippingOption("express", "Hỏa tốc", 92_100, 0, "2 giờ"));
        applyShippingForAddress();
    }

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
            if ("express".equals(o.id) && !hanoi) continue;
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

    private boolean isHanoiProvince() {
        UserProfile user = preferenceManager.getUser();
        String province = user != null ? user.getProvince() : "";
        return noAccent(province).contains("ha noi");
    }

    private ShippingOption findShippingOptionById(String id) {
        for (ShippingOption o : displayedShippingOptions) if (o.id.equals(id)) return o;
        return null;
    }

    private static String noAccent(String s) {
        if (s == null) return "";
        String n = java.text.Normalizer.normalize(s, java.text.Normalizer.Form.NFD).replaceAll("\\p{M}+", "").replace('đ', 'd').replace('Đ', 'D');
        return n.toLowerCase(Locale.ROOT).trim();
    }

    private String shippingDisplayName(ShippingOption option) {
        switch (option.id) {
            case "economy":  return getString(com.tiredcity.app.R.string.pay_ship_economy);
            case "express":  return getString(com.tiredcity.app.R.string.pay_ship_express);
            case "standard": return getString(com.tiredcity.app.R.string.pay_ship_standard);
            default: return option.name != null ? option.name.replaceFirst("(?i)^SPX\\s*", "") : "";
        }
    }

    private void renderShippingOptions() {
        binding.tvShippingLoading.setVisibility(View.GONE);
        binding.containerShippingOptions.removeAllViews();
        boolean freeByThreshold = priceCalculator.isFreeshipByThreshold();

        for (ShippingOption option : displayedShippingOptions) {
            View root = getLayoutInflater().inflate(com.tiredcity.app.R.layout.item_shipping_option, binding.containerShippingOptions, false);
            TextView tvName = root.findViewById(com.tiredcity.app.R.id.tv_option_name);
            TextView tvPrice = root.findViewById(com.tiredcity.app.R.id.tv_option_price);
            TextView tvOriginal = root.findViewById(com.tiredcity.app.R.id.tv_option_price_original);
            View ivVoucher = root.findViewById(com.tiredcity.app.R.id.iv_freeship_voucher);
            TextView tvEta = root.findViewById(com.tiredcity.app.R.id.tv_option_eta);
            View badge = root.findViewById(com.tiredcity.app.R.id.badge_eta);
            TextView tvBadge = root.findViewById(com.tiredcity.app.R.id.tv_option_eta_badge);

            tvName.setText(shippingDisplayName(option));

            if (option.price <= 0 || freeByThreshold) {
                tvPrice.setText(com.tiredcity.app.R.string.pay_ship_free);
                tvPrice.setTextColor(androidx.core.content.ContextCompat.getColor(this, com.tiredcity.app.R.color.tc_ship_green));
                ivVoucher.setVisibility(View.VISIBLE);
                double original = freeByThreshold && option.price > 0 ? option.price : option.originalPrice;
                if (original > 0) {
                    tvOriginal.setVisibility(View.VISIBLE);
                    tvOriginal.setText(PriceUtils.format(original));
                    tvOriginal.setPaintFlags(tvOriginal.getPaintFlags() | android.graphics.Paint.STRIKE_THRU_TEXT_FLAG);
                }
            } else {
                tvPrice.setText(PriceUtils.format(option.price));
                tvPrice.setTextColor(androidx.core.content.ContextCompat.getColor(this, com.tiredcity.app.R.color.text_primary));
                ivVoucher.setVisibility(View.GONE);
            }

            String estimate = option.estimate != null ? option.estimate : "";
            if (estimate.isEmpty()) tvEta.setVisibility(View.GONE);
            else {
                String range = etaRangeText(estimate);
                if (range == null) {
                    tvEta.setText(com.tiredcity.app.R.string.pay_ship_eta_in);
                    badge.setVisibility(View.VISIBLE);
                    tvBadge.setText(estimate);
                } else tvEta.setText(range);
            }
            root.setTag(option.id);
            root.findViewById(com.tiredcity.app.R.id.row_shipping_option).setOnClickListener(v -> selectShippingOption(option));
            binding.containerShippingOptions.addView(root);
        }
        binding.tvShippingInspection.setVisibility(View.VISIBLE);
        binding.tvShippingSeeAll.setVisibility(displayedShippingOptions.size() > 1 ? View.VISIBLE : View.GONE);
        binding.tvShippingSeeAll.setOnClickListener(v -> {
            shippingExpanded = !shippingExpanded;
            applyShippingCollapse();
        });
    }

    private void updateShippingSelectionUi() {
        String selectedId = selectedShippingOption != null ? selectedShippingOption.id : null;
        for (int i = 0; i < binding.containerShippingOptions.getChildCount(); i++) {
            View root = binding.containerShippingOptions.getChildAt(i);
            boolean selected = root.getTag() != null && root.getTag().equals(selectedId);
            root.findViewById(com.tiredcity.app.R.id.row_shipping_option).setSelected(selected);
            root.findViewById(com.tiredcity.app.R.id.iv_option_ribbon).setVisibility(selected ? View.VISIBLE : View.GONE);
        }
        applyShippingCollapse();
    }

    private void applyShippingCollapse() {
        String selectedId = selectedShippingOption != null ? selectedShippingOption.id : null;
        for (int i = 0; i < binding.containerShippingOptions.getChildCount(); i++) {
            View root = binding.containerShippingOptions.getChildAt(i);
            boolean selected = root.getTag() != null && root.getTag().equals(selectedId);
            root.setVisibility(shippingExpanded || selected ? View.VISIBLE : View.GONE);
        }
        binding.tvShippingSeeAll.setText(com.tiredcity.app.R.string.pay_ship_see_all);
    }

    private void selectShippingOption(ShippingOption option) {
        selectedShippingOption = option;
        shippingMethodName = shippingDisplayName(option);
        priceCalculator.setShippingFee(option.price);
        updateShippingSelectionUi();
        refreshTotals();
    }

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
        if (max == min) return getString(com.tiredcity.app.R.string.pay_ship_eta_on, from);
        cal = java.util.Calendar.getInstance();
        cal.add(java.util.Calendar.DAY_OF_YEAR, max);
        return getString(com.tiredcity.app.R.string.pay_ship_eta_range, from, fmt.format(cal.getTime()));
    }

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
        if (code != null) binding.tvVoucherStatus.setText(getString(com.tiredcity.app.R.string.pay_voucher_applied, code));
        else binding.tvVoucherStatus.setText(com.tiredcity.app.R.string.pay_voucher_hint);
    }

    private void showVoucherSheet() {
        VoucherBottomSheet sheet = new VoucherBottomSheet();
        sheet.setCurrentCode(priceCalculator.getAppliedVoucherCode());
        sheet.setOnVoucherAppliedListener(rawCode -> {
            if (rawCode == null || rawCode.trim().isEmpty()) {
                priceCalculator.clearVoucher();
                updateVoucherStatusLabel();
                refreshTotals();
                return;
            }
            
            final String code = rawCode.trim().toUpperCase(Locale.ROOT);
            
            // 1. Kiem tra ma demo/he thong truoc
            if (priceCalculator.applyVoucher(code)) {
                Toast.makeText(this, "Áp dụng mã thành công!", Toast.LENGTH_SHORT).show();
                updateVoucherStatusLabel();
                refreshTotals();
                return;
            }

            // 2. Kiem tra tren Firestore neu khong phai ma demo
            FirebaseFirestore.getInstance().collection("vouchers")
                    .whereEqualTo("code", code)
                    .get()
                    .addOnSuccessListener(query -> {
                        if (!query.isEmpty()) {
                            DocumentSnapshot doc = query.getDocuments().get(0);
                            boolean active = Boolean.TRUE.equals(doc.getBoolean("isActive"));

                            // Kiểm tra ngày hiệu lực
                            boolean expired = false;
                            boolean notStarted = false;
                            String startStr = doc.getString("startDate");
                            String expiryStr = doc.getString("expiry");
                            java.util.Date now = new java.util.Date();
                            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd", Locale.US);

                            try {
                                if (startStr != null && !startStr.isEmpty()) {
                                    java.util.Date startDate = sdf.parse(startStr);
                                    if (startDate != null && now.before(startDate)) notStarted = true;
                                }
                                if (expiryStr != null && !expiryStr.isEmpty()) {
                                    java.util.Date expiryDate = sdf.parse(expiryStr);
                                    if (expiryDate != null) {
                                        java.util.Calendar cal = java.util.Calendar.getInstance();
                                        cal.setTime(expiryDate);
                                        cal.set(java.util.Calendar.HOUR_OF_DAY, 23);
                                        cal.set(java.util.Calendar.MINUTE, 59);
                                        cal.set(java.util.Calendar.SECOND, 59);
                                        if (now.after(cal.getTime())) expired = true;
                                    }
                                }
                            } catch (Exception ignored) {}

                            if (active && !expired && !notStarted) {
                                // Map fields từ Firestore (hỗ trợ cả schema cũ và schema admin)
                                String type = doc.getString("type");
                                if (type == null) {
                                    type = doc.contains("discount") ? "PERCENT" : "FLAT";
                                }

                                double value = 0;
                                if (doc.contains("value")) {
                                    Double v = doc.getDouble("value");
                                    if (v != null) value = v;
                                } else if (doc.contains("discount")) {
                                    Double d = doc.getDouble("discount");
                                    if (d != null) value = d;
                                }

                                double minOrder = 0;
                                if (doc.contains("minOrder")) {
                                    Double m = doc.getDouble("minOrder");
                                    if (m != null) minOrder = m;
                                } else if (doc.contains("minSpend")) {
                                    Double s = doc.getDouble("minSpend");
                                    if (s != null) minOrder = s;
                                }

                                if (priceCalculator.applyVoucher(code, type, value, minOrder)) {
                                    Toast.makeText(this, "Áp dụng mã thành công!", Toast.LENGTH_SHORT).show();
                                } else {
                                    String minOrderFmt = String.format(Locale.US, "%,.0f", minOrder);
                                    Toast.makeText(this, "Mã yêu cầu đơn tối thiểu " + minOrderFmt + "đ", Toast.LENGTH_SHORT).show();
                                    priceCalculator.clearVoucher();
                                }
                            } else {
                                String msg = "Mã giảm giá không hợp lệ hoặc đã hết hạn";
                                if (expired) msg = "Mã giảm giá đã hết hạn";
                                else if (notStarted) msg = "Mã giảm giá chưa đến ngày sử dụng";
                                else if (!active) msg = "Mã giảm giá đã bị vô hiệu";

                                Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
                                priceCalculator.clearVoucher();
                            }
                        } else {
                            Toast.makeText(this, "Mã giảm giá không tồn tại", Toast.LENGTH_SHORT).show();
                            priceCalculator.clearVoucher();
                        }
                        updateVoucherStatusLabel();
                        refreshTotals();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "Lỗi kiểm tra mã: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        updateVoucherStatusLabel();
                        refreshTotals();
                    });
        });
        sheet.show(getSupportFragmentManager(), "voucher_sheet");
    }

    private void showEditRecipientDialog() {
        DialogEditRecipientBinding dialogBinding = DialogEditRecipientBinding.inflate(getLayoutInflater());
        UserProfile user = preferenceManager.getUser();
        if (user != null) {
            dialogBinding.etRecipientName.setText(user.getName());
            dialogBinding.etRecipientPhone.setText(user.getPhone());
            dialogBinding.etRecipientAddress.setText(user.getStreet());
        }
        AutoCompleteTextView actProvince = dialogBinding.actProvince;
        AutoCompleteTextView actDistrict = dialogBinding.actDistrict;
        AutoCompleteTextView actWard = dialogBinding.actWard;
        setDropdown(actProvince, new ArrayList<>(java.util.Arrays.asList(getResources().getStringArray(com.tiredcity.app.R.array.vn_provinces))));
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
        AlertDialog dialog = new AlertDialog.Builder(this).setView(dialogBinding.getRoot()).create();
        if (dialog.getWindow() != null) dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
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
            applyShippingForAddress();
            dialog.dismiss();
        });
        dialog.show();
    }

    private void setDropdown(AutoCompleteTextView view, List<String> items) {
        view.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, items));
    }

    private void placeOrder(String address, String paymentMethod) {
        binding.btnPlaceOrder.setEnabled(false);
        List<CartItem> items = selectedItems();
        if (items == null || items.isEmpty()) {
            binding.btnPlaceOrder.setEnabled(true);
            Toast.makeText(this, getString(com.tiredcity.app.R.string.error_order_failed), Toast.LENGTH_SHORT).show();
            return;
        }

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        String datePrefix = new SimpleDateFormat("yyMMdd", Locale.US).format(new Date());
        String randomSuffix = String.format("%04d", (int)(Math.random() * 10000));
        String orderCode = "TC-" + datePrefix + "-" + randomSuffix;

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
        UserProfile user = preferenceManager.getUser();

        Map<String, Object> orderData = new HashMap<>();
        orderData.put("orderCode", orderCode);
        orderData.put("userId", currentUid);
        orderData.put("userName", user != null ? user.getName() : "");
        orderData.put("userEmail", user != null ? user.getEmail() : "");
        orderData.put("phone", user != null ? user.getPhone() : "");
        orderData.put("items", itemList);
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

        db.runTransaction(transaction -> {
            Map<String, Map<String, Object>> productUpdates = new HashMap<>();
            for (CartItem item : items) {
                if (item.getProduct() != null) {
                    String pId = item.getProduct().getId().trim().toUpperCase();
                    if (pId.matches("^[A-Z]{2}\\d$")) pId = pId.substring(0, 2) + "0" + pId.substring(2);
                    com.google.firebase.firestore.DocumentReference pRef = db.collection("products").document(pId);
                    DocumentSnapshot pSnap = transaction.get(pRef);
                    if (pSnap.exists()) {
                        Map<String, Object> updates = productUpdates.get(pId);
                        if (updates == null) updates = new HashMap<>();
                        long currentStock = pSnap.getLong("stock") != null ? pSnap.getLong("stock") : 0;
                        if (updates.containsKey("stock")) currentStock = (long) updates.get("stock");
                        if (currentStock < item.getQuantity()) throw new RuntimeException("Sản phẩm " + item.getProduct().getName() + " không đủ hàng!");
                        updates.put("stock", currentStock - item.getQuantity());
                        productUpdates.put(pId, updates);
                    }
                }
            }
            for (Map.Entry<String, Map<String, Object>> entry : productUpdates.entrySet()) {
                transaction.update(db.collection("products").document(entry.getKey()), entry.getValue());
            }
            com.google.firebase.firestore.DocumentReference orderRef = db.collection("orders").document();
            transaction.set(orderRef, orderData);
            return orderRef.getId();
        }).addOnSuccessListener(docId -> {
            for (CartItem item : items) if (item.getProduct() != null) cartLocalStore.removeItem(item.getProduct().getId());
            Intent intent = new Intent(PaymentActivity.this, OrderSuccessActivity.class);
            intent.putExtra("order_id", docId);
            intent.putExtra("order_code", orderCode);
            intent.putExtra("order_total", total);
            intent.putExtra("order_payment", paymentMethod);
            intent.putExtra("order_item_count", items.size());
            startActivity(intent);
            finish();
        }).addOnFailureListener(e -> {
            binding.btnPlaceOrder.setEnabled(true);
            Toast.makeText(this, "Đặt hàng thất bại: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        });
    }
}
