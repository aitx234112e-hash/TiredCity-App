package com.tiredcity.app.ui.explore;

import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.tiredcity.app.R;
import com.tiredcity.app.adapter.ArticleAdapter;
import com.tiredcity.app.data.network.ApiClient;
import com.tiredcity.app.data.repository.ArticleRepository;
import com.tiredcity.app.databinding.ActivityArticleBinding;
import com.tiredcity.app.ui.base.BaseActivity;

import java.util.ArrayList;

/** Trang Tin tức — danh sách bài viết. Mở từ mục "Tin tức" ở Trang chủ. */
public class ArticleActivity extends BaseActivity {

    private ActivityArticleBinding binding;
    private ArticleViewModel viewModel;
    private final ArticleAdapter adapter = new ArticleAdapter(new ArrayList<>());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityArticleBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Khởi tạo Repository và ViewModel
        ArticleRepository repository = new ArticleRepository(ApiClient.getApiService(preferenceManager.getToken()));
        ArticleViewModelFactory factory = new ArticleViewModelFactory(repository);
        viewModel = new ViewModelProvider(this, factory).get(ArticleViewModel.class);

        binding.btnBack.setOnClickListener(v -> finish());

        binding.rvArticles.setLayoutManager(new LinearLayoutManager(this));
        binding.rvArticles.setAdapter(adapter);

        adapter.setOnArticleClickListener(article -> {
            android.content.Intent intent = new android.content.Intent(this, ArticleDetailActivity.class);
            intent.putExtra("article_id", article.getId());
            // Truyen them cac field co san de hien thi ngay (fast-path)
            intent.putExtra("article_title", article.getTitleVi());
            intent.putExtra("article_author", article.getAuthor());
            intent.putExtra("article_image", article.getImageUrl());
            startActivity(intent);
        });

        binding.swipeRefresh.setColorSchemeColors(getColor(R.color.tc_red));
        binding.swipeRefresh.setOnRefreshListener(() -> viewModel.loadArticles());

        observeViewModel();
        
        viewModel.loadArticles();
    }

    private void observeViewModel() {
        viewModel.getArticles().observe(this, articles -> {
            binding.swipeRefresh.setRefreshing(false);
            adapter.updateArticles(articles);
            binding.tvEmpty.setVisibility(articles.isEmpty() ? View.VISIBLE : View.GONE);
        });

        viewModel.getIsLoading().observe(this, isLoading -> {
            binding.swipeRefresh.setRefreshing(isLoading);
        });

        viewModel.getErrorMessage().observe(this, message -> {
            if (message != null && !message.isEmpty()) {
                Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }
}
