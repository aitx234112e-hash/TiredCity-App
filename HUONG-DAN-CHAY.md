# Hướng dẫn tự chạy dự án TiredCity

Repo có 4 phần. Quan hệ phụ thuộc:

```
backend (NestJS, cổng 4000)  ──┬──>  web-admin (Next.js, cổng 3000)   ← "web"
   cần PostgreSQL              └──>  app-admin (Android admin)        ← "app-admin"

app (Android customer)  ── chạy độc lập, KHÔNG cần backend
```

> Quy tắc vàng: **bật backend TRƯỚC**, rồi mới tới web-admin / app-admin. Nếu không, chúng vẫn mở được nhưng mọi thứ cần dữ liệu sẽ báo lỗi gọi mạng.

---

## 1. WEB (web-admin) — giao diện quản lý trên trình duyệt

Mở **PowerShell** hoặc **Terminal**, chạy:

```powershell
cd D:\TiredCity-App-main\TiredCity-App-main\web-admin

# Chỉ chạy 1 LẦN DUY NHẤT (lần đầu hoặc khi đổi máy):
copy .env.local.example .env.local      # tạo file cấu hình
npm install                              # tải thư viện (~1 phút)

# Lệnh chạy MỖI LẦN muốn bật web:
npm run dev
```

→ Mở trình duyệt vào **http://localhost:3000**

- Tắt server: bấm **Ctrl + C** trong cửa sổ terminal đó.
- File `.env.local` đã trỏ sẵn tới backend: `NEXT_PUBLIC_API_URL=http://localhost:4000/api`.

---

## 2. BACKEND (NestJS) — bắt buộc nếu muốn web/app-admin có dữ liệu

### Chuẩn bị 1 lần
Backend cần **PostgreSQL**. Có 2 cách lấy DB:
- **Dễ nhất:** tạo DB miễn phí trên [Neon](https://neon.tech) hoặc [Supabase](https://supabase.com), copy chuỗi `DATABASE_URL` họ cho.
- Hoặc cài PostgreSQL trên máy (cổng 5432).

```powershell
cd D:\TiredCity-App-main\TiredCity-App-main\backend

# Chạy 1 LẦN:
copy .env.example .env          # rồi MỞ file .env, dán DATABASE_URL thật vào
npm install                     # tải thư viện
npm run prisma:generate         # sinh Prisma client
npm run prisma:migrate          # tạo bảng trong DB
npm run prisma:seed             # (tùy chọn) đổ dữ liệu mẫu + cấp quyền admin
```

> Trong `.env` tối thiểu phải sửa `DATABASE_URL`. Các phần Google OAuth / Cloudinary chỉ cần khi dùng đăng nhập Google và upload ảnh — để mặc định vẫn chạy được phần cơ bản.

### Lệnh chạy mỗi lần
```powershell
cd D:\TiredCity-App-main\TiredCity-App-main\backend
npm run start:dev               # tự reload khi sửa code
```

→ Thấy dòng `🚀 TiredCity Admin API: http://localhost:4000/api` là OK.

---

## 3. APP-ADMIN (Android) — app quản lý cho điện thoại

Đây là **Gradle project Android độc lập**, cách dễ nhất là dùng **Android Studio** (không dùng dòng lệnh):

1. Mở **Android Studio** → **File → Open…** → chọn đúng thư mục
   `D:\TiredCity-App-main\TiredCity-App-main\app-admin`
   (mở riêng thư mục `app-admin`, KHÔNG mở thư mục gốc repo).
2. Đợi Android Studio **Sync Gradle** xong (thanh dưới chạy hết).
3. Chọn thiết bị ở thanh trên cùng:
   - Có sẵn emulator **Pixel 4** → chọn nó. Nếu chưa bật: **Device Manager** (bên phải) → bấm nút ▶ cạnh Pixel 4.
4. Bấm nút **▶ Run (Shift + F10)**. App sẽ build, cài và tự mở trên emulator.

> App-admin gọi backend qua `http://10.0.2.2:4000` (địa chỉ đặc biệt để emulator nói chuyện với "localhost" của máy bạn). Nên **backend ở mục 2 phải đang chạy** thì màn Dashboard mới có số liệu. Nếu chạy trên điện thoại thật → đổi địa chỉ thành IP LAN của máy (vd `192.168.1.x:4000`) trong `app-admin/app/build.gradle.kts`.

---

## 4. APP CUSTOMER (Android) — app khách hàng (tham khảo)

Cũng mở bằng Android Studio nhưng **mở thư mục gốc repo**
`D:\TiredCity-App-main\TiredCity-App-main` → chọn module **app** → bấm **▶ Run**.

- Đăng nhập là **demo/offline**: nhập **email + mật khẩu bất kỳ** đều vào được → Onboarding → Trang chủ.
- Không cần backend.

---

## Tóm tắt "mỗi ngày mở lên làm việc"

| Muốn dùng | Lệnh / thao tác | Xem ở đâu |
|-----------|-----------------|-----------|
| Web quản lý | `cd web-admin` → `npm run dev` | http://localhost:3000 |
| Backend (data) | `cd backend` → `npm run start:dev` | http://localhost:4000/api |
| App admin (đt) | Android Studio mở `app-admin` → ▶ Run | trên emulator |
| App khách hàng | Android Studio mở repo gốc → module `app` → ▶ Run | trên emulator |

## Lỗi hay gặp

- **Web mở được nhưng trang trắng / lỗi gọi API** → backend chưa bật (mục 2).
- **`npm` báo "not recognized"** → chưa cài [Node.js](https://nodejs.org) (bản LTS). Cài xong mở lại terminal.
- **Cửa sổ emulator nhảy lên sát mép trên màn hình** → kéo thanh tiêu đề của nó xuống; hoặc trong Android Studio dùng **View → Tool Windows → Running Devices** để xem emulator nhúng ngay trong AS.
- **Backend lỗi `Can't reach database`** → `DATABASE_URL` trong `backend/.env` sai hoặc DB chưa bật.
- **Port bị chiếm (3000/4000 đã dùng)** → tắt tiến trình cũ, hoặc đổi `PORT` trong `.env`.
