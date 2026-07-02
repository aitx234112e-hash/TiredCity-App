package com.tiredcity.app.data.model;

import androidx.annotation.ColorInt;
import androidx.annotation.DrawableRes;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;

public class CategoryItem {
    private final int id;
    @StringRes private final int nameResId;
    @StringRes private final int descriptionResId;
    private final String nameText;
    private final String descriptionText;
    @DrawableRes private final int imageRes;
    @ColorInt private final int placeholderColor;
    /** Tên danh mục cha gửi lên API khi mở CategoryActivity (vd "ÁO DÀI", "PHỤ KIỆN"). */
    @Nullable private final String parentCategory;
    /** Giá trị lọc con (nhóm màu hoặc phân loại phụ kiện), khớp với Product.getColors(). */
    @Nullable private final String filterValue;

    /** Resource-based constructor (i18n strings). */
    public CategoryItem(int id, @StringRes int nameResId, @StringRes int descriptionResId,
                         @DrawableRes int imageRes, @ColorInt int placeholderColor) {
        this(id, nameResId, descriptionResId, null, null, imageRes, placeholderColor, null, null);
    }

    /** Literal-text constructor (used for tab-driven category lists). */
    public CategoryItem(int id, String nameText, String descriptionText,
                         @DrawableRes int imageRes, @ColorInt int placeholderColor) {
        this(id, 0, 0, nameText, descriptionText, imageRes, placeholderColor, null, null);
    }

    /** Literal-text constructor + API filter wiring (tiles that open CategoryActivity filtered). */
    public CategoryItem(int id, String nameText, String descriptionText,
                         @DrawableRes int imageRes, @ColorInt int placeholderColor,
                         @Nullable String parentCategory, @Nullable String filterValue) {
        this(id, 0, 0, nameText, descriptionText, imageRes, placeholderColor, parentCategory, filterValue);
    }

    private CategoryItem(int id, @StringRes int nameResId, @StringRes int descriptionResId,
                          String nameText, String descriptionText,
                          @DrawableRes int imageRes, @ColorInt int placeholderColor,
                          @Nullable String parentCategory, @Nullable String filterValue) {
        this.id = id;
        this.nameResId = nameResId;
        this.descriptionResId = descriptionResId;
        this.nameText = nameText;
        this.descriptionText = descriptionText;
        this.imageRes = imageRes;
        this.placeholderColor = placeholderColor;
        this.parentCategory = parentCategory;
        this.filterValue = filterValue;
    }

    public int getId() { return id; }
    @StringRes public int getNameResId() { return nameResId; }
    @StringRes public int getDescriptionResId() { return descriptionResId; }
    public String getNameText() { return nameText; }
    public String getDescriptionText() { return descriptionText; }
    @DrawableRes public int getImageRes() { return imageRes; }
    @ColorInt public int getPlaceholderColor() { return placeholderColor; }
    @Nullable public String getParentCategory() { return parentCategory; }
    @Nullable public String getFilterValue() { return filterValue; }
}
