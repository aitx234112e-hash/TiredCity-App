# TiredCity — Monorepo

Hệ thống e-commerce Việt Phục: **3 client** (web quản lý, app admin, app customer) dùng chung **Firebase (Authentication + Cloud Firestore)**, tổ chức theo mô hình monorepo.

> Mở `TiredCity.code-workspace` bằng VS Code để thấy tất cả thành phần dưới dạng multi-root.

## Thành phần

| Thư mục | Vai trò | Stack | Lệnh chạy |
|---|---|---|---|
| `web-admin/` | Website quản lý (A1–A10) | Angular + Firebase | `npm run dev` → :4300 |
| `app-admin/` | App admin di động | Android (Java) | mở bằng Android Studio |
| `app/` (gốc) | App customer | Android | mở bằng Android Studio (thư mục gốc) |

## Bắt đầu nhanh

```bash
# Web quản lý
cd web-admin && npm install && npm run dev
```

App Android (`app/`, `app-admin/`) mở bằng Android Studio.

## Lưu ý
- **App Customer giữ nguyên ở thư mục gốc** (không di chuyển) để bảo toàn git history + Gradle.
- Tất cả client xác thực và lưu dữ liệu qua **Firebase**.
