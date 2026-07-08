package com.tiredcity.app.ui.cart;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Toast;

import androidx.core.content.ContextCompat;

import com.tiredcity.app.R;
import com.tiredcity.app.databinding.ActivityQrPaymentBinding;
import com.tiredcity.app.ui.base.BaseActivity;
import com.tiredcity.app.utils.PriceUtils;
import com.tiredcity.app.utils.QrGenerator;

import java.util.Locale;
import java.util.Random;

/**
 * Trang QR thanh toán — mở SAU bước xác thực (captcha) với các phương thức online
 * (MoMo / Chuyển khoản / Thẻ). Khách quét mã, bấm "Tôi đã thanh toán" → trả RESULT_OK
 * để {@link PaymentActivity} ghi đơn vào Firestore. COD không đi qua màn này.
 */
public class QrPaymentActivity extends BaseActivity {

    public static final String EXTRA_AMOUNT = "qr_amount";
    public static final String EXTRA_METHOD = "qr_method";
    /** Mã đơn hiển thị trên QR — trả lại cho PaymentActivity để màn thành công dùng đúng mã này. */
    public static final String EXTRA_RESULT_ORDER_CODE = "qr_result_order_code";

    /** Thời gian hiệu lực mã QR: 10 phút. */
    private static final long EXPIRY_MILLIS = 10 * 60 * 1000L;

    /** Sau khi hiện QR 10 giây thì tự động coi như thanh toán thành công (app demo). */
    private static final long AUTO_SUCCESS_MILLIS = 10 * 1000L;

    private ActivityQrPaymentBinding binding;
    private CountDownTimer countDownTimer;
    private final Handler autoSuccessHandler = new Handler(Looper.getMainLooper());
    private final Runnable autoSuccessRunnable = this::completePayment;
    private String orderCode;
    private double amount;
    private String method;
    private boolean expired = false;
    private boolean completed = false;

    /** Tạo Intent mở trang QR với số tiền + phương thức đã chọn. */
    public static Intent newIntent(Context ctx, double amount, String method) {
        Intent intent = new Intent(ctx, QrPaymentActivity.class);
        intent.putExtra(EXTRA_AMOUNT, amount);
        intent.putExtra(EXTRA_METHOD, method);
        return intent;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityQrPaymentBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.toolbar.setNavigationOnClickListener(v -> cancel());

        amount = getIntent().getDoubleExtra(EXTRA_AMOUNT, 0);
        method = getIntent().getStringExtra(EXTRA_METHOD);
        if (method == null) method = "MOMO";
        orderCode = generateOrderCode();

        bindMethod();
        bindGuideSteps();
        binding.tvAmount.setText(PriceUtils.formatVnd(amount));
        binding.tvOrderCode.setText(orderCode);
        renderQr();
        startCountdown();

        // Không còn nút "Tôi đã thanh toán": chờ 10s tự động chuyển sang màn thành công.
        autoSuccessHandler.postDelayed(autoSuccessRunnable, AUTO_SUCCESS_MILLIS);

        binding.btnChangeMethod.setOnClickListener(v -> cancel());
    }

    /** Icon + tên phương thức trên chip đầu trang. */
    private void bindMethod() {
        int iconRes;
        int nameRes;
        switch (method) {
            case "BANK_TRANSFER":
                iconRes = R.drawable.ic_pay_bank;
                nameRes = R.string.pay_method_bank;
                break;
            case "CARD":
                iconRes = R.drawable.ic_pay_card;
                nameRes = R.string.pay_method_card;
                break;
            case "MOMO":
            default:
                iconRes = R.drawable.ic_pay_momo;
                nameRes = R.string.pay_method_momo;
                break;
        }
        binding.ivMethodIcon.setImageResource(iconRes);
        binding.tvMethodName.setText(nameRes);
    }

    /** Ba bước hướng dẫn theo từng phương thức. */
    private void bindGuideSteps() {
        int step1, step2, step3;
        switch (method) {
            case "BANK_TRANSFER":
                step1 = R.string.qr_bank_step1;
                step2 = R.string.qr_bank_step2;
                step3 = R.string.qr_bank_step3;
                break;
            case "CARD":
                step1 = R.string.qr_card_step1;
                step2 = R.string.qr_card_step2;
                step3 = R.string.qr_card_step3;
                break;
            case "MOMO":
            default:
                step1 = R.string.qr_momo_step1;
                step2 = R.string.qr_momo_step2;
                step3 = R.string.qr_momo_step3;
                break;
        }
        binding.tvStep1.setText(step1);
        binding.tvStep2.setText(step2);
        binding.tvStep3.setText(step3);
    }

    /** Vẽ mã QR từ chuỗi payload (thương hiệu + số tiền + mã đơn). MoMo → ô QR hồng + viền hồng. */
    private void renderQr() {
        boolean isMomo = "MOMO".equals(method);
        int fg = ContextCompat.getColor(this, isMomo ? R.color.momo_pink : R.color.tc_red_deep);
        Bitmap qr = QrGenerator.create(buildPayload(), 640, fg, 0xFFFFFFFF);
        if (qr != null) {
            binding.ivQr.setImageBitmap(qr);
        } else {
            binding.ivQr.setImageResource(R.drawable.ic_pay_momo);
        }
        // Khung hồng bao quanh QR cho đúng bộ nhận diện MoMo.
        if (isMomo) {
            int pad = Math.round(12 * getResources().getDisplayMetrics().density);
            binding.ivQr.setBackgroundResource(R.drawable.bg_momo_qr_frame);
            binding.ivQr.setPadding(pad, pad, pad, pad);
        } else {
            binding.ivQr.setBackground(null);
            binding.ivQr.setPadding(0, 0, 0, 0);
        }
    }

    /**
     * Nội dung mã hoá trong QR. Đây là app demo (không có cổng thanh toán thật) nên
     * mã hoá một chuỗi mô tả giao dịch — quét được, hiển thị thông tin đơn cho khách.
     */
    private String buildPayload() {
        return "TIREDCITY|" + method
                + "|amount=" + (long) amount
                + "|ref=" + orderCode;
    }

    /** Mã đơn ngắn kiểu "TC-XXXXXX" (chữ + số, dễ đọc). */
    private String generateOrderCode() {
        final String pool = "0123456789ABCDEFGHJKLMNPQRSTUVWXYZ"; // bỏ I,O gây nhầm
        StringBuilder sb = new StringBuilder("TC-");
        Random r = new Random();
        for (int i = 0; i < 6; i++) sb.append(pool.charAt(r.nextInt(pool.length())));
        return sb.toString();
    }

    private void startCountdown() {
        countDownTimer = new CountDownTimer(EXPIRY_MILLIS, 1000) {
            @Override
            public void onTick(long msLeft) {
                long totalSec = msLeft / 1000;
                binding.tvCountdown.setText(
                        String.format(Locale.US, "%02d:%02d", totalSec / 60, totalSec % 60));
            }

            @Override
            public void onFinish() {
                expired = true;
                binding.tvCountdown.setText(R.string.qr_expired);
                binding.ivQr.setAlpha(0.25f);
                binding.btnChangeMethod.setText(R.string.qr_btn_retry);
                Toast.makeText(QrPaymentActivity.this, R.string.qr_expired_toast, Toast.LENGTH_LONG).show();
            }
        }.start();
    }

    /** Tự động (sau 10s) coi như thanh toán thành công, trả kết quả cho PaymentActivity. */
    private void completePayment() {
        if (expired || completed) return;
        completed = true;
        Intent data = new Intent();
        data.putExtra(EXTRA_RESULT_ORDER_CODE, orderCode);
        setResult(RESULT_OK, data);
        finish();
    }

    /** Hết hạn → nút "Tạo mã mới" khởi động lại màn; bình thường → huỷ, quay lại. */
    private void cancel() {
        autoSuccessHandler.removeCallbacks(autoSuccessRunnable);
        if (expired) {
            recreate();
            return;
        }
        setResult(RESULT_CANCELED);
        finish();
    }

    @Override
    public void onBackPressed() {
        cancel();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (countDownTimer != null) countDownTimer.cancel();
        autoSuccessHandler.removeCallbacks(autoSuccessRunnable);
        binding = null;
    }
}
