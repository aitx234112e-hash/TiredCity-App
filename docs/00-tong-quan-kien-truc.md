# TiredCity — Tổng quan kiến trúc

Hệ thống e-commerce Việt Phục gồm **4 client surface** dùng chung **1 backend**.

## Sơ đồ tổng thể

```
                       ┌─────────────────────────────┐
                       │   backend/  (NestJS API)     │
                       │   PostgreSQL + Prisma        │
                       │   :4000  /api                │
                       └──────────────┬──────────────┘
            ┌─────────────────────────┼─────────────────────────┐
            │                         │                          │
   ┌────────▼────────┐     ┌──────────▼─────────┐     ┌──────────▼─────────┐
   │ web-admin/      │     │ app-admin/         │     │ app/ (customer)    │
   │ Next.js  :3000  │     │ Android (admin)    │     │ Android (khách)    │
   │ Website quản lý │     │ com.tiredcity.admin│     │ com.tiredcity.app  │
   └─────────────────┘     └────────────────────┘     └────────────────────┘
```

## Vai trò từng surface

| Surface | Thư mục | Công nghệ | Người dùng | Mô tả |
|---|---|---|---|---|
| **Website quản lý** | `web-admin/` | Next.js 14 + Tailwind | Admin / Staff | Back-office đầy đủ: A1–A10 |
| **App Admin** | `app-admin/` | Android (Java) | Admin / Staff | Bản di động gọn: dashboard, duyệt đơn |
| **App Customer** | `app/` | Android (Java/Kotlin) | Khách hàng | Mua sắm, Mệnh card, Styling… (đang có) |
| **Backend** | `backend/` | NestJS + Prisma + PostgreSQL | — | API + Auth + RBAC dùng chung |

## Cổng (port) khi chạy local

| Service | URL |
|---|---|
| Backend API | http://localhost:4000/api |
| Web quản lý | http://localhost:3000 |
| App Admin / Customer (emulator) | gọi backend qua http://10.0.2.2:4000/api |

## Xác thực dùng chung
Cả 3 client đăng nhập bằng **Google OAuth 2.0**, backend cấp **JWT** và phân quyền **RBAC** (`CUSTOMER / STAFF / ADMIN / AUDITOR`).
- Web: nhận JWT qua cookie `httpOnly`.
- App Android: nhận JWT qua response, lưu `SharedPreferences`, gắn header `Authorization: Bearer`.

## Mapping 10 module Admin → nơi triển khai

| Module | Web quản lý | App Admin |
|---|---|---|
| A1 Auth | ✅ | ✅ |
| A2 Dashboard | ✅ | ✅ (rút gọn) |
| A3 Orders | ✅ | ✅ |
| A4 Products | ✅ | ⛔ (quản lý nặng → để web) |
| A5–A10 | ✅ | tùy nhu cầu |
