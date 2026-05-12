# Báo cáo Dự án: Satellite Simulator 3D

## 1. Thông tin chung

| Mục | Nội dung |
|---|---|
| Tên dự án | Satellite Simulator 3D |
| Ngôn ngữ | Java 17 |
| Giao diện | JavaFX 21 (3D) |
| Cơ sở dữ liệu | Microsoft SQL Server 2022 |
| Build tool | Apache Maven 3.9.x |
| Deadline | 15/05/2026 |

### Phân công công việc

| Thành viên | Vai trò | Nội dung |
|---|---|---|
| Thành viên A | DB & Model | Schema SQL, Planet/Satellite model, DAO layer, CoordConverter |
| Thành viên B | Physics & UI | PhysicsEngine, OrbitalSatellite, JavaFX 3D UI, AnimationTimer |
| Thành viên C | Routing | Line-of-Sight, Dijkstra, visualize laser beam *(chưa hoàn thành)* |

---

## 2. Cấu trúc dự án

```
SatelliteSimulator3D/
├── pom.xml                          # Maven dependencies
├── sql/
│   └── create_tables.sql            # Script tạo DB + seed data
└── src/main/
    ├── java/com/satellite/
    │   ├── Main.java                 # JavaFX Application — UI chính
    │   ├── ConsoleDemo.java          # Demo console (kiểm chứng physics)
    │   ├── model/
    │   │   ├── Planet.java           # Thực thể hành tinh
    │   │   └── Satellite.java        # Thực thể vệ tinh
    │   ├── dao/
    │   │   ├── DatabaseConnection.java  # Kết nối SQL Server
    │   │   ├── PlanetDAO.java           # CRUD hành tinh
    │   │   └── SatelliteDAO.java        # CRUD vệ tinh
    │   ├── physics/
    │   │   ├── PhysicsEngine.java    # Tính vận tốc, chu kỳ, tần số góc
    │   │   └── OrbitalSatellite.java # Cập nhật vị trí 3D theo thời gian
    │   └── util/
    │       └── CoordConverter.java   # Chuyển (lat, lon, alt) → (x, y, z)
    └── resources/
        └── textures/
            ├── earth.jpg             # Texture Trái Đất (solarsystemscope.com)
            └── mars.jpg              # Texture Sao Hỏa (solarsystemscope.com)
```

---

## 3. Những gì đã thực hiện

### 3.1 Nền tảng dữ liệu (Thành viên A)

- **Thiết kế schema SQL**: bảng `planets` (id, name, radius_km, mass_kg, texture_file) và `satellites` (id, planet_id, name, latitude, longitude, altitude_km, is_relay) với khóa ngoại.
- **Seed data**: 2 hành tinh (Earth, Mars) và 3 vệ tinh mẫu tự động insert khi chạy script.
- **Model classes**: `Planet.java`, `Satellite.java` với đầy đủ getter/setter. `Satellite` lưu thêm tọa độ 3D runtime (x, y, z).
- **DAO layer**: `PlanetDAO.findAll()`, `findById()`; `SatelliteDAO.findByPlanet()`, `insert()`, `delete()` sử dụng PreparedStatement để chống SQL Injection.
- **CoordConverter**: Chuyển đổi (lat°, lon°, altKm) → (x, y, z) theo công thức hình cầu:
  - `r = (R_planet + alt) × scale`
  - `x = r·cos(lat)·cos(lon)`, `y = r·sin(lat)`, `z = r·cos(lat)·sin(lon)`

### 3.2 Physics Engine (Thành viên B)

**`PhysicsEngine.java`** — Tất cả công thức cơ học quỹ đạo:

| Công thức | Biểu thức | Ý nghĩa |
|---|---|---|
| Vận tốc quỹ đạo | `v = √(GM/r)` | Vận tốc tối thiểu để không rơi |
| Tần số góc | `ω = v/r` | rad/giây, dùng để animate |
| Chu kỳ quỹ đạo | `T = 2π/ω` | Thời gian một vòng |

Kiểm chứng với giá trị thực tế NASA:

| Vệ tinh | Độ cao | v tính được | v thực tế |
|---|---|---|---|
| ISS | 408 km | 7.67 km/s | ~7.66 km/s |
| GPS | 20180 km | 3.87 km/s | ~3.87 km/s |
| Địa tĩnh | 35786 km | 3.07 km/s | ~3.07 km/s |
| Hubble | 540 km | 7.59 km/s | ~7.59 km/s |

**`OrbitalSatellite.java`** — Cập nhật vị trí theo thời gian:
- Khởi tạo `θ₀` từ kinh độ ban đầu của vệ tinh.
- Mỗi frame: `θ += ω × dt × TIME_SCALE`
- Vị trí 3D: vệ tinh chuyển động trên mặt phẳng nghiêng theo vĩ độ ban đầu.

### 3.3 JavaFX 3D UI (Thành viên B)

**Giao diện gồm 3 vùng:**

**Trái (SubScene 900×720)** — Không gian 3D:
- Nền màu `#02030a` (màu vũ trụ).
- `PerspectiveCamera` đặt tại z = -900 (hoặc xa hơn với hành tinh lớn).
- Ánh sáng: `AmbientLight` (70,70,80) + `PointLight` trắng mô phỏng mặt trời.
- Hành tinh là `Sphere` với `PhongMaterial` có diffuse map từ texture file.
- Vệ tinh là `Sphere` nhỏ: vệ tinh liên lạc (relay) màu vàng, vệ tinh thường màu cyan.
- Tương tác: kéo chuột để xoay, lăn chuột để zoom.

**Phải (Control Panel 360px)** — Bảng điều khiển:
- ComboBox chọn hành tinh (Earth / Mars).
- Nút Play/Pause animation.
- Slider TIME_SCALE (1× → 50000×) — điều chỉnh tốc độ mô phỏng.
- ListView danh sách vệ tinh với icon emoji (📡 relay, 🛰 thường).
- Nút xóa vệ tinh đã chọn.
- Form thêm vệ tinh: Tên, Lat, Lon, Alt (km), checkbox Is Relay.

**Dưới (Status Bar)** — Hiển thị tên hành tinh đang chọn và số vệ tinh.

### 3.4 Định tuyến & Benchmark (Thành viên C)

- **Thuật toán Line-of-Sight (LOS)**: Tính toán hình học không gian 3D để xác định đường truyền giữa 2 vệ tinh có bị hành tinh (Trái Đất/Sao Hỏa) che khuất hay không (`LineOfSight.java`).
- **Định tuyến Dijkstra (`RoutingEngine.java`)**: Tìm đường đi ngắn nhất truyền tín hiệu qua các trạm vệ tinh tiếp sóng (có cờ `is_relay = true`). Xây dựng đồ thị động dựa trên khoảng cách và vị trí hiện thời của vệ tinh.
- **Mô phỏng đường truyền bằng Laser 3D**: Sử dụng `Cylinder` và `Rotate` trong JavaFX 3D để vẽ các tia laser màu xanh lục (Lime). Hình ảnh được cập nhật liên tục theo thời gian thực tương ứng với quá trình di chuyển quỹ đạo của vệ tinh.
- **Kiểm thử trên Cơ Sở Dữ Liệu (`seed_routing_test.sql`)**: Hệ thống đã được kiểm thử toàn diện bằng kịch bản mạng lưới vệ tinh phân bố dày đặc lấy trực tiếp từ SQL Server. Các vệ tinh liên lạc (màu vàng) và vệ tinh thường (màu cyan) kết nối ổn định; thuật toán hoạt động chính xác khi query dữ liệu thực tế từ DB.
- **Báo cáo Benchmark (`BenchmarkRunner.java`)**: Đã chạy thử nghiệm thuật toán nghiệm thu trên mô hình N = 10, 20, 50 vệ tinh ngẫu nhiên.
  - Kết quả: Thời gian định tuyến hoàn thành cực kỳ nhanh (< 10ms) cho 50 node.
  - Độ phức tạp thời gian: $O(N^2)$ (xây dựng đồ thị) + $O(N \log N)$ (chạy Dijkstra).
---

## 4. Những thay đổi so với kế hoạch ban đầu

| Hạng mục | Kế hoạch ban đầu | Thực tế |
|---|---|---|
| IDE | Eclipse IDE | Maven CLI (`mvn javafx:run`) |
| DB Authentication | Windows Integrated Auth | SQL Server Auth (user: `satellite_user`) |
| Login Mode | Windows-only (mặc định) | Mixed Mode (cần bật thủ công trong Registry) |
| Texture nguồn | Link trực tiếp trong code | Tải về và nhúng vào `resources/textures/` |
| Texture mars.jpg | solarsystemscope.com | GitHub mirror (solarsystemscope blocked) |
| pom.xml encoding | Không khai báo | Thêm `<project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>` |


---

## 5. Hướng dẫn cài đặt chi tiết

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

### Bước 6 — Tạo Database và User

Mở **SSMS**, kết nối tới `localhost`, mở New Query và chạy:
```sql
-- Tạo database
CREATE DATABASE satellite_db;
GO
USE satellite_db;
GO

-- Tạo bảng
CREATE TABLE planets (
    id           INT PRIMARY KEY IDENTITY(1,1),
    name         NVARCHAR(100) NOT NULL,
    radius_km    FLOAT NOT NULL,
    mass_kg      FLOAT NOT NULL,
    texture_file NVARCHAR(255)
);

CREATE TABLE satellites (
    id           INT PRIMARY KEY IDENTITY(1,1),
    planet_id    INT NOT NULL,
    name         NVARCHAR(100) NOT NULL,
    latitude     FLOAT NOT NULL,
    longitude    FLOAT NOT NULL,
    altitude_km  FLOAT NOT NULL,
    is_relay     BIT DEFAULT 0,
    FOREIGN KEY (planet_id) REFERENCES planets(id)
);
GO

-- Dữ liệu mẫu
INSERT INTO planets (name, radius_km, mass_kg, texture_file) VALUES
  ('Earth', 6371.0, 5.972e24, 'earth.jpg'),
  ('Mars',  3390.0, 6.390e23, 'mars.jpg');

INSERT INTO satellites (planet_id, name, latitude, longitude, altitude_km, is_relay) VALUES
  (1, 'SAT-1',  45.0,  90.0, 400.0, 1),
  (1, 'SAT-2', -30.0, 180.0, 550.0, 1),
  (1, 'SAT-3',  10.0,  45.0, 350.0, 0);
GO

-- Tạo login SQL Server
CREATE LOGIN satellite_user WITH PASSWORD = 'Satellite@2024';
USE satellite_db;
CREATE USER satellite_user FOR LOGIN satellite_user;
ALTER ROLE db_owner ADD MEMBER satellite_user;
GO
```

### Bước 7 — Tải texture hành tinh

Trước khi build, tải 2 file texture vào thư mục `SatelliteSimulator3D/src/main/resources/textures/`:

- **earth.jpg** — Tải tại: https://www.solarsystemscope.com/textures/ (2k_earth_daymap.jpg)
- **mars.jpg** — Tải tại: https://www.solarsystemscope.com/textures/ (2k_mars.jpg)

Đổi tên thành `earth.jpg` và `mars.jpg` rồi đặt vào thư mục trên.

> Nếu không có texture, chương trình vẫn chạy được nhưng hành tinh sẽ hiển thị màu đặc (Earth màu xanh, Mars màu đỏ cam).

### Bước 8 — Build và chạy chương trình

```powershell
cd SatelliteSimulator3D
mvn javafx:run
```

---

## 6. Hướng dẫn sử dụng

### 6.1 Giao diện tổng quan

```
┌─────────────────────────────────────┬──────────────────────┐
│                                     │  [Hành tinh]         │
│                                     │  ComboBox ▼          │
│                                     ├──────────────────────┤
│         Không gian 3D               │  [Animation]         │
│      (kéo chuột để xoay,            │  ⏸ Pause             │
│       lăn chuột để zoom)            │  TIME_SCALE: 1000×   │
│                                     │  ━━━━━━━━━━━━━━━━━━  │
│                                     ├──────────────────────┤
│                                     │  [Danh sách vệ tinh] │
│                                     │  📡 SAT-1  (400 km)  │
│                                     │  📡 SAT-2  (550 km)  │
│                                     │  🛰 SAT-3  (350 km)  │
│                                     │  [🗑 Xoá vệ tinh]    │
│                                     ├──────────────────────┤
│                                     │  [Thêm vệ tinh mới]  │
│                                     │  Tên: ___________    │
│                                     │  Lat: ___________    │
│                                     │  Lon: ___________    │
│                                     │  Alt: ___________    │
│                                     │  □ Vệ tinh liên lạc  │
│                                     │  [➕ Thêm vào DB]    │
<<<<<<< HEAD
=======
│                                     ├──────────────────────┤
│                                     │  [Định tuyến]        │
│                                     │  Từ:  [SAT-1 ▼]      │
│                                     │  Đến: [SAT-3 ▼]      │
│                                     │  [🔍 Tìm đường]      │
│                                     │  [❌ Xoá đường]      │
>>>>>>> 7107ddad8d4205652deb5824bd076c6f36659cf9
├─────────────────────────────────────┴──────────────────────┤
│  Hành tinh: Earth  |  Vệ tinh: 3  |  Kéo chuột để xoay... │
└────────────────────────────────────────────────────────────┘
```

### 6.2 Điều hướng camera

| Thao tác | Kết quả |
|---|---|
| Kéo chuột trái | Xoay hành tinh |
| Lăn chuột lên | Zoom in (lại gần) |
| Lăn chuột xuống | Zoom out (ra xa) |

### 6.3 Chọn hành tinh

Nhấn vào ComboBox ở góc trên phải, chọn **Earth** hoặc **Mars**. Giao diện 3D sẽ tự cập nhật: hiển thị hành tinh mới và tất cả vệ tinh thuộc hành tinh đó từ database.

### 6.4 Điều khiển animation

- **⏸ Pause / ▶ Play**: Dừng hoặc tiếp tục chuyển động của các vệ tinh.
- **TIME_SCALE slider**: Kéo sang phải để tăng tốc mô phỏng.
  - `1×` = thời gian thực (vệ tinh hầu như không di chuyển)
  - `1000×` = 1 giây thực = ~16 phút quỹ đạo (mặc định)
  - `50000×` = 1 giây thực = ~13 giờ quỹ đạo

### 6.5 Thêm vệ tinh mới

Điền đầy đủ 4 trường ở form dưới panel phải:

| Trường | Ý nghĩa | Ví dụ |
|---|---|---|
| Tên | Tên định danh vệ tinh | `ISS-2` |
| Lat | Vĩ độ (−90 ÷ 90) | `51.6` |
| Lon | Kinh độ (−180 ÷ 180) | `0.0` |
| Alt km | Độ cao so với bề mặt (km) | `408` |
| Relay | Tích nếu là vệ tinh liên lạc | ✓ |

Nhấn **➕ Thêm vào DB**. Vệ tinh sẽ xuất hiện ngay trong cảnh 3D và được lưu vào database.

> Vệ tinh relay (liên lạc) hiển thị màu **vàng** (📡), vệ tinh thường màu **xanh cyan** (🛰).

### 6.6 Xóa vệ tinh

Nhấn vào tên vệ tinh trong danh sách để chọn (highlight xanh), sau đó nhấn **🗑 Xoá vệ tinh đã chọn**. Vệ tinh sẽ bị xóa khỏi cảnh 3D và khỏi database ngay lập tức.

<<<<<<< HEAD
=======
### 6.7 Định tuyến giữa 2 vệ tinh (Routing)

- Trong khu vực **Định tuyến (Dijkstra)**, chọn vệ tinh nguồn ở **Từ:** và vệ tinh đích ở **Đến:**.
- Nhấn nút **🔍 Tìm đường**. Kết quả số bước nhảy (hop) và lộ trình sẽ được hiện ra.
- Trong giao diện 3D, các tia laser màu xanh lục sẽ lập tức xuất hiện nối liền các vệ tinh nằm trong lộ trình. Tia laser sẽ tự động chuyển động theo vệ tinh khi chạy hoạt ảnh (animation).
- Nhấn nút **❌ Xoá đường** để huỷ bỏ đường truyền và tắt hiển thị tia laser.

>>>>>>> 7107ddad8d4205652deb5824bd076c6f36659cf9
---

## 7. Trạng thái hiện tại

| Tính năng | Trạng thái |
|---|---|
| Hiển thị hành tinh 3D (Earth, Mars) với texture | Hoàn thành |
| Xoay / zoom camera | Hoàn thành |
| Lưu vệ tinh vào DB (lat, lon, alt, is_relay) | Hoàn thành |
| Tính vận tốc quỹ đạo (PhysicsEngine) | Hoàn thành |
| Animate vệ tinh bay quanh hành tinh | Hoàn thành |
| Thêm / xóa vệ tinh qua UI | Hoàn thành |
| Phân biệt relay (vàng) vs thường (cyan) | Hoàn thành |
<<<<<<< HEAD
| Định tuyến A→B qua relay (Line-of-Sight + Dijkstra) | Hoàn thành (Thành viên C) |
| Visualize đường truyền bằng laser beam 3D | Hoàn thành (Thành viên C) |
| Báo cáo benchmark số lượng vệ tinh | Hoàn thành (Thành viên C) |
=======
| Định tuyến A→B qua relay (Line-of-Sight + Dijkstra) | Hoàn thành |
| Visualize đường truyền bằng laser beam 3D | Hoàn thành |
| Báo cáo benchmark số lượng vệ tinh | Hoàn thành |
>>>>>>> 7107ddad8d4205652deb5824bd076c6f36659cf9

---

## 8. Ghi chú kỹ thuật

- **JavaFX Y-axis**: Trục Y của JavaFX 3D hướng xuống dưới. Khi áp vị trí lên scene, code dùng `-s.getY()` để đảo chiều, đảm bảo vĩ độ Bắc hiển thị đúng hướng lên trên.
- **SCALE = 1.0/100.0**: 1 đơn vị scene = 100 km. Trái Đất bán kính ~63.7 đơn vị scene.
- **TIME_SCALE**: Hệ số nhân thời gian. Với `1000`, 1 giây thực tương đương 1000 giây quỹ đạo. ISS (chu kỳ ~93 phút thực) sẽ hoàn thành 1 vòng sau khoảng 5.6 giây thực trên màn hình.
- **SQL Authentication**: Kết nối DB dùng SQL Server Authentication (không dùng Windows Integrated), yêu cầu SQL Server được bật Mixed Mode và có login `satellite_user`.
