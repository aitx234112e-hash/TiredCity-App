package com.tiredcity.app.ui.onboarding;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.signature.ObjectKey;
import com.tiredcity.app.R;
import com.tiredcity.app.databinding.ActivityOnboardingBinding;
import com.tiredcity.app.ui.base.BaseActivity;
import com.tiredcity.app.ui.main.MainActivity;

import java.util.Arrays;
import java.util.List;

public class OnboardingActivity extends BaseActivity {

    private ActivityOnboardingBinding binding;
    private List<OnboardingPage> pages;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityOnboardingBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // 5 slide — ảnh nền tự lấy theo tên onboarding_1 … onboarding_5 trong res/drawable.
        // Chỉ cần bỏ ảnh vào là tự hiện; chưa có thì nền nâu mặc định.
        pages = Arrays.asList(
            new OnboardingPage("onboarding_1",
                    getString(R.string.onboarding_title_1), getString(R.string.onboarding_desc_1)),
            new OnboardingPage("onboarding_2",
                    getString(R.string.onboarding_title_2), getString(R.string.onboarding_desc_2)),
            new OnboardingPage("onboarding_3",
                    getString(R.string.onboarding_title_3), getString(R.string.onboarding_desc_3)),
            new OnboardingPage("onboarding_4",
                    getString(R.string.onboarding_title_4), getString(R.string.onboarding_desc_4)),
            new OnboardingPage("onboarding_5",
                    getString(R.string.onboarding_title_5), getString(R.string.onboarding_desc_5))
        );

        OnboardingAdapter adapter = new OnboardingAdapter(pages);
        binding.vpOnboarding.setOffscreenPageLimit(pages.size());
        binding.vpOnboarding.setAdapter(adapter);
        binding.dotsIndicator.attachTo(binding.vpOnboarding);

        binding.vpOnboarding.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                boolean isLast = position == pages.size() - 1;
                binding.btnNext.setText(isLast
                        ? getString(R.string.btn_get_started)
                        : getString(R.string.onboarding_next));
            }
        });

        binding.btnNext.setOnClickListener(v -> {
            int current = binding.vpOnboarding.getCurrentItem();
            if (current < pages.size() - 1) {
                binding.vpOnboarding.setCurrentItem(current + 1);
            } else {
                finishOnboarding();
            }
        });

        binding.tvSkip.setOnClickListener(v -> finishOnboarding());
    }

    private void finishOnboarding() {
        preferenceManager.setOnboardingShown(true);
        startActivity(new Intent(this, MainActivity.class));
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

            int imgId = holder.itemView.getResources().getIdentifier(
                    page.imageName, "drawable", holder.itemView.getContext().getPackageName());
            Glide.with(holder.ivBg).clear(holder.ivBg);
            if (imgId != 0) {
                // Bỏ qua cache của Glide cho ảnh trong drawable để tránh việc
                // các resource id khác nhau bị trả về cùng một ảnh (ảnh bị lặp).
                Glide.with(holder.ivBg)
                        .load(imgId)
                        .signature(new ObjectKey(page.imageName))
                        .diskCacheStrategy(DiskCacheStrategy.NONE)
                        .skipMemoryCache(true)
                        .centerCrop()
                        .into(holder.ivBg);
            } else {
                holder.ivBg.setImageDrawable(null);
            }

            holder.tvTitle.setText(page.title);
            holder.tvDescription.setText(page.description);
        }

        @Override
        public int getItemCount() { return pages.size(); }

        static class VH extends RecyclerView.ViewHolder {
            ImageView ivBg;
            TextView tvTitle, tvDescription;

            VH(@NonNull View itemView) {
                super(itemView);
                ivBg          = itemView.findViewById(R.id.iv_bg);
                tvTitle       = itemView.findViewById(R.id.tv_title);
                tvDescription = itemView.findViewById(R.id.tv_description);
            }
        }
    }

    private static class OnboardingPage {
        final String imageName, title, description;

        OnboardingPage(String imageName, String title, String description) {
            this.imageName   = imageName;
            this.title       = title;
            this.description = description;
        }
    }
}
