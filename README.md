# iPanda 🐼

iPanda là một ứng dụng Movie Crawler và Streaming đa nền tảng (Android & Desktop) được xây dựng dựa trên Kotlin Multiplatform (KMP).

## 🚀 Tính năng chính

- **Duyệt phim:** Hiển thị danh sách phim mới nhất, phim bộ, phim lẻ.
- **Tìm kiếm:** Tìm kiếm phim yêu thích.
- **Yêu thích:** Lưu trữ phim vào danh sách yêu thích cá nhân (sử dụng Room Database).
- **Xem phim:** Hỗ trợ phát video trực tiếp với khả năng "sniff" link stream tự động.
- **Đa nền tảng:** Chạy mượt mà trên cả Android và Desktop (Windows/macOS/Linux).

## 🏗 Kiến trúc dự án

Dự án được thiết kế theo hướng module hóa chặt chẽ:

- **`:shared` (KMP):** Chứa business logic thuần túy, các Repository interface và data classes. Sử dụng SQLDelight/Room để lưu trữ local.
- **`:movie-crawler` (JVM/Java 21):** Module thực hiện việc cào dữ liệu (Scraping) bằng Jsoup và bắt link stream (Sniffing) qua Browserless/Ktor. Tận dụng Java 21 Virtual Threads để xử lý song song hiệu suất cao.
- **`:desktopApp` (Compose Multiplatform):** Giao diện người dùng cho phiên bản Desktop.
- **`:androidApp` (Compose):** Giao diện người dùng cho phiên bản Android.

## 🛠 Tech Stack

- **Ngôn ngữ:** Kotlin, Java 21.
- **UI Framework:** Compose Multiplatform.
- **Network:** Ktor Client.
- **Database:** Room (KMP).
- **Parsing/Scraping:** Jsoup.
- **Concurrency:** Kotlin Coroutines & Java 21 Virtual Threads.
- **Dependency Injection:** Koin (hoặc Manual DI tùy vào cấu hình hiện tại).
- **Serialization:** Kotlinx Serialization.

## ⚙️ Cài đặt & Chạy ứng dụng

### Yêu cầu cấu hình

- Java 21+.
- Android Studio hoặc IntelliJ IDEA.

### Chạy ứng dụng Desktop

```bash
./gradlew :desktopApp:run
```

### Chạy ứng dụng Android

Mở dự án trong Android Studio và chạy cấu hình `androidApp`.

## 🎨 Tiêu chuẩn thiết kế

Ứng dụng hướng tới trải nghiệm người dùng cao cấp (Premium UI) với:

- Dark Mode chủ đạo.
- Hiệu ứng Shimmer khi tải dữ liệu.
- Transition mượt mà và animation hiện đại (Hero banners, card hover).

---
*Dự án đang trong quá trình phát triển.*
