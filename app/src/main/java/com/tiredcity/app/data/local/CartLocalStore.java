package com.tiredcity.app.data.local;

import android.content.Context;
import android.content.SharedPreferences;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.tiredcity.app.data.model.CartItem;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class CartLocalStore {
    private static final String PREF_CART = "cart_local";
    private static final String KEY_CART_ITEMS = "cart_items";

    private final SharedPreferences prefs;
    private final Gson gson;

    public CartLocalStore(Context context) {
        prefs = context.getSharedPreferences(PREF_CART, Context.MODE_PRIVATE);
        gson = new Gson();
    }

    public List<CartItem> getCartItems() {
        String json = prefs.getString(KEY_CART_ITEMS, null);
        if (json == null) return new ArrayList<>();
        Type type = new TypeToken<List<CartItem>>() {}.getType();
        return gson.fromJson(json, type);
    }

    public void saveCartItems(List<CartItem> items) {
        prefs.edit().putString(KEY_CART_ITEMS, gson.toJson(items)).apply();
    }

    public void addItem(CartItem newItem) {
        List<CartItem> items = getCartItems();
        for (CartItem item : items) {
            if (item.getProduct().getId().equals(newItem.getProduct().getId())) {
                item.setQuantity(item.getQuantity() + newItem.getQuantity());
                saveCartItems(items);
                return;
            }
        }
        items.add(newItem);
        saveCartItems(items);
    }

    public void removeItem(String productId) {
        List<CartItem> items = getCartItems();
        items.removeIf(item -> item.getProduct().getId().equals(productId));
        saveCartItems(items);
    }

    public void clearCart() {
        prefs.edit().remove(KEY_CART_ITEMS).apply();
    }

    public int getCartCount() {
        return getCartItems().size();
    }
}
