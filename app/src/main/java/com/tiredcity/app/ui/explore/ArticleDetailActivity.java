package com.tiredcity.app.ui.explore;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.tiredcity.app.R;
import com.tiredcity.app.databinding.ActivityArticleDetailBinding;

public class ArticleDetailActivity extends AppCompatActivity {

    private ActivityArticleDetailBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityArticleDetailBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        String id = getIntent().getStringExtra("article_id");

        // Bìa đầu + tiêu đề/nội dung luôn dùng bản cố định "Sắc Phục Việt" (blog_1)
        // để khớp với chữ trên ảnh bìa; không ghi đè bằng dữ liệu Firestore/intent.
        Glide.with(this)
                .load(R.drawable.blog_1)
                .centerCrop()
                .into(binding.ivHero);

        binding.toolbar.setNavigationOnClickListener(v -> finish());

        if (id != null) {
            // Vẫn đếm lượt xem, nhưng giữ nguyên nội dung mặc định.
            new com.tiredcity.app.data.repository.ArticleRepository(null).incrementArticleViews(id);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }
}
