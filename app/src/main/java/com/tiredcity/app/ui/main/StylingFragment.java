package com.tiredcity.app.ui.main;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.LinearInterpolator;
import androidx.annotation.ColorInt;
import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import com.google.android.material.tabs.TabLayout;
import com.tiredcity.app.R;
import com.tiredcity.app.adapter.StylingAdapter;
import com.tiredcity.app.data.local.CartLocalStore;
import com.tiredcity.app.data.model.CartItem;
import com.tiredcity.app.data.model.CategoryItem;
import com.tiredcity.app.databinding.FragmentStylingBinding;
import com.tiredcity.app.ui.cart.CartActivity;
import com.tiredcity.app.ui.shop.CategoryActivity;
import com.tiredcity.app.ui.shop.SearchActivity;
import com.tiredcity.app.utils.ColorTaxonomy;
import java.util.ArrayList;
import java.util.List;

/**
 * Trang Danh mục: khách hàng duyệt các nhóm trang phục Việt Phục
 * (Áo Dài / Nhật Bình / Áo Tấc / Giao Lĩnh / Yếm Đào / Phụ Kiện). Chạm một danh mục → mở lưới sản phẩm.
 * 5 danh mục trang phục được chia theo MÀU — mỗi danh mục chỉ liệt kê những màu thực sự có mặt
 * trong danh mục đó (theo bảng dữ liệu sản phẩm), không phải toàn bộ bảng màu chung.
 * Phụ Kiện được chia theo loại phụ kiện (khăn, quạt, ô, mũ).
 */
public class StylingFragment extends Fragment {

    /** Key Bundle: id danh mục (vd "ÁO DÀI") để mở sẵn đúng tab khi vào từ nơi khác (Trang chủ,…). */
    public static final String ARG_CATEGORY_ID = "category_id";

    private FragmentStylingBinding binding;
    private StylingAdapter adapter;
    private boolean bannerInitialized = false;

    // Nhãn các tab (giữ in hoa từ string resource).
    private static final int[] TAB_LABELS = {
            R.string.cat_tab_ao_dai,
            R.string.cat_tab_nhat_binh,
            R.string.cat_tab_ao_tac,
            R.string.cat_tab_giao_linh,
            R.string.cat_tab_yem_dao,
            R.string.cat_tab_phu_kien
    };

    // Id danh mục ứng với từng tab theo đúng thứ tự TAB_LABELS — khớp với CategoryActivity.EXTRA_CATEGORY_ID.
    private static final String[] TAB_CATEGORY_IDS = {
            "ÁO DÀI", "NHẬT BÌNH", "ÁO TẤC", "GIAO LĨNH", "YẾM ĐÀO", "PHỤ KIỆN"
    };

    // Ảnh banner danh mục ứng với từng tab, theo đúng thứ tự TAB_CATEGORY_IDS.
    private static final int[] TAB_BANNER_IMAGES = {
            R.drawable.cat_banner_ao_dai,
            R.drawable.cat_banner_nhat_binh,
            R.drawable.cat_banner_ao_tac,
            R.drawable.cat_banner_giao_linh,
            R.drawable.cat_banner_yem_dao,
            R.drawable.cat_banner_phu_kien
    };

    // Tỷ lệ rộng/cao thật của ảnh banner danh mục (1999×787) — dùng để khung hiện FULL ảnh
    private static final float CATEGORY_BANNER_RATIO = 1999f / 787f;
    private static final long BANNER_SLIDE_DURATION = 220L;
    private static final long SHIMMER_DURATION = 900L;
    private static final long SHIMMER_DELAY = 150L;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentStylingBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setupCategoryList();
        setupCategoryBanner();
        setupTabs();
        setupHeader();
    }

    @Override
    public void onResume() {
        super.onResume();
        updateCartBadge();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (binding != null) {
            binding.frameCategoryBanner.animate().cancel();
            binding.shimmerCategoryBanner.animate().cancel();
        }
        binding = null;
    }

    // ── Header: ô tìm kiếm + quét mã + giỏ hàng ────────────────────────────────

    private void setupHeader() {
        binding.llSearch.setOnClickListener(v -> openSearch());
        binding.ibScan.setOnClickListener(v -> openSearch());
        binding.btnCart.setOnClickListener(v ->
                startActivity(new Intent(requireContext(), CartActivity.class)));
    }

    private void openSearch() {
        startActivity(new Intent(requireContext(), SearchActivity.class));
    }

    private void updateCartBadge() {
        if (binding == null) return;
        int count = 0;
        for (CartItem item : new CartLocalStore(requireContext()).getCartItems()) {
            count += item.getQuantity();
        }
        if (count <= 0) {
            binding.tvCartBadge.setVisibility(View.GONE);
        } else {
            binding.tvCartBadge.setVisibility(View.VISIBLE);
            binding.tvCartBadge.setText(count > 9 ? getString(R.string.cart_badge_overflow) : String.valueOf(count));
        }
    }

    // ── Danh sách danh mục ─────────────────────────────────────────────────────

    private void setupCategoryList() {
        adapter = new StylingAdapter(new ArrayList<>());
        adapter.setOnCategoryClickListener(item -> {
            Intent intent = new Intent(requireContext(), CategoryActivity.class);
            String displayName = item.getNameText() != null ? item.getNameText() : getString(item.getNameResId());
            intent.putExtra(CategoryActivity.EXTRA_CATEGORY_NAME, displayName);
            intent.putExtra(CategoryActivity.EXTRA_CATEGORY_ID, item.getParentCategory());
            if (item.getFilterValue() != null) {
                intent.putExtra(CategoryActivity.EXTRA_TAG_FILTER, item.getFilterValue());
            }
            startActivity(intent);
        });
        binding.rvCategories.setLayoutManager(new GridLayoutManager(requireContext(), 2));
        binding.rvCategories.setAdapter(adapter);
        // Tắt animator mặc định để không "đánh nhau" với hiệu ứng mờ+trượt lên tự vẽ trong
        // StylingAdapter (mỗi lần notifyDataSetChanged khi đổi tab).
        binding.rvCategories.setItemAnimator(null);
    }

    // ── Tab nhóm trang phục ────────────────────────────────────────────────────

    private void setupTabs() {
        TabLayout tabs = binding.tabGroups;
        for (int labelRes : TAB_LABELS) {
            tabs.addTab(tabs.newTab().setText(getString(labelRes)));
        }
        tabs.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override public void onTabSelected(TabLayout.Tab tab) {
                adapter.setItems(buildCategories(tab.getPosition()));
                binding.rvCategories.scrollToPosition(0);
                updateCategoryBanner(tab.getPosition());
            }
            @Override public void onTabUnselected(TabLayout.Tab tab) { }
            @Override public void onTabReselected(TabLayout.Tab tab) { }
        });

        // Chọn sẵn tab tương ứng với ARG_CATEGORY_ID (vd. mở từ thẻ danh mục ở Trang chủ), mặc định tab đầu.
        int startTab = resolveStartTab();
        TabLayout.Tab tab = tabs.getTabAt(startTab);
        if (tab != null) tab.select();
        adapter.setItems(buildCategories(startTab));
    }

    private int resolveStartTab() {
        Bundle args = getArguments();
        String categoryId = args != null ? args.getString(ARG_CATEGORY_ID) : null;
        if (categoryId == null) return 0;
        for (int i = 0; i < TAB_CATEGORY_IDS.length; i++) {
            if (TAB_CATEGORY_IDS[i].equals(categoryId)) return i;
        }
        return 0;
    }

    // ── Banner danh mục — 1 ảnh riêng cho tab đang chọn ────────────────────────

    /** Chỉ đo khung theo tỷ lệ ảnh gốc; ảnh thật được nạp lần đầu qua updateCategoryBanner()
     *  khi setupTabs() chọn tab mặc định (tab.select() tự gọi listener bên dưới). */
    private void setupCategoryBanner() {
        applyAspectRatio(binding.frameCategoryBanner, CATEGORY_BANNER_RATIO);
    }

    /** Đổi ảnh banner theo tab: lần đầu vào màn hình banner Áo Dài hiện ra NGAY (không trượt) —
     *  chỉ vệt sáng chéo "tráng gương" lướt qua; các lần đổi tab sau đó thì banner cũ trượt ra
     *  ngoài bên trái rồi banner mới trượt vào từ bên phải, đồng bộ ngôn ngữ hiệu ứng với các
     *  banner ở Trang chủ. */
    private void updateCategoryBanner(int tabPosition) {
        if (binding == null || tabPosition < 0 || tabPosition >= TAB_BANNER_IMAGES.length) return;
        int imageRes = TAB_BANNER_IMAGES[tabPosition];
        View banner = binding.frameCategoryBanner;

        if (!bannerInitialized) {
            bannerInitialized = true;
            binding.ivCategoryBanner.setImageResource(imageRes);
            banner.post(this::playBannerShimmer);
            return;
        }

        banner.animate().cancel();
        banner.animate()
                .translationX(-banner.getWidth())
                .setDuration(BANNER_SLIDE_DURATION)
                .setInterpolator(new AccelerateInterpolator())
                .withEndAction(() -> {
                    if (binding == null) return;
                    binding.ivCategoryBanner.setImageResource(imageRes);
                    slideBannerIn(banner);
                })
                .start();
    }

    /** Đặt banner ra ngoài mép phải rồi trượt vào vị trí — dùng chung cho lần vào màn hình đầu
     *  tiên lẫn mỗi lần đổi tab sau đó. */
    private void slideBannerIn(View banner) {
        if (binding == null) return;
        int width = banner.getWidth();
        if (width <= 0) return;
        banner.setTranslationX(width);
        banner.animate()
                .translationX(0f)
                .setDuration(BANNER_SLIDE_DURATION)
                .setInterpolator(new DecelerateInterpolator())
                .withEndAction(this::playBannerShimmer)
                .start();
    }

    /** Vệt sáng chéo lướt 1 lần qua banner — xem HomeFragment.ShimmerSweep (cùng ý tưởng). */
    private void playBannerShimmer() {
        if (binding == null) return;
        View shimmer = binding.shimmerCategoryBanner;
        int width = binding.frameCategoryBanner.getWidth();
        if (width <= 0) return;
        shimmer.animate().cancel();
        shimmer.setTranslationX(-width * 0.6f);
        shimmer.animate()
                .translationX(width * 1.3f)
                .setStartDelay(SHIMMER_DELAY)
                .setDuration(SHIMMER_DURATION)
                .setInterpolator(new LinearInterpolator())
                .start();
    }

    /** Ép container về đúng tỷ lệ rộng/cao theo bề rộng đo được thực tế (giống HomeFragment). */
    private static void applyAspectRatio(View container, float widthToHeightRatio) {
        container.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                int width = container.getWidth();
                if (width <= 0) return;
                int desiredHeight = Math.round(width / widthToHeightRatio);
                ViewGroup.LayoutParams params = container.getLayoutParams();
                if (params.height != desiredHeight) {
                    params.height = desiredHeight;
                    container.setLayoutParams(params);
                }
            }
        });
    }

    /** Danh mục con hiển thị cho từng tab. */
    private List<CategoryItem> buildCategories(int tab) {
        switch (tab) {
            case 1: return buildNhatBinhCategories();
            case 2: return buildAoTacCategories();
            case 3: return buildGiaoLinhCategories();
            case 4: return buildYemDaoCategories();
            case 5: return buildPhuKienCategories();
            default: return buildAoDaiCategories();
        }
    }

    // ── ÁO DÀI: Trắng, Xanh, Vàng, Hồng (đúng các màu có trong bảng sản phẩm) ──

    private List<CategoryItem> buildAoDaiCategories() {
        return buildColorCategories("ÁO DÀI", new Object[][]{
                {ColorTaxonomy.TRANG, R.string.desc_ao_dai_trang},
                {ColorTaxonomy.XANH,  R.string.desc_ao_dai_xanh},
                {ColorTaxonomy.VANG,  R.string.desc_ao_dai_vang},
                {ColorTaxonomy.HONG,  R.string.desc_ao_dai_hong},
        });
    }

    // ── NHẬT BÌNH: Trắng, Xanh lá, Xanh, Đỏ, Vàng ──────────────────────────────

    private List<CategoryItem> buildNhatBinhCategories() {
        return buildColorCategories("NHẬT BÌNH", new Object[][]{
                {ColorTaxonomy.TRANG,   R.string.desc_nhat_binh_trang},
                {ColorTaxonomy.XANH_LA, R.string.desc_nhat_binh_xanh_la},
                {ColorTaxonomy.XANH,    R.string.desc_nhat_binh_xanh},
                {ColorTaxonomy.DO,      R.string.desc_nhat_binh_do},
                {ColorTaxonomy.VANG,    R.string.desc_nhat_binh_vang},
        });
    }

    // ── ÁO TẤC: Xanh, Trắng, Cam, Xanh lá ──────────────────────────────────────

    private List<CategoryItem> buildAoTacCategories() {
        return buildColorCategories("ÁO TẤC", new Object[][]{
                {ColorTaxonomy.XANH,    R.string.desc_ao_tac_xanh},
                {ColorTaxonomy.TRANG,   R.string.desc_ao_tac_trang},
                {ColorTaxonomy.CAM,     R.string.desc_ao_tac_cam},
                {ColorTaxonomy.XANH_LA, R.string.desc_ao_tac_xanh_la},
        });
    }

    // ── GIAO LĨNH: Xanh lá, Vàng, Đỏ ────────────────────────────────────────────

    private List<CategoryItem> buildGiaoLinhCategories() {
        return buildColorCategories("GIAO LĨNH", new Object[][]{
                {ColorTaxonomy.XANH_LA, R.string.desc_giao_linh_xanh_la},
                {ColorTaxonomy.VANG,    R.string.desc_giao_linh_vang},
                {ColorTaxonomy.DO,      R.string.desc_giao_linh_do},
        });
    }

    // ── YẾM ĐÀO: Đỏ, Xanh lá, Vàng, Hồng ────────────────────────────────────────

    private List<CategoryItem> buildYemDaoCategories() {
        return buildColorCategories("YẾM ĐÀO", new Object[][]{
                {ColorTaxonomy.DO,      R.string.desc_yem_dao_do},
                {ColorTaxonomy.XANH_LA, R.string.desc_yem_dao_xanh_la},
                {ColorTaxonomy.VANG,    R.string.desc_yem_dao_vang},
                {ColorTaxonomy.HONG,    R.string.desc_yem_dao_hong},
        });
    }

    /**
     * Danh mục con dạng "màu" dùng chung cho các tab trang phục — mỗi tab tự khai chỉ những màu mình có.
     * {@code colorsWithDescRes[i]} = {bucket (String, khoá lọc dữ liệu), mô tả (@StringRes Integer)}.
     * Tên hiển thị lấy qua {@link ColorTaxonomy#displayNameRes}, tách riêng khỏi khoá lọc để dịch được.
     */
    private List<CategoryItem> buildColorCategories(String parentCategory, Object[][] colorsWithDescRes) {
        Context ctx = requireContext();
        List<CategoryItem> list = new ArrayList<>();
        for (int i = 0; i < colorsWithDescRes.length; i++) {
            String bucket = (String) colorsWithDescRes[i][0];
            int descRes = (int) colorsWithDescRes[i][1];
            list.add(new CategoryItem(i + 1, ColorTaxonomy.displayNameRes(bucket), descRes,
                    imageForBucket(bucket), swatchFor(ctx, bucket), parentCategory, bucket));
        }
        return list;
    }

    /** Ảnh minh hoạ riêng cho từng nhóm màu (thay ô màu phẳng) — trả về 0 nếu chưa có ảnh
     *  (StylingAdapter sẽ tự lùi về màu phẳng của swatchFor() làm nền). */
    @DrawableRes
    private static int imageForBucket(String bucket) {
        if (ColorTaxonomy.DO.equals(bucket))      return R.drawable.swatch_do;
        if (ColorTaxonomy.XANH.equals(bucket))    return R.drawable.swatch_xanh;
        if (ColorTaxonomy.VANG.equals(bucket))    return R.drawable.swatch_vang;
        if (ColorTaxonomy.TRANG.equals(bucket))   return R.drawable.swatch_trang;
        if (ColorTaxonomy.HONG.equals(bucket))    return R.drawable.swatch_hong;
        if (ColorTaxonomy.TIM.equals(bucket))     return R.drawable.swatch_tim;
        if (ColorTaxonomy.XANH_LA.equals(bucket)) return R.drawable.swatch_xanh_la;
        if (ColorTaxonomy.CAM.equals(bucket))     return R.drawable.swatch_cam;
        return 0; // Đen Huyền Bí: chưa có ảnh riêng
    }

    @ColorInt
    private int swatchFor(Context ctx, String bucket) {
        if (ColorTaxonomy.DO.equals(bucket))      return ctx.getColor(R.color.tc_swatch_do);
        if (ColorTaxonomy.XANH.equals(bucket))    return ctx.getColor(R.color.tc_swatch_xanh);
        if (ColorTaxonomy.VANG.equals(bucket))    return ctx.getColor(R.color.tc_swatch_vang);
        if (ColorTaxonomy.TRANG.equals(bucket))   return ctx.getColor(R.color.tc_swatch_trang);
        if (ColorTaxonomy.DEN.equals(bucket))     return ctx.getColor(R.color.tc_swatch_den);
        if (ColorTaxonomy.HONG.equals(bucket))    return ctx.getColor(R.color.tc_swatch_hong);
        if (ColorTaxonomy.TIM.equals(bucket))     return ctx.getColor(R.color.tc_swatch_tim);
        if (ColorTaxonomy.XANH_LA.equals(bucket)) return ctx.getColor(R.color.tc_swatch_xanh_la);
        if (ColorTaxonomy.CAM.equals(bucket))     return ctx.getColor(R.color.tc_swatch_cam);
        return ctx.getColor(R.color.tc_bg_subtle);
    }

    // ── PHỤ KIỆN: chia theo loại phụ kiện, không phải theo màu ─────────────────

    private List<CategoryItem> buildPhuKienCategories() {
        // {tên hiển thị (@StringRes), mô tả (@StringRes), giá trị lọc khớp với cột MÀU trong dữ liệu phụ kiện, ảnh minh hoạ}
        Object[][] data = {
                {R.string.phukien_khan_title,     R.string.phukien_khan_desc,     "Khăn đội đầu", R.drawable.phukien_khan},
                {R.string.phukien_quat_title,     R.string.phukien_quat_desc,     "Quạt",         R.drawable.phukien_quat},
                {R.string.phukien_oche_title,     R.string.phukien_oche_desc,     "Ô che",        R.drawable.phukien_oche},
                {R.string.phukien_mudoidau_title, R.string.phukien_mudoidau_desc, "Mũ đội đầu",   R.drawable.phukien_mudoidau},
        };
        Context ctx = requireContext();
        int[] palette = {
                ctx.getColor(R.color.tc_red),
                ctx.getColor(R.color.tc_espresso),
                ctx.getColor(R.color.tc_gold),
                ctx.getColor(R.color.tc_bg_subtle)
        };

        List<CategoryItem> list = new ArrayList<>();
        for (int i = 0; i < data.length; i++) {
            list.add(new CategoryItem(i + 1, (int) data[i][0], (int) data[i][1], (int) data[i][3],
                    palette[i % palette.length], "PHỤ KIỆN", (String) data[i][2]));
        }
        return list;
    }
}
