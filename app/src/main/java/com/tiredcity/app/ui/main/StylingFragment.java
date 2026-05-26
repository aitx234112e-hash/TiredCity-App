package com.tiredcity.app.ui.main;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.tiredcity.app.R;
import com.tiredcity.app.adapter.ProductAdapter;
import com.tiredcity.app.data.model.Product;
import com.tiredcity.app.databinding.FragmentStylingBinding;
import com.tiredcity.app.ui.shop.ProductDetailActivity;
import com.tiredcity.app.ui.styling.AiStylingActivity;
import com.tiredcity.app.ui.styling.ChatBotActivity;
import com.tiredcity.app.utils.Constants;
import java.util.ArrayList;
import java.util.List;

public class StylingFragment extends Fragment {

    private FragmentStylingBinding binding;

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

        binding.cvAiStyling.setOnClickListener(v -> openAiStyling());
        binding.cvChatbot.setOnClickListener(v -> openChatBot());

        setupRecentRecommendations();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    private void openAiStyling() {
        startActivity(new Intent(requireContext(), AiStylingActivity.class));
    }

    private void openChatBot() {
        startActivity(new Intent(requireContext(), ChatBotActivity.class));
    }

    private void setupRecentRecommendations() {
        // Mock recent items — real data comes from ProductRepository
        List<Product> recent = buildMockRecentItems();
        ProductAdapter adapter = new ProductAdapter(recent);
        adapter.setOnProductClickListener(new ProductAdapter.OnProductClickListener() {
            @Override
            public void onProductClick(Product product) {
                Intent intent = new Intent(requireContext(), ProductDetailActivity.class);
                intent.putExtra(Constants.EXTRA_PRODUCT_ID, product.getId());
                startActivity(intent);
            }

            @Override
            public void onSaveToggle(Product product, boolean saved) {}
        });

        binding.rvRecommendations.setLayoutManager(
            new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false));
        binding.rvRecommendations.setAdapter(adapter);
    }

    private List<Product> buildMockRecentItems() {
        String[][] data = {
            {"1", "Áo Dài Lụa Trắng", "Lụa tơ tằm", "850000", "0",  "4.8"},
            {"2", "Áo Tấc Gấm",       "Gấm thêu",   "650000", "10", "4.6"},
            {"3", "Khăn Đóng",         "Lụa mềm",    "250000", "0",  "4.3"},
        };
        List<Product> list = new ArrayList<>();
        for (String[] row : data) {
            Product p = new Product();
            p.setId(row[0]); p.setName(row[1]); p.setMaterial(row[2]);
            p.setPrice(Double.parseDouble(row[3]));
            p.setDiscount(Integer.parseInt(row[4]));
            p.setRating(Double.parseDouble(row[5]));
            list.add(p);
        }
        return list;
    }
}
