package com.tiredcity.app.ui.profile;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import androidx.recyclerview.widget.GridLayoutManager;
import com.tiredcity.app.adapter.ProductAdapter;
import com.tiredcity.app.data.local.CartLocalStore;
import com.tiredcity.app.data.model.Product;
import com.tiredcity.app.databinding.ActivityWardrobeBinding;
import com.tiredcity.app.ui.base.BaseActivity;
import com.tiredcity.app.ui.shop.ProductDetailActivity;
import com.tiredcity.app.utils.Constants;
import java.util.ArrayList;
import java.util.List;

public class WardrobeActivity extends BaseActivity {

    private ActivityWardrobeBinding binding;
    private ProductAdapter productAdapter;
    private final List<Product> wardrobeItems = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityWardrobeBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Tủ đồ của tôi");
        }
        binding.toolbar.setNavigationOnClickListener(v -> finish());

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
        });

        binding.rvSavedItems.setLayoutManager(new GridLayoutManager(this, 2));
        binding.rvSavedItems.setAdapter(productAdapter);

        binding.swipeRefresh.setColorSchemeColors(
            getResources().getColor(com.tiredcity.app.R.color.brand_gold, getTheme()));
        binding.swipeRefresh.setOnRefreshListener(this::loadWardrobeItems);

        loadWardrobeItems();
    }

    private void loadWardrobeItems() {
        binding.swipeRefresh.setRefreshing(false);
        // Wardrobe items are stored locally via ProductAdapter's saved IDs.
        // In a real implementation, fetch saved IDs from server.
        wardrobeItems.clear();
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
        productAdapter.notifyDataSetChanged();
        if (wardrobeItems.isEmpty()) {
            binding.layoutEmpty.setVisibility(View.VISIBLE);
            binding.swipeRefresh.setVisibility(View.GONE);
        }
    }
}
