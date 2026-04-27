# 🍳 Hệ Thống Quản Lý Tủ Lạnh & Chi Tiêu Thông Minh - PantrySmart

<div align="center">

![Android](https://img.shields.io/badge/Platform-Android-3DDC84?style=for-the-badge&logo=android&logoColor=white)
![Java](https://img.shields.io/badge/Language-Java-007396?style=for-the-badge&logo=java&logoColor=white)
![Room Database](https://img.shields.io/badge/Database-Room-blue?style=for-the-badge)
![Gemini API](https://img.shields.io/badge/AI-Gemini_API-orange?style=for-the-badge&logo=google)

*Giải pháp toàn diện giúp quản lý thực phẩm và tự động hóa nấu nướng với AI*

[Tính năng](#-tính-năng-chính) • [Cài đặt](#-hướng-dẫn-cài-đặt) • [Sử dụng](#-hướng-dẫn-sử-dụng)

</div>

---

## 📖 Giới Thiệu

Dự án **PantrySmart** được phát triển bằng ngôn ngữ **Java** trên nền tảng **Android**, tích hợp trí tuệ nhân tạo (Google Gemini) để cung cấp giải pháp thông minh cho việc quản lý thực phẩm trong tủ lạnh, theo dõi hạn sử dụng, và tự động đề xuất công thức nấu ăn.

### 🎯 Mục Tiêu Dự Án

- ✅ Xây dựng hệ thống quản lý thực phẩm cá nhân/gia đình hoàn chỉnh
- ✅ Ứng dụng trí tuệ nhân tạo (AI) vào việc nhận diện hình ảnh và xử lý dữ liệu (OCR)
- ✅ Quản lý dữ liệu mượt mà, lưu trữ an toàn với Room Database
- ✅ Tạo giao diện Android hiện đại, thân thiện, dễ sử dụng cho người dùng

---

## ✨ Tính Năng Chính

<table>
<tr>
<td width="50%">

### 🧊 Quản Lý Tủ Lạnh (Smart Fridge)
- ➕ Thêm, sửa, xóa thông tin thực phẩm
- 📅 Theo dõi số lượng và ngày hết hạn
- 🤖 **Nhận diện AI**: Chụp ảnh để tự động phân loại thực phẩm

### 🍲 Trợ Lý Nấu Ăn (AI Recipe)
- 💡 Gợi ý món ăn dựa trên nguyên liệu có sẵn trong tủ
- 📝 Hiển thị chi tiết công thức và các bước thực hiện
- 🔄 Tự động trừ số lượng nguyên liệu sau khi nấu

### 💰 Quản Lý Ngân Sách
- 📋 Lập ngân sách đi chợ theo tuần/tháng
- 🧾 **Quét hóa đơn**: Dùng AI (OCR) bóc tách thông tin từ biên lai siêu thị
- 📊 Theo dõi tình hình chi tiêu

</td>
<td width="50%">

### 🔔 Cảnh Báo & Thông Báo
- ⏰ Tự động gửi cảnh báo (Push Notification) khi thực phẩm sắp hết hạn
- ⚙️ Chạy ngầm mượt mà bằng WorkManager
- 📱 Xem lại lịch sử thông báo

### 📊 Thống Kê & Lịch Sử
- 🍳 **Lịch sử nấu nướng**: Lưu lại các món đã nấu
- 💸 **Chi phí**: Phân tích chi phí mua sắm thực phẩm
- 📉 Biểu đồ trực quan cho ngân sách

### 💾 Quản Lý Dữ Liệu
- 🔄 Lưu trữ Local an toàn, tốc độ cao
- 🗄️ Kiến trúc cơ sở dữ liệu tối ưu (Entities, Relations)

</td>
</tr>
</table>

---

## 🛠️ Công Nghệ Sử Dụng

| Công nghệ | Phiên bản | Mô tả |
|-----------|-----------|-------|
| ![Java](https://img.shields.io/badge/Java-007396?style=flat&logo=java&logoColor=white) | 11+ | Ngôn ngữ lập trình chính |
| ![Android](https://img.shields.io/badge/Android-3DDC84?style=flat&logo=android&logoColor=white) | Min SDK 24 | Nền tảng phát triển |
| ![Room](https://img.shields.io/badge/Room_Database-blue?style=flat) | Mới nhất | Hệ quản trị cơ sở dữ liệu Local |
| ![Gemini API](https://img.shields.io/badge/Gemini_API-orange?style=flat&logo=google) | Pro/Vision | Xử lý AI, nhận diện ảnh & gợi ý |
| ![Android Studio](https://img.shields.io/badge/Android_Studio-5C2D91?style=flat&logo=android-studio&logoColor=white) | Iguana+ | IDE phát triển |

---

## 📂 Cấu Trúc Dự Án

<pre>
PantrySmart/
│
├── 📂 src/main/java/hcmute/edu/vn/pantrysmart/
│   ├── 📄 MainActivity.java                   # Entry point
│   │
│   ├── 📂 fragment/                           # Các module màn hình chính
│   │   ├── 🧊 FridgeFragment.java             # Quản lý tủ lạnh
│   │   ├── 🍲 SuggestFragment.java            # Gợi ý món ăn
│   │   ├── 💰 BudgetFragment.java             # Quản lý chi tiêu
│   │   └── 📂 helper/                         # Chứa Dialog, BottomSheet
│   │
│   ├── 📂 data/local/                         # Xử lý dữ liệu Room
│   │   ├── 📂 entity/                         # Table Models
│   │   └── 📂 dao/                            # Data Access Objects
│   │
│   ├── 📂 config/                             # Cấu hình AI & API
│   │   ├── 🤖 GeminiFoodRecognitionService.java
│   │   └── 🤖 GeminiReceiptParser.java
│   │
│   ├── 📂 adapter/                            # Kết nối dữ liệu với UI
│   ├── 📂 notification/                       # Quản lý Push Notification
│   └── 📂 worker/                             # Chạy ngầm (ExpiryCheckWorker)
│
├── ⚙️ local.properties                        # Cấu hình API Keys
└── 📖 README.md
</pre>

---

## 🚀 Hướng Dẫn Cài Đặt

### 📋 Yêu Cầu Hệ Thống

- 💻 **OS**: Windows / macOS
- 🔧 **Java**: JDK 11 trở lên
- 📱 **Thiết bị**: Máy ảo (Emulator) hoặc điện thoại Android thật (từ Android 7.0)
- 🔑 **API Key**: Cần có khóa API của Google Gemini và Pexels.

### 📥 Các Bước Cài Đặt

#### 1️⃣ Clone Repository
Mở Terminal / Command Prompt và chạy lệnh:
<pre>
git clone https://github.com/HauPhan-devJava2103/PantrySmart.git
cd PantrySmart
</pre>

#### 2️⃣ Cấu Hình API Keys
- Mở solution bằng Android Studio.
- Tìm file `local.properties` (nằm ở thư mục gốc của dự án).
- Thêm 2 dòng sau vào file:
<pre>
GEMINI_API_KEY="thay_api_key_gemini_cua_ban_vao_day"
PEXELS_API_KEY="thay_api_key_pexels_cua_ban_vao_day"
</pre>

#### 3️⃣ Build và Chạy
- Build Project: Nhấn `Ctrl + F9`
- Chạy App: Nhấn `Shift + F10`

---

## 📱 Hướng Dẫn Sử Dụng

### 🧊 Quản Lý Tủ Lạnh
1. Khởi động ứng dụng, chọn tab **Tủ lạnh**.
2. Nhấn nút dấu `+` để thêm thực phẩm thủ công hoặc chọn tính năng **Quét AI**.
3. Chụp ảnh nguyên liệu, AI sẽ tự điền tên và phân loại giúp bạn.

### 🍲 Nấu Ăn Cùng AI
1. Vào tab **Gợi ý**.
2. Hệ thống tự động phân tích những nguyên liệu bạn đang có.
3. Chọn một công thức để xem cách làm chi tiết.
4. Nhấn **Nấu món này** để hệ thống trừ bớt nguyên liệu trong kho.

### 🧾 Quản Lý Chi Tiêu & Quét Hóa Đơn
1. Chuyển sang tab **Ngân sách**.
2. Chọn **Quét hóa đơn** và chụp lại biên lai siêu thị.
3. AI sẽ tự động đọc giá tiền và các mặt hàng bạn đã mua để tính vào chi phí.
4. Tính năng cảnh báo khi vượt quá ngân sách theo tuần, tháng.

---

## 👨‍💻 Thành Viên Phát Triển

<table>
  <tr>
    <td align="center">
      <a href="https://github.com/Thaibinh2005">
        <img src="https://github.com/Thaibinh2005.png" width="100px;" alt=""/>
        <br />
        <sub><b>Thaibinh2005</b></sub>
      </a>
    </td>
    <td align="center">
      <a href="https://github.com/nguyentranhuynhchi">
        <img src="https://github.com/nguyentranhuynhchi.png" width="100px;" alt=""/>
        <br />
        <sub><b>nguyentranhuynhchi</b></sub>
      </a>
    </td>
    <td align="center">
      <a href="https://github.com/HauPhan-devJava2103">
        <img src="https://github.com/HauPhan-devJava2103.png" width="100px;" alt=""/>
        <br />
        <sub><b>HauPhan-devJava2103</b></sub>
      </a>
    </td>
  </tr>
</table>

*(Đồ án môn học - Đại học Sư phạm Kỹ thuật TP.HCM HCMUTE)*

---

## 📝 Ghi Chú

> ⚠️ **Lưu ý quan trọng:**
> - Đây là dự án học tập, được phát triển cho mục đích giáo dục tại HCMUTE.
> - Các tính năng AI phụ thuộc vào giới hạn quota (lượt gọi API) miễn phí của Google Gemini.
> - Vui lòng không push file `local.properties` chứa API Key thật của bạn lên GitHub.

---

## 📄 License

Dự án này được phát triển cho mục đích học tập tại trường Đại học. 

---

## 📌 Phiên Bản

### **Version 1.0.0** - Phát hành bản Final
- ✅ Hoàn thiện giao diện tủ lạnh và ngân sách
- ✅ Tích hợp thành công Gemini Vision và Text
- ✅ Cảnh báo notification chạy ngầm ổn định

---

## 🐛 Báo Lỗi

Nếu phát hiện lỗi, vui lòng tạo issue tại repository của dự án:
👉 [https://github.com/HauPhan-devJava2103/PantrySmart/issues](https://github.com/HauPhan-devJava2103/PantrySmart/issues)

**Khi báo lỗi, vui lòng cung cấp:**
- Mô tả chi tiết lỗi (app bị crash ở màn hình nào)
- Các bước để tái hiện lỗi
- Tên thiết bị Android hoặc phiên bản SDK máy ảo

---

<div align="center">

**Made with ❤️ by PantrySmart Team**

</div>
