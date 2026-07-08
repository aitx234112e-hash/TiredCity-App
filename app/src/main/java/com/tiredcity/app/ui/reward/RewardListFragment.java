package com.tiredcity.app.ui.reward;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.tiredcity.app.R;
import com.tiredcity.app.adapter.RewardAdapter;
import com.tiredcity.app.data.model.Reward;
import com.tiredcity.app.databinding.FragmentRewardListBinding;
import java.util.ArrayList;
import java.util.List;

/**
 * Một tab trong trang "Ưu đãi".
 * Mỗi voucher có banner riêng, nội dung chi tiết riêng và mã vạch riêng.
 */
public class RewardListFragment extends Fragment implements RewardAdapter.OnRewardClickListener {

    private static final String ARG_CATEGORY = "arg_category";

    // Loại tab — quyết định nội dung hiển thị.
    public static final int CATEGORY_ALL        = 0;
    public static final int CATEGORY_BIRTHDAY   = 1;
    public static final int CATEGORY_LAST_CHANCE = 2;
    public static final int CATEGORY_FEATURED   = 3;

    private FragmentRewardListBinding binding;
    private final RewardAdapter adapter = new RewardAdapter(new ArrayList<>());
    private int category = CATEGORY_ALL;

    public static RewardListFragment newInstance(int category) {
        RewardListFragment fragment = new RewardListFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_CATEGORY, category);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentRewardListBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if (getArguments() != null) {
            category = getArguments().getInt(ARG_CATEGORY, CATEGORY_ALL);
        }
        initView();
        initData();
    }

    private void initView() {
        binding.rvRewards.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvRewards.setAdapter(adapter);
        adapter.setOnRewardClickListener(this);

        binding.btnExplore.setOnClickListener(v ->
                Toast.makeText(requireContext(), R.string.reward_empty_title, Toast.LENGTH_SHORT).show());
    }

    private void initData() {
        List<Reward> rewards = buildRewards(category);
        if (rewards.isEmpty()) {
            showEmptyState();
        } else {
            showRewards(rewards);
        }
    }

    private void showRewards(List<Reward> rewards) {
        binding.rvRewards.setVisibility(View.VISIBLE);
        binding.layoutEmpty.setVisibility(View.GONE);
        adapter.updateRewards(rewards);
    }

    private void showEmptyState() {
        binding.rvRewards.setVisibility(View.GONE);
        binding.layoutEmpty.setVisibility(View.VISIBLE);
    }

    /** Bấm vào thẻ → xem thông tin chi tiết của ưu đãi. */
    @Override
    public void onOpenDetail(Reward reward) {
        Intent intent = new Intent(requireContext(), VoucherDetailActivity.class);
        intent.putExtra(VoucherDetailActivity.EXTRA_TITLE, reward.getTitle());
        intent.putExtra(VoucherDetailActivity.EXTRA_SUBTITLE, reward.getSubtitle());
        intent.putExtra(VoucherDetailActivity.EXTRA_BANNER, reward.getBannerRes());
        intent.putExtra(VoucherDetailActivity.EXTRA_CODE, reward.getCode());
        intent.putExtra(VoucherDetailActivity.EXTRA_VALIDITY, reward.getValidity());
        intent.putExtra(VoucherDetailActivity.EXTRA_DESC, reward.getDescription());
        startActivity(intent);
    }

    /** Bấm "Sử dụng ngay" ngay trên thẻ → mở mã vạch của đúng voucher đó. */
    @Override
    public void onUseNow(Reward reward) {
        Intent intent = new Intent(requireContext(), BarcodeActivity.class);
        intent.putExtra(BarcodeActivity.EXTRA_CODE, reward.getCode());
        intent.putExtra(BarcodeActivity.EXTRA_TITLE, reward.getTitle());
        startActivity(intent);
    }

    /** Dữ liệu mẫu offline cho bản demo giao diện. */
    private List<Reward> buildRewards(int category) {
        List<Reward> list = new ArrayList<>();

        // SINH NHẬT — Birthday TO YOU.
        if (category == CATEGORY_ALL || category == CATEGORY_BIRTHDAY) {
            list.add(new Reward(
                    getString(R.string.reward_voucher_birthday_title),
                    getString(R.string.reward_voucher_birthday_subtitle),
                    R.drawable.reward_birthday,
                    getString(R.string.barcode_code_birthday),
                    getString(R.string.reward_voucher_birthday_validity),
                    getString(R.string.reward_voucher_birthday_desc)));
        }

        // LAST CHANCE — sắp hết hạn: giảm 30% áo dài lụa & mua 2 tặng 1.
        if (category == CATEGORY_ALL || category == CATEGORY_LAST_CHANCE) {
            list.add(new Reward(
                    getString(R.string.reward_voucher_sale30_title),
                    getString(R.string.reward_voucher_sale30_subtitle),
                    R.drawable.reward_giam30,
                    getString(R.string.barcode_code_sale30),
                    getString(R.string.reward_voucher_sale30_validity),
                    getString(R.string.reward_voucher_sale30_desc)));
            list.add(new Reward(
                    getString(R.string.reward_voucher_buy2get1_title),
                    getString(R.string.reward_voucher_buy2get1_subtitle),
                    R.drawable.reward_mua2tang1,
                    getString(R.string.barcode_code_buy2get1),
                    getString(R.string.reward_voucher_buy2get1_validity),
                    getString(R.string.reward_voucher_buy2get1_desc)));
        }

        // NỔI BẬT — Voucher Tri Ân & quà tặng độc quyền cho đơn trên 1 triệu.
        if (category == CATEGORY_ALL || category == CATEGORY_FEATURED) {
            list.add(new Reward(
                    getString(R.string.reward_voucher_tri_an_title),
                    getString(R.string.reward_voucher_tri_an_subtitle),
                    R.drawable.reward_tri_an,
                    getString(R.string.barcode_code_tri_an),
                    getString(R.string.reward_voucher_tri_an_validity),
                    getString(R.string.voucher_detail_desc)));
            list.add(new Reward(
                    getString(R.string.reward_voucher_gift1m_title),
                    getString(R.string.reward_voucher_gift1m_subtitle),
                    R.drawable.reward_qua_tang,
                    getString(R.string.barcode_code_gift1m),
                    getString(R.string.reward_voucher_gift1m_validity),
                    getString(R.string.reward_voucher_gift1m_desc)));
        }
        return list;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
