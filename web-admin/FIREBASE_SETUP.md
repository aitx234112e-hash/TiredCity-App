# Kết nối Firebase (Auth + Firestore)

Backend cũ (`my-server` + MongoDB) đã được thay bằng **Firebase Authentication + Cloud Firestore** thông qua `@angular/fire`.

## 1. Tạo project Firebase
1. Vào https://console.firebase.google.com → **Add project**.
2. **Build → Authentication → Get started → Sign-in method → bật Email/Password**.
3. **Build → Firestore Database → Create database** (chọn *test mode* khi đang phát triển).

## 2. Dán config vào app
Firebase Console → ⚙️ **Project settings → Your apps → Web app** → copy object `firebaseConfig`,
dán vào cả hai file (thay các giá trị `YOUR_...`):
- `src/environments/environment.ts`
- `src/environments/environment.prod.ts`

## 3. Tạo tài khoản admin (để đăng nhập)
1. **Authentication → Users → Add user** (nhập email + password) → copy **User UID**.
2. **Firestore → Start collection** tên `users` → **Document ID = đúng UID vừa copy** → thêm các field:
   - `role` = `admin` *(bắt buộc — chỉ `admin`/`superadmin` mới vào được trang quản trị)*
   - `profileName` = `Admin`
   - `email`, `fullName`, `phone`, `gender` … (tuỳ chọn)

Đăng nhập tại `/login` bằng email/password đó → vào `/admin`.

## 4. Các collection Firestore đang dùng
| Collection  | Dùng ở |
|-------------|--------|
| `users`     | Đăng nhập, User management |
| `products`  | Product management |
| `orders`    | Order management, thống kê dashboard |
| `feedback`  | Feedback management |
| `blogs`     | Blog management |
| `addresses` | Địa chỉ (field `userId` để lọc theo user) |

Mỗi document được đọc ra kèm `_id` = document id (giữ tương thích với code cũ dùng `_id`).

## 5. Firestore Security Rules
Khi phát triển có thể để tạm (test mode). Khi cần siết, ví dụ chỉ admin ghi:
```
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    function isAdmin() {
      return request.auth != null &&
        get(/databases/$(database)/documents/users/$(request.auth.uid)).data.role in ['admin','superadmin'];
    }
    match /{document=**} {
      allow read: if true;
      allow write: if isAdmin();
    }
  }
}
```

## Lưu ý
- **Ảnh**: hiện lưu dưới dạng **base64 data URL ngay trong Firestore** (chưa dùng Firebase Storage).
  Firestore giới hạn 1 document ~1MB nên ảnh lớn có thể vượt hạn mức → nên chuyển sang
  `@angular/fire/storage` (`uploadBytes` + `getDownloadURL`) cho sản phẩm thật. Chỗ cần đổi:
  `ProductApiService.uploadImage()` và `BlogApiService.uploadImage()`.
- **Cài lại package**: `@angular/fire` đang dùng bản RC cho Angular 21 nên khi chạy `npm install`
  hãy thêm `--legacy-peer-deps`.
- `addUser()` (User management) chỉ tạo document trong `users`, **không** tạo tài khoản đăng nhập.
  Muốn tài khoản đăng nhập được, tạo qua Authentication (hoặc dùng `register()`).
