package com.tiredcity.app.utils;

import androidx.annotation.NonNull;

import com.tiredcity.app.BuildConfig;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * Gọi THẲNG Google Gemini (bậc miễn phí) để sinh "Lời khuyên phong cách" theo mệnh.
 *
 * <p>Cách lấy khoá MIỄN PHÍ: vào https://aistudio.google.com → "Get API key" →
 * tạo khoá (không cần thẻ) → thêm dòng {@code GEMINI_API_KEY=khoa_cua_ban} vào file
 * {@code local.properties} (file này KHÔNG commit lên git nên khoá không bị lộ).
 *
 * <p>Lưu ý: khoá được nhúng vào app lúc build (BuildConfig). Với đồ án nộp bài thì
 * chấp nhận được; nếu lên Google Play thì nên chuyển sang gọi qua backend.
 */
public final class GeminiStylist {

    /** Khoá được nạp lúc build từ local.properties (GEMINI_API_KEY) → BuildConfig. */
    private static final String GEMINI_API_KEY = BuildConfig.GEMINI_API_KEY;

    /** Model miễn phí, nhanh — đủ đẹp cho đoạn ngắn. */
    private static final String MODEL = "gemini-2.5-flash";

    private static final String ENDPOINT =
            "https://generativelanguage.googleapis.com/v1beta/models/" + MODEL + ":generateContent";

    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    private static final OkHttpClient CLIENT = new OkHttpClient.Builder()
            .callTimeout(30, TimeUnit.SECONDS)
            .build();

    /** Callback trả về trên LUỒNG NỀN — nơi gọi tự đẩy về UI thread. */
    public interface Callback {
        void onAdvice(@NonNull String advice);
        void onError();
    }

    private GeminiStylist() {}

    /** True nếu đã dán khoá (chưa dán thì bỏ qua, dùng lời khuyên tĩnh). */
    public static boolean isConfigured() {
        return GEMINI_API_KEY != null
                && !GEMINI_API_KEY.trim().isEmpty()
                && !GEMINI_API_KEY.equals("PASTE_YOUR_GEMINI_API_KEY_HERE");
    }

    public static void suggest(String menh, List<String> colors, List<String> products,
                               @NonNull Callback cb) {
        if (!isConfigured()) { cb.onError(); return; }

        final String body;
        try {
            body = buildRequestBody(menh, colors, products);
        } catch (Exception e) {
            cb.onError();
            return;
        }

        Request request = new Request.Builder()
                .url(ENDPOINT + "?key=" + GEMINI_API_KEY)
                .post(RequestBody.create(body, JSON))
                .build();

        CLIENT.newCall(request).enqueue(new okhttp3.Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                cb.onError();
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) {
                try (Response r = response) {
                    if (!r.isSuccessful() || r.body() == null) { cb.onError(); return; }
                    String advice = parseAdvice(r.body().string());
                    if (advice != null && !advice.isEmpty()) cb.onAdvice(advice);
                    else cb.onError();
                } catch (Exception e) {
                    cb.onError();
                }
            }
        });
    }

    // ── Dựng prompt + body JSON theo định dạng Gemini generateContent ───────────

    private static String buildRequestBody(String menh, List<String> colors,
                                           List<String> products) throws Exception {
        String colorText = (colors != null && !colors.isEmpty())
                ? String.join(", ", colors) : "màu hợp mệnh";

        StringBuilder productText = new StringBuilder();
        if (products != null) {
            int count = 0;
            for (String p : products) {
                if (p == null || p.trim().isEmpty()) continue;
                productText.append("- ").append(p.trim()).append("\n");
                if (++count >= 6) break;
            }
        }
        if (productText.length() == 0) productText.append("(chưa có sản phẩm cụ thể)");

        String prompt = "Khách hàng thuộc mệnh " + menh + " (ngũ hành).\n"
                + "Màu sắc hợp mệnh: " + colorText + ".\n"
                + "Một vài trang phục Việt phục đang được gợi ý cho khách:\n"
                + productText
                + "\nHãy viết MỘT đoạn lời khuyên phong cách ngắn (2–3 câu, tiếng Việt) dành riêng "
                + "cho người mệnh " + menh + ": nên ưu tiên gam màu nào, kiểu dáng/chất liệu ra sao, "
                + "và khéo léo nhắc tên 1–2 sản phẩm trong danh sách trên nếu phù hợp. Giọng ấm áp, "
                + "tinh tế, như một stylist am hiểu văn hoá Việt. Trả về DUY NHẤT đoạn văn xuôi, "
                + "không markdown, không gạch đầu dòng.";

        JSONObject userPart = new JSONObject().put("text", prompt);
        JSONObject userContent = new JSONObject()
                .put("role", "user")
                .put("parts", new JSONArray().put(userPart));

        JSONObject systemInstruction = new JSONObject().put("parts", new JSONArray().put(
                new JSONObject().put("text",
                        "Bạn là chuyên gia tư vấn thời trang Việt phục (áo dài, nhật bình, áo tấc…), "
                                + "kết hợp hiểu biết phong thuỷ ngũ hành. Trả lời ngắn gọn, tự nhiên, đúng trọng tâm.")));

        // gemini-2.5-flash mặc định bật "thinking" (ăn hết token → câu bị cắt). Tắt đi
        // (thinkingBudget = 0) để trả thẳng lời khuyên, nhanh và không phí token.
        JSONObject generationConfig = new JSONObject()
                .put("maxOutputTokens", 400)
                .put("temperature", 0.9)
                .put("thinkingConfig", new JSONObject().put("thinkingBudget", 0));

        return new JSONObject()
                .put("contents", new JSONArray().put(userContent))
                .put("systemInstruction", systemInstruction)
                .put("generationConfig", generationConfig)
                .toString();
    }

    private static String parseAdvice(String json) {
        try {
            JSONObject root = new JSONObject(json);
            JSONArray candidates = root.optJSONArray("candidates");
            if (candidates == null || candidates.length() == 0) return null;
            JSONObject content = candidates.getJSONObject(0).optJSONObject("content");
            if (content == null) return null;
            JSONArray parts = content.optJSONArray("parts");
            if (parts == null) return null;
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < parts.length(); i++) {
                String t = parts.getJSONObject(i).optString("text", "");
                sb.append(t);
            }
            return sb.toString().trim();
        } catch (Exception e) {
            return null;
        }
    }
}
