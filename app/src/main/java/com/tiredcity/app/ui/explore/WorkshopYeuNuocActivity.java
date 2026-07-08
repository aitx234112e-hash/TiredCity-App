package com.tiredcity.app.ui.explore;

import android.os.Bundle;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.bumptech.glide.Glide;
import com.tiredcity.app.R;
import com.tiredcity.app.databinding.ActivityWorkshopYeuNuocBinding;
import com.tiredcity.app.ui.base.BaseActivity;

/**
 * Trang chi tiết riêng cho sự kiện "Workshop YÊU NƯỚC TỪ TRONG NÔI" (CANIFA Kid Tour).
 * Nội dung tĩnh, biên tập sẵn — mở khi chạm đúng card này trong danh sách Sự kiện.
 */
public class WorkshopYeuNuocActivity extends BaseActivity {

    private ActivityWorkshopYeuNuocBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityWorkshopYeuNuocBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        applyEdgeToEdgeInsets();

        // Nạp ảnh panel chiến dịch (giữ trọn tỉ lệ, không center-crop).
        loadImage(R.drawable.ynn_hero, binding.ivHero);
        loadImage(R.drawable.ynn_keyvisual, binding.ivKeyvisual);
        loadImage(R.drawable.ynn_mascot, binding.ivMascot);
        loadImage(R.drawable.ynn_activity, binding.ivActivity);
        loadImage(R.drawable.ynn_merch, binding.ivMerch);

        // Ảnh động (GIF) — Glide tự phát và giảm mẫu để nhẹ bộ nhớ.
        loadImage(R.drawable.ynn_anim_logo, binding.ivAnimLogo);
        loadImage(R.drawable.ynn_anim_passport, binding.ivAnimPassport);
        loadImage(R.drawable.ynn_anim_stickers, binding.ivAnimStickers);

        binding.btnBack.setOnClickListener(v -> finish());
        binding.btnRegister.setOnClickListener(v ->
                Toast.makeText(this, "Cảm ơn bạn! Chúng tôi sẽ liên hệ xác nhận suất tham gia.",
                        Toast.LENGTH_LONG).show());
    }

    private void loadImage(int resId, android.widget.ImageView target) {
        Glide.with(this).load(resId).into(target);
    }

    /** targetSdk 35 ép edge-to-edge: đẩy nút quay lại xuống dưới status bar và
     *  chừa chỗ cho thanh điều hướng ở thanh "Đăng ký". */
    private void applyEdgeToEdgeInsets() {
        final int backBaseTop = dp(12);
        final int barBasePad = dp(12);
        ViewCompat.setOnApplyWindowInsetsListener(binding.getRoot(), (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());

            ViewGroup.MarginLayoutParams backLp =
                    (ViewGroup.MarginLayoutParams) binding.btnBack.getLayoutParams();
            backLp.topMargin = backBaseTop + bars.top;
            binding.btnBack.setLayoutParams(backLp);

            binding.bottomBar.setPadding(
                    binding.bottomBar.getPaddingLeft(),
                    barBasePad,
                    binding.bottomBar.getPaddingRight(),
                    barBasePad + bars.bottom);

            binding.scroll.setPadding(0, 0, 0, dp(92) + bars.bottom);
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
