package com.tiredcity.app.data.repository;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.tiredcity.app.data.local.CartLocalStore;
import com.tiredcity.app.data.model.ApiListResponse;
import com.tiredcity.app.data.model.ApiResponse;
import com.tiredcity.app.data.model.CartItem;
import com.tiredcity.app.data.network.ApiService;
import java.util.List;
import retrofit2.Call;

public class CartRepository {
    private final ApiService apiService;
    private final CartLocalStore localStore;
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    public CartRepository(ApiService apiService, CartLocalStore localStore) {
        this.apiService = apiService;
        this.localStore = localStore;
    }

    /** Đồng bộ giỏ hàng cục bộ lên Firestore. */
    public void syncCartToCloud() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        List<CartItem> items = localStore.getCartItems();
        if (items == null) return;

        db.collection("carts").document(user.getUid()).set(new CartItemsWrapper(items));
    }

    /** Tải giỏ hàng từ Firestore về máy. */
    public void fetchCartFromCloud(OnCartSyncedListener listener) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        db.collection("carts").document(user.getUid()).get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        CartItemsWrapper wrapper = doc.toObject(CartItemsWrapper.class);
                        if (wrapper != null && wrapper.getItems() != null) {
                            localStore.saveCartItems(wrapper.getItems());
                            if (listener != null) listener.onSynced(wrapper.getItems());
                        }
                    }
                });
    }

    public static class CartItemsWrapper {
        private List<CartItem> items;
        public CartItemsWrapper() {}
        public CartItemsWrapper(List<CartItem> items) { this.items = items; }
        public List<CartItem> getItems() { return items; }
        public void setItems(List<CartItem> items) { this.items = items; }
    }

    public interface OnCartSyncedListener {
        void onSynced(List<CartItem> items);
    }

    public Call<ApiListResponse<CartItem>> getCartItems() {
        return apiService.getCartItems();
    }

    public Call<ApiResponse<CartItem>> addToCart(CartItem item) {
        return apiService.addToCart(item);
    }

    public Call<ApiResponse<CartItem>> updateCartItem(String id, CartItem item) {
        return apiService.updateCartItem(id, item);
    }

    public Call<ApiResponse<Void>> removeCartItem(String id) {
        return apiService.removeCartItem(id);
    }

    public CartLocalStore getLocalStore() {
        return localStore;
    }
}
