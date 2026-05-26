package com.tiredcity.app.ui.shop;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import androidx.recyclerview.widget.GridLayoutManager;
import com.tiredcity.app.adapter.ProductAdapter;
import com.tiredcity.app.data.model.ApiListResponse;
import com.tiredcity.app.data.model.Product;
import com.tiredcity.app.data.network.ApiClient;
import com.tiredcity.app.data.repository.ProductRepository;
import com.tiredcity.app.databinding.ActivityCategoryBinding;
import com.tiredcity.app.ui.base.BaseActivity;
import com.tiredcity.app.utils.Constants;
import java.util.List;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CategoryActivity extends BaseActivity {

    public static final String EXTRA_CATEGORY_ID   = "category_id";
    public static final String EXTRA_CATEGORY_NAME = "category_name";

    private ActivityCategoryBinding binding;
    private ProductRepository productRepository;
    private ProductAdapter productAdapter;
    private String categoryId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityCategoryBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        categoryId = getIntent().getStringExtra(EXTRA_CATEGORY_ID);
        String categoryName = getIntent().getStringExtra(EXTRA_CATEGORY_NAME);

        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(categoryName != null ? categoryName : "Danh mục");
        }
        binding.toolbar.setNavigationOnClickListener(v -> finish());

        productRepository = new ProductRepository(ApiClient.getApiService(preferenceManager.getToken()));

        productAdapter = new ProductAdapter(null);
        productAdapter.setOnProductClickListener(new ProductAdapter.OnProductClickListener() {
            @Override
            public void onProductClick(Product product) {
                Intent intent = new Intent(CategoryActivity.this, ProductDetailActivity.class);
                intent.putExtra(Constants.EXTRA_PRODUCT_ID, product.getId());
                startActivity(intent);
            }

            @Override
            public void onSaveToggle(Product product, boolean saved) {}
        });

        binding.rvProducts.setLayoutManager(new GridLayoutManager(this, 2));
        binding.rvProducts.setAdapter(productAdapter);

        binding.swipeRefresh.setColorSchemeColors(
            getResources().getColor(com.tiredcity.app.R.color.brand_gold, getTheme()));
        binding.swipeRefresh.setOnRefreshListener(this::loadProducts);

        loadProducts();
    }

    private void loadProducts() {
        binding.swipeRefresh.setRefreshing(true);
        productRepository.getProducts(1, 40, categoryId, null)
            .enqueue(new Callback<ApiListResponse<Product>>() {
                @Override
                public void onResponse(Call<ApiListResponse<Product>> call, Response<ApiListResponse<Product>> response) {
                    binding.swipeRefresh.setRefreshing(false);
                    if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                        List<Product> products = response.body().getData();
                        productAdapter.updateData(products);
                    }
                }

                @Override
                public void onFailure(Call<ApiListResponse<Product>> call, Throwable t) {
                    binding.swipeRefresh.setRefreshing(false);
                }
            });
    }
}
