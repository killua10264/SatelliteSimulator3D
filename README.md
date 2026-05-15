# Satellite Simulator 3D

Mô phỏng vệ tinh quỹ đạo 3D theo thời gian thực — JavaFX 21 + SQL Server 2022.

---

## Yêu cầu hệ thống

- Java 17 (Eclipse Temurin khuyến nghị)
- Apache Maven 3.9.x
- Microsoft SQL Server 2022 (Express trở lên)

---

## Cài đặt & Chạy
### Bước 1 — Cài Java 17 (Eclipse Temurin)

Tải tại: https://adoptium.net/temurin/releases/?version=17

Sau khi cài, mở PowerShell và kiểm tra:
```powershell
java -version
# Kết quả mong đợi: openjdk version "17.x.x" ... Temurin
```

Nếu cần đặt thủ công (thêm vào PowerShell profile):
```powershell
$env:JAVA_HOME = "C:\Program Files\Eclipse Adoptium\jdk-17.x.x-hotspot"
$env:PATH      = "$env:JAVA_HOME\bin;$env:PATH"
```

### Bước 2 — Cài Apache Maven

Tải tại: https://maven.apache.org/download.cgi (chọn `apache-maven-3.9.x-bin.zip`)

Giải nén vào `C:\maven`, rồi thêm vào PATH:
```powershell
$env:PATH = "C:\maven\bin;$env:PATH"
mvn -v
# Kết quả mong đợi: Apache Maven 3.9.x
```

### Bước 3 — Cài SQL Server 2022 Express

Tải tại: https://www.microsoft.com/en-us/sql-server/sql-server-downloads (chọn Express)

Trong lúc cài, chọn **Mixed Mode Authentication** và đặt mật khẩu SA.

### Bước 4 — Bật TCP/IP cho SQL Server

1. Mở **SQL Server Configuration Manager**.
2. Vào `SQL Server Network Configuration` → `Protocols for MSSQLSERVER`.
3. Nhấp đúp `TCP/IP` → chuyển sang **Enabled**.
4. Khởi động lại service: mở **Services** → tìm `SQL Server (MSSQLSERVER)` → Restart.

### Bước 5 — Bật Mixed Mode Authentication (nếu chưa)

Mở PowerShell **Administrator** và chạy:
```powershell
Set-ItemProperty -Path "HKLM:\SOFTWARE\Microsoft\Microsoft SQL Server\MSSQL16.MSSQLSERVER\MSSQLServer" -Name "LoginMode" -Value 2
Restart-Service MSSQLSERVER
```

### Bước 6. Chuẩn bị Database

Mở **SSMS**, kết nối `localhost`, chạy lần lượt 2 file sau:

| Thứ tự | File | Mục đích |
|---|---|---|
| 1 | `sql/create_tables.sql` | Tạo database, bảng, user (`satellite_user / Satellite@2024`) |
| 2 | `sql/seed_satellites.sql` | Chèn dữ liệu vệ tinh mẫu (idempotent — an toàn chạy nhiều lần) |

> File `sql/seed_routing_test.sql` là tùy chọn — chứa dữ liệu kiểm thử định tuyến dày đặc hơn.

Hoặc dùng `sqlcmd`:
```powershell
sqlcmd -S localhost -U satellite_user -P "Satellite@2024" -d satellite_db -i sql/create_tables.sql
sqlcmd -S localhost -U satellite_user -P "Satellite@2024" -d satellite_db -i sql/seed_satellites.sql
```

### Bước 7. Tải texture hành tinh

Tải 2 file vào `SatelliteSimulator3D/src/main/resources/textures/`:

- `earth.jpg` — https://www.solarsystemscope.com/textures/ (2k_earth_daymap.jpg)
- `mars.jpg` — https://www.solarsystemscope.com/textures/ (2k_mars.jpg)

> Không có texture vẫn chạy được — hành tinh hiển thị màu đặc.
> Bước này hiện tại sẽ không cần thiết vì hai file texture đã có trong thư mục resources/textures, nếu không thấy thì tải về và thêm vào thư mục.

### Bước 8. Chạy chương trình

**Maven CLI:**
```powershell
cd SatelliteSimulator3D
mvn javafx:run
```

**Eclipse IDE:**
1. **File** → **Import** → **Maven** → **Existing Maven Projects** → trỏ vào thư mục `SatelliteSimulator3D/`.
2. Chuột phải project → **Run As** → **Maven Build** → Goals: `javafx:run` → **Run**.

### Bước 9. Kiểm chứng benchmark (tùy chọn)

Chạy lại benchmark để đối chiếu với kết quả trong báo cáo:

```powershell
cd SatelliteSimulator3D
mvn compile exec:java -Dexec.mainClass="com.satellite.util.BenchmarkRunner"
```

Output in ra console và ghi vào `benchmark_report.txt`. Kết quả phải khớp với file `benchmark_report.txt` đã có trong repo vì thuật toán dùng **seed ngẫu nhiên cố định (`seed=42`)** — đảm bảo tái lập được trên mọi máy.

---

## Hướng dẫn sử dụng

### Giao diện tổng quan

```
┌─────────────────────────────────────┬──────────────────────┐
│                                     │  [Hành tinh]         │
│                                     │  ComboBox ▼          │
│                                     ├──────────────────────┤
│                                     │  [Animation]         │
│         Không gian 3D               │  ⏸ Pause             │
│      (kéo chuột để xoay,            │  TIME_SCALE: 1000×   │
│       lăn chuột để zoom)            │  ━━━━━━━━━━━━━━━━━━  │
│                                     ├──────────────────────┤
│                                     │  Vệ tinh (3)  🔄     │
│                                     │  📡 SAT-1  (400 km)  │
│                                     │  📡 SAT-2  (550 km)  │
│                                     │  🛰 SAT-3  (350 km)  │
│                                     │  [🗑 Xoá vệ tinh]    │
│                                     ├──────────────────────┤
│                                     │  [Thêm vệ tinh mới]  │
│                                     │  Tên / Lat / Lon / Alt│
│                                     │  □ Vệ tinh liên lạc  │
│                                     │  [➕ Thêm vào DB]    │
│                                     ├──────────────────────┤
│                                     │  [Sinh tự động]      │
│                                     │  Số vệ tinh N: ___   │
│                                     │  □ Là relay          │
│                                     │  [⚡ Sinh tự động]   │
│                                     ├──────────────────────┤
│                                     │  [Định tuyến]        │
│                                     │  Từ:  [SAT-1 ▼]      │
│                                     │  Đến: [SAT-3 ▼]      │
│                                     │  [🔍 Tìm đường]      │
│                                     │  [❌ Xoá đường]      │
├─────────────────────────────────────┴──────────────────────┤
│  Hành tinh: Earth  |  Vệ tinh: 3  |  Kéo chuột để xoay... │
└────────────────────────────────────────────────────────────┘
```

> Sidebar có thanh cuộn dọc — kéo xuống để thấy phần Sinh tự động và Định tuyến.

---

### Điều hướng camera

| Thao tác | Kết quả |
|---|---|
| Kéo chuột trái | Xoay hành tinh |
| Lăn chuột lên | Zoom in |
| Lăn chuột xuống | Zoom out |

---

### Chọn hành tinh

Nhấn ComboBox góc trên phải, chọn **Earth** hoặc **Mars**. Giao diện 3D tự cập nhật hành tinh và danh sách vệ tinh từ database.

---

### Điều khiển animation

- **⏸ Pause / ▶ Play** — Dừng / tiếp tục chuyển động vệ tinh.
- **TIME_SCALE slider** — Tốc độ mô phỏng:
  - `1×` = thời gian thực
  - `1000×` = 1 giây thực ≈ 16 phút quỹ đạo *(mặc định)*
  - `50000×` = 1 giây thực ≈ 13 giờ quỹ đạo

---

### Thêm vệ tinh thủ công

Điền form phần **Thêm vệ tinh mới**:

| Trường | Ý nghĩa | Ví dụ |
|---|---|---|
| Tên | Tên định danh | `ISS-2` |
| Lat | Vĩ độ (−90 ÷ 90) | `51.6` |
| Lon | Kinh độ (−180 ÷ 180) | `0.0` |
| Alt km | Độ cao so với bề mặt | `408` |
| Relay | Tích nếu là vệ tinh liên lạc | ✓ |

Nhấn **➕ Thêm vào DB**. Vệ tinh xuất hiện ngay trong cảnh 3D và lưu vào database.

> Vệ tinh relay màu **vàng** 📡 — vệ tinh thường màu **xanh cyan** 🛰.

---

### Xóa vệ tinh

Nhấn tên vệ tinh trong danh sách để chọn → nhấn **🗑 Xoá vệ tinh đã chọn**.

Nút **🔄** làm mới danh sách từ database (dùng khi thêm/xóa trực tiếp qua SSMS).

---

### Sinh vệ tinh tự động

1. Nhập số N vào ô **Số vệ tinh N** (khuyến nghị N ≥ 15 để định tuyến hoạt động tốt).
2. Tích **Là relay** nếu muốn vệ tinh sinh ra làm được trạm tiếp sóng.
3. Nhấn **⚡ Sinh tự động**.

Thuật toán phân bố N vệ tinh đều trên mặt cầu (Fibonacci sphere) — vĩ độ ±55°, độ cao ngẫu nhiên 800–2000 km. Vệ tinh đặt tên `AUTO-1`, `AUTO-2`, ... Mỗi lần sinh xóa toàn bộ vệ tinh AUTO cũ trước khi tạo mới.

---

### Định tuyến giữa 2 vệ tinh

1. Chọn vệ tinh nguồn (**Từ:**) và đích (**Đến:**) trong phần Định tuyến.
2. Nhấn **🔍 Tìm đường**. Kết quả hiện lộ trình và số hop:
   ```
   SAT-1 → RELAY-A → RELAY-C → SAT-3  (15.2 units, 3 hop)
   ```
3. Các tia laser **xanh lục** xuất hiện trong cảnh 3D, bám theo vệ tinh khi di chuyển.
4. Nhấn **❌ Xóa đường** để tắt laser.

**Relay vs thường:**
- **Relay** — có thể làm trạm trung gian, tín hiệu đi qua để đến đích.
- **Thường** — chỉ là điểm đầu hoặc điểm cuối, không thể làm trung gian.

**Tự động định tuyến lại:**
- Khi vệ tinh di chuyển và một đoạn bị hành tinh che khuất, laser chuyển **đỏ**.
- Hệ thống tự chạy lại Dijkstra ngay lập tức.
  - Tìm được đường mới → `↺ Tự động định tuyến lại: [lộ trình mới]`
  - Không tìm được → thông báo chẩn đoán, ví dụ: `Không có relay nào nhìn thấy SAT-1. Thêm relay gần vệ tinh này.`
