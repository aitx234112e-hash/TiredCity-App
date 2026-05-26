package com.tiredcity.app.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;
import android.widget.ImageView;
import java.io.ByteArrayOutputStream;

public class ImageUtils {
    private ImageUtils() {}

    public static void loadImage(Context context, String url, ImageView imageView) {
        // Implement with your chosen image loading library (Glide, Picasso, etc.)
        // Example with Glide:
        // Glide.with(context).load(url).into(imageView);
    }

    public static void loadCircleImage(Context context, String url, ImageView imageView) {
        // Implement with your chosen image loading library
        // Example with Glide:
        // Glide.with(context).load(url).circleCrop().into(imageView);
    }

    public static byte[] bitmapToBytes(Bitmap bitmap) {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 80, stream);
        return stream.toByteArray();
    }

    public static Uri getUriFromBitmap(Context context, Bitmap bitmap) {
        // Returns a content URI for the bitmap — use for sharing or uploading
        return null;
    }
}
