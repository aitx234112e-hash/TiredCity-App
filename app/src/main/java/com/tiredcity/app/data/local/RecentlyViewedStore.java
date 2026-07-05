package com.tiredcity.app.data.local;

import android.content.Context;
import android.content.SharedPreferences;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.tiredcity.app.data.model.Product;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

/**
 * Lưu danh sách sản phẩm khách hàng THẬT SỰ đã xem (mở trang chi tiết) — cục bộ trên máy.
 * Cùng pattern SharedPreferences+Gson với {@link FavoritesLocalStore}. Mới nhất đứng đầu,
 * không trùng lặp theo id, giới hạn {@link #MAX_ITEMS} sản phẩm.
 */
public class RecentlyViewedStore {
    private static final String PREF_RECENT = "recently_viewed_local";
    private static final String KEY_ITEMS = "recent_items";
    private static final int MAX_ITEMS = 12;

    private final SharedPreferences prefs;
    private final Gson gson;

    public RecentlyViewedStore(Context context) {
        prefs = context.getApplicationContext().getSharedPreferences(PREF_RECENT, Context.MODE_PRIVATE);
        gson = new Gson();
    }

    public List<Product> getRecentlyViewed() {
        String json = prefs.getString(KEY_ITEMS, null);
        if (json == null) return new ArrayList<>();
        Type type = new TypeToken<List<Product>>() {}.getType();
        List<Product> items = gson.fromJson(json, type);
        return items != null ? items : new ArrayList<>();
    }

    /** Ghi nhận một lượt xem: đưa sản phẩm lên đầu, bỏ bản trùng cũ, cắt còn tối đa {@link #MAX_ITEMS}. */
    public void addProduct(Product product) {
        if (product == null || product.getId() == null) return;
        List<Product> items = getRecentlyViewed();
        items.removeIf(item -> product.getId().equals(item.getId()));
        items.add(0, product);
        while (items.size() > MAX_ITEMS) {
            items.remove(items.size() - 1);
        }
        prefs.edit().putString(KEY_ITEMS, gson.toJson(items)).apply();
    }
}
