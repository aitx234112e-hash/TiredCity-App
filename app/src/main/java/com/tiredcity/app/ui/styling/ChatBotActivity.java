package com.tiredcity.app.ui.styling;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.tiredcity.app.R;
import com.tiredcity.app.adapter.ChatMessageAdapter;
import com.tiredcity.app.data.model.ChatMessage;
import com.tiredcity.app.data.model.ClaudeRequest;
import com.tiredcity.app.data.model.ClaudeResponse;
import com.tiredcity.app.data.network.ClaudeApiClient;
import com.tiredcity.app.databinding.ActivityChatbotBinding;
import com.tiredcity.app.ui.base.BaseActivity;
import com.tiredcity.app.utils.MenhCalculator;
import java.util.ArrayList;
import java.util.List;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ChatBotActivity extends BaseActivity {

    private ActivityChatbotBinding binding;
    private ChatMessageAdapter chatAdapter;
    private final List<ChatMessage> messages = new ArrayList<>();

    /** Vi tri bong bong "dang soan tin"; -1 khi khong hien. */
    private int typingIndex = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityChatbotBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.btnBack.setOnClickListener(v -> finish());

        chatAdapter = new ChatMessageAdapter(messages);
        binding.rvMessages.setLayoutManager(new LinearLayoutManager(this));
        binding.rvMessages.setAdapter(chatAdapter);

        // Khi bàn phím hiện lên (list bị thu nhỏ) → cuộn xuống tin nhắn mới nhất.
        binding.rvMessages.addOnLayoutChangeListener(
                (v, left, top, right, bottom, oldL, oldT, oldR, oldB) -> {
                    if (bottom < oldB && chatAdapter.getItemCount() > 0) {
                        binding.rvMessages.postDelayed(
                                () -> binding.rvMessages.scrollToPosition(chatAdapter.getItemCount() - 1),
                                100);
                    }
                });

        binding.btnSend.setOnClickListener(v -> sendUserMessage());
        binding.etMessage.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEND) {
                sendUserMessage();
                return true;
            }
            return false;
        });

        buildSuggestionChips();
        sendWelcomeMessage();
    }

    /** Tạo dải gợi ý câu hỏi nhanh; chạm vào một gợi ý sẽ gửi luôn câu đó. */
    private void buildSuggestionChips() {
        LinearLayout container = binding.suggestionContainer;
        String[] suggestions = getResources().getStringArray(R.array.chatbot_suggestions);

        int hPad = dp(14), vPad = dp(8), gap = dp(8);
        for (String suggestion : suggestions) {
            TextView chip = new TextView(this);
            chip.setText(suggestion);
            chip.setTextSize(13);
            chip.setTextColor(ContextCompat.getColor(this, R.color.tc_red));
            chip.setBackgroundResource(R.drawable.tc_bg_chip_suggestion);
            chip.setPadding(hPad, vPad, hPad, vPad);
            chip.setGravity(Gravity.CENTER);
            chip.setSingleLine(true);
            chip.setForeground(rippleForeground());

            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            lp.setMarginEnd(gap);
            chip.setLayoutParams(lp);

            chip.setOnClickListener(v -> sendMessageText(suggestion));
            container.addView(chip);
        }
    }

    /** Hiệu ứng ripple khi chạm chip (dùng thuộc tính selectableItemBackground của theme). */
    private android.graphics.drawable.Drawable rippleForeground() {
        TypedValue tv = new TypedValue();
        getTheme().resolveAttribute(
                android.R.attr.selectableItemBackground, tv, true);
        return ContextCompat.getDrawable(this, tv.resourceId);
    }

    private int dp(int value) {
        return Math.round(TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, value, getResources().getDisplayMetrics()));
    }

    private void sendWelcomeMessage() {
        String menh = preferenceManager.getMenh();
        String intro = getString(R.string.bot_intro_message);
        String welcome = (menh != null)
            ? intro + "\n\nBạn mệnh " + menh
              + " " + MenhCalculator.getEmojiMenh(menh)
              + ". Tôi có thể giúp bạn chọn trang phục hợp mệnh và tư vấn phong cách cá nhân. Bạn muốn hỏi về điều gì?"
            : intro + "\n\nTôi có thể giúp bạn chọn trang phục phù hợp với mệnh và phong cách của bạn. Bạn muốn hỏi về điều gì?";
        receiveMessage(welcome);
    }

    private void sendUserMessage() {
        sendMessageText(binding.etMessage.getText().toString());
    }

    /** Gửi một tin nhắn của người dùng (từ ô nhập hoặc từ chip gợi ý). */
    private void sendMessageText(String raw) {
        String text = raw == null ? "" : raw.trim();
        if (TextUtils.isEmpty(text)) return;

        binding.etMessage.setText("");
        hideKeyboard();

        messages.add(new ChatMessage(text, true));
        chatAdapter.notifyItemInserted(messages.size() - 1);
        binding.rvMessages.scrollToPosition(messages.size() - 1);

        respond(text);
    }

    /**
     * Tra loi nguoi dung: uu tien goi Claude API; neu chua cau hinh key hoac
     * loi mang thi dung bo tra loi rule-based lam phuong an du phong.
     */
    private void respond(String userText) {
        if (!ClaudeApiClient.isConfigured()) {
            receiveMessage(ruleBasedResponse(userText));
            return;
        }

        List<ClaudeRequest.Message> history = buildHistory();
        showTyping();

        ClaudeRequest request = new ClaudeRequest(
                "claude-opus-4-8", 1024, buildSystemPrompt(), history);

        ClaudeApiClient.get().createMessage(request).enqueue(new Callback<ClaudeResponse>() {
            @Override
            public void onResponse(Call<ClaudeResponse> call, Response<ClaudeResponse> resp) {
                hideTyping();
                String text = resp.isSuccessful() && resp.body() != null
                        ? resp.body().text() : null;
                if (text != null && !text.trim().isEmpty()) {
                    receiveMessage(text.trim());
                } else {
                    receiveMessage(ruleBasedResponse(userText)); // du phong khi API loi
                }
            }

            @Override
            public void onFailure(Call<ClaudeResponse> call, Throwable t) {
                hideTyping();
                receiveMessage(ruleBasedResponse(userText));
            }
        });
    }

    /** System prompt dinh nghia tinh cach & pham vi cua tro ly. */
    private String buildSystemPrompt() {
        String menh = preferenceManager.getMenh();
        StringBuilder sb = new StringBuilder();
        sb.append("Ban la tro ly ao cua TiredCity - thuong hieu Viet Phuc va thoi trang sang tao Viet Nam. ")
          .append("Nhiem vu: tu van chon trang phuc hop menh, goi y phoi do, gioi thieu san pham, ")
          .append("giai dap ve gia, size, chat lieu, giao hang va uu dai. ")
          .append("Tra loi bang tieng Viet co dau, than thien, ngan gon, co the dung emoji va gach dau dong. ")
          .append("Neu khong chac thong tin cu the (dia chi, gia, khuyen mai that) thi de nghi khach xem trong app ")
          .append("hoac lien he cua hang, tuyet doi khong bia so lieu.");
        if (menh != null) {
            sb.append(" Khach hang nay menh ").append(menh)
              .append(", hay uu tien goi y mau sac hop menh.");
        }
        return sb.toString();
    }

    /** Chuyen lich su chat thanh danh sach message cho Claude (bo bong bong dang soan). */
    private List<ClaudeRequest.Message> buildHistory() {
        List<ClaudeRequest.Message> out = new ArrayList<>();
        for (ChatMessage m : messages) {
            out.add(new ClaudeRequest.Message(
                    m.isFromUser() ? "user" : "assistant", m.getText()));
        }
        // API yeu cau message dau tien phai la "user" -> bo cac loi chao mo dau cua bot.
        while (!out.isEmpty() && "assistant".equals(out.get(0).role)) {
            out.remove(0);
        }
        return out;
    }

    private void showTyping() {
        messages.add(new ChatMessage("…", false));
        typingIndex = messages.size() - 1;
        chatAdapter.notifyItemInserted(typingIndex);
        binding.rvMessages.scrollToPosition(typingIndex);
    }

    private void hideTyping() {
        if (typingIndex >= 0 && typingIndex < messages.size()) {
            int pos = typingIndex;
            typingIndex = -1;
            messages.remove(pos);
            chatAdapter.notifyItemRemoved(pos);
        }
    }

    private void hideKeyboard() {
        InputMethodManager imm =
                (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        if (imm != null && binding.etMessage.getWindowToken() != null) {
            imm.hideSoftInputFromWindow(binding.etMessage.getWindowToken(), 0);
        }
    }

    /** Bo tra loi rule-based, dung khi chua co key Claude hoac API loi. */
    private String ruleBasedResponse(String userMessage) {
        String lower = userMessage.toLowerCase();
        String response;

        if (matches(lower, "cửa hàng", "cua hang", "địa chỉ", "dia chi", "ở đâu", "o dau", "store", "shop")) {
            response = "TiredCity hiện có mặt tại các thành phố lớn 📍\n\n"
                + "• Hà Nội: 12 Nhà Thờ, Q. Hoàn Kiếm\n"
                + "• TP.HCM: 25 Lý Tự Trọng, Q.1\n"
                + "• Đà Nẵng: 88 Bạch Đằng, Q. Hải Châu\n\n"
                + "Cửa hàng mở cửa 9:00 – 21:00 mỗi ngày. Bạn muốn tôi chỉ đường tới chi nhánh nào gần bạn không? 😊";
        } else if (matches(lower, "giảm", "giam", "sale", "khuyến mãi", "khuyen mai", "ưu đãi", "uu dai", "voucher", "mã giảm")) {
            response = "Đang có vài ưu đãi khá hời đây 🔥\n\n"
                + "• Áo Tấc \"Sen Vàng\" – giảm 30%, còn 1.750.000đ\n"
                + "• Set Áo Giao Lĩnh – mua 1 tặng phụ kiện cài áo\n"
                + "• Nhật Bình bản giới hạn – ưu đãi 15% cho thành viên\n\n"
                + "Nhập mã TIREDCITY10 để giảm thêm 10% cho đơn đầu tiên. Bạn muốn xem chi tiết món nào? 🛍️";
        } else if (matches(lower, "áo dài", "ao dai")) {
            response = "Áo dài là biểu tượng thời trang Việt Nam! ✨\n"
                + "Để chọn áo dài đẹp, bạn nên xem xét:\n"
                + "• Chất liệu: lụa tơ tằm, gấm, voan\n"
                + "• Màu sắc hợp mệnh của bạn\n"
                + "• Phom dáng phù hợp với vóc dáng\n\n"
                + "Bạn muốn áo dài cho dịp nào để tôi gợi ý cụ thể hơn? 😊";
        } else if (matches(lower, "mệnh", "menh", "hợp mệnh", "hop menh")) {
            String menh = preferenceManager.getMenh();
            response = (menh != null)
                ? "Bạn mệnh " + menh + " " + MenhCalculator.getEmojiMenh(menh)
                  + ".\n\nMàu sắc hợp mệnh giúp tăng cường năng lượng tích cực cho bạn. "
                  + "Tôi gợi ý ưu tiên những trang phục có gam màu tương sinh để mang lại may mắn. "
                  + "Bạn muốn tôi gợi ý vài mẫu cụ thể theo mệnh không? ✨"
                : "Tôi chưa có thông tin về mệnh của bạn 🙏\n"
                  + "Hãy vào Hồ sơ → cài đặt ngày sinh để tôi tính toán mệnh và tư vấn màu sắc phù hợp cho bạn nhé!";
        } else if (matches(lower, "phối", "phoi", "mix", "phong cách", "phong cach", "style", "outfit", "lên đồ")) {
            response = "Vài nguyên tắc phối Việt Phục vừa chuẩn vừa hiện đại 👗\n\n"
                + "• Giữ 1 điểm nhấn: nếu áo cầu kỳ thì chân váy/quần nên trơn\n"
                + "• Chọn tông màu theo mệnh làm màu chủ đạo\n"
                + "• Phụ kiện tối giản: trâm cài, túi thêu, hài\n"
                + "• Đi sự kiện thì ưu tiên gấm, dự tiệc nhẹ thì lụa/voan\n\n"
                + "Bạn định phối cho dịp nào để tôi gợi ý set hoàn chỉnh? 😊";
        } else if (matches(lower, "giao hàng", "giao hang", "ship", "vận chuyển", "van chuyen", "phí ship", "freeship")) {
            response = "Thông tin giao hàng của TiredCity 🚚\n\n"
                + "• Nội thành HN & HCM: 20.000đ, giao trong 1–2 ngày\n"
                + "• Tỉnh thành khác: 30.000đ, 3–5 ngày\n"
                + "• Miễn phí giao hàng cho đơn từ 1.000.000đ\n\n"
                + "Bạn có thể theo dõi đơn ở mục Đơn hàng của tôi nhé!";
        } else if (matches(lower, "size", "kích cỡ", "kich co", "số đo", "so do", "vừa không", "form")) {
            response = "Để chọn size chuẩn, bạn cho tôi biết chiều cao & cân nặng nhé 📏\n\n"
                + "Tham khảo nhanh:\n"
                + "• S: 45–52kg\n"
                + "• M: 52–60kg\n"
                + "• L: 60–68kg\n"
                + "• XL: 68–76kg\n\n"
                + "Việt Phục thường có phom rộng nhẹ, nếu bạn ở giữa hai size thì chọn size nhỏ hơn sẽ tôn dáng hơn. 😊";
        } else if (matches(lower, "giá", "gia", "bao nhiêu", "bao nhieu", "price")) {
            response = "Giá sản phẩm TiredCity dao động từ 500.000đ đến 5.000.000đ tuỳ loại trang phục:\n"
                + "• Phụ kiện: 500K – 1.5M\n"
                + "• Áo Tấc / Áo Giao Lĩnh: 1.5M – 3M\n"
                + "• Áo Dài cao cấp: 2M – 5M\n"
                + "• Nhật Bình: 3M – 5M\n\n"
                + "Ghé tab Cửa hàng để xem đầy đủ sản phẩm nhé! 🛍️";
        } else if (matches(lower, "chất liệu", "chat lieu", "vải", "vai", "material")) {
            response = "TiredCity dùng các chất liệu truyền thống được tuyển chọn kỹ 🧵\n\n"
                + "• Lụa tơ tằm: mềm, mát, sang trọng\n"
                + "• Gấm: dày dặn, hoa văn nổi, hợp dịp trang trọng\n"
                + "• Voan/tơ: nhẹ, bay, thoải mái cho ngày thường\n\n"
                + "Bạn muốn ưu tiên sự thoải mái hay vẻ sang trọng để tôi gợi ý chất liệu phù hợp?";
        } else if (matches(lower, "cảm ơn", "cam on", "thanks", "thank")) {
            response = "Rất vui được giúp bạn! 💕 Nếu cần tư vấn thêm về trang phục, mệnh hay ưu đãi, cứ nhắn tôi bất cứ lúc nào nhé.";
        } else if (matches(lower, "chào", "chao", "hello", "hi ", "xin chào", "xin chao")) {
            response = "Chào bạn 👋 Rất vui được gặp bạn! Tôi có thể giúp bạn chọn trang phục hợp mệnh, gợi ý cách phối đồ hoặc thông tin ưu đãi. Bạn đang quan tâm điều gì?";
        } else {
            response = "Câu hỏi hay đấy! Tôi vẫn đang học hỏi mỗi ngày 😊\n"
                + "Hiện tôi có thể tư vấn tốt nhất về:\n"
                + "• Trang phục Việt Phục & áo dài\n"
                + "• Màu sắc hợp mệnh\n"
                + "• Cách phối đồ, chọn size\n"
                + "• Ưu đãi, giá cả & giao hàng\n\n"
                + "Bạn thử chọn một gợi ý bên dưới hoặc hỏi tôi cụ thể hơn nhé!";
        }

        return response;
    }

    /** True nếu văn bản chứa bất kỳ từ khoá nào. */
    private boolean matches(String text, String... keywords) {
        for (String k : keywords) {
            if (text.contains(k)) return true;
        }
        return false;
    }

    private void receiveMessage(String text) {
        messages.add(new ChatMessage(text, false));
        chatAdapter.notifyItemInserted(messages.size() - 1);
        binding.rvMessages.scrollToPosition(messages.size() - 1);
    }
}
