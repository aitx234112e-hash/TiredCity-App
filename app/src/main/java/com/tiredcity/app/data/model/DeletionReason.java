package com.tiredcity.app.data.model;

import androidx.annotation.StringRes;

/** Một lý do trong danh sách "Yêu cầu xóa tài khoản". */
public class DeletionReason {

    @StringRes public final int titleRes;
    @StringRes public final int descRes;

    public DeletionReason(@StringRes int titleRes, @StringRes int descRes) {
        this.titleRes = titleRes;
        this.descRes  = descRes;
    }
}
