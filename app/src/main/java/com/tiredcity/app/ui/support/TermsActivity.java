package com.tiredcity.app.ui.support;

import android.os.Build;
import android.os.Bundle;
import android.text.Html;
import android.widget.TextView;
import com.tiredcity.app.R;
import com.tiredcity.app.databinding.ActivityTermsBinding;
import com.tiredcity.app.ui.base.BaseActivity;

/**
 * Trang Điều khoản & Điều kiện — một trang chữ liền mạch kèm ảnh cửa hàng
 * TiredCity và ảnh minh hoạ chính sách. Nội dung tĩnh, không cần RecyclerView.
 */
public class TermsActivity extends BaseActivity {

    private ActivityTermsBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityTermsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.btnBack.setOnClickListener(v -> finish());

        initContent();
    }

    private void initContent() {
        // Ghép 7 mục thành một bài viết liền mạch: tiêu đề in đậm + nội dung.
        int[] titles = {
                R.string.terms_1_title, R.string.terms_2_title, R.string.terms_3_title,
                R.string.terms_4_title, R.string.terms_5_title, R.string.terms_6_title,
                R.string.terms_7_title
        };
        int[] contents = {
                R.string.terms_1_content, R.string.terms_2_content, R.string.terms_3_content,
                R.string.terms_4_content, R.string.terms_5_content, R.string.terms_6_content,
                R.string.terms_7_content
        };

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < titles.length; i++) {
            if (i > 0) sb.append("<br/><br/>");
            sb.append("<b><font color=\"#A80D15\">")
              .append(getString(titles[i]))
              .append("</font></b><br/><br/>")
              .append(getString(contents[i]).trim());
        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        binding.toolbar.setNavigationOnClickListener(v -> finish());

        String content = getString(R.string.terms_content);
        setHtmlText(binding.tvContent, sb.toString());
    }

    private void setHtmlText(TextView textView, String html) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            binding.tvTermsContent.setText(Html.fromHtml(content, Html.FROM_HTML_MODE_COMPACT));
            textView.setText(Html.fromHtml(html, Html.FROM_HTML_MODE_COMPACT));
        } else {
            binding.tvTermsContent.setText(Html.fromHtml(content));
            textView.setText(Html.fromHtml(html));
        }
    }
}
