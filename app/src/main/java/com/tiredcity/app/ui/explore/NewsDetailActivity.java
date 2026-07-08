package com.tiredcity.app.ui.explore;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.tiredcity.app.R;
import com.tiredcity.app.databinding.ActivityNewsDetailBinding;
import com.tiredcity.app.ui.base.BaseActivity;

/**
 * Trang chi tiết tin tức "Sắc Phục Việt" — bài tạp chí CỐ ĐỊNH.
 * Toàn bộ chữ lấy từ chuỗi nd_* trong layout, ảnh bìa dùng blog_1 để khớp nội dung;
 * KHÔNG ghi đè bằng bài mới nhất trên Firestore (tránh lệch tiêu đề/ảnh như "Lụa tơ tằm").
 */
public class NewsDetailActivity extends BaseActivity {

    private ActivityNewsDetailBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityNewsDetailBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.btnBack.setOnClickListener(v -> finish());
        binding.btnOrder.setOnClickListener(v -> Toast.makeText(this, R.string.nd_order_toast, Toast.LENGTH_SHORT).show());
        binding.btnShare.setOnClickListener(v -> shareArticle());

        setupStaticDemo();
    }

    private void setupStaticDemo() {
        loadImage(binding.ivHero, R.drawable.blog_1);
        loadImage(binding.ivPhoto1, R.drawable.news_3);
        loadImage(binding.ivPhoto2, R.drawable.news_2);
        loadImage(binding.ivPhoto3, R.drawable.news_4);
        loadImage(binding.ivPhoto4, R.drawable.news_5);
    }

    private void loadImage(ImageView target, int resId) {
        Glide.with(this)
                .load(resId)
                .diskCacheStrategy(DiskCacheStrategy.NONE)
                .skipMemoryCache(true)
                .centerCrop()
                .into(target);
    }

    private void shareArticle() {
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("text/plain");
        String title = getString(R.string.nd_hero_title);
        String sub = getString(R.string.nd_hero_subtitle);

        intent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.nd_share_subject));
        intent.putExtra(Intent.EXTRA_TEXT, title + "\n" + sub);
        startActivity(Intent.createChooser(intent, getString(R.string.nd_cta_share)));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }
}
