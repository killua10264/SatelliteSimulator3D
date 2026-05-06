# 🛸 Satellite Simulator 3D

Dự án Java/JavaFX mô phỏng không gian 3D, tính toán quỹ đạo vệ tinh và định tuyến truyền tin sử dụng thuật toán Dijkstra và kiểm tra Line-of-Sight.

## 🌟 Tính năng chính

- **Môi trường 3D**: Hiển thị hành tinh 3D (Trái Đất, Sao Hỏa) với giao diện tương tác (xoay, zoom).
- **Quản lý Vệ tinh (CRUD)**: Thêm, hiển thị vệ tinh tĩnh dựa trên tọa độ địa lý (Kinh độ, Vĩ độ, Độ cao).
- **Vật lý Quỹ đạo**: Tính toán vận tốc và mô phỏng chuyển động bay quanh hành tinh theo thời gian thực.
- **Định tuyến Truyền tin**: Thuật toán tìm đường đi ngắn nhất (Dijkstra) giữa các vệ tinh, có xét đến vật cản là hành tinh (Line-of-Sight).

## 🛠 Công nghệ sử dụng

- **Ngôn ngữ**: Java 17 (hoặc 21)
- **Giao diện**: JavaFX 21 (với thư viện 3D tích hợp)
- **Cơ sở dữ liệu**: SQL Server 2019/2022
- **Quản lý dự án**: Maven
- **IDE Đề xuất**: Eclipse 2023-12

## 📁 Cấu trúc Dự án (Nền tảng)

Dự án được chia theo mô hình MVC kết hợp DAO pattern:

```text
com.satellite
├── model/      # Các thực thể dữ liệu (Planet, Satellite)
├── dao/        # Tương tác với Database (PlanetDAO, SatelliteDAO)
├── physics/    # Logic vật lý, tính toán vận tốc, quỹ đạo
├── routing/    # Thuật toán Line-of-Sight và Dijkstra
├── ui/         # Giao diện JavaFX (Scene 3D, Bảng điều khiển)
└── util/       # Công cụ hỗ trợ (CoordConverter - Chuyển đổi tọa độ)
```

## 👥 Nhóm phát triển
- **Thành viên A**: Môi trường 3D & CRUD
- **Thành viên B**: Vật lý Quỹ đạo & Animation
- **Thành viên C**: Định tuyến & Đánh giá

---
*Vui lòng xem file `INSTALL.md` để biết chi tiết cách cài đặt và thiết lập dự án.*
