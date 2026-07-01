# TiredCity — App Admin (Android)

App di động cho **admin/staff** quản lý hệ thống khi không ngồi máy tính (xem doanh thu, duyệt đơn…).
Dùng **chung backend NestJS** với web quản lý và app customer.

- Package: `com.tiredcity.admin`
- Stack: Java + ViewBinding + Retrofit + Google Sign-In
- Cấu trúc package mirror app customer: `ui/ · data/ · adapter/ · utils/`

```
app-admin/
├── settings.gradle.kts        # rootProject "TiredCityAdmin", include :app
├── build.gradle.kts
└── app/
    ├── build.gradle.kts       # namespace com.tiredcity.admin, API_BASE_URL
    └── src/main/
        ├── AndroidManifest.xml
        ├── java/com/tiredcity/admin/
        │   ├── TiredCityAdminApp.java
        │   ├── ui/auth/LoginActivity.java        # Google Sign-In
        │   ├── ui/dashboard/DashboardActivity.java
        │   ├── data/remote/ApiClient.java        # Retrofit -> backend
        │   ├── data/remote/AdminApi.java
        │   ├── data/model/DashboardOverview.java
        │   ├── adapter/                          # RecyclerView adapters
        │   └── utils/Constants.java
        └── res/{layout,values}/
```

## Chạy
1. Mở thư mục `app-admin/` trong **Android Studio** (đây là một Gradle project độc lập).
2. Android Studio sẽ tự sinh Gradle wrapper. Nếu thiếu, copy thư mục `gradle/` + `gradlew*` từ app customer (ở thư mục gốc repo).
3. Backend phải chạy ở `http://localhost:4000`. Trên emulator dùng `http://10.0.2.2:4000` (đã cấu hình trong `build.gradle.kts` → `API_BASE_URL`). Máy thật: đổi sang IP LAN.

## Trạng thái
Skeleton — màn Login + Dashboard gọi `GET /api/dashboard/overview`. Google Sign-In + lưu JWT sẽ hoàn thiện ở giai đoạn sau (xem `docs/`).
