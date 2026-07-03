package com.tiredcity.app.data.local;

import android.content.Context;
import android.content.SharedPreferences;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.tiredcity.app.data.model.Product;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

/** Lưu "Tủ đồ yêu thích" cục bộ trên máy — cùng pattern SharedPreferences+Gson với {@link CartLocalStore}. */
public class FavoritesLocalStore {
    private static final String PREF_FAVORITES = "favorites_local";
    private static final String KEY_FAVORITE_ITEMS = "favorite_items";

    private final SharedPreferences prefs;
    private final Gson gson;

    public FavoritesLocalStore(Context context) {
        prefs = context.getApplicationContext().getSharedPreferences(PREF_FAVORITES, Context.MODE_PRIVATE);
        gson = new Gson();
    }

    public List<Product> getFavorites() {
        String json = prefs.getString(KEY_FAVORITE_ITEMS, null);
        if (json == null) return new ArrayList<>();
        Type type = new TypeToken<List<Product>>() {}.getType();
        List<Product> items = gson.fromJson(json, type);
        return items != null ? items : new ArrayList<>();
    }

    private void saveFavorites(List<Product> items) {
        prefs.edit().putString(KEY_FAVORITE_ITEMS, gson.toJson(items)).apply();
    }

    public boolean isFavorite(String productId) {
        if (productId == null) return false;
        for (Product item : getFavorites()) {
            if (productId.equals(item.getId())) return true;
        }
        return false;
    }

    /** Thêm nếu chưa có, xoá nếu đã có. Trả về trạng thái mới (true = vừa được thêm). */
    public boolean toggleFavorite(Product product) {
        List<Product> items = getFavorites();
        boolean removed = items.removeIf(item -> item.getId() != null && item.getId().equals(product.getId()));
        if (!removed) items.add(product);
        saveFavorites(items);
        return !removed;
    }

    public void removeFavorite(String productId) {
        List<Product> items = getFavorites();
        items.removeIf(item -> productId != null && productId.equals(item.getId()));
        saveFavorites(items);
    }
}
