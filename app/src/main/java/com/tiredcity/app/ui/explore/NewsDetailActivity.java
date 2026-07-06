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

/** Trang chi tiết tin tức "Sắc Phục Việt" — mở khi chạm ảnh Tin tức ở Trang chủ.
 *  Nội dung tĩnh (bản demo). 5 ảnh (news_1..news_5, trong đó news_2/news_4 là gif động)
 *  nạp qua Glide để tự thu nhỏ theo kích thước view, tránh giải mã 5 bitmap full-size cùng lúc.
 *  Cùng họ thiết kế với {@link EventDetailActivity}. */
public class NewsDetailActivity extends BaseActivity {

    private ActivityNewsDetailBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityNewsDetailBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        loadImage(binding.ivHero, R.drawable.news_1);
        loadImage(binding.ivPhoto1, R.drawable.news_3);
        loadImage(binding.ivPhoto2, R.drawable.news_2);   // gif động
        loadImage(binding.ivPhoto3, R.drawable.news_4);   // gif động
        loadImage(binding.ivPhoto4, R.drawable.news_5);

        binding.btnBack.setOnClickListener(v -> finish());

        binding.btnOrder.setOnClickListener(v ->
                Toast.makeText(this, R.string.nd_order_toast, Toast.LENGTH_SHORT).show());

        binding.btnShare.setOnClickListener(v -> shareArticle());
    }

    /** Glide tự nhận diện gif và phát động; không cache đĩa để khi thay file ảnh mà
     *  versionCode không đổi vẫn nạp lại bản mới (giống EventDetailActivity). */
    private void loadImage(ImageView target, int resId) {
        Glide.with(this)
                .load(resId)
                .diskCacheStrategy(DiskCacheStrategy.NONE)
                .skipMemoryCache(true)
                .centerCrop()
                .into(target);
    }

    /** Chia sẻ tiêu đề bài viết qua ứng dụng bất kỳ trên máy. */
    private void shareArticle() {
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.nd_share_subject));
        intent.putExtra(Intent.EXTRA_TEXT,
                getString(R.string.nd_hero_title) + "\n" + getString(R.string.nd_hero_subtitle));
        startActivity(Intent.createChooser(intent, getString(R.string.nd_cta_share)));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }
}
