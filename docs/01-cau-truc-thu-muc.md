# Cấu trúc thư mục monorepo

Thư mục gốc repo vừa là **App Customer (Android)** vừa là **root của monorepo** (giống cách VuaVuiVe gom mọi service vào 1 workspace).

```
TiredCity/                         ← mở bằng TiredCity.code-workspace
├── app/                           📱 App Customer (Android) — module hiện có
├── build.gradle.kts               (Gradle root của app customer)
├── settings.gradle.kts
├── gradle/
│
├── backend/                       🛠️  NestJS API (dùng chung)
│   ├── prisma/schema.prisma
│   └── src/{auth,products,orders,dashboard,cloudinary,prisma}
│
├── web-admin/                     🖥️  Website quản lý (Next.js)
│   └── src/app/{(admin),login}
│
├── app-admin/                     📱 App Admin (Android) — Gradle project riêng
│   └── app/src/main/java/com/tiredcity/admin/{ui,data,adapter,utils}
│
├── docs/                          📄 Tài liệu thiết kế
└── TiredCity.code-workspace       🧩 VS Code multi-root workspace
```

## Vì sao App Customer nằm ở gốc (không phải `app-customer/`)?
App Android customer đã tồn tại sẵn với Gradle root tại thư mục gốc repo và lịch sử git theo đường dẫn `app/`. **Di chuyển nó vào `app-customer/` sẽ phá vỡ git history và cấu hình Gradle**, nên ta giữ nguyên vị trí và gom các surface khác làm thư mục con. VS Code workspace (`.code-workspace`) cho phép mở tất cả như các "root" riêng biệt mà không cần di chuyển file.

## Mở dự án
- **Toàn hệ thống:** mở `TiredCity.code-workspace` bằng VS Code → thấy 5 root: Backend, Web quản lý, App Admin, App Customer, Docs.
- **Riêng app Android:** mở thư mục gốc (customer) hoặc `app-admin/` bằng **Android Studio**.
