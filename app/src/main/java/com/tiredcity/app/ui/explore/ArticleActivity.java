package com.tiredcity.app.ui.explore;

import android.os.Bundle;
import android.view.View;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.tiredcity.app.R;
import com.tiredcity.app.adapter.ArticleAdapter;
import com.tiredcity.app.data.model.Article;
import com.tiredcity.app.data.repository.FirestoreBlogRepository;
import com.tiredcity.app.databinding.ActivityArticleBinding;
import com.tiredcity.app.ui.base.BaseActivity;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/** Trang Tin tức — danh sách bài viết. Mở từ mục "Tin tức" ở Trang chủ. */
public class ArticleActivity extends BaseActivity {

    private ActivityArticleBinding binding;
    private final ArticleAdapter adapter = new ArticleAdapter(new ArrayList<>());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityArticleBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.btnBack.setOnClickListener(v -> finish());

        binding.rvArticles.setLayoutManager(new LinearLayoutManager(this));
        binding.rvArticles.setAdapter(adapter);

        adapter.setOnArticleClickListener(article -> {
            String title = article.getTitleVi() != null ? article.getTitleVi() : article.getTitle();
            if (HatBoiMagazineActivity.isFeatureArticle(title)) {
                startActivity(new android.content.Intent(this, HatBoiMagazineActivity.class));
            }
        });

        binding.swipeRefresh.setColorSchemeColors(getColor(R.color.tc_red));
        binding.swipeRefresh.setOnRefreshListener(this::loadArticles);

        loadArticles();
    }

    private void loadArticles() {
        binding.swipeRefresh.setRefreshing(true);
        // Đọc thẳng Firestore (collection blogs) — cùng nguồn với admin.
        new FirestoreBlogRepository().getArticles(articles -> {
            if (binding == null) return;
            if (articles != null && !articles.isEmpty()) {
                showArticles(articles);
            } else {
                // Firestore lỗi/rỗng → hiển thị dữ liệu mẫu để vẫn xem được giao diện.
                showArticles(buildMockArticles());
            }
        });
    }

    private void showArticles(List<Article> articles) {
        binding.swipeRefresh.setRefreshing(false);
        // Giữ nguyên tên/ảnh của card từ Firestore; chỉ cần chạm vào card chuyên đề
        // Hát Bội thì mở trang tạp chí HatBoiMagazineActivity (xử lý ở listener bên trên).
        adapter.updateArticles(articles);
        binding.tvEmpty.setVisibility(articles.isEmpty() ? View.VISIBLE : View.GONE);
    }

    /** Dữ liệu mẫu offline cho bản demo giao diện. */
    private List<Article> buildMockArticles() {
        String[][] data = {
            {"Hát Bội – Tinh hoa Đất Việt", "Tạp chí Văn Hóa", "2"},
            {"Áo Tấc – vẻ đẹp tối giản của Việt phục", "Tired City", "6"},
            {"Phối màu trang phục theo Ngũ Hành mệnh", "Tired City", "11"},
            {"Hành trình phục dựng cổ phục Việt", "Tired City", "20"},
            {"Lụa tơ tằm: chất liệu vượt thời gian", "Tired City", "30"},
        };
        List<Article> list = new ArrayList<>();
        long now = System.currentTimeMillis();
        int i = 0;
        for (String[] row : data) {
            Article a = new Article();
            a.setId(String.valueOf(++i));
            a.setTitleVi(row[0]);
            a.setAuthor(row[1]);
            a.setPublishedAt(new Date(now - Long.parseLong(row[2]) * 86_400_000L));
            list.add(a);
        }
        return list;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }
}
