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
            {R.drawable.hb_foreword, "01", "Măng-sét", "Trang 01–02 · Thư ngỏ số 27",
                    "Trang bìa lót ghi danh ê-kíp thực hiện số báo: tổng biên tập, cố vấn, thiết kế và nguồn hình ảnh sưu tầm."},
            {R.drawable.hb_contents, "02", "Mục lục", "Trang 03–04 · Sáu chương của hành trình",
                    "Lịch sử · Nhân vật · Mặt nạ · Tác phẩm để đời · Hát Bội với thời trang · Hát Bội với giới trẻ."},
            {R.drawable.hb_history, "03", "Lịch sử Hát Bội", "Trang 05–06 · Phát triển và thăng trầm",
                    "Bắt nguồn từ hát múa dân gian Việt cổ và giao thoa cùng hí kịch Trung Hoa, hát bội từ nghi lễ đình – đền – chùa "
                            + "vươn lên thành sân khấu phổ biến khắp miền Nam cuối thế kỷ 19, rồi nhường bước trước cải lương thập niên 30–40."},
            {R.drawable.hb_characters, "04", "Nhân vật", "Trang 07–08 · Kép, Lão, Đào và Mụ",
                    "Kép chính trực, Lão dựng mạch truyện, Đào dịu dàng hiền thục, Mụ gian ngoa mưu mô — mỗi tuyến vai là một biểu tượng "
                            + "phẩm hạnh, khắc họa mâu thuẫn thiện – ác và những bài học nhân sinh."},
            {R.drawable.hb_makeup, "05", "Trang điểm Hát Bội", "Trang 09–10 · Đỉnh cao nghệ thuật hóa trang",
                    "Màu sắc, vẽ mặt, mắt, môi và lông mày — mỗi chi tiết là một thông điệp. Đỏ, vàng, xanh, trắng, đen không chỉ làm đẹp "
                            + "mà tượng trưng cho tính cách, vai trò và trạng thái của nhân vật."},
            {R.drawable.hb_masterpiece, "06", "Tác phẩm để đời", "Trang 11–12 · Đào Tam Xuân · Lôi Vũ · Ngọc Hân",
                    "Nữ tướng Đào Tam Xuân — người phụ nữ tài sắc vẹn toàn mà bạc phận — cùng Lôi Vũ và Ngọc Hân công chúa kết tinh "
                            + "chất bi hùng, lòng trung thành và sự hy sinh của sân khấu tuồng cổ."},
            {R.drawable.hb_modern, "07", "Giao thoa cũ & mới", "Trang 13–14 · Luồng gió mới từ MV ‘Chân Ái’",
                    "Lấy bối cảnh sân khấu kịch thập niên 60, ‘Chân Ái’ (Orange × Khói × Châu Đăng Khoa) trộn rap, hip-hop và ballad "
                            + "với hóa trang hát bội — đưa di sản đến gần giới trẻ và mở lại câu hỏi về cách bảo tồn."},
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
