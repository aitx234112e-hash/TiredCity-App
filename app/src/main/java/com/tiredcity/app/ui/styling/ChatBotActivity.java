package com.tiredcity.app.ui.styling;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.inputmethod.EditorInfo;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.tiredcity.app.adapter.ChatMessageAdapter;
import com.tiredcity.app.data.model.ChatMessage;
import com.tiredcity.app.databinding.ActivityChatbotBinding;
import com.tiredcity.app.ui.base.BaseActivity;
import com.tiredcity.app.utils.MenhCalculator;
import java.util.ArrayList;
import java.util.List;

public class ChatBotActivity extends BaseActivity {

    private ActivityChatbotBinding binding;
    private ChatMessageAdapter chatAdapter;
    private final List<ChatMessage> messages = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityChatbotBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Trợ lý thời trang AI");
        }
        binding.toolbar.setNavigationOnClickListener(v -> finish());

        chatAdapter = new ChatMessageAdapter(messages);
        binding.rvMessages.setLayoutManager(new LinearLayoutManager(this));
        binding.rvMessages.setAdapter(chatAdapter);

        binding.btnSend.setOnClickListener(v -> sendUserMessage());
        binding.etMessage.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEND) {
                sendUserMessage();
                return true;
            }
            return false;
        });

        sendWelcomeMessage();
    }

    private void sendWelcomeMessage() {
        String menh = preferenceManager.getMenh();
        String welcome = (menh != null)
            ? "Xin chào! Tôi là trợ lý thời trang Việt Phục 🎨\n\nBạn mệnh " + menh
              + " " + MenhCalculator.getEmojiMenh(menh)
              + ". Tôi có thể giúp bạn chọn trang phục hợp mệnh và tư vấn phong cách cá nhân. Bạn muốn hỏi về điều gì?"
            : "Xin chào! Tôi là trợ lý thời trang Việt Phục 🎨\n\nTôi có thể giúp bạn chọn trang phục phù hợp với mệnh và phong cách của bạn. Bạn muốn hỏi về điều gì?";
        receiveMessage(welcome);
    }

    private void sendUserMessage() {
        String text = binding.etMessage.getText().toString().trim();
        if (TextUtils.isEmpty(text)) return;

        binding.etMessage.setText("");
        messages.add(new ChatMessage(text, true));
        chatAdapter.notifyItemInserted(messages.size() - 1);
        binding.rvMessages.scrollToPosition(messages.size() - 1);

        processBotResponse(text);
    }

    private void processBotResponse(String userMessage) {
        // Simple rule-based responses while waiting for Claude API integration
        String lower = userMessage.toLowerCase();
        String response;

        if (lower.contains("áo dài") || lower.contains("ao dai")) {
            response = "Áo dài là biểu tượng thời trang Việt Nam! "
                + "Để chọn áo dài đẹp, bạn nên xem xét:\n"
                + "• Chất liệu: lụa tơ tằm, gấm, voan\n"
                + "• Màu sắc hợp mệnh của bạn\n"
                + "• Phom dáng phù hợp với vóc dáng\n\n"
                + "Bạn muốn tôi gợi ý cụ thể hơn không? 😊";
        } else if (lower.contains("mệnh") || lower.contains("menh")) {
            String menh = preferenceManager.getMenh();
            response = (menh != null)
                ? "Bạn mệnh " + menh + " " + MenhCalculator.getEmojiMenh(menh)
                  + ".\n\nMàu sắc hợp mệnh của bạn giúp tăng cường năng lượng tích cực. "
                  + "Hãy chọn những trang phục có gam màu phù hợp để mang lại may mắn! ✨"
                : "Tôi chưa có thông tin về mệnh của bạn. "
                  + "Hãy vào Hồ sơ → cài đặt ngày sinh để tôi tính toán mệnh cho bạn nhé!";
        } else if (lower.contains("giá") || lower.contains("gia")) {
            response = "Giá sản phẩm Tired City dao động từ 500.000đ đến 5.000.000đ tuỳ loại trang phục:\n"
                + "• Phụ kiện: 500K - 1.5M\n"
                + "• Áo Tấc / Áo Giao Lĩnh: 1.5M - 3M\n"
                + "• Áo Dài cao cấp: 2M - 5M\n"
                + "• Nhật Bình: 3M - 5M\n\n"
                + "Ghé tab Cửa hàng để xem đầy đủ sản phẩm nhé! 🛍️";
        } else {
            response = "Cảm ơn bạn đã hỏi! Tôi đang học hỏi để trở thành trợ lý thời trang tốt hơn. "
                + "Bạn có thể hỏi tôi về:\n"
                + "• Trang phục Việt Phục\n"
                + "• Màu sắc hợp mệnh\n"
                + "• Phong cách và cách phối đồ\n"
                + "• Giá cả và chất liệu vải 😊";
        }

        receiveMessage(response);
    }

    private void receiveMessage(String text) {
        messages.add(new ChatMessage(text, false));
        chatAdapter.notifyItemInserted(messages.size() - 1);
        binding.rvMessages.scrollToPosition(messages.size() - 1);
    }
}
