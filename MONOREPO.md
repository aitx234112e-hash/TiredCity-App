# TiredCity — Monorepo

Hệ thống e-commerce Việt Phục: **1 backend** + **3 client** (web quản lý, app admin, app customer), tổ chức theo mô hình monorepo giống dự án tham khảo VuaVuiVe.

> Mở `TiredCity.code-workspace` bằng VS Code để thấy tất cả thành phần dưới dạng multi-root.

## Thành phần

| Thư mục | Vai trò | Stack | Lệnh chạy |
|---|---|---|---|
| `backend/` | API + Auth + RBAC (dùng chung) | NestJS + Prisma + PostgreSQL | `npm run start:dev` → :4000 |
| `web-admin/` | Website quản lý (A1–A10) | Next.js 14 + Tailwind | `npm run dev` → :3000 |
| `app-admin/` | App admin di động | Android (Java) | mở bằng Android Studio |
| `app/` (gốc) | App customer | Android | mở bằng Android Studio (thư mục gốc) |
| `docs/` | Tài liệu kiến trúc | Markdown | — |

## Bắt đầu nhanh

```bash
# 1) Backend
cd backend && cp .env.example .env   # điền DATABASE_URL, Google OAuth, Cloudinary
npm install && npm run prisma:migrate && npm run prisma:seed && npm run start:dev

# 2) Web quản lý
cd ../web-admin && cp .env.local.example .env.local
npm install && npm run dev
```

App Android (`app/`, `app-admin/`) mở bằng Android Studio, trỏ `API_BASE_URL` về backend.

## Tài liệu
- [Tổng quan kiến trúc](docs/00-tong-quan-kien-truc.md)
- [Cấu trúc thư mục](docs/01-cau-truc-thu-muc.md)
- [Hướng dẫn chi tiết Backend + Web](backend/.env.example) · [Web README](web-admin)

## Lưu ý
- **App Customer giữ nguyên ở thư mục gốc** (không di chuyển) để bảo toàn git history + Gradle. Xem lý do trong [docs/01](docs/01-cau-truc-thu-muc.md).
- Tất cả client xác thực qua **Google OAuth 2.0 → JWT → RBAC** từ backend.
