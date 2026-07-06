# TiredCity — AI Agent trên n8n (Hướng A)

Hướng dẫn dựng chatbot khách hiện tại thành **AI Agent** biết gọi tool lấy dữ liệu
thật (đơn hàng, giá, tồn kho) và chuyển người thật khi cần.

- **LLM:** Google Gemini (free — key từ https://aistudio.google.com)
- **n8n:** n8n Cloud (có HTTPS sẵn, không vướng `10.0.2.2`/cleartext của Android)
- **Firestore:** kết nối bằng **Service Account** (đọc `orders`, `products`; ghi `support_tickets`)

Sơ đồ:

```
ChatBotActivity ──POST {message, sessionId, orderId?}──▶ n8n Webhook
                                                              │
                                                    [AI Agent · Gemini]
                                                    (system prompt bên dưới)
                                          ┌──────────────────┼──────────────────┐
                                    get_order_status    get_product      escalate_to_human
                                    Firestore orders    Firestore products  Firestore support_tickets
                                                              │            + Telegram (tuỳ chọn)
ChatBotActivity ◀──────── { reply } ──────── Respond to Webhook
```

---

## 0) Chuẩn bị credentials (làm 1 lần)

### 0.1 Gemini API key
1. Vào https://aistudio.google.com → **Get API key** → tạo key (không cần thẻ).
2. Lưu lại chuỗi key.

### 0.2 Firebase Service Account (để n8n đọc/ghi Firestore)
1. Firebase Console → ⚙️ **Project settings** → tab **Service accounts**.
2. **Generate new private key** → tải file JSON về (giữ bí mật, KHÔNG commit).
3. Mở file JSON, sẽ dùng 3 field: `project_id`, `client_email`, `private_key`.

> Service Account bỏ qua Security Rules → n8n đọc/ghi được mà không phải nới rule.

---

## 1) Tạo workflow n8n

Tạo workflow mới, thêm các node theo thứ tự dưới.

### Node 1 — Webhook (nhận request từ app)
- Type: **Webhook**
- HTTP Method: **POST**
- Path: `tiredcity-agent`
- Respond: **Using 'Respond to Webhook' node**
- Sau khi Activate, URL production dạng:
  `https://<your>.app.n8n.cloud/webhook/tiredcity-agent`

App sẽ gửi body JSON:
```json
{ "message": "đơn TC12345 tới đâu rồi?", "sessionId": "user-abc", "orderId": "" }
```

### Node 2 — AI Agent
- Type: **AI Agent** (Tools Agent)
- **Chat Model** con: **Google Gemini Chat Model** → chọn credential Gemini (mục 0.1),
  model `gemini-2.5-flash`.
- **Memory** con (tuỳ chọn nhưng nên có): **Window Buffer Memory**,
  Session Key = `{{ $json.body.sessionId }}` → giữ ngữ cảnh theo từng khách.
- **User message:** `{{ $json.body.message }}`
- **System Message:** dán nguyên khối ở **Mục 2** bên dưới.
- **Tools:** gắn 3 tool ở **Mục 3**.

### Node 3 — Respond to Webhook
- Type: **Respond to Webhook**
- Respond With: **JSON**
- Body:
```json
{ "reply": "{{ $json.output }}" }
```

---

## 2) System Prompt (dán vào AI Agent → System Message)

```
Bạn là trợ lý AI của TiredCity, thương hiệu thời trang/lifestyle in artwork bản quyền.
Nhiệm vụ: tư vấn sản phẩm và hỗ trợ CSKH.

QUY TẮC BẮT BUỘC:
- KHÔNG được tự bịa thông tin tồn kho, giá, trạng thái đơn hàng.
  Luôn gọi tool tương ứng để lấy dữ liệu thật:
  • Hỏi về đơn hàng / trạng thái / theo dõi → gọi get_order_status.
  • Hỏi về giá / còn hàng / tồn kho của sản phẩm → gọi get_product.
- Nếu thiếu thông tin để gọi tool (vd: chưa có mã đơn hàng), hỏi lại khách.
  Mã đơn TiredCity có dạng ví dụ TC12345.
- Nếu khách bực bội, yêu cầu gặp người thật, hoặc vấn đề vượt phạm vi
  (khiếu nại nghiêm trọng, hoàn tiền lớn) → gọi escalate_to_human, rồi báo khách
  rằng yêu cầu đã được chuyển tới nhân viên và sẽ liên hệ lại sớm.
- Giọng điệu: thân thiện, trẻ trung, đúng chất TiredCity. Trả lời tiếng Việt có dấu,
  ngắn gọn, có thể dùng emoji. Không trả về markdown thô/bảng.
```

---

## 3) Định nghĩa 3 Tool

Mỗi tool là 1 sub-node gắn vào cổng **Tool** của AI Agent.

### Tool A — `get_order_status`  (Firestore đọc collection `orders`)

Dùng node **Google Firebase Cloud Firestore Tool** (hoặc HTTP Request Tool nếu bản n8n
không có node Firestore Tool — xem Phụ lục).

- **Tool Name:** `get_order_status`
- **Description (cho LLM biết khi nào gọi):**
  `Lấy trạng thái và thông tin một đơn hàng theo mã đơn (orderID, ví dụ TC12345). Dùng khi khách hỏi đơn tới đâu, khi nào giao, trạng thái đơn.`
- **Tham số LLM tự điền:** `orderId` (string, bắt buộc).
- **Cấu hình node:**
  - Credential: Service Account (mục 0.2)
  - Project ID: `project_id` từ file JSON
  - Operation: **Get Many / Query**
  - Collection: `orders`
  - Filter: field `orderID` **==** `{{ $fromAI("orderId") }}`
- **Field trả về khách cần:** `status`, `createdAt`, `totalPrice`, `userName`.

> Ý nghĩa `status`: `pending` (chờ xác nhận) → `processing` (đang chuẩn bị) →
> `shipped` (đang giao) → `delivered` (đã giao) / `cancelled` (đã huỷ).
> Nên để 1 dòng map các trạng thái này sang tiếng Việt ngay trong Description
> để Gemini diễn giải cho khách.

### Tool B — `get_product`  (Firestore đọc collection `products`)

- **Tool Name:** `get_product`
- **Description:**
  `Tra giá và tồn kho một sản phẩm theo tên. Dùng khi khách hỏi giá bao nhiêu, còn hàng không, size nào còn.`
- **Tham số LLM:** `productName` (string).
- **Cấu hình:**
  - Collection: `products`
  - Query field `product_name` chứa/khớp `{{ $fromAI("productName") }}`
    (Firestore không có "contains" — có thể lấy toàn bộ rồi để Agent lọc, hoặc
    query gần đúng bằng range `>=`/`<`; với đồ án lấy vài chục SP rồi lọc là đủ).
- **Field trả về:** `product_name`, giá, `stocked_quantity` (tồn kho).

### Tool C — `escalate_to_human`  (Firestore ghi `support_tickets` + Telegram)

- **Tool Name:** `escalate_to_human`
- **Description:**
  `Chuyển yêu cầu cho nhân viên thật. Gọi khi khách bực bội, đòi gặp người thật, khiếu nại nặng hoặc hoàn tiền lớn.`
- **Tham số LLM:** `reason` (string — tóm tắt vấn đề), `sessionId` (string).
- **Cấu hình:**
  1. Node **Firestore → Create** vào collection `support_tickets`:
     ```
     { reason, sessionId, status: "open", createdAt: <now> }
     ```
  2. (Tuỳ chọn) Node **Telegram → Send Message** báo cho admin:
     `🆘 Ticket mới từ ${sessionId}: ${reason}`
- Sau khi ghi xong, Agent báo khách: "Mình đã chuyển yêu cầu tới nhân viên…".

---

## 4) Test workflow (trước khi đụng tới app)

Trong n8n bấm **Execute Workflow**, hoặc gọi bằng curl:

```bash
curl -X POST https://<your>.app.n8n.cloud/webhook/tiredcity-agent \
  -H "Content-Type: application/json" \
  -d '{"message":"đơn TC12345 tới đâu rồi?","sessionId":"test-1","orderId":""}'
```

Kỳ vọng: Agent hỏi lại mã đơn hoặc (nếu có mã) gọi `get_order_status` và trả trạng thái thật.
Thử tiếp: `"áo Sen Vàng còn hàng không?"` → gọi `get_product`.
Thử: `"cho tôi gặp nhân viên, tôi rất bực"` → gọi `escalate_to_human`.

Khi cả 3 luồng chạy đúng → chuyển sang nối app (phần code, làm ở bước sau).

---

## 5) Nối app (tóm tắt — sẽ code ở bước sau)

Trong [ChatBotActivity.java](../app/src/main/java/com/tiredcity/app/ui/styling/ChatBotActivity.java)
hiện `respond()` đang gọi thẳng Claude. Ta sẽ:
1. Thêm `N8nAgentClient` (Retrofit) trỏ tới webhook URL.
2. `respond()` POST `{message, sessionId, orderId}` → nhận `{reply}` → hiển thị.
3. Giữ nguyên `ruleBasedResponse()` làm fallback khi mạng lỗi (đã có sẵn).

> `sessionId` nên dùng userId/uid hiện có để Agent nhớ theo từng khách.

---

## Phụ lục — Nếu n8n không có node "Firestore Tool"

Dùng **HTTP Request Tool** gọi Firestore REST:
```
GET https://firestore.googleapis.com/v1/projects/<PROJECT_ID>/databases/(default)/documents/orders
```
kèm OAuth2 (Service Account) trong Credentials của n8n. Cách này linh hoạt nhưng
phải tự parse JSON `documents[].fields.status.stringValue`. Với đồ án, ưu tiên node
Firestore Tool sẵn có cho gọn.
