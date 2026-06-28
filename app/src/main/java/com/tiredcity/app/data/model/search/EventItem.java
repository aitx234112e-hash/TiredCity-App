package com.tiredcity.app.data.model.search;

import androidx.annotation.DrawableRes;
import androidx.annotation.StringRes;

/** Loại 2: Sự kiện — banner (trái) + tiêu đề + thời gian diễn ra. */
public class EventItem implements SearchItem {

    @StringRes private final int titleResId;
    @StringRes private final int timeResId;
    @DrawableRes private final int bannerRes;

    public EventItem(@StringRes int titleResId, @StringRes int timeResId,
                     @DrawableRes int bannerRes) {
        this.titleResId = titleResId;
        this.timeResId  = timeResId;
        this.bannerRes  = bannerRes;
    }

    @StringRes public int getTitleResId() { return titleResId; }
    @StringRes public int getTimeResId() { return timeResId; }
    @DrawableRes public int getBannerRes() { return bannerRes; }

    @Override
    public int getType() { return TYPE_EVENT; }
}
