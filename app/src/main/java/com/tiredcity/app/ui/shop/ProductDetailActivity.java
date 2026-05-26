package com.tiredcity.app.ui.shop;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;
import com.bumptech.glide.Glide;
import com.tiredcity.app.R;
import com.tiredcity.app.data.local.CartLocalStore;
import com.tiredcity.app.data.model.ApiResponse;
import com.tiredcity.app.data.model.CartItem;
import com.tiredcity.app.data.model.Product;
import com.tiredcity.app.data.network.ApiClient;
import com.tiredcity.app.data.repository.ProductRepository;
import com.tiredcity.app.databinding.ActivityProductDetailBinding;
import com.tiredcity.app.ui.base.BaseActivity;
import com.tiredcity.app.ui.cart.CartActivity;
import com.tiredcity.app.utils.Constants;
import com.tiredcity.app.utils.PriceUtils;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ProductDetailActivity extends BaseActivity {

    public static final String EXTRA_PRODUCT_ID = Constants.EXTRA_PRODUCT_ID;

    private ActivityProductDetailBinding binding;
    private ProductRepository productRepository;
    private CartLocalStore cartLocalStore;
    private Product currentProduct;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityProductDetailBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Back button (layout uses ImageButton, not Toolbar)
        binding.btnBack.setOnClickListener(v -> finish());

        String productId = getIntent().getStringExtra(EXTRA_PRODUCT_ID);
        productRepository = new ProductRepository(ApiClient.getApiService(preferenceManager.getToken()));
        cartLocalStore     = new CartLocalStore(this);

        binding.btnAddToCart.setOnClickListener(v -> addToCart());
        binding.btnBuyNow.setOnClickListener(v -> {
            addToCart();
            openCart();
        });

        loadProduct(productId);
    }

    private void loadProduct(String productId) {
        if (productId == null) { finish(); return; }
        productRepository.getProductById(productId).enqueue(new Callback<ApiResponse<Product>>() {
            @Override
            public void onResponse(Call<ApiResponse<Product>> call, Response<ApiResponse<Product>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    currentProduct = response.body().getData();
                    bindProduct(currentProduct);
                } else {
                    Toast.makeText(ProductDetailActivity.this, "Không tải được sản phẩm", Toast.LENGTH_SHORT).show();
                    finish();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<Product>> call, Throwable t) {
                Toast.makeText(ProductDetailActivity.this, "Lỗi mạng", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void bindProduct(Product product) {
        binding.tvProductName.setText(product.getName());

        // tv_price is the ID in the layout (not tv_product_price)
        binding.tvPrice.setText(PriceUtils.format(product.getEffectivePrice()));

        // tv_material and tv_description are the IDs in the layout
        binding.tvMaterial.setText(product.getMaterial() != null ? product.getMaterial() : "");
        binding.tvOrigin.setText(product.getOrigin() != null ? product.getOrigin() : "Việt Nam");
        binding.tvDescription.setText(product.getDescription() != null ? product.getDescription() : "");
        binding.rbRating.setRating((float) product.getRating());

        // Load first image into the ViewPager2 by setting a simple image adapter
        String imageUrl = product.getFirstImage();
        if (imageUrl != null && !imageUrl.isEmpty()) {
            // Create a minimal single-image adapter
            binding.vpProductImages.setAdapter(new androidx.recyclerview.widget.RecyclerView.Adapter<androidx.recyclerview.widget.RecyclerView.ViewHolder>() {
                @androidx.annotation.NonNull
                @Override
                public androidx.recyclerview.widget.RecyclerView.ViewHolder onCreateViewHolder(
                        @androidx.annotation.NonNull android.view.ViewGroup parent, int viewType) {
                    android.widget.ImageView iv = new android.widget.ImageView(parent.getContext());
                    iv.setLayoutParams(new android.view.ViewGroup.LayoutParams(
                        android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                        android.view.ViewGroup.LayoutParams.MATCH_PARENT));
                    iv.setScaleType(android.widget.ImageView.ScaleType.CENTER_CROP);
                    return new androidx.recyclerview.widget.RecyclerView.ViewHolder(iv) {};
                }

                @Override
                public void onBindViewHolder(@androidx.annotation.NonNull androidx.recyclerview.widget.RecyclerView.ViewHolder holder, int position) {
                    Glide.with(holder.itemView.getContext())
                        .load(imageUrl)
                        .centerCrop()
                        .placeholder(R.color.bg_subtle)
                        .into((android.widget.ImageView) holder.itemView);
                }

                @Override
                public int getItemCount() {
                    return (product.getImages() != null) ? product.getImages().size() : 1;
                }
            });
        }
    }

    private void addToCart() {
        if (currentProduct == null) return;
        CartItem item = new CartItem(currentProduct, 1);
        cartLocalStore.addItem(item);
        Toast.makeText(this, "Đã thêm vào giỏ hàng 🛒", Toast.LENGTH_SHORT).show();
    }

    private void openCart() {
        startActivity(new Intent(this, CartActivity.class));
    }
}
