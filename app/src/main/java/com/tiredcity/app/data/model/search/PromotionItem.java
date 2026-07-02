package com.tiredcity.app.data.model.search;

import androidx.annotation.DrawableRes;
import androidx.annotation.StringRes;

/** Loại 1: Ưu đãi — ảnh voucher + nội dung; bấm vào mở trang chi tiết voucher. */
public class PromotionItem implements SearchItem {

    @StringRes private final int contentResId;
    @DrawableRes private final int bannerRes;
    @StringRes private final int voucherTitleResId;
    @StringRes private final int voucherSubtitleResId;

    public PromotionItem(@StringRes int contentResId,
                         @DrawableRes int bannerRes,
                         @StringRes int voucherTitleResId,
                         @StringRes int voucherSubtitleResId) {
        this.contentResId = contentResId;
        this.bannerRes = bannerRes;
        this.voucherTitleResId = voucherTitleResId;
        this.voucherSubtitleResId = voucherSubtitleResId;
    }

    @StringRes public int getContentResId() { return contentResId; }

    @DrawableRes public int getBannerRes() { return bannerRes; }

    @StringRes public int getVoucherTitleResId() { return voucherTitleResId; }

    @StringRes public int getVoucherSubtitleResId() { return voucherSubtitleResId; }

    @Override
    public int getType() { return TYPE_PROMOTION; }
}
