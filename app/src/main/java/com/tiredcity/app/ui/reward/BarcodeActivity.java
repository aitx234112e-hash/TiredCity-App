package com.tiredcity.app.ui.reward;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Build;
import android.os.Bundle;
import android.text.Html;
import android.text.TextUtils;
import android.widget.Toast;
import com.tiredcity.app.R;
import com.tiredcity.app.databinding.ActivityBarcodeBinding;
import com.tiredcity.app.ui.base.BaseActivity;

/**
 * Màn hình Mã Vạch ưu đãi — mở khi bấm "SỬ DỤNG NGAY".
 * Hiển thị barcode (vẽ tay từ mã code) + mã text kèm nút Copy.
 */
public class BarcodeActivity extends BaseActivity {

    public static final String EXTRA_CODE = "extra_code";

    private ActivityBarcodeBinding binding;
    private String code;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityBarcodeBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        code = getIntent().getStringExtra(EXTRA_CODE);
        if (TextUtils.isEmpty(code)) code = getString(R.string.barcode_code);

        bindData();
        setupClicks();
    }

    private void bindData() {
        binding.tvInStore.setText(html(
                getString(R.string.barcode_in_store_title), getString(R.string.barcode_in_store_desc)));
        binding.tvOnline.setText(html(
                getString(R.string.barcode_online_title), getString(R.string.barcode_online_desc)));
        binding.tvCode.setText(code);
        binding.ivBarcode.setImageBitmap(generateBarcode(code));
    }

    private CharSequence html(String boldPart, String normalPart) {
        String raw = "<b>" + boldPart + "</b>" + normalPart;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            return Html.fromHtml(raw, Html.FROM_HTML_MODE_COMPACT);
        }
        return Html.fromHtml(raw);
    }

    private void setupClicks() {
        binding.btnBack.setOnClickListener(v -> finish());
        binding.btnCopy.setOnClickListener(v -> copyCode());
    }

    private void copyCode() {
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        if (clipboard != null) {
            clipboard.setPrimaryClip(ClipData.newPlainText("TiredCity voucher", code));
            Toast.makeText(this, R.string.barcode_copied, Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Vẽ một mã vạch tượng trưng từ chuỗi mã (không phụ thuộc thư viện ngoài).
     * Mỗi ký tự sinh ra 8 vạch đen/trắng xen kẽ với độ rộng suy từ các bit của ký tự.
     */
    private Bitmap generateBarcode(String value) {
        final int module = 3;          // độ rộng đơn vị (px)
        final int height = 220;        // chiều cao vùng vạch (px)
        final int quietZone = module * 10;
        final int barsPerChar = 8;

        // Tính tổng chiều rộng trước.
        int contentWidth = 0;
        for (int i = 0; i < value.length(); i++) {
            int c = value.charAt(i);
            for (int b = 0; b < barsPerChar; b++) {
                contentWidth += (((c >> b) & 1) == 1 ? 2 : 1) * module;
            }
        }
        int width = contentWidth + quietZone * 2;
        if (width <= 0) width = quietZone * 2 + module;

        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        canvas.drawColor(Color.WHITE);

        Paint paint = new Paint();
        paint.setColor(Color.BLACK);
        paint.setAntiAlias(false);

        int x = quietZone;
        for (int i = 0; i < value.length(); i++) {
            int c = value.charAt(i);
            for (int b = 0; b < barsPerChar; b++) {
                int barWidth = (((c >> b) & 1) == 1 ? 2 : 1) * module;
                // Vạch ở vị trí chẵn vẽ đen, vị trí lẻ để trắng → xen kẽ như barcode thật.
                if (b % 2 == 0) {
                    canvas.drawRect(x, 0, x + barWidth, height, paint);
                }
                x += barWidth;
            }
        }
        return bitmap;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }
}
