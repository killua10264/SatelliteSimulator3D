# ⚙️ Hướng dẫn Cài đặt & Khởi tạo (Phase 1)

Tài liệu này hướng dẫn cài đặt môi trường và xây dựng nền tảng dữ liệu cho dự án (Hoàn thành đến Bước 7 của Gói 1).

## Yêu cầu Hệ thống
- JDK 17 hoặc 21 (Nên dùng [Adoptium](https://adoptium.net/))
- Eclipse IDE for Java Developers (Bản 2023-12 trở lên)
- SQL Server (2019/2022 Express) & SSMS
- JavaFX SDK 21 (Tải từ [GluonHQ](https://gluonhq.com/products/javafx/))

---

## 1. Khởi tạo Database (SQL Server)

1. Mở **SQL Server Management Studio (SSMS)** và kết nối tới `localhost`.
2. Mở một New Query và chạy đoạn script sau để tạo Database và Bảng:

```sql
CREATE DATABASE satellite_db;
GO
USE satellite_db;
GO

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

-- Thêm dữ liệu hành tinh mẫu
INSERT INTO planets (name, radius_km, mass_kg, texture_file) VALUES
  ('Earth', 6371.0, 5.972e24, 'earth.jpg'),
  ('Mars',  3390.0, 6.390e23, 'mars.jpg');
GO
```

---

## 2. Thiết lập Project trong Eclipse

1. Trong Eclipse: Chọn `File` -> `New` -> `Other` -> `Maven` -> `Maven Project`.
2. Tích vào **Create a simple project (skip archetype selection)**.
3. Nhập Group Id: `com.satellite`, Artifact Id: `SatelliteSimulator` -> Nhấn **Finish**.
4. Mở file `pom.xml` và cấu hình thư viện JavaFX & Microsoft SQL Server JDBC:

```xml
<dependencies>
    <!-- JavaFX -->
    <dependency>
        <groupId>org.openjfx</groupId>
        <artifactId>javafx-controls</artifactId>
        <version>21.0.2</version>
    </dependency>
    <!-- Microsoft SQL Server JDBC -->
    <dependency>
        <groupId>com.microsoft.sqlserver</groupId>
        <artifactId>mssql-jdbc</artifactId>
        <version>12.6.1.jre11</version>
    </dependency>
</dependencies>
```
5. Nhấn chuột phải vào Project -> `Maven` -> `Update Project...` để tải thư viện.

---

> [!NOTE]
> Hệ trục tọa độ của JavaFX có trục Y hướng xuống, do đó khi đưa vật thể lên Scene, cần dùng `-y` để vệ tinh nằm đúng bán cầu Bắc/Nam.

---
### Các bước tiếp theo
Dự án đã sẵn sàng nền tảng dữ liệu (DB, Model, DAO) và công cụ tính toán tọa độ. Vui lòng chuyển sang tích hợp JavaFX Scene 3D để hiển thị đồ họa.
