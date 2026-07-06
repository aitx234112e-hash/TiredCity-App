package com.tiredcity.app.utils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;

/**
 * Tính tạm tính / phí ship / giảm giá / tổng cộng cho màn Thanh toán.
 * Tách riêng khỏi Activity để logic giá tiền dễ test và không lẫn với UI.
 * Voucher hiện là danh sách demo phía client (dự án chưa có backend voucher).
 */
public class CheckoutPriceCalculator {

    public enum VoucherType { PERCENT, FLAT, FREE_SHIP }

    /** 1 mã giảm giá demo — kèm tiêu đề/mô tả để hiển thị trong bottom sheet chọn voucher. */
    public static class Voucher {
        public final String code;
        public final String title;
        public final String description;
        public final VoucherType type;
        /** % nếu PERCENT, số tiền nếu FLAT. */
        public final double value;
        /** Trần giảm cho PERCENT (0 = không giới hạn). */
        public final double maxDiscount;
        /** Đơn tối thiểu để dùng mã (0 = không yêu cầu). */
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
        put(new Voucher("SALE10", "Giảm 10% (tối đa 100K)",
                "Áp dụng cho toàn bộ đơn hàng", VoucherType.PERCENT, 10, 100_000, 0));
        put(new Voucher("SALE20", "Giảm 20% (tối đa 200K)",
                "Cho đơn hàng từ 1.000.000đ", VoucherType.PERCENT, 20, 200_000, 1_000_000));
        put(new Voucher("GIAM50K", "Giảm 50.000đ",
                "Áp dụng cho mọi sản phẩm", VoucherType.FLAT, 50_000, 0, 0));
        put(new Voucher("FREESHIP", "Miễn phí vận chuyển",
                "Cho mọi đơn hàng, mọi gói giao", VoucherType.FREE_SHIP, 0, 0, 0));
    }
    private static void put(Voucher v) { DEMO_VOUCHERS.put(v.code, v); }

    /** Danh sách mã có thể chọn trong bottom sheet. */
    public static List<Voucher> getAvailableVouchers() {
        return new ArrayList<>(DEMO_VOUCHERS.values());
    }

    private double subtotal;
    private double shippingFee;
    /** Ngưỡng freeship do admin cấu hình (shipping_settings/general). 0 = không áp dụng. */
    private double freeshipThreshold;
    private Voucher appliedVoucher;

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

    /** @return true nếu mã hợp lệ (và đạt đơn tối thiểu) và đã được áp dụng. */
    /** @return true nếu mã hợp lệ và đã được áp dụng. */
    public boolean applyVoucher(String code, String typeStr, double value) {
        if (code == null || typeStr == null) return false;

        VoucherType type;
        try {
            type = VoucherType.valueOf(typeStr.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return false;
        }

        appliedRule = new VoucherRule(type, value);
        appliedVoucherCode = code.trim().toUpperCase(Locale.ROOT);
        return true;
    }

    public boolean applyVoucher(String code) {
        if (code == null) return false;
        Voucher v = DEMO_VOUCHERS.get(code.trim().toUpperCase(Locale.ROOT));
        if (v == null || subtotal < v.minSpend) return false;
        appliedVoucher = v;
        return true;
    }

    public void clearVoucher() {
        appliedVoucher = null;
    }

    public String getAppliedVoucherCode() {
        return appliedVoucher != null ? appliedVoucher.code : null;
    }

    public Voucher getAppliedVoucher() {
        return appliedVoucher;
    }

    public double getSubtotal() {
        return subtotal;
    }

    /** Phí ship sau khi trừ voucher (nếu có). */
    /** Phí ship sau khi trừ voucher miễn phí ship hoặc đạt ngưỡng freeship. */
    public double getEffectiveShippingFee() {
        if (isFreeShipVoucher()) {
            return 0;
        }
        return shippingFee;
        return (isFreeShipVoucher() || isFreeshipByThreshold()) ? 0 : shippingFee;
    }

    /** Số tiền được giảm — hiển thị ở dòng "Giảm giá". */
    public double getDiscountAmount() {
        if (appliedVoucher == null) return 0;
        switch (appliedVoucher.type) {
            case PERCENT:
                double d = subtotal * appliedVoucher.value / 100.0;
                return appliedVoucher.maxDiscount > 0 ? Math.min(d, appliedVoucher.maxDiscount) : d;
            case FLAT:      return Math.min(appliedVoucher.value, subtotal);
            case FREE_SHIP: return shippingFee;
            default:        return 0;
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
