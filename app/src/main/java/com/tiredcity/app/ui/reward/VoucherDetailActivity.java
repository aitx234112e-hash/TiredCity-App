package com.tiredcity.app.ui.reward;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Toast;
import androidx.annotation.DrawableRes;
import com.tiredcity.app.R;
import com.tiredcity.app.databinding.ActivityVoucherDetailBinding;
import com.tiredcity.app.ui.base.BaseActivity;

/**
 * Chi tiết một ưu đãi: banner, hiệu lực và điều khoản áp dụng.
 * Hai nút cố định: "ĐƯỢC TẶNG" (viền đỏ) và "SỬ DỤNG NGAY" (nền đỏ → mở mã vạch của voucher đó).
 */
public class VoucherDetailActivity extends BaseActivity {

    public static final String EXTRA_TITLE    = "extra_voucher_title";
    public static final String EXTRA_SUBTITLE = "extra_voucher_subtitle";
    public static final String EXTRA_BANNER   = "extra_voucher_banner";
    public static final String EXTRA_CODE     = "extra_voucher_code";
    public static final String EXTRA_VALIDITY = "extra_voucher_validity";
    public static final String EXTRA_DESC     = "extra_voucher_desc";

    private ActivityVoucherDetailBinding binding;
    private String code;
    private String title;

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

        title = intent.getStringExtra(EXTRA_TITLE);
        String subtitle = intent.getStringExtra(EXTRA_SUBTITLE);
        String validity = intent.getStringExtra(EXTRA_VALIDITY);
        String desc     = intent.getStringExtra(EXTRA_DESC);
        @DrawableRes int banner = intent.getIntExtra(EXTRA_BANNER, R.drawable.reward_tri_an);

        code = intent.getStringExtra(EXTRA_CODE);
        if (TextUtils.isEmpty(code)) code = getString(R.string.barcode_code);
        if (TextUtils.isEmpty(title)) title = getString(R.string.reward_voucher_tri_an_title);
        if (TextUtils.isEmpty(validity)) validity = getString(R.string.voucher_detail_validity);
        if (TextUtils.isEmpty(desc)) desc = getString(R.string.voucher_detail_desc);

        binding.tvTitle.setText(title);
        binding.tvSubtitle.setText(subtitle);
        binding.tvSubtitle.setVisibility(TextUtils.isEmpty(subtitle) ? View.GONE : View.VISIBLE);
        binding.tvValidity.setText(validity);
        binding.tvDesc.setText(desc);
        binding.ivBanner.setImageResource(banner);
    }

    private void setupClicks() {
        binding.btnBack.setOnClickListener(v -> finish());

        binding.btnShare.setOnClickListener(v -> {
            Intent share = new Intent(Intent.ACTION_SEND);
            share.setType("text/plain");
            share.putExtra(Intent.EXTRA_TEXT, title + "\n" + code);
            startActivity(Intent.createChooser(share, getString(R.string.cd_share)));
        });

        binding.btnGifted.setOnClickListener(v ->
                Toast.makeText(this, R.string.voucher_gifted_done, Toast.LENGTH_SHORT).show());

        binding.btnUseNow.setOnClickListener(v -> {
            Intent intent = new Intent(this, BarcodeActivity.class);
            intent.putExtra(BarcodeActivity.EXTRA_CODE, code);
            intent.putExtra(BarcodeActivity.EXTRA_TITLE, title);
            startActivity(intent);
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }
}
