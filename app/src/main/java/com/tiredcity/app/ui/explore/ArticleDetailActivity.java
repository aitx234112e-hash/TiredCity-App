package com.tiredcity.app.ui.explore;

import android.os.Bundle;
import android.text.Html;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.google.firebase.firestore.FirebaseFirestore;
import com.tiredcity.app.R;
import com.tiredcity.app.data.model.Article;
import com.tiredcity.app.databinding.ActivityArticleDetailBinding;
import com.tiredcity.app.utils.DateUtils;

public class ArticleDetailActivity extends AppCompatActivity {

    private ActivityArticleDetailBinding binding;
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityArticleDetailBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        String id = getIntent().getStringExtra("article_id");
        
        // Fast-path: hien thi nhung gi truyen qua intent truoc
        binding.tvTitle.setText(getIntent().getStringExtra("article_title"));
        binding.tvAuthor.setText(getIntent().getStringExtra("article_author"));
        Glide.with(this)
                .load(getIntent().getStringExtra("article_image"))
                .centerCrop()
                .into(binding.ivHeader);

        binding.toolbar.setNavigationOnClickListener(v -> finish());

        if (id != null) {
            loadArticleDetail(id);
            // Tăng lượt xem
            new com.tiredcity.app.data.repository.ArticleRepository(null).incrementArticleViews(id);
        }
    }

    private void loadArticleDetail(String id) {
        db.collection("blogs").document(id).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Article article = documentSnapshot.toObject(Article.class);
                        if (article != null) {
                            displayArticle(article);
                        }
                    }
                });
    }

    private void displayArticle(Article article) {
        binding.tvTitle.setText(article.getTitleVi());
        binding.tvAuthor.setText(article.getAuthor());
        binding.tvDate.setText(article.getPublishedDate() != null 
                ? DateUtils.formatDisplayDate(article.getPublishedDate()) : "");

        if (article.getContent() != null) {
            // Ho tro HTML vi content tu web-admin thuong la RichText
            binding.tvContent.setText(Html.fromHtml(article.getContent(), Html.FROM_HTML_MODE_COMPACT));
        }

        Glide.with(this)
                .load(article.getImageUrl())
                .centerCrop()
                .placeholder(R.color.bg_subtle)
                .into(binding.ivHeader);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }
}
