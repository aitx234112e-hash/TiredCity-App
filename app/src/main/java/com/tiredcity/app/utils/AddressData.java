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

/** Dữ liệu hành chính VN từ assets/vn_address.json: Tỉnh → Quận/Huyện → Phường/Xã. */
public final class AddressData {

    private static Map<String, Map<String, List<String>>> data;

    private AddressData() {}

    public static void init(Context ctx) {
        if (data != null) return;
        try (InputStream in = ctx.getAssets().open("vn_address.json");
             InputStreamReader r = new InputStreamReader(in, StandardCharsets.UTF_8)) {
            data = new Gson().fromJson(r,
                new TypeToken<LinkedHashMap<String, LinkedHashMap<String, List<String>>>>() {}.getType());
        } catch (Exception e) {
            data = new LinkedHashMap<>();
        }
    }

    /** Danh sách Quận/Huyện của một tỉnh (rỗng nếu chưa có dữ liệu → người dùng tự gõ). */
    public static List<String> getDistricts(String province) {
        if (data == null || province == null) return new ArrayList<>();
        Map<String, List<String>> d = data.get(province);
        return d == null ? new ArrayList<>() : new ArrayList<>(d.keySet());
    }

    /** Danh sách Phường/Xã của một quận (rỗng nếu chưa có dữ liệu → người dùng tự gõ). */
    public static List<String> getWards(String province, String district) {
        if (data == null || province == null || district == null) return new ArrayList<>();
        Map<String, List<String>> d = data.get(province);
        if (d == null) return new ArrayList<>();
        List<String> w = d.get(district);
        return w == null ? new ArrayList<>() : new ArrayList<>(w);
    }
}
