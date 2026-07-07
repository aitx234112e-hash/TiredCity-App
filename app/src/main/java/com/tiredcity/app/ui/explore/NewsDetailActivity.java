package com.tiredcity.app.ui.explore;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.tiredcity.app.R;
import com.tiredcity.app.data.model.Article;
import com.tiredcity.app.data.network.ApiClient;
import com.tiredcity.app.data.repository.ArticleRepository;
import com.tiredcity.app.databinding.ActivityNewsDetailBinding;
import com.tiredcity.app.ui.base.BaseActivity;
import com.tiredcity.app.utils.Constants;

/** Trang chi tiết tin tức — đồng bộ dữ liệu từ Firestore. */
public class NewsDetailActivity extends BaseActivity {

    private ActivityNewsDetailBinding binding;
    private ArticleRepository repository;
    private Article currentArticle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityNewsDetailBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        repository = new ArticleRepository(ApiClient.getApiService(preferenceManager.getToken()));
        binding.btnBack.setOnClickListener(v -> finish());
        binding.btnOrder.setOnClickListener(v -> Toast.makeText(this, R.string.nd_order_toast, Toast.LENGTH_SHORT).show());
        binding.btnShare.setOnClickListener(v -> shareArticle());

        String articleId = getIntent().getStringExtra(Constants.EXTRA_ARTICLE_ID);
        if (articleId != null) {
            loadArticleDetails(articleId);
        } else {
            setupStaticDemo();
        }
    }

    private void loadArticleDetails(String id) {
        repository.getArticleById(id, new ArticleRepository.OnArticleLoadedListener() {
            @Override
            public void onSuccess(Article article) {
                if (binding == null) return;
                currentArticle = article;
                bindArticleData(article);
            }

            @Override
            public void onError(String message) {
                setupStaticDemo();
            }
        });
    }

    private void bindArticleData(Article a) {
        binding.tvHeroTitle.setText(a.getTitleVi() != null ? a.getTitleVi() : a.getTitle());
        binding.tvHeroSubtitle.setText(a.getExcerpt());
        binding.tvLead.setText(a.getExcerpt());
        binding.tvSec1Body.setText(a.getContent());
        
        if (a.getImageUrl() != null && !a.getImageUrl().isEmpty()) {
            Glide.with(this).load(a.getImageUrl()).centerCrop().into(binding.ivHero);
        } else {
            binding.ivHero.setImageResource(R.drawable.news_1);
        }

        // Keep other static images for demo feel
        loadImage(binding.ivPhoto1, R.drawable.news_3);
        loadImage(binding.ivPhoto2, R.drawable.news_2);
        loadImage(binding.ivPhoto3, R.drawable.news_4);
        loadImage(binding.ivPhoto4, R.drawable.news_5);
    }

    private void setupStaticDemo() {
        loadImage(binding.ivHero, R.drawable.news_1);
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
        String title = currentArticle != null ? currentArticle.getTitleVi() : getString(R.string.nd_hero_title);
        String sub = currentArticle != null ? currentArticle.getExcerpt() : getString(R.string.nd_hero_subtitle);
        
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
