USE satellite_db;
GO

-- Chạy script này nếu bảng satellites trống sau khi cài đặt theo INSTALL.md
-- Script không DROP bảng, chỉ INSERT dữ liệu mẫu nếu chưa có

IF NOT EXISTS (SELECT 1 FROM satellites WHERE planet_id = 1)
BEGIN
    INSERT INTO satellites (planet_id, name, latitude, longitude, altitude_km, is_relay) VALUES
      (1, 'ISS',      51.6,    0.0,  408.0, 0),
      (1, 'RELAY-A',   0.0,    0.0,  500.0, 1),
      (1, 'RELAY-B',  30.0,   72.0,  500.0, 1),
      (1, 'RELAY-C', -15.0,  144.0,  500.0, 1),
      (1, 'RELAY-D',  20.0,  216.0,  500.0, 1),
      (1, 'RELAY-E', -10.0,  288.0,  500.0, 1),
      (1, 'SAT-LEO',  10.0,   45.0,  350.0, 0);
    PRINT 'Da them 7 ve tinh mau vao Earth.';
END
ELSE
    PRINT 'Bang satellites da co du lieu, bo qua.';

IF NOT EXISTS (SELECT 1 FROM satellites WHERE planet_id = 2)
BEGIN
    INSERT INTO satellites (planet_id, name, latitude, longitude, altitude_km, is_relay) VALUES
      (2, 'MRO',    -30.0,  180.0, 300.0, 0),
      (2, 'MAVEN',   60.0,   90.0, 6200.0, 1),
      (2, 'MARSAT',   0.0,   -90.0, 400.0, 1);
    PRINT 'Da them 3 ve tinh mau vao Mars.';
END
ELSE
    PRINT 'Mars da co du lieu, bo qua.';
GO

SELECT planet_id, COUNT(*) AS so_ve_tinh FROM satellites GROUP BY planet_id;
GO
