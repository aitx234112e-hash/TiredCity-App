package com.tiredcity.app.ui.support;

import android.os.Build;
import android.os.Bundle;
import android.text.Html;
import android.text.Spanned;
import android.widget.TextView;
import com.tiredcity.app.R;
import com.tiredcity.app.databinding.ActivityTermsBinding;
import com.tiredcity.app.ui.base.BaseActivity;

/**
 * TermsActivity - Trang Điều khoản & Điều kiện.
 * Nội dung được ghép từ các mục nhỏ hoặc hiển thị terms_content nếu có.
 */
public class TermsActivity extends BaseActivity {

    private ActivityTermsBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityTermsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.btnBack.setOnClickListener(v -> finishSmoothly());

        initContent();
    }

    private void initContent() {
        // Danh sách tiêu đề và nội dung điều khoản
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
            try {
                String title = getString(titles[i]);
                String body = getString(contents[i]);

                if (i > 0) sb.append("<br/><br/>");
                sb.append("<b><font color=\"#A80D15\">")
                  .append(title)
                  .append("</font></b><br/><br/>")
                  .append(body.trim());
            } catch (Exception e) {
                // Bỏ qua nếu thiếu resource
            }
        }

        if (sb.length() > 0) {
            setHtmlText(binding.tvTermsContent, sb.toString());
        } else {
            // Fallback nếu không có các mục lẻ
            String fallback = getString(R.string.terms_content);
            setHtmlText(binding.tvTermsContent, fallback);
        }
    }

    private void setHtmlText(TextView textView, String html) {
        if (html == null) return;
        Spanned spanned;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            spanned = Html.fromHtml(html, Html.FROM_HTML_MODE_COMPACT);
        } else {
            spanned = Html.fromHtml(html);
        }
        textView.setText(spanned);
    }
}
