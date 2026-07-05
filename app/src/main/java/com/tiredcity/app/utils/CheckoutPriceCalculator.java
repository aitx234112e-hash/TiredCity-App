package com.tiredcity.app.utils;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Tính tạm tính / phí ship / giảm giá / tổng cộng cho màn Thanh toán.
 * Tách riêng khỏi Activity để logic giá tiền dễ test và không lẫn với UI.
 * Voucher hiện là danh sách demo phía client (dự án chưa có backend voucher).
 */
public class CheckoutPriceCalculator {

    public enum VoucherType { PERCENT, FLAT, FREE_SHIP }

    private static class VoucherRule {
        final VoucherType type;
        final double value;
        VoucherRule(VoucherType type, double value) { this.type = type; this.value = value; }
    }

    private static final Map<String, VoucherRule> DEMO_VOUCHERS = new HashMap<>();
    static {
        DEMO_VOUCHERS.put("SALE10", new VoucherRule(VoucherType.PERCENT, 10));
        DEMO_VOUCHERS.put("SALE20", new VoucherRule(VoucherType.PERCENT, 20));
        DEMO_VOUCHERS.put("GIAM50K", new VoucherRule(VoucherType.FLAT, 50_000));
        DEMO_VOUCHERS.put("FREESHIP", new VoucherRule(VoucherType.FREE_SHIP, 0));
    }

    private double subtotal;
    private double shippingFee;
    /** Ngưỡng freeship do admin cấu hình (shipping_settings/general). 0 = không áp dụng. */
    private double freeshipThreshold;
    private String appliedVoucherCode;
    private VoucherRule appliedRule;

    public void setSubtotal(double subtotal) {
        this.subtotal = subtotal;
    }

    public void setShippingFee(double shippingFee) {
        this.shippingFee = shippingFee;
    }

    public void setFreeshipThreshold(double freeshipThreshold) {
        this.freeshipThreshold = freeshipThreshold;
    }

    /** Đơn đạt ngưỡng freeship của admin → miễn phí vận chuyển mọi gói. */
    public boolean isFreeshipByThreshold() {
        return freeshipThreshold > 0 && subtotal >= freeshipThreshold;
    }

    /** @return true nếu mã hợp lệ và đã được áp dụng. */
    public boolean applyVoucher(String code) {
        if (code == null) return false;
        VoucherRule rule = DEMO_VOUCHERS.get(code.trim().toUpperCase(Locale.ROOT));
        if (rule == null) return false;
        appliedRule = rule;
        appliedVoucherCode = code.trim().toUpperCase(Locale.ROOT);
        return true;
    }

    public void clearVoucher() {
        appliedRule = null;
        appliedVoucherCode = null;
    }

    public String getAppliedVoucherCode() {
        return appliedVoucherCode;
    }

    public double getSubtotal() {
        return subtotal;
    }

    /** Phí ship sau khi trừ voucher miễn phí ship hoặc đạt ngưỡng freeship. */
    public double getEffectiveShippingFee() {
        return (isFreeShipVoucher() || isFreeshipByThreshold()) ? 0 : shippingFee;
    }

    /** Số tiền được giảm — hiển thị ở dòng "Giảm giá". */
    public double getDiscountAmount() {
        if (appliedRule == null) return 0;
        switch (appliedRule.type) {
            case PERCENT:   return subtotal * appliedRule.value / 100.0;
            case FLAT:      return Math.min(appliedRule.value, subtotal);
            case FREE_SHIP: return shippingFee;
            default:        return 0;
        }
    }

    public double getTotal() {
        double total = subtotal + getEffectiveShippingFee();
        if (appliedRule != null && appliedRule.type != VoucherType.FREE_SHIP) {
            total -= getDiscountAmount();
        }
        return Math.max(0, total);
    }

    private boolean isFreeShipVoucher() {
        return appliedRule != null && appliedRule.type == VoucherType.FREE_SHIP;
    }
}
