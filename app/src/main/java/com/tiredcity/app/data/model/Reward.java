package com.tiredcity.app.data.model;

import androidx.annotation.DrawableRes;

/**
 * Một ưu đãi / voucher hiển thị ở trang "Ưu đãi".
 * Model nội bộ (chưa nối backend) — dùng cho danh sách voucher demo.
 */
public class Reward {

    private final String title;
    private final String subtitle;
    @DrawableRes private final int bannerRes;
    private final String code;
    private final String validity;
    private final String description;

    public Reward(String title, String subtitle, @DrawableRes int bannerRes,
                  String code, String validity, String description) {
        this.title = title;
        this.subtitle = subtitle;
        this.bannerRes = bannerRes;
        this.code = code;
        this.validity = validity;
        this.description = description;
    }

    public String getTitle() {
        return title;
    }

    public String getSubtitle() {
        return subtitle;
    }

    @DrawableRes
    public int getBannerRes() {
        return bannerRes;
    }

    /** Mã dùng để sinh mã vạch ở màn "Sử dụng ngay". */
    public String getCode() {
        return code;
    }

    /** Khoảng thời gian hiệu lực, ví dụ "01/07/2026 - 31/07/2026". */
    public String getValidity() {
        return validity;
    }

    /** Nội dung chi tiết hiển thị ở màn Chi tiết voucher. */
    public String getDescription() {
        return description;
    }
}
