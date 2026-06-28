package com.tiredcity.app.utils;

import android.content.Context;
import android.net.Uri;
import android.widget.ImageView;
import com.bumptech.glide.Glide;
import com.tiredcity.app.R;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

/** Tiện ích ảnh đại diện: mặc định là logo gà TiredCity, có thể đổi bằng ảnh từ thư viện. */
public final class AvatarUtils {
    private AvatarUtils() {}

    /** Hiển thị avatar: ảnh người dùng đã chọn nếu có, ngược lại dùng logo gà. */
    public static void load(Context ctx, ImageView target) {
        String path = new PreferenceManager(ctx).getAvatarPath();
        if (path != null && !path.isEmpty() && new File(path).exists()) {
            Glide.with(ctx).load(new File(path)).circleCrop().into(target);
        } else {
            Glide.with(ctx).load(R.drawable.ic_tc_logo).into(target);
        }
    }

    /** Copy ảnh từ Uri (thư viện) vào bộ nhớ trong của app, trả về đường dẫn đã lưu. */
    public static String saveFromUri(Context ctx, Uri uri) throws Exception {
        File out = new File(ctx.getFilesDir(), "avatar.jpg");
        try (InputStream in = ctx.getContentResolver().openInputStream(uri);
             FileOutputStream fos = new FileOutputStream(out)) {
            byte[] buf = new byte[8192];
            int n;
            while ((n = in.read(buf)) > 0) fos.write(buf, 0, n);
        }
        return out.getAbsolutePath();
    }
}
