USE satellite_db;
GO

-- Xoá nếu tồn tại (chạy lại an toàn)
IF OBJECT_ID('satellites','U') IS NOT NULL DROP TABLE satellites;
IF OBJECT_ID('planets','U')    IS NOT NULL DROP TABLE planets;
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

-- Dữ liệu mẫu
INSERT INTO planets (name, radius_km, mass_kg, texture_file) VALUES
  ('Earth', 6371.0, 5.972e24, 'earth.jpg'),
  ('Mars',  3390.0, 6.390e23, 'mars.jpg');

INSERT INTO satellites (planet_id, name, latitude, longitude, altitude_km, is_relay) VALUES
  (1, 'SAT-1',  45.0,  90.0, 400.0, 1),
  (1, 'SAT-2', -30.0, 180.0, 550.0, 1),
  (1, 'SAT-3',  10.0,  45.0, 350.0, 0);
GO
