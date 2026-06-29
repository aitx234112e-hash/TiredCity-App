package com.tiredcity.app.data.model.search;

import androidx.annotation.StringRes;

/** Loại 1: Ưu đãi — biểu tượng quà tặng + nội dung voucher. */
public class PromotionItem implements SearchItem {

    @StringRes private final int contentResId;

    public PromotionItem(@StringRes int contentResId) {
        this.contentResId = contentResId;
    }

    @StringRes public int getContentResId() { return contentResId; }

    @Override
    public int getType() { return TYPE_PROMOTION; }
}
