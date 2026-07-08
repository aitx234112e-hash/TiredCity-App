package com.tiredcity.app.utils;

import android.content.Context;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Dữ liệu hành chính VN từ assets/vn_address.json: Tỉnh → Phường/Xã.
 *
 * <p>Từ 01/07/2025 Việt Nam sáp nhập còn 34 tỉnh/thành và BỎ cấp quận/huyện, nên cấu trúc
 * chỉ còn 2 cấp (trước đây Tỉnh → Quận/Huyện → Phường/Xã). Danh sách tỉnh lấy thẳng từ file
 * này qua {@link #getProvinces()} — không dùng R.array.vn_provinces nữa — để dropdown không
 * bao giờ hiện một tỉnh rồi lại không sổ ra phường/xã nào.
 */
public final class AddressData {

    private static Map<String, List<String>> data;

    private AddressData() {}

    public static void init(Context ctx) {
        if (data != null) return;
        try (InputStream in = ctx.getAssets().open("vn_address.json");
             InputStreamReader r = new InputStreamReader(in, StandardCharsets.UTF_8)) {
            data = new Gson().fromJson(r,
                new TypeToken<LinkedHashMap<String, List<String>>>() {}.getType());
        } catch (Exception e) {
            data = new LinkedHashMap<>();
        }
    }

    /** 34 tỉnh/thành, đã sắp theo tên. */
    public static List<String> getProvinces() {
        if (data == null) return new ArrayList<>();
        return new ArrayList<>(data.keySet());
    }

    /** Danh sách Phường/Xã của một tỉnh (rỗng nếu chưa có dữ liệu → người dùng tự gõ). */
    public static List<String> getWards(String province) {
        if (data == null || province == null) return new ArrayList<>();
        List<String> w = data.get(province);
        return w == null ? new ArrayList<>() : new ArrayList<>(w);
    }
}
