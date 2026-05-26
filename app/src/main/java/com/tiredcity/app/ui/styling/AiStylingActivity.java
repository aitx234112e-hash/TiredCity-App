package com.tiredcity.app.ui.styling;

import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.tiredcity.app.R;
import com.tiredcity.app.adapter.ProductAdapter;
import com.tiredcity.app.data.model.ApiListResponse;
import com.tiredcity.app.data.model.Product;
import com.tiredcity.app.data.model.UserProfile;
import com.tiredcity.app.data.network.ApiClient;
import com.tiredcity.app.data.network.ApiService;
import com.tiredcity.app.databinding.ActivityAiStylingBinding;
import com.tiredcity.app.ui.base.BaseActivity;
import com.tiredcity.app.utils.MenhCalculator;
import java.util.List;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AiStylingActivity extends BaseActivity {

    private ActivityAiStylingBinding binding;
    private ApiService apiService;
    private ProductAdapter recommendedAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityAiStylingBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        binding.toolbar.setNavigationOnClickListener(v -> finish());

        apiService = ApiClient.getApiService(preferenceManager.getToken());

        recommendedAdapter = new ProductAdapter(null);
        binding.rvSuggestions.setLayoutManager(new LinearLayoutManager(this));
        binding.rvSuggestions.setAdapter(recommendedAdapter);

        binding.btnRefreshSuggestions.setOnClickListener(v -> loadUserProfileAndRecommend());

        loadUserProfileAndRecommend();
    }

    private void loadUserProfileAndRecommend() {
        // Use saved menh if available
        String savedMenh = preferenceManager.getMenh();
        if (savedMenh != null) {
            setupMenhUI(savedMenh);
            loadRecommendedProducts(savedMenh);
            return;
        }

        // Otherwise fetch profile to calculate menh
        apiService.getProfile().enqueue(new Callback<com.tiredcity.app.data.model.ApiResponse<UserProfile>>() {
            @Override
            public void onResponse(Call<com.tiredcity.app.data.model.ApiResponse<UserProfile>> call,
                                   Response<com.tiredcity.app.data.model.ApiResponse<UserProfile>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    UserProfile profile = response.body().getData();
                    int birthYear = profile.getBirthYear();
                    String menh = (birthYear > 0)
                            ? MenhCalculator.tinhMenh(birthYear)
                            : MenhCalculator.tinhMenh(2000);
                    preferenceManager.setMenh(menh);
                    setupMenhUI(menh);
                    loadRecommendedProducts(menh);
                }
            }

            @Override
            public void onFailure(Call<com.tiredcity.app.data.model.ApiResponse<UserProfile>> call, Throwable t) {}
        });
    }

    private void setupMenhUI(String menh) {
        binding.tvMenhTitle.setText("Mệnh " + menh);
        binding.tvMenhEmoji.setText(MenhCalculator.getEmojiMenh(menh));

        // Zodiac subtitle
        String zodiac = preferenceManager.getZodiac();
        if (zodiac != null) {
            binding.tvMenhSubtitle.setText(zodiac);
        }

        // Color labels
        binding.layoutColors.removeAllViews();
        String[] colorNames = MenhCalculator.getMauHopMenh(menh);
        for (String colorName : colorNames) {
            android.widget.TextView chip = new android.widget.TextView(this);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
            lp.setMarginEnd(8);
            chip.setLayoutParams(lp);
            chip.setText(colorName);
            chip.setPadding(16, 8, 16, 8);
            chip.setTextSize(12);
            chip.setTextColor(getResources().getColor(R.color.brand_dark, getTheme()));
            chip.setBackgroundResource(R.drawable.bg_rounded_surface);
            binding.layoutColors.addView(chip);
        }
    }

    private void loadRecommendedProducts(String menh) {
        apiService.getRecommendedProducts(menh).enqueue(new Callback<ApiListResponse<Product>>() {
            @Override
            public void onResponse(Call<ApiListResponse<Product>> call, Response<ApiListResponse<Product>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    List<Product> products = response.body().getData();
                    recommendedAdapter.updateData(products);
                }
            }

            @Override
            public void onFailure(Call<ApiListResponse<Product>> call, Throwable t) {}
        });
    }
}
