package com.tiredcity.app.ui.shop;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.tiredcity.app.data.model.ApiListResponse;
import com.tiredcity.app.data.model.ApiResponse;
import com.tiredcity.app.data.model.Product;
import com.tiredcity.app.data.model.Review;
import com.tiredcity.app.data.repository.ProductRepository;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.ArrayList;
import java.util.Date;
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
    private final MutableLiveData<Boolean> isSubmitting = new MutableLiveData<>(false);
    private final MutableLiveData<Boolean> isSubmitSuccess = new MutableLiveData<>(false);
    private final MutableLiveData<Boolean> canUserReview = new MutableLiveData<>(false);

    public ProductDetailViewModel(ProductRepository repository) {
        this.repository = repository;
    }

    public LiveData<Product> getProduct() { return product; }
    public LiveData<List<Review>> getReviews() { return reviews; }
    public LiveData<List<Product>> getRelatedProducts() { return relatedProducts; }
    public LiveData<Boolean> getIsLoading() { return isLoading; }
    public LiveData<String> getErrorMessage() { return errorMessage; }
    public LiveData<Boolean> getIsSubmitting() { return isSubmitting; }
    public LiveData<Boolean> getIsSubmitSuccess() { return isSubmitSuccess; }
    public LiveData<Boolean> getCanUserReview() { return canUserReview; }

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
        checkPurchaseStatus(p.getId());
    }

    private void checkPurchaseStatus(String productId) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            canUserReview.setValue(false);
            return;
        }

        repository.checkUserPurchasedProduct(user.getUid(), productId, new ProductRepository.OnCheckPurchaseListener() {
            @Override
            public void onResult(boolean hasPurchased) {
                canUserReview.setValue(hasPurchased);
            }

            @Override
            public void onError(String message) {
                canUserReview.setValue(false);
            }
        });
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

    public void submitReview(float rating, String comment) {
        Product p = product.getValue();
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        if (p == null) {
            errorMessage.setValue("Không xác định được sản phẩm");
            return;
        }

        if (user == null) {
            errorMessage.setValue("Vui lòng đăng nhập để gửi nhận xét");
            return;
        }

        if (rating <= 0) {
            errorMessage.setValue("Vui lòng chọn số sao đánh giá");
            return;
        }

        if (comment == null || comment.trim().isEmpty()) {
            errorMessage.setValue("Vui lòng nhập nội dung nhận xét");
            return;
        }

        isSubmitting.setValue(true);
        Review review = new Review();
        review.setProductId(p.getId());
        review.setUserId(user.getUid());
        review.setUserName(user.getDisplayName() != null ? user.getDisplayName() : "Người dùng");
        review.setUserAvatarUrl(user.getPhotoUrl() != null ? user.getPhotoUrl().toString() : "");
        review.setRating(rating);
        review.setComment(comment);
        review.setCreatedAt(new Date());

        repository.addReviewToFirestore(review, new ProductRepository.OnActionCompleteListener() {
            @Override
            public void onSuccess() {
                isSubmitting.setValue(false);
                isSubmitSuccess.setValue(true);
                // Reset state success sau khi đã thông báo xong (tuỳ logic UI)
            }

            @Override
            public void onError(String message) {
                isSubmitting.setValue(false);
                errorMessage.setValue("Lỗi gửi nhận xét: " + message);
            }
        });
    }
}
