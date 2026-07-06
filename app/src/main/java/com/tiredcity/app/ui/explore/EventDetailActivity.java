package com.tiredcity.app.ui.explore;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.tiredcity.app.R;
import com.tiredcity.app.databinding.ActivityEventDetailBinding;
import com.tiredcity.app.ui.base.BaseActivity;

/** Trang chi tiết sự kiện "DÂN TỘC TỰ HÀO" — mở khi chạm ảnh Sự kiện ở Trang chủ.
 *  Nội dung tĩnh (bản demo), 9 ảnh nạp qua Glide để tự thu nhỏ theo kích thước view,
 *  tránh giải mã 9 bitmap full-size cùng lúc trong NestedScrollView. */
public class EventDetailActivity extends BaseActivity {

    /** Toạ độ Văn Miếu – Quốc Tử Giám cho nút "Chỉ đường". */
    private static final String VAN_MIEU_GEO =
            "geo:21.0287,105.8355?q=Văn Miếu Quốc Tử Giám, Hà Nội";

    private ActivityEventDetailBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityEventDetailBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        loadImage(binding.ivHero, R.drawable.event_1);
        loadImage(binding.ivDisplay, R.drawable.event_4);
        loadImage(binding.ivView, R.drawable.event_8);
        loadImage(binding.ivExp1, R.drawable.event_2);
        loadImage(binding.ivExp2, R.drawable.event_6);
        loadImage(binding.ivExp3, R.drawable.event_5);
        loadImage(binding.ivExp4, R.drawable.event_7);
        loadImage(binding.ivVisitors, R.drawable.event_3);
        loadImage(binding.ivPoster, R.drawable.event_9);

        binding.btnBack.setOnClickListener(v -> finish());

        binding.btnRegister.setOnClickListener(v ->
                Toast.makeText(this, R.string.ed_register_toast, Toast.LENGTH_SHORT).show());

        binding.btnDirection.setOnClickListener(v -> openDirections());
    }

    private void loadImage(ImageView target, int resId) {
        // Không cache đĩa cho ảnh sự kiện (là drawable tĩnh): khi thay file ảnh mà
        // versionCode không đổi, Glide sẽ trả ảnh cũ trong cache → luôn nạp lại bản mới.
        Glide.with(this)
                .load(resId)
                .diskCacheStrategy(DiskCacheStrategy.NONE)
                .skipMemoryCache(true)
                .centerCrop()
                .into(target);
    }

    /** Mở ứng dụng bản đồ tới Văn Miếu; nếu máy không có app bản đồ thì báo nhẹ. */
    private void openDirections() {
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(VAN_MIEU_GEO));
        try {
            startActivity(intent);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, R.string.ed_fact_place_value, Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }
}
