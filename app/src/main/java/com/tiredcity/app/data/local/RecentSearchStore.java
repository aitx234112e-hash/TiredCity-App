package com.tiredcity.app.data.local;

import android.content.Context;
import android.content.SharedPreferences;
import java.util.ArrayList;
import java.util.List;

/** Lưu lịch sử tìm kiếm gần đây trên máy (SharedPreferences), mới nhất lên đầu, không trùng lặp. */
public class RecentSearchStore {

    private static final String PREF_NAME = "recent_search";
    private static final String KEY_ITEMS = "items";
    private static final String KEY_SEEDED = "seeded";
    private static final String DELIMITER = "";
    private static final int MAX_ITEMS = 8;

    private final SharedPreferences prefs;

    public RecentSearchStore(Context context) {
        prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public List<String> getAll() {
        String raw = prefs.getString(KEY_ITEMS, "");
        List<String> result = new ArrayList<>();
        if (!raw.isEmpty()) {
            for (String s : raw.split(DELIMITER)) {
                if (!s.trim().isEmpty()) result.add(s);
            }
        }
        return result;
    }

    public void add(String query) {
        if (query == null || query.trim().isEmpty()) return;
        String trimmed = query.trim();
        List<String> items = new ArrayList<>(getAll());
        items.removeIf(s -> s.equalsIgnoreCase(trimmed));
        items.add(0, trimmed);
        while (items.size() > MAX_ITEMS) items.remove(items.size() - 1);
        save(items);
    }

    public void remove(String query) {
        List<String> items = new ArrayList<>(getAll());
        items.removeIf(s -> s.equalsIgnoreCase(query));
        save(items);
    }

    public void clearAll() {
        prefs.edit().remove(KEY_ITEMS).apply();
    }

    /**
     * Xoá các từ khoá mẫu đã bơm sẵn ở phiên bản cũ (chạy một lần), để "Tìm kiếm gần đây"
     * bắt đầu trống và chỉ hiện khi người dùng tìm thật.
     */
    public void clearLegacySeedsOnce() {
        if (prefs.getBoolean(KEY_SEEDED, false)) {
            prefs.edit().remove(KEY_ITEMS).remove(KEY_SEEDED).apply();
        }
    }

    private void save(List<String> items) {
        prefs.edit().putString(KEY_ITEMS, String.join(DELIMITER, items)).apply();
    }
}
