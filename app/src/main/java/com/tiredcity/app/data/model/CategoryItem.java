package com.tiredcity.app.data.model;

import androidx.annotation.ColorInt;
import androidx.annotation.DrawableRes;
import androidx.annotation.StringRes;

public class CategoryItem {
    private final int id;
    @StringRes private final int nameResId;
    @StringRes private final int descriptionResId;
    @DrawableRes private final int imageRes;
    @ColorInt private final int placeholderColor;

    public CategoryItem(int id, @StringRes int nameResId, @StringRes int descriptionResId,
                         @DrawableRes int imageRes, @ColorInt int placeholderColor) {
        this.id = id;
        this.nameResId = nameResId;
        this.descriptionResId = descriptionResId;
        this.imageRes = imageRes;
        this.placeholderColor = placeholderColor;
    }

    public int getId() { return id; }
    @StringRes public int getNameResId() { return nameResId; }
    @StringRes public int getDescriptionResId() { return descriptionResId; }
    @DrawableRes public int getImageRes() { return imageRes; }
    @ColorInt public int getPlaceholderColor() { return placeholderColor; }
}
