USE satellite_db;
GO

-- Thêm 5 vệ tinh relay trải đều quanh Earth (LEO 500km)
-- Dùng để test định tuyến Dijkstra + Line-of-Sight
INSERT INTO satellites (planet_id, name, latitude, longitude, altitude_km, is_relay) VALUES
  (1, 'RELAY-A',   0.0,    0.0, 500.0, 1),
  (1, 'RELAY-B',  30.0,   72.0, 500.0, 1),
  (1, 'RELAY-C', -15.0,  144.0, 500.0, 1),
  (1, 'RELAY-D',  20.0,  216.0, 500.0, 1),
  (1, 'RELAY-E', -10.0,  288.0, 500.0, 1);
GO

-- Kiểm tra tổng vệ tinh
SELECT id, name, latitude, longitude, altitude_km, is_relay
FROM satellites
WHERE planet_id = 1
ORDER BY id;
GO
