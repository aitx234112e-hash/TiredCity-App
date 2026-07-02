package com.tiredcity.app.ui.shop;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.Toast;
import androidx.annotation.ColorInt;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import com.tiredcity.app.R;
import com.tiredcity.app.adapter.ProductAdapter;
import com.tiredcity.app.data.local.CartLocalStore;
import com.tiredcity.app.data.model.ApiListResponse;
import com.tiredcity.app.data.model.CartItem;
import com.tiredcity.app.data.model.Product;
import com.tiredcity.app.data.network.ApiClient;
import com.tiredcity.app.data.repository.ProductRepository;
import com.tiredcity.app.databinding.ActivityCategoryBinding;
import com.tiredcity.app.ui.base.BaseActivity;
import com.tiredcity.app.ui.cart.CartActivity;
import com.tiredcity.app.utils.ColorTaxonomy;
import com.tiredcity.app.utils.Constants;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CategoryActivity extends BaseActivity {

    public static final String EXTRA_CATEGORY_ID   = "category_id";
    public static final String EXTRA_CATEGORY_NAME = "category_name";
    /** Nhóm màu (5 danh mục trang phục) hoặc phân loại phụ kiện (Phụ Kiện) — lọc client-side theo Product.getColors(). */
    public static final String EXTRA_TAG_FILTER    = "tag_filter";

    private ActivityCategoryBinding binding;
    private ProductRepository productRepository;
    private ProductAdapter productAdapter;
    private GridLayoutManager gridManager;
    private String categoryId;
    private String tagFilter;
    private int spanCount = 2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityCategoryBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        categoryId = getIntent().getStringExtra(EXTRA_CATEGORY_ID);
        tagFilter = getIntent().getStringExtra(EXTRA_TAG_FILTER);
        String categoryName = getIntent().getStringExtra(EXTRA_CATEGORY_NAME);
        binding.tvTitle.setText(categoryName != null ? categoryName
                : getString(R.string.category_default_title));

        binding.btnBack.setOnClickListener(v -> finish());
        binding.btnCart.setOnClickListener(v ->
                startActivity(new Intent(this, CartActivity.class)));

        setupProductGrid();
        setupViewModeToggle();
        setupFilter();

        loadProducts();
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateCartBadge();
    }

    // ── Lưới sản phẩm ──────────────────────────────────────────────────────────

    private void setupProductGrid() {
        productAdapter = new ProductAdapter(null);
        productAdapter.setFillWidth(true);
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

        gridManager = new GridLayoutManager(this, spanCount);
        binding.rvProducts.setLayoutManager(gridManager);
        binding.rvProducts.setAdapter(productAdapter);

        binding.swipeRefresh.setColorSchemeColors(
                ContextCompat.getColor(this, R.color.tc_red));
        binding.swipeRefresh.setOnRefreshListener(this::loadProducts);
    }

    // ── Chế độ xem 1 / 2 / 3 cột ──────────────────────────────────────────────

    private void setupViewModeToggle() {
        binding.btnView1.setOnClickListener(v -> setSpanCount(1));
        binding.btnView2.setOnClickListener(v -> setSpanCount(2));
        binding.btnView3.setOnClickListener(v -> setSpanCount(3));
        highlightViewMode();
    }

    private void setSpanCount(int span) {
        if (span == spanCount) return;
        spanCount = span;
        gridManager.setSpanCount(span);
        productAdapter.notifyDataSetChanged();
        highlightViewMode();
    }

    private void highlightViewMode() {
        @ColorInt int active   = ContextCompat.getColor(this, R.color.tc_red);
        @ColorInt int inactive = ContextCompat.getColor(this, R.color.tc_text_secondary);
        tint(binding.btnView1, spanCount == 1 ? active : inactive);
        tint(binding.btnView2, spanCount == 2 ? active : inactive);
        tint(binding.btnView3, spanCount == 3 ? active : inactive);
    }

    private void tint(ImageButton button, @ColorInt int color) {
        button.setColorFilter(color);
    }

    // ── Bộ lọc ─────────────────────────────────────────────────────────────────

    private void setupFilter() {
        binding.btnFilter.setOnClickListener(v ->
                Toast.makeText(this, R.string.filter_coming_soon, Toast.LENGTH_SHORT).show());
    }

    // ── Giỏ hàng badge ─────────────────────────────────────────────────────────

    private void updateCartBadge() {
        int count = 0;
        for (CartItem item : new CartLocalStore(this).getCartItems()) {
            count += item.getQuantity();
        }
        if (count <= 0) {
            binding.tvCartBadge.setVisibility(android.view.View.GONE);
        } else {
            binding.tvCartBadge.setVisibility(android.view.View.VISIBLE);
            binding.tvCartBadge.setText(count > 9 ? "9+" : String.valueOf(count));
        }
    }

    // ── Tải sản phẩm ───────────────────────────────────────────────────────────

    private void loadProducts() {
        binding.swipeRefresh.setRefreshing(true);
        productRepository = productRepository != null ? productRepository
                : new ProductRepository(ApiClient.getApiService(preferenceManager.getToken()));
        productRepository.getProducts(1, 40, categoryId, null)
            .enqueue(new Callback<ApiListResponse<Product>>() {
                @Override
                public void onResponse(Call<ApiListResponse<Product>> call, Response<ApiListResponse<Product>> response) {
                    binding.swipeRefresh.setRefreshing(false);
                    List<Product> products = null;
                    if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                        products = response.body().getData();
                    }
                    List<Product> source = (products != null && !products.isEmpty())
                            ? products : buildMockProducts(categoryId);
                    productAdapter.updateData(applyTagFilter(source));
                }

                @Override
                public void onFailure(Call<ApiListResponse<Product>> call, Throwable t) {
                    binding.swipeRefresh.setRefreshing(false);
                    // Không có mạng/backend → hiển thị dữ liệu mẫu để xem trước lưới.
                    productAdapter.updateData(applyTagFilter(buildMockProducts(categoryId)));
                }
            });
    }

    /**
     * Lọc theo nhóm màu (5 danh mục trang phục, qua {@link ColorTaxonomy}) hoặc theo phân loại
     * phụ kiện (Phụ Kiện, so khớp trực tiếp) — cả hai đều dựa trên {@link Product#getColors()}.
     */
    private List<Product> applyTagFilter(List<Product> products) {
        if (tagFilter == null || products == null) return products;
        boolean matchByColorBucket = !"PHỤ KIỆN".equals(categoryId);

        List<Product> filtered = new ArrayList<>();
        for (Product p : products) {
            List<String> colors = p.getColors();
            boolean matches = matchByColorBucket
                    ? ColorTaxonomy.matchesBucket(colors, tagFilter)
                    : (colors != null && colors.contains(tagFilter));
            if (matches) filtered.add(p);
        }
        return filtered;
    }

    /** Dữ liệu mẫu cho chế độ xem trước offline khi chưa có API/backend trả về. */
    private List<Product> buildMockProducts(String categoryId) {
        if ("ÁO DÀI".equals(categoryId))    return buildAoDaiMock();
        if ("NHẬT BÌNH".equals(categoryId)) return buildNhatBinhMock();
        if ("ÁO TẤC".equals(categoryId))    return buildAoTacMock();
        if ("GIAO LĨNH".equals(categoryId)) return buildGiaoLinhMock();
        if ("YẾM ĐÀO".equals(categoryId))   return buildYemDaoMock();
        if ("PHỤ KIỆN".equals(categoryId))  return buildPhuKienMock();
        return buildGenericMock();
    }

    /** Áo Dài mẫu, màu lấy đúng theo bảng danh mục sản phẩm hiện có. */
    private List<Product> buildAoDaiMock() {
        List<Product> list = new ArrayList<>();
        list.add(mockProduct("ad1", "Khói Trắng Kết Duyên",  "ÁO DÀI", 2890000, "Trắng"));
        list.add(mockProduct("ad2", "Lam Lụa Cố Trạch",      "ÁO DÀI", 1590000, "Xanh"));
        list.add(mockProduct("ad3", "Kim Vũ Phong Hoa",      "ÁO DÀI", 1750000, "Vàng"));
        list.add(mockProduct("ad4", "Hồng Trần Mộc Dược",    "ÁO DÀI", 1290000, "Hồng"));
        list.add(mockProduct("ad5", "Lục Thuỷ Hoàng Lan",    "ÁO DÀI", 1450000, "Xanh", "Hồng"));
        list.add(mockProduct("ad6", "Nguyệt Cầm Phấn Hồng",  "ÁO DÀI", 1350000, "Hồng"));
        list.add(mockProduct("ad7", "Phấn Hoa Cổ Điển",      "ÁO DÀI", 1200000, "Trắng"));
        return list;
    }

    /** Nhật Bình mẫu, màu lấy đúng theo bảng danh mục sản phẩm hiện có. */
    private List<Product> buildNhatBinhMock() {
        List<Product> list = new ArrayList<>();
        list.add(mockProduct("nb1",  "Xích Bào Đối Ấn",       "NHẬT BÌNH", 3490000, "Trắng"));
        list.add(mockProduct("nb2",  "Thạch Lam Hoàng Cung",  "NHẬT BÌNH", 3290000, "Xanh lá"));
        list.add(mockProduct("nb3",  "Lục Triều Tiểu Yến",    "NHẬT BÌNH", 2890000, "Xanh lá"));
        list.add(mockProduct("nb4",  "Hoàng Triều Kim Tuyến", "NHẬT BÌNH", 2190000, "Xanh"));
        list.add(mockProduct("nb5",  "Vọng Nguyệt Lam Cung",  "NHẬT BÌNH", 2690000, "Đỏ"));
        list.add(mockProduct("nb6",  "Nhật Bình Lam Vũ",      "NHẬT BÌNH", 2990000, "Xanh"));
        list.add(mockProduct("nb7",  "Lục Yên Lam Bửu",       "NHẬT BÌNH", 2750000, "Xanh lá"));
        list.add(mockProduct("nb8",  "Tử Vân Yên Thảo",       "NHẬT BÌNH", 2450000, "Vàng"));
        list.add(mockProduct("nb9",  "Trầm Hồng Cổ Các",      "NHẬT BÌNH", 2150000, "Vàng"));
        list.add(mockProduct("nb10", "Kim Trần Mộc Dược",     "NHẬT BÌNH", 1890000, "Vàng"));
        return list;
    }

    /** Áo Tấc mẫu, màu lấy đúng theo bảng danh mục sản phẩm hiện có. */
    private List<Product> buildAoTacMock() {
        List<Product> list = new ArrayList<>();
        list.add(mockProduct("at1", "Lục Ngọc Vấn Khăn",  "ÁO TẤC", 1590000, "Xanh"));
        list.add(mockProduct("at2", "Ngọc Vũ Yên Sa",     "ÁO TẤC", 1890000, "Trắng"));
        list.add(mockProduct("at3", "Mộc Vân Thổ Xà",     "ÁO TẤC", 1290000, "Cam"));
        list.add(mockProduct("at4", "Thanh Long Cổ Trấn", "ÁO TẤC", 1450000, "Xanh lá"));
        list.add(mockProduct("at5", "Lục Y Phù Quạt",     "ÁO TẤC", 1690000, "Xanh"));
        list.add(mockProduct("at6", "Tơ Ngà Vấn Nguyệt",  "ÁO TẤC", 1750000, "Trắng"));
        return list;
    }

    /** Giao Lĩnh mẫu, màu lấy đúng theo bảng danh mục sản phẩm hiện có. */
    private List<Product> buildGiaoLinhMock() {
        List<Product> list = new ArrayList<>();
        list.add(mockProduct("gl1", "Bạch Sa Liên Vũ",      "GIAO LĨNH", 2290000, "Xanh lá"));
        list.add(mockProduct("gl2", "Lam Ngọc Cổ Trấn",     "GIAO LĨNH", 2490000, "Xanh lá"));
        list.add(mockProduct("gl3", "Lục Trúc Vân Khúc",    "GIAO LĨNH", 2450000, "Xanh lá"));
        list.add(mockProduct("gl4", "Kim Sắc Hoàng Triều",  "GIAO LĨNH", 2290000, "Vàng"));
        list.add(mockProduct("gl5", "Cam Giao Lĩnh Bào",    "GIAO LĨNH", 1890000, "Đỏ"));
        list.add(mockProduct("gl6", "Hắc Kim Mẫu Đơn",      "GIAO LĨNH", 2690000, "Đỏ"));
        return list;
    }

    /** Yếm Đào mẫu, màu lấy đúng theo bảng danh mục sản phẩm hiện có. */
    private List<Product> buildYemDaoMock() {
        List<Product> list = new ArrayList<>();
        list.add(mockProduct("yd1", "Sương Mai Bạch Vũ",     "YẾM ĐÀO", 2290000, "Đỏ"));
        list.add(mockProduct("yd2", "Trúc Lục Khuê Phòng",   "YẾM ĐÀO", 2690000, "Xanh lá"));
        list.add(mockProduct("yd3", "Yên Hoa Bạch Liên",     "YẾM ĐÀO", 1990000, "Vàng", "Đỏ"));
        list.add(mockProduct("yd4", "Thanh Lam Trì Liên",    "YẾM ĐÀO", 2350000, "Xanh lá"));
        list.add(mockProduct("yd5", "Bích Lam Cẩm Tú",       "YẾM ĐÀO", 2450000, "Vàng"));
        list.add(mockProduct("yd6", "Dạ Kim Mẫu Đơn",        "YẾM ĐÀO", 2890000, "Hồng", "Xanh lá"));
        return list;
    }

    /** Phụ kiện mẫu — cột "màu" ở đây thực chất là loại phụ kiện (khăn/quạt/ô/mũ). */
    private List<Product> buildPhuKienMock() {
        List<Product> list = new ArrayList<>();
        list.add(mockProduct("pk1", "Vấn Nguyệt Bạch Vân Cẩm",       "PHỤ KIỆN", 350000, "Khăn đội đầu"));
        list.add(mockProduct("pk2", "Nón Dâu Cổ Phong",              "PHỤ KIỆN", 80000,  "Mũ đội đầu"));
        list.add(mockProduct("pk3", "Quạt Xếp Khổng Tước Khai Bình", "PHỤ KIỆN", 420000, "Quạt"));
        list.add(mockProduct("pk4", "Lọng Tán Trường An",            "PHỤ KIỆN", 650000, "Ô che"));
        list.add(mockProduct("pk5", "Khăn Vấn Thổ Mộc",              "PHỤ KIỆN", 890000, "Khăn đội đầu"));
        list.add(mockProduct("pk6", "Vân Kiên Đám Mây",              "PHỤ KIỆN", 120000, "Ô che"));
        list.add(mockProduct("pk7", "Quan Phượng Vũ Triều Thiên",    "PHỤ KIỆN", 990000, "Mũ đội đầu"));
        list.add(mockProduct("pk8", "Ngự Lạp Kim Sa",                "PHỤ KIỆN", 750000, "Mũ đội đầu"));
        list.add(mockProduct("pk9", "Lọng Ngự Tán Kim Vân",          "PHỤ KIỆN", 550000, "Ô che"));
        return list;
    }

    private Product mockProduct(String id, String name, String category, double price, String... colors) {
        Product p = new Product();
        p.setId(id);
        p.setName(name);
        p.setCategory(category);
        p.setColors(Arrays.asList(colors));
        p.setMaterial("PHỤ KIỆN".equals(category) ? "Thêu tay truyền thống" : "Lụa tơ tằm");
        p.setPrice(price);
        p.setRating(4.7);
        return p;
    }

    /** Dữ liệu mẫu Việt Phục chung cho các danh mục khác khi chưa có API. */
    private List<Product> buildGenericMock() {
        String[][] data = {
            {"1", "Áo Ngũ Thân Tay Chẽn",  "Gấm dệt hoa",  "1250000", "15", "4.8"},
            {"2", "Áo Tấc Nam Truyền Thống","Đũi tơ tằm",   "980000",  "10", "4.7"},
            {"3", "Áo Dài Nam Cách Tân",    "Lụa Hà Đông",  "850000",  "0",  "4.6"},
            {"4", "Khăn Đóng Gấm Thêu",     "Gấm thêu chỉ", "320000",  "20", "4.9"},
            {"5", "Áo Ngũ Thân Tay Thụng",  "Tơ sống",      "1450000", "12", "4.8"},
            {"6", "Áo Bào Nhật Bình Nam",   "Gấm cao cấp",  "2350000", "18", "5.0"},
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
