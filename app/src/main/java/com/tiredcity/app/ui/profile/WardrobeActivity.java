package com.tiredcity.app.ui.profile;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;
import androidx.recyclerview.widget.GridLayoutManager;
import com.tiredcity.app.R;
import com.tiredcity.app.adapter.ProductAdapter;
import com.tiredcity.app.data.local.CartLocalStore;
import com.tiredcity.app.data.model.CartItem;
import com.tiredcity.app.data.local.FavoritesLocalStore;
import com.tiredcity.app.data.model.Product;
import com.tiredcity.app.data.repository.FavoritesRepository;
import com.tiredcity.app.databinding.ActivityWardrobeBinding;
import com.tiredcity.app.ui.base.BaseActivity;
import com.tiredcity.app.ui.shop.ProductDetailActivity;
import com.tiredcity.app.utils.Constants;
import java.util.ArrayList;
import java.util.List;

public class WardrobeActivity extends BaseActivity {

    private ActivityWardrobeBinding binding;
    private ProductAdapter productAdapter;
    private FavoritesLocalStore favoritesStore;
    private FavoritesRepository favoritesRepository;
    private final List<Product> wardrobeItems = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityWardrobeBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(getString(R.string.label_wardrobe));
        }
        binding.toolbar.setNavigationOnClickListener(v -> finish());

        favoritesStore = new FavoritesLocalStore(this);
        favoritesRepository = new FavoritesRepository(favoritesStore);
        
        productAdapter = new ProductAdapter(wardrobeItems);
        productAdapter.setOnProductClickListener(new ProductAdapter.OnProductClickListener() {
            @Override
            public void onProductClick(Product product) {
                Intent intent = new Intent(WardrobeActivity.this, ProductDetailActivity.class);
                intent.putExtra(Constants.EXTRA_PRODUCT_ID, product.getId());
                startActivity(intent);
            }

            @Override
            public void onSaveToggle(Product product, boolean saved) {
                // Remove from wardrobe when un-saved
                if (!saved) removeFromWardrobe(product);
            }

            @Override
            public void onAddToCartClick(Product product) {
                // Bắt buộc chọn size -> Mở màn hình chi tiết
                Intent intent = new Intent(WardrobeActivity.this, ProductDetailActivity.class);
                intent.putExtra(Constants.EXTRA_PRODUCT_ID, product.getId());
                startActivity(intent);
                Toast.makeText(WardrobeActivity.this, "Vui lòng chọn Size trước khi thêm vào giỏ", Toast.LENGTH_SHORT).show();
            }
        });

        binding.rvSavedItems.setLayoutManager(new GridLayoutManager(this, 2));
        binding.rvSavedItems.setAdapter(productAdapter);

        binding.swipeRefresh.setColorSchemeColors(
            getResources().getColor(R.color.tc_red, getTheme()));
        binding.swipeRefresh.setOnRefreshListener(this::loadWardrobeItems);

        // Sync từ cloud về
        favoritesRepository.fetchFavoritesFromCloud(items -> runOnUiThread(this::loadWardrobeItems));

        loadWardrobeItems();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Trạng thái yêu thích có thể đã đổi ở màn hình khác (chi tiết sản phẩm, danh sách...)
        loadWardrobeItems();
    }

    private void loadWardrobeItems() {
        binding.swipeRefresh.setRefreshing(false);
        wardrobeItems.clear();
        wardrobeItems.addAll(favoritesStore.getFavorites());
        productAdapter.notifyDataSetChanged();

        if (wardrobeItems.isEmpty()) {
            binding.layoutEmpty.setVisibility(View.VISIBLE);
            binding.swipeRefresh.setVisibility(View.GONE);
        } else {
            binding.layoutEmpty.setVisibility(View.GONE);
            binding.swipeRefresh.setVisibility(View.VISIBLE);
        }
    }

    private void removeFromWardrobe(Product product) {
        wardrobeItems.remove(product);
        favoritesRepository.syncFavoritesToCloud(); // Đồng bộ sau khi xoá
        productAdapter.notifyDataSetChanged();
        if (wardrobeItems.isEmpty()) {
            binding.layoutEmpty.setVisibility(View.VISIBLE);
            binding.swipeRefresh.setVisibility(View.GONE);
        }
    }
}
