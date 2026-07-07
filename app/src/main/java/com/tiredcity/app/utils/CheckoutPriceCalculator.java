package com.tiredcity.app.utils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;

/**
 * Tính tạm tính / phí ship / giảm giá / tổng cộng cho màn Thanh toán.
 */
public class CheckoutPriceCalculator {

    public enum VoucherType { PERCENT, FLAT, FREE_SHIP }

    public static class Voucher {
        public final String code;
        public final String title;
        public final String description;
        public final VoucherType type;
        public final double value;
        public final double maxDiscount;
        public final double minSpend;

        public Voucher(String code, String title, String description,
                       VoucherType type, double value, double maxDiscount, double minSpend) {
            this.code = code;
            this.title = title;
            this.description = description;
            this.type = type;
            this.value = value;
            this.maxDiscount = maxDiscount;
            this.minSpend = minSpend;
        }
    }

    private static final LinkedHashMap<String, Voucher> DEMO_VOUCHERS = new LinkedHashMap<>();
    static {
        put(new Voucher("SALE10", "Giảm 10% (tối đa 100K)", "Áp dụng cho toàn bộ đơn hàng", VoucherType.PERCENT, 10, 100_000, 0));
        put(new Voucher("SALE20", "Giảm 20% (tối đa 200K)", "Cho đơn hàng từ 1.000.000đ", VoucherType.PERCENT, 20, 200_000, 1_000_000));
        put(new Voucher("GIAM50K", "Giảm 50.000đ", "Áp dụng cho mọi sản phẩm", VoucherType.FLAT, 50_000, 0, 0));
        put(new Voucher("FREESHIP", "Miễn phí vận chuyển", "Cho mọi đơn hàng, mọi gói giao", VoucherType.FREE_SHIP, 0, 0, 0));
    }
    private static void put(Voucher v) { DEMO_VOUCHERS.put(v.code, v); }

    public static List<Voucher> getAvailableVouchers() {
        return new ArrayList<>(DEMO_VOUCHERS.values());
    }

    private double subtotal;
    private double shippingFee;
    private double freeshipThreshold;
    private Voucher appliedVoucher;

    public void setSubtotal(double subtotal) { this.subtotal = subtotal; }
    public void setShippingFee(double shippingFee) { this.shippingFee = shippingFee; }
    public void setFreeshipThreshold(double threshold) { this.freeshipThreshold = threshold; }

    public boolean isFreeshipByThreshold() {
        return freeshipThreshold > 0 && subtotal >= freeshipThreshold;
    }

    public boolean applyVoucher(String code, String typeStr, double value, double minSpend) {
        if (code == null || typeStr == null) return false;
        if (subtotal < minSpend) return false;
        try {
            VoucherType type = VoucherType.valueOf(typeStr.toUpperCase(Locale.ROOT));
            appliedVoucher = new Voucher(code, code, "Mã giảm giá từ hệ thống", type, value, 0, minSpend);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    public boolean applyVoucher(String code, String typeStr, double value) {
        return applyVoucher(code, typeStr, value, 0);
    }

    public boolean applyVoucher(String code) {
        if (code == null) return false;
        Voucher v = DEMO_VOUCHERS.get(code.trim().toUpperCase(Locale.ROOT));
        if (v == null || subtotal < v.minSpend) return false;
        appliedVoucher = v;
        return true;
    }

    public void clearVoucher() { appliedVoucher = null; }
    public String getAppliedVoucherCode() { return appliedVoucher != null ? appliedVoucher.code : null; }
    public Voucher getAppliedVoucher() { return appliedVoucher; }
    public double getSubtotal() { return subtotal; }

    public double getEffectiveShippingFee() {
        return (isFreeShipVoucher() || isFreeshipByThreshold()) ? 0 : shippingFee;
    }

    public double getDiscountAmount() {
        if (appliedVoucher == null) return 0;
        switch (appliedVoucher.type) {
            case PERCENT:
                double d = subtotal * appliedVoucher.value / 100.0;
                return appliedVoucher.maxDiscount > 0 ? Math.min(d, appliedVoucher.maxDiscount) : d;
            case FLAT: return Math.min(appliedVoucher.value, subtotal);
            case FREE_SHIP: return shippingFee;
            default: return 0;
        }
    }

    public double getTotal() {
        double total = subtotal + getEffectiveShippingFee();
        if (appliedVoucher != null && appliedVoucher.type != VoucherType.FREE_SHIP) {
            total -= getDiscountAmount();
        }
        return Math.max(0, total);
    }

    private boolean isFreeShipVoucher() {
        return appliedVoucher != null && appliedVoucher.type == VoucherType.FREE_SHIP;
    }
}
