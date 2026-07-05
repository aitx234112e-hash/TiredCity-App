package com.tiredcity.app.ui.shop;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.tiredcity.app.data.model.ApiListResponse;
import com.tiredcity.app.data.model.ApiResponse;
import com.tiredcity.app.data.model.Product;
import com.tiredcity.app.data.model.Review;
import com.tiredcity.app.data.repository.ProductRepository;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * ProductDetailViewModel - Kiến trúc MVVM chuẩn mực.
 * Hợp nhất logic Realtime Firestore và REST API fallback.
 */
public class ProductDetailViewModel extends ViewModel {
    private final ProductRepository repository;

    private final MutableLiveData<Product> product = new MutableLiveData<>();
    private final MutableLiveData<List<Review>> reviews = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<List<Product>> relatedProducts = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();

    public ProductDetailViewModel(ProductRepository repository) {
        this.repository = repository;
    }

    public LiveData<Product> getProduct() { return product; }
    public LiveData<List<Review>> getReviews() { return reviews; }
    public LiveData<List<Product>> getRelatedProducts() { return relatedProducts; }
    public LiveData<Boolean> getIsLoading() { return isLoading; }
    public LiveData<String> getErrorMessage() { return errorMessage; }

    public void loadProduct(String productId) {
        if (productId == null || productId.isEmpty()) return;
        
        isLoading.setValue(true);
        // 1. Ưu tiên nạp từ Firestore Realtime
        repository.getProductByIdFromFirestore(productId, new ProductRepository.OnProductLoadedListener() {
            @Override
            public void onSuccess(Product p) {
                if (p != null) {
                    product.setValue(p);
                    loadExtraData(p);
                    isLoading.setValue(false);
                } else {
                    // 2. Fallback sang API cũ nếu Firestore chưa có (dữ liệu migrate chưa xong)
                    loadFromApi(productId);
                }
            }

            @Override
            public void onError(String message) {
                loadFromApi(productId);
            }
        });
    }

    private void loadFromApi(String productId) {
        repository.getProductById(productId).enqueue(new Callback<ApiResponse<Product>>() {
            @Override
            public void onResponse(Call<ApiResponse<Product>> call, Response<ApiResponse<Product>> response) {
                isLoading.setValue(false);
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    Product p = response.body().getData();
                    product.setValue(p);
                    loadExtraData(p);
                } else {
                    errorMessage.setValue("Lỗi tải thông tin sản phẩm");
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<Product>> call, Throwable t) {
                isLoading.setValue(false);
                errorMessage.setValue("Lỗi kết nối máy chủ");
            }
        });
    }

    private void loadExtraData(Product p) {
        loadReviews(p.getId());
        loadRelated(p);
    }

    private void loadReviews(String productId) {
        repository.getProductReviewsFromFirestore(productId, new ProductRepository.OnReviewsLoadedListener() {
            @Override
            public void onSuccess(List<Review> list) {
                reviews.setValue(list);
            }

            @Override
            public void onError(String message) {
                // Fallback to API
                repository.getProductReviews(productId).enqueue(new Callback<ApiListResponse<Review>>() {
                    @Override
                    public void onResponse(Call<ApiListResponse<Review>> call, Response<ApiListResponse<Review>> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            reviews.setValue(response.body().getData());
                        }
                    }
                    @Override public void onFailure(Call<ApiListResponse<Review>> call, Throwable t) {}
                });
            }
        });
    }

    private void loadRelated(Product p) {
        repository.getProductsByCategoryFromFirestore(p.getCategory(), new ProductRepository.OnProductsLoadedListener() {
            @Override
            public void onSuccess(List<Product> list) {
                List<Product> filtered = new ArrayList<>();
                for (Product item : list) {
                    if (!item.getId().equals(p.getId())) filtered.add(item);
                }
                relatedProducts.setValue(filtered);
            }
            @Override public void onError(String message) {}
        });
    }
}
