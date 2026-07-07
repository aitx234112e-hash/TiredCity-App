package com.tiredcity.app.data.repository;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.tiredcity.app.data.local.FavoritesLocalStore;
import com.tiredcity.app.data.model.Product;
import java.util.List;

public class FavoritesRepository {
    private final FavoritesLocalStore localStore;
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    public FavoritesRepository(FavoritesLocalStore localStore) {
        this.localStore = localStore;
    }

    public void syncFavoritesToCloud() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        List<Product> items = localStore.getFavorites();
        db.collection("favorites").document(user.getUid()).set(new FavoritesWrapper(items));
    }

    public void fetchFavoritesFromCloud(OnFavoritesSyncedListener listener) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        db.collection("favorites").document(user.getUid()).get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        FavoritesWrapper wrapper = doc.toObject(FavoritesWrapper.class);
                        if (wrapper != null && wrapper.getItems() != null) {
                            // Cập nhật local
                            for (Product p : wrapper.getItems()) {
                                if (!localStore.isFavorite(p.getId())) {
                                    localStore.toggleFavorite(p);
                                }
                            }
                            if (listener != null) listener.onSynced(localStore.getFavorites());
                        }
                    }
                });
    }

    public static class FavoritesWrapper {
        private List<Product> items;
        public FavoritesWrapper() {}
        public FavoritesWrapper(List<Product> items) { this.items = items; }
        public List<Product> getItems() { return items; }
        public void setItems(List<Product> items) { this.items = items; }
    }

    public interface OnFavoritesSyncedListener {
        void onSynced(List<Product> items);
    }
}
