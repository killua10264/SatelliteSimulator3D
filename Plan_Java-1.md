# Kế hoạch Triển khai: Mô phỏng 3D & Định tuyến Vệ tinh (Java/JavaFX)

## Tổng quan Đề bài

Xây dựng ứng dụng Desktop Java/JavaFX mô phỏng vệ tinh bay quanh hành tinh 3D, gồm:
1. Hiển thị hành tinh 3D với texture từ solarsystemscope.com
2. Thiết lập & lưu vị trí vệ tinh vào CSDL (kinh độ, vĩ độ, độ cao)
3. Tính vận tốc bay quỹ đạo (vật lý), hiển thị chuyển động
4. Định tuyến Dijkstra + Line-of-Sight giữa các vệ tinh liên lạc
5. Hướng dẫn cài đặt & sử dụng chi tiết
6. Báo cáo đánh giá hiệu năng định tuyến với số lượng vệ tinh khác nhau

---

## Kiến trúc Dự án

```
SatelliteSimulator/
├── src/main/java/com/satellite/
│   ├── model/          # Entity thuần (Planet, Satellite)
│   ├── dao/            # JDBC Data Access (PlanetDAO, SatelliteDAO)
│   ├── physics/        # PhysicsEngine (tính vận tốc, quỹ đạo)
│   ├── routing/        # RoutingEngine (LineOfSight, Dijkstra)
│   ├── ui/             # JavaFX Controllers & Scenes
│   │   ├── MainController.java
│   │   ├── Scene3D.java
│   │   └── ControlPanel.java
│   ├── util/           # CoordConverter, BenchmarkRunner
│   └── Main.java
├── src/main/resources/
│   ├── textures/       # earth.jpg, mars.jpg... (tải từ solarsystemscope)
│   └── fxml/           # FXML layouts
├── sql/
│   └── create_tables.sql
├── docs/
│   ├── INSTALL.md
│   ├── USER_GUIDE.md
│   └── benchmark_report.txt
└── pom.xml             # Maven: JavaFX + MySQL Connector
```

---

## Database Schema

```sql
-- sql/create_tables.sql
CREATE TABLE planets (
    id       INT PRIMARY KEY AUTO_INCREMENT,
    name     VARCHAR(100) NOT NULL,
    radius_km   DOUBLE NOT NULL,   -- bán kính (km)
    mass_kg     DOUBLE NOT NULL,   -- khối lượng (kg) → tính GM
    texture_file VARCHAR(255)       -- tên file texture
);

CREATE TABLE satellites (
    id          INT PRIMARY KEY AUTO_INCREMENT,
    planet_id   INT NOT NULL,
    name        VARCHAR(100) NOT NULL,
    latitude    DOUBLE NOT NULL,   -- vĩ độ φ (độ)
    longitude   DOUBLE NOT NULL,   -- kinh độ λ (độ)
    altitude_km DOUBLE NOT NULL,   -- độ cao h (km)
    is_relay    BOOLEAN DEFAULT FALSE,  -- TRUE = vệ tinh liên lạc
    FOREIGN KEY (planet_id) REFERENCES planets(id)
);

INSERT INTO planets VALUES (1,'Earth',6371,5.972e24,'earth.jpg');
INSERT INTO planets VALUES (2,'Mars', 3390,6.390e23,'mars.jpg');
```

---

## Phân công Chi tiết theo Gói

---

### 👤 Thành viên A — Gói 1: Nền tảng 3D & CRUD

#### Nhiệm vụ cốt lõi

**1. Tạo khung Maven + Cấu hình `pom.xml`**
```xml
<dependencies>
  <dependency><!-- JavaFX --></dependency>
  <dependency><!-- MySQL Connector 8.x --></dependency>
</dependencies>
```

**2. `DatabaseConnection.java`** — Singleton JDBC
```java
public class DatabaseConnection {
    private static Connection conn;
    public static Connection get() {
        if (conn == null)
            conn = DriverManager.getConnection(URL, USER, PASS);
        return conn;
    }
}
```

**3. Entity Model**
```java
public class Satellite {
    private int id;
    private String name;
    private double latitude, longitude, altitudeKm;
    private boolean isRelay;
    // x, y, z được tính từ lat/lon/alt — KHÔNG lưu DB
    private double x, y, z;
}
```

**4. `CoordConverter.java`** — Chuyển đổi hệ tọa độ
```
x = (R + h) * cos(φ) * cos(λ)
y = (R + h) * sin(φ)
z = (R + h) * cos(φ) * sin(λ)
(φ = latitude, λ = longitude, h = altitude, R = planet radius)
```

**5. `SatelliteDAO.java`**
```java
void insert(Satellite s)           // Thêm vệ tinh mới
List<Satellite> findAll(int planetId) // Lấy danh sách theo hành tinh
void delete(int id)
```

**6. JavaFX — `Scene3D.java`**
- Tạo `Sphere` kích thước lớn → apply `PhongMaterial` với texture từ file
- Mỗi vệ tinh = `Sphere` nhỏ (r=5) đặt tại (x, y, z) tính từ CoordConverter
- Camera: `PerspectiveCamera` + `MouseDragRotate` để xoay cảnh

**7. Form CRUD**
- TextField: Tên, Lat, Long, Alt, isRelay checkbox
- Nút "Thêm" → gọi DAO → refresh danh sách 3D

---

### 👤 Thành viên B — Gói 2: Vật lý Quỹ đạo & Animation

#### Công thức Toán học

```
G  = 6.674 × 10⁻¹¹  (hằng số hấp dẫn)
r  = R_planet + altitude_km (km → m)
v  = √(G × M / r)            → vận tốc quỹ đạo (m/s)
ω  = v / r                   → vận tốc góc (rad/s)
T  = 2π / ω                  → chu kỳ quỹ đạo (s)
```

**`PhysicsEngine.java`**
```java
public class PhysicsEngine {
    public static double calcOrbitalVelocity(double massKg, double radiusM) {
        return Math.sqrt(G * massKg / radiusM);
    }
    public static double calcAngularVelocity(double v, double r) {
        return v / r;
    }
}
```

#### Animation trong JavaFX

```java
// Mỗi vệ tinh có góc θ riêng, tăng theo thời gian
AnimationTimer timer = new AnimationTimer() {
    long lastTime = 0;
    public void handle(long now) {
        double dt = (now - lastTime) / 1e9; // giây
        for (SatelliteNode sn : satelliteNodes) {
            sn.theta += sn.omega * dt * TIME_SCALE;
            double r = sn.planet.radius + sn.altitudeKm * 1000;
            sn.node.setTranslateX(r * Math.cos(sn.theta));
            sn.node.setTranslateZ(r * Math.sin(sn.theta));
            // y giữ nguyên theo latitude
        }
        lastTime = now;
    }
};
```

> **Lưu ý:** Mặt phẳng quỹ đạo nghiêng theo `latitude` ban đầu (dùng Rotation transform).

#### (Tuỳ chọn) Vẽ Quỹ đạo
- Dùng `Torus` hoặc vẽ `Polyline3D` bằng các `Cylinder` nhỏ nối điểm

---

### 👤 Thành viên C — Gói 3: Line-of-Sight & Dijkstra

#### Thuật toán Line-of-Sight

Kiểm tra đường thẳng nối Sat1→Sat2 có đi qua hành tinh (bán kính R) không:

```java
public static boolean hasLOS(double[] p1, double[] p2, double R) {
    // Vector d = p2 - p1
    double dx = p2[0]-p1[0], dy = p2[1]-p1[1], dz = p2[2]-p1[2];
    // t tối ưu = -(p1·d) / |d|²
    double dot = p1[0]*dx + p1[1]*dy + p1[2]*dz;
    double len2 = dx*dx + dy*dy + dz*dz;
    double t = Math.max(0, Math.min(1, -dot / len2));
    // Điểm gần nhất với tâm hành tinh
    double cx = p1[0]+t*dx, cy = p1[1]+t*dy, cz = p1[2]+t*dz;
    double dist2 = cx*cx + cy*cy + cz*cz;
    return dist2 > R * R;  // true = thấy nhau
}
```

#### Dijkstra trên đồ thị vệ tinh

```java
// Graph: adjacency list, trọng số = khoảng cách Euclidean
// Chỉ thêm cạnh nếu hasLOS(sat1, sat2, R) == true
public List<Integer> dijkstra(int src, int dst, List<Satellite> sats, double R) {
    // Priority queue (distance, nodeId)
    // Standard Dijkstra — trả về List<Integer> là chuỗi ID vệ tinh
}
```

#### Hiển thị tia Laser 3D

```java
// Vẽ Cylinder giữa 2 điểm trong không gian 3D
private Cylinder makeLaserLine(double[] from, double[] to) {
    double dist = distance(from, to);
    Cylinder cyl = new Cylinder(1, dist);
    // Tính midpoint và góc xoay để cyl nằm đúng hướng
    // Dùng Rotate transform
    return cyl;
}
```

#### Benchmark Report

```java
// BenchmarkRunner.java
for (int n : new int[]{10, 20, 50}) {
    List<Satellite> sats = generateRandomSats(n);
    long start = System.nanoTime();
    engine.dijkstra(0, n-1, sats, R);
    long elapsed = System.nanoTime() - start;
    report.write("N=" + n + " → " + elapsed/1e6 + " ms\n");
}
```

---

## Lịch Sprint Tối ưu (10 Ngày)

| Ngày | A | B | C |
|------|---|---|---|
| 1 | Tạo project Maven, DB schema, `DatabaseConnection`, entity `Planet`/`Satellite` → **Push lên GitHub** | Setup môi trường, đọc hiểu công thức vật lý | Viết Dijkstra thuần trên mảng tĩnh (không cần DB) |
| 2 | `CoordConverter` + load texture hành tinh 3D | Viết `PhysicsEngine` (unit test độc lập) | Viết `checkLineOfSight` trên tọa độ tĩnh |
| 3 | `SatelliteDAO` (insert/findAll) + Form CRUD | Viết `AnimationTimer` scaffold | Kết hợp LOS + Dijkstra → test với 10 node giả |
| 4 | **🔄 Review**: A merge CRUD lên `main` | Nhận entity từ A, gắn vào AnimationTimer | Đọc code A, chuẩn bị interface nhận tọa độ live |
| 5 | Hiển thị vệ tinh tĩnh từ DB lên Scene3D | Satellite bắt đầu bay (animation chạy được) | Chuyển từ mảng tĩnh → nhận List từ DAO |
| 6 | Hỗ trợ C vẽ laser, polish UI | **⚡ Push code Animation** | Gọi tọa độ real-time từ B, vẽ laser Dijkstra |
| 7 | **🔄 Review**: B và C merge lên `main` | | |
| 8-9 | Cả nhóm: bug fix, test kịch bản thực | | C: chạy Benchmark + viết report |
| 10 | A+B: viết INSTALL.md, USER_GUIDE.md | | C: hoàn thiện benchmark_report.txt |

---

## Giao diện (UI Layout Đề xuất)

```
┌─────────────────────────────────────────────────────┐
│  [Planet Selector ▼]  [Add Satellite] [Route A→B]   │ ← ToolBar
├──────────────────────────────┬──────────────────────┤
│                              │  CONTROL PANEL       │
│    SCENE 3D (JavaFX 3D)      │  ─────────────────── │
│                              │  Satellite List      │
│   🌍 + 🛸🛸🛸 (orbiting)     │  [id] Name  Alt  Relay│
│       with laser beams       │  ─────────────────── │
│                              │  Add Satellite Form  │
│                              │  Name: [_________]   │
│                              │  Lat:  [___] Lon:[__]│
│                              │  Alt:  [___] km      │
│                              │  Relay: [✓]          │
│                              │  [Save to DB]        │
│                              │  ─────────────────── │
│                              │  Routing             │
│                              │  From: [id▼] To:[id▼]│
│                              │  [Find Route]        │
│                              │  Result: A→C→E→B     │
└──────────────────────────────┴──────────────────────┘
│  Status Bar: 12 satellites | Route: 4 hops | 284ms   │
└─────────────────────────────────────────────────────┘
```

---

## Dependency Map (Cập nhật)

```
A (Nền tảng)
 └─► Ngày 1: Push Satellite.java, DatabaseConnection.java
 └─► Ngày 4: Push SatelliteDAO + Scene3D cơ bản

B (Animation)        C (Routing)
 └─► Nhận từ A       └─► Tự test ngày 1-5 (tọa độ tĩnh)
 └─► Ngày 6: Push    └─► Nhận tọa độ live từ B (ngày 6)
     getCurrentPos()      → vẽ laser + chạy benchmark
```

---

## Checklist Nộp bài

- [ ] Chạy được trên máy mới sau khi làm theo `INSTALL.md`
- [ ] Kết nối DB thành công, CRUD vệ tinh hoạt động
- [ ] Vệ tinh bay theo đúng công thức vật lý
- [ ] Routing A→B vẽ được laser 3D, hiển thị lộ trình
- [ ] File `benchmark_report.txt` với N = 10, 20, 50
- [ ] `USER_GUIDE.md` có ảnh chụp màn hình

---

## Ghi chú Kỹ thuật Quan trọng

> [!WARNING]
> JavaFX 3D yêu cầu máy có GPU hỗ trợ hardware acceleration. Kiểm tra bằng `System.out.println(Platform.isSupported(ConditionalFeature.SCENE3D))`.

> [!TIP]
> Dùng `TIME_SCALE = 1000` để tăng tốc animation (1 giây thực = 1000 giây quỹ đạo) giúp demo rõ hơn.

> [!NOTE]
> Texture từ solarsystemscope.com có sẵn dạng `.jpg` miễn phí. Tải `2k_earth_daymap.jpg` và đặt vào `src/main/resources/textures/`.
