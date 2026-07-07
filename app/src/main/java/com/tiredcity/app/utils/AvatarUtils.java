package com.tiredcity.app.utils;

import android.content.Context;
import android.net.Uri;
import android.widget.ImageView;
import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.tiredcity.app.R;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

/** Tiện ích ảnh đại diện: mặc định là logo gà TiredCity, có thể đổi bằng ảnh từ thư viện. */
public final class AvatarUtils {
    private AvatarUtils() {}

    /** Hiển thị avatar: ưu tiên URL từ profile, sau đó đến file cục bộ, cuối cùng là logo gà. */
    public static void load(Context ctx, ImageView target) {
        PreferenceManager pm = new PreferenceManager(ctx);
        String path = pm.getAvatarPath(); // Local path
        
        com.tiredcity.app.data.model.UserProfile user = pm.getUser();
        if (user != null && user.getAvatar() != null && !user.getAvatar().isEmpty()) {
            // Ưu tiên ảnh từ Cloud
            Glide.with(ctx).load(user.getAvatar()).circleCrop().into(target);
        } else if (path != null && !path.isEmpty() && new File(path).exists()) {
            // Dùng ảnh cục bộ nếu chưa có Cloud URL
            Glide.with(ctx).load(new File(path)).circleCrop().into(target);
        } else {
            Glide.with(ctx).load(R.drawable.ic_tc_logo).into(target);
        }
    }

    /** Tải ảnh lên Firebase Storage và trả về URL. */
    public static void uploadToCloud(Context ctx, Uri uri, OnUploadListener listener) {
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null) return;

        StorageReference ref = FirebaseStorage.getInstance().getReference()
                .child("avatars/" + uid + ".jpg");

        ref.putFile(uri)
                .addOnSuccessListener(taskSnapshot -> ref.getDownloadUrl()
                        .addOnSuccessListener(downloadUri -> {
                            listener.onSuccess(downloadUri.toString());
                        }))
                .addOnFailureListener(e -> listener.onError(e.getMessage()));
    }

    public interface OnUploadListener {
        void onSuccess(String url);
        void onError(String message);
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
