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
        // Luôn hiển thị một mệnh ngay lập tức để giao diện không trống:
        // ưu tiên mệnh đã lưu → tính từ hồ sơ cache → mặc định "Kim".
        String menh = resolveMenh();
        setupMenhUI(menh);
        loadRecommendedProducts(menh);

        // Nếu chưa lưu mệnh, thử lấy hồ sơ từ server để tính lại chính xác.
        if (preferenceManager.getMenh() != null) return;

        apiService.getProfile().enqueue(new Callback<com.tiredcity.app.data.model.ApiResponse<UserProfile>>() {
            @Override
            public void onResponse(Call<com.tiredcity.app.data.model.ApiResponse<UserProfile>> call,
                                   Response<com.tiredcity.app.data.model.ApiResponse<UserProfile>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()
                        && response.body().getData() != null) {
                    int birthYear = response.body().getData().getBirthYear();
                    if (birthYear <= 0) return;
                    String refined = MenhCalculator.tinhMenh(birthYear);
                    preferenceManager.setMenh(refined);
                    setupMenhUI(refined);
                    loadRecommendedProducts(refined);
                }
            }

            @Override
            public void onFailure(Call<com.tiredcity.app.data.model.ApiResponse<UserProfile>> call, Throwable t) {}
        });
    }

    /** Ưu tiên tính từ năm sinh thực tế để đảm bảo chính xác theo công thức mới. */
    private String resolveMenh() {
        UserProfile cached = preferenceManager.getUser();
        if (cached != null && cached.getBirthYear() > 0) {
            String menh = MenhCalculator.tinhMenh(cached.getBirthYear());
            // Tự sửa lại cache nếu đang lưu sai
            if (!menh.equals(preferenceManager.getMenh())) {
                preferenceManager.setMenh(menh);
                cached.setMenh(menh);
                preferenceManager.saveUser(cached);
            }
            return menh;
        }
        
        String saved = preferenceManager.getMenh();
        return (saved != null) ? saved : "Kim";
    }

    private void setupMenhUI(String menh) {
        binding.tvMenhTitle.setText(getString(R.string.menh_label, menh));
        binding.tvMenhEmoji.setText(MenhCalculator.getEmojiMenh(menh));

        // Zodiac subtitle — nếu chưa có cung hoàng đạo thì hiển thị mô tả ngũ hành.
        String zodiac = preferenceManager.getZodiac();
        binding.tvMenhSubtitle.setText(
                (zodiac != null && !zodiac.isEmpty()) ? zodiac : getString(R.string.aistyle_menh_tagline));

        // Color swatches — vòng tròn màu thực + nhãn
        binding.layoutColors.removeAllViews();
        float density = getResources().getDisplayMetrics().density;
        int dotSize = (int) (32 * density);
        int strokeColor = getResources().getColor(R.color.tc_stroke, getTheme());
        String[] colorNames = MenhCalculator.getMauHopMenh(menh);
        for (String colorName : colorNames) {
            LinearLayout swatch = new LinearLayout(this);
            swatch.setOrientation(LinearLayout.VERTICAL);
            swatch.setGravity(android.view.Gravity.CENTER_HORIZONTAL);
            LinearLayout.LayoutParams swatchLp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            swatchLp.setMarginEnd((int) (16 * density));
            swatch.setLayoutParams(swatchLp);

            android.graphics.drawable.GradientDrawable dot =
                    new android.graphics.drawable.GradientDrawable();
            dot.setShape(android.graphics.drawable.GradientDrawable.OVAL);
            dot.setColor(hexForColorName(colorName));
            dot.setStroke((int) (1 * density), strokeColor);

            View dotView = new View(this);
            dotView.setLayoutParams(new LinearLayout.LayoutParams(dotSize, dotSize));
            dotView.setBackground(dot);
            swatch.addView(dotView);

            android.widget.TextView label = new android.widget.TextView(this);
            label.setText(MenhCalculator.localizeColor(this, colorName));
            label.setTextSize(11);
            label.setTextColor(getResources().getColor(R.color.text_secondary, getTheme()));
            label.setGravity(android.view.Gravity.CENTER);
            LinearLayout.LayoutParams labelLp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            labelLp.topMargin = (int) (5 * density);
            label.setLayoutParams(labelLp);
            swatch.addView(label);

            binding.layoutColors.addView(swatch);
        }
    }

    /** Ánh xạ tên màu hợp mệnh sang mã màu hiển thị cho swatch. */
    private int hexForColorName(String name) {
        String hex;
        switch (name) {
            case "Trắng":     hex = "#FFFFFF"; break;
            case "Vàng":      hex = "#F4C430"; break;
            case "Bạc":       hex = "#CDD1D4"; break;
            case "Xám":       hex = "#9E9E9E"; break;
            case "Xanh lá":   hex = "#4CAF50"; break;
            case "Xanh lam":  hex = "#2196F3"; break;
            case "Xanh rêu":  hex = "#6B8E23"; break;
            case "Đen":       hex = "#1A1208"; break;
            case "Xanh navy": hex = "#1A2F5A"; break;
            case "Tím":       hex = "#7E57C2"; break;
            case "Đỏ":        hex = "#A80D15"; break;
            case "Hồng":      hex = "#E91E63"; break;
            case "Cam":       hex = "#FF7043"; break;
            case "Vàng đất":  hex = "#C9A45C"; break;
            case "Nâu":       hex = "#6D4C41"; break;
            case "Be":        hex = "#E8DCC6"; break;
            case "Cam nhạt":  hex = "#FFB74D"; break;
            default:          hex = "#C9A45C"; break;
        }
        return android.graphics.Color.parseColor(hex);
    }

    private void loadRecommendedProducts(String menh) {
        apiService.getRecommendedProducts(menh).enqueue(new Callback<ApiListResponse<Product>>() {
            @Override
            public void onResponse(Call<ApiListResponse<Product>> call, Response<ApiListResponse<Product>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()
                        && response.body().getData() != null && !response.body().getData().isEmpty()) {
                    recommendedAdapter.updateData(response.body().getData());
                } else {
                    // Không có backend → hiển thị gợi ý mẫu để mục không trống.
                    recommendedAdapter.updateData(buildMockSuggestions());
                }
            }

            @Override
            public void onFailure(Call<ApiListResponse<Product>> call, Throwable t) {
                recommendedAdapter.updateData(buildMockSuggestions());
            }
        });
    }

    /** Gợi ý trang phục mẫu (offline) để mục luôn có nội dung. */
    private List<Product> buildMockSuggestions() {
        String[][] data = {
            {"1", "Áo Dài Lụa Trắng",   "Lụa tơ tằm",  "850000",  "10", "4.8"},
            {"2", "Nhật Bình Vàng Đồng", "Gấm thêu kim", "1450000", "15", "4.9"},
            {"5", "Áo Tấc Trắng Ngà",    "Đũi tơ cao cấp", "990000", "0",  "4.7"},
        };
        List<Product> list = new java.util.ArrayList<>();
        for (String[] row : data) {
            Product p = new Product();
            p.setId(row[0]);
            p.setName(row[1]);
            p.setMaterial(row[2]);
            p.setPrice(Double.parseDouble(row[3]));
            p.setDiscount(Integer.parseInt(row[4]));
            p.setRating(Double.parseDouble(row[5]));
            list.add(p);
        }
        return list;
    }
}
