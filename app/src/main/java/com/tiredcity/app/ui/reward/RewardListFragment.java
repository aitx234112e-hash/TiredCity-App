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
 * - Tab có voucher (TẤT CẢ, SINH NHẬT) → hiển thị danh sách thẻ voucher.
 * - Tab chưa có voucher (LAST CHANCE, NỔI BẬT) → hiển thị empty state.
 */
public class RewardListFragment extends Fragment {

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
        adapter.setOnRewardClickListener(this::openVoucherDetail);

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

    /** Mở trang chi tiết voucher (không dùng hạng thành viên). */
    private void openVoucherDetail(Reward reward) {
        Intent intent = new Intent(requireContext(), VoucherDetailActivity.class);
        intent.putExtra(VoucherDetailActivity.EXTRA_TITLE, reward.getTitle());
        intent.putExtra(VoucherDetailActivity.EXTRA_SUBTITLE, reward.getSubtitle());
        intent.putExtra(VoucherDetailActivity.EXTRA_BANNER, reward.getBannerRes());
        intent.putExtra(VoucherDetailActivity.EXTRA_CODE, reward.getCode());
        startActivity(intent);
    }

    /** Dữ liệu mẫu offline cho bản demo giao diện. */
    private List<Reward> buildRewards(int category) {
        List<Reward> list = new ArrayList<>();
        if (category == CATEGORY_ALL || category == CATEGORY_BIRTHDAY) {
            list.add(new Reward(
                    getString(R.string.reward_voucher_birthday_title),
                    getString(R.string.reward_voucher_birthday_subtitle),
                    R.drawable.banner_1,
                    0,
                    getString(R.string.barcode_code_birthday)));
        }
        // Voucher Tri Ân — BST Áo Dài / Nhật Bình (không dùng hạng thành viên).
        if (category == CATEGORY_ALL || category == CATEGORY_FEATURED) {
            list.add(new Reward(
                    getString(R.string.reward_voucher_tri_an_title),
                    getString(R.string.reward_voucher_tri_an_subtitle),
                    R.drawable.banner_2,
                    0,
                    getString(R.string.barcode_code_tri_an)));
        }
        // LAST CHANCE → để trống (empty state).
        return list;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
