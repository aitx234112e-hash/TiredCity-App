package com.tiredcity.app.ui.main;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
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
import java.util.ArrayList;
import java.util.List;

/**
 * Trang Danh mục: khách hàng duyệt các nhóm trang phục Việt Phục theo đối tượng
 * (Nữ / Nam / Trẻ em / Phụ kiện / Lễ hội). Chạm một danh mục → mở lưới sản phẩm.
 */
public class StylingFragment extends Fragment {

    private FragmentStylingBinding binding;
    private StylingAdapter adapter;

    // Nhãn các tab (giữ in hoa từ string resource).
    private static final int[] TAB_LABELS = {
            R.string.cat_tab_women,
            R.string.cat_tab_men,
            R.string.cat_tab_kids,
            R.string.cat_tab_accessory,
            R.string.cat_tab_festival
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
            binding.tvCartBadge.setText(count > 9 ? "9+" : String.valueOf(count));
        }
    }

    // ── Danh sách danh mục ─────────────────────────────────────────────────────

    private void setupCategoryList() {
        adapter = new StylingAdapter(new ArrayList<>());
        adapter.setOnCategoryClickListener(item -> {
            Intent intent = new Intent(requireContext(), CategoryActivity.class);
            intent.putExtra(CategoryActivity.EXTRA_CATEGORY_NAME, item.getNameText());
            startActivity(intent);
        });
        binding.rvCategories.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvCategories.setAdapter(adapter);
    }

    // ── Tab nhóm đối tượng ─────────────────────────────────────────────────────

    private void setupTabs() {
        TabLayout tabs = binding.tabGroups;
        for (int labelRes : TAB_LABELS) {
            tabs.addTab(tabs.newTab().setText(getString(labelRes)));
        }
        tabs.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override public void onTabSelected(TabLayout.Tab tab) {
                adapter.setItems(buildCategories(tab.getPosition()));
                binding.rvCategories.scrollToPosition(0);
            }
            @Override public void onTabUnselected(TabLayout.Tab tab) { }
            @Override public void onTabReselected(TabLayout.Tab tab) { }
        });
        // Hiển thị danh mục cho tab đầu tiên.
        adapter.setItems(buildCategories(0));
    }

    /** Danh mục Việt Phục cho từng nhóm đối tượng. */
    private List<CategoryItem> buildCategories(int tab) {
        @ColorInt int red    = requireContext().getColor(R.color.tc_red);
        @ColorInt int dark   = requireContext().getColor(R.color.tc_espresso);
        @ColorInt int gold   = requireContext().getColor(R.color.tc_gold);
        @ColorInt int subtle = requireContext().getColor(R.color.tc_bg_subtle);
        int[] palette = { red, dark, gold, subtle };

        String[][] data;
        switch (tab) {
            case 1: // NAM
                data = new String[][]{
                        {"ÁO NGŨ THÂN", "Ngũ thân tay chẽn, tay thụng,…"},
                        {"ÁO TẤC",       "Áo Tấc nam, Áo Bào,…"},
                        {"KHĂN ĐÓNG",    "Khăn đóng, Khăn xếp,…"},
                        {"PHỤ KIỆN",     "Hia, Guốc mộc, Quạt,…"}
                };
                break;
            case 2: // TRẺ EM
                data = new String[][]{
                        {"ÁO DÀI BÉ",    "Áo dài bé gái, bé trai,…"},
                        {"SET LỄ PHỤC",  "Bộ lễ phục trẻ em,…"},
                        {"NHẬT BÌNH BÉ", "Nhật Bình thu nhỏ,…"},
                        {"PHỤ KIỆN",     "Mấn bé, Nón, Hài,…"}
                };
                break;
            case 3: // PHỤ KIỆN
                data = new String[][]{
                        {"TRÂM & THOA",  "Trâm cài tóc, Thoa,…"},
                        {"MẤN & KHĂN",   "Mấn đội đầu, Khăn vành,…"},
                        {"TÚI & VÍ",     "Túi thêu, Ví cầm tay,…"},
                        {"TRANG SỨC",    "Vòng, Nhẫn, Kiềng,…"}
                };
                break;
            case 4: // LỄ HỘI
                data = new String[][]{
                        {"ÁO DÀI CƯỚI",      "Áo dài cưới đôi,…"},
                        {"NHẬT BÌNH ĐẠI LỄ", "Đại lễ, Hoàng hậu,…"},
                        {"ÁO BÀO",           "Áo bào, Long bào,…"},
                        {"CỔ PHỤC",          "Việt phục trình diễn,…"}
                };
                break;
            default: // NỮ
                data = new String[][]{
                        {"ÁO DÀI",    "Áo dài truyền thống, cách tân,…"},
                        {"NHẬT BÌNH", "Nhật Bình, Bào hoàng hậu,…"},
                        {"ÁO TẤC",    "Áo Tấc nữ, Giao lĩnh,…"},
                        {"PHỤ KIỆN",  "Mấn, Trâm cài, Khăn vành,…"}
                };
                break;
        }

        List<CategoryItem> list = new ArrayList<>();
        for (int i = 0; i < data.length; i++) {
            list.add(new CategoryItem(i + 1, data[i][0], data[i][1], 0, palette[i % palette.length]));
        }
        return list;
    }
}
