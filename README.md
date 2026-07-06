# TiredCity

Hệ thống e-commerce **Việt Phục** (trang phục truyền thống) gồm 3 client dùng chung **Firebase** (Authentication + Cloud Firestore): app khách hàng, app quản trị và web quản trị.

> Đây là repo monorepo. Xem thêm chi tiết tổ chức thư mục ở [MONOREPO.md](MONOREPO.md). Mở `TiredCity.code-workspace` bằng VS Code để thấy tất cả thành phần dưới dạng multi-root workspace.

## Thành phần

| Thư mục | Vai trò | Stack | Lệnh chạy |
|---|---|---|---|
| `app/` | App khách hàng (Customer) | Android (Java) | Mở bằng Android Studio |
| `app-admin/` | App quản trị di động | Android (Java) | Mở bằng Android Studio |
| `web-admin/` | Website quản trị | Angular + Firebase | `cd web-admin && npm install && npm run start` → :4300 |

## Kiến trúc xác thực

Chỉ có **một trang đăng nhập** duy nhất, nằm ở app khách hàng, với toggle **KHÁCH HÀNG / QUẢN TRỊ**. Nếu mở trực tiếp app-admin, ứng dụng sẽ điều hướng về trang đăng nhập chung; form đăng nhập riêng trong app-admin chỉ là phương án dự phòng.

## Dữ liệu dùng chung

Cả app khách hàng và cả hai app admin (di động + web) đều đọc/ghi chung một Firestore, quan trọng nhất là collection `products` (sản phẩm) và `orders` (đơn hàng — dùng trường `orderID`/`status`).

## Bắt đầu nhanh

### Web quản lý
```bash
cd web-admin
npm install
npm run start
```

### App Android (`app/`, `app-admin/`)
Mở thư mục tương ứng bằng Android Studio và build/run trên emulator hoặc thiết bị thật. Repo không có sẵn `gradlew`; dùng bản Gradle đã cache và JBR của Android Studio làm `JAVA_HOME`.

## Tính năng AI

- **Gợi ý phong cách theo mệnh**: tính mệnh theo công thức nạp âm can-chi, hiển thị tranh minh họa tương ứng.
- **Lời khuyên phong cách (Gemini)**: gọi thẳng Gemini API miễn phí từ app khách ([GeminiStylist.java](app/src/main/java/com/tiredcity/app/utils/GeminiStylist.java)) — lấy khoá ở [aistudio.google.com](https://aistudio.google.com) rồi thêm dòng `GEMINI_API_KEY=...` vào `local.properties` (file này không commit lên git).
- **AI Agent trong chat** (n8n Cloud + Gemini): xem hướng dẫn cấu hình ở [docs/n8n-ai-agent-setup.md](docs/n8n-ai-agent-setup.md).

## Ghi chú

- App Customer giữ nguyên ở thư mục gốc (không di chuyển) để bảo toàn git history và cấu hình Gradle.
- Tất cả client xác thực và lưu dữ liệu qua Firebase.
