package com.tiredcity.app.ui.explore;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.bumptech.glide.Glide;
import com.tiredcity.app.R;
import com.tiredcity.app.databinding.ActivityHatBoiMagazineBinding;
import com.tiredcity.app.databinding.ItemHbSpreadBinding;
import com.tiredcity.app.ui.base.BaseActivity;

/**
 * Trang chi tiết riêng cho bài viết đặc biệt "VĂN HÓA · HÁT BỘI" (Tinh hoa Đất Việt).
 * Nội dung tĩnh, biên tập sẵn dưới dạng tạp chí điện tử — mở khi chạm đúng card
 * chuyên đề Hát Bội trong danh sách Bài viết (ArticleActivity).
 */
public class HatBoiMagazineActivity extends BaseActivity {

    private ActivityHatBoiMagazineBinding binding;

    /** Tiêu đề hiển thị của card chuyên đề trong danh sách Bài viết. */
    public static final String FEATURE_TITLE = "Hát Bội – Tinh hoa Đất Việt";

    /** Nhận diện đúng card mở chuyên đề này (card gốc "Nhật Bình" hoặc bản đã đổi tên). */
    public static boolean isFeatureArticle(String title) {
        if (title == null) return false;
        return title.contains("Hát Bội") || title.contains("Nhật Bình");
    }

    /** Mỗi "kỳ báo": {drawable ảnh, số thứ tự, tên chương, phụ đề, chú thích}. */
    private static final Object[][] SPREADS = {
            {R.drawable.hb_foreword, "01", "Lời tựa", "Măng-sét & thư ngỏ số 27",
                    "Trang bìa lót mở đầu chuyên đề, giới thiệu ê-kíp thực hiện và tôn chỉ của số báo dành cho nghệ thuật Hát Bội."},
            {R.drawable.hb_contents, "02", "Mục lục", "Bảy chương của hành trình",
                    "Toàn cảnh nội dung: Lịch sử – Nhân vật – Mặt nạ – Tác phẩm để đời – Hát Bội với thời trang & giới trẻ."},
            {R.drawable.hb_history, "03", "Lịch sử Hát Bội", "Từ nghi lễ dân gian đến sân khấu cung đình",
                    "Khởi nguồn từ hát múa dân gian, giao thoa với hí kịch, Hát Bội thăng hoa rồi trải qua bao thăng trầm theo dòng lịch sử."},
            {R.drawable.hb_characters, "04", "Nhân vật", "Kép, Lão, Đào và Mụ",
                    "Mỗi tuyến nhân vật mang một phẩm cách và vai trò riêng, khắc họa những mâu thuẫn thiện – ác trên sân khấu."},
            {R.drawable.hb_makeup, "05", "Nghệ thuật hóa trang", "Sắc màu vẽ mặt & thần thái",
                    "Đỏ, vàng, xanh, trắng, đen — mỗi màu là một tính cách; từng đường nét vẽ mặt kể tiếp câu chuyện của vở tuồng."},
            {R.drawable.hb_masterpiece, "06", "Tác phẩm để đời", "Đào Tam Xuân · Lôi Vũ · Ngọc Hân",
                    "Những vở tuồng cổ điển kết tinh giá trị lịch sử, đạo lý và chất bi hùng của sân khấu truyền thống Việt Nam."},
            {R.drawable.hb_modern, "07", "Giao thoa cũ & mới", "Hát Bội trong MV ‘Chân Ái’",
                    "Luồng gió đương đại đưa Hát Bội đến gần giới trẻ, gợi mở cách bảo tồn và tiếp nối di sản trong đời sống hôm nay."},
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityHatBoiMagazineBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        applyEdgeToEdgeInsets();

        Glide.with(this).load(R.drawable.hb_cover).into(binding.ivCover);
        binding.btnBack.setOnClickListener(v -> finish());

        buildSpreads();
    }

    private void buildSpreads() {
        LayoutInflater inflater = LayoutInflater.from(this);
        for (Object[] s : SPREADS) {
            ItemHbSpreadBinding item = ItemHbSpreadBinding.inflate(inflater, binding.spreadContainer, false);
            item.tvNo.setText((String) s[1]);
            item.tvChapter.setText((String) s[2]);
            item.tvSub.setText((String) s[3]);
            item.tvCaption.setText((String) s[4]);
            Glide.with(this).load((Integer) s[0]).into(item.ivPage);
            binding.spreadContainer.addView(item.getRoot());
        }
    }

    /** targetSdk 35 ép edge-to-edge: đẩy nút quay lại xuống dưới status bar,
     *  chừa chỗ cho thanh điều hướng ở đáy trang. */
    private void applyEdgeToEdgeInsets() {
        final int backBaseTop = dp(12);
        ViewCompat.setOnApplyWindowInsetsListener(binding.getRoot(), (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());

            ViewGroup.MarginLayoutParams backLp =
                    (ViewGroup.MarginLayoutParams) binding.btnBack.getLayoutParams();
            backLp.topMargin = backBaseTop + bars.top;
            binding.btnBack.setLayoutParams(backLp);

            binding.scroll.setPadding(0, 0, 0, dp(24) + bars.bottom);
            return insets;
        });
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }
}
