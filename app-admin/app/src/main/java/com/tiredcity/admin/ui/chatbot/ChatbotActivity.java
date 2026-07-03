package com.tiredcity.admin.ui.chatbot;

import android.os.Bundle;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.tiredcity.admin.R;
import com.tiredcity.admin.adapter.ChatAdapter;
import com.tiredcity.admin.data.model.ChatMessage;
import com.tiredcity.admin.databinding.ActivityChatbotBinding;
import com.tiredcity.admin.utils.DocUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Tro ly Admin — chatbot theo luat (rule-based), mirror trang chatbot ben web-admin.
 * Doc san orders/users/products tu Firestore roi tra loi cac cau hoi ve
 * doanh thu, don hang, nguoi dung, ton kho va tra cuu don theo ma.
 */
public class ChatbotActivity extends AppCompatActivity {

    private static final Pattern ORDER_ID = Pattern.compile(
            "(?:đơn|order|mã)\\s*#?\\s*([a-z0-9_-]{4,})",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);

    private static final String[] SUGGESTIONS = {
            "Doanh thu hiện tại?",
            "Có bao nhiêu đơn hàng?",
            "Bao nhiêu người dùng?",
            "Đơn nào đang chờ xử lý?",
            "Sản phẩm sắp hết hàng",
    };

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private ActivityChatbotBinding binding;
    private ChatAdapter adapter;

    private List<DocumentSnapshot> orders = new ArrayList<>();
    private List<DocumentSnapshot> users = new ArrayList<>();
    private List<DocumentSnapshot> products = new ArrayList<>();

    private boolean ready = false;
    private int loaded = 0;
    private boolean errorShown = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityChatbotBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        getWindow().setStatusBarColor(ContextCompat.getColor(this, R.color.m_chatbot));
        binding.btnBack.setOnClickListener(v -> finish());

        adapter = new ChatAdapter();
        LinearLayoutManager lm = new LinearLayoutManager(this);
        lm.setStackFromEnd(true);
        binding.rvMessages.setLayoutManager(lm);
        binding.rvMessages.setAdapter(adapter);

        buildSuggestions();

        binding.btnSend.setOnClickListener(v -> ask(binding.etInput.getText().toString()));
        binding.etInput.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEND) {
                ask(binding.etInput.getText().toString());
                return true;
            }
            return false;
        });

        addBot(getString(R.string.chatbot_greeting));
        loadData();
    }

    private void buildSuggestions() {
        float d = getResources().getDisplayMetrics().density;
        for (String s : SUGGESTIONS) {
            TextView chip = new TextView(this);
            chip.setText(s);
            chip.setTextSize(13f);
            chip.setTextColor(ContextCompat.getColor(this, R.color.tc_brand));
            chip.setBackgroundResource(R.drawable.bg_chip_suggestion);
            chip.setPadding((int) (14 * d), (int) (8 * d), (int) (14 * d), (int) (8 * d));
            android.widget.LinearLayout.LayoutParams lp =
                    new android.widget.LinearLayout.LayoutParams(
                            android.view.ViewGroup.LayoutParams.WRAP_CONTENT,
                            android.view.ViewGroup.LayoutParams.WRAP_CONTENT);
            lp.setMarginEnd((int) (8 * d));
            chip.setLayoutParams(lp);
            chip.setOnClickListener(v -> ask(s));
            binding.suggestionRow.addView(chip);
        }
    }

    // ------------------------------------------------------------------ data

    private void loadData() {
        db.collection("orders").get()
                .addOnSuccessListener(s -> { orders = s.getDocuments(); onDone(true); })
                .addOnFailureListener(e -> onDone(false));
        db.collection("users").get()
                .addOnSuccessListener(s -> { users = s.getDocuments(); onDone(true); })
                .addOnFailureListener(e -> onDone(false));
        db.collection("products").get()
                .addOnSuccessListener(s -> { products = s.getDocuments(); onDone(true); })
                .addOnFailureListener(e -> onDone(false));
    }

    private void onDone(boolean ok) {
        loaded++;
        if (!ok && !errorShown) {
            errorShown = true;
            addBot(getString(R.string.chatbot_data_error));
        }
        if (loaded >= 3) {
            ready = true;
            binding.tvStatus.setText(R.string.chatbot_ready);
        }
    }

    // ------------------------------------------------------------------ chat

    private void ask(String raw) {
        String text = raw == null ? "" : raw.trim();
        if (text.isEmpty()) return;

        addUser(text);
        binding.etInput.setText("");

        addBot(answer(text.toLowerCase(new Locale("vi", "VN"))));

        if (adapter.size() > 2) binding.suggestionScroll.setVisibility(View.GONE);
    }

    private void addBot(String t) {
        adapter.add(new ChatMessage(ChatMessage.FROM_BOT, t));
        scrollDown();
    }

    private void addUser(String t) {
        adapter.add(new ChatMessage(ChatMessage.FROM_USER, t));
        scrollDown();
    }

    private void scrollDown() {
        binding.rvMessages.post(() -> binding.rvMessages.scrollToPosition(adapter.size() - 1));
    }

    // ------------------------------------------------------------------ logic

    private String answer(String q) {
        if (!ready) return "Mình đang tải dữ liệu, bạn đợi vài giây rồi hỏi lại nhé.";

        // Tra cuu don theo ma
        Matcher m = ORDER_ID.matcher(q);
        if (m.find() && (q.contains("tra") || q.contains("tìm") || q.contains("#") || q.contains("mã"))) {
            String id = m.group(1).toLowerCase(Locale.ROOT);
            DocumentSnapshot found = null;
            for (DocumentSnapshot o : orders) {
                String oid = DocUtils.str(o, "orderID").toLowerCase(Locale.ROOT);
                if (oid.contains(id) || o.getId().toLowerCase(Locale.ROOT).contains(id)) {
                    found = o;
                    break;
                }
            }
            if (found != null) {
                String code = DocUtils.str(found, "orderID");
                if (code.isEmpty()) code = found.getId();
                String cust = DocUtils.str(found, "userName");
                if (cust.isEmpty()) cust = "Guest";
                String status = DocUtils.str(found, "status");
                if (status.isEmpty()) status = "—";
                String date = DocUtils.date(found, "createdAt");
                if (date.isEmpty()) date = "—";
                return "Đơn " + code + ": khách \"" + cust + "\", tổng "
                        + DocUtils.money(DocUtils.num(found, "totalPrice", "total", "amount"))
                        + ", trạng thái \"" + status + "\", đặt ngày " + date + ".";
            }
            return "Mình không tìm thấy đơn nào khớp mã \"" + id + "\".";
        }

        // Doanh thu
        if (q.contains("doanh thu") || q.contains("revenue") || q.contains("tiền")) {
            return "Tổng doanh thu hiện tại là " + DocUtils.money(revenue())
                    + " từ " + orders.size() + " đơn hàng.";
        }

        // Don cho xu ly
        if ((q.contains("chờ") || q.contains("pending") || q.contains("xử lý")) && q.contains("đơn")) {
            int n = countStatus("pending");
            return "Hiện có " + n + " đơn đang chờ xử lý (pending)"
                    + (n > 0 ? ". Bạn vào mục Đơn hàng để xác nhận nhé." : ".");
        }

        // Don hang tong quat
        if (q.contains("đơn") || q.contains("order")) {
            return "Tổng cộng " + orders.size() + " đơn hàng — "
                    + countStatus("pending") + " chờ xử lý, "
                    + countStatus("processing") + " đang chuẩn bị, "
                    + countStatus("shipped") + " đang giao, "
                    + countStatus("delivered") + " đã giao, "
                    + countStatus("cancelled") + " đã huỷ.";
        }

        // Nguoi dung
        if (q.contains("người dùng") || q.contains("user") || q.contains("khách") || q.contains("tài khoản")) {
            int admins = 0;
            for (DocumentSnapshot u : users) {
                String r = DocUtils.str(u, "role");
                if ("admin".equals(r) || "superadmin".equals(r)) admins++;
            }
            return "Có " + users.size() + " tài khoản (" + admins + " admin, "
                    + (users.size() - admins) + " khách hàng).";
        }

        // San pham sap het
        if (q.contains("hết hàng") || q.contains("sắp hết") || q.contains("tồn kho") || q.contains("low stock")) {
            List<DocumentSnapshot> low = new ArrayList<>();
            for (DocumentSnapshot p : products) {
                if (DocUtils.num(p, "stocked_quantity", "stock") <= 5) low.add(p);
            }
            if (low.isEmpty()) return "Không có sản phẩm nào sắp hết hàng (tồn kho ≤ 5). 👍";
            StringBuilder sb = new StringBuilder();
            int lim = Math.min(5, low.size());
            for (int i = 0; i < lim; i++) {
                DocumentSnapshot p = low.get(i);
                String name = DocUtils.str(p, "product_name", "name");
                long st = (long) DocUtils.num(p, "stocked_quantity", "stock");
                if (i > 0) sb.append(", ");
                sb.append(name).append(" (").append(st).append(")");
            }
            return "Có " + low.size() + " sản phẩm tồn kho thấp: " + sb
                    + (low.size() > 5 ? "…" : "") + ".";
        }

        // San pham tong quat
        if (q.contains("sản phẩm") || q.contains("product")) {
            return "Cửa hàng đang có " + products.size() + " sản phẩm.";
        }

        // Chao hoi
        if (q.contains("chào") || q.contains("hello") || q.contains("hi") || q.contains("giúp")) {
            return "Mình có thể trả lời: doanh thu, số đơn hàng & trạng thái, số người dùng, "
                    + "sản phẩm tồn kho thấp, và tra cứu đơn theo mã (VD: \"tra đơn #ABC123\").";
        }

        return "Mình chưa hiểu câu hỏi này 🤔. Bạn thử hỏi về \"doanh thu\", \"đơn hàng\", "
                + "\"người dùng\", \"sản phẩm\", hoặc \"tra đơn #<mã>\".";
    }

    private double revenue() {
        double sum = 0;
        for (DocumentSnapshot o : orders) sum += DocUtils.num(o, "totalPrice", "total", "amount");
        return sum;
    }

    private int countStatus(String status) {
        int n = 0;
        for (DocumentSnapshot o : orders) {
            if (status.equals(DocUtils.str(o, "status").toLowerCase(Locale.ROOT))) n++;
        }
        return n;
    }
}
