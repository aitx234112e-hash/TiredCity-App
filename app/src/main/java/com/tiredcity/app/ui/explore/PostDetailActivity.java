package com.tiredcity.app.ui.explore;

import android.content.Context;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.tiredcity.app.R;
import com.tiredcity.app.databinding.ActivityPostDetailBinding;
import com.tiredcity.app.databinding.ItemPostGalleryBinding;
import com.tiredcity.app.databinding.ItemPostHeadingBinding;
import com.tiredcity.app.databinding.ItemPostParagraphBinding;
import com.tiredcity.app.ui.base.BaseActivity;

/**
 * Trang chi tiết dùng chung cho bài Sự kiện và bài Tin tức: ảnh bìa, tiêu đề,
 * thông tin nhanh, nội dung và bộ ảnh minh hoạ. Nội dung do {@link PostContent} dựng.
 */
public class PostDetailActivity extends BaseActivity {

    private static final String EXTRA_HEADER = "post_header";
    private static final String EXTRA_TITLE = "post_title";
    private static final String EXTRA_META = "post_meta";
    private static final String EXTRA_KICKER = "post_kicker";
    private static final String EXTRA_FACT_TIME = "post_fact_time";
    private static final String EXTRA_FACT_PLACE = "post_fact_place";
    private static final String EXTRA_BODY = "post_body";
    private static final String EXTRA_BLOCK_KINDS = "post_block_kinds";
    private static final String EXTRA_BLOCK_TEXTS = "post_block_texts";
    private static final String EXTRA_BLOCK_IMAGES = "post_block_images";
    private static final String EXTRA_QUOTE = "post_quote";
    private static final String EXTRA_CREDIT = "post_credit";
    private static final String EXTRA_HERO_RES = "post_hero_res";
    private static final String EXTRA_HERO_URL = "post_hero_url";
    private static final String EXTRA_GALLERY = "post_gallery";
    private static final String EXTRA_GALLERY_CAPTIONS = "post_gallery_captions";
    private static final String EXTRA_GALLERY_LABEL = "post_gallery_label";

    private ActivityPostDetailBinding binding;

    /** Mở trang chi tiết cho một bài đã dựng nội dung sẵn. */
    public static void start(Context context, PostContent content) {
        Intent intent = new Intent(context, PostDetailActivity.class);
        intent.putExtra(EXTRA_HEADER, content.header);
        intent.putExtra(EXTRA_TITLE, content.title);
        intent.putExtra(EXTRA_META, content.meta);
        intent.putExtra(EXTRA_KICKER, content.kicker);
        intent.putExtra(EXTRA_FACT_TIME, content.factTime);
        intent.putExtra(EXTRA_FACT_PLACE, content.factPlace);
        intent.putExtra(EXTRA_BODY, content.body);
        intent.putExtra(EXTRA_BLOCK_KINDS, content.blockKinds);
        intent.putExtra(EXTRA_BLOCK_TEXTS, content.blockTexts);
        intent.putExtra(EXTRA_BLOCK_IMAGES, content.blockImages);
        intent.putExtra(EXTRA_QUOTE, content.quote);
        intent.putExtra(EXTRA_CREDIT, content.credit);
        intent.putExtra(EXTRA_HERO_RES, content.heroRes);
        intent.putExtra(EXTRA_HERO_URL, content.heroUrl);
        intent.putExtra(EXTRA_GALLERY, content.gallery);
        intent.putExtra(EXTRA_GALLERY_CAPTIONS, content.galleryCaptions);
        intent.putExtra(EXTRA_GALLERY_LABEL, content.galleryLabel);
        context.startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityPostDetailBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.btnBack.setOnClickListener(v -> finish());

        Intent intent = getIntent();
        binding.tvHeader.setText(intent.getStringExtra(EXTRA_HEADER));
        binding.tvPostTitle.setText(intent.getStringExtra(EXTRA_TITLE));
        binding.tvPostMeta.setText(intent.getStringExtra(EXTRA_META));

        bindBody(intent.getStringExtra(EXTRA_BODY),
                intent.getIntArrayExtra(EXTRA_BLOCK_KINDS),
                intent.getStringArrayExtra(EXTRA_BLOCK_TEXTS),
                intent.getIntArrayExtra(EXTRA_BLOCK_IMAGES));

        bindOptional(binding.tvKicker, intent.getStringExtra(EXTRA_KICKER));
        bindOptional(binding.tvCredit, intent.getStringExtra(EXTRA_CREDIT));

        String quote = intent.getStringExtra(EXTRA_QUOTE);
        binding.boxQuote.setVisibility(TextUtils.isEmpty(quote) ? View.GONE : View.VISIBLE);
        binding.tvQuote.setText(quote);

        bindFacts(intent.getStringExtra(EXTRA_FACT_TIME), intent.getStringExtra(EXTRA_FACT_PLACE));
        bindHero(intent.getStringExtra(EXTRA_HERO_URL), intent.getIntExtra(EXTRA_HERO_RES, 0));
        bindGallery(intent.getIntArrayExtra(EXTRA_GALLERY),
                intent.getStringArrayExtra(EXTRA_GALLERY_CAPTIONS),
                intent.getStringExtra(EXTRA_GALLERY_LABEL));
    }

    /** Đặt nội dung, ẩn hẳn view khi không có gì để hiện. */
    private void bindOptional(TextView view, String text) {
        view.setVisibility(TextUtils.isEmpty(text) ? View.GONE : View.VISIBLE);
        view.setText(text);
    }

    /** Khối "🗓 thời gian / 📍 địa điểm" chỉ hiện với bài sự kiện. */
    private void bindFacts(String time, String place) {
        boolean hasTime = !TextUtils.isEmpty(time);
        boolean hasPlace = !TextUtils.isEmpty(place);
        binding.boxFacts.setVisibility(hasTime || hasPlace ? View.VISIBLE : View.GONE);
        binding.tvFactTime.setVisibility(hasTime ? View.VISIBLE : View.GONE);
        binding.tvFactPlace.setVisibility(hasPlace ? View.VISIBLE : View.GONE);
        binding.tvFactTime.setText(time);
        binding.tvFactPlace.setText(place);
    }

    /** Ảnh bìa: ưu tiên ảnh drawable nội bộ, không có thì tải theo URL. */
    private void bindHero(String url, int res) {
        if (!TextUtils.isEmpty(url)) {
            Glide.with(this).load(url).centerCrop().into(binding.ivHero);
        } else if (res != 0) {
            Glide.with(this).load(res).centerCrop().into(binding.ivHero);
        } else {
            binding.ivHero.setVisibility(View.GONE);
        }
    }

    /**
     * Thân bài. Bài đặc biệt gửi kèm các khối (tiêu đề mục / đoạn văn / ảnh xen giữa) và
     * được dựng trong {@code body_container}; bài thường vẫn dùng một TextView duy nhất.
     */
    private void bindBody(String body, int[] kinds, String[] texts, int[] images) {
        boolean hasBlocks = kinds != null && kinds.length > 0;
        binding.tvPostBody.setVisibility(hasBlocks ? View.GONE : View.VISIBLE);
        binding.bodyContainer.setVisibility(hasBlocks ? View.VISIBLE : View.GONE);
        binding.bodyContainer.removeAllViews();

        if (!hasBlocks) {
            binding.tvPostBody.setText(body);
            return;
        }

        LayoutInflater inflater = LayoutInflater.from(this);
        for (int i = 0; i < kinds.length; i++) {
            String text = texts != null && i < texts.length ? texts[i] : "";
            switch (kinds[i]) {
                case PostContent.BLOCK_HEADING: {
                    ItemPostHeadingBinding item =
                            ItemPostHeadingBinding.inflate(inflater, binding.bodyContainer, false);
                    item.tvHeading.setText(text);
                    binding.bodyContainer.addView(item.getRoot());
                    break;
                }
                case PostContent.BLOCK_IMAGE: {
                    int res = images != null && i < images.length ? images[i] : 0;
                    if (res != 0) addGalleryImage(binding.bodyContainer, inflater, res, text);
                    break;
                }
                default: {
                    ItemPostParagraphBinding item =
                            ItemPostParagraphBinding.inflate(inflater, binding.bodyContainer, false);
                    item.tvParagraph.setText(boldKeywords(text));
                    binding.bodyContainer.addView(item.getRoot());
                    break;
                }
            }
        }
    }

    /** Đổi các đoạn bọc trong {@code **…**} thành chữ in đậm, màu vàng đất. */
    private CharSequence boldKeywords(String text) {
        if (text == null || !text.contains("**")) return text;
        int keywordColor = getColor(R.color.tc_gold_deep);
        SpannableStringBuilder out = new SpannableStringBuilder();
        int cursor = 0;
        while (true) {
            int open = text.indexOf("**", cursor);
            int close = open < 0 ? -1 : text.indexOf("**", open + 2);
            if (close < 0) break;
            out.append(text, cursor, open);
            int start = out.length();
            out.append(text, open + 2, close);
            out.setSpan(new StyleSpan(Typeface.BOLD), start, out.length(),
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            out.setSpan(new ForegroundColorSpan(keywordColor), start, out.length(),
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            cursor = close + 2;
        }
        out.append(text, cursor, text.length());
        return out;
    }

    /**
     * Dựng khối ảnh cuối bài. Ảnh có chú thích được trình bày như một trang tạp chí
     * (khung vàng kim, giữ nguyên tỉ lệ); ảnh không chú thích giữ dạng cắt vuông như cũ.
     */
    private void bindGallery(int[] images, String[] captions, String label) {
        binding.galleryContainer.removeAllViews();
        if (images == null || images.length == 0) {
            binding.tvGalleryLabel.setVisibility(View.GONE);
            return;
        }

        LayoutInflater inflater = LayoutInflater.from(this);
        int shown = 0;
        for (int i = 0; i < images.length; i++) {
            if (images[i] == 0) continue;
            String caption = captions != null && i < captions.length ? captions[i] : null;
            addGalleryImage(binding.galleryContainer, inflater, images[i], caption);
            shown++;
        }

        binding.tvGalleryLabel.setVisibility(shown > 0 ? View.VISIBLE : View.GONE);
        binding.tvGalleryLabel.setText(
                TextUtils.isEmpty(label) ? getString(R.string.post_detail_gallery) : label);
    }

    private void addGalleryImage(ViewGroup parent, LayoutInflater inflater, int res, String caption) {
        ItemPostGalleryBinding item = ItemPostGalleryBinding.inflate(inflater, parent, false);

        if (TextUtils.isEmpty(caption)) {
            item.tvCaption.setVisibility(View.GONE);
            item.framePage.setBackground(null);
            item.framePage.setPadding(0, 0, 0, 0);
            ViewGroup.LayoutParams lp = item.ivPage.getLayoutParams();
            lp.height = dp(200);
            item.ivPage.setLayoutParams(lp);
            item.ivPage.setAdjustViewBounds(false);
            item.ivPage.setScaleType(ImageView.ScaleType.CENTER_CROP);
        } else {
            item.tvCaption.setText(caption);
        }

        Glide.with(this).load(res).into(item.ivPage);
        parent.addView(item.getRoot());
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
