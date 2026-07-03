package com.tiredcity.admin.utils;

import android.util.Base64;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.RequestBuilder;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;

/**
 * Tai anh san pham bang Glide — mirror cach app khach hang load anh.
 * Ho tro ca URL http(s) va data-URL base64 (do web-admin luu anh duoi dang
 * data:image/...;base64,... khi chua dung Firebase Storage).
 */
public final class ImageLoader {
    private ImageLoader() {}

    private static final int PLACEHOLDER = 0xFFF0E7DA;

    public static void loadProduct(ImageView iv, String url) {
        if (url == null || url.trim().isEmpty()) {
            iv.setImageDrawable(null);
            iv.setBackgroundColor(PLACEHOLDER);
            return;
        }
        RequestBuilder<android.graphics.drawable.Drawable> req;
        if (url.startsWith("data:")) {
            byte[] bytes = decodeDataUrl(url);
            if (bytes == null) {
                iv.setImageDrawable(null);
                iv.setBackgroundColor(PLACEHOLDER);
                return;
            }
            req = Glide.with(iv.getContext()).load(bytes);
        } else {
            req = Glide.with(iv.getContext()).load(url.trim());
        }
        req.centerCrop()
                .transition(DrawableTransitionOptions.withCrossFade())
                .into(iv);
    }

    /** Tach phan base64 sau dau phay va giai ma; tra ve null neu khong hop le. */
    private static byte[] decodeDataUrl(String dataUrl) {
        int comma = dataUrl.indexOf(',');
        if (comma < 0 || comma + 1 >= dataUrl.length()) return null;
        try {
            return Base64.decode(dataUrl.substring(comma + 1), Base64.DEFAULT);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
