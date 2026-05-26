package com.tiredcity.app.ui.onboarding;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;
import com.tbuonomo.viewpagerdotsindicator.DotsIndicator;
import com.tiredcity.app.R;
import com.tiredcity.app.databinding.ActivityOnboardingBinding;
import com.tiredcity.app.ui.auth.LoginActivity;
import com.tiredcity.app.ui.base.BaseActivity;

import java.util.Arrays;
import java.util.List;

public class OnboardingActivity extends BaseActivity {

    private ActivityOnboardingBinding binding;

    private static final List<OnboardingPage> PAGES = Arrays.asList(
        new OnboardingPage("🏛️", "Khám phá Việt Phục",
            "Trải nghiệm vẻ đẹp truyền thống của trang phục Việt Nam qua bộ sưu tập được chế tác tinh xảo từ làng nghề lâu đời."),
        new OnboardingPage("🎨", "Phong cách theo mệnh",
            "AI của chúng tôi gợi ý trang phục hợp mệnh Ngũ Hành và cung hoàng đạo của bạn, mang lại may mắn và vẻ đẹp riêng."),
        new OnboardingPage("✨", "Trợ lý thời trang AI",
            "Chat trực tiếp với trợ lý AI để được tư vấn phong cách cá nhân hoá mọi lúc, mọi nơi.")
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityOnboardingBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        OnboardingAdapter adapter = new OnboardingAdapter(PAGES);
        binding.vpOnboarding.setAdapter(adapter);
        binding.dotsIndicator.attachTo(binding.vpOnboarding);

        binding.vpOnboarding.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                boolean isLast = position == PAGES.size() - 1;
                binding.btnNext.setText(isLast ? "Bắt đầu" : "Tiếp theo");
            }
        });

        binding.btnNext.setOnClickListener(v -> {
            int current = binding.vpOnboarding.getCurrentItem();
            if (current < PAGES.size() - 1) {
                binding.vpOnboarding.setCurrentItem(current + 1);
            } else {
                finishOnboarding();
            }
        });

        binding.tvSkip.setOnClickListener(v -> finishOnboarding());
    }

    private void finishOnboarding() {
        preferenceManager.setOnboardingShown(true);
        startActivity(new Intent(this, LoginActivity.class));
        finish();
    }

    // ── Inner adapter ──────────────────────────────────────────────────────────

    private static class OnboardingAdapter extends RecyclerView.Adapter<OnboardingAdapter.VH> {

        private final List<OnboardingPage> pages;

        OnboardingAdapter(List<OnboardingPage> pages) { this.pages = pages; }

        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_onboarding_page, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull VH holder, int position) {
            OnboardingPage page = pages.get(position);
            holder.tvEmoji.setText(page.emoji);
            holder.tvTitle.setText(page.title);
            holder.tvDescription.setText(page.description);
        }

        @Override
        public int getItemCount() { return pages.size(); }

        static class VH extends RecyclerView.ViewHolder {
            TextView tvEmoji, tvTitle, tvDescription;

            VH(@NonNull View itemView) {
                super(itemView);
                tvEmoji       = itemView.findViewById(R.id.tv_emoji);
                tvTitle       = itemView.findViewById(R.id.tv_title);
                tvDescription = itemView.findViewById(R.id.tv_description);
            }
        }
    }

    private static class OnboardingPage {
        final String emoji, title, description;
        OnboardingPage(String emoji, String title, String description) {
            this.emoji       = emoji;
            this.title       = title;
            this.description = description;
        }
    }
}
