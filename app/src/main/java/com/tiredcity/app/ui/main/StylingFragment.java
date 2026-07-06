package com.tiredcity.app.ui.main;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.viewpager2.widget.ViewPager2;
import com.google.android.material.tabs.TabLayout;
import com.tiredcity.app.R;
import com.tiredcity.app.adapter.StylingAdapter;
import com.tiredcity.app.adapter.StylingCarouselAdapter;
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
 * Đầu trang có carousel ảnh trang phục THẬT của danh mục đang chọn (Horizontal Scroll Section).
 */
public class StylingFragment extends Fragment {

    /** Key Bundle: id danh mục (vd "ÁO DÀI") để mở sẵn đúng tab khi vào từ nơi khác (Trang chủ,…). */
    public static final String ARG_CATEGORY_ID = "category_id";

    private FragmentStylingBinding binding;
    private StylingAdapter adapter;
    private StylingCarouselAdapter carouselAdapter;

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

    // Ảnh trang phục THẬT cho carousel, 5 ảnh/danh mục, theo đúng thứ tự TAB_CATEGORY_IDS.
    // Ảnh "dm_*" lấy từ bộ ảnh D:\DANH MỤC (ưu tiên đứng trước); những danh mục chưa đủ 5 ảnh
    // được bù thêm bằng ảnh carousel cũ (carousel_*) của chính danh mục đó.
    private static final int[][] CAROUSEL_IMAGES = {
            {R.drawable.dm_aodai_1, R.drawable.dm_aodai_2, R.drawable.dm_aodai_3,
                    R.drawable.dm_aodai_4, R.drawable.carousel_aodai_5},
            {R.drawable.dm_nhatbinh_1, R.drawable.dm_nhatbinh_2, R.drawable.dm_nhatbinh_3,
                    R.drawable.dm_nhatbinh_4, R.drawable.dm_nhatbinh_5},
            {R.drawable.dm_aotac_1, R.drawable.dm_aotac_2, R.drawable.dm_aotac_3,
                    R.drawable.dm_aotac_4, R.drawable.carousel_aotac_5},
            {R.drawable.dm_giaolinh_1, R.drawable.dm_giaolinh_2, R.drawable.dm_giaolinh_3,
                    R.drawable.carousel_giaolinh_4, R.drawable.carousel_giaolinh_5},
            {R.drawable.dm_yemdao_1, R.drawable.dm_yemdao_2, R.drawable.dm_yemdao_3,
                    R.drawable.dm_yemdao_4, R.drawable.carousel_yemdao_5},
            {R.drawable.dm_phukien_1, R.drawable.dm_phukien_2, R.drawable.dm_phukien_3,
                    R.drawable.dm_phukien_4, R.drawable.carousel_phukien_5},
    };

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
        setupCarousel();
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

    // ── Carousel ảnh trang phục (Horizontal Scroll Section — banner ngang full-bleed) ──────

    /** Mỗi trang là 1 ảnh full màn hình, không viền/bo góc, cuộn ngang như 1 banner quảng cáo. */
    private void setupCarousel() {
        carouselAdapter = new StylingCarouselAdapter(imagesForTab(0));

        ViewPager2 pager = binding.vpCategoryCarousel;
        pager.setAdapter(carouselAdapter);
        pager.setOffscreenPageLimit(3);

        binding.dotsCategoryCarousel.attachTo(pager);
    }

    private void updateCarousel(int tabPosition) {
        carouselAdapter.updateData(imagesForTab(tabPosition));
        binding.vpCategoryCarousel.setCurrentItem(0, false);
    }

    private static List<Integer> imagesForTab(int tab) {
        int[] images = CAROUSEL_IMAGES[tab];
        List<Integer> list = new ArrayList<>(images.length);
        for (int image : images) list.add(image);
        return list;
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
                updateCarousel(tab.getPosition());
            }
            @Override public void onTabUnselected(TabLayout.Tab tab) { }
            @Override public void onTabReselected(TabLayout.Tab tab) { }
        });

        // Chọn sẵn tab tương ứng với ARG_CATEGORY_ID (vd. mở từ thẻ danh mục ở Trang chủ), mặc định tab đầu.
        int startTab = resolveStartTab();
        TabLayout.Tab tab = tabs.getTabAt(startTab);
        if (tab != null) tab.select();
        adapter.setItems(buildCategories(startTab));
        updateCarousel(startTab);
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
                {ColorTaxonomy.TRANG, R.string.desc_ao_dai_trang, R.drawable.real_aodai_trang},
                {ColorTaxonomy.XANH,  R.string.desc_ao_dai_xanh,  R.drawable.real_aodai_xanh},
                {ColorTaxonomy.VANG,  R.string.desc_ao_dai_vang,  R.drawable.real_aodai_vang},
                {ColorTaxonomy.HONG,  R.string.desc_ao_dai_hong,  R.drawable.real_aodai_hong},
        });
    }

    // ── NHẬT BÌNH: Trắng, Xanh lá, Xanh, Đỏ, Vàng ──────────────────────────────

    private List<CategoryItem> buildNhatBinhCategories() {
        return buildColorCategories("NHẬT BÌNH", new Object[][]{
                {ColorTaxonomy.TRANG,   R.string.desc_nhat_binh_trang,   R.drawable.real_nhatbinh_trang},
                {ColorTaxonomy.XANH_LA, R.string.desc_nhat_binh_xanh_la, R.drawable.real_nhatbinh_xanhla},
                {ColorTaxonomy.XANH,    R.string.desc_nhat_binh_xanh,    R.drawable.real_nhatbinh_xanh},
                {ColorTaxonomy.DO,      R.string.desc_nhat_binh_do,      R.drawable.real_nhatbinh_do},
                {ColorTaxonomy.VANG,    R.string.desc_nhat_binh_vang,    R.drawable.real_nhatbinh_vang},
        });
    }

    // ── ÁO TẤC: Xanh, Trắng, Cam, Xanh lá ──────────────────────────────────────

    private List<CategoryItem> buildAoTacCategories() {
        return buildColorCategories("ÁO TẤC", new Object[][]{
                {ColorTaxonomy.XANH,    R.string.desc_ao_tac_xanh,    R.drawable.real_aotac_xanh},
                {ColorTaxonomy.TRANG,   R.string.desc_ao_tac_trang,   R.drawable.real_aotac_trang},
                {ColorTaxonomy.CAM,     R.string.desc_ao_tac_cam,     R.drawable.real_aotac_cam},
                {ColorTaxonomy.XANH_LA, R.string.desc_ao_tac_xanh_la, R.drawable.real_aotac_xanhla},
        });
    }

    // ── GIAO LĨNH: Xanh lá, Vàng, Đỏ ────────────────────────────────────────────

    private List<CategoryItem> buildGiaoLinhCategories() {
        return buildColorCategories("GIAO LĨNH", new Object[][]{
                {ColorTaxonomy.XANH_LA, R.string.desc_giao_linh_xanh_la, R.drawable.real_giaolinh_xanhla},
                {ColorTaxonomy.VANG,    R.string.desc_giao_linh_vang,    R.drawable.real_giaolinh_vang},
                {ColorTaxonomy.DO,      R.string.desc_giao_linh_do,      R.drawable.real_giaolinh_do},
        });
    }

    // ── YẾM ĐÀO: Đỏ, Xanh lá, Vàng, Hồng ────────────────────────────────────────

    private List<CategoryItem> buildYemDaoCategories() {
        return buildColorCategories("YẾM ĐÀO", new Object[][]{
                {ColorTaxonomy.DO,      R.string.desc_yem_dao_do,      R.drawable.real_yemdao_do},
                {ColorTaxonomy.XANH_LA, R.string.desc_yem_dao_xanh_la, R.drawable.real_yemdao_xanhla},
                {ColorTaxonomy.VANG,    R.string.desc_yem_dao_vang,    R.drawable.real_yemdao_vang},
                {ColorTaxonomy.HONG,    R.string.desc_yem_dao_hong,    R.drawable.real_yemdao_hong},
        });
    }

    /**
     * Danh mục con dạng "màu" dùng chung cho các tab trang phục — mỗi tab tự khai chỉ những màu mình có.
     * {@code colorsWithDescRes[i]} = [bucket (String, khoá lọc dữ liệu), mô tả (@StringRes Integer),
     * ảnh trang phục THẬT đúng danh mục + đúng màu (@DrawableRes Integer, lấy từ thư mục DANH MỤC)].
     * Tên hiển thị lấy qua {@link ColorTaxonomy#displayNameRes}, tách riêng khỏi khoá lọc để dịch được.
     */
    private List<CategoryItem> buildColorCategories(String parentCategory, Object[][] colorsWithDescRes) {
        Context ctx = requireContext();
        List<CategoryItem> list = new ArrayList<>();
        for (int i = 0; i < colorsWithDescRes.length; i++) {
            String bucket = (String) colorsWithDescRes[i][0];
            int descRes = (int) colorsWithDescRes[i][1];
            int imageRes = (int) colorsWithDescRes[i][2];
            list.add(new CategoryItem(i + 1, ColorTaxonomy.displayNameRes(bucket), descRes,
                    imageRes, swatchFor(ctx, bucket), parentCategory, bucket));
        }
        return list;
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
        // Cả 4 ảnh phụ kiện đều là ảnh THẬT tương đối tối/rực (cam lọng, đỏ mão, tím quạt, khăn
        // nhiều màu) — dùng toàn màu tối cho placeholderColor để StylingAdapter.contrastColorFor
        // luôn chọn chữ TRẮNG, tránh chữ than đậm khó đọc trên ảnh như trước (tc_gold/tc_bg_subtle
        // có độ chói cao khiến bị tính nhầm là nền sáng).
        int[] palette = {
                ctx.getColor(R.color.tc_red),
                ctx.getColor(R.color.tc_espresso),
                ctx.getColor(R.color.tc_red_deep),
                ctx.getColor(R.color.tc_noir)
        };

        List<CategoryItem> list = new ArrayList<>();
        for (int i = 0; i < data.length; i++) {
            list.add(new CategoryItem(i + 1, (int) data[i][0], (int) data[i][1], (int) data[i][3],
                    palette[i % palette.length], "PHỤ KIỆN", (String) data[i][2]));
        }
        return list;
    }
}
