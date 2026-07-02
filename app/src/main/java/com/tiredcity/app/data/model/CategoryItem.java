package com.tiredcity.app.data.model;

import androidx.annotation.ColorInt;
import androidx.annotation.DrawableRes;
import androidx.annotation.StringRes;

public class CategoryItem {
    private final int id;
    @StringRes private final int nameResId;
    @StringRes private final int descriptionResId;
    private final String nameText;
    private final String descriptionText;
    @DrawableRes private final int imageRes;
    @ColorInt private final int placeholderColor;

    /** Resource-based constructor (i18n strings). */
    public CategoryItem(int id, @StringRes int nameResId, @StringRes int descriptionResId,
                         @DrawableRes int imageRes, @ColorInt int placeholderColor) {
        this.id = id;
        this.nameResId = nameResId;
        this.descriptionResId = descriptionResId;
        this.nameText = null;
        this.descriptionText = null;
        this.imageRes = imageRes;
        this.placeholderColor = placeholderColor;
    }

    /** Literal-text constructor (used for tab-driven category lists). */
    public CategoryItem(int id, String nameText, String descriptionText,
                         @DrawableRes int imageRes, @ColorInt int placeholderColor) {
        this.id = id;
        this.nameResId = 0;
        this.descriptionResId = 0;
        this.nameText = nameText;
        this.descriptionText = descriptionText;
        this.imageRes = imageRes;
        this.placeholderColor = placeholderColor;
    }

    public int getId() { return id; }
    @StringRes public int getNameResId() { return nameResId; }
    @StringRes public int getDescriptionResId() { return descriptionResId; }
    public String getNameText() { return nameText; }
    public String getDescriptionText() { return descriptionText; }
    @DrawableRes public int getImageRes() { return imageRes; }
    @ColorInt public int getPlaceholderColor() { return placeholderColor; }
}
