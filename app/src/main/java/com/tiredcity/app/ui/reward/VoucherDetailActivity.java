package com.tiredcity.app.ui.reward;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Toast;
import androidx.annotation.DrawableRes;
import com.tiredcity.app.R;
import com.tiredcity.app.databinding.ActivityVoucherDetailBinding;
import com.tiredcity.app.ui.base.BaseActivity;

/**
 * Chi tiết Voucher (không dùng hạng thành viên) — BST Áo Dài / Nhật Bình.
 * Hai nút cố định: "ĐƯỢC TẶNG" (viền đỏ) và "SỬ DỤNG NGAY" (nền đỏ → mở mã vạch).
 */
public class VoucherDetailActivity extends BaseActivity {

    public static final String EXTRA_TITLE    = "extra_voucher_title";
    public static final String EXTRA_SUBTITLE = "extra_voucher_subtitle";
    public static final String EXTRA_BANNER   = "extra_voucher_banner";
    public static final String EXTRA_CODE     = "extra_voucher_code";

    private ActivityVoucherDetailBinding binding;
    private String code;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityVoucherDetailBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        bindData();
        setupClicks();
    }

    private void bindData() {
        Intent intent = getIntent();

        String title    = intent.getStringExtra(EXTRA_TITLE);
        String subtitle = intent.getStringExtra(EXTRA_SUBTITLE);
        @DrawableRes int banner = intent.getIntExtra(EXTRA_BANNER, R.drawable.banner_2);
        code = intent.getStringExtra(EXTRA_CODE);
        if (TextUtils.isEmpty(code)) code = getString(R.string.barcode_code);

        if (TextUtils.isEmpty(title)) {
            title = getString(R.string.reward_voucher_tri_an_title);
        }
        if (!TextUtils.isEmpty(subtitle)) {
            title = title + " - " + subtitle;
        }

        binding.tvTitle.setText(title);
        binding.ivBanner.setImageResource(banner);
    }

    private void setupClicks() {
        binding.btnBack.setOnClickListener(v -> finish());

        binding.btnShare.setOnClickListener(v -> {
            Intent share = new Intent(Intent.ACTION_SEND);
            share.setType("text/plain");
            share.putExtra(Intent.EXTRA_TEXT,
                    binding.tvTitle.getText().toString() + "\n" + code);
            startActivity(Intent.createChooser(share, getString(R.string.cd_share)));
        });

        binding.btnGifted.setOnClickListener(v ->
                Toast.makeText(this, R.string.voucher_gifted, Toast.LENGTH_SHORT).show());

        binding.btnUseNow.setOnClickListener(v -> {
            Intent intent = new Intent(this, BarcodeActivity.class);
            intent.putExtra(BarcodeActivity.EXTRA_CODE, code);
            startActivity(intent);
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }
}
