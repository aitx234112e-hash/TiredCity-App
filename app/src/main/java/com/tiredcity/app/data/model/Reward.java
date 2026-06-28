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
    private final int points;
    private final String code;

    public Reward(String title, String subtitle, @DrawableRes int bannerRes, int points) {
        this(title, subtitle, bannerRes, points, null);
    }

    public Reward(String title, String subtitle, @DrawableRes int bannerRes, int points, String code) {
        this.title = title;
        this.subtitle = subtitle;
        this.bannerRes = bannerRes;
        this.points = points;
        this.code = code;
    }

    public String getCode() {
        return code;
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

    public int getPoints() {
        return points;
    }
}
