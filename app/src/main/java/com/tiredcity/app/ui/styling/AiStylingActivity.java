package com.tiredcity.app.ui.styling;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import androidx.recyclerview.widget.GridLayoutManager;
import android.widget.Toast;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.tiredcity.app.R;
import com.tiredcity.app.adapter.ProductPhotoCardAdapter;
import com.tiredcity.app.data.model.Product;
import com.tiredcity.app.data.model.UserProfile;
import com.tiredcity.app.data.network.ApiClient;
import com.tiredcity.app.data.network.ApiService;
import com.tiredcity.app.data.repository.FirestoreProductRepository;
import com.tiredcity.app.databinding.ActivityAiStylingBinding;
import com.tiredcity.app.ui.base.BaseActivity;
import com.tiredcity.app.utils.ColorTaxonomy;
import com.tiredcity.app.utils.GeminiStylist;
import com.tiredcity.app.utils.MenhCalculator;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AiStylingActivity extends BaseActivity {

    private ActivityAiStylingBinding binding;
    private ApiService apiService;
    private final FirestoreProductRepository firestoreRepository = new FirestoreProductRepository();
    private ProductPhotoCardAdapter recommendedAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityAiStylingBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        binding.toolbar.setNavigationOnClickListener(v -> finish());

        apiService = ApiClient.getApiService(preferenceManager.getToken());

        recommendedAdapter = new ProductPhotoCardAdapter(null);
        binding.rvSuggestions.setLayoutManager(new GridLayoutManager(this, 2));
        recommendedAdapter = new ProductAdapter(null);
        recommendedAdapter.setOnProductClickListener(new ProductAdapter.OnProductClickListener() {
            @Override
            public void onProductClick(Product product) {
                Intent intent = new Intent(AiStylingActivity.this, com.tiredcity.app.ui.shop.ProductDetailActivity.class);
                intent.putExtra(com.tiredcity.app.utils.Constants.EXTRA_PRODUCT_ID, product.getId());
                startActivity(intent);
            }

            @Override
            public void onSaveToggle(Product product, boolean saved) {}

            @Override
            public void onAddToCartClick(Product product) {
                // Bắt buộc chọn size -> Mở màn hình chi tiết
                Intent intent = new Intent(AiStylingActivity.this, com.tiredcity.app.ui.shop.ProductDetailActivity.class);
                intent.putExtra(com.tiredcity.app.utils.Constants.EXTRA_PRODUCT_ID, product.getId());
                startActivity(intent);
                Toast.makeText(AiStylingActivity.this, "Vui lòng chọn Size trước khi thêm vào giỏ", Toast.LENGTH_SHORT).show();
            }
        });
        binding.rvSuggestions.setLayoutManager(new LinearLayoutManager(this));
        binding.rvSuggestions.setAdapter(recommendedAdapter);
        binding.rvSuggestions.setNestedScrollingEnabled(false);
        recommendedAdapter.setOnProductClickListener(product -> {
            if (product.getId() == null) return;
            android.content.Intent i = new android.content.Intent(
                    AiStylingActivity.this, com.tiredcity.app.ui.shop.ProductDetailActivity.class);
            i.putExtra(com.tiredcity.app.utils.Constants.EXTRA_PRODUCT_ID, product.getId());
            startActivity(i);
        });

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

    /** Ưu tiên tính lại từ năm sinh (nạp âm đúng, chữa lành cache cũ) → mệnh đã lưu → "Kim". */
    /** Ưu tiên tính từ năm sinh thực tế để đảm bảo chính xác theo công thức mới. */
    private String resolveMenh() {
        UserProfile cached = preferenceManager.getUser();
        if (cached != null && cached.getBirthYear() > 0) {
            String fresh = MenhCalculator.tinhMenh(cached.getBirthYear());
            preferenceManager.setMenh(fresh);
            return fresh;
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
        return saved != null ? saved : "Kim";

        String saved = preferenceManager.getMenh();
        return (saved != null) ? saved : "Kim";
    }

    private void setupMenhUI(String menh) {
        binding.tvMenhTitle.setText(getString(R.string.menh_label, menh));
        binding.tvMenhEmoji.setText(MenhCalculator.getEmojiMenh(menh));

        // Tranh hero theo mệnh — nếu chưa có ảnh riêng thì ẩn ImageView,
        // để lộ nền gradient sơn mài đỏ của khung hero.
        int artwork = MenhCalculator.getMenhArtwork(menh);
        if (artwork != 0) {
            binding.imgMenhHero.setImageResource(artwork);
            binding.imgMenhHero.setVisibility(View.VISIBLE);
        } else {
            binding.imgMenhHero.setVisibility(View.GONE);
        }

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
            // Nhãn nằm trên nền đen tuyền của thẻ → dùng màu kem sáng cho nổi bật.
            label.setTextColor(getResources().getColor(R.color.tc_text_on_dark, getTheme()));
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

    /** Số gợi ý hiển thị mỗi lần, và cỡ nhóm liên quan để "Làm mới" đổi được món. */
    private static final int SUGGESTION_COUNT = 6;
    private static final int RELEVANT_POOL = 12;

    /**
     * Gợi ý trang phục = sản phẩm THẬT từ Firestore (cùng nguồn với admin, có ảnh),
     * lọc theo màu hợp mệnh. Nếu không có món hợp màu thì dùng hàng đánh giá cao nhất.
     * Firestore lỗi/rỗng mới lùi về dữ liệu mẫu offline để mục không trống.
     */
    private void loadRecommendedProducts(String menh) {
        firestoreRepository.getProducts(all -> {
            if (binding == null) return;
            if (all == null || all.isEmpty()) {
                recommendedAdapter.updateData(buildMockSuggestions());
                return;
            }

            List<Product> matched = filterByMenh(all, menh);
            // Không có SP hợp màu mệnh → vẫn ưu tiên hàng thật đánh giá cao (có ảnh).
            List<Product> pool = matched.isEmpty() ? sortByRelevance(new ArrayList<>(all)) : matched;

            // Lấy nhóm liên quan nhất rồi xáo trộn để "Làm mới" cho ra bộ gợi ý khác nhau.
            List<Product> topPool = pool.subList(0, Math.min(pool.size(), RELEVANT_POOL));
            List<Product> picks = new ArrayList<>(topPool);
            Collections.shuffle(picks);
            List<Product> shown = new ArrayList<>(picks.subList(0, Math.min(picks.size(), SUGGESTION_COUNT)));
            recommendedAdapter.updateData(shown);
            loadAiStylingTip(menh, shown);
        });
    }

    /**
     * Sinh "Lời khuyên phong cách" bằng Gemini (gọi thẳng từ app) theo mệnh + màu hợp
     * mệnh + sản phẩm đang gợi ý. Chưa dán khoá / lỗi / không có mạng → giữ lời khuyên tĩnh.
     */
    private void loadAiStylingTip(String menh, List<Product> products) {
        if (binding == null) return;

        // Chưa cấu hình khoá Gemini → giữ nguyên lời khuyên mặc định, không hiện "đang tạo".
        if (!GeminiStylist.isConfigured()) {
            binding.tvStylingTip.setText(getString(R.string.aistyle_tip_desc));
            return;
        }

        binding.tvStylingTip.setText(getString(R.string.aistyle_tip_loading));

        List<String> productNames = new ArrayList<>();
        if (products != null) {
            for (Product p : products) {
                if (p.getName() != null && !p.getName().trim().isEmpty()) productNames.add(p.getName());
            }
        }
        List<String> colors = new ArrayList<>(Arrays.asList(MenhCalculator.getMauHopMenh(menh)));

        GeminiStylist.suggest(menh, colors, productNames, new GeminiStylist.Callback() {
            @Override
            public void onAdvice(String advice) {
                runOnUiThread(() -> {
                    if (binding != null) binding.tvStylingTip.setText(advice);
                });
            }

            @Override
            public void onError() {
                runOnUiThread(() -> {
                    if (binding != null) binding.tvStylingTip.setText(getString(R.string.aistyle_tip_desc));
                });
            }
        });
    }

    /** Lọc sản phẩm có màu thuộc nhóm màu hợp mệnh, đã xếp theo độ liên quan. */
    private List<Product> filterByMenh(List<Product> all, String menh) {
        String[] buckets = menhColorBuckets(menh);
        List<Product> out = new ArrayList<>();
        for (Product p : all) {
            for (String bucket : buckets) {
                if (ColorTaxonomy.matchesBucket(p.getColors(), bucket)) {
                    out.add(p);
                    break;
                }
            }
        }
        return sortByRelevance(out);
    }

    /** Nhóm màu chuẩn (ColorTaxonomy) tương ứng với màu hợp từng mệnh. */
    private static String[] menhColorBuckets(String menh) {
        if (menh == null) return new String[0];
        switch (menh) {
            case "Kim":  return new String[]{ColorTaxonomy.TRANG, ColorTaxonomy.VANG};
            case "Mộc":  return new String[]{ColorTaxonomy.XANH_LA, ColorTaxonomy.XANH};
            case "Thủy": return new String[]{ColorTaxonomy.DEN, ColorTaxonomy.XANH, ColorTaxonomy.TIM};
            case "Hỏa":  return new String[]{ColorTaxonomy.DO, ColorTaxonomy.HONG, ColorTaxonomy.CAM, ColorTaxonomy.TIM};
            case "Thổ":  return new String[]{ColorTaxonomy.VANG, ColorTaxonomy.CAM};
            default:     return new String[0];
        }
    }

    /** Đánh giá cao trước, rồi tới khuyến mãi sâu hơn. */
    private List<Product> sortByRelevance(List<Product> list) {
        Collections.sort(list, (a, b) -> {
            int byRating = Double.compare(b.getRating(), a.getRating());
            if (byRating != 0) return byRating;
            return b.getDiscount() - a.getDiscount();
        });
        return list;
    }

    /** Gợi ý trang phục mẫu (offline) — chỉ dùng khi Firestore lỗi/rỗng. */
    private List<Product> buildMockSuggestions() {
        String[][] data = {
            {"1", "Áo Dài Lụa Trắng",   "Lụa tơ tằm",  "850000",  "10", "4.8"},
            {"2", "Nhật Bình Vàng Đồng", "Gấm thêu kim", "1450000", "15", "4.9"},
            {"5", "Áo Tấc Trắng Ngà",    "Đũi tơ cao cấp", "990000", "0",  "4.7"},
        };
        List<Product> list = new ArrayList<>();
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
